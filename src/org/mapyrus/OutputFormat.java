/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package org.mapyrus;
 
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import javax.imageio.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.awt.image.*;
import java.awt.Color;

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
	private static final int IMAGE_FILE = 2;
	private static final int POSTSCRIPT = 3;

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
	 * Format for coordinates, colors and rotation in PostScript files.
	 */	
	private DecimalFormat mLinearFormat;
	private DecimalFormat mColorFormat;
	private DecimalFormat mRotationFormat;
		
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
	private Process mOutputProcess;
	
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
	 * Indentation for PostScript commands.
	 */
	private int mPostScriptIndent;

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
	private Font mBaseFont;
	
	/**
	 * Write PostScript file header, including document structuring conventions (DSC).
	 * @param width width of page in mm.
	 * @param height height of page in mm.
	 * @param fontList list of PostScript fonts to include in header.
	 */
	private void writePostScriptHeader(double width, double height, ArrayList fontList)
		throws IOException, MapyrusException
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);

		mWriter.print("%!PS-Adobe-3.0");
		if (mFormatName.equals("eps"))
			mWriter.print(" EPSF-3.0");
		mWriter.println("");

		mWriter.println("%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);
		mWriter.println("%%DocumentData: Clean7Bit");
		mWriter.println("%%LanguageLevel: 2");
		mWriter.println("%%Creator: " + Constants.PROGRAM_NAME);
		Date now = new Date();
		mWriter.println("%%CreationDate: " + now.toString());
		String username = System.getProperty("user.name");
		if (username != null)
			mWriter.println("%%For: " + username);

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

		/*
		 * Prevent anything being displayed outside bounding box we've just defined.
		 */
		mWriter.println("0 0 " + widthInPoints + " " + heightInPoints + " rectclip");

		/*
		 * Set plotting units to millimetres.
		 */
		mWriter.println(Constants.POINTS_PER_INCH + " " + Constants.MM_PER_INCH +
			" div dup scale");

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
		mWriter.println("/font { /frot exch radtodeg def");
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
		mWriter.println("2 index neg fjy add fsize mul");
		mWriter.println("rmoveto show grestore newpath } bind def");

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
	 * Sets correct background, rendering hints and transformation
	 * for buffered image we will plot to.
	 */
	private void setupBufferedImage(double resolution)
	{
		double scale;
	
		mGraphics2D.setColor(Color.WHITE);
		mGraphics2D.fillRect(0, 0, mImage.getWidth(), mImage.getHeight());
		
		scale = resolution / Constants.MM_PER_INCH;
		
		/*
		 * Set transform with origin in lower-left corner and
		 * Y axis increasing upwards.
		 */
		mGraphics2D.translate(0, mImage.getHeight());
		mGraphics2D.scale(scale, -scale);
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

	/**
	 * Creates new graphics file, ready for drawing to.
	 * @param filename name of image file output will be saved to.
	 * If filename begins with '|' character then output is piped as
	 * input to that command.
	 * @param format is the graphics format to use.
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param resolution is resolution for output in dots per inch (DPI)
	 * @param extras contains extra settings for this output.
	 * @param stdoutStream standard output stream for program.
	 */
	public OutputFormat(String filename, String format,
		double width, double height, double resolution, String extras,
		PrintStream stdoutStream)
		throws IOException, MapyrusException
	{
		mFormatName = format.toLowerCase();

		/*
		 * Check that Java can write this image format to a file.
		 */				
		if (mFormatName.equals("ps") || mFormatName.equals("eps"))
		{
			mOutputType = POSTSCRIPT;
		}	
		else
		{
			if (!isSupportedImageFormat(mFormatName))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OUTPUT) +
					": " + format);
			}
		
			mOutputType = IMAGE_FILE;
		}

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

		/*
		 * Setup file we are writing to.
		 */
		if (mOutputType == POSTSCRIPT)
		{
			mLinearFormat = new DecimalFormat("#.##");
			mColorFormat = new DecimalFormat("#.###");
			mRotationFormat = new DecimalFormat("#.####");
			mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)));
			
			mSuppliedFontResources = new HashSet();
			ArrayList fontList = new ArrayList();
			StringTokenizer st = new StringTokenizer(extras);
			mEncodeAsISOLatin1 = new HashSet();
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
							fontList.add(new PostScriptFont(pfaFilename));
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
			}
			writePostScriptHeader(width, height, fontList);

			mPostScriptIndent = 0;
			mNeededFontResources = new HashSet();
		}
		else
		{
			/*
			 * Create a BufferedImage to draw into.  We'll save it to a file
			 * when user has finished drawing to it.
			 */
			int widthInPixels = (int)Math.round(width / Constants.MM_PER_INCH * resolution);
			int heightInPixels = (int)Math.round(height / Constants.MM_PER_INCH * resolution);
			mImage = new BufferedImage(widthInPixels, heightInPixels,
				BufferedImage.TYPE_3BYTE_BGR);
			mGraphics2D = (Graphics2D)(mImage.getGraphics());
			setupBufferedImage(resolution);
			
			mTTFFonts = new HashMap();
			StringTokenizer st = new StringTokenizer(extras);
			while (st.hasMoreTokens())
			{
				String token = st.nextToken();
				if (token.startsWith("ttffiles="))
				{
					/*
					 * Build list of TrueType font filenames user wants
					 * to open with Java methods.
					 */
					StringTokenizer st2 = new StringTokenizer(token.substring(9), ",");
					while (st2.hasMoreTokens())
					{
						String ttfFilename = st2.nextToken();
						if (ttfFilename.length() > 0)
						{
							TrueTypeFont ttf = new TrueTypeFont(ttfFilename);
							mTTFFonts.put(ttf.getName(), ttf);
						}
					}
				}
			}
		}
		mFilename = filename;
		mPageWidth = width;
		mPageHeight = height;
		mResolution = Constants.MM_PER_INCH / resolution;
		mFontCache = new FontCache();
		mJustificationShiftX = mJustificationShiftY = 0.0;

		/*
		 * Set impossible current font rotation so first font
		 * accessed will be loaded.
		 */
		mFontRotation = Double.MAX_VALUE;
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

	/*
	 * Write a line to PostScript file.  Line is indented to show
	 * saving and restoring of state more clearly.
	 */
	private void writePostScriptLine(String line)
	{
		for (int i = 0; i < mPostScriptIndent; i++)
		{
			mWriter.print(' ');
		}
		mWriter.println(line);
	}
	
	/**
	 * Save state, protecting color, linestyle, transform of output.
	 * This state can be restored later with restoreState().
	 */
	public void saveState()
	{
		if (mOutputType == POSTSCRIPT)
		{
			writePostScriptLine("gs");
			mPostScriptIndent++;
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

		if (mOutputType == POSTSCRIPT)
		{
			mPostScriptIndent--;
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
		if (mOutputType == POSTSCRIPT)
		{
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
		}
		else if (mOutputType == IMAGE_FILE)
		{
			/*
			 * Write image buffer to file.
			 */
			ImageIO.write(mImage, mFormatName, mOutputStream);

			if (mIsStandardOutput)
				mOutputStream.flush();
			else
				mOutputStream.close();
			mImage = null;
			mGraphics2D = null;
		}
		
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
					": " + mFilename);
			}
		}
	}

	/**
	 * Set font for labelling in output format.
	 * @param fontName is name of font as defined in java.awt.Font class.
	 * @param fontSize is size for labelling in millimetres.
	 * @param fontRotation is rotation angle for font, in degrees,
	 * measured counter-clockwise.
	 */
	public void setFontAttribute(String fontName, double fontSize, double fontRotation)
		throws IOException, MapyrusException
	{
		if (mOutputType == POSTSCRIPT)
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
				mLinearFormat.format(fontSize) + " " +
				mRotationFormat.format(fontRotation) + " font");
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
		 * Font rotation not easily held in a Graphics2D object so keep
		 * track of it's current value ourselves.
		 */
		mFontRotation = fontRotation;
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

		if (mOutputType == POSTSCRIPT)
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
		if (mOutputType == POSTSCRIPT)
		{
			float c[] = color.getRGBColorComponents(null);
			writePostScriptLine(mColorFormat.format(c[0]) + " " +
				mColorFormat.format(c[1]) + " " +
				mColorFormat.format(c[2]) + " rgb");
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
		if (mOutputType == POSTSCRIPT)
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

			writePostScriptLine(mLinearFormat.format(linestyle.getLineWidth()) + " " +
				cap + " " + join + " " +
				mLinearFormat.format(linestyle.getMiterLimit()) + " sl");

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
					s.append(mLinearFormat.format(dashes[i]));
				}
				s.append("] ");
				s.append(mLinearFormat.format(linestyle.getDashPhase()));
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
		if (mOutputType != POSTSCRIPT)
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
		int segmentType;
		
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			switch (segmentType)
			{
				case PathIterator.SEG_MOVETO:
					writePostScriptLine(mLinearFormat.format(coords[0]) + " " +
						mLinearFormat.format(coords[1]) + " m");
					break;
					
				case PathIterator.SEG_LINETO:
					writePostScriptLine(mLinearFormat.format(coords[0]) + " " +
						mLinearFormat.format(coords[1]) + " l");
					break;
				
				case PathIterator.SEG_CLOSE:
					writePostScriptLine("closepath");
					break;
					
				case PathIterator.SEG_CUBICTO:
					writePostScriptLine(mLinearFormat.format(coords[0]) + " " +
						mLinearFormat.format(coords[1]) + " " +
						mLinearFormat.format(coords[2]) + " " +
						mLinearFormat.format(coords[3]) + " " +
						mLinearFormat.format(coords[4]) + " " +
						mLinearFormat.format(coords[5]) + " " +
						"curveto");
					break;
			}
			pi.next();			
		}
	}
		
	/**
	 * Draw currently defined path to output page.
	 */
	public void stroke(Shape shape)
	{
		if (mOutputType == POSTSCRIPT)
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
		if (mOutputType == POSTSCRIPT)
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
		if (mOutputType == POSTSCRIPT)
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
		
		if (mOutputType != POSTSCRIPT)
			frc = mGraphics2D.getFontRenderContext();

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

				if (mOutputType == POSTSCRIPT)
				{
					writePostScriptLine(mLinearFormat.format(x) + " " +
						mLinearFormat.format(y) + " m");

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
					mGraphics2D.drawString(nextLine, (float)(startPt.getX()),
						(float)(startPt.getY()));
				}
				lineNumber++;
			}
		}
	}
}
