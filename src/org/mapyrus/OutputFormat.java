/**
 * Abstraction of a graphics format.  Provides methods to create new
 * output files and then draw to them, independent of the graphics
 * format.
 */

/*
 * $Id$
 */
 
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.PathIterator;
import java.awt.Shape;
import javax.imageio.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
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
	 * File or image that drawing commands are
	 * writing to.
	 */
	private int mOutputType;
	private String mFormatName;
	private BufferedImage mImage;
	private File mOutputFile;
	private PrintWriter mWriter;
	private Graphics2D mGraphics2D;
	
	/**
	 * Creates new graphics file, ready for drawing to.
	 * @param filename name of image file output will be saved to
	 * (suffix determines the graphics format used).
	 * @param width is the page width (in points).
	 * @param height is the page height (in points).
	 * @param extras contains extra settings for this output.
	 */
	public OutputFormat(String filename,
		int width, int height, String extras)
		throws IOException, MapyrusException
	{
		int dotIndex;
		
		/*
		 * Determine the type of output to create from
		 * filename suffix.
		 */
		dotIndex = filename.lastIndexOf('.');
		if (dotIndex < 0)
		{
			/*
			 * Use PNG format if there's no suffix.
			 */
			mFormatName = "PNG";
		}
		else
		{
			mFormatName = filename.substring(dotIndex + 1).toUpperCase();
		}
	
		/*
		 * Are we writing a PostScript file or an image file?
		 */
		if (mFormatName.equals("PS") || mFormatName.equals("EPS"))
		{
			mOutputType = POSTSCRIPT;
			mWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			writePostScriptHeader(width, height);
		}
		else
		{
			/*
			 * Check that Java can write this image format to a file.
			 */
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
				throw new MapyrusException("Cannot write image format: " + mFormatName);
			}

			/*
			 * Create a BufferedImage to draw into.  We'll save it to a file
			 * when user has finished drawing to it.
			 */
			mOutputType = IMAGE_FILE;
			mImage = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
			mGraphics2D = (Graphics2D)(mImage.getGraphics());
		}
		mOutputFile = new File(filename);
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
	}
	 
	/**
	 * Closes current output that was opened with setOutput.
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
					mWriter.println("showpage");
			}
			mWriter.println("%%EOF");
			mWriter.close();
			
			if (mWriter.checkError())
			{
				throw new MapyrusException("Error writing to PostScript file " +
					mOutputFile.toString());
			}
		}
		else if (mOutputType == IMAGE_FILE)
		{
			/*
			 * Write image buffer to file.
			 */
			ImageIO.write(mImage, mFormatName, mOutputFile);
			mImage = null;
			mGraphics2D = null;
		}
	}
	
	/*
	 * Write PostScript file header.
	 */
	private void writePostScriptHeader(int width, int height)
	{
		mWriter.println("%!PS-Adobe-3.0");
		mWriter.println("%%BoundingBox: 0 0 " + width + " " + height);
		mWriter.println("%%DocumentData: Clean7Bit");
		mWriter.println("%%Creator: " + Mapyrus.PROGRAM_NAME);
		mWriter.println("%%EndComments");
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
	
	/**
	 * Draw currently defined path to output page.
	 */
	public void stroke(Shape shape)
	{
		if (mOutputType == POSTSCRIPT)
		{
			/*
			 * Walk through path, converting it to PostScript.
			 */
			PathIterator pi = shape.getPathIterator(null);
			float coords[] = new float[6];
			int segmentType;
			
			while (!pi.isDone())
			{
				segmentType = pi.currentSegment(coords);
				switch (segmentType)
				{
					case PathIterator.SEG_MOVETO:
						mWriter.println(coords[0] + " " + coords[1] + " moveto");
						break;
						
					case PathIterator.SEG_LINETO:
						mWriter.println(coords[0] + " " + coords[1] + " lineto");
						break;
					
					case PathIterator.SEG_CLOSE:
						mWriter.println("closepath");
						break;
				}
				pi.next();			
			}
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
}
