/*
 * $Id$
 */
package au.id.chenery.mapyrus;
 
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.PathIterator;
import java.awt.Shape;
import javax.imageio.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
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
	 * Number of points and millimetres per inch.
	 */
	private static final int POINTS_PER_INCH = 72;
	public static final double MM_PER_INCH = 25.4;

	/*
	 * Format for coordinates and colors in PostScript files.
	 */	
	private DecimalFormat mLinearFormat;
	private DecimalFormat mColorFormat;
		
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
	 * Write PostScript file header.
	 */
	private void writePostScriptHeader(double width, double height)
	{
		long widthInPoints = Math.round(width / MM_PER_INCH * POINTS_PER_INCH);
		long heightInPoints = Math.round(height / MM_PER_INCH * POINTS_PER_INCH);
		
		mWriter.println("%!PS-Adobe-3.0");
		mWriter.println("%%BoundingBox: 0 0 " + widthInPoints + " " + heightInPoints);
		mWriter.println("%%DocumentData: Clean7Bit");
		mWriter.println("%%Creator: " + Mapyrus.PROGRAM_NAME);
		mWriter.println("%%EndComments");
		mWriter.println("");

		/*
		 * Prevent anything being displayed outside bounding box we've just defined.
		 */
		mWriter.println("0 0 " + widthInPoints + " " + heightInPoints + " rectclip");

		/*
		 * Set plotting units to millimetres.
		 */
		mWriter.println(POINTS_PER_INCH + " " + MM_PER_INCH + " div dup scale");
		
		/*
		 * Define shorter names for most commonly used operations.
		 */
		mWriter.println("/m { moveto } def /l { lineto } def");
		mWriter.println("/s { stroke } def /f { fill } def");
		mWriter.println("/gs { gsave } def /gr { grestore } def");
		mWriter.println("/rgb { setrgbcolor } def /sl { setlinewidth } def");
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
		
		scale = resolution / MM_PER_INCH;
		
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
			mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)));
			writePostScriptHeader(width, height);
		}
		else
		{
			/*
			 * Create a BufferedImage to draw into.  We'll save it to a file
			 * when user has finished drawing to it.
			 */
			int widthInPixels = (int)Math.round(width / MM_PER_INCH * resolution);
			int heightInPixels = (int)Math.round(height / MM_PER_INCH * resolution);
			mImage = new BufferedImage(widthInPixels, heightInPixels,
				BufferedImage.TYPE_3BYTE_BGR);
			mGraphics2D = (Graphics2D)(mImage.getGraphics());
			setupBufferedImage(resolution);
		}
		mFilename = filename;
		mPostScriptIndent = 0;
		mPageWidth = width;
		mPageHeight = height;
		mResolution = MM_PER_INCH / resolution;
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
	 * @param lineWidth is width of line to use for drawing.
	 * @param is clip path.
	 */
	public void setAttributes(Color color, double lineWidth, Shape clipPath)
	{

		if (mOutputType == POSTSCRIPT)
		{
			writePostScriptLine(mLinearFormat.format(lineWidth) + " sl");
			float c[] = color.getRGBColorComponents(null);
			writePostScriptLine(mColorFormat.format(c[0]) + " " +
				mColorFormat.format(c[1]) + " " +
				mColorFormat.format(c[2]) + " rgb");
		}
		else
		{
			mGraphics2D.setColor(color);
			mGraphics2D.setStroke(new BasicStroke((float)lineWidth));
			mGraphics2D.setClip(clipPath);
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
			writePostScriptShape(shape);
			writePostScriptLine("s");
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
			writePostScriptShape(shape);
			writePostScriptLine("f");
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
			writePostScriptShape(shape);
			writePostScriptLine("clip newpath");
		}
	}
}
