/**
 * A context for interpretation.  Holds the graphics context (color, line styles,
 * transformations, etc.), the variables set by the user and connections to external
 * data sources.
 */

/*
 * $Id$
 */
import java.lang.String;
import java.util.Hashtable;
import java.lang.Math;
import java.awt.Color;
import java.io.IOException;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

public class Context
{
	/*
	 * Graphical settings
	 */	
	private Color mColor;
	private double mLineWidth;
	private boolean mAttributesChanged;
	
	/*
	 * Transformation matrix.
	 */
	private AffineTransform mCtm;
	
	/*
	 * Coordinates making up path.
	 */
	private GeometricPath mPath;

	/*
	 * Output device we are drawing to.
	 */
	private OutputFormat mOutputFormat;
		
	/*
	 * Currently defined variables.
	 */
	private Hashtable mVars;
	
	/**
	 * Create a new context with reasonable default values.
	 */
	public Context()
	{
		mColor = Color.GRAY;
		mLineWidth = 0.1;
	
		mCtm = new AffineTransform();
		mVars = new Hashtable();
		mPath = new GeometricPath();
		mOutputFormat = null;
		mAttributesChanged = true;
	}

	/*
	 * Set graphics attributes (color, line width, etc.) again if
	 * they have changed since the last time we drew something.
	 */
	private void setGraphicsAttributes()
	{
		if (mAttributesChanged)
		{
			mOutputFormat.setAttributes(mColor, mLineWidth);
			mAttributesChanged = false;
		}
	}

	/**
	 * Sets output file for drawing to.
	 * @param filename name of image file output will be saved to
	 * @param width is the page width (in points).
	 * @param height is the page height (in points).
	 * @param extras contains extra settings for this output.
	 */
	public void setOutputFormat(String filename,
		int width, int height, String extras)
		throws IOException, MapyrusException
	{
		closeOutputFormat();
		mOutputFormat = new OutputFormat(filename, width, height, extras);
		mAttributesChanged = true;
	}

	/**
	 * Closes any output being created.
	 */
	public void closeOutputFormat() throws IOException, MapyrusException
	{
		if (mOutputFormat != null)
		{
			mOutputFormat.closeOutputFormat();
			mOutputFormat = null;
		}
	}
					
	/**
	 * Sets line width.
	 * @param width is width for lines in millimetres.
	 */
	public void setLineWidth(double width)
	{
		mLineWidth = width;
		mAttributesChanged = true;
	}

	/**
	 * Sets color.
	 * @param c is new color for drawing.
	 */
	public void setColor(Color c)
	{
		mColor = c;
		mAttributesChanged = true;
	}
	
	/**
	 * Sets scaling for subsequent coordinates.
	 * @param x is new scaling in X axis.
	 * @param y is new scaling in Y axis.
	 */
	public void setScaling(double x, double y)
	{
		mCtm.scale(x, y);
		mAttributesChanged = true;
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void moveTo(double x, double y)
	{
		double srcPts[] = {x, y};
		float dstPts[] = new float[2];
		
		/*
		 * Transform point to millimetre position on page.
		 */
		mCtm.transform(srcPts, 0, dstPts, 0, 1);
		mPath.moveTo(dstPts[0], dstPts[1]);
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void lineTo(double x, double y)
	{
		double srcPts[] = {x, y};
		float dstPts[] = new float[2];

		/*
		 * Transform point to millimetre position on page.
		 */		
		mCtm.transform(srcPts, 0, dstPts, 0, 1);
		mPath.lineTo(dstPts[0], dstPts[1]);
	}

	/**
	 * Draw currently defined path.
	 */
	public void stroke()
	{
		setGraphicsAttributes();
		mOutputFormat.stroke(mPath.getShape());
	}

	/**
	 * Fill currently defined path.
	 */
	public void fill()
	{
		setGraphicsAttributes();
		mOutputFormat.fill(mPath.getShape());
	}
		
	/**
	 * Returns value of a variable.
	 * @param variable name to lookup.
	 * @return value of variable, or null if it is not defined.
	 */
	public Argument getVariableValue(String varName)
	{
		Argument retval;
		
		if (varName.startsWith(Mapyrus.PROGRAM_NAME))
		{
			/*
			 * Return internal/system variable.
			 */
			if (varName.equals(Mapyrus.PROGRAM_NAME + ".random"))
			{
				retval = new Argument(Math.random());
			}
			else
			{
				retval = new Argument(3.14);
			}
		}
		else
		{
			retval = (Argument)mVars.get(varName);
		}
		return(retval);
	}
	
	/**
	 * Define a variable, replacing any existing variable of the same name.
	 * @param varName name of variable to define.
	 * @param value is value for this variable
	 */
	public void defineVariable(String varName, Argument value)
	{
		mVars.put(varName, value);
	}
}
