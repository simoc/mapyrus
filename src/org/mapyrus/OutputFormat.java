/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;
 
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import javax.imageio.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
	private static final int BUFFERED_IMAGE = 1;
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
	private boolean mPipedOutput;	
	private Process mOutputProcess;
	
	/*
	 * Frequently used fonts.
	 */
	private FontCache mFontCache;

	/*
	 * List of fonts used in PostScript file to add at end of PostScript
	 * file as required resources.
	 */
	private HashSet mFontResources;
	
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
	
	/*
	 * Write PostScript file header, including document structuring conventions (DSC).
	 */
	private void writePostScriptHeader(double width, double height)
	{
		long widthInPoints = Math.round(width / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);
		long heightInPoints = Math.round(height / Constants.MM_PER_INCH *
			Constants.POINTS_PER_INCH);

		mWriter.print("%!PS-Adobe-3.0");
		if (mFormatName.equals("EPS"))
			mWriter.print(" EPSF-3.0");
		mWriter.println("");

		mWriter.println("%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);
		mWriter.println("%%DocumentData: Clean7Bit");
		mWriter.println("%%LanguageLevel: 2");
		mWriter.println("%%Creator: " + Mapyrus.PROGRAM_NAME);
		mWriter.println("%%DocumentRequiredResources: (atend)");
		mWriter.println("%%EndComments");
		mWriter.println("");

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
		 */
		mWriter.println("/m { moveto } def /l { lineto } def");
		mWriter.println("/s { stroke } def /f { fill } def");
		mWriter.println("/font { /fjy exch def /fjx exch def /frot exch def");
		mWriter.println("/fsize exch def findfont fsize scalefont setfont } def");
		mWriter.println("/radtodeg { 180 mul 3.1415629 div } def");
		mWriter.println("% Rotate and justify text using current font settings");
		mWriter.println("/t { gsave currentpoint translate frot radtodeg rotate");
		mWriter.println("dup stringwidth pop fjx mul fsize fjy mul rmoveto");
		mWriter.println("show grestore newpath } def");
		mWriter.println("/gs { gsave } def /gr { grestore } def");
		mWriter.println("/rgb { setrgbcolor } def");
		mWriter.println("/sl { setmiterlimit setlinejoin setlinecap setlinewidth } def");
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
	 * Creates new graphics file, ready for drawing to.
	 * @param filename name of image file output will be saved to.
	 * If filename begins with '|' character then output is piped as
	 * input to that command.
	 * @param format is the graphics format to use.
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param resolution is resolution for output in dots per inch (DPI)
	 * @param extras contains extra settings for this output.
	 */
	public OutputFormat(String filename, String format,
		double width, double height, double resolution, String extras)
		throws IOException, MapyrusException
	{
		mFormatName = format.toUpperCase();

		/*
		 * Check that Java can write this image format to a file.
		 */				
		if (mFormatName.equals("PS") || mFormatName.equals("EPS"))
		{
			mOutputType = POSTSCRIPT;
		}	
		else
		{
			boolean found = false;
			String knownFormats[] = ImageIO.getWriterFormatNames();
			for (int i = 0; i < knownFormats.length && found == false; i++)
			{
				if (mFormatName.equalsIgnoreCase(knownFormats[i]))
				{
					found = true;
				}
			}

			if (found == false)
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
		mPipedOutput = filename.startsWith("|");
		if (mPipedOutput)
		{
			String pipeCommand = filename.substring(1).trim();
			mOutputProcess = Runtime.getRuntime().exec(pipeCommand);
			mOutputStream = mOutputProcess.getOutputStream();
		}
		else
		{
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
			writePostScriptHeader(width, height);
			mPostScriptIndent = 0;
			mFontResources = new HashSet();
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
		}
		mFilename = filename;
		mPageWidth = width;
		mPageHeight = height;
		mResolution = Constants.MM_PER_INCH / resolution;
		mFontCache = new FontCache();
		mJustificationShiftX = mJustificationShiftY = mFontRotation = 0.0;
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
			mWriter.print(" ");
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
	 * Writes trailing information and closes output file.
	 */
	public void closeOutputFormat() throws IOException, MapyrusException
	{
		if (mOutputType == POSTSCRIPT)
		{
			/*
			 * Finish off PostScript file.
			 */
			if (mFormatName.equals("PS"))
			{
				/*
				 * showpage is not included in Encapsulated PostScript files.
				 */
				mWriter.println("showpage");
			}

			mWriter.println("%%Trailer");	
			mWriter.println("%%DocumentNeededResources:");
			Iterator it = mFontResources.iterator();
			while (it.hasNext())
				mWriter.println("%%+ font " + (String)(it.next()));
			mWriter.println("%%EOF");
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
			mOutputStream.close();
			mImage = null;
			mGraphics2D = null;
		}
		
		/*
		 * If we are piping output to another program then wait for
		 * that program to finish.  Then check that it succeeded.
		 */
		if (mPipedOutput)
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
	 * Set graphics attributes.
	 * @param color is color to draw in.
	 * @param linestyle is Java2D line width, cap and join style, dash pattern.
	 * @param justify is label justification value, combination of JUSTIFY_* bit flags.
	 * @param fontName is name of font as defined in java.awt.Font class.
	 * @param fontStyle is a style as defined in java.awt.Font class.
	 * @param fontSize is size for labelling in millimetres.
	 * @param fontRotation is rotation angle for font, in degrees,
	 * measured counter-clockwise.
	 * @param is clip path.
	 */
	public void setAttributes(Color color, BasicStroke linestyle, int justify,
		String fontName, int fontStyle, double fontSize, double fontRotation,
		Shape clipPath)
	{
		int cap, join;
		String styleName;
		Font font;
		int pointSize;

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

		mFontRotation = fontRotation;

		if (mOutputType == POSTSCRIPT)
		{
			/*
			 * Convert BasicStroke end cap and line join values to PostScript.
			 */
			cap = linestyle.getEndCap();
			if (cap == BasicStroke.CAP_BUTT)
				cap = 0;
			else if (cap == BasicStroke.CAP_ROUND)
				cap = 1;
			else /* SQUARE */
				cap = 2;

			join = linestyle.getLineJoin();
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

			/*
			 * Set colour.
			 */
			float c[] = color.getRGBColorComponents(null);
			writePostScriptLine(mColorFormat.format(c[0]) + " " +
				mColorFormat.format(c[1]) + " " +
				mColorFormat.format(c[2]) + " rgb");
				
			/*
			 * Set font for labelling.  Define dictionary entries for justification
			 * settings for PostScript procedure to use for aligning text correctly
			 * itself.
			 */
			if (fontStyle == Font.BOLD)
				styleName = "Bold";
			else if (fontStyle == Font.ITALIC)
				styleName = "Italic";
			else if (fontStyle == (Font.BOLD | Font.ITALIC))
				styleName = "BoldItalic";
			else
				styleName = "";

			String psFontName = "/" + fontName + styleName;

			writePostScriptLine(psFontName + " " +
				mLinearFormat.format(fontSize) + " " +
				mRotationFormat.format(fontRotation) + " " +
				mJustificationShiftX + " " + mJustificationShiftY + " font");
			mFontResources.add(psFontName);
		}
		else
		{
			/*
			 * Get font from cache, or create it if it is not in cache.
			 * Font size is defined in millimetres because it will later be
			 * displayed with a Graphics2D object scaled to millimetre units.
			 */
			int size = (int)Math.round(fontSize);
			font = mFontCache.get(fontName, fontStyle, size, fontRotation);
			if (font == null)
			{
				font = new Font(fontName, fontStyle, size);

				/*
				 * Mirror Y axis of fonts so that they can be displayed
				 * with a Graphics2D object that has inverted the Y axis to
				 * increase upwards.
				 */
				AffineTransform fontTransform =
					AffineTransform.getRotateInstance(fontRotation);
				fontTransform.scale(1, -1);
				font = font.deriveFont(fontTransform);
				mFontCache.put(fontName, fontStyle, size, fontRotation, font);
			}

			/*
			 * Set color, linestyle, font and clip path for graphics context.
			 */
			mGraphics2D.setColor(color);
			mGraphics2D.setStroke(linestyle);
			mGraphics2D.setClip(clipPath);
			mGraphics2D.setFont(font);
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
				 * outisde page instead so that nothing is shown.
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
				buffer.delete(0, buffer.length());
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
		Point2D pt;
		double x, y;

		/*
		 * Draw label at each position in list.
		 */
		for (int i = 0; i < pointList.size(); i++)
		{
			pt = (Point2D)(pointList.get(i));
			x = pt.getX();
			y = pt.getY();

			if (mOutputType == POSTSCRIPT)
			{
				writePostScriptLine(mLinearFormat.format(x) + " " +
					mLinearFormat.format(y) + " m");
// TODO need to split multiline strings.
				writePostScriptString(label);
				writePostScriptLine("t");
			}
			else
			{
				/*
				 * Reposition label from original point so it has correct justification.
				 */
				if (mJustificationShiftX != 0.0 || mJustificationShiftY != 0.0)
				{
   					FontMetrics fontMetrics = mGraphics2D.getFontMetrics();
   					Rectangle2D bounds = fontMetrics.getStringBounds(label, mGraphics2D);
   					x += bounds.getWidth() * mJustificationShiftX * Math.cos(mFontRotation) +
   						bounds.getHeight() * mJustificationShiftY * Math.sin(mFontRotation);
					y += bounds.getWidth() * mJustificationShiftX * Math.sin(mFontRotation) +
						bounds.getHeight() * mJustificationShiftY * Math.sin(mFontRotation);
				}
				mGraphics2D.drawString(label, (float)x, (float)y);
			}
		}
	}
}
