/**
 * Abstraction of a graphics format.  Provides methods to create new
 * output files and then draw to them, independent of the graphics
 * format.
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;
 
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.PathIterator;
import java.awt.Shape;
import javax.imageio.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.awt.image.*;
import java.awt.Color;

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
	private static final double MM_PER_INCH = 25.4;

	/*
	 * Format for coordinates in PostScript files.
	 */	
	DecimalFormat mDecimalFormat;
		
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
		 * Set plotting units to millimetres.
		 */
		mWriter.println(POINTS_PER_INCH + " " + MM_PER_INCH + " div dup scale");
		
		/*
		 * Define shorter names for most commonly used operations.
		 */
		mWriter.println("/m { moveto } def /l { lineto } def");
	}

	/*
	 * Sets correct background, rendering hints and transformation
	 * for buffered image we will plot to.
	 */
	private void setupBufferedImage(int resolution)
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
	 * Return resolution to use for image files we create.
	 * @return resolution to use for images as dots per inch value.
	 */
	private int getResolution()
	{
		int resolution;
			
		/*
		 * If a display resolution is given as a property then use that,
		 * otherwise assume 72 DPI.  That is, an image 100mm wide will be made
		 * 720 pixels wide.
		 */
		try
		{
			String property = System.getProperty(Mapyrus.PROGRAM_NAME + ".resolution");
			if (property != null)
				resolution = Integer.parseInt(property);
			else
				resolution = POINTS_PER_INCH;
			
		}
		catch (SecurityException e)
		{
			resolution = POINTS_PER_INCH;
		}
		catch (NumberFormatException e)
		{
			resolution = POINTS_PER_INCH;
		}
		
		return(resolution);
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
	 */
	public OutputFormat(String filename, String format,
		double width, double height, String extras)
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
				throw new MapyrusException("Cannot write image format: " + format);
				
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
			mDecimalFormat = new DecimalFormat("#.##");
			mWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream)));
			writePostScriptHeader(width, height);
		}
		else
		{
			/*
			 * Create a BufferedImage to draw into.  We'll save it to a file
			 * when user has finished drawing to it.
			 */
			int resolution = getResolution();
			int widthInPixels = (int)Math.round(width / MM_PER_INCH * resolution);
			int heightInPixels = (int)Math.round(height / MM_PER_INCH * resolution);
			mImage = new BufferedImage(widthInPixels, heightInPixels,
				BufferedImage.TYPE_3BYTE_BGR);
			mGraphics2D = (Graphics2D)(mImage.getGraphics());
			setupBufferedImage(resolution);
		}
		mFilename = filename;
	}

	/**
	 * Set a buffered image as output.
	 * @param image is the image to draw to.
	 */
	public OutputFormat(BufferedImage image)
		throws IOException, MapyrusException
	{
		mOutputType = BUFFERED_IMAGE;
		mImage = image;
		mGraphics2D = (Graphics2D)(mImage.getGraphics());
		setupBufferedImage(getResolution());
		mPipedOutput = false;
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
				throw new MapyrusException("Error writing to PostScript file " +
					mFilename);
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
				throw new MapyrusException(e.getMessage() +
					" during output to " + mFilename);
			}
			
			if (retval != 0)
			{
				throw new MapyrusException(mFilename +
					"returned failure status " + retval);
			}
		}
	}
	
	/**
	 * Set graphics attributes.
	 * @param color is color to draw in.
	 * @param lineWidth is width of line to use for drawing.
	 */
	public void setAttributes(Color color, double lineWidth)
	{

		if (mOutputType == POSTSCRIPT)
		{
			mWriter.println(lineWidth + " setlinewidth");
			float c[] = color.getRGBColorComponents(null);
			mWriter.println(c[0] + " " + c[1] + " " + c[2] + " setrgbcolor");
		}
		else
		{
			mGraphics2D.setColor(color);
			mGraphics2D.setStroke(new BasicStroke((float)lineWidth));
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
					mWriter.println(mDecimalFormat.format(coords[0]) + " " +
						mDecimalFormat.format(coords[1]) + " m");
					break;
					
				case PathIterator.SEG_LINETO:
					mWriter.println(mDecimalFormat.format(coords[0]) + " " +
						mDecimalFormat.format(coords[1]) + " l");
					break;
				
				case PathIterator.SEG_CLOSE:
					mWriter.println("closepath");
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
			mWriter.println("stroke");
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
			mWriter.println("fill");
		}
		else
		{
			/*
			 * Fill path in image.
			 */
			mGraphics2D.fill(shape);
		}
	}
}
