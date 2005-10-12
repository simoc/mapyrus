/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import org.mapyrus.font.AdobeFontMetricsManager;
import org.mapyrus.font.PostScriptFont;
import org.mapyrus.font.StringDimension;
import org.mapyrus.font.TrueTypeFont;
import org.mapyrus.image.ImageIOWrapper;
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
	 * Portable Documnet Format output.
	 */
	private static final int PDF = 6;

	/*
	 * Scalable Vector Graphics (SVG) output.
	 */
	private static final int SVG = 7;

	/*
	 * HTML image map being created in combination with an image.
	 */
	private static final int IMAGEMAP = 8;

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
	 * List of fonts that are always available in PDF file.
	 */
	private static final String []PDF_FONTS =
	{
		"Courier", "Courier-Bold", "Courier-BoldOblique", "Courier-Oblique",
		"Helvetica", "Helvetica-Bold", "Helvetica-BoldOblique", "Helvetica-Oblique",
		"Symbol",
		"Times-Bold", "Times-BoldItalic", "Times-Italic", "Times-Roman",
		"ZapfDingbats"
	};

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
	private PrintWriter mImageMapWriter;
	
	/*
	 * Frequently used fonts.
	 */
	private FontCache mFontCache;

	/*
	 * List of font definitions included in this PostScript file and list of fonts
	 * used in this file but not defined.
	 */
	private HashSet mSuppliedFontResources;
	private LinkedList mNeededFontResources;

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
	 * Selected font and size.
	 */
	private String mFontName;
	private double mFontSize;

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

	/*
	 * Counts clip paths and gradient fills set for SVG output
	 * so each clip path and gradient can be given a unique id.
	 */
	private int mClipPathCounter;
	private boolean mIsClipPathActive;
	private int mGradientCounter;

	/*
	 * File offset of each object in PDF file and buffers containing
	 * all geometry and images to be included in PDF file. 
	 */
	private ArrayList mPDFFileOffsets;
	private StringWriter mPDFGeometryStringWriter;
	private PrintWriter mPDFGeometryWriter;
	private HashMap mPDFImageObjects;

	/*
	 * Format for writing coordinate values.
	 */
	private DecimalFormat mCoordinateDecimal = new DecimalFormat("#.###",
			Constants.US_DECIMAL_FORMAT_SYMBOLS);

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

		StringBuffer sb = new StringBuffer("%!PS-Adobe-3.0");
		if (mFormatName.equals("eps") || mFormatName.equals("epsimage"))
			sb.append(" EPSF-3.0");
		writeLine(mWriter, sb.toString());

		if (turnPage)
			writeLine(mWriter, "%%BoundingBox: 0 0 " + heightInPoints + " " + widthInPoints);
		else
			writeLine(mWriter, "%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);

		if ((!mFormatName.equals("eps")) && (!mFormatName.equals("epsimage")))
			writeLine(mWriter, "%%Pages: 1");

		writeLine(mWriter, "%%DocumentData: Clean7Bit");
		writeLine(mWriter, "%%LanguageLevel: 2");
		writeLine(mWriter, "%%Creator: (" + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + ")");
		writeLine(mWriter, "%%OperatorMessage: (Mapyrus Output...)");
		Date now = new Date();
		writeLine(mWriter, "%%CreationDate: (" + now.toString() + ")");
		String username = System.getProperty("user.name");
		if (username != null)
			writeLine(mWriter, "%%For: (" + username + ")");

		/*
		 * List fonts included in this PostScript file.
		 */
		writeLine(mWriter, "%%DocumentRequiredResources: (atend)");
		if (fontList.size() > 0)
		{
			sb = new StringBuffer("%%DocumentSuppliedResources: font");
			Iterator it = fontList.iterator();
			while (it.hasNext())
			{
				PostScriptFont psFont = (PostScriptFont)(it.next());
				sb.append(" ").append(psFont.getName());
				mSuppliedFontResources.add(psFont.getName());
			}
			writeLine(mWriter, sb.toString());
		}
		writeLine(mWriter, "%%EndComments");
		writeLine(mWriter, "");
		writeLine(mWriter, "% Resolution " + resolution + " DPI");

		/*
		 * Inline font definitions.
		 */
		writeLine(mWriter, "%%BeginSetup");
		Iterator it = fontList.iterator();
		while (it.hasNext())
		{
			PostScriptFont psFont = (PostScriptFont)(it.next());

			writeLine(mWriter, "%%BeginResource: font " + psFont.getName());
			String fontDefinition = psFont.getFontDefinition();
			writeLine(mWriter, fontDefinition);
			writeLine(mWriter, "%%EndResource");			
		}
		writeLine(mWriter, "%%EndSetup");

		/*
		 * Set color and linestyle to reasonable default values.
		 * Taken from 'initgraphics' operator example in PostScript Language
		 * Reference Manual.
		 */
		writeLine(mWriter, "1 setlinewidth 0 setlinecap 0 setlinejoin");
		writeLine(mWriter, "[] 0 setdash 0 setgray 10 setmiterlimit");

		if (turnPage)
		{
			/*
			 * Turn page 90 degrees so that a landscape orientation page appears
			 * on a portrait page.
			 */
			writeLine(mWriter, "% Turn page 90 degrees.");
			writeLine(mWriter, "90 rotate 0 " + heightInPoints + " neg translate");
		}

		/* 
		 * Prevent anything being displayed outside bounding box we've just defined.
		 */
		writeLine(mWriter, "0 0 " + widthInPoints + " " + heightInPoints + " rectclip");

		/*
		 * Set background color for page.
		 */
		writeLine(mWriter, "/RG { setrgbcolor } bind def");
		if (backgroundColor != null)
		{
			float c[] = backgroundColor.getRGBColorComponents(null);
			writeLine(mWriter, "gsave");
			writeLine(mWriter, mCoordinateDecimal.format(c[0]) + " " +
				mCoordinateDecimal.format(c[1]) + " " +
				mCoordinateDecimal.format(c[2]) + " RG");
			writeLine(mWriter, "0 0 " + widthInPoints + " " + heightInPoints + " rectfill");
			writeLine(mWriter, "grestore");
		}

		/*
		 * Define shorter names for most commonly used operations.
		 * Bind all operators names to improve performance (see 3.11 of
		 * PostScript Language Reference Manual).
		 */
		writeLine(mWriter, "/m { moveto } bind def /l { lineto } bind def");
		writeLine(mWriter, "/c { curveto } bind def /h { closepath } bind def");
		writeLine(mWriter, "/S { stroke } bind def /f { fill } bind def");
		writeLine(mWriter, "/W { clip } bind def /n { newpath } bind def");
		writeLine(mWriter, "/ju { /fjy exch def /fjx exch def } bind def");

		/*
		 * Define font and dictionary entries for font size and justification.
		 * Don't bind these as font loading operators may be overridden in interpreter.
		 */
		writeLine(mWriter, "/font {");
		writeLine(mWriter, "/foutline exch def");
		writeLine(mWriter, "/frot exch radtodeg def");
		writeLine(mWriter, "/fsize exch def findfont fsize scalefont setfont } def");
		writeLine(mWriter, "/radtodeg { 180 mul 3.1415629 div } bind def");

		/*
		 * Draw text string, after setting correct position, rotation,
		 * justifying it horizontally and vertically for current font size
		 * and shifting it down a number of lines if it is part of a multi-line
		 * string.
		 *
		 * Line number (starting at 0) and string to show are passed to this procedure.
		 */
		writeLine(mWriter, "/t { gsave currentpoint translate frot rotate");
		writeLine(mWriter, "dup stringwidth pop fjx mul");
		writeLine(mWriter, "3 -1 roll neg fjy add fsize mul");
		writeLine(mWriter, "rmoveto foutline 0 gt");
		writeLine(mWriter, "{false charpath foutline w 0 j 0 J 2 M stroke} {show} ifelse");
		writeLine(mWriter, "grestore newpath } bind def");

		writeLine(mWriter, "/w { setlinewidth } bind def");
		writeLine(mWriter, "/J { setlinecap } bind def");
		writeLine(mWriter, "/j { setlinejoin } bind def");
		writeLine(mWriter, "/M { setmiterlimit } bind def");
		writeLine(mWriter, "/d { setdash } bind def");

		/*
		 * Use new dictionary in saved state so that variables we define
		 * do not overwrite variables in parent state.
		 */
		writeLine(mWriter, "/q { gsave 4 dict begin } bind def");
		writeLine(mWriter, "/Q { end grestore } bind def");
		writeLine(mWriter, "");
	}

	/**
	 * Write PDF file header.
	 * @param filename name of PDF file being created.
	 * @param width width of page in mm.
	 * @param height height of page in mm.
	 * @param resolution resolution of page in DPI.
	 * @param turnPage flag true when page is to be rotated 90 degrees.
	 * @param fontList list of PostScript fonts to include in header.
	 * @param backgroundColor background color for page, or null if no background.
	 */
	private void writePDFHeader(String filename, double width, double height,
		int resolution, boolean turnPage, ArrayList fontList, Color backgroundColor)
		throws IOException, MapyrusException
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *	Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH * Constants.POINTS_PER_INCH);

		mPDFFileOffsets = new ArrayList();

		int nChars = writeLine(mWriter, "%PDF-1.4");

		mPDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(mWriter, "1 0 obj % Document Catalog");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "/Type /Catalog");
		nChars += writeLine(mWriter, "/Outlines 3 0 R");
		nChars += writeLine(mWriter, "/Pages 4 0 R");
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "endobj");

		mPDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(mWriter, "2 0 obj % Document Metadata");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "/Creator (" + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + ")");
		String author = System.getProperty("user.name");
		if (author != null)
			nChars += writeLine(mWriter, "/Author (" + author + ")");
		
		StringBuffer date = new StringBuffer("D:");
		date.append(new SimpleDateFormat("yyyyMMddHHmmssZZZZZ").format(new Date()));
		date.insert(date.length() - 2, '\'');
		date.append('\'');
		nChars += writeLine(mWriter, "/CreationDate (" + date.toString() + ")");
		nChars += writePostScriptString(mWriter, "/Title", filename);
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "endobj");

		mPDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(mWriter, "3 0 obj % Document");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "/Type /Outlines");
		nChars += writeLine(mWriter, "/Count 0");
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "endobj");
		mWriter.flush();

		mPDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(mWriter, "4 0 obj % Page Tree Node");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "/Type /Pages");
		nChars += writeLine(mWriter, "/Kids [5 0 R]");
		nChars += writeLine(mWriter, "/Count 1");
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "endobj");

		mPDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(mWriter, "5 0 obj % Single Page");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "/Type /Page");
		nChars += writeLine(mWriter, "/Parent 4 0 R");
		String mediaBox;
		if (turnPage)
			mediaBox = "/MediaBox [0 0 " + heightInPoints + " " + widthInPoints + "]";
		else
			mediaBox = "/MediaBox [0 0 " + widthInPoints + " " + heightInPoints + "]";
		nChars += writeLine(mWriter, mediaBox);
		nChars += writeLine(mWriter, "/Resources");
		nChars += writeLine(mWriter, "<<");
		nChars += writeLine(mWriter, "  /ProcSet [/PDF /Text /ImageB /ImageC]");
		nChars += writeLine(mWriter, "  /XObject 7 0 R");
		nChars += writeLine(mWriter, "  /Font");
		nChars += writeLine(mWriter, "  <<");
		for (int i = 0; i < PDF_FONTS.length; i++)
		{
			nChars += writeLine(mWriter, "    /F" + i +
				" << /Type /Font /Subtype /Type1");
			nChars += writeLine(mWriter, "      /BaseFont /" + PDF_FONTS[i] +
				" /Name /F" + i);
			if (!PDF_FONTS[i].equals("ZapfDingbats"))
				nChars += writeLine(mWriter, "    /Encoding /WinAnsiEncoding");
			nChars += writeLine(mWriter, "    >>");
		}
		nChars += writeLine(mWriter, "  >>");
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "/Contents 6 0 R % Page Drawing Stream");
		nChars += writeLine(mWriter, ">>");
		nChars += writeLine(mWriter, "endobj");

		mPDFFileOffsets.add(new Integer(nChars));

		mPDFImageObjects = new HashMap();
		mPDFGeometryStringWriter = new StringWriter();
		mPDFGeometryWriter = new PrintWriter(mPDFGeometryStringWriter);

		if (turnPage)
		{
			/*
			 * Turn page 90 degrees so that a landscape orientation page appears
			 * on a portrait page.
			 */
			writeLine(mPDFGeometryWriter, "0 1 -1 0 0 0 cm");
			writeLine(mPDFGeometryWriter, "1 0 0 1 0 " + (-heightInPoints) + " cm");
		}

		if (backgroundColor != null)
		{
			/*
			 * Write object to set page background.
			 */
			float components[] = backgroundColor.getColorComponents(null);
			writeLine(mPDFGeometryWriter, "q");
			writeLine(mPDFGeometryWriter, "0 0 " + widthInPoints + " " + heightInPoints + " re");
			for (int i = 0; i < components.length; i++)
				writeLine(mPDFGeometryWriter, mCoordinateDecimal.format(components[i]));
			writeLine(mPDFGeometryWriter, "rg f Q");
		}
		writeLine(mPDFGeometryWriter, "0 0 0 RG 0 0 0 rg");

		/*
		 * Set scale so that we can give all coordinate positions in millimetres.
		 */
		double scale = Constants.POINTS_PER_INCH / Constants.MM_PER_INCH;
		writeLine(mPDFGeometryWriter, scale + " 0 0 " + scale + " 0 0 cm");
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
		writeLine(mWriter, "% Set scaling so that (x, y) coordinates are given in millimetres");
		writeLine(mWriter, Constants.POINTS_PER_INCH + " " + Constants.MM_PER_INCH +
			" div dup scale");
	}

	/**
	 * Write SVG file header, starting with XML waffle.
	 * @param width width of page in mm.
	 * @param height height of page in mm.
	 * @param backgroundColor background color for page, or null if no background.
	 */
	private void writeSVGHeader(double width, double height, Color backgroundColor)
	{
		writeLine(mWriter, "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>");

		Date now = new Date();
		writeLine(mWriter, "<!-- Created by " + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + " on " + now.toString() + " -->");

		double pxPerMM = Constants.getScreenResolution() / Constants.MM_PER_INCH;

		writeLine(mWriter, "<svg width=\"" +
			mCoordinateDecimal.format(width * pxPerMM) + "\"");
		writeLine(mWriter, "  height=\"" +
			mCoordinateDecimal.format(height * pxPerMM) + "\"");
		writeLine(mWriter, "  version=\"1.1\"");
		writeLine(mWriter, "  overflow=\"hidden\"");
		writeLine(mWriter, "  xml:space=\"preserve\"");
		writeLine(mWriter, "  xmlns=\"http://www.w3.org/2000/svg\">");

		if (backgroundColor != null)
		{
			writeLine(mWriter, "<rect x=\"0\" y=\"0\" width=\"100%\"");
			writeLine(mWriter, "  height=\"100%\" stroke=\"none\"");
			writeLine(mWriter, "  fill=\"" +
				ColorDatabase.toHexString(backgroundColor) + "\" fill-opacity=\"1\"/>");
		}

		/*
		 * Set scaling so that units can be given in millimetres.
		 * Set reasonable default values for rarely used settings that are
		 * not given each time a shape is displayed.
		 */
		writeLine(mWriter, "<g transform=\"scale(" + pxPerMM + ")\"");
		writeLine(mWriter, "  style=\"fill-rule:nonzero;fill-opacity:1;stroke-opacity:1;stroke-dasharray:none;\"");
		writeLine(mWriter, "  clip-rule=\"nonzero\">");
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
		String knownFormats[] = ImageIOWrapper.getWriterFormatNames();
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
		boolean compressOutput = false;
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
							String fontName = ttf.getName();
							mTTFFonts.put(fontName, ttf);
							mTTFFonts.put(fontName.toLowerCase(), ttf);
							mTTFFonts.put(fontName.toUpperCase(), ttf);
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
			else if (token.startsWith("update=") && mOutputType != SVG && mOutputType != PDF)
			{
				String flag = token.substring(7);
				mIsUpdatingFile = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("compress="))
			{
				String flag = token.substring(9);
				compressOutput = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("imagemap=") && mOutputType == IMAGE_FILE)
			{
				mImageMapWriter = new PrintWriter(new FileWriter(token.substring(9)));
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

		if ((mOutputType == POSTSCRIPT_GEOMETRY ||
			mOutputType == POSTSCRIPT_IMAGE ||
			mOutputType == PDF ||
			mOutputType == IMAGE_FILE ||
			mOutputType == SVG) && (!mIsUpdatingFile))
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
				String []cmdArray;
				if (Constants.getOSName().indexOf("WIN") >= 0)
					cmdArray = new String[]{pipeCommand};
				else
					cmdArray = new String[]{"sh", "-c", pipeCommand};
				mOutputProcess = Runtime.getRuntime().exec(cmdArray);
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
			 * Compress output too if the user wants it.
			 */
			if (compressOutput)
				mOutputStream = new GZIPOutputStream(mOutputStream);
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
		if (mOutputType == POSTSCRIPT_GEOMETRY ||
			mOutputType == POSTSCRIPT_IMAGE ||
			mOutputType == PDF)
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

			if (mOutputType == PDF)
				writePDFHeader(filename, width, height, resolution, turnPage, fontList, backgroundColor);
			else
				writePostScriptHeader(width, height, resolution, turnPage, fontList, backgroundColor);

			mNeededFontResources = new LinkedList();

			if (mIsUpdatingFile)
			{
				/*
				 * Append contents of existing file as an included document
				 * to the new file we are creating.
				 */
				writeLine(mWriter, "save");
				writeLine(mWriter, "/showpage {} def");
				writeLine(mWriter, "%%BeginDocument: " + filename);
				BufferedReader r = null;

				try
				{
					r = new BufferedReader(new FileReader(filename));
					String line;
					while ((line = r.readLine()) != null)
					{
						writeLine(mWriter, line);
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
				writeLine(mWriter, "%%EndDocument");
				writeLine(mWriter, "restore");
			}
			if (mOutputType != PDF)
				writePostScriptScaling();
		}
		else if (mOutputType == SVG)
		{
			mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)));
			writeSVGHeader(width, height, backgroundColor);

			/*
			 * Create a graphics context we can use for saving current graphics
			 * state whilst drawing.
			 */
			BufferedImage anyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			mGraphics2D = (Graphics2D)anyImage.getGraphics();
		}

		if (mOutputType != POSTSCRIPT_GEOMETRY && mOutputType != SVG && mOutputType != PDF)
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
					mImage = ImageIOWrapper.read(f, Color.BLACK);
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
					 * JPEG and PPM (which do not support it).
					 */
					if (mFormatName.equals("jpg") || mFormatName.equals("jpeg") || mFormatName.equals("ppm"))
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

		if (mImageMapWriter != null)
		{
			/*
		 	 * Create image map.
		 	 */
			mImageMapWriter.println("<!-- HTML Imagemap created by Mapyrus for image " + filename + " -->");
			mImageMapWriter.println("<!-- Surround contents of this file with <map name=\"foo\"> ... </map> and -->");
			mImageMapWriter.println("<!-- combine it with the image using HTML like <img src=\"" + filename + "\" usemap=\"#foo\"> -->");
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
		else if (mFormatName.equals("pdf") || mFormatName.equals("application/pdf"))
		{
			mOutputType = PDF;
		}
		else if (mFormatName.equals("svg") ||
			mFormatName.equals("image/svg+xml"))
		{
			mFormatName = "svg";
			mOutputType = SVG;
		}
		else if (mFormatName.equals("screen"))
		{
			mOutputType = SCREEN_WINDOW;
		}
		else
		{
			if (mFormatName.equals("image/x-portable-pixmap"))
				mFormatName = "ppm";
			else if (mFormatName.startsWith("image/"))
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
			if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
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
	 * Write a line to PostScript, PDF or SVG file.
	 * @return number of characters written to file.
	 */
	private int writeLine(PrintWriter writer, String line)
	{
		writer.write(line);
		writer.write("\r\n");
		return(line.length() + 2);
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
	private void writePostScriptOrPDFImage(BufferedImage image, double x, double y,
		double width, double height, double rotation)
		throws IOException, MapyrusException
	{
		int pixelWidth, pixelHeight;
		int step;
		String imageKey = null;
		if (mPDFImageObjects != null)
			imageKey = "Im" + mPDFImageObjects.size();
		StringWriter sw = null;
		PrintWriter pw;
		if (mOutputType == PDF)
			pw = new PrintWriter(sw = new StringWriter());
		else
			pw = mWriter;

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
		if (mOutputType == PDF)
		{
			writeLine(mPDFGeometryWriter, "q");
			writeLine(mPDFGeometryWriter, "1 0 0 1 " +
				mCoordinateDecimal.format(x) + " " +
				mCoordinateDecimal.format(y) + " cm % translate");

			double sin = Math.sin(rotation);
			double cos = Math.cos(rotation);
			writeLine(mPDFGeometryWriter, mCoordinateDecimal.format(cos) + " " +
				mCoordinateDecimal.format(sin) + " " +
				mCoordinateDecimal.format(-sin) + " " +
				mCoordinateDecimal.format(cos) + " 0 0 cm % rotate " + rotation);

			writeLine(mPDFGeometryWriter, mCoordinateDecimal.format(width) +
				" 0 0 " + mCoordinateDecimal.format(height) +
				" 0 0 cm % scale");
			writeLine(mPDFGeometryWriter, "1 0 0 1 -0.5 -0.5 cm % translate");
			writeLine(mPDFGeometryWriter, "/" + imageKey + " Do");
			writeLine(mPDFGeometryWriter, "Q");
		}
		else
		{
			writeLine(pw, "q");
			writeLine(pw, "/DeviceRGB setcolorspace");

			writeLine(pw, x + " " + y + " translate");
			writeLine(pw, rotation + " radtodeg rotate");
			writeLine(pw, width + " " + height + " scale");
		
			/*
			 * Image is centred at each point.
			 * Shift image left and down half it's size so that it is displayed centred.
			 */
			writeLine(pw, "-0.5 -0.5 translate");
		}

		/*
		 * Set color for drawing single color images.
		 */
		if (singleColor != null)
		{
			float []c = singleColor.getColorComponents(null);
			PrintWriter pw2 = (mOutputType == PDF) ? mPDFGeometryWriter : mWriter;
			writeLine(pw2, mCoordinateDecimal.format(c[0]) + " " +
				mCoordinateDecimal.format(c[1]) + " " +
				mCoordinateDecimal.format(c[2]) + " RG");
		}

		writeLine(pw, "% original image size " + pixelWidth + "x" + pixelHeight + " with reduction factor " + step);
		writeLine(pw, "<<");
		if (mOutputType == PDF)
			writeLine(pw, "/Type /XObject /Subtype /Image /ColorSpace /DeviceRGB");
		else
			writeLine(pw, "/ImageType 1");
		writeLine(pw, "/Width " + reducedPixelWidth);
		writeLine(pw, "/Height " + reducedPixelHeight);
		if (singleColor != null)
		{
			writeLine(pw, "/BitsPerComponent 1");
			writeLine(pw, "/Decode [0 1]");
			if (mOutputType == PDF)
				writeLine(pw, "/ImageMask true");
		}
		else
		{
			writeLine(pw, "/BitsPerComponent 8");
			writeLine(pw, "/Decode [0 1 0 1 0 1]");
		}
		if (mOutputType == PDF)
		{
			writeLine(pw, "/Filter /ASCII85Decode");
		}
		else
		{
			writeLine(pw, "/ImageMatrix [" + reducedPixelWidth + " 0 0 " +
					-reducedPixelHeight + " 0 " + reducedPixelHeight + "]");
			writeLine(pw, "/DataSource currentfile /ASCII85Decode filter");
			writeLine(pw, ">>");

			if (singleColor != null)
				writeLine(pw, "imagemask");
			else
				writeLine(pw, "image");
		}

		/*
		 * Write ASCII85 encoded string containing all pixel values.
		 */
		StringWriter ascii85sw = new StringWriter();
		PrintWriter ascii85pw = (mOutputType == PDF) ? new PrintWriter(ascii85sw) : pw;
		ASCII85Writer ascii85 = new ASCII85Writer(ascii85pw);
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
		writeLine(ascii85pw, "~>");
		if (mOutputType == PDF)
		{
			ascii85pw.flush();
			String s = ascii85sw.toString();
			writeLine(pw, "/Length " + (s.length() + 2));
			writeLine(pw, ">>");
			writeLine(pw, "stream");
			writeLine(pw, s);
			writeLine(pw, "endstream");
			pw.flush();
			mPDFImageObjects.put(imageKey, sw);
		}
		else
		{
			writeLine(pw, "Q");
		}
	}

	/**
	 * Save state, protecting color, linestyle, transform of output.
	 * This state can be restored later with restoreState().
	 */
	public void saveState()
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY)
			writeLine(mWriter, "q");
		else if (mOutputType == PDF)
			writeLine(mPDFGeometryWriter, "q");
		else if (mOutputType == SVG)
			writeLine(mWriter, "<g>");
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
			writeLine(mWriter, "Q");
			retval = true;
		}
		else if (mOutputType == PDF)
		{
			writeLine(mPDFGeometryWriter, "Q");
			retval = false;
		}
		else 
		{
			if (mOutputType == SVG)
				writeLine(mWriter, "</g>");

			/*
			 * Can't restore state when drawing to an image or SVG file.  Caller
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
				writePostScriptOrPDFImage(mImage, mPageWidth / 2, mPageHeight / 2,
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
				writeLine(mWriter, "showpage");
			}

			writeLine(mWriter, "%%Trailer");
			
			/*
			 * Included list of fonts we used in this file but did
			 * not include in the header.
			 */	
			writeLine(mWriter, "%%DocumentNeededResources:");
			Iterator it = mNeededFontResources.iterator();
			while (it.hasNext())
			{
				String fontName = (String)(it.next());
				if (!mSuppliedFontResources.contains(fontName))
					writeLine(mWriter, "%%+ font " + fontName);
			}
			writeLine(mWriter, "%%EOF");

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
		else if (mOutputType == PDF)
		{
			/*
			 * Now that we have the complete geometry, we can write it to the PDF file.
			 */
			mPDFGeometryWriter.flush();
			String geometry = mPDFGeometryStringWriter.toString();
			int objIndex = mPDFFileOffsets.size();
			int nChars = writeLine(mWriter, objIndex + " 0 obj % Geometry Object");
			objIndex++;
			nChars += writeLine(mWriter, "<< /Length " + geometry.length() + " >>");
			nChars += writeLine(mWriter, "stream");
			nChars += writeLine(mWriter, geometry);
			nChars += writeLine(mWriter, "endstream");
			nChars += writeLine(mWriter, "endobj");

			/*
			 * Write dictionary containing each image used in file.
			 */
			Integer offset = (Integer)mPDFFileOffsets.get(mPDFFileOffsets.size() - 1);
			mPDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(mWriter, objIndex + " 0 obj % Image Dictionary");
			objIndex++;
			nChars += writeLine(mWriter, "<<");

			Iterator it = mPDFImageObjects.keySet().iterator();
			int counter = 0;
			while (it.hasNext())
			{
				Object key = it.next();
				nChars += writeLine(mWriter, "/" + key.toString() +
					" " + (objIndex + counter) + " 0 R");
				counter++;
			}
			nChars += writeLine(mWriter, ">>");
			nChars += writeLine(mWriter, "endobj");

			/*
			 * Write each image to file.
			 */
			it = mPDFImageObjects.keySet().iterator();
			while (it.hasNext())
			{
				offset = (Integer)mPDFFileOffsets.get(mPDFFileOffsets.size() - 1);
				mPDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				Object key = it.next();
				nChars = writeLine(mWriter, objIndex + " 0 obj");
				nChars += writeLine(mWriter, mPDFImageObjects.get(key).toString());
				nChars += writeLine(mWriter, "endobj");
				objIndex++;
			}

			/*
			 * Write cross reference table giving file offset of each
			 * object in PDF file.
			 */
			writeLine(mWriter, "xref");
			writeLine(mWriter, "0 " + objIndex);
			writeLine(mWriter, "0000000000 65535 f");
			it = mPDFFileOffsets.iterator();
			while (it.hasNext())
			{
				String fileOffset = it.next().toString();
				int padding = 10 - fileOffset.length();
				StringBuffer sb = new StringBuffer();
				while (padding-- > 0)
					sb.append('0');
				sb.append(fileOffset);
				sb.append(" 00000 n");
				writeLine(mWriter, sb.toString());
			}

			writeLine(mWriter, "trailer");
			writeLine(mWriter, "<< /Size " + objIndex);
			writeLine(mWriter, "/Root 1 0 R");
			writeLine(mWriter, "/Info 2 0 R");
			writeLine(mWriter, ">>");

			/*
			 * Write file offset of start of cross reference table.
			 */
			writeLine(mWriter, "startxref");
			offset = (Integer)mPDFFileOffsets.get(mPDFFileOffsets.size() - 1);
			writeLine(mWriter, Integer.toString(offset.intValue() + nChars));
			writeLine(mWriter, "%%EOF");

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
		else if (mOutputType == SVG)
		{
			writeLine(mWriter, "</g>");
			writeLine(mWriter, "</svg>");

			if (mIsStandardOutput)
				mWriter.flush();
			else
				mWriter.close();

			if (mWriter.checkError())
			{
				throw new MapyrusException(mFilename +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_SVG));
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
			ImageIOWrapper.write(mImage, mFormatName, mOutputStream);

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

		if (mImageMapWriter != null)
		{
			mImageMapWriter.close();
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
				writeLine(mWriter, isoLatinEncode(fontName));
				mEncodeAsISOLatin1.remove(fontName);
			}

			/*
			 * Set font and size for labelling.
			 */
			writeLine(mWriter, "/" + fontName + " " +
				fontSize + " " +
				fontRotation + " " +
				outlineWidth + " font");
			mNeededFontResources.add(fontName);
		}
		else if (mOutputType == PDF)
		{

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
			float newSize = (float)fontSize;
			float currentSize = currentFont.getSize2D();
			String currentFontName = currentFont.getName();
			if (newSize != currentSize ||
				style != currentFont.getStyle() ||
				(!fontName.equals(currentFontName)) ||
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
						mBaseFont = ttf.getFont().deriveFont(style, newSize);
					else
						mBaseFont = new Font(fontName, style, (int)newSize).deriveFont(newSize);
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
		 * Some Font settings not easily held in a Graphics2D
		 * or PDF graphics state so keep track of current values ourselves.
		 */
		mFontRotation = fontRotation;
		mFontOutlineWidth = outlineWidth;
		mFontName = fontName;
		mFontSize = fontSize;
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
			writeLine(mWriter, mJustificationShiftX + " " + mJustificationShiftY + " ju");
		}
	}

	/**
	 * Set color in output format.
	 * @param color is color to draw in.
	 */
	public void setColorAttribute(Color color)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
		{
			float c[] = color.getRGBColorComponents(null);
			PrintWriter pw;
			
			if (mOutputType == PDF)
				pw = mPDFGeometryWriter;
			else
				pw = mWriter;

			StringBuffer sb = new StringBuffer();
			sb.append(mCoordinateDecimal.format(c[0]));
			sb.append(' ');
			sb.append(mCoordinateDecimal.format(c[1]));
			sb.append(' ');
			sb.append(mCoordinateDecimal.format(c[2]));
			writeLine(pw, sb.toString() + " RG");
			if (mOutputType == PDF)
				writeLine(pw, sb.toString() + " rg");
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
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
		{
			PrintWriter pw;
			if (mOutputType == PDF)
				pw = mPDFGeometryWriter;
			else
				pw = mWriter;

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

			writeLine(pw, mCoordinateDecimal.format(linestyle.getLineWidth()) + " w " +
				cap + " J " + join + " j " +
				mCoordinateDecimal.format(linestyle.getMiterLimit()) + " M");

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
					s.append(mCoordinateDecimal.format(dashes[i]));
				}
				s.append("] ");
				s.append(linestyle.getDashPhase());
				s.append(" d");
				writeLine(pw, s.toString());
			}
			else
			{
				/*
				 * Remove any dashed line previously defined.
				 */
				writeLine(pw, "[] 0 d");
			}
		}
		else
		{
			mGraphics2D.setStroke(linestyle);
		}
	}

	/**
	 * Set clip path for output format.
	 * @param clipPaths are polygons to clip against, or null if
	 * there are no clip polygons.
	 */
	public void setClipAttribute(ArrayList clipPaths)
	{
		if (mOutputType != POSTSCRIPT_GEOMETRY && mOutputType != PDF)
		{
			mGraphics2D.setClip(null);
			mIsClipPathActive = (clipPaths != null && clipPaths.size() > 0);
			if (mIsClipPathActive)
			{
				if (mOutputType == SVG)
				{
					mClipPathCounter++;
					writeLine(mWriter, "<clipPath id=\"clip" + mClipPathCounter + "\">");
				}

				for (int i = 0; i < clipPaths.size(); i++)
				{
					GeometricPath clipPath = (GeometricPath)(clipPaths.get(i));
					if (mOutputType == SVG)
					{
						writeLine(mWriter, "<path d=\"");
						writeShape(clipPath.getShape(), mOutputType, mWriter, null);
						writeLine(mWriter, "\"/>");
					}
					else
					{
						mGraphics2D.clip(clipPath.getShape());
					}
				}

				if (mOutputType == SVG)
					writeLine(mWriter, "</clipPath>");
			}
		}
	}

	/*
	 * Walk through path, converting it to output format.
	 */	
	private void writeShape(Shape shape, int outputType, PrintWriter pw, String scriptCommands)
	{
		PathIterator pi;

		if (outputType == IMAGEMAP)
		{
			pi = shape.getPathIterator(null, mResolution);
		}
		else
		{
			pi = shape.getPathIterator(null);
		}

		float coords[] = new float[6];
		float lastX = 0.0f, lastY = 0.0f;
		float x = 0.0f, y = 0.0f;
		float distSquared;
		float resolutionSquared = (float)(mResolution * mResolution);
		int segmentType = PathIterator.SEG_CLOSE;
		boolean skippedLastSegment = false;
		String imageMapString = null;

		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			switch (segmentType)
			{
				case PathIterator.SEG_MOVETO:
					lastX = coords[0];
					lastY = coords[1];
					if (outputType == SVG)
					{
						writeLine(pw,
							"M " + mCoordinateDecimal.format(lastX) +
							" " + mCoordinateDecimal.format(mPageHeight - lastY));
					}
					else if (outputType == IMAGEMAP)
					{
						if (imageMapString != null)
							mImageMapWriter.println(imageMapString);

						mImageMapWriter.println("<area shape=\"polygon\" coords=\"" +
							Math.round(lastX / mResolution) + "," +
							Math.round((mPageHeight - lastY) / mResolution));

						imageMapString = "\" " + scriptCommands + " >";
					}
					else
					{
						writeLine(pw,
							mCoordinateDecimal.format(lastX) +
							" " + mCoordinateDecimal.format(lastY) + " m");
					}
					skippedLastSegment = false;
					break;

				case PathIterator.SEG_LINETO:
					x = coords[0];
					y = coords[1];
					distSquared = (lastX - x) * (lastX - x) + (lastY - y) * (lastY - y);
					if (distSquared >= resolutionSquared)
					{
						if (outputType == SVG)
						{
							String sx = mCoordinateDecimal.format(x);
							String sy = mCoordinateDecimal.format(mPageHeight - y);

							/*
							 * Use shortcut path types for horizontal
							 * and vertical line segments.
							 */
							if (x == lastX)
								writeLine(pw, "V " + sy);
							else if (y == lastY)
								writeLine(pw, "H " + sx);
							else
								writeLine(pw, "L " + sx + " " + sy);
						}
						else if (outputType == IMAGEMAP)
						{
							mImageMapWriter.println("," + Math.round(x / mResolution) +
								"," + Math.round((mPageHeight - y) / mResolution));
						}
						else
						{
							writeLine(pw, mCoordinateDecimal.format(x) +
								" " + mCoordinateDecimal.format(y) + " l");
						}
						lastX = x;
						lastY = y;
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
					{
						if (outputType == SVG)
						{
							writeLine(pw, "L " + mCoordinateDecimal.format(x) +
								" " + mCoordinateDecimal.format(mPageHeight - y));
						}
						else if (outputType == IMAGEMAP)
						{
							if (imageMapString != null)
							{
								mImageMapWriter.println("," + Math.round(x / mResolution) +
									"," + Math.round((mPageHeight - y) / mResolution));
							}
						}
						else
						{
							writeLine(pw, mCoordinateDecimal.format(x) + " " +
								mCoordinateDecimal.format(y) + " l");
						}
					}

					if (outputType == SVG)
					{
						writeLine(pw, "z");
					}
					else if (outputType == IMAGEMAP)
					{
						if (imageMapString != null)
						{
							mImageMapWriter.println(imageMapString);
							imageMapString = null;
						}
					}
					else
					{
						writeLine(pw, "h");
					}
					skippedLastSegment = false;
					break;

				case PathIterator.SEG_CUBICTO:
					if (outputType == SVG)
					{
						writeLine(pw, "C " + mCoordinateDecimal.format(coords[0]) + " " +
							mCoordinateDecimal.format(mPageHeight - coords[1]) + " " +
							mCoordinateDecimal.format(coords[2]) + " " +
							mCoordinateDecimal.format(mPageHeight - coords[3]) + " " +
							mCoordinateDecimal.format(coords[4]) + " " +
							mCoordinateDecimal.format(mPageHeight - coords[5]));
					}
					else
					{
						writeLine(pw, mCoordinateDecimal.format(coords[0]) + " " +
							mCoordinateDecimal.format(coords[1]) + " " +
							mCoordinateDecimal.format(coords[2]) + " " +
							mCoordinateDecimal.format(coords[3]) + " " +
							mCoordinateDecimal.format(coords[4]) + " " +
							mCoordinateDecimal.format(coords[5]) + " " +
							"c");
					}
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
			if (outputType == SVG)
			{
				writeLine(pw, "L " + mCoordinateDecimal.format(x) + " " +
					mCoordinateDecimal.format(mPageHeight - y));
			}
			else if (outputType == IMAGEMAP)
			{
				if (imageMapString != null)
				{
					mImageMapWriter.println("," + Math.round(x / mResolution) +
						"," + Math.round((mPageHeight - y) / mResolution));
				}
			}
			else
			{
				writeLine(pw, mCoordinateDecimal.format(x) +
					" " + mCoordinateDecimal.format(y) + " l");
			}
		}

		/*
		 * Complete any imagemap being created.
		 */
		if (outputType == IMAGEMAP && imageMapString != null)
		{
			mImageMapWriter.println(imageMapString);
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

		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
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
					writePostScriptOrPDFImage(image, x, y, mmWidth, mmHeight, rotation);
				}
			}
		}
		else if (mOutputType != SVG)
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
	 * Draw geo-referenced image on page.
	 * @param image image to display.
	 * @param x X coordinate of bottom left corner of image.
	 * @param y Y coordinate of bottom left corner of image.
	 * @param width width of image in millimetres.
	 * @param height height of image in millimetres.
	 */
	public void drawGeoImage(BufferedImage image,
		double x, double y, double width, double height)
		throws MapyrusException, IOException
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
		{
			writePostScriptOrPDFImage(image, x + width / 2,
				y + height / 2, width, height, 0);
		}
		else
		{
			/*
			 * Position image on page, inverting it to appear right way up
			 * and scaling it to requested size.
			 */
			AffineTransform affine = AffineTransform.getTranslateInstance(x, y + height);
			affine.scale(1, -1);
			affine.scale(width / image.getWidth(), height / image.getHeight());
			mGraphics2D.drawImage(image, affine, null);
		}
	}

	/**
	 * Draw EPS file at points on page.
	 * @param pointList is list of Point2D objects at which to draw EPS file.
	 * @param EPS filename.
	 * @param size size for EPS file on page in millimetres.
	 * @param rotation rotation angle for EPS file.
	 * @param scaling scale factor for EPS file.
	 */
	public void drawEPS(ArrayList pointList, String filename,
		double size, double rotation, double scaling)
		throws IOException, MapyrusException
	{
		PostScriptFile psfile = new PostScriptFile(filename);
		Rectangle boundingBox = psfile.getBoundingBox();
		int pointWidth = (int)boundingBox.getWidth();
		int pointHeight = (int)boundingBox.getHeight();
		Point2D pt;
		int i;
		double x, y;

		/*
		 * If size not given then make EPS about as large as defined in the EPS file.
		 */
		if (size <= 0.0)
		{
			size = Math.max(pointWidth, pointHeight) *
				(Constants.MM_PER_INCH / Constants.POINTS_PER_INCH);
		}
		size *= scaling;

		if (mOutputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Include EPS file at each position in list.
			 */
			for (i = 0; i < pointList.size(); i++)
			{
				pt = (Point2D)(pointList.get(i));
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + size >= 0 && x - size <= mPageWidth &&
					y + size >= 0.0 && y - size <= mPageHeight)
				{
					writeLine(mWriter, "save");
					writeLine(mWriter, x + " " + y + " translate");
					writeLine(mWriter, rotation + " radtodeg rotate");

					/*
					 * EPS file is centred at each point.
					 * Shift position left and down half it's size
					 * so that it is displayed centered.
					 */
					writeLine(mWriter, -(size / 2) + " " + -(size / 2) + " translate");

					double scale = size / Math.max(pointWidth, pointHeight);
					writeLine(mWriter, scale + " dup scale");

					/*
					 * Shift EPS file so that lower-left corner of EPS file is in
					 * lower left corner of our box on the page.
					 */
					writeLine(mWriter, -boundingBox.getMinX() + " " + -boundingBox.getMinY() +
						" translate");
						
					/*
					 * Set graphics attributes to initial values, as described
					 * on page 728 of PostScript Language Reference Manual.
					 */
					writeLine(mWriter, "/showpage {} def");
					writeLine(mWriter, "0 setgray 0 setlinecap 1 setlinewidth");
					writeLine(mWriter, "0 setlinejoin 10 setmiterlimit [] 0 setdash newpath");

					writeLine(mWriter, "%%BeginDocument: (" + filename + ")");
					BufferedReader reader = null;
					try
					{
						reader = new FileOrURL(filename).getReader();

						String line;
						while ((line = reader.readLine()) != null)
						{
							writeLine(mWriter, line);
						}
						writeLine(mWriter, "%%EndDocument");
						writeLine(mWriter, "restore");
					}
					finally
					{
						/*
						 * Ensure EPS file is always closed.
						 */
						if (reader != null)
							reader.close();
					}
				}
			}
		}
		else
		{
			/*
			 * We cannot show EPS files when drawing to an image file so show a
			 * transparent grey box where the EPS file would appear.
			 */
			GeneralPath path = new GeneralPath();
			Color currentColor = mGraphics2D.getColor();
			mGraphics2D.setColor(new Color(127, 127, 127, 127));

			for (i = 0; i < pointList.size(); i++)
			{
				pt = (Point2D)(pointList.get(i));
				x = pt.getX();
				y = pt.getY();
				double xDist = Math.cos(rotation) * size / 2;
				double yDist = Math.sin(rotation) * size / 2;
				Point2D currentPoint;

				path.reset();
				path.moveTo((float)(x - xDist + yDist), (float)(y - yDist - xDist));
				currentPoint = path.getCurrentPoint();
				path.lineTo((float)(currentPoint.getX() - yDist - yDist),
					(float)(currentPoint.getY() + xDist + xDist));
				currentPoint = path.getCurrentPoint();
				path.lineTo((float)(currentPoint.getX() + xDist + xDist),
					(float)(currentPoint.getY() + yDist + yDist));
				currentPoint = path.getCurrentPoint();
				path.lineTo((float)(currentPoint.getX() + yDist + yDist),
					(float)(currentPoint.getY() - xDist - xDist));
				path.closePath();
				fill(path);
			}
			mGraphics2D.setColor(currentColor);	
		}
	}

	/**
	 * Draw currently defined path to output page.
	 */
	public void stroke(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == SVG || mOutputType == PDF)
		{
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				if (mOutputType == SVG)
				{
					writeLine(mWriter, "<path d=\"");
					writeShape(shape, mOutputType, mWriter, null);
					writeLine(mWriter, "\"");
					Color color = mGraphics2D.getColor();
					BasicStroke stroke = (BasicStroke)mGraphics2D.getStroke();
					float width = stroke.getLineWidth();
					int endCap = stroke.getEndCap();
					int lineJoin = stroke.getLineJoin();
					float []dashArray = stroke.getDashArray();
					float dashPhase = stroke.getDashPhase();
					String capString, joinString;

					if (endCap == BasicStroke.CAP_BUTT)
						capString = "butt";
					else if (endCap == BasicStroke.CAP_SQUARE)
						capString = "square";
					else
						capString = "round";
					
					if (lineJoin == BasicStroke.JOIN_BEVEL)
						joinString = "bevel";
					else if (lineJoin == BasicStroke.JOIN_MITER)
						joinString = "miter";
					else
						joinString = "round";

					if (mIsClipPathActive)
					{
						writeLine(mWriter, "  clip-path=\"url(#clip" + mClipPathCounter + ")\"");
					}
					writeLine(mWriter, "  style=\"stroke:" + ColorDatabase.toHexString(color) +
						";stroke-width:" + width +
						";stroke-linecap:" + capString +
						";stroke-linejoin:" + joinString);
					if (dashArray != null)
					{
						StringBuffer dashes = new StringBuffer(";stroke-dasharray:");
						for (int i = 0 ; i < dashArray.length; i++)
						{
							if (i > 0)
								dashes.append(",");
							dashes.append(mCoordinateDecimal.format(dashArray[i]));
						}
						writeLine(mWriter, dashes.toString());
						writeLine(mWriter, ";stroke-dashoffset:" + dashPhase);
					}
					int alpha = color.getAlpha();
					if (alpha != 255)
					{
						writeLine(mWriter, ";stroke-opacity:" + (alpha / 255.0f));
					}

					writeLine(mWriter, ";fill:none\"/>");
				}
				else
				{
					PrintWriter pw;
					if (mOutputType == PDF)
						pw = mPDFGeometryWriter;
					else
						pw = mWriter;
					writeShape(shape, mOutputType, pw, null);
					writeLine(pw, "S");
				}
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
	 * @param shape shape to fill on page.
	 * run when this shape is clicked.
	 */
	public void fill(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == SVG || mOutputType == PDF)
		{
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				if (mOutputType == SVG)
				{
					writeLine(mWriter, "<path d=\"");
					writeShape(shape, mOutputType, mWriter, null);
					writeLine(mWriter, "\"");
					Color color = mGraphics2D.getColor();
					int alpha = color.getAlpha();

					if (mIsClipPathActive)
					{
						writeLine(mWriter, "  clip-path=\"url(#clip" + mClipPathCounter + ")\"");
					}
					StringBuffer sb = new StringBuffer("  style=\"fill:");
					sb.append(ColorDatabase.toHexString(color));
					if (alpha != 255)
					{
						sb.append(";fill-opacity:" + (alpha / 255.0f));
					}
					sb.append(";stroke:none\"/>");
					writeLine(mWriter, sb.toString());
				}
				else
				{
					PrintWriter pw;
					if (mOutputType == PDF)
						pw = mPDFGeometryWriter;
					else
						pw = mWriter;
					writeShape(shape, mOutputType, pw, null);
					writeLine(pw, "f");
				}
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
	 * Set script commands for path on output page.
	 * @param shape shape to fill on page.
	 * @param scriptCommands script language commands to
	 * run when this shape is clicked.
	 */
	public void setEventScript(Shape shape, String scriptCommands)
	{
		if (mImageMapWriter != null)
		{
			/*
			 * Write shape to image map together with script commands.
			 */
			writeShape(shape, IMAGEMAP, mWriter, scriptCommands);
		}
		else if (mOutputType == SVG)
		{
			/*
			 * Embed script commands in SVG file.
			 */
			writeLine(mWriter, "<path d=\"");
			writeShape(shape, mOutputType, mWriter, scriptCommands);
			writeLine(mWriter, "\"");
			writeLine(mWriter, scriptCommands);
			writeLine(mWriter, "/>");
		}
	}

	/**
	 * Fill currently defined path with gradient fill pattern.
	 * @param shape current path to be filled.
	 * @param isVerticalGradient true if vertical color gradient to be used,
	 * false for horizontal.
	 * @param c1 color for left or bottom of path.
	 * @param c2 color for right or top of path.
	 */
	public void gradientFill(Shape shape, boolean isVerticalGradient, Color c1, Color c2)
	{
		if (mOutputType == SVG)
		{
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				String uniqueId = "gradient" + mGradientCounter++;
				writeLine(mWriter, "<defs>");
				writeLine(mWriter, "<linearGradient id=\"" + uniqueId + "\"");

				/*
				 * SVG supports only horizontal gradients (default) or vertical
				 * gradients.
				 */
				if (isVerticalGradient)
					writeLine(mWriter, "x1=\"0%\" y1=\"100%\" x2=\"0%\" y2=\"0%\"");

				writeLine(mWriter, ">");
				writeLine(mWriter, "<stop offset=\"0%\" stop-color=\"" +
					ColorDatabase.toHexString(c1) + "\"/>");
				writeLine(mWriter, "<stop offset=\"100%\" stop-color=\"" +
						ColorDatabase.toHexString(c2) + "\"/>");
				writeLine(mWriter, "</linearGradient>");
				writeLine(mWriter, "</defs>");
				writeLine(mWriter, "<path d=\"");
				writeShape(shape, mOutputType, mWriter, null);
				writeLine(mWriter, "\"");

				if (mIsClipPathActive)
				{
					writeLine(mWriter, "  clip-path=\"url(#clip" + mClipPathCounter + ")\"");
				}
				writeLine(mWriter, "  fill=\"url(#" + uniqueId + ")\" stroke=\"none\"/>");
			}
		}
	}

	/**
	 * Set clip region to inside of currently defined path on output page.
	 */
	public void clip(Shape shape)
	{
		if (mOutputType == POSTSCRIPT_GEOMETRY || mOutputType == PDF)
		{
			PrintWriter pw;
			if (mOutputType == PDF)
				pw = mPDFGeometryWriter;
			else
				pw = mWriter;

			/*
			 * Set clip path now, then it stays in effect until previous
			 * state is restored.
			 */
			if (shape.intersects(0.0, 0.0, mPageWidth, mPageHeight))
			{
				writeShape(shape, mOutputType, pw, null);
			}
			else
			{
				/*
				 * Clip region is outside page.  Clip to simple rectangle
				 * outside page instead so that nothing is shown.
				 */
				writeShape(new Rectangle2D.Float(-1.0f, -1.0f, 0.1f, 0.1f),
					mOutputType, pw, null);
			}
			writeLine(pw, "W n");
		}
	}

	/**
	 * Convert a string to PostScript format, escaping special characters and
	 * write it to PostScript file.
	 * @param writer file to write to.
	 * @param prefix keyword to write before string, such as "%%Title:".
	 * @param s is string to convert and write.
	 */
	private int writePostScriptString(PrintWriter writer, String prefix, String s)
	{
		char c;
		int nChars = 0;
		StringBuffer buffer = new StringBuffer();
		if (prefix != null)
			buffer.append(prefix).append(' ');
		buffer.append("(");
		for (int i = 0; i < s.length(); i++)
		{
			/*
			 * Wrap strings that get too long.
			 */
			if (buffer.length() > 72)
			{
				buffer.append('\\');
				nChars += writeLine(writer, buffer.toString());
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
		nChars += writeLine(writer, buffer.toString());
		return(nChars);
	}

	/**
	 * Draw label positioned at (or along) currently defined path.
	 * @param pointList is list of Point2D objects at which to draw label.
	 * @param label is string to draw on path.
	 */
	public void label(ArrayList pointList, String label) throws IOException, MapyrusException
	{
		Point2D pt, startPt;
		double x, y;
		double lastX = 0, lastY = 0;
		String nextLine;
		StringTokenizer st;
		int lineNumber;
		AffineTransform affine;
		FontRenderContext frc = null;
		Stroke originalStroke = null;

		if (mOutputType != POSTSCRIPT_GEOMETRY && mOutputType != PDF)
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

			if (mOutputType == PDF)
			{
				/*
				 * Set transformation with text rotated and origin
				 * moved to positions for text.
				 */
				lastX = lastY = 0;
				writeLine(mPDFGeometryWriter, "q");
				double cos = Math.cos(mFontRotation);
				double sin = Math.sin(mFontRotation);
				writeLine(mPDFGeometryWriter, mCoordinateDecimal.format(cos) + " " +
					mCoordinateDecimal.format(sin) + " " +
					mCoordinateDecimal.format(-sin) + " " +
					mCoordinateDecimal.format(cos) + " " +
					mCoordinateDecimal.format(x) + " " +
					mCoordinateDecimal.format(y) + " cm");
				writeLine(mPDFGeometryWriter, "BT");
				int j = 0, index = -1;
				while (j < PDF_FONTS.length && index < 0)
				{
					if (PDF_FONTS[j].equals(mFontName))
						index = j;
					else
						j++;
				}
				if (index < 0)
				{
					index = 4;
				}
				writeLine(mPDFGeometryWriter, "/F" + index + " " +
					mCoordinateDecimal.format(mFontSize) + " Tf");
				if (mFontOutlineWidth > 0)
				{
					writeLine(mPDFGeometryWriter, "1 Tr " +
						mCoordinateDecimal.format(mFontOutlineWidth) + " w");
				}
				else
				{
					writeLine(mPDFGeometryWriter, "0 Tr");
				}
			}

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
					writeLine(mWriter, mCoordinateDecimal.format(x) + " " +
						mCoordinateDecimal.format(y) + " m");

					/*
					 * Pass counter and line to PostScript procedure for
					 * drawing each line of the label.
					 */
					writeLine(mWriter, Integer.toString(lineNumber));
					writePostScriptString(mWriter, null, nextLine);
					writeLine(mWriter, "t");
				}
				else if (mOutputType == PDF)
				{
					StringDimension dim = getStringDimension(nextLine, mFontName, mFontSize);
					double x2 = dim.getWidth() * mJustificationShiftX;
					double y2 = dim.getHeight() * mJustificationShiftY;
					y2 -= lineNumber * mFontSize;
					writeLine(mPDFGeometryWriter, mCoordinateDecimal.format(x2 - lastX) + " " +
						mCoordinateDecimal.format(y2 - lastY) + " Td");
					lastX = x2;
					lastY = y2;

					/*
					 * Draw each line of the label to PDF file.
					 */
					writePostScriptString(mPDFGeometryWriter, null, nextLine);
					writeLine(mPDFGeometryWriter, "Tj");
				}
				else if (mOutputType == SVG)
				{
					double yInc = 0;

					if (lineNumber > 0)
					{
						Rectangle2D bounds = mBaseFont.getStringBounds(nextLine, frc);
						yInc = bounds.getHeight() * lineNumber;
					}

					String anchor;
					if (mJustificationShiftX == -1)
						anchor = "end";
					else if (mJustificationShiftX == 0)
						anchor = "start";
					else
						anchor = "middle";

					Color color = mGraphics2D.getColor();
					int alpha = color.getAlpha();
					Font font = mGraphics2D.getFont();

					StringBuffer extras = new StringBuffer();
					if (font.isBold())
						extras.append(" font-weight=\"bold\" ");
					if (font.isItalic())
						extras.append(" font-style=\"italic\" ");
					if (mFontOutlineWidth > 0)
					{
						extras.append(" stroke=\"");
						extras.append(ColorDatabase.toHexString(color));
						extras.append("\" stroke-width=\"");
						extras.append(mFontOutlineWidth);
						extras.append("\" ");

						if (alpha != 255)
						{
							extras.append(" stroke-opacity=\"");
							extras.append(alpha / 255.0f);
							extras.append("\" ");
						}
					}
					else
					{
						extras.append(" fill=\"");
						extras.append(ColorDatabase.toHexString(color));
						extras.append("\" ");

						if (alpha != 255)
						{
							extras.append(" fill-opacity=\"");
							extras.append(alpha / 255.0f);
							extras.append("\" ");
						}
					}
					if (mIsClipPathActive)
					{
						extras.append(" clip-path=\"url(#clip");
						extras.append(mClipPathCounter);
						extras.append(")\" ");
					}

					double px, py;

					if (mFontRotation != 0)
					{
						/*
						 * Rotate text around origin point.
						 * Rotation is negative sense because Y axis
						 * decreases downwards.
						 */
						writeLine(mWriter, "<g transform=\"translate(" +
							mCoordinateDecimal.format(x) + ", " +
							mCoordinateDecimal.format(mPageHeight - y) +
							") rotate(" +
							mCoordinateDecimal.format(Math.toDegrees(-mFontRotation)) +
							")\">");
						px = py = 0;
					}
					else
					{
						px = x;
						py = mPageHeight - (y - yInc);
					}

					/*
					 * Shift text to correct vertical alignment.
					 */
					py -= mJustificationShiftY * font.getSize2D();
					
					writeLine(mWriter, "<text x=\"" + mCoordinateDecimal.format(px) +
						"\" y=\"" + mCoordinateDecimal.format(py) +
						"\" text-anchor=\"" + anchor + "\"");

					String fontName = font.getName();

					/*
					 * Change default Java font names to something sensible.
					 */
					if (fontName.equalsIgnoreCase("sansserif") ||
						fontName.equalsIgnoreCase("dialog"))
					{
						fontName = "Courier";
					}

					writeLine(mWriter, "  font-family=\"" + fontName + "\" " +
						"font-size=\"" + font.getSize2D() + "\" " +
						extras.toString());

					/*
					 * Make text string XML safe.
					 */
					nextLine = nextLine.replaceAll("&", "&amp;");
					nextLine = nextLine.replaceAll("<", "&lt;");
					nextLine = nextLine.replaceAll(">", "&gt;");
					nextLine = nextLine.replaceAll("\"", "&quot;");
					writeLine(mWriter, ">" + nextLine + "</text>");

					if (mFontRotation != 0)
						writeLine(mWriter, "</g>");
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
			
			if (mOutputType == PDF)
				writeLine(mPDFGeometryWriter, "ET Q");
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
