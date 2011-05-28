/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
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
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import org.mapyrus.font.AdobeFontMetrics;
import org.mapyrus.font.AdobeFontMetricsManager;
import org.mapyrus.font.PostScriptFont;
import org.mapyrus.font.StringDimension;
import org.mapyrus.font.TrueTypeFont;
import org.mapyrus.gui.MapyrusFrame;
import org.mapyrus.image.BlendComposite;
import org.mapyrus.image.ImageIOWrapper;
import org.mapyrus.io.ASCII85Writer;
import org.mapyrus.io.WildcardFile;
import org.mapyrus.pdf.PDFFile;
import org.mapyrus.ps.PostScriptFile;
import org.mapyrus.svg.SVGFile;

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
	 * Mitering for outlined fonts to prevent sharp angles
	 * extending too far.
	 */
	private static final int FONT_OUTLINE_MITER_LIMIT = 2;

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
	 * Prefixes for SVG and PDF objects containing fonts, images and graphics states.
	 */
	private String m_PDFFontPrefix;
	private String m_PDFImagePrefix;
	private String m_PDFGstatePrefix;
	private String m_SVGClipPathPrefix;

	/*
	 * File or image that drawing commands are
	 * writing to.
	 */
	private int m_outputType;
	private String m_formatName;
	private BufferedImage m_image;
	private String m_filename;
	private PrintWriter m_writer;
	private OutputStream m_outputStream;
	private Graphics2D m_graphics2D;
	private boolean m_isPipedOutput;	
	private boolean m_isStandardOutput;
	private boolean m_isUpdatingFile;
	private Process m_outputProcess;
	private File m_tempFile;
	private PrintWriter m_imageMapWriter;
	private String m_uniqueKey;
	private Throttle m_throttle;

	/*
	 * Frequently used fonts.
	 */
	private FontCache m_fontCache;

	/*
	 * List of font definitions included in this PostScript file and list of fonts
	 * used in this file but not defined.
	 */
	private HashSet<String> m_suppliedFontResources;
	private HashSet<String> m_neededFontResources;

	/*
	 * Fonts which are to be re-encoded to ISOLatin1 or ISOLatin2 in PostScript file.
	 * This is normally done so that extended symbols (such as degree symbol)
	 * can be used.
	 */
	private HashSet<String> m_encodeAsISOLatin1;
	private HashSet<String> m_encodeAsISOLatin2;
	private HashSet<String> m_reencodedFonts;

	/*
	 * Adobe Font Metrics files containing character width information for all fonts.
	 */
	private AdobeFontMetricsManager m_adobeFontMetrics;
	private ArrayList<String> m_afmFiles;
	private ArrayList<PostScriptFont> m_pfbFiles;
	private ArrayList<AdobeFontMetrics> m_PDFFonts;
	
	/*
	 * List of TrueType fonts to load using Java Font.createFont() method.
	 */
	private HashMap<String, TrueTypeFont> m_TTFFonts;

	/*
	 * Page dimensions and resolution.
	 */
	private double m_pageWidth;
	private double m_pageHeight;
	private double m_resolution;
	private String m_mediaBox;

	/*
	 * Minimum line width.
	 */
	private double m_minimumLineWidth;

	/*
	 * Maximum amount of memory to use for holding images when creating PDF output.
	 */
	private long m_maxImageMemory;
	private long m_imageMemory;

	/*
	 * Selected font and size.
	 */
	private String m_fontName;
	private double m_fontSize;

	/*
	 * Justification for labels in X and Y directions.
	 */
	private int m_justificationShiftX;
	private int m_justificationShiftY;

	/*
	 * Rotation of current font in radians, with 0 horizontal,
	 * measured counter-clockwise.
	 */
	private double m_fontRotation;

	/*
	 * If non-zero, gives linewidth to use for drawing outlines of
	 * each character of labels.
	 */
	private double m_fontOutlineWidth;

	/*
	 * Spacing between lines in multi-line labels, as a factor
	 * of the font size.
	 */
	private double m_fontLineSpacing;

	private Font m_baseFont;

	/*
	 * Mask containing protected areas of the page.
	 */
	private PageMask m_pageMask;

	/*
	 * Counts clip paths and gradient fills set for SVG output
	 * so each clip path and gradient can be given a unique id.
	 */
	private int m_clipPathCounter;
	private Stack<Integer> m_SVGOpenGTags;
	private int m_gradientCounter;

	/*
	 * File offset of each object in PDF file and buffers containing
	 * all geometry and additional objects (images and graphics
	 * states) to be included in PDF file. 
	 */
	private ArrayList<Integer> m_PDFFileOffsets;
	private StringWriter m_PDFGeometryStringWriter;
	private PrintWriter m_PDFGeometryWriter;
	private HashMap<String, String> m_PDFExtGStateObjects;
	private HashMap<String, BigString> m_PDFImageObjects;

	/*
	 * Pages in external PDF files to be included in this one.
	 */
	private ArrayList<PDFFile> m_PDFIncludedFiles;
	private ArrayList<ArrayList<Integer>> m_PDFIncludedPages;

	/*
	 * Format for writing coordinate values.
	 */
	private DecimalFormat m_coordinateDecimal = new DecimalFormat("#.###",
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
		int resolution, boolean turnPage, ArrayList<PostScriptFont> fontList, Color backgroundColor)
		throws IOException, MapyrusException
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);

		StringBuffer sb = new StringBuffer("%!PS-Adobe-3.0");
		if (m_formatName.equals("eps") || m_formatName.equals("epsimage"))
			sb.append(" EPSF-3.0");
		writeLine(m_writer, sb.toString());

		if (turnPage)
			writeLine(m_writer, "%%BoundingBox: 0 0 " + heightInPoints + " " + widthInPoints);
		else
			writeLine(m_writer, "%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);

		if ((!m_formatName.equals("eps")) && (!m_formatName.equals("epsimage")))
			writeLine(m_writer, "%%Pages: 1");

		writeLine(m_writer, "%%DocumentData: Clean7Bit");
		writeLine(m_writer, "%%LanguageLevel: 2");
		writeLine(m_writer, "%%Creator: (" + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + ")");
		writeLine(m_writer, "%%OperatorMessage: (Mapyrus Output...)");
		Date now = new Date();
		writeLine(m_writer, "%%CreationDate: (" + now.toString() + ")");
		String username = System.getProperty("user.name");
		if (username != null)
			writeLine(m_writer, "%%For: (" + username + ")");

		/*
		 * List fonts included in this PostScript file.
		 */
		writeLine(m_writer, "%%DocumentRequiredResources: (atend)");
		if (fontList.size() > 0)
		{
			sb = new StringBuffer("%%DocumentSuppliedResources: font");
			Iterator<PostScriptFont> it = fontList.iterator();
			while (it.hasNext())
			{
				PostScriptFont psFont = it.next();
				sb.append(" ").append(psFont.getName());
				m_suppliedFontResources.add(psFont.getName());
			}
			writeLine(m_writer, sb.toString());
		}
		writeLine(m_writer, "%%EndComments");
		writeLine(m_writer, "");
		writeLine(m_writer, "% Resolution " + resolution + " DPI");

		/*
		 * Inline font definitions.
		 */
		writeLine(m_writer, "%%BeginSetup");
		Iterator<PostScriptFont> it = fontList.iterator();
		while (it.hasNext())
		{
			PostScriptFont psFont = it.next();

			writeLine(m_writer, "%%BeginResource: font " + psFont.getName());
			String fontDefinition = psFont.getFontDefinition();
			writeLine(m_writer, fontDefinition);
			writeLine(m_writer, "%%EndResource");			
		}
		writeLine(m_writer, "%%EndSetup");

		/*
		 * Set color and linestyle to reasonable default values.
		 * Taken from 'initgraphics' operator example in PostScript Language
		 * Reference Manual.
		 */
		writeLine(m_writer, "1 setlinewidth 0 setlinecap 0 setlinejoin");
		writeLine(m_writer, "[] 0 setdash 0 setgray 10 setmiterlimit");

		if (turnPage)
		{
			/*
			 * Turn page 90 degrees so that a landscape orientation page appears
			 * on a portrait page.
			 */
			writeLine(m_writer, "% Turn page 90 degrees.");
			writeLine(m_writer, "90 rotate 0 " + heightInPoints + " neg translate");
		}

		/* 
		 * Prevent anything being displayed outside bounding box we've just defined.
		 */
		writeLine(m_writer, "0 0 " + widthInPoints + " " + heightInPoints + " rectclip");

		/*
		 * Set background color for page.
		 */
		writeLine(m_writer, "/RG { setrgbcolor } bind def");
		if (backgroundColor != null)
		{
			float c[] = backgroundColor.getRGBColorComponents(null);
			writeLine(m_writer, "gsave");
			writeLine(m_writer, m_coordinateDecimal.format(c[0]) + " " +
				m_coordinateDecimal.format(c[1]) + " " +
				m_coordinateDecimal.format(c[2]) + " RG");
			writeLine(m_writer, "0 0 " + widthInPoints + " " + heightInPoints + " rectfill");
			writeLine(m_writer, "grestore");
		}

		/*
		 * Define shorter names for most commonly used operations.
		 * Bind all operators names to improve performance (see 3.11 of
		 * PostScript Language Reference Manual).
		 */
		writeLine(m_writer, "/m { moveto } bind def /l { lineto } bind def");
		writeLine(m_writer, "/c { curveto } bind def /h { closepath } bind def");
		writeLine(m_writer, "/S { stroke } bind def /f { fill } bind def");
		writeLine(m_writer, "/W { clip } bind def /n { newpath } bind def");
		writeLine(m_writer, "/ju { /fjy exch def /fjx exch def } bind def");

		/*
		 * Define font and dictionary entries for font size and justification.
		 * Don't bind these as font loading operators may be overridden in
		 * interpreter.
		 */
		writeLine(m_writer, "/font {");
		writeLine(m_writer, "/foutline exch def");
		writeLine(m_writer, "/flinespacing exch def");
		writeLine(m_writer, "/frot exch radtodeg def");
		writeLine(m_writer, "/fsize exch def findfont fsize scalefont setfont } def");
		writeLine(m_writer, "/radtodeg { 180 mul 3.1415629 div } bind def");

		/*
		 * Draw text string, after setting correct position, rotation,
		 * justifying it horizontally and vertically for current font size
		 * and shifting it down or right to match correct justification.
		 *
		 * Line number (starting at 0) and string to show are passed
		 * to this procedure.
		 */
		writeLine(m_writer, "/t {");
		writeLine(m_writer, "/str exch def");
		writeLine(m_writer, "/nlines exch def");
		writeLine(m_writer, "/linenum exch def");
		writeLine(m_writer, "gsave currentpoint translate frot rotate");

		writeLine(m_writer, "% Shift label to correct justification");
		writeLine(m_writer, JUSTIFY_LEFT + " fjx eq {0} if");
		writeLine(m_writer, JUSTIFY_CENTER + " fjx eq {str stringwidth pop neg 2 div} if");
		writeLine(m_writer, JUSTIFY_RIGHT + " fjx eq {str stringwidth pop neg} if");

		writeLine(m_writer, JUSTIFY_BOTTOM + " fjy eq {nlines 1 sub fsize mul flinespacing mul} if");
		writeLine(m_writer, JUSTIFY_MIDDLE + " fjy eq {nlines fsize mul " +
			"nlines 1 sub fsize mul flinespacing 1 sub mul add 2 div fsize sub} if");
		writeLine(m_writer, JUSTIFY_TOP + " fjy eq {fsize neg} if");

		writeLine(m_writer, "% Shift multi-line labels down");
		writeLine(m_writer, "linenum fsize mul flinespacing mul sub");
		writeLine(m_writer, "rmoveto");
		writeLine(m_writer, "% Draw label or label outline");
		writeLine(m_writer, "foutline 0 gt");
		writeLine(m_writer, "{str false charpath foutline w 0 j 0 J " + FONT_OUTLINE_MITER_LIMIT + " M stroke}");
		writeLine(m_writer, "{str show}");
		writeLine(m_writer, "ifelse");
		writeLine(m_writer, "grestore newpath");
		writeLine(m_writer, "} bind def");

		writeLine(m_writer, "/w { setlinewidth } bind def");
		writeLine(m_writer, "/J { setlinecap } bind def");
		writeLine(m_writer, "/j { setlinejoin } bind def");
		writeLine(m_writer, "/M { setmiterlimit } bind def");
		writeLine(m_writer, "/d { setdash } bind def");

		/*
		 * Use new dictionary in saved state so that variables we define
		 * do not overwrite variables in parent state.
		 */
		writeLine(m_writer, "/q { gsave 12 dict begin } bind def");
		writeLine(m_writer, "/Q { end grestore } bind def");
		writeLine(m_writer, "");
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
		int resolution, boolean turnPage, ArrayList<PostScriptFont> fontList,
		Color backgroundColor)
		throws IOException, MapyrusException
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *	Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH * Constants.POINTS_PER_INCH);

		m_pfbFiles = fontList;

		m_PDFFileOffsets = new ArrayList<Integer>();

		int nChars = writeLine(m_writer, "%PDF-1.4");

		m_PDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(m_writer, "1 0 obj % Document Catalog");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/Type /Catalog");
		nChars += writeLine(m_writer, "/Outlines 3 0 R");
		nChars += writeLine(m_writer, "/Pages 4 0 R");
		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, "endobj");

		m_PDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(m_writer, "2 0 obj % Document Metadata");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/Creator (" + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + ")");
		String author = System.getProperty("user.name");
		if (author != null)
			nChars += writeLine(m_writer, "/Author (" + author + ")");
		
		StringBuffer date = new StringBuffer("D:");
		date.append(new SimpleDateFormat("yyyyMMddHHmmssZZZZZ").format(new Date()));
		date.insert(date.length() - 2, '\'');
		date.append('\'');
		nChars += writeLine(m_writer, "/CreationDate (" + date.toString() + ")");
		nChars += writePostScriptString(m_writer, "/Title", filename);
		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, "endobj");

		m_PDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(m_writer, "3 0 obj % Document");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/Type /Outlines");
		nChars += writeLine(m_writer, "/Count 0");
		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, "endobj");

		m_PDFFileOffsets.add(new Integer(nChars));
		nChars += writeLine(m_writer, "4 0 obj % Page Tree Node");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/Type /Pages");
		nChars += writeLine(m_writer, "/Kids [5 0 R]");
		nChars += writeLine(m_writer, "/Count 1");
		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, "endobj");
		m_PDFFileOffsets.add(new Integer(nChars));

		if (turnPage)
			m_mediaBox = "[0 0 " + heightInPoints + " " + widthInPoints + "]";
		else
			m_mediaBox = "[0 0 " + widthInPoints + " " + heightInPoints + "]";
		m_writer.flush();

		m_PDFExtGStateObjects = new HashMap<String, String>();
		m_PDFImageObjects = new HashMap<String, BigString>();
		m_PDFIncludedFiles = new ArrayList<PDFFile>();
		m_PDFIncludedPages = new ArrayList<ArrayList<Integer>>();
		m_PDFGeometryStringWriter = new StringWriter();
		m_PDFGeometryWriter = new PrintWriter(m_PDFGeometryStringWriter);

		if (turnPage)
		{
			/*
			 * Turn page 90 degrees so that a landscape orientation page appears
			 * on a portrait page.
			 */
			writeLine(m_PDFGeometryWriter, "0 1 -1 0 0 0 cm");
			writeLine(m_PDFGeometryWriter, "1 0 0 1 0 " + (-heightInPoints) + " cm");
		}

		if (backgroundColor != null)
		{
			/*
			 * Write object to set page background.
			 */
			float components[] = backgroundColor.getColorComponents(null);
			writeLine(m_PDFGeometryWriter, "q");
			writeLine(m_PDFGeometryWriter, "0 0 " + widthInPoints + " " + heightInPoints + " re");
			for (int i = 0; i < components.length; i++)
				writeLine(m_PDFGeometryWriter, m_coordinateDecimal.format(components[i]));
			writeLine(m_PDFGeometryWriter, "rg f Q");
		}
		writeLine(m_PDFGeometryWriter, "0 0 0 RG 0 0 0 rg");

		/*
		 * Set scale so that we can give all coordinate positions in millimetres.
		 */
		double scale = Constants.POINTS_PER_INCH / Constants.MM_PER_INCH;
		writeLine(m_PDFGeometryWriter, scale + " 0 0 " + scale + " 0 0 cm");

		for (int i = 0; i < m_afmFiles.size(); i++)
		{
			/*
			 * Read .afm file for each additional font file given by user.
			 */
			String afmFilename = (String)m_afmFiles.get(i);
			BufferedReader reader = null;
			AdobeFontMetrics afm = null;
			try
			{
				reader = new BufferedReader(new FileReader(afmFilename));
				afm = new AdobeFontMetrics(reader, afmFilename, m_encodeAsISOLatin1);
				m_PDFFonts.add(afm);
			}
			finally
			{
				if (reader != null)
					reader.close();
			}
		}
	}

	/**
	 * Write resource dictionary to PDF file defining fonts and images
	 * used in PDF file.
	 */
	private void writePDFResources() throws IOException, MapyrusException
	{
		int objectCounter = 6;
		String newline = "\r\n";

		StringBuffer fontDictionary = new StringBuffer(4 * 1024);
		ArrayList<StringBuffer> pdfFontObjects = new ArrayList<StringBuffer>();
		fontDictionary.append("<<").append(newline);
		for (int i = 0; i < PDF_FONTS.length; i++)
		{
			/*
			 * Define names for each of the standard PDF fonts.
			 */
			fontDictionary.append("/" + m_PDFFontPrefix + i);
			fontDictionary.append(newline);
			fontDictionary.append("<< /Type /Font /Subtype /Type1");
			fontDictionary.append(" /BaseFont /" + PDF_FONTS[i] +
				" /Name /" + m_PDFFontPrefix + i);
			if (m_encodeAsISOLatin1.contains(PDF_FONTS[i]))
			{
				fontDictionary.append(" /Encoding /WinAnsiEncoding");
			}
			else if (m_encodeAsISOLatin2.contains(PDF_FONTS[i]))
			{
				fontDictionary.append(" /Encoding << /Type /Encoding /Differences [ " +
					AdobeFontMetricsManager.getISOLatin2Encoding() + " ] >>");
			}
			fontDictionary.append(" >>");
			fontDictionary.append(newline);
		}

		for (int i = 0; i < m_PDFFonts.size(); i++)
		{
			AdobeFontMetrics afm = (AdobeFontMetrics)m_PDFFonts.get(i);

			/*
			 * Add each user defined font, given by a .pfa file.
			 * Find if there is a matching .pfb file for this font too.
			 */
			boolean foundPfbFile = false;
			int j = 0;
			PostScriptFont font = null;
			while (j < m_pfbFiles.size() && (!foundPfbFile))
			{
				font = (PostScriptFont)m_pfbFiles.get(j);
				if (font.getName().equals(afm.getFontName()))
					foundPfbFile = true;
				else
					j++;
			}

			/*
			 * Add font dictionary for this font file.
			 */
			fontDictionary.append("/" + m_PDFFontPrefix + (PDF_FONTS.length + i));
			fontDictionary.append(newline);
			fontDictionary.append("<< /Type /Font /Subtype /Type1");
			fontDictionary.append(" /BaseFont /").append(afm.getFontName());
			fontDictionary.append(" /FirstChar ").append(afm.getFirstChar());
			fontDictionary.append(" /LastChar ").append(afm.getLastChar());
			fontDictionary.append(" /Widths ").append(objectCounter).append(" 0 R");

			/*
			 * Build object containing widths for this font.
			 * We can then add it after completing the font dictionary.
			 */
			StringBuffer sb = new StringBuffer();
			sb.append(objectCounter).append(" 0 obj % Character Widths for ").append(afm.getFontName());
			sb.append(newline);
			objectCounter++;
			sb.append("[");
			for (int k = afm.getFirstChar(); k <= afm.getLastChar(); k++)
			{
				if (k % 16 == 0)
					sb.append(newline);
				sb.append(' ').append(afm.getCharWidth(k));
			}
			sb.append("]").append(newline).append("endobj").append(newline);
			pdfFontObjects.add(sb);

			fontDictionary.append(" /FontDescriptor ").append(objectCounter).append(" 0 R");

			sb = new StringBuffer();
			sb.append(objectCounter).append(" 0 obj % Font Descriptor");
			sb.append(newline);
			objectCounter++;
			sb.append("<<");
			sb.append(newline);
			sb.append("/Type /FontDescriptor");
			sb.append(newline);
			sb.append("/FontName /").append(afm.getFontName());
			sb.append(newline);
			sb.append("/Flags ").append(afm.getFlags());
			sb.append(newline);
			Rectangle rect = afm.getFontBBox();
			sb.append("/FontBBox [" +
				Math.round(rect.getMinX()) + " " +
				Math.round(rect.getMinY()) + " " +
				Math.round(rect.getMaxX()) + " " +
				Math.round(rect.getMaxY()) + "]");
			sb.append(newline);
			sb.append("/ItalicAngle ").append(afm.getItalicAngle());
			sb.append(newline);
			sb.append("/Ascent ").append(afm.getAscender());
			sb.append(newline);
			sb.append("/Descent ").append(afm.getDescender());
			sb.append(newline);
			sb.append("/CapHeight ").append(afm.getCapHeight());
			sb.append(newline);
			sb.append("/StemV 105");
			sb.append(newline);
			if (foundPfbFile)
				sb.append("/FontFile ").append(objectCounter).append(" 0 R").append(newline);
			sb.append(">>").append(newline);
			sb.append("endobj").append(newline);
			pdfFontObjects.add(sb);

			if (foundPfbFile)
			{
				sb = new StringBuffer();
				sb.append(objectCounter).append(" 0 obj % Font File for ").append(font.getName());
				sb.append(newline);
				objectCounter++;
				sb.append(font.getFontDefinition()).append(newline);
				sb.append("endobj").append(newline);
				pdfFontObjects.add(sb);
			}

			if (m_encodeAsISOLatin1.contains(afm.getFontName()))
			{
				fontDictionary.append(" /Encoding /WinAnsiEncoding");
			}
			else if (m_encodeAsISOLatin2.contains(afm.getFontName()))
			{
				fontDictionary.append(" /Encoding << /Type /Encoding /Differences [ " +
					AdobeFontMetricsManager.getISOLatin2Encoding() + " ] >>");
			}
			fontDictionary.append(" >>").append(newline);
		}

		for (int i = 0; i < m_PDFIncludedFiles.size(); i++)
		{
			PDFFile pdfFile = (PDFFile)m_PDFIncludedFiles.get(i);
			ArrayList<Integer> pageNumbers = m_PDFIncludedPages.get(i);
			for (int j = 0; j < pageNumbers.size(); j++)
			{
				Integer pageNumber = (Integer)pageNumbers.get(j);
				ArrayList<StringBuffer> list = pdfFile.getFont(pageNumber.intValue(), objectCounter);
				if (list != null && !list.isEmpty())
				{
					/*
					 * Include dictionary keys from external PDF files
					 * and any other objects that the keys refer to.
					 */
					fontDictionary.append(list.get(0).toString());
					for (int k = 1; k < list.size(); k++)
					{
						pdfFontObjects.add(list.get(k));
						objectCounter++;
					}
				}
			}
		}
		fontDictionary.append(">>").append(newline);

		Integer offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
		int nChars = offset.intValue();
		nChars += writeLine(m_writer, "5 0 obj % Single Page");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/Type /Page");
		nChars += writeLine(m_writer, "/Parent 4 0 R");
		nChars += writeLine(m_writer, "/MediaBox " + m_mediaBox);
		nChars += writeLine(m_writer, "/Contents " + objectCounter + " 0 R");
		objectCounter++;
		nChars += writeLine(m_writer, "/Resources");
		nChars += writeLine(m_writer, "<<");
		nChars += writeLine(m_writer, "/ProcSet [/PDF /Text /ImageB /ImageC]");
		nChars += writeLine(m_writer, "/ExtGState " + objectCounter + " 0 R");
		objectCounter++;
		nChars += writeLine(m_writer, "/ColorSpace " + objectCounter + " 0 R");
		objectCounter++;
		nChars += writeLine(m_writer, "/Pattern " + objectCounter + " 0 R");
		objectCounter++;
		nChars += writeLine(m_writer, "/Shading " + objectCounter + " 0 R");
		objectCounter++;
		nChars += writeLine(m_writer, "/XObject " + objectCounter + " 0 R");
		objectCounter++;

		nChars += writeLine(m_writer, "/Font");
		nChars += writeLine(m_writer, fontDictionary.toString());

		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, ">>");
		nChars += writeLine(m_writer, "endobj");

		/*
		 * Now add font width, font descriptor and font file objects.
		 */
		m_PDFFileOffsets.add(new Integer(nChars));
		for (int i = 0; i < pdfFontObjects.size(); i++)
		{
			nChars += writeLine(m_writer, pdfFontObjects.get(i).toString());
			m_PDFFileOffsets.add(new Integer(nChars));
		}
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
		writeLine(m_writer, "% Set scaling so that (x, y) coordinates are given in millimetres");
		writeLine(m_writer, Constants.POINTS_PER_INCH + " " + Constants.MM_PER_INCH +
			" div dup scale");
	}

	/**
	 * Write SVG file header, starting with XML waffle.
	 * @param width width of page in mm.
	 * @param height height of page in mm.
	 * @param scriptFilename additional XML file to be added to start of SVG file.
	 * @param backgroundColor background color for page, or null if no background.
	 */
	private void writeSVGHeader(double width, double height, String scriptFilename, Color backgroundColor)
		throws IOException, MapyrusException
	{
		writeLine(m_writer, "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>");

		Date now = new Date();
		writeLine(m_writer, "<!-- Created by " + Constants.PROGRAM_NAME +
			" " + Constants.getVersion() + " on " + now.toString() + " -->");

		double pxPerMM = Constants.getScreenResolution() / Constants.MM_PER_INCH;

		writeLine(m_writer, "<svg width=\"" +
			m_coordinateDecimal.format(width * pxPerMM) + "\"");
		writeLine(m_writer, "  height=\"" +
			m_coordinateDecimal.format(height * pxPerMM) + "\"");
		writeLine(m_writer, "  version=\"1.1\"");
		writeLine(m_writer, "  overflow=\"hidden\"");
		writeLine(m_writer, "  xml:space=\"preserve\"");
		writeLine(m_writer, "  xmlns=\"http://www.w3.org/2000/svg\">");

		if (scriptFilename != null)
		{
			/*
			 * Add any other XML elements (for example Javascript functions).
			 */
			LineNumberReader reader = null;
			try
			{
				String line;
				reader = new FileOrURL(scriptFilename).getReader();
				while ((line = reader.readLine()) != null)
				{
					writeLine(m_writer, line);
				}
			}
			finally
			{
				if (reader != null)
					reader.close();
			}
		}

		if (backgroundColor != null)
		{
			writeLine(m_writer, "<rect x=\"0\" y=\"0\" width=\"100%\"");
			writeLine(m_writer, "  height=\"100%\" stroke=\"none\"");
			writeLine(m_writer, "  fill=\"" +
				ColorDatabase.toHexString(backgroundColor) + "\" fill-opacity=\"1\"/>");
		}

		/*
		 * Set scaling so that units can be given in millimetres.
		 * Set reasonable default values for rarely used settings that are
		 * not given each time a shape is displayed.
		 */
		writeLine(m_writer, "<g transform=\"scale(" + pxPerMM + ")\"");
		writeLine(m_writer, "  style=\"fill-rule:nonzero;fill-opacity:1;stroke-opacity:1;stroke-dasharray:none;\"");
		writeLine(m_writer, "  clip-rule=\"nonzero\">");
		m_SVGOpenGTags.push(Integer.valueOf(1));

		/*
		 * Define filters for all possible transparent color blending modes.
		 */
		writeLine(m_writer, "<defs>");
		String []blends = {"normal", "multiply", "screen", "darken", "lighten"};
		for (int i = 0; i < blends.length; i++)
		{
			writeLine(m_writer, "<filter id=\"" + blends[i] + "\">");
			writeLine(m_writer, "<feBlend mode=\"" + blends[i] + "\" in2=\"BackgroundImage\" in=\"SourceGraphic\"/>");
			writeLine(m_writer, "</filter>");
		}
		writeLine(m_writer, "</defs>");
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
		boolean lineAntiAliasing, boolean labelAntiAliasing,
		boolean fractionalFontMetrics)
	{
		double scale;

		scale = resolution / Constants.MM_PER_INCH;

		/*
		 * Set background of entire image to desired color.
		 */
		if (backgroundColor != null)
		{
			Color originalColor = m_graphics2D.getColor();
			m_graphics2D.setColor(backgroundColor);
			m_graphics2D.fillRect(0, 0, m_image.getWidth(), m_image.getHeight());
			m_graphics2D.setColor(originalColor);
		}

		/*
		 * Set transform with origin in lower-left corner and
		 * Y axis increasing upwards.
		 */
		m_graphics2D.translate(0, m_image.getHeight());
		m_graphics2D.scale(scale, -scale);
		
		/*
		 * Set anti-aliasing for labels and lines if the user wants it.
		 */
		if (lineAntiAliasing)
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
		}
		else
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF));
		}

		if (labelAntiAliasing)
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		}
		else
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
		}

		if (fractionalFontMetrics)
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON));
		}
		else
		{
			m_graphics2D.addRenderingHints(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
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
	 * Return PostScript commands to re-encode a font.
	 * @param fontName name of font to re-encode.
	 * @param encoding encoding name or array.
	 * @return string containing PostScript commands to re-encode font.
	 */
	private String reencodeFont(String fontName, String encoding)
	{
		/*
		 * Re-encoding commands taken from section 5.9.1 of Adobe PostScript
		 * Language Reference Manual (2nd Edition).
		 */
		return("/" + fontName + " findfont" + Constants.LINE_SEPARATOR +
			"dup length dict begin" + Constants.LINE_SEPARATOR +
			"{1 index /FID ne {def} {pop pop} ifelse} forall" + Constants.LINE_SEPARATOR +
			"/Encoding " + encoding + " def" + Constants.LINE_SEPARATOR +
			"currentdict" + Constants.LINE_SEPARATOR +
			"end" + Constants.LINE_SEPARATOR +
			"/" + fontName + " exch definefont pop");
	}

	/**
	 * Get unique key.
	 * @return 8 digit hexadecimal key.
	 */
	private String getUniqueKey()
	{
		StringBuffer sb = new StringBuffer();
		long timeStamp = (System.currentTimeMillis() & 0x7fffffff);
		String s = Long.toHexString(timeStamp);
		int zeroPadding = 8 - s.length();
		sb.append(Constants.PROGRAM_NAME.charAt(0));
		while (zeroPadding-- > 0)
			sb.append("0");
		sb.append(s);
		return(sb.toString());
	}

	private void setOutput(String filename, double width, double height,
		String extras, PrintStream stdoutStream)
		throws IOException, MapyrusException
	{
		/*
		 * Parse list of additional options given by caller.
		 */
		ArrayList<PostScriptFont> fontList = new ArrayList<PostScriptFont>();
		m_encodeAsISOLatin1 = new HashSet<String>();
		m_encodeAsISOLatin2 = new HashSet<String>();
		m_reencodedFonts = new HashSet<String>();
		m_TTFFonts = new HashMap<String, TrueTypeFont>();
		m_PDFFonts = new ArrayList<AdobeFontMetrics>();
		m_afmFiles = new ArrayList<String>();
		m_SVGOpenGTags = new Stack<Integer>();
		m_isUpdatingFile = false;
		int resolution;
		boolean turnPage = false;
		Color backgroundColor = null;
		boolean labelAntiAliasing = true;
		boolean lineAntiAliasing = false;
		boolean fractionalFontMetrics = false;
		boolean compressOutput = false;
		String scriptFilename = null;
		Rectangle2D existingBoundingBox = null;
		m_uniqueKey = getUniqueKey();
		m_PDFFontPrefix =  m_uniqueKey + "F";
		m_PDFImagePrefix = m_uniqueKey + "Img";
		m_PDFGstatePrefix = m_uniqueKey + "Gstate";
		m_SVGClipPathPrefix = m_uniqueKey + "C";

		if (m_outputType == POSTSCRIPT_GEOMETRY)
			resolution = 300;
		else if (m_outputType == PDF)
			resolution = 72;
		else
			resolution = Constants.getScreenResolution();

		/*
		 * Reading all font metrics information takes some time.
		 * Wait until we really need it before loading it.
		 */
		m_adobeFontMetrics = null;

		m_minimumLineWidth = 0;

		m_maxImageMemory = 16 * 1024 * 1024;
		m_imageMemory = 0;

		StringTokenizer st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if ((token.startsWith("pfafiles=") && m_outputType != PDF) ||
				(token.startsWith("pfbfiles=") && m_outputType == PDF))
			{
				boolean isBinary = (m_outputType == PDF);

				/*
				 * Build list of font filenames user wants
				 * to include in this PostScript file.
				 */
				StringTokenizer st2 = new StringTokenizer(token.substring(9), ",");
				while (st2.hasMoreTokens())
				{
					String fontFilename = st2.nextToken();
					if (fontFilename.length() > 0)
					{
						if (!m_throttle.isIOAllowed())
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
								": " + fontFilename);
						}

						/*
						 * Accept wildcards in filenames.
						 */
						WildcardFile wildcard = new WildcardFile(fontFilename);
						Iterator<String> it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
							fontList.add(new PostScriptFont(it.next(), isBinary));
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
						if (!m_throttle.isIOAllowed())
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
								": " + afmFilename);
						}

						/*
						 * Accept wildcards in filenames.
						 */
						WildcardFile wildcard = new WildcardFile(afmFilename);
						Iterator<String> it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
						{
							m_afmFiles.add(it.next());
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
						m_encodeAsISOLatin1.add(fontName);
				}
			}
			else if (token.startsWith("isolatin2fonts="))
			{
				/*
				 * Build list of fonts to encode in ISOLatin2.
				 */
				StringTokenizer st2 = new StringTokenizer(token.substring(15), ",");
				while (st2.hasMoreTokens())
				{
					String fontName = st2.nextToken();
					if (fontName.length() > 0)
						m_encodeAsISOLatin2.add(fontName);
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
						if (!m_throttle.isIOAllowed())
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
								": " + ttfFilename);
						}

						WildcardFile wildcard = new WildcardFile(ttfFilename);
						Iterator<String> it = wildcard.getMatchingFiles().iterator();
						while (it.hasNext())
						{
							String s = it.next();
							TrueTypeFont ttf = new TrueTypeFont(s);
							String fontName = ttf.getName();
							m_TTFFonts.put(fontName, ttf);
							m_TTFFonts.put(fontName.toLowerCase(), ttf);
							m_TTFFonts.put(fontName.toUpperCase(), ttf);
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
			else if (token.startsWith("fractionalfontmetrics="))
			{
				String flag = token.substring(22);
				fractionalFontMetrics = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("update=") && m_outputType != SVG && m_outputType != PDF)
			{
				String flag = token.substring(7);
				m_isUpdatingFile = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("compress="))
			{
				String flag = token.substring(9);
				compressOutput = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("imagemap=") && m_outputType == IMAGE_FILE)
			{
				String imageMapFilename = token.substring(9);
				if (!m_throttle.isIOAllowed())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
						": " + imageMapFilename);
				}
				m_imageMapWriter = new PrintWriter(new FileWriter(imageMapFilename));
			}
			else if (token.startsWith("background="))
			{
				String colorName = token.substring(11);
				backgroundColor = ColorDatabase.getColor(colorName, 255, Color.BLACK);
				if (backgroundColor == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.COLOR_NOT_FOUND) +
						": " + colorName);
				}
			}
			else if (token.startsWith("scriptfile="))
			{
				scriptFilename = token.substring(11);
				if (!m_throttle.isIOAllowed())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
						": " + scriptFilename);
				}
			}
			else if (token.startsWith("minimumlinewidth="))
			{
				String lineWidth = token.substring(17);
				try
				{
					m_minimumLineWidth = Double.parseDouble(lineWidth);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINE_WIDTH) +
						": " + e.getMessage());
				}
			}
			else if (token.startsWith("maximumimagememory="))
			{
				String memoryLimit = token.substring(19);
				try
				{
					m_maxImageMemory = Long.parseLong(memoryLimit) * 1024 * 1024;
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + e.getMessage());
				}
			}
		}

		if ((m_outputType == POSTSCRIPT_GEOMETRY ||
			m_outputType == POSTSCRIPT_IMAGE ||
			m_outputType == PDF ||
			m_outputType == IMAGE_FILE ||
			m_outputType == SVG) && (!m_isUpdatingFile))
		{
			/*
			 * Should we pipe the output to another program
			 * instead of writing a file?
			 */
			m_isPipedOutput = filename.startsWith("|");

			/*
			 * Are we writing to standard output instead of to a file?
			 */
			m_isStandardOutput = filename.equals("-");

			if (m_isPipedOutput)
			{
				if (!m_throttle.isIOAllowed())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
						": " + filename);
				}

				String pipeCommand = filename.substring(1).trim();
				String []cmdArray;
				if (Constants.getOSName().indexOf("WIN") >= 0)
					cmdArray = new String[]{pipeCommand};
				else
					cmdArray = new String[]{"sh", "-c", pipeCommand};
				m_outputProcess = Runtime.getRuntime().exec(cmdArray);
				m_outputStream = m_outputProcess.getOutputStream();
			}
			else
			{
				if (m_isStandardOutput)
					m_outputStream = stdoutStream;
				else
				{
					if (!m_throttle.isIOAllowed())
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
							": " + filename);
					}
					m_outputStream = new FileOutputStream(filename);
				}
			}

			/*
			 * Compress output too if the user wants it.
			 */
			if (compressOutput)
				m_outputStream = new GZIPOutputStream(m_outputStream);
		}

		File f = new File(filename);
		if (m_isUpdatingFile)
		{
			if (!m_throttle.isIOAllowed())
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
					": " + filename);
			}
			if (!f.canWrite())
			{
				throw new IOException(MapyrusMessages.get(MapyrusMessages.READ_ONLY) + ": " + filename);
			}
		}

		/*
		 * Setup file we are writing to.
		 */
		if (m_outputType == POSTSCRIPT_GEOMETRY ||
			m_outputType == POSTSCRIPT_IMAGE ||
			m_outputType == PDF)
		{
			if (m_isUpdatingFile)
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
				m_tempFile = File.createTempFile(Constants.PROGRAM_NAME, null,
					new File(filename).getAbsoluteFile().getParentFile());
				m_outputStream = new FileOutputStream(m_tempFile);
			}

			m_writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_outputStream)));

			m_suppliedFontResources = new HashSet<String>();

			if (m_outputType == PDF)
				writePDFHeader(filename, width, height, resolution, turnPage, fontList, backgroundColor);
			else
				writePostScriptHeader(width, height, resolution, turnPage, fontList, backgroundColor);

			m_neededFontResources = new HashSet<String>();

			if (m_isUpdatingFile)
			{
				/*
				 * Append contents of existing file as an included document
				 * to the new file we are creating.
				 */
				writeLine(m_writer, "save");
				writeLine(m_writer, "/showpage {} def");
				writeLine(m_writer, "%%BeginDocument: " + filename);
				BufferedReader r = null;

				try
				{
					r = new BufferedReader(new FileReader(filename));
					String line;
					while ((line = r.readLine()) != null)
					{
						writeLine(m_writer, line);
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
				writeLine(m_writer, "%%EndDocument");
				writeLine(m_writer, "restore");
			}
			if (m_outputType != PDF)
				writePostScriptScaling();
		}
		else if (m_outputType == SVG)
		{
			m_writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_outputStream)));
			writeSVGHeader(width, height, scriptFilename, backgroundColor);

			/*
			 * Create a graphics context we can use for saving current graphics
			 * state whilst drawing.
			 */
			BufferedImage anyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			m_graphics2D = (Graphics2D)anyImage.getGraphics();
		}

		if (m_outputType != POSTSCRIPT_GEOMETRY && m_outputType != SVG && m_outputType != PDF)
		{
			/*
			 * Create image to draw into.
			 */
			if (m_outputType == IMAGE_FILE ||
				m_outputType == SCREEN_WINDOW ||
				m_outputType == POSTSCRIPT_IMAGE)
			{
				if (m_isUpdatingFile && m_outputType == IMAGE_FILE)
				{
					/*
					 * Read existing image for editing.
					 * Set page width and height to size of existing image.
					 */
					m_image = ImageIOWrapper.read(f, Color.BLACK);
					if (m_image == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) + ": " + filename);
					}

					width = m_image.getWidth() / (resolution / Constants.MM_PER_INCH);
					height = m_image.getHeight() / (resolution / Constants.MM_PER_INCH);
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
					if (m_formatName.equals("jpg") || m_formatName.equals("jpeg") || m_formatName.equals("ppm") || m_formatName.equals("bmp"))
						imageType = BufferedImage.TYPE_3BYTE_BGR;
					else
						imageType = BufferedImage.TYPE_INT_ARGB;

					m_image = new BufferedImage(widthInPixels, heightInPixels,
						imageType);
				}
			}
			else if (m_outputType == INTERNAL_IMAGE)
			{
				/*
				 * Calculate width of page, based on image and resolution given
				 * by user.
				 */
				width = m_image.getWidth() / (resolution / Constants.MM_PER_INCH);
				height = m_image.getHeight() / (resolution / Constants.MM_PER_INCH);
			}
			m_graphics2D = (Graphics2D)(m_image.getGraphics());
			setupBufferedImage(resolution, backgroundColor, lineAntiAliasing, labelAntiAliasing, fractionalFontMetrics);
		}

		if (m_imageMapWriter != null)
		{
			/*
		 	 * Create image map.
		 	 */
			m_imageMapWriter.println("<!-- HTML Imagemap created by Mapyrus for image " + filename + " -->");
			m_imageMapWriter.println("<!-- Surround contents of this file with <map name=\"foo\"> ... </map> and -->");
			m_imageMapWriter.println("<!-- combine it with the image using HTML like <img src=\"" + filename + "\" usemap=\"#foo\"> -->");
		}

		m_filename = filename;
		m_pageWidth = width;
		m_pageHeight = height;
		m_resolution = Constants.MM_PER_INCH / resolution;
		m_fontCache = new FontCache();
		m_justificationShiftX = JUSTIFY_LEFT;
		m_justificationShiftY = JUSTIFY_BOTTOM;
		m_fontOutlineWidth = 0.0;
		m_fontLineSpacing = 1;

		/*
		 * Set impossible current font rotation so first font
		 * accessed will be loaded.
		 */
		m_fontRotation = Double.MAX_VALUE;

		/*
		 * Do not allocate page mask until needed to save memory.
		 */
		m_pageMask = null;
	}

	/*
	 * Set writer for HTML image map.
     * @param imageMapWriter is HTML image map to write to.
	 */
	public void setImageMapWriter(PrintWriter imageMapWriter)
	{
		m_imageMapWriter = imageMapWriter;
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
	 * @param throttle throttle limiting CPU usage.
	 */
	public OutputFormat(String filename, String format,
		double width, double height, String extras,
		PrintStream stdoutStream, Throttle throttle)
		throws IOException, MapyrusException
	{
		m_formatName = format.toLowerCase();
		m_throttle = throttle;

		/*
		 * Check that Java can write this image format to a file.
		 */				
		if (m_formatName.equals("ps") ||
			m_formatName.equals("postscript") ||
			m_formatName.equals("application/postscript"))
		{
			m_formatName = "ps";
			m_outputType = POSTSCRIPT_GEOMETRY;
		}
		else if (m_formatName.equals("eps"))
		{
			m_outputType = POSTSCRIPT_GEOMETRY;
		}
		else if (m_formatName.equals("epsimage"))
		{
			m_outputType = POSTSCRIPT_IMAGE;
		}
		else if (m_formatName.equals("pdf") || m_formatName.equals("application/pdf"))
		{
			m_outputType = PDF;
		}
		else if (m_formatName.equals("svg") ||
			m_formatName.equals("image/svg+xml"))
		{
			m_formatName = "svg";
			m_outputType = SVG;
		}
		else if (m_formatName.equals("screen"))
		{
			m_outputType = SCREEN_WINDOW;
		}
		else
		{
			if (m_formatName.equals("image/x-portable-pixmap"))
				m_formatName = "ppm";
			else if (m_formatName.startsWith("image/"))
				m_formatName = m_formatName.substring(6);

			if (!isSupportedImageFormat(m_formatName))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) +
					": " + format);
			}
		
			m_outputType = IMAGE_FILE;
		}
		try
		{
			setOutput(filename, width, height, extras, stdoutStream);
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Sets image for drawing into.
	 * @param image is buffered image to draw into.
	 * @param extras contains extra settings for this output.
	 */
	public OutputFormat(BufferedImage image, String extras)
		throws IOException, MapyrusException
	{
		m_outputType = INTERNAL_IMAGE;
		m_image = image;
		m_formatName = "png";
		m_throttle = new Throttle();
		PrintStream dummyStdout = new PrintStream(new ByteArrayOutputStream());
		try
		{
			setOutput("", 0, 0, extras, dummyStdout);
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Return page width.
	 * @return width in millimetres.
	 */
	public double getPageWidth()
	{
		return(m_pageWidth);
	}
	
	/**
	 * Return page height.
	 * @return height in millimetres.
	 */
	public double getPageHeight()
	{
		return(m_pageHeight);
	}

	/**
	 * Return file format of page.
	 * @return file format of page in lowercase.
	 */
	public String getPageFormat()
	{
		return(m_formatName);
	}
	
	/**
	 * Return resolution of page as a distance measurement.
	 * @return distance in millimetres between centres of adjacent pixels.
	 */
	public double getResolution()
	{
		return(m_resolution);
	}

	/**
	 * Returns height and width of a string, drawn to current page.
	 * @param s string to calculate width for.
	 * @param fontName name of font to calculate dimensions for.
	 * @param fontSize size of characters in millimetres.
	 * @return height and width of string in millimetres.
	 */
	public StringDimension getStringDimension(String s, String fontName, double fontSize, double lineSpacing)
		throws IOException, MapyrusException
	{
		StringDimension retval = new StringDimension();
		BufferedReader stringReader = new BufferedReader(new StringReader(s));
		double width = 0, height = 0, ascent = 0, descent = fontSize;
		String token;
		double w, a, d;
		int lineNumber = 0;

		/*
		 * Break multi-line strings into separate lines so we
		 * can find the width of the longest line.
		 */
		while ((token = stringReader.readLine()) != null)
		{
			if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
			{
				/*
				 * Load Font Metrics information only when it is needed.
				 */
				if (m_adobeFontMetrics == null)
					m_adobeFontMetrics = new AdobeFontMetricsManager(m_afmFiles, m_encodeAsISOLatin1);

				double pointSize = fontSize / Constants.MM_PER_INCH * Constants.POINTS_PER_INCH;
				StringDimension dim = m_adobeFontMetrics.getStringDimension(fontName, pointSize, token);
				w = dim.getWidth();
				a = dim.getAscent();
				d = dim.getDescent();

				w = w / Constants.POINTS_PER_INCH * Constants.MM_PER_INCH;
				a = a / Constants.POINTS_PER_INCH * Constants.MM_PER_INCH;
				d = d / Constants.POINTS_PER_INCH * Constants.MM_PER_INCH;

				if (w > width)
					width = w;
				if (lineNumber == 0)
				{
					height += fontSize;
					ascent = a;
				}
				else
				{
					height += fontSize * lineSpacing;
					ascent += fontSize * lineSpacing;
				}
				descent = d;
			}
			else
			{
				/*
				 * Use Java2D calculation for bounding box of string displayed with
				 * horizontal font.
				 */
				FontRenderContext frc = m_graphics2D.getFontRenderContext();
				Rectangle2D stringBounds = m_baseFont.getStringBounds(token, frc);
				Rectangle2D glyphBounds = m_baseFont.createGlyphVector(frc, token).getVisualBounds();
				w = stringBounds.getWidth();
				if (w > width)
					width = w;
				if (lineNumber == 0)
				{
					height += fontSize;
					ascent = -glyphBounds.getMinY();
				}
				else
				{
					height += fontSize * lineSpacing;
					ascent += fontSize * lineSpacing;	
				}
				descent = -(glyphBounds.getMinY() + glyphBounds.getHeight());
			}
			lineNumber++;
		}

		if (descent > ascent)
			descent = ascent;
		retval.setSize(width, height, ascent, descent);
		return(retval);
	}

	/**
	 * Return mask for this page.
	 * @return page mask.
	 */
	public PageMask getPageMask()
	{
		if (m_pageMask == null)
		{
			m_pageMask = new PageMask((int)Math.round(m_pageWidth),
				(int)Math.round(m_pageHeight));
		}
		return(m_pageMask);
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

	/*
	 * Write a line to PostScript, PDF or SVG file.
	 * @return number of characters written to file.
	 */
	private int writeLine(PrintWriter writer, StringBuffer sb)
	{
		/*
		 * Write one character at a time to avoid very long strings
		 * exhausting memory because some Writer classes buffer everything.
		 */
		int len = sb.length();
		for (int i = 0; i < len; i++)
		{
			writer.write(sb.charAt(i));
		}
		writer.write("\r\n");
		return(len + 2);
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
		if (m_PDFImageObjects != null)
			imageKey = m_PDFImagePrefix + m_PDFImageObjects.size();
		StringWriter sw = null;
		PrintWriter pw;
		if (m_outputType == PDF)
			pw = new PrintWriter(sw = new StringWriter());
		else
			pw = m_writer;

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
			double bestPixelWidth = width / m_resolution;
			double bestPixelHeight = height / m_resolution;

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
		if (m_outputType == PDF)
		{
			writeLine(m_PDFGeometryWriter, "q");
			writeLine(m_PDFGeometryWriter, "1 0 0 1 " +
				m_coordinateDecimal.format(x) + " " +
				m_coordinateDecimal.format(y) + " cm % translate");

			double sin = Math.sin(rotation);
			double cos = Math.cos(rotation);
			writeLine(m_PDFGeometryWriter, m_coordinateDecimal.format(cos) + " " +
				m_coordinateDecimal.format(sin) + " " +
				m_coordinateDecimal.format(-sin) + " " +
				m_coordinateDecimal.format(cos) +
				" 0 0 cm % rotate " +
				m_coordinateDecimal.format(rotation));

			writeLine(m_PDFGeometryWriter, m_coordinateDecimal.format(width) +
				" 0 0 " + m_coordinateDecimal.format(height) +
				" 0 0 cm % scale");
			writeLine(m_PDFGeometryWriter, "1 0 0 1 -0.5 -0.5 cm % translate");

			/*
		 	 * Set color for drawing single color images.
		 	 */
			if (singleColor != null)
			{
				float []c = singleColor.getColorComponents(null);
				writeLine(m_PDFGeometryWriter,
					m_coordinateDecimal.format(c[0]) + " " +
					m_coordinateDecimal.format(c[1]) + " " +
					m_coordinateDecimal.format(c[2]) + " rg");
			}

			writeLine(m_PDFGeometryWriter, "/" + imageKey + " Do");
			writeLine(m_PDFGeometryWriter, "Q");
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

			/*
		 	 * Set color for drawing single color images.
		 	 */
			if (singleColor != null)
			{
				float []c = singleColor.getColorComponents(null);
				writeLine(pw, m_coordinateDecimal.format(c[0]) + " " +
					m_coordinateDecimal.format(c[1]) + " " +
					m_coordinateDecimal.format(c[2]) + " RG");
			}
		}


		writeLine(pw, "% original image size " + pixelWidth + "x" + pixelHeight + " with reduction factor " + step);
		writeLine(pw, "<<");
		if (m_outputType == PDF)
		{
			writeLine(pw, "/Type /XObject /Subtype /Image");
			if (singleColor == null)
				writeLine(pw, "/ColorSpace /DeviceRGB");
		}
		else
		{
			writeLine(pw, "/ImageType 1");
		}
		writeLine(pw, "/Width " + reducedPixelWidth);
		writeLine(pw, "/Height " + reducedPixelHeight);
		if (singleColor != null)
		{
			writeLine(pw, "/BitsPerComponent 1");
			writeLine(pw, "/Decode [0 1]");
			if (m_outputType == PDF)
				writeLine(pw, "/ImageMask true");
		}
		else
		{
			writeLine(pw, "/BitsPerComponent 8");
			writeLine(pw, "/Decode [0 1 0 1 0 1]");
		}
		if (m_outputType == PDF)
		{
			writeLine(pw, "/Filter [/ASCII85Decode /FlateDecode]");
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
		StringWriter ascii85sw = null;
		PrintWriter ascii85pw = null;
		BufferedWriter ascii85fw = null;
		File tempFile = null;
		if (m_outputType == PDF)
		{
			/*
			 * Writing 3 RGB bytes per pixel.
			 */
			int encodedSize = reducedPixelHeight * reducedPixelWidth * 3;

			if (m_throttle.isIOAllowed() &&
				m_imageMemory + encodedSize > m_maxImageMemory)
			{
				/*
				 * Write large images to a temporary file that we'll read
				 * back later to avoid exhausting memory.
				 */
				String dir = System.getProperty("java.io.tmpdir");
				if (dir == null)
					dir = System.getProperty("user.dir");
				tempFile = new File(dir, Constants.PROGRAM_NAME + "." + imageKey);
				ascii85fw = new BufferedWriter(new FileWriter(tempFile));
				ascii85pw = new PrintWriter(ascii85fw);
			}
			else
			{
				/*
				 *  Not sure how well image will compress though so
				 *  allocate big buffer.
				 */
				ascii85sw = new StringWriter(encodedSize + 1);
				ascii85pw = new PrintWriter(ascii85sw);
				m_imageMemory += encodedSize;
			}
		}
		else
		{
			ascii85pw = pw;
		}
		ASCII85Writer ascii85 = new ASCII85Writer(ascii85pw, m_outputType == PDF);
		int byteValue = 0;
		int bitCounter = 0;
		for (int row = 0; row < pixelHeight; row += step)
		{
			m_throttle.sleep();
			if (tempFile != null && ascii85pw.checkError())
			{
				/*
				 * Ensure temporary file is thrown away before we fail.
				 */
				try
				{
					ascii85fw.close();
				}
				catch (IOException e)
				{
				}
				tempFile.delete();
				throw new MapyrusException(tempFile.getPath() +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_FILE));
			}

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
		int nEncodedChars = ascii85.getEncodedLength();

		/*
		 * Write ASCII85 end-of-data marker.
		 */
		nEncodedChars += writeLine(ascii85pw, "~>");
		if (m_outputType == PDF)
		{
			ascii85pw.flush();

			writeLine(pw, "/Length " + nEncodedChars);
			writeLine(pw, ">>");
			writeLine(pw, "stream");
			pw.flush();
			BigString bigs = new BigString();
			if (ascii85sw != null)
			{
				/*
				 * Everything is held in memory
				 * in a single string.
				 */
				StringBuffer sb = ascii85sw.getBuffer();
				writeLine(pw, sb);
				writeLine(pw, "endstream");
				pw.flush();
				bigs.append(sw.getBuffer());
			}
			else
			{
				boolean checkError = ascii85pw.checkError();
				if (ascii85fw != null)
					ascii85fw.close();

				if (tempFile != null && checkError)
				{
					tempFile.delete();
					throw new MapyrusException(tempFile.getPath() +
						": " + MapyrusMessages.get(MapyrusMessages.ERROR_FILE));
				}

				/*
				 * Append PDF commands, then large temporary file containing
				 * image, then more PDF commands. 
				 */				
				bigs.append(sw.getBuffer());
				bigs.append(tempFile);

				StringWriter sw2 = new StringWriter();
				pw = new PrintWriter(sw2);
				writeLine(pw, "endstream");
				pw.flush();
				bigs.append(sw2.getBuffer());
			}
			m_PDFImageObjects.put(imageKey, bigs);
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
		if (m_outputType == POSTSCRIPT_GEOMETRY)
			writeLine(m_writer, "q");
		else if (m_outputType == PDF)
			writeLine(m_PDFGeometryWriter, "q");
		else if (m_outputType == SVG)
		{
			writeLine(m_writer, "<g>");
			
			/*
			 * We will use <g> tags for clipping too.  Keep track
			 * of how many we open so that we can close them all when
			 * we restore the state.
			 */
			m_SVGOpenGTags.push(Integer.valueOf(1));
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

		if (m_outputType == POSTSCRIPT_GEOMETRY)
		{
			writeLine(m_writer, "Q");
			retval = true;
		}
		else if (m_outputType == PDF)
		{
			writeLine(m_PDFGeometryWriter, "Q");
			retval = false;
		}
		else 
		{
			if (m_outputType == SVG)
			{
				/*
				 * Close all the <g> tags that we opened.
				 */
				int nStates = m_SVGOpenGTags.pop().intValue();
				for (int i = 0; i < nStates; i++)
					writeLine(m_writer, "</g>");
			}

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
		try
		{
			flushOutput();
		}
		finally
		{
			if (m_outputType == PDF)
			{
				/*
				 * Ensure any temporary image files are deleted.
				 */
				Iterator<BigString> it = m_PDFImageObjects.values().iterator();
				while (it.hasNext())
				{
					it.next().deleteFiles();
				}
			}
		}
	}

	/**
	 * Write file trailer and buffered data then close file.
	 */
	private void flushOutput() throws IOException, MapyrusException
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == POSTSCRIPT_IMAGE)
		{
			if (m_outputType == POSTSCRIPT_IMAGE)
			{
				/*
				 * Write image file containing page.
				 */
				writePostScriptOrPDFImage(m_image, m_pageWidth / 2, m_pageHeight / 2,
					m_pageWidth, m_pageHeight, 0);
			}

			/*
			 * Finish off PostScript file.
			 */
			if (m_formatName.equals("ps"))
			{
				/*
				 * showpage is not included in Encapsulated PostScript files.
				 */
				writeLine(m_writer, "showpage");
			}

			writeLine(m_writer, "%%Trailer");
			
			/*
			 * Included list of fonts we used in this file but did
			 * not include in the header.
			 */	
			writeLine(m_writer, "%%DocumentNeededResources:");
			Iterator<String> it = m_neededFontResources.iterator();
			while (it.hasNext())
			{
				String fontName = it.next();
				if (!m_suppliedFontResources.contains(fontName))
					writeLine(m_writer, "%%+ font " + fontName);
			}
			writeLine(m_writer, "%%EOF");

			if (m_isStandardOutput)
				m_writer.flush();
			else
				m_writer.close();

			if (m_writer.checkError())
			{
				throw new MapyrusException(m_filename +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_PS));
			}

			/*
			 * If updating file then replace existing file with completed
			 * temporary file now.
			 */
			if (m_tempFile != null)
			{
				if ((!new File(m_filename).delete()) || (!m_tempFile.renameTo(new File(m_filename))))
				{
					m_tempFile.delete();
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.READ_ONLY) + ": " + m_filename);
				}
			}
		}
		else if (m_outputType == PDF)
		{
			/*
			 * Now we have finished the page we know all needed resources
			 * and can write the resource dictionary.
			 */
			writePDFResources();

			/*
			 * Now that we have the complete geometry, we can compress it
			 * and write it to the PDF file.
			 */
			m_PDFGeometryWriter.flush();
			StringBuffer sb2 = m_PDFGeometryStringWriter.getBuffer();
			int stringLength = sb2.length();
			StringWriter sw = new StringWriter(stringLength);
			ASCII85Writer ascii85 = new ASCII85Writer(sw, true);
			for (int i = 0; i < stringLength; i++)
				ascii85.write(sb2.charAt(i));
			ascii85.close();
			sw.write("~>");
			StringBuffer compressedGeometry = sw.getBuffer();
			int geometryLength = compressedGeometry.length() + 2;

			int objIndex = m_PDFFileOffsets.size();
			int nChars = writeLine(m_writer, objIndex + " 0 obj % Geometry Object");
			objIndex++;
			nChars += writeLine(m_writer, "<< /Length " + geometryLength);
			nChars += writeLine(m_writer, "/Filter [/ASCII85Decode /FlateDecode] >>");
			nChars += writeLine(m_writer, "stream");
			nChars += writeLine(m_writer, compressedGeometry.toString());
			nChars += writeLine(m_writer, "endstream");
			nChars += writeLine(m_writer, "endobj");

			/*
			 * Write dictionary containing graphics states defining
			 * blend modes and alpha values.
			 */
			Integer offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(m_writer, objIndex + " 0 obj % Graphics States");
			objIndex++;
			nChars += writeLine(m_writer, "<<");

			Object []pdfExtGStateObjs = m_PDFExtGStateObjects.keySet().toArray();
			Arrays.sort(pdfExtGStateObjs);
			int counter = 0;
			for (int i = 0; i < pdfExtGStateObjs.length; i++)
			{
				String key = pdfExtGStateObjs[i].toString();
				nChars += writeLine(m_writer, "/" + key +
					" " + (objIndex + counter + 4) + " 0 R");
				counter++;
			}

			ArrayList<StringBuffer> includedExtGstateObjects = new ArrayList<StringBuffer>();
			for (int i = 0; i < m_PDFIncludedFiles.size(); i++)
			{
				PDFFile pdfFile = (PDFFile)m_PDFIncludedFiles.get(i);
				ArrayList<Integer> pageNumbers = m_PDFIncludedPages.get(i);
				for (int j = 0; j < pageNumbers.size(); j++)
				{
					Integer pageNumber = (Integer)pageNumbers.get(j);
					ArrayList<StringBuffer> list = pdfFile.getExtGState(pageNumber.intValue(),
						objIndex + counter + 4);
					if (list != null && !list.isEmpty())
					{
						/*
						 * Include dictionary keys from external PDF files
						 * and any other objects that the keys refer to.
						 */
						nChars += writeLine(m_writer, list.get(0).toString());
						for (int k = 1; k < list.size(); k++)
						{
							includedExtGstateObjects.add(list.get(k));
							counter++;
						}
					}
				}
			}
			nChars += writeLine(m_writer, ">>");
			nChars += writeLine(m_writer, "endobj");

			/*
			 * Write dictionary containing colorspaces used in external
			 * PDF files.
			 */
			offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(m_writer, objIndex + " 0 obj % ColorSpace");
			objIndex++;
			nChars += writeLine(m_writer, "<<");
			ArrayList<StringBuffer> includedColorSpaceObjects = new ArrayList<StringBuffer>();
			for (int i = 0; i < m_PDFIncludedFiles.size(); i++)
			{
				PDFFile pdfFile = (PDFFile)m_PDFIncludedFiles.get(i);
				ArrayList<Integer> pageNumbers = m_PDFIncludedPages.get(i);
				for (int j = 0; j < pageNumbers.size(); j++)
				{
					Integer pageNumber = (Integer)pageNumbers.get(j);
					ArrayList<StringBuffer> list = pdfFile.getColorSpace(pageNumber.intValue(),
						objIndex + counter + 3);
					if (list != null && !list.isEmpty())
					{
						/*
						 * Include dictionary keys from external PDF files
						 * and any other objects that the keys refer to.
						 */
						nChars += writeLine(m_writer, list.get(0).toString());
						for (int k = 1; k < list.size(); k++)
						{
							includedColorSpaceObjects.add(list.get(k));
							counter++;
						}
					}
				}
			}
			nChars += writeLine(m_writer, ">>");
			nChars += writeLine(m_writer, "endobj");
			
			/*
			 * Write dictionary containing patterns used in external
			 * PDF files.
			 */
			offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(m_writer, objIndex + " 0 obj % Pattern");
			objIndex++;
			nChars += writeLine(m_writer, "<<");
			nChars += writeLine(m_writer, ">>");
			nChars += writeLine(m_writer, "endobj");

			/*
			 * Write dictionary containing shading used in external
			 * PDF files.
			 */
			offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(m_writer, objIndex + " 0 obj % Shading");
			objIndex++;
			nChars += writeLine(m_writer, "<<");
			nChars += writeLine(m_writer, ">>");
			nChars += writeLine(m_writer, "endobj");

			/*
			 * Write dictionary containing each image used in file.
			 */
			offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));
			nChars = writeLine(m_writer, objIndex + " 0 obj % Image Dictionary");
			objIndex++;
			nChars += writeLine(m_writer, "<<");

			Object []pdfImageObjs = m_PDFImageObjects.keySet().toArray();
			Arrays.sort(pdfImageObjs);
			for (int i = 0; i < pdfImageObjs.length; i++)
			{
				String key = pdfImageObjs[i].toString();
				nChars += writeLine(m_writer, "/" + key +
					" " + (objIndex + counter) + " 0 R");
				counter++;
			}
			ArrayList<StringBuffer> includedImageObjects = new ArrayList<StringBuffer>();
			for (int i = 0; i < m_PDFIncludedFiles.size(); i++)
			{
				PDFFile pdfFile = (PDFFile)m_PDFIncludedFiles.get(i);
				ArrayList<Integer> pageNumbers = m_PDFIncludedPages.get(i);
				for (int j = 0; j < pageNumbers.size(); j++)
				{
					Integer pageNumber = (Integer)pageNumbers.get(j);
					ArrayList<StringBuffer> list = pdfFile.getXObject(pageNumber.intValue(),
						objIndex + counter);
					if (list != null && !list.isEmpty())
					{
						/*
						 * Include dictionary keys from external PDF files
						 * and any other objects that the keys refer to.
						 */
						nChars += writeLine(m_writer, list.get(0).toString());
						for (int k = 1; k < list.size(); k++)
						{
							includedImageObjects.add(list.get(k));
							counter++;
						}
					}
				}
			}
			nChars += writeLine(m_writer, ">>");
			nChars += writeLine(m_writer, "endobj");

			/*
			 * Write each graphics state and each image to PDF file.
			 */
			for (int i = 0; i < pdfExtGStateObjs.length; i++)
			{
				offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
				m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				Object key = pdfExtGStateObjs[i];
				nChars = writeLine(m_writer, objIndex + " 0 obj % " + key);
				nChars += writeLine(m_writer, m_PDFExtGStateObjects.get(key).toString());
				nChars += writeLine(m_writer, "endobj");
				objIndex++;
			}
			for (int i = 0; i < includedExtGstateObjects.size(); i++)
			{
				offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
				m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				String extGState = includedExtGstateObjects.get(i).toString();
				nChars = writeLine(m_writer, extGState);
				objIndex++;
			}
			for (int i = 0; i < includedColorSpaceObjects.size(); i++)
			{
				offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
				m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				String colorSpace = includedColorSpaceObjects.get(i).toString();
				nChars = writeLine(m_writer, colorSpace);
				objIndex++;
			}
			for (int i = 0; i < pdfImageObjs.length; i++)
			{
				offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
				m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				Object key = pdfImageObjs[i];
				nChars = writeLine(m_writer, objIndex + " 0 obj % " + key);
				BigString bigString = m_PDFImageObjects.get(key);
				nChars += bigString.writeTo(m_filename, m_writer);
				nChars += writeLine(m_writer, "");
				nChars += writeLine(m_writer, "endobj");
				objIndex++;
			}

			for (int i = 0; i < includedImageObjects.size(); i++)
			{
				offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
				m_PDFFileOffsets.add(new Integer(offset.intValue() + nChars));

				String image = includedImageObjects.get(i).toString();
				nChars = writeLine(m_writer, image);
				objIndex++;
			}

			/*
			 * Write cross reference table giving file offset of each
			 * object in PDF file.
			 */
			writeLine(m_writer, "xref");
			writeLine(m_writer, "0 " + (m_PDFFileOffsets.size() + 1));
			writeLine(m_writer, "0000000000 65535 f");
			Iterator<Integer> it = m_PDFFileOffsets.iterator();
			while (it.hasNext())
			{
				String fileOffset = it.next().toString();
				int padding = 10 - fileOffset.length();
				StringBuffer sb = new StringBuffer();
				while (padding-- > 0)
					sb.append('0');
				sb.append(fileOffset);
				sb.append(" 00000 n");
				writeLine(m_writer, sb.toString());
			}

			writeLine(m_writer, "trailer");
			writeLine(m_writer, "<<");
			writeLine(m_writer, "/Size " + (m_PDFFileOffsets.size() + 1));
			writeLine(m_writer, "/Root 1 0 R");
			writeLine(m_writer, "/Info 2 0 R");
			writeLine(m_writer, ">>");

			/*
			 * Write file offset of start of cross reference table.
			 */
			writeLine(m_writer, "startxref");
			offset = m_PDFFileOffsets.get(m_PDFFileOffsets.size() - 1);
			writeLine(m_writer, Integer.toString(offset.intValue() + nChars));
			writeLine(m_writer, "%%EOF");

			if (m_isStandardOutput)
				m_writer.flush();
			else
				m_writer.close();

			for (int i = 0; i < m_PDFIncludedFiles.size(); i++)
			{
				PDFFile pdfFile = (PDFFile)m_PDFIncludedFiles.get(i);
				pdfFile.close();
			}

			if (m_writer.checkError())
			{
				throw new MapyrusException(m_filename +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_PS));
			}
		}
		else if (m_outputType == SVG)
		{
			int nStates = m_SVGOpenGTags.pop().intValue();
			for (int i = 0; i < nStates; i++)
				writeLine(m_writer, "</g>");
			writeLine(m_writer, "</svg>");

			if (m_isStandardOutput)
				m_writer.flush();
			else
				m_writer.close();

			if (m_writer.checkError())
			{
				throw new MapyrusException(m_filename +
					": " + MapyrusMessages.get(MapyrusMessages.ERROR_SVG));
			}
		}
		else if (m_outputType == IMAGE_FILE)
		{
			/*
			 * If updating file then overwrite it now with new image.
			 */
			if (m_isUpdatingFile)
				m_outputStream = new FileOutputStream(m_filename);

			/*
			 * Write image buffer to file.
			 */
			ImageIOWrapper.write(m_image, m_formatName, m_outputStream);

			if (m_isStandardOutput)
				m_outputStream.flush();
			else
				m_outputStream.close();
		}
		else if (m_outputType == SCREEN_WINDOW)
		{
			/*
			 * Show image we have created in a window.
			 * Then wait for user to close the window.
			 */
			String title = Constants.PROGRAM_NAME + ": " + m_filename;
			new MapyrusFrame(title, m_image);
		}

		m_image = null;
		m_graphics2D = null;

		if (m_outputType != INTERNAL_IMAGE)
		{
			/*
			 * If we are piping output to another program then wait for
			 * that program to finish.  Then check that it succeeded.
			 */
			if (m_isPipedOutput)
			{
				int retval = 0;

				try
				{
					retval = m_outputProcess.waitFor();
				}
				catch (InterruptedException e)
				{
					throw new MapyrusException(m_filename + ": " + e.getMessage());
				}

				if (retval != 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.PROCESS_ERROR) +
						": " + retval + ": " + m_filename);
				}
			}
		}

		if (m_imageMapWriter != null)
		{
			m_imageMapWriter.close();
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
	 * @param lineSpacing spacing between lines in multi-line labels, as
	 * a multiple of the font size.
	 */
	public void setFontAttribute(String fontName, double fontSize,
		double fontRotation, double outlineWidth, double lineSpacing)
		throws IOException, MapyrusException
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY)
		{
			if (m_encodeAsISOLatin1.contains(fontName) && (!m_reencodedFonts.contains(fontName)))
			{
				/*
				 * Re-encode font from StandardEncoding to ISOLatin1Encoding
				 * before it is used.
				 */
				writeLine(m_writer, reencodeFont(fontName, "ISOLatin1Encoding"));
				m_reencodedFonts.add(fontName);
			}

			/*
			 * Set font and size for labelling.
			 */
			writeLine(m_writer, "/" + fontName + " " +
				fontSize + " " +
				fontRotation + " " +
				lineSpacing + " " +
				outlineWidth + " font");
			m_neededFontResources.add(fontName);
		}
		else if (m_outputType == PDF)
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
			Font currentFont = m_graphics2D.getFont();
			float newSize = (float)fontSize;
			float currentSize = currentFont.getSize2D();
			String currentFontName = currentFont.getName();
			if (newSize != currentSize ||
				style != currentFont.getStyle() ||
				(!fontName.equals(currentFontName)) ||
				fontRotation != m_fontRotation)
			{
				/*
				 * We need a base font that is not rotated for calculating
				 * string widths for justifying text.
				 * Get base font from cache, or create it if we don't find it there.
				 */
				m_baseFont = m_fontCache.get(fontName, style, newSize, 0);
				if (m_baseFont == null)
				{
					/*
					 * If this is a font for which user provided a TTF file then
					 * use that, else expect the operating system to be able to
					 * open the font.
					 */
					TrueTypeFont ttf = (TrueTypeFont)m_TTFFonts.get(fontName);
					if (ttf != null)
						m_baseFont = ttf.getFont().deriveFont(style, newSize);
					else
						m_baseFont = new Font(fontName, style, (int)newSize).deriveFont(newSize);
					m_fontCache.put(fontName, style, newSize, 0, m_baseFont);
				}

				/*
				 * The real font used for labelling must be mirrored in Y axis
				 * (to reverse the transform we use on Graphics2D objects) and
				 * rotated to the angle the user wants.
				 *
				 * Look it up in cache too.
				 */
				Font font = m_fontCache.get(fontName, style, -newSize, fontRotation);
				if (font == null)
				{
					AffineTransform fontTransform;
					fontTransform = AffineTransform.getRotateInstance(fontRotation);
					fontTransform.scale(1, -1);
					font = m_baseFont.deriveFont(fontTransform);
					m_fontCache.put(fontName, style, -newSize, fontRotation, font);
				}

				m_graphics2D.setFont(font);
			}
		}

		/*
		 * Some Font settings not easily held in a Graphics2D
		 * or PDF graphics state so keep track of current values ourselves.
		 */
		m_fontRotation = fontRotation;
		m_fontOutlineWidth = outlineWidth;
		m_fontLineSpacing = lineSpacing;
		m_fontName = fontName;
		m_fontSize = fontSize;
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
			m_justificationShiftX = JUSTIFY_LEFT;
		else if ((justify & JUSTIFY_CENTER) != 0)
			m_justificationShiftX = JUSTIFY_CENTER;
		else
			m_justificationShiftX = JUSTIFY_RIGHT;

		if ((justify & JUSTIFY_BOTTOM) != 0)
			m_justificationShiftY = JUSTIFY_BOTTOM;
		else if ((justify & JUSTIFY_MIDDLE) != 0)
			m_justificationShiftY = JUSTIFY_MIDDLE;
		else
			m_justificationShiftY = JUSTIFY_TOP;

		if (m_outputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Define dictionary entries for justification settings for PostScript
			 * procedure to use for aligning text correctly itself.
			 */
			writeLine(m_writer, m_justificationShiftX + " " + m_justificationShiftY + " ju");
		}
	}

	/**
	 * Set color in output format.
	 * @param color is color to draw in.
	 */
	public void setColorAttribute(Color color)
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
		{
			float c[] = color.getRGBColorComponents(null);
			PrintWriter pw;
			
			if (m_outputType == PDF)
				pw = m_PDFGeometryWriter;
			else
				pw = m_writer;

			StringBuffer sb = new StringBuffer();
			sb.append(m_coordinateDecimal.format(c[0]));
			sb.append(' ');
			sb.append(m_coordinateDecimal.format(c[1]));
			sb.append(' ');
			sb.append(m_coordinateDecimal.format(c[2]));
			writeLine(pw, sb.toString() + " RG");
			if (m_outputType == PDF)
			{
				writeLine(pw, sb.toString() + " rg");

				/*
				 * Write graphics state dictionary entry setting
				 * alpha to desired value.
				 *
				 * Alpha value is used in dictionary name so that
				 * repeated use of the same alpha value results in
				 * the same dictionary value being used.
				 */
				int alpha = color.getAlpha();
				String as = m_coordinateDecimal.format(alpha / 255.0);
				String gsKey = m_PDFGstatePrefix + alpha;
				m_PDFExtGStateObjects.put(gsKey, "<< /Type /ExtGState /CA " +
					as + " /ca " + as + " >>");

				/*
				 * Set graphics state in new dictionary entry.
				 */
				writeLine(pw, "/" + gsKey + " gs");
			}
		}
		else
		{
			m_graphics2D.setColor(color);
		}
	}

	/**
	 * Sets color blending mode.
	 * @param blend is color blending mode.
	 */
	public void setBlendAttribute(String blend)
	{
		blend = blend.toLowerCase().trim();
		if (m_outputType == PDF)
		{
			/*
			 * Ensure correct capitalisation of name.
			 */
			if (blend.equals("colordodge"))
				blend = "ColorDodge";
			else if (blend.equals("colorburn"))
				blend = "ColorBurn";
			else if (blend.equals("hardlight"))
				blend = "HardLight";
			else if (blend.equals("softlight"))
				blend = "SoftLight";
			else if (blend.length() > 0)
				blend = Character.toUpperCase(blend.charAt(0)) + blend.substring(1);

			/*
			 * Write graphics state dictionary entry setting
			 * blend mode to desired value.
			 *
			 * Blend mode is used in dictionary name so that
			 * repeated use of the same blend results in
			 * the same dictionary value being used.
			 */
			String gsKey = m_PDFGstatePrefix + blend;
			m_PDFExtGStateObjects.put(gsKey, "<< /Type /ExtGState /BM /" + blend + " >>");
			writeLine(m_PDFGeometryWriter, "/" + gsKey + " gs");
		}
		else if (m_outputType != POSTSCRIPT_GEOMETRY)
		{
			Composite comp = BlendComposite.getBlendComposite(blend);
			if (comp == null)
				comp = AlphaComposite.SrcOver;
			m_graphics2D.setComposite(comp);
		}
	}

	/**
	 * Set linestyle in output format.
	 * @param linestyle is Java2D line width, cap and join style, dash pattern.
	 */
	public void setLinestyleAttribute(BasicStroke linestyle)
	{
		double width = linestyle.getLineWidth();
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
		{
			PrintWriter pw;
			if (m_outputType == PDF)
				pw = m_PDFGeometryWriter;
			else
				pw = m_writer;

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

			if (width < m_minimumLineWidth)
				width = m_minimumLineWidth;
			writeLine(pw, m_coordinateDecimal.format(width) + " w " +
				cap + " J " + join + " j " +
				m_coordinateDecimal.format(linestyle.getMiterLimit()) + " M");

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
					s.append(m_coordinateDecimal.format(dashes[i]));
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
			if (width < m_minimumLineWidth)
			{
				float []dashes = linestyle.getDashArray();
				if (dashes == null)
				{
					linestyle = new BasicStroke((float)m_minimumLineWidth, linestyle.getEndCap(),
						linestyle.getLineJoin(), linestyle.getMiterLimit());
				}
				else
				{
					linestyle = new BasicStroke((float)m_minimumLineWidth, linestyle.getEndCap(),
							linestyle.getLineJoin(), linestyle.getMiterLimit(), dashes,
							linestyle.getDashPhase());
				}
			}
			m_graphics2D.setStroke(linestyle);
		}
	}

	/**
	 * Set clip path for output format.
	 * @param clipPaths are polygons to clip against, or null if
	 * there are no clip polygons.
	 */
	public void setClipAttribute(ArrayList<GeometricPath> clipPaths)
	{
		if (m_outputType != POSTSCRIPT_GEOMETRY && m_outputType != PDF && m_outputType != SVG)
		{
			m_graphics2D.setClip(null);
			boolean isClipPathActive = (clipPaths != null && clipPaths.size() > 0);
			if (isClipPathActive)
			{
				for (int i = 0; i < clipPaths.size(); i++)
				{
					GeometricPath clipPath = clipPaths.get(i);
					m_graphics2D.clip(clipPath.getShape());
				}
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
			pi = shape.getPathIterator(null, m_resolution);
		}
		else
		{
			pi = shape.getPathIterator(null);
		}

		float coords[] = new float[6];
		float lastX = 0.0f, lastY = 0.0f;
		float moveX = 0.0f, moveY = 0.0f;
		float x = 0.0f, y = 0.0f;
		float distSquared;
		float resolutionSquared = (float)(m_resolution * m_resolution);
		int segmentType = PathIterator.SEG_CLOSE;
		boolean skippedLastSegment = false;
		int moveCounter = 0;

		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			switch (segmentType)
			{
				case PathIterator.SEG_MOVETO:
					moveX = lastX = coords[0];
					moveY = lastY = coords[1];
					moveCounter++;
					if (outputType == SVG)
					{
						writeLine(pw,
							"M " + m_coordinateDecimal.format(lastX) +
							" " + m_coordinateDecimal.format(m_pageHeight - lastY));
					}
					else if (outputType == IMAGEMAP)
					{
						if (moveCounter == 1)
							m_imageMapWriter.print("<area shape=\"polygon\" coords=\"");
						else
							m_imageMapWriter.print(",");
						m_imageMapWriter.println(Math.round(lastX / m_resolution) + "," +
							Math.round((m_pageHeight - lastY) / m_resolution));
					}
					else
					{
						writeLine(pw,
							m_coordinateDecimal.format(lastX) +
							" " + m_coordinateDecimal.format(lastY) + " m");
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
							String sx = m_coordinateDecimal.format(x);
							String sy = m_coordinateDecimal.format(m_pageHeight - y);

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
							m_imageMapWriter.println("," + Math.round(x / m_resolution) +
								"," + Math.round((m_pageHeight - y) / m_resolution));
						}
						else
						{
							writeLine(pw, m_coordinateDecimal.format(x) +
								" " + m_coordinateDecimal.format(y) + " l");
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
							writeLine(pw, "L " + m_coordinateDecimal.format(x) +
								" " + m_coordinateDecimal.format(m_pageHeight - y));
						}
						else if (outputType == IMAGEMAP)
						{
							if (moveCounter > 0)
							{
								m_imageMapWriter.println("," + Math.round(x / m_resolution) +
									"," + Math.round((m_pageHeight - y) / m_resolution));
							}
						}
						else
						{
							writeLine(pw, m_coordinateDecimal.format(x) + " " +
								m_coordinateDecimal.format(y) + " l");
						}
					}

					if (outputType == SVG)
					{
						writeLine(pw, "z");
					}
					else if (outputType == IMAGEMAP)
					{
						if (moveCounter > 0)
						{
							/*
							 * Add first point of polygon again.
							 */
							m_imageMapWriter.println("," + Math.round(moveX / m_resolution) +
								"," + Math.round((m_pageHeight - moveY) / m_resolution));
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
						writeLine(pw, "C " + m_coordinateDecimal.format(coords[0]) + " " +
							m_coordinateDecimal.format(m_pageHeight - coords[1]) + " " +
							m_coordinateDecimal.format(coords[2]) + " " +
							m_coordinateDecimal.format(m_pageHeight - coords[3]) + " " +
							m_coordinateDecimal.format(coords[4]) + " " +
							m_coordinateDecimal.format(m_pageHeight - coords[5]));
					}
					else
					{
						writeLine(pw, m_coordinateDecimal.format(coords[0]) + " " +
							m_coordinateDecimal.format(coords[1]) + " " +
							m_coordinateDecimal.format(coords[2]) + " " +
							m_coordinateDecimal.format(coords[3]) + " " +
							m_coordinateDecimal.format(coords[4]) + " " +
							m_coordinateDecimal.format(coords[5]) + " " +
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
				writeLine(pw, "L " + m_coordinateDecimal.format(x) + " " +
					m_coordinateDecimal.format(m_pageHeight - y));
			}
			else if (outputType == IMAGEMAP)
			{
				if (moveCounter > 0)
				{
					m_imageMapWriter.println("," + Math.round(x / m_resolution) +
						"," + Math.round((m_pageHeight - y) / m_resolution));
				}
			}
			else
			{
				writeLine(pw, m_coordinateDecimal.format(x) +
					" " + m_coordinateDecimal.format(y) + " l");
			}
		}

		/*
		 * Complete any imagemap being created.
		 */
		if (outputType == IMAGEMAP && moveCounter > 0)
		{
			String imageMapString = "\" " + scriptCommands + " >";
			m_imageMapWriter.println(imageMapString);
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
	public void drawIcon(ArrayList<Point2D> pointList, BufferedImage image, double size,
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

		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
		{
			/*
			 * Draw icon at each position in list.
			 */
			for (i = 0; i < pointList.size(); i++)
			{
				pt = pointList.get(i);
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + mmWidth >= 0 && x - mmWidth <= m_pageWidth &&
					y + mmHeight >= 0.0 && y - mmHeight <= m_pageHeight)
				{
					writePostScriptOrPDFImage(image, x, y, mmWidth, mmHeight, rotation);
				}
			}
		}
		else if (m_outputType != SVG)
		{
			double mmPerPixel = Constants.MM_PER_INCH / Constants.getScreenResolution();
			double xScale = (mmWidth / mmPerPixel) / pixelWidth;
			double yScale = (mmHeight / mmPerPixel) / pixelHeight;

			int reduction = (int)Math.round(1 / xScale);
			int step = 1;
			Image imageToDisplay = image;
			while (reduction > 1)
			{
				reduction = reduction / 2;
				step = step * 2;
			}
			if (step > 2)
			{
				/*
				 * Java2D uses nearest-neighbour resampling to draw scaled
				 * icons.  This produces a poor result.
				 *
				 * For large icons drawn at small sizes, create a smaller,
				 * smoothed copy of the icon and draw that icon instead.
				 * This produces a better result.
				 */
				int reducedWidth = pixelWidth / step;
				int reducedHeight = pixelHeight / step;
				if (reducedWidth > 0 && reducedHeight > 0)
				{
					imageToDisplay = image.getScaledInstance(reducedWidth,
						reducedHeight, Image.SCALE_SMOOTH);
					xScale *= ((double)pixelWidth / reducedWidth);
					yScale *= ((double)pixelHeight / reducedHeight);
					pixelWidth = reducedWidth;
					pixelHeight = reducedHeight;
				}
			}

			for (i = 0; i < pointList.size(); i++)
			{
				pt = (Point2D)(pointList.get(i));
				x = pt.getX();
				y = pt.getY();
				AffineTransform affine = AffineTransform.getTranslateInstance(x, y);

				/*
				 * Scale transformation so that units are in pixels.
				 */
				affine.scale(mmPerPixel, mmPerPixel * -1);

				/*
				 * Rotate clockwise around point (x, y).
				 */
				affine.rotate(-rotation);

				/*
				 * Scale image to requested size.
				 */
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
					m_graphics2D.drawImage(imageToDisplay, affine, null);
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
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
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
			m_graphics2D.drawImage(image, affine, null);
		}
	}

	private void drawBoundingBoxes(ArrayList<Point2D> pointList, double size, double rotation)
	{
		GeneralPath path = new GeneralPath();
		Color currentColor = m_graphics2D.getColor();
		m_graphics2D.setColor(new Color(127, 127, 127, 127));

		for (int i = 0; i < pointList.size(); i++)
		{
			Point2D pt = pointList.get(i);
			double x = pt.getX();
			double y = pt.getY();
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
			fill(path, null);
		}
		m_graphics2D.setColor(currentColor);	
	}

	/**
	 * Draw EPS file at points on page.
	 * @param pointList is list of Point2D objects at which to draw EPS file.
	 * @param EPS filename.
	 * @param size size for EPS file on page in millimetres.
	 * @param rotation rotation angle for EPS file.
	 * @param scaling scale factor for EPS file.
	 */
	public void drawEPS(ArrayList<Point2D> pointList, String filename,
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

		if (m_outputType == POSTSCRIPT_GEOMETRY)
		{
			/*
			 * Include EPS file at each position in list.
			 */
			for (i = 0; i < pointList.size(); i++)
			{
				pt = pointList.get(i);
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + size >= 0 && x - size <= m_pageWidth &&
					y + size >= 0.0 && y - size <= m_pageHeight)
				{
					writeLine(m_writer, "save");
					writeLine(m_writer, x + " " + y + " translate");
					writeLine(m_writer, rotation + " radtodeg rotate");

					/*
					 * EPS file is centred at each point.
					 * Shift position left and down half it's size
					 * so that it is displayed centered.
					 */
					writeLine(m_writer, -(size / 2) + " " + -(size / 2) + " translate");

					double scale = size / Math.max(pointWidth, pointHeight);
					writeLine(m_writer, scale + " dup scale");

					/*
					 * Shift EPS file so that lower-left corner of EPS file is in
					 * lower left corner of our box on the page.
					 */
					writeLine(m_writer, -boundingBox.getMinX() + " " + -boundingBox.getMinY() +
						" translate");
						
					/*
					 * Set graphics attributes to initial values, as described
					 * on page 728 of PostScript Language Reference Manual.
					 */
					writeLine(m_writer, "/showpage {} def");
					writeLine(m_writer, "0 setgray 0 setlinecap 1 setlinewidth");
					writeLine(m_writer, "0 setlinejoin 10 setmiterlimit [] 0 setdash newpath");

					writeLine(m_writer, "%%BeginDocument: (" + filename + ")");
					BufferedReader reader = null;
					try
					{
						reader = new FileOrURL(filename).getReader();

						String line;
						while ((line = reader.readLine()) != null)
						{
							writeLine(m_writer, line);
						}
						writeLine(m_writer, "%%EndDocument");
						writeLine(m_writer, "restore");
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
			drawBoundingBoxes(pointList, size, rotation);
		}
	}

	/**
	 * Draw SVG file at points on page.
	 * @param pointList is list of Point2D objects at which to draw SVG file.
	 * @param SVG filename.
	 * @param size size for SVG file on page in millimetres.
	 * @param rotation rotation angle for SVG file.
	 * @param scaling scale factor for SVG file.
	 */
	public void drawSVG(ArrayList<Point2D> pointList, String filename,
		double size, double rotation, double scaling)
		throws IOException, MapyrusException
	{
		SVGFile svgfile = new SVGFile(filename);
		Rectangle2D boundingBox = svgfile.getBoundingBox();
		int pointWidth = (int)boundingBox.getWidth();
		int pointHeight = (int)boundingBox.getHeight();
		Point2D pt;
		int i;
		double x, y;

		/*
		 * If size not given then make SVG about as large as defined in the SVG file.
		 */
		if (size <= 0.0)
		{
			size = Math.max(pointWidth, pointHeight) *
				(Constants.MM_PER_INCH / Constants.POINTS_PER_INCH);
		}
		size *= scaling;

		if (m_outputType == SVG)
		{
			/*
			 * Include SVG file at each position in list.
			 */
			for (i = 0; i < pointList.size(); i++)
			{
				pt = pointList.get(i);
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + size >= 0 && x - size <= m_pageWidth &&
					y + size >= 0.0 && y - size <= m_pageHeight)
				{
					writeLine(m_writer, "<!-- begin " + filename + " -->");
					writeLine(m_writer, "<g");
					writeLine(m_writer, svgfile.getSVGAttributes());
					writeLine(m_writer, "transform=\"translate(" + x + "," + (m_pageHeight - y) + ")");
					writeLine(m_writer, "rotate(" + Math.toDegrees(-rotation) + ")");

					/*
					 * SVG file is centred at each point.
					 * Shift position left and down half it's size
					 * so that it is displayed centered.
					 */
					writeLine(m_writer, "translate(" + m_coordinateDecimal.format(-size / 2) +
						"," + m_coordinateDecimal.format(-size / 2) + ")");

					double scale = size / Math.max(pointWidth, pointHeight);
					writeLine(m_writer, "scale(" + scale + ")\">");

					writeLine(m_writer, svgfile.toString());
					writeLine(m_writer, "<!-- end " + filename + " -->");
					writeLine(m_writer, "</g>");
				}
			}
		}
		else
		{
			/*
			 * We cannot show SVG files when drawing to an image file so show a
			 * transparent grey box where the SVG file would appear.
			 */
			drawBoundingBoxes(pointList, size, rotation);
		}
	}

	/**
	 * Add Scalable Vector Graphics code to page.
	 * @param xml XML elements to add to SVG file.
	 */
	public void addSVGCode(String xml)
		throws IOException, MapyrusException
	{
		if (m_outputType == SVG)
		{
			writeLine(m_writer, "<!-- svgcode -->");
			writeLine(m_writer, xml);
		}
	}

	/**
	 * Draw PDF file at points on page.
	 * @param pointList is list of Point2D objects at which to draw PDF file.
	 * @param PDF filename.
	 * @param page page number in PDF file to display.
	 * @param size size for PDF file on page in millimetres.
	 * @param rotation rotation angle for PDF file.
	 * @param scaling scale factor for PDF file.
	 */
	public void drawPDF(ArrayList<Point2D> pointList, String filename, int page,
		double size, double rotation, double scaling)
		throws IOException, MapyrusException
	{
		/*
		 * If we have used this PDF file before then use same file,
		 * otherwise we need to open it.
		 */
		PDFFile pdfFile = null;
		int index = 0;
		if (m_outputType == PDF)
		{		
			while (index < m_PDFIncludedFiles.size() && pdfFile == null)
			{
				PDFFile p = (PDFFile)m_PDFIncludedFiles.get(index);
				if (p.getFilename().equals(filename))
					pdfFile = p;
				else
					index++;
			}
		}
		if (pdfFile == null)
		{
			pdfFile = new PDFFile(filename);
			if (m_outputType == PDF)
			{
				m_PDFIncludedFiles.add(pdfFile);
				m_PDFIncludedPages.add(new ArrayList<Integer>());
				index = m_PDFIncludedFiles.size() - 1;
			}
		}

		if (page < 1 || page > pdfFile.getPageCount())
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_NUMBER) +
				": " + pdfFile.getFilename() + ": " + page);
		}

		/*
		 * Add this page to the list of pages to display from the PDF file.
		 */
		if (m_outputType == PDF)
		{
			ArrayList<Integer> pageNumbers = m_PDFIncludedPages.get(index);
			Integer pageNumber = new Integer(page);
			if (!pageNumbers.contains(pageNumber))
				pageNumbers.add(pageNumber);
		}

		int[] boundingBox = pdfFile.getMediaBox(page);
		int pointWidth = boundingBox[2] - boundingBox[0];
		int pointHeight = boundingBox[3] - boundingBox[1];
		Point2D pt;
		int i;
		double x, y;

		/*
		 * If size not given then make PDF about as large as defined in the PDF file.
		 */
		if (size <= 0.0)
		{
			size = Math.max(pointWidth, pointHeight) *
				(Constants.MM_PER_INCH / Constants.POINTS_PER_INCH);
		}
		size *= scaling;

		if (m_outputType == PDF)
		{
			/*
			 * Include PDF file at each position in list.
			 */
			byte []contentsBuf = pdfFile.getContents(page);
			for (i = 0; i < pointList.size(); i++)
			{
				pt = pointList.get(i);
				x = pt.getX();
				y = pt.getY();

				/*
				 * Skip points that are outside page.
				 */
				if (x + size >= 0 && x - size <= m_pageWidth &&
					y + size >= 0.0 && y - size <= m_pageHeight)
				{
					/*
					 * PDF file is centred at each point.
					 * Shift position left and down half it's size
					 * so that it is displayed centered.
					 */
					writeLine(m_PDFGeometryWriter, "% " + filename);
					writeLine(m_PDFGeometryWriter, "q");
					setColorAttribute(Color.BLACK);
					writeLine(m_PDFGeometryWriter, "1 0 0 1 " +
						m_coordinateDecimal.format(x) + " " +
						m_coordinateDecimal.format(y) + " cm");

					double cosRotation = Math.cos(rotation);
					double sinRotation = Math.sin(rotation);
					writeLine(m_PDFGeometryWriter, cosRotation + " " + sinRotation +
						" " + (-sinRotation) + " " + cosRotation + " 0 0 cm");
					writeLine(m_PDFGeometryWriter, "1 0 0 1 " +
						-(size / 2) + " " + -(size / 2) + " cm");

					double scale = size / Math.max(pointWidth, pointHeight);
					writeLine(m_PDFGeometryWriter,
						scale + " 0 0 " + scale + " 0 0 cm");

					/*
					 * Shift EPS file so that lower-left corner of EPS file is in
					 * lower left corner of our box on the page.
					 */
					writeLine(m_PDFGeometryWriter, "1 0 0 1 " +
						-boundingBox[0] + " " + -boundingBox[1] + " cm");

					for (int j = 0; j < contentsBuf.length; j++)
						m_PDFGeometryWriter.write(contentsBuf[j]);
					writeLine(m_PDFGeometryWriter, " Q");
				}
			}
		}
		else
		{
			/*
			 * We cannot show PDF files when drawing to an image file so show a
			 * transparent grey box where the PDF file would appear.
			 */
			drawBoundingBoxes(pointList, size, rotation);
		}

	}

	/**
	 * Draw currently defined path to output page.
	 * @param shape
	 * @param xmlAttributes XML attributes to add for SVG output.
	 */
	public void stroke(Shape shape, String xmlAttributes)
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == SVG || m_outputType == PDF)
		{
			if (shape.intersects(0.0, 0.0, m_pageWidth, m_pageHeight))
			{
				if (m_outputType == SVG)
				{
					writeLine(m_writer, "<path d=\"");
					writeShape(shape, m_outputType, m_writer, null);
					writeLine(m_writer, "\"");
					Color color = m_graphics2D.getColor();
					BasicStroke stroke = (BasicStroke)m_graphics2D.getStroke();
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

					writeLine(m_writer, "  style=\"stroke:" + ColorDatabase.toHexString(color) +
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
							dashes.append(m_coordinateDecimal.format(dashArray[i]));
						}
						writeLine(m_writer, dashes.toString());
						writeLine(m_writer, ";stroke-dashoffset:" + dashPhase);
					}
					int alpha = color.getAlpha();
					if (alpha != 255)
					{
						writeLine(m_writer, ";stroke-opacity:" + (alpha / 255.0f));
					}

					writeLine(m_writer, ";fill:none\"");
					Composite comp = m_graphics2D.getComposite();
					if (comp instanceof BlendComposite)
					{
						BlendComposite blendComposite = (BlendComposite)comp;
						writeLine(m_writer, "filter=\"url(#" +
							blendComposite.getName() + ")\"");
					}

					if (xmlAttributes != null)
						writeLine(m_writer, xmlAttributes);
					writeLine(m_writer, "/>");
				}
				else
				{
					PrintWriter pw;
					if (m_outputType == PDF)
						pw = m_PDFGeometryWriter;
					else
						pw = m_writer;
					writeShape(shape, m_outputType, pw, null);
					writeLine(pw, "S");
				}
			}
		}
		else
		{
			/*
			 * Draw path into image.
			 */
			m_graphics2D.draw(shape);
		}
	}

	/**
	 * Fill currently defined path on output page.
	 * @param shape shape to fill on page.
	 * @param xmlAttributes XML attributes to add for SVG output.
	 * run when this shape is clicked.
	 */
	public void fill(Shape shape, String xmlAttributes)
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == SVG || m_outputType == PDF)
		{
			if (shape.intersects(0.0, 0.0, m_pageWidth, m_pageHeight))
			{
				if (m_outputType == SVG)
				{
					writeLine(m_writer, "<path d=\"");
					writeShape(shape, m_outputType, m_writer, null);
					writeLine(m_writer, "\"");
					Color color = m_graphics2D.getColor();
					int alpha = color.getAlpha();

					StringBuffer sb = new StringBuffer("  style=\"fill:");
					sb.append(ColorDatabase.toHexString(color));
					if (alpha != 255)
					{
						sb.append(";fill-opacity:" + (alpha / 255.0f));
					}
					sb.append(";stroke:none\" ");
					Composite comp = m_graphics2D.getComposite();
					if (comp instanceof BlendComposite)
					{
						BlendComposite blendComposite = (BlendComposite)comp;
						writeLine(m_writer, "filter=\"url(#" +
							blendComposite.getName() + ")\"");
					}

					if (xmlAttributes != null)
						sb.append(xmlAttributes);
					sb.append("/>");
					writeLine(m_writer, sb.toString());
				}
				else
				{
					PrintWriter pw;
					if (m_outputType == PDF)
						pw = m_PDFGeometryWriter;
					else
						pw = m_writer;
					writeShape(shape, m_outputType, pw, null);
					writeLine(pw, "f");
				}
			}
		}
		else
		{
			/*
			 * Fill path in image.
			 */
			m_graphics2D.fill(shape);
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
		if (m_imageMapWriter != null)
		{
			/*
			 * Write shape to image map together with script commands.
			 */
			writeShape(shape, IMAGEMAP, m_writer, scriptCommands);
		}
		else if (m_outputType == SVG)
		{
			/*
			 * Embed script commands in SVG file.
			 */
			writeLine(m_writer, "<path d=\"");
			writeShape(shape, m_outputType, m_writer, scriptCommands);
			writeLine(m_writer, "\"");
			writeLine(m_writer, scriptCommands);
			writeLine(m_writer, "/>");
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
		if (m_outputType == SVG)
		{
			if (shape.intersects(0.0, 0.0, m_pageWidth, m_pageHeight))
			{
				String uniqueId = "gradient" + m_gradientCounter++;
				writeLine(m_writer, "<defs>");
				writeLine(m_writer, "<linearGradient id=\"" + uniqueId + "\"");

				/*
				 * SVG supports only horizontal gradients (default) or vertical
				 * gradients.
				 */
				if (isVerticalGradient)
					writeLine(m_writer, "x1=\"0%\" y1=\"100%\" x2=\"0%\" y2=\"0%\"");

				writeLine(m_writer, ">");
				writeLine(m_writer, "<stop offset=\"0%\" stop-color=\"" +
					ColorDatabase.toHexString(c1) + "\"/>");
				writeLine(m_writer, "<stop offset=\"100%\" stop-color=\"" +
						ColorDatabase.toHexString(c2) + "\"/>");
				writeLine(m_writer, "</linearGradient>");
				writeLine(m_writer, "</defs>");
				writeLine(m_writer, "<path d=\"");
				writeShape(shape, m_outputType, m_writer, null);
				writeLine(m_writer, "\"");

				Composite comp = m_graphics2D.getComposite();
				if (comp instanceof BlendComposite)
				{
					BlendComposite blendComposite = (BlendComposite)comp;
					writeLine(m_writer, "filter=\"url(#" +
						blendComposite.getName() + ")\"");
				}
				writeLine(m_writer, "  fill=\"url(#" + uniqueId + ")\" stroke=\"none\"/>");
			}
		}
	}

	/**
	 * Set clip region to inside of currently defined path on output page.
	 */
	public void clip(Shape shape)
	{
		if (m_outputType == POSTSCRIPT_GEOMETRY || m_outputType == PDF)
		{
			PrintWriter pw;
			if (m_outputType == PDF)
				pw = m_PDFGeometryWriter;
			else
				pw = m_writer;

			/*
			 * Set clip path now, then it stays in effect until previous
			 * state is restored.
			 */
			if (shape.intersects(0.0, 0.0, m_pageWidth, m_pageHeight))
			{
				writeShape(shape, m_outputType, pw, null);
			}
			else
			{
				/*
				 * Clip region is outside page.  Clip to simple rectangle
				 * outside page instead so that nothing is shown.
				 */
				writeShape(new Rectangle2D.Float(-1.0f, -1.0f, 0.1f, 0.1f),
					m_outputType, pw, null);
			}
			writeLine(pw, "W n");
		}
		else if (m_outputType == SVG)
		{
			/*
			 * Ensure that each clip path gets a unique name.
			 */
			m_clipPathCounter++;
			writeLine(m_writer, "<clipPath id=\"" + m_SVGClipPathPrefix + m_clipPathCounter + "\">");
			writeLine(m_writer, "<path d=\"");
			writeShape(shape, m_outputType, m_writer, null);
			writeLine(m_writer, "\"/>");
			writeLine(m_writer, "</clipPath>");
			writeLine(m_writer, "<g clip-path=\"url(#" + m_SVGClipPathPrefix + m_clipPathCounter + ")\">");

			/*
			 * Increment number of "<g>" graphics states we have written so we
			 * know how many to remove later in the SVG file. 
			 */
			Integer nStates = m_SVGOpenGTags.pop();
			m_SVGOpenGTags.push(Integer.valueOf(nStates + 1));
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
		throws MapyrusException, IOException
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
				if (c > 'z')
				{
					/*
					 * Find character in PostScript font this character.
					 */
					if (m_adobeFontMetrics == null)
						m_adobeFontMetrics = new AdobeFontMetricsManager(m_afmFiles, m_encodeAsISOLatin1);
					c = m_adobeFontMetrics.getEncodedChar(m_fontName, c);
				}
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
	public void label(ArrayList<Point2D> pointList, String label) throws IOException, MapyrusException
	{
		Point2D pt, startPt;
		double x, y;
		double lastX = 0, lastY = 0;
		String line, nextLine;
		double lineNumber;
		AffineTransform affine;
		FontRenderContext frc = null;
		Stroke originalStroke = null;
		ArrayList<String> lines = new ArrayList<String>();

		BufferedReader stringReader = new BufferedReader(new StringReader(label));
		while ((line = stringReader.readLine()) != null)
			lines.add(line);

		if (m_outputType != POSTSCRIPT_GEOMETRY && m_outputType != PDF)
		{
			frc = m_graphics2D.getFontRenderContext();
			
			if (m_fontOutlineWidth > 0)
			{
				/*
				 * Save existing linestyle and create new one for drawing outlines of each letter.
				 */
				originalStroke = m_graphics2D.getStroke();
				BasicStroke outlineStroke = new BasicStroke((float)m_fontOutlineWidth,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, (float)FONT_OUTLINE_MITER_LIMIT);
				m_graphics2D.setStroke(outlineStroke);
			}
		}

		/*
		 * Calculate vertical shift to give label correct alignment.
		 */
		double yShift = 0;
		if (m_justificationShiftY == JUSTIFY_TOP)
		{
			yShift = -m_fontSize;
		}
		else if (m_justificationShiftY == JUSTIFY_MIDDLE)
		{
			yShift = lines.size() * m_fontSize +
				(lines.size() - 1) * m_fontSize *
				(m_fontLineSpacing - 1);
			yShift = yShift / 2 - m_fontSize;
		}
		else
		{
			yShift = (lines.size() - 1) * m_fontSize * m_fontLineSpacing;
		}

		/*
		 * Draw label at each position in list.
		 */
		for (int i = 0; i < pointList.size(); i++)
		{
			pt = (Point2D)(pointList.get(i));
			x = pt.getX();
			y = pt.getY();

			if (m_outputType == PDF)
			{
				/*
				 * Set transformation with text rotated and origin
				 * moved to positions for text.
				 */
				lastX = lastY = 0;
				writeLine(m_PDFGeometryWriter, "q");
				double cos = Math.cos(m_fontRotation);
				double sin = Math.sin(m_fontRotation);
				writeLine(m_PDFGeometryWriter, m_coordinateDecimal.format(cos) + " " +
					m_coordinateDecimal.format(sin) + " " +
					m_coordinateDecimal.format(-sin) + " " +
					m_coordinateDecimal.format(cos) + " " +
					m_coordinateDecimal.format(x) + " " +
					m_coordinateDecimal.format(y) + " cm");
				writeLine(m_PDFGeometryWriter, "BT");
				int j = 0, index = -1;
				while (j < PDF_FONTS.length && index < 0)
				{
					/*
					 * Is it one of the standard PDF fonts?
					 */
					if (PDF_FONTS[j].equals(m_fontName))
						index = j;
					else
						j++;
				}
				if (index < 0)
				{
					/*
					 * Or is it a font given by the user?
					 */
					for (int k = 0; k < m_PDFFonts.size() && index < 0; k++)
					{
						AdobeFontMetrics afm = (AdobeFontMetrics)m_PDFFonts.get(k);
						if (afm.getFontName().equals(m_fontName))
						{
							index = k + PDF_FONTS.length;
						}
					}
					if (index < 0)
						index = 4;
				}
				writeLine(m_PDFGeometryWriter, "/" + m_PDFFontPrefix + index +
					" " + m_coordinateDecimal.format(m_fontSize) + " Tf");
				if (m_fontOutlineWidth > 0)
				{
					writeLine(m_PDFGeometryWriter, "1 Tr " +
						m_coordinateDecimal.format(m_fontOutlineWidth) + " w " + FONT_OUTLINE_MITER_LIMIT + " M 2 J 0 j [] 0 d");
				}
				else
				{
					writeLine(m_PDFGeometryWriter, "0 Tr");
				}
			}

			/*
			 * Draw each line of label below the one above.
			 */
			Iterator<String> it = lines.iterator();
			lineNumber = 0;
			while (it.hasNext())
			{
				nextLine = (String)it.next();

				if (m_outputType == POSTSCRIPT_GEOMETRY)
				{
					writeLine(m_writer, m_coordinateDecimal.format(x) + " " +
						m_coordinateDecimal.format(y) + " m");

					/*
					 * Pass counter and line to PostScript procedure for
					 * drawing each line of the label.
					 */
					writeLine(m_writer, m_coordinateDecimal.format(lineNumber));
					writeLine(m_writer, Integer.toString(lines.size()));
					writePostScriptString(m_writer, null, nextLine);
					writeLine(m_writer, "t");
				}
				else if (m_outputType == PDF)
				{
					StringDimension dim = getStringDimension(nextLine, m_fontName, m_fontSize, 1);
					double x2 = 0;
					if (m_justificationShiftX == JUSTIFY_RIGHT)
						x2 = -dim.getWidth();
					else if (m_justificationShiftX == JUSTIFY_CENTER)
						x2 = -dim.getWidth() / 2.0;

					double y2 = yShift - lineNumber * m_fontSize * m_fontLineSpacing;
					writeLine(m_PDFGeometryWriter, m_coordinateDecimal.format(x2 - lastX) + " " +
						m_coordinateDecimal.format(y2 - lastY) + " Td");
					lastX = x2;
					lastY = y2;

					/*
					 * Draw each line of the label to PDF file.
					 */
					writePostScriptString(m_PDFGeometryWriter, null, nextLine);
					writeLine(m_PDFGeometryWriter, "Tj");
				}
				else if (m_outputType == SVG)
				{
					String anchor;
					if (m_justificationShiftX == JUSTIFY_RIGHT)
						anchor = "end";
					else if (m_justificationShiftX == JUSTIFY_LEFT)
						anchor = "start";
					else
						anchor = "middle";

					Color color = m_graphics2D.getColor();
					int alpha = color.getAlpha();
					Font font = m_graphics2D.getFont();

					StringBuffer extras = new StringBuffer();
					if (font.isBold())
						extras.append(" font-weight=\"bold\" ");
					if (font.isItalic())
						extras.append(" font-style=\"italic\" ");
					if (m_fontOutlineWidth > 0)
					{
						extras.append(" stroke=\"");
						extras.append(ColorDatabase.toHexString(color));
						extras.append("\" stroke-width=\"");
						extras.append(m_fontOutlineWidth);
						extras.append("\" stroke-miterlimit=\"" + FONT_OUTLINE_MITER_LIMIT + "\" ");

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

					double px, py;
					double y2 = y + yShift - lineNumber * m_fontSize * m_fontLineSpacing;

					if (m_fontRotation != 0)
					{
						/*
						 * Rotate text around origin point.
						 * Rotation is negative sense because Y axis
						 * decreases downwards.
						 */
						writeLine(m_writer, "<g transform=\"translate(" +
							m_coordinateDecimal.format(x) + ", " +
							m_coordinateDecimal.format(m_pageHeight - y2) +
							") rotate(" +
							m_coordinateDecimal.format(Math.toDegrees(-m_fontRotation)) +
							")\">");
						px = py = 0;
					}
					else
					{
						px = x;
						py = m_pageHeight - y2;
					}

					writeLine(m_writer, "<text x=\"" + m_coordinateDecimal.format(px) +
						"\" y=\"" + m_coordinateDecimal.format(py) +
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

					Composite comp = m_graphics2D.getComposite();
					if (comp instanceof BlendComposite)
					{
						BlendComposite blendComposite = (BlendComposite)comp;
						extras.append(" filter=\"url(#");
						extras.append(blendComposite.getName());
						extras.append(")\" ");
					}

					writeLine(m_writer, "  font-family=\"" + fontName + "\" " +
						"font-size=\"" + font.getSize2D() + "\" " +
						extras.toString());

					/*
					 * Make text string XML safe.
					 */
					StringBuffer sb = new StringBuffer(nextLine.length() * 2);
					for (int j = 0; j < nextLine.length(); j++)
					{
						char c = nextLine.charAt(j);
						if (c == '&' || c == '<' || c == '>' || c == '"' || c > 127)
						{
								/*
								 * Give character codes for special characters.
								 */
								sb.append("&#").append(Integer.toString(c)).append(";");
						}
						else
						{
								sb.append(c);
						}
					}
					writeLine(m_writer, ">" + sb.toString() + "</text>");

					if (m_fontRotation != 0)
						writeLine(m_writer, "</g>");
				}
				else
				{
					/*
					 * Reposition label from original point so it has correct justification.
					 */
					if (m_justificationShiftX != JUSTIFY_LEFT ||
						m_justificationShiftY != JUSTIFY_BOTTOM ||
						lines.size() > 1 || m_fontRotation != 0)
					{
						Rectangle2D bounds = m_baseFont.getStringBounds(nextLine, frc);
						affine = AffineTransform.getTranslateInstance(x, y);
						affine.rotate(m_fontRotation);
						double x2 = 0;
						if (m_justificationShiftX == JUSTIFY_RIGHT)
							x2 = -bounds.getWidth();
						else if (m_justificationShiftX == JUSTIFY_CENTER)
							x2 = -bounds.getWidth() / 2.0;

	   					startPt = new Point2D.Double(x2, yShift -
	   						lineNumber * m_fontSize * m_fontLineSpacing);
	   					affine.transform(startPt, startPt);
					}
					else
					{
						startPt = pt;
					}
					
					float fx = (float)startPt.getX();
					float fy = (float)startPt.getY();
					
					if (m_fontOutlineWidth > 0)
					{
						/*
						 * Draw only outline of letters in label as lines.
						 */
						GlyphVector glyphs = m_graphics2D.getFont().createGlyphVector(frc, nextLine);
						Shape outline = glyphs.getOutline(fx, fy);						
						m_graphics2D.draw(outline);
						
					}
					else
					{
						/*
						 * Draw plain label.
						 */
						m_graphics2D.drawString(nextLine, fx, fy);
					}
				}
				lineNumber++;
			}
			
			if (m_outputType == PDF)
				writeLine(m_PDFGeometryWriter, "ET Q");
		}

		if (originalStroke != null)
		{
			/*
			 * Restore line style.
			 */
			m_graphics2D.setStroke(originalStroke);
		}
	}
}
