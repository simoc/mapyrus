/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package org.mapyrus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.mapyrus.font.AdobeFontMetricsManager;
import org.mapyrus.font.PostScriptFont;
import org.mapyrus.font.StringDimension;
import org.mapyrus.font.TrueTypeFont;
import org.mapyrus.io.ASCII85Writer;
import org.mapyrus.io.WildcardFile;
import org.mapyrus.ps.PostScriptFile;

/**
 * Abstraction of a graphics format.  Provides methods to create new
 * output files and then draw to them, independent of the graphics
 * format.
 */
public class OutputFormat
{
	/*
	 * Type of output currently being generated.
	 */
	private static final int INTERNAL_IMAGE = 1;
	private static final int IMAGE_FILE = 2;

	/*
	 * Output to window on screen.
	 */
	private static final int SCREEN_WINDOW = 3;

	/*
	 * PostScript output can be created as either moveto-lineto-stroke
	 * commands to draw shapes on page or as an single image covering
	 * the whole page containing all drawn shapes.
	 */
	private static final int POSTSCRIPT_GEOMETRY = 4;
	private static final int POSTSCRIPT_IMAGE = 5;

	/*
	 * Type of justification for labels on page, as used
	 * in a word processor and in HTML tags.
	 */
	public static final int JUSTIFY_LEFT = 1;
	public static final int JUSTIFY_CENTER = 2;
	public static final int JUSTIFY_RIGHT = 4;
	public static final int JUSTIFY_TOP = 8;
	public static final int JUSTIFY_MIDDLE = 16;
	public static final int JUSTIFY_BOTTOM = 32;
	
	/*
	 * File or image that drawing commands are
	 * writing to.
	 */
	private int mOutputType;
	private String mFormatName;
	private BufferedImage mImage;
	private String mFilename;
	private PrintWriter mWriter;
	private OutputStream mOutputStream;
	private Graphics2D mGraphics2D;
	private boolean mIsPipedOutput;	
	private boolean mIsStandardOutput;
	private boolean mIsUpdatingFile;
	private Process mOutputProcess;
	private File mTempFile;
	
	/*
	 * Frequently used fonts.
	 */
	private FontCache mFontCache;

	/*
	 * List of font definitions included in this PostScript file and list of fonts
	 * used in this file but not defined.
	 */
	private HashSet mSuppliedFontResources;
	private HashSet mNeededFontResources;

	/*
	 * Fonts which are to be re-encoded to ISOLatin1 in PostScript file.
	 * This is normally done so that extended symbols (such as degree symbol)
	 * can be used.
	 */
	private HashSet mEncodeAsISOLatin1;

	/*
	 * Adobe Font Metrics files containing character width information for all fonts.
	 */
	private AdobeFontMetricsManager mAdobeFontMetrics;
	private ArrayList mAfmFiles;
	
	/*
	 * List of TrueType fonts to load using Java Font.createFont() method.
	 */
	private HashMap mTTFFonts;

	/*
	 * Page dimensions and resolution.
	 */
	private double mPageWidth;
	private double mPageHeight;
	private double mResolution;
	
	/*
	 * Justification for labels as fraction of string height and width
	 * to move string in X and Y direction to achieve correct justification.
	 */
	private double mJustificationShiftX;
	private double mJustificationShiftY;

	/*
	 * Rotation of current font in radians, with 0 horizontal,
	 * measured counter-clockwise.
	 */
	private double mFontRotation;

	/*
	 * If non-zero, gives linewidth to use for drawing outlines of
	 * each character of labels.
	 */
	private double mFontOutlineWidth;

	private Font mBaseFont;

	/*
	 * Mask containing protected areas of the page.
	 */
	private PageMask mPageMask;

	/**
	 * Write PostScript file header, including document structuring conventions (DSC).
	 * @param width width of page in mm.
	 * @param height height of page in mm.
	 * @param resolution resolution of page in DPI.
	 * @param turnPage flag true when page is to be rotated 90 degrees.
	 * @param fontList list of PostScript fonts to include in header.
	 * @param backgroundColor background color for page, or null if no background.
	 */
	private void writePostScriptHeader(double width, double height,
		int resolution, boolean turnPage, ArrayList fontList, Color backgroundColor)
		throws IOException, MapyrusException
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);

		mWriter.print("%!PS-Adobe-3.0");
		if (mFormatName.equals("eps") || mFormatName.equals("epsimage"))
			mWriter.print(" EPSF-3.0");
		mWriter.println("");

		if (turnPage)
			mWriter.println("%%BoundingBox: 0 0 " + heightInPoints + " " + widthInPoints);
		else
			mWriter.println("%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);

		if ((!mFormatName.equals("eps")) && (!mFormatName.equals("epsimage")))
			mWriter.println("%%Pages: 1");

		mWriter.println("%%DocumentData: Clean7Bit");
		mWriter.println("%%LanguageLevel: 2");
		mWriter.println("%%Creator: (" + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + ")");
		mWriter.println("%%OperatorMessage: (Mapyrus Output...)");
		Date now = new Date();
		mWriter.println("%%CreationDate: (" + now.toString() + ")");
		String username = System.getProperty("user.name");
		if (username != null)
			mWriter.println("%%For: (" + username + ")");

		/*
		 * List fonts included in this PostScript file.
		 */
		mWriter.println("%%DocumentRequiredResources: (atend)");
		if (fontList.size() > 0)
		{
			mWriter.print("%%DocumentSuppliedResources: font");
			Iterator it = fontList.iterator();
			while (it.hasNext())
			{
				PostScriptFont psFont = (PostScriptFont)(it.next());
				mWriter.print(" " + psFont.getName());
				mSuppliedFontResources.add(psFont.getName());
			}
			mWriter.println("");
		}
		mWriter.println("%%EndComments");
		mWriter.println("");
		mWriter.println("% Resolution " + resolution + " DPI");

		/*
		 * Inline font definitions.
		 */
		mWriter.println("%%BeginSetup");
		Iterator it = fontList.iterator();
		while (it.hasNext())
		{
			PostScriptFont psFont = (PostScriptFont)(it.next());

			mWriter.println("%%BeginResource: font " + psFont.getName());
			String fontDefinition = psFont.getFontDefinition();
			mWriter.println(fontDefinition);
			mWriter.println("%%EndResource");			
		}
		mWriter.println("%%EndSetup");

		/*
		 * Set color and linestyle to reasonable default values.
		 * Taken from 'initgraphics' operator example in PostScript Language
		 * Reference Manual.
		 */
		mWriter.println("1 setlinewidth 0 setlinecap 0 setlinejoin");
		mWriter.println("[] 0 setdash 0 setgray 10 setmiterlimit");

		if (turnPage)
		{
			/*
			 * Turn page 90 degrees so that a landscape orientation page appears
			 * on a portrait page.
			 */
			mWriter.println("% Turn page 90 degrees.");
			mWriter.println("90 rotate 0 " + heightInPoints + " neg translate");
		}

		/* 
		 * Prevent anything being displayed outside bounding box we've just defined.
		 */
		mWriter.println("0 0 " + widthInPoints + " " + heightInPoints + " rectclip");

		/*
		 * Set background color for page.
		 */
		if (backgroundColor != null)
		{
			float c[] = backgroundColor.getRGBColorComponents(null);
			mWriter.println("gsave");
			mWriter.println(c[0] + " " + c[1] + " " + c[2] + " setrgbcolor");
			mWriter.println("0 0 " + widthInPoints + " " + heightInPoints + " rectfill");
			mWriter.println("grestore");
		}

		/*
		 * Define shorter names for most commonly used operations.
		 * Bind all operators names to improve performance (see 3.11 of
		 * PostScript Language Reference Manual).
		 */
		mWriter.println("/m { moveto } bind def /l { lineto } bind def");
		mWriter.println("/s { stroke } bind def /f { fill } bind def");
		mWriter.println("/j { /fjy exch def /fjx exch def } bind def");

		/*
		 * Define font and dictionary entries for font size and justification.
		 * Don't bind these as font loading operators may be overridden in interpreter.
		 */
		mWriter.println("/font {");
		mWriter.println("/foutline exch def");
		mWriter.println("/frot exch radtodeg def");
		mWriter.println("/fsize exch def findfont fsize scalefont setfont } def");
		mWriter.println("/radtodeg { 180 mul 3.1415629 div } bind def");

		/*
		 * Draw text string, after setting correct position, rotation,
		 * justifying it horizontally and vertically for current font size
		 * and shifting it down a number of lines if it is part of a multi-line
		 * string.
		 *
		 * Line number (starting at 0) and string to show are passed to this procedure.
		 */
		mWriter.println("/t { gsave currentpoint translate frot rotate");
		mWriter.println("dup stringwidth pop fjx mul");
		mWriter.println("3 -1 roll neg fjy add fsize mul");
		mWriter.println("rmoveto foutline 0 gt");
		mWriter.println("{false charpath foutline 0 0 2 sl stroke} {show} ifelse");
		mWriter.println("grestore newpath } bind def");

		mWriter.println("/rgb { setrgbcolor } bind def");
		mWriter.println("/sl { setmiterlimit setlinejoin setlinecap");
		mWriter.println("setlinewidth } bind def");

		/*
		 * Use new dictionary in saved state so that variables we define
		 * do not overwrite variables in parent state.
		 */
		mWriter.println("/gs { gsave 4 dict begin } bind def");
		mWriter.println("/gr { end grestore } bind def");
		mWriter.println("");
	}

	/*
	 * Set scale in PostScript file so that we can give all coordinate
	 * positions in millimetres.
	 */
	private void writePostScriptScaling()
	{
		/*
		 * Set plotting units to millimetres.
		 */
		mWriter.println("% Set scaling so that (x, y) coordinates are given in millimetres");
		mWriter.println(Constants.POINTS_PER_INCH + " " + Constants.MM_PER_INCH +
			" div dup scale");
	}
	
	/**
	 * Sets correct rendering hints and transformation
	 * for buffered image we will plot to.
	 * @param resolution resolution for page in DPI.
	 * @param backgroundColor background color for page, or null if no background.
	 * @param lineAliasing flag true if lines should be drawn with anti-aliasing.
	 * @param labelAliasing flag true if labels should be drawn with anti-aliasing.
	 */
	private void setupBufferedImage(double resolution, Color backgroundColor,
		boolean lineAntiAliasing, boolean labelAntiAliasing)
	{
		double scale;

		scale = resolution / Constants.MM_PER_INCH;

		/*
		 * Set background of entire image to desired color.
		 */
		if (backgroundColor != null)
		{
			Color originalColor = mGraphics2D.getColor();
			mGraphics2D.setColor(backgroundColor);
			mGraphics2D.fillRect(0, 0, mImage.getWidth(), mImage.getHeight());
			mGraphics2D.setColor(originalColor);
		}

		/*
		 * Set transform with origin in lower-left corner and
		 * Y axis increasing upwards.
		 */
		mGraphics2D.translate(0, mImage.getHeight());
		mGraphics2D.scale(scale, -scale);
		
		/*
		 * Set anti-aliasing for labels and lines if the user wants it.
		 */
		if (lineAntiAliasing)
		{
			mGraphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
		}
		else
		{
			mGraphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF));
		}

		if (labelAntiAliasing)
		{
			mGraphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		}
		else
		{
			mGraphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
		}
	}

	/**
	 * Indicates whether an image format is supported or not.
	 * @param formatName
	 * @return true if creation of images in given format is supported.
	 */
	private boolean isSupportedImageFormat(String formatName)
	{
		boolean found = false;
		String knownFormats[] = ImageIO.getWriterFormatNames();
		for (int i = 0; i < knownFormats.length && found == false; i++)
		{
			if (formatName.equalsIgnoreCase(knownFormats[i]))
			{
				found = true;
			}
		}
		return(found);
	}

	/**
	 * Return PostScript commands to re-encode a font in ISOLatin1 encoding.
	 * @param fontName name of font to re-encode.
	 * @return string containing PostScript commands to re-encode font.
	 */
	private String isoLatinEncode(String fontName)
	{
		/*
		 * Re-encoding commands taken from section 5.6.1 of Adobe PostScript
		 * Language Reference Manual (2nd Edition).
		 */
		return("/" + fontName + " findfont" + Constants.LINE_SEPARATOR +
			"dup length dict begin" + Constants.LINE_SEPARATOR +
			"{1 index /FID ne {def} {pop pop} ifelse} forall" + Constants.LINE_SEPARATOR +
			"/Encoding ISOLatin1Encoding def" + Constants.LINE_SEPARATOR +
			"currentdict" + Constants.LINE_SEPARATOR +
			"end" + Constants.LINE_SEPARATOR +
			"/" + fontName + " exch definefont pop");
	}

	private void setOutput(String filename, double width, double height,
		String extras, PrintStream stdoutStream)
		throws IOException, MapyrusException
	{
		/*
		 * Parse list of additional options given by caller.
		 */
		ArrayList fontList = new ArrayList();
		mEncodeAsISOLatin1 = new HashSet();
		mTTFFonts = new HashMap();
		mAfmFiles = new ArrayList();
		mIsUpdatingFile = false;
		int resolution;
		boolean turnPage = false;
		Color backgroundColor = null;
		boolean labelAntiAliasing = true;
		boolean lineAntiAliasing = false;
		Rectangle2D existingBoundingBox = null;

		if (mOutputType == POSTSCRIPT_GEOMETRY)
			resolution = 300;
		else
			resolution = Constants.getScreenResolution();

		/*
		 * Reading all font metrics information takes some time.
		 * Wait until we really need it before loading it.
		 */
		mAdobeFontMetrics = null;
	
		StringTokenizer st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("pfafiles="))
			{
				/*
				 * Build list of font filenames user wants
				 * to include in this PostScript file.
				 */
				StringTokenizer st2 = new StringTokenizer(token.substring(9), ",");
				while (st2.hasMoreTokens())
				{
					String pfaFilename = st2.nextToken();
					if (pfaFilename.length() > 0)
					{
						/*
						 * Accept wildcards in filenames.
						 */
						WildcardFile wildcard = new WildcardFile(pfaFilename);
						Iterator it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
							fontList.add(new PostScriptFont((String)it.next()));
					}
				}
			}
			if (token.startsWith("afmfiles="))
			{
				StringTokenizer st2 = new StringTokenizer(token.substring(9), ",");
				while (st2.hasMoreTokens())
				{
					String afmFilename = st2.nextToken();
					if (afmFilename.length() > 0)
					{
						/*
						 * Accept wildcards in filenames.
						 */
						WildcardFile wildcard = new WildcardFile(afmFilename);
						Iterator it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
						{
							mAfmFiles.add(it.next());
						}
										
					}
				}
			}
			else if (token.startsWith("isolatinfonts="))
			{
				/*
				 * Build list of fonts to encode in ISOLatin1.
				 */
				StringTokenizer st2 = new StringTokenizer(token.substring(14), ",");
				while (st2.hasMoreTokens())
				{
					String fontName = st2.nextToken();
					if (fontName.length() > 0)
						mEncodeAsISOLatin1.add(fontName);
				}
			}
			else if (token.startsWith("resolution="))
			{
				String r = token.substring(11);
				try
				{
					resolution = Integer.parseInt(r);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_RESOLUTION) +
						": " + r);
				}
				if (resolution < 1)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_RESOLUTION) +
						": " + r);
				}	
			}
			else if (token.startsWith("ttffiles="))
			{
				/*
				 * Build list of TrueType font filenames user wants
				 * to open with Java methods.
				 */
				StringTokenizer st2 = new StringTokenizer(token.substring(9), ",");
				while (st2.hasMoreTokens())
				{
					String ttfFilename = st2.nextToken();
						
					/*
					 * Accept wildcards in filenames.
					 */
					if (ttfFilename.length() > 0)
					{
						WildcardFile wildcard = new WildcardFile(ttfFilename);
						Iterator it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
						{
							String s = (String)it.next();
							TrueTypeFont ttf = new TrueTypeFont(s);
							mTTFFonts.put(ttf.getName(), ttf);
						}
					}
				}
			}
			else if (token.startsWith("turnpage="))
			{
				String flag = token.substring(9);
				turnPage = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("labelantialiasing="))
			{
				String flag = token.substring(18);
				labelAntiAliasing = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("lineantialiasing="))
			{
				String flag = token.substring(17);
				lineAntiAliasing = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("update="))
			{
				String flag = token.substring(7);
				mIsUpdatingFile = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("background="))
			{
				String colorName = token.substring(11);
				backgroundColor = ColorDatabase.getColor(colorName, 255);
				if (backgroundColor == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.COLOR_NOT_FOUND) +
						": " + colorName);
				}
			}
		}

		if ((mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == POSTSCRIPT_IMAGE ||
			mOutputType == IMAGE_FILE) && (!mIsUpdatingFile))
		{
			/*
			 * Should we pipe the output to another program
			 * instead of writing a file?
			 */
			mIsPipedOutput = filename.startsWith("|");

			/*
			 * Are we writing to standard output instead of to a file?
			 */
			mIsStandardOutput = filename.equals("-");

			if (mIsPipedOutput)
			{
				String pipeCommand = filename.substring(1).trim();
				mOutputProcess = Runtime.getRuntime().exec(pipeCommand);
				mOutputStream = mOutputProcess.getOutputStream();
			}
			else
			{
				if (mIsStandardOutput)
					mOutputStream = stdoutStream;
				else
					mOutputStream = new FileOutputStream(filename);
			}
		}

		File f = new File(filename);
		if (mIsUpdatingFile)
		{
			if (!f.canWrite())
			{
				throw new IOException(MapyrusMessages.get(MapyrusMessages.READ_ONLY) + ": " + filename);
			}
		}

		/*
		 * Setup file we are writing to.
		 */
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == POSTSCRIPT_IMAGE)
		{
			if (mIsUpdatingFile)
			{
				
				PostScriptFile ps = new PostScriptFile(filename);
				if (ps.getNumberOfPages() > 1)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_PS_FILE) + ": " + filename);
				}

				/*
				 * Use size of existing PostScript file as size for new page.
				 */
				existingBoundingBox = ps.getBoundingBox();
				width = existingBoundingBox.getMaxX() / Constants.POINTS_PER_INCH *
					Constants.MM_PER_INCH;
				height = existingBoundingBox.getMaxY() / Constants.POINTS_PER_INCH *
					Constants.MM_PER_INCH;

				/*
				 * Start writing to a temporary file in same directory.  We'll replace the
				 * original file at the end when the file is successfully completed.
				 */
				mTempFile = File.createTempFile(Constants.PROGRAM_NAME, null,
					new File(filename).getAbsoluteFile().getParentFile());
				mOutputStream = new FileOutputStream(mTempFile);
			}

			mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)));

			mSuppliedFontResources = new HashSet();

			writePostScriptHeader(width, height, resolution, turnPage, fontList, backgroundColor);

			mNeededFontResources = new HashSet();

			if (mIsUpdatingFile)
			{
				/*
				 * Append contents of existing file as an included document
				 * to the new file we are creating.
				 */
				writePostScriptLine("save");
				writePostScriptLine("/showpage {} def");
				writePostScriptLine("%%BeginDocument: " + filename);
				BufferedReader r = null;

				try
				{
					r = new BufferedReader(new FileReader(filename));
					String line;
					while ((line = r.readLine()) != null)
					{
						writePostScriptLine(line);
					}
				}
				finally
				{
					try
					{
						if (r != null)
							r.close();
					}
					catch (IOException e)
					{
					}
				}
				writePostScriptLine("%%EndDocument");
				writePostScriptLine("restore");
			}
			writePostScriptScaling();
		}

		if (mOutputType != POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Create image to draw into.
			 */
			if (mOutputType == IMAGE_FILE ||
				mOutputType == SCREEN_WINDOW ||
				mOutputType == POSTSCRIPT_IMAGE)
			{
				if (mIsUpdatingFile && mOutputType == IMAGE_FILE)
				{
					/*
					 * Read existing image for editing.
					 * Set page width and height to size of existing image.
					 */
					mImage = ImageIO.read(f);
					if (mImage == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) + ": " + filename);
					}

					width = mImage.getWidth() / (resolution / Constants.MM_PER_INCH);
					height = mImage.getHeight() / (resolution / Constants.MM_PER_INCH);
				}
				else
				{
					/*
					 * Create a BufferedImage to draw into.  We'll save it to a file
					 * when user has finished drawing to it.
					 */
					int widthInPixels = (int)Math.round(width / Constants.MM_PER_INCH * resolution);
					int heightInPixels = (int)Math.round(height / Constants.MM_PER_INCH * resolution);
					int imageType;

					/*
					 * Create images with transparency for all formats except
					 * JPEG (which does not support it).
					 */
					if (mFormatName.equals("jpg") || mFormatName.equals("jpeg"))
						imageType = BufferedImage.TYPE_3BYTE_BGR;
					else
						imageType = BufferedImage.TYPE_INT_ARGB;

					mImage = new BufferedImage(widthInPixels, heightInPixels,
						imageType);
				}
			}
			else if (mOutputType == INTERNAL_IMAGE)
			{
				/*
				 * Calculate width of page, based on image and resolution given
				 * by user.
				 */
				width = mImage.getWidth() / (resolution / Constants.MM_PER_INCH);
				height = mImage.getHeight() / (resolution / Constants.MM_PER_INCH);
			}
			mGraphics2D = (Graphics2D)(mImage.getGraphics());
			setupBufferedImage(resolution, backgroundColor, lineAntiAliasing, labelAntiAliasing);
		}
		mFilename = filename;
		mPageWidth = width;
		mPageHeight = height;
		mResolution = Constants.MM_PER_INCH / resolution;
		mFontCache = new FontCache();
		mJustificationShiftX = mJustificationShiftY = 0.0;
		mFontOutlineWidth = 0.0;

		/*
		 * Set impossible current font rotation so first font
		 * accessed will be loaded.
		 */
		mFontRotation = Double.MAX_VALUE;

		/*
		 * Do not allocate page mask until needed to save memory.
		 */
		mPageMask = null;
	}

	/**
	 * Creates new graphics file, ready for drawing to.
	 * @param filename name of image file output will be saved to.
	 * If filename begins with '|' character then output is piped as
	 * input to that command.
	 * @param format is the graphics format to use.
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param extras contains extra settings for this output.
	 * @param stdoutStream standard output stream for program.
	 */
	public OutputFormat(String filename, String format,
		double width, double height, String extras,
		PrintStream stdoutStream)
		throws IOException, MapyrusException
	{
		mFormatName = format.toLowerCase();

		/*
		 * Check that Java can write this image format to a file.
		 */				
		if (mFormatName.equals("ps") ||
			mFormatName.equals("postscript") ||
			mFormatName.equals("application/postscript"))
		{
			mFormatName = "ps";
			mOutputType = POSTSCRIPT_GEOMETRY;
		}
		else if (mFormatName.equals("eps"))
		{
			mOutputType = POSTSCRIPT_GEOMETRY;
		}
		else if (mFormatName.equals("epsimage"))
		{
			mOutputType = POSTSCRIPT_IMAGE;
		}
		else if (mFormatName.equals("screen"))
		{
			mOutputType = SCREEN_WINDOW;
		}
		else
		{
			if (mFormatName.startsWith("image/"))
				mFormatName = mFormatName.substring(6);

			if (!isSupportedImageFormat(mFormatName))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) +
					": " + format);
			}
		
			mOutputType = IMAGE_FILE;
		}
		setOutput(filename, width, height, extras, stdoutStream);
	}

	/**
	 * Sets image for drawing into.
	 * @param image is buffered image to draw into.
	 * @param extras contains extra settings for this output.
	 */
	public OutputFormat(BufferedImage image, String extras)
		throws IOException, MapyrusException
	{
		mOutputType = INTERNAL_IMAGE;
		mImage = image;
		mFormatName = "png";
		PrintStream dummyStdout = new PrintStream(new ByteArrayOutputStream());
		setOutput("", 0, 0, extras, dummyStdout);
	}

	/**
	 * Return page width.
	 * @return width in millimetres.
	 */
	public double getPageWidth()
	{
		return(mPageWidth);
	}
	
	/**
	 * Return page height.
	 * @return height in millimetres.
	 */
	public double getPageHeight()
	{
		return(mPageHeight);
	}

	/**
	 * Return file format of page.
	 * @return file format of page in lowercase.
	 */
	public String getPageFormat()
	{
		return(mFormatName);
	}
	
	/**
	 * Return resolution of page as a distance measurement.
	 * @return distance in millimetres between centres of adjacent pixels.
	 */
	public double getResolution()
	{
		return(mResolution);
	}

	/**
	 * Returns height and width of a string, drawn to current page.
	 * @param s string to calculate width for.
	 * @param fontName name of font to calculate dimensions for.
	 * @param fontSize size of characters in millimetres.
	 * @return height and width of string in millimetres.
	 */
	public StringDimension getStringDimension(String s, String fontName, double fontSize)
		throws IOException, MapyrusException
	{
		StringDimension retval = new StringDimension();
		BufferedReader stringReader = new BufferedReader(new StringReader(s));
		double width = 0, height = 0;
		String token;
		double tokenWidth;

		/*
		 * Break multi-line strings into separate lines so we
		 * can find the width of the longest line.
		 */
		while ((token = stringReader.readLine()) != null)
		{
			if (mOutputType == POSTSCRIPT_GEOMETRY)
			{
				/*
				 * Load Font Metrics information only when it is needed.
				 */
				if (mAdobeFontMetrics == null)
					mAdobeFontMetrics = new AdobeFontMetricsManager(mAfmFiles, mEncodeAsISOLatin1);
	
				double pointSize = fontSize / Constants.MM_PER_INCH * Constants.POINTS_PER_INCH;
				tokenWidth = mAdobeFontMetrics.getStringWidth(fontName, pointSize, token);
				tokenWidth = tokenWidth / Constants.POINTS_PER_INCH * Constants.MM_PER_INCH;
				if (tokenWidth > width)
					width = tokenWidth;
				height += fontSize;
			}
			else
			{
				/*
				 * Use Java2D calculation for bounding box of string displayed with
				 * horizontal font.
				 */
				FontRenderContext frc = mGraphics2D.getFontRenderContext();
				Rectangle2D bounds = mBaseFont.getStringBounds(token, frc);
				tokenWidth = bounds.getWidth();
				if (tokenWidth > width)
					width = tokenWidth;
				height += bounds.getHeight();
			}
		}

		retval.setSize(width, height);
		return(retval);
	}

	/**
	 * Return mask for this page.
	 * @return page mask.
	 */
	public PageMask getPageMask()
	{
		if (mPageMask == null)
		{
			mPageMask = new PageMask((int)Math.round(mPageWidth),
				(int)Math.round(mPageHeight));
		}
		return(mPageMask);
	}

	/*
	 * Write a line to PostScript file.  Line is indented to show
	 * saving and restoring of state more clearly.
	 */
	private void writePostScriptLine(String line)
	{
		mWriter.println(line);
	}

	/**
	 * Write image to PostScript file.
	 * @param image image to write.
	 * @param x center position on page for image.
	 * @param y center position on page for image.
	 * @param width width of image in millimetres.
	 * @param height height of image in millimetres.
	 * @param rotation rotation angle for image.
	 */
	private void writePostScriptImage(BufferedImage image, double x, double y,
		double width, double height, double rotation)
		throws IOException, MapyrusException
	{
		int pixelWidth, pixelHeight;
		int step;

		pixelWidth = image.getWidth();
		pixelHeight = image.getHeight();

		/*
		 * Calculate reduction in image size so that it is 
		 * an appropriate size for the resolution of the page.
		 */
		if (pixelWidth <= 16 || pixelHeight <= 16)
		{
			step = 1;
		}
		else
		{
			double bestPixelWidth = width / mResolution;
			double bestPixelHeight = height / mResolution;

			/*
			 * Keep reducing image 1/2, 1/3, 1/4, ... until it reaches the
			 * resolution of the page.  Use that reduction for image.
			 */
			step = 1;
			while (pixelHeight / (step + 1) > bestPixelHeight &&
				pixelWidth / (step + 1) > bestPixelWidth)
			{
				step++;
			}
		}

		int reducedPixelWidth = (pixelWidth + step - 1) / step;
		int reducedPixelHeight = (pixelHeight + step - 1) / step;

		/*
		 * Check if image is a single color.
		 * Draw single color images with transparent background
		 * using PostScript 'imagemask' operator.
		 * Draw other images as RGB images using 'image' operator.
		 */
		Color singleColor = getSingleImageColor(image);

		/*
		 * Write PostScript image directionary entry to draw image.
		 * Taken from Adobe PostScript Language Reference Manual
		 * (2nd Edition), p. 234.
		 */
		writePostScriptLine("gs");
		writePostScriptLine("/DeviceRGB setcolorspace");

		writePostScriptLine(x + " " + y + " translate");
		writePostScriptLine(rotation + " radtodeg rotate");
		writePostScriptLine(width + " " + height + " scale");

		/*
		 * Image is centred at each point.
		 * Shift image left and down half it's size so that it is displayed centred.
		 */
		writePostScriptLine("-0.5 -0.5 translate");

		/*
		 * Set color for drawing single color images.
		 */
		if (singleColor != null)
		{
			float []c = singleColor.getColorComponents(null);
			writePostScriptLine(c[0] + " " + c[1] + " " + c[2] + " rgb");
		}

		writePostScriptLine("% original image size " + pixelWidth + "x" + pixelHeight + " with reduction factor " + step);
		writePostScriptLine("<<");
		writePostScriptLine("/ImageType 1");
		writePostScriptLine("/Width " + reducedPixelWidth);
		writePostScriptLine("/Height " + reducedPixelHeight);
		if (singleColor != null)
		{
			writePostScriptLine("/BitsPerComponent 1");
			writePostScriptLine("/Decode [0 1]");
		}
		else
		{
			writePostScriptLine("/BitsPerComponent 8");
			writePostScriptLine("/Decode [0 1 0 1 0 1]");
		}
		writePostScriptLine("/ImageMatrix [" + reducedPixelWidth + " 0 0 " +
			-reducedPixelHeight + " 0 " + reducedPixelHeight + "]");
		writePostScriptLine("/DataSource currentfile /ASCII85Decode filter");
		writePostScriptLine(">>");

		if (singleColor != null)
			writePostScriptLine("imagemask");
		else
			writePostScriptLine("image");

		/*
		 * Write ASCII85 encoded string containing all pixel values.
		 */
		ASCII85Writer ascii85 = new ASCII85Writer(mWriter);
		int byteValue = 0;
		int bitCounter = 0;
		for (int row = 0; row < pixelHeight; row += step)
		{
			for (int col = 0; col < pixelWidth; col += step)
			{
				int pixel = image.getRGB(col, row);

				if (singleColor != null)
				{
					/*
					 * Pixel is set in PostScript image if it is transparent.
					 */
					int nextBit = ((pixel >> 24) == 0) ? 1 : 0;

					/*
					 * Store next pixel value as a single bit in a byte.
					 * If we've completed a byte or reached the end of a row
					 * then write byte out and begin next byte.
					 */
					nextBit <<= (7 - bitCounter);
					byteValue |= nextBit;
					bitCounter++;
	
					if (bitCounter == 8 || (col + step >= pixelWidth))
					{
						ascii85.write(byteValue);
						byteValue = bitCounter = 0;
					}
				}
				else
				{
					/*
					 * Ignore transparency, we want only red, green, blue components
					 * of pixel.
					 */
					int blue = (pixel & 0xff);
					int green = ((pixel >> 8) & 0xff);
					int red = ((pixel >> 16) & 0xff);
	
					ascii85.write(red);
					ascii85.write(green);
					ascii85.write(blue);
				}
			}
		}
		ascii85.close();

		/*
		 * Write ASCII85 end-of-data marker.
		 */
		writePostScriptLine("~>");
		writePostScriptLine("gr");
	}

	/**
	 * Save state, protecting color, linestyle, transform of output.
	 * This state can be restored later with restoreState().
	 */
	public void saveState()
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			writePostScriptLine("gs");
		}
	}

	/**
	 * Restore state saved with saveState().
	 * @return true if saved state was successfully restored.
	 * Only PostScript format can be successfully restored, caller
	 * will have to reset values for other formats.
	 */
	public boolean restoreState()
	{
		boolean retval;

		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			writePostScriptLine("gr");
			retval = true;
		}
		else
		{
			/*
			 * Can't restore state when drawing to an image.  Caller
			 * must set everything to correct values again.
			 */
			retval = false;
		}
		return(retval);
	}

	/**
	 * Writes trailing and buffered information, then closes output file.
	 */
	public void closeOutputFormat() throws IOException, MapyrusException
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == POSTSCRIPT_IMAGE)
		{
			if (mOutputType == POSTSCRIPT_IMAGE)
			{
				/*
				 * Write image file containing page.
				 */
				writePostScriptImage(mImage, mPageWidth / 2, mPageHeight / 2,
					mPageWidth, mPageHeight, 0);
			}

			/*
			 * Finish off PostScript file.
			 */
			if (mFormatName.equals("ps"))
			{
				/*
				 * showpage is not included in Encapsulated PostScript files.
				 */
				mWriter.println("showpage");
			}

			mWriter.println("%%Trailer");
			
			/*
			 * Included list of fonts we used in this file but did
			 * not include in the header.
			 */	
			mWriter.println("%%DocumentNeededResources:");
			Iterator it = mNeededFontResources.iterator();
			while (it.hasNext())
			{
				String fontName = (String)(it.next());
				if (!mSuppliedFontResources.contains(fontName))
					mWriter.println("%%+ font " + fontName);
			}
			mWriter.println("%%EOF");

			if (mIsStandardOutput)
				mWriter.flush();
			else
				mWriter.close();

			if (mWriter.checkError())
			{
				throw new MapyrusException(mFilename +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_PS));
			}

			/*
			 * If updating file then replace existing file with completed
			 * temporary file now.
			 */
			if (mTempFile != null)
			{
				if ((!new File(mFilename).delete()) || (!mTempFile.renameTo(new File(mFilename))))
				{
					mTempFile.delete();
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.READ_ONLY) + ": " + mFilename);
				}
			}
		}
		else if (mOutputType == IMAGE_FILE)
		{
			/*
			 * If updating file then overwrite it now with new image.
			 */
			if (mIsUpdatingFile)
				mOutputStream = new FileOutputStream(mFilename);

			/*
			 * Write image buffer to file.
			 */
			ImageIO.write(mImage, mFormatName, mOutputStream);

			if (mIsStandardOutput)
				mOutputStream.flush();
			else
				mOutputStream.close();
		}
		else if (mOutputType == SCREEN_WINDOW)
		{
			/*
			 * Show image we have created in a window.
			 * Then wait for user to close the window.
			 */
			String title = Constants.PROGRAM_NAME + ": " + mFilename;
			MapyrusFrame frame = new MapyrusFrame(title, mImage);
			frame.waitForClose();
		}

		mImage = null;
		mGraphics2D = null;

		if (mOutputType != INTERNAL_IMAGE)
		{
			/*
			 * If we are piping output to another program then wait for
			 * that program to finish.  Then check that it succeeded.
			 */
			if (mIsPipedOutput)
			{
				int retval = 0;

				try
				{
					retval = mOutputProcess.waitFor();
				}
				catch (InterruptedException e)
				{
					throw new MapyrusException(mFilename + ": " + e.getMessage());
				}

				if (retval != 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.PROCESS_ERROR) +
						": " + retval + ": " + mFilename);
				}
			}
		}
	}

	/**
	 * Set font for labelling in output format.
	 * @param fontName is name of font as defined in java.awt.Font class.
	 * @param fontSize is size for labelling in millimetres.
	 * @param fontRotation is rotation angle for font, in degrees,
	 * measured counter-clockwise.
	 * @param outlineWidth if non-zero, labels will drawn as character outlines
	 * with this width.
	 */
	public void setFontAttribute(String fontName, double fontSize, double fontRotation, double outlineWidth)
		throws IOException, MapyrusException
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			if (mEncodeAsISOLatin1.contains(fontName))
			{
				/*
				 * Re-encode font from StandardEncoding to ISOLatin1Encoding
				 * before it is used.
				 */
				writePostScriptLine(isoLatinEncode(fontName));
				mEncodeAsISOLatin1.remove(fontName);
			}

			/*
			 * Set font and size for labelling.
			 */
			writePostScriptLine("/" + fontName + " " +
				fontSize + " " +
				fontRotation + " " +
				outlineWidth + " font");
			mNeededFontResources.add(fontName);
		}
		else
		{
			/*
			 * Split font name into font and style.
			 */
			int style = Font.PLAIN;

			if (fontName.endsWith("-Bold"))
			{
				style = Font.BOLD;
				fontName = fontName.substring(0, fontName.length() - 5);
			}
			else if (fontName.endsWith("-Italic"))
			{
				style = Font.ITALIC;
				fontName = fontName.substring(0, fontName.length() - 7);
			}
			else if (fontName.endsWith("-BoldItalic"))
			{
				style = Font.BOLD|Font.ITALIC;
				fontName = fontName.substring(0, fontName.length() - 11);
			}

			/*
			 * Continually opening and deriving fonts is probably expensive.
			 * Check that new font is actually different to current font
			 * before defining it.
			 */
			Font currentFont = mGraphics2D.getFont();
			int newSize = (int)Math.round(fontSize);
			if (newSize != currentFont.getSize() ||
				style != currentFont.getStyle() ||
				(!fontName.equals(currentFont.getFontName())) ||
				fontRotation != mFontRotation)
			{
				/*
				 * We need a base font that is not rotated for calculating
				 * string widths for justifying text.
				 * Get base font from cache, or create it if we don't find it there.
				 */
				mBaseFont = mFontCache.get(fontName, style, newSize, 0);
				if (mBaseFont == null)
				{
					/*
					 * If this is a font for which user provided a TTF file then
					 * use that, else expect the operating system to be able to
					 * open the font.
					 */
					TrueTypeFont ttf = (TrueTypeFont)mTTFFonts.get(fontName);
					if (ttf != null)
						mBaseFont = ttf.getFont().deriveFont(style, (float)newSize);
					else
						mBaseFont = new Font(fontName, style, newSize);
					mFontCache.put(fontName, style, newSize, 0, mBaseFont);
				}

				/*
				 * The real font used for labelling must be mirrored in Y axis
				 * (to reverse the transform we use on Graphics2D objects) and
				 * rotated to the angle the user wants.
				 *
				 * Look it up in cache too.
				 */
				Font font = mFontCache.get(fontName, style, -newSize, fontRotation);
				if (font == null)
				{
					AffineTransform fontTransform;
					fontTransform = AffineTransform.getRotateInstance(fontRotation);
					fontTransform.scale(1, -1);
					font = mBaseFont.deriveFont(fontTransform);
					mFontCache.put(fontName, style, -newSize, fontRotation, font);
				}

				mGraphics2D.setFont(font);
			}
		}

		/*
		 * Font rotation and outlining not easily held in a Graphics2D
		 * object so keep track of it's current value ourselves.
		 */
		mFontRotation = fontRotation;
		mFontOutlineWidth = outlineWidth;
	}
	
	/**
	 * Set label justification in output format.
	 * @param justify is label justification value, combination of JUSTIFY_* bit flags.
	 */
	public void setJustifyAttribute(int justify)
	{
		/*
		 * Calculate fraction of string height and width to move text to get required
		 * justification.
		 */
		if ((justify & JUSTIFY_LEFT) != 0)
			mJustificationShiftX = 0.0;
		else if ((justify & JUSTIFY_CENTER) != 0)
			mJustificationShiftX = -0.5;
		else
			mJustificationShiftX = -1.0;

		if ((justify & JUSTIFY_BOTTOM) != 0)
			mJustificationShiftY = 0.0;
		else if ((justify & JUSTIFY_MIDDLE) != 0)
			mJustificationShiftY = -0.5;
		else
			mJustificationShiftY = -1.0;

		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Define dictionary entries for justification settings for PostScript
			 * procedure to use for aligning text correctly itself.
			 */
			writePostScriptLine(mJustificationShiftX + " " + mJustificationShiftY + " j");
		}
	}

	/**
	 * Set color in output format.
	 * @param color is color to draw in.
	 */
	public void setColorAttribute(Color color)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			float c[] = color.getRGBColorComponents(null);
			writePostScriptLine(c[0] + " " + c[1] + " " + c[2] + " rgb");
		}
		else
		{
			mGraphics2D.setColor(color);
		}
	}

	/**
	 * Set linestyle in output format.
	 * @param linestyle is Java2D line width, cap and join style, dash pattern.
	 */
	public void setLinestyleAttribute(BasicStroke linestyle)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Convert BasicStroke end cap and line join values to PostScript.
			 */
			int cap = linestyle.getEndCap();
			if (cap == BasicStroke.CAP_BUTT)
				cap = 0;
			else if (cap == BasicStroke.CAP_ROUND)
				cap = 1;
			else /* SQUARE */
				cap = 2;

			int join = linestyle.getLineJoin();
			if (join == BasicStroke.JOIN_MITER)
				join = 0;
			else if (join == BasicStroke.JOIN_ROUND)
				join = 1;
			else /* BEVEL */
				join = 2;

			writePostScriptLine(linestyle.getLineWidth() + " " +
				cap + " " + join + " " +
				linestyle.getMiterLimit() + " sl");

			/*
			 * If there a dash pattern then set that too.
			 */
			float dashes[] = linestyle.getDashArray();
			if (dashes != null)
			{
				StringBuffer s = new StringBuffer("[");
				for (int i = 0; i < dashes.length; i++)
				{
					if (i > 0)
						s.append(" ");
					s.append(dashes[i]);
				}
				s.append("] ");
				s.append(linestyle.getDashPhase());
				s.append(" setdash");
				writePostScriptLine(s.toString());
			}
			else
			{
				/*
				 * Remove any dashed line previously defined.
				 */
				writePostScriptLine("[] 0 setdash");
			}
		}
		else
		{
			mGraphics2D.setStroke(linestyle);
		}
	}

	/**
	 * Set clip path for output format.
	 * @param clipPaths are polygons to clip against, or null if there are no clip polygons.
	 */
	public void setClipAttribute(ArrayList clipPaths)
	{
		if (mOutputType != POSTSCRIPT_GEOMETRY)
		{
			mGraphics2D.setClip(null);
			if (clipPaths != null)
			{
				for (int i = 0; i < clipPaths.size(); i++)
				{
					GeometricPath clipPath = (GeometricPath)(clipPaths.get(i));
					mGraphics2D.clip(clipPath.getShape());
				}
			}
		}
	}

	/*
	 * Walk through path, converting it to PostScript.
	 */	
	private void writePostScriptShape(Shape shape)
	{
		PathIterator pi = shape.getPathIterator(null);
		float coords[] = new float[6];
		float lastX = 0.0f, lastY = 0.0f;
		float x = 0.0f, y = 0.0f;
		float distSquared;
		float resolutionSquared = (float)(mResolution * mResolution);
		int segmentType;
		boolean skippedLastSegment = false;

		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			switch (segmentType)
			{
				case PathIterator.SEG_MOVETO:
					lastX = coords[0];
					lastY = coords[1];
					writePostScriptLine(lastX + " " + lastY + " m");
					skippedLastSegment = false;
					break;

				case PathIterator.SEG_LINETO:
					x = coords[0];
					y = coords[1];
					distSquared = (lastX - x) * (lastX - x) + (lastY - y) * (lastY - y);
					if (distSquared >= resolutionSquared)
					{
						lastX = x;
						lastY = y;
						writePostScriptLine(lastX + " " + lastY + " l");
						skippedLastSegment = false;
					}
					else
					{
						/*
						 * Skip segments that are less than one unit of resolution in length.
						 */
						skippedLastSegment = true;
					}
					break;

				case PathIterator.SEG_CLOSE:
					if (skippedLastSegment)
						writePostScriptLine(x + " " + y + " l");
					writePostScriptLine("closepath");
					skippedLastSegment = false;
					break;

				case PathIterator.SEG_CUBICTO:
					writePostScriptLine(coords[0] + " " +
						coords[1] + " " +
						coords[2] + " " +
						coords[3] + " " +
						coords[4] + " " +
						coords[5] + " " +
						"curveto");
					lastX = coords[4];
					lastY = coords[5];
					skippedLastSegment = false;
					break;
			}
			pi.next();			
		}

		if (skippedLastSegment)
		{
			/*
			 * Always include last point in lines and polygons,
			 * never skip it.
			 */
			writePostScriptLine(x + " " + y + " l");
		}
	}

	/**
	 * Determines single color used in an image.
	 * @param image image to check.
	 * @return single non-transparent color used in an image, or null if
	 * image has many colors.
	 */
	private Color getSingleImageColor(BufferedImage image)
	{
		Color singleColor = Color.BLACK;
		boolean foundDifferentColors = false;
		boolean foundFirstColor = false;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		/*
		 * Check if all pixels are the same color, or transparent.
		 */
		int y = 0;
		while (y < imageHeight && (!foundDifferentColors))
		{
			int x = 0;
			while (x < imageWidth && (!foundDifferentColors))
			{
				int pixel = image.getRGB(x, y);
				if ((pixel & 0xff000000) != 0)
				{
					/*
					 * Pixel is not transparent.
					 */
					if (!foundFirstColor)
					{
						foundFirstColor = true;
						singleColor = new Color(pixel & 0xffffff);
					}
					else
					{
						foundDifferentColors = (pixel != singleColor.getRGB());
					}
				}
				x++;
			}
			y++;
		}

		if (foundDifferentColors)
			singleColor = null;
		return(singleColor);
	}

	/**
	 * Draw icon at points on page.
	 * @param pointList is list of Point2D objects at which to draw icon.
	 * @param icon image to draw.
	 * @param size is size of icon in millimeters, or zero for screen size.
	 * @param rotation rotation angle for icon.
	 * @param scaling scale factor for icon.
	 */
	public void drawIcon(ArrayList pointList, BufferedImage image, double size,
		double rotation, double scaling)
		throws IOException, MapyrusException
	{
		int pixelWidth = image.getWidth();
		int pixelHeight = image.getHeight();
		Point2D pt;
		int i;
		double x, y, mmWidth, mmHeight;

		/*
		 * If size not given then make icon about as large as it would appear
		 * on the screen in an image viewer, with one image pixel in one screen
		 * pixel.
		 */
		if (size <= 0.0)
		{
			size = Math.max(pixelWidth, pixelHeight) * (Constants.MM_PER_INCH /
				Constants.getScreenResolution());
		}
		size *= scaling;

		/*
		 * Calculate width and height for non-square images.
		 */
		if (pixelWidth > pixelHeight)
		{
			mmWidth = size;
			mmHeight = size * ((double)pixelHeight / pixelWidth);
		}
		else
		{
			mmHeight = size;
			mmWidth = size * ((double)pixelWidth / pixelHeight);
		}

		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Draw icon at each position in list.
			 */
			for (i = 0; i < pointList.size(); i++)
			{
				pt = (Point2D)(pointList.get(i));
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + mmWidth >= 0 && x - mmWidth <= mPageWidth &&
					y + mmHeight >= 0.0 && y - mmHeight <= mPageHeight)
				{
					writePostScriptImage(image, x, y, mmWidth, mmHeight, rotation);
				}
			}
		}
		else
		{
			for (i = 0; i < pointList.size(); i++)
			{
				pt = (Point2D)(pointList.get(i));
				x = pt.getX();
				y = pt.getY();
				AffineTransform affine = AffineTransform.getTranslateInstance(x, y);

				/*
				 * Scale transformation so that units are in pixels.
				 */
				double mmPerPixel = Constants.MM_PER_INCH / Constants.getScreenResolution();
				affine.scale(mmPerPixel, mmPerPixel * -1);

				/*
				 * Rotate clockwise around point (x, y).
				 */
				affine.rotate(-rotation);

				/*
				 * Scale image to requested size.
				 */
				double xScale = (mmWidth / mmPerPixel) / pixelWidth;
				double yScale = (mmHeight / mmPerPixel) / pixelHeight;
				affine.scale(xScale, yScale);

				/*
				 * Shift origin so that middle of image is at point (x, y).
				 */
				affine.translate(-pixelWidth / 2.0, -pixelHeight / 2.0);

				try
				{
					/*
					 * Sun JVM throws NullPointerException if image is
					 * too big to fit in memory.
					 */
					mGraphics2D.drawImage(image, affine, null);
				}
				catch (NullPointerException e)
				{
					throw new OutOfMemoryError("Failed loading icon.");
				}
			}
		}
	}

	/**
	 * Draw currently defined path to output page.
	 */
	public void stroke(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				writePostScriptShape(shape);
				writePostScriptLine("s");
			}
		}
		else
		{
			/*
			 * Draw path into image.
			 */
			mGraphics2D.draw(shape);
		}
	}

	/**
	 * Fill currently defined path on output page.
	 */
	public void fill(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				writePostScriptShape(shape);
				writePostScriptLine("f");
			}
		}
		else
		{
			/*
			 * Fill path in image.
			 */
			mGraphics2D.fill(shape);
		}
	}

	/**
	 * Set clip region to inside of currently defined path on output page.
	 */
	public void clip(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Set clip path now, then it stays in effect until previous
			 * state is restored.
			 */
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				writePostScriptShape(shape);
			}
			else
			{
				/*
				 * Clip region is outside page.  Clip to simple rectangle
				 * outside page instead so that nothing is shown.
				 */
				writePostScriptShape(new Rectangle2D.Float(-1.0f, -1.0f, 0.1f, 0.1f));
			}
			writePostScriptLine("clip newpath");
		}
	}

	/**
	 * Convert a string to PostScript format, escaping special characters and
	 * write it to PostScript file.
	 * @param s is string to convert and write.
	 */
	private void writePostScriptString(String s)
	{
		char c;
		StringBuffer buffer = new StringBuffer("(");
		for (int i = 0; i < s.length(); i++)
		{
			/*
			 * Wrap strings that get too long.
			 */
			if (buffer.length() > 72)
			{
				buffer.append('\\');
				mWriter.println(buffer.toString());
				buffer.setLength(0);
			}

			/*
			 * Convert backslashes to '\\' and other special characters to octal code.
			 */
			c = s.charAt(i);
			if (c == '\\')
			{
				buffer.append("\\\\");
			}
			else if (c == '(' || c == ')' || c == '%' || c < ' ' || c > 'z')
			{
				int extendedChar = c;
				int b1 = extendedChar / (8 * 8);
				extendedChar -= b1 * (8 * 8);
				int b2 = extendedChar / 8;
				extendedChar -= b2 * 8;
				int b3 = extendedChar;
				buffer.append('\\');
				buffer.append(b1);
				buffer.append(b2);
				buffer.append(b3);
			}
			else
			{
				buffer.append(c);
			}
		}
		buffer.append(")");
		mWriter.println(buffer.toString());
	}

	/**
	 * Draw label positioned at (or along) currently defined path.
	 * @param pointList is list of Point2D objects at which to draw label.
	 * @param label is string to draw on path.
	 */
	public void label(ArrayList pointList, String label)
	{
		Point2D pt, startPt;
		double x, y;
		String nextLine;
		StringTokenizer st;
		int lineNumber;
		AffineTransform affine;
		FontRenderContext frc = null;
		Stroke originalStroke = null;

		if (mOutputType != POSTSCRIPT_GEOMETRY)
		{
			frc = mGraphics2D.getFontRenderContext();
			
			if (mFontOutlineWidth > 0)
			{
				/*
				 * Save existing linestyle and create new one for drawing outlines of each letter.
				 */
				originalStroke = mGraphics2D.getStroke();
				BasicStroke outlineStroke = new BasicStroke((float)mFontOutlineWidth,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f);
				mGraphics2D.setStroke(outlineStroke);
			}
		}

		/*
		 * Draw label at each position in list.
		 */
		for (int i = 0; i < pointList.size(); i++)
		{
			pt = (Point2D)(pointList.get(i));
			x = pt.getX();
			y = pt.getY();

			/*
			 * Draw each line of label below the one above.
			 */
			st = new StringTokenizer(label, Constants.LINE_SEPARATOR);
			lineNumber = 0;
			while (st.hasMoreTokens())
			{
				nextLine = st.nextToken();

				if (mOutputType == POSTSCRIPT_GEOMETRY)
				{
					writePostScriptLine(x + " " + y + " m");

					/*
					 * Pass counter and line to PostScript procedure for
					 * drawing each line of the label.
					 */
					writePostScriptLine(Integer.toString(lineNumber));
					writePostScriptString(nextLine);
					writePostScriptLine("t");
				}
				else
				{
					/*
					 * Reposition label from original point so it has correct justification.
					 */
					if (mJustificationShiftX != 0.0 || mJustificationShiftY != 0.0 || lineNumber > 0)
					{
						Rectangle2D bounds = mBaseFont.getStringBounds(nextLine, frc);
						affine = AffineTransform.getTranslateInstance(x, y);
						affine.rotate(mFontRotation);

	   					startPt = new Point2D.Double(bounds.getWidth() * mJustificationShiftX,
	   						bounds.getHeight() * (mJustificationShiftY - lineNumber));
	   					affine.transform(startPt, startPt);
					}
					else
					{
						startPt = pt;
					}
					
					float fx = (float)startPt.getX();
					float fy = (float)startPt.getY();
					
					if (mFontOutlineWidth > 0)
					{
						/*
						 * Draw only outline of letters in label as lines.
						 */
						GlyphVector glyphs = mGraphics2D.getFont().createGlyphVector(frc, nextLine);
						Shape outline = glyphs.getOutline(fx, fy);						
						mGraphics2D.draw(outline);
						
					}
					else
					{
						/*
						 * Draw plain label.
						 */
						mGraphics2D.drawString(nextLine, fx, fy);
					}
				}
				lineNumber++;
			}
		}

		if (originalStroke != null)
		{
			/*
			 * Restore line style.
			 */
			mGraphics2D.setStroke(originalStroke);
		}
	}
}
