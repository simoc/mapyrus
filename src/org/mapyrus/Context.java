/**
 * Maintains state information during interpretation inside a single procedure block. 
 * Holds the graphics attributes (color, line styles, transformations, etc.), the 
 * variables set by the user and connections to external data sources.
 */

/*
 * $Id$
 */
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.Hashtable;
import java.io.IOException;

public class Context
{
	/*
	 * Graphical attributes
	 */	
	private Color mColor;
	private double mLineWidth;
	
	/*
	 * Have graphical attributes been set in this context?
	 * Have graphical attributes been changed in this context
	 * since something was last drawn?
	 */
	private boolean mAttributesSet;
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
	 * Path in context from which this context was created.
	 * Used when path is not modified in this context to avoid
	 * needlessly copying paths from one context to another.
	 */
	private GeometricPath mExistingPath;

	/*
	 * Currently defined variables.
	 */
	private Hashtable mVars;

	/*
	 * Output device we are drawing to.
	 */
	private OutputFormat mOutputFormat;
	
	/*
	 * Flag true if output defined in this context.  In this case
	 * we must close the output file when this context is finished.
	 */
	private boolean mOutputDefined;
						
	/**
	 * Create a new context with reasonable default values.
	 */		
	public Context()
	{
		mColor = Color.GRAY;
		mLineWidth = 0.1;
	
		mCtm = new AffineTransform();
		mVars = null;
		mPath = null;
		mOutputFormat = null;
		mOutputDefined = false;
		mAttributesChanged = true;
		mAttributesSet = false;
	}

	/**
	 * Create a new context, making a copy from an existing context.
	 * @param existing is context to copy from.
	 */
	public Context(Context existing)
	{
		mColor = existing.mColor;
		mLineWidth = existing.mLineWidth;
		mCtm = new AffineTransform(existing.mCtm);
		
		/*
		 * Only create variable lookup table when values defined locally.
		 */
		mVars = null;

		/*
		 * Don't copy path -- it can be large.  Just keep reference to
		 * existing path.
		 * 
		 * Create path locally when needed.  If it is referenced without
		 * being created then take path from existing context instead.
		 * 
		 * This saves unnecessary copying of paths when contexts are created.
		 */
		mPath = null;
		if (existing.mPath != null)
			mExistingPath = existing.mPath;
		else
			mExistingPath = existing.mExistingPath;

		mOutputFormat = existing.mOutputFormat;
		mOutputDefined = false;

		mAttributesChanged = existing.mAttributesChanged;
		mAttributesSet = false;
	}
			
	/**
	 * Set graphics attributes (color, line width, etc.) if they
	 * have changed since the last time we drew something.
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
	 * Flag that graphics attributes have been changed.
	 */
	public void setAttributesChanged()
	{
		mAttributesChanged = mAttributesSet = true;
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
		mOutputFormat = new OutputFormat(filename, width, height, extras);
		mAttributesChanged = true;
		mOutputDefined = true;
	}

	/**
	 * Closes a context.  Any output started in this context is completed,
	 * memory used for context is released.
	 * A context cannot be used again after this call.
	 * @return flag indicating if graphical attributes set in this context.
	 */
	public boolean closeContext() throws IOException, MapyrusException
	{
		if (mOutputDefined)
		{
			mOutputFormat.closeOutputFormat();
			mOutputFormat = null;
			mOutputDefined = false;
		}
		mPath = null;
		mVars = null;
		return(mAttributesSet);
	}
					
	/**
	 * Sets line width.
	 * @param width is width for lines in millimetres.
	 */
	public void setLineWidth(double width)
	{
		mLineWidth = width;
		mAttributesChanged = mAttributesSet = true;
	}

	/**
	 * Sets color.
	 * @param c is new color for drawing.
	 */
	public void setColor(Color c)
	{
		mColor = c;
		mAttributesChanged = mAttributesSet = true;
	}
	
	/**
	 * Sets scaling for subsequent coordinates.
	 * @param x is new scaling in X axis.
	 * @param y is new scaling in Y axis.
	 */
	public void setScaling(double x, double y)
	{
		mCtm.scale(x, y);
		mAttributesChanged = mAttributesSet = true;
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
		if (mPath == null)
			mPath = new GeometricPath();
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
		if (mPath == null)
			mPath = new GeometricPath();
		mPath.lineTo(dstPts[0], dstPts[1]);
	}

	/**
	 * Clears currently defined path.
	 */
	public void clearPath()
	{
		if (mPath == null)
			mPath = new GeometricPath();
		else
			mPath.reset();
	}
		
	/**
	 * Draw currently defined path.
	 */
	public void stroke()
	{
		GeometricPath path;
		
		/*
		 * If path defined in this context then use that,
		 * else use context defined in previous context.
		 */
		if (mPath != null)
			path = mPath;
		else
			path = mExistingPath;
		
		if (path != null)
		{
			setGraphicsAttributes();
			mOutputFormat.stroke(path.getShape());
		}
	}

	/**
	 * Fill currently defined path.
	 */
	public void fill()
	{
		GeometricPath path;
		
		/*
		 * If path defined in this context then use that,
		 * else use context defined in previous context.
		 */
		if (mPath != null)
			path = mPath;
		else
			path = mExistingPath;
		
		if (path != null)
		{	
			setGraphicsAttributes();
			mOutputFormat.fill(path.getShape());
		}
	}
	
	/**
	 * Returns value of a variable.
	 * @param variable name to lookup.
	 * @return value of variable, or null if it is not defined.
	 */
	public Argument getVariableValue(String varName)
	{
		Argument retval;
		
		/*
		 * Variable is not set if no lookup table is defined.
		 */
		if (mVars == null)
			retval = null;
		else		
			retval = (Argument)mVars.get(varName);
			
		return(retval);
	}
	
	/**
	 * Define variable in current context, replacing any existing
	 * variable of the same name.
	 * @param varName name of variable to define.
	 * @param value is value for this variable
	 */
	public void defineVariable(String varName, Argument value)
	{
		/*
		 * Create new variable.
		 */
		if (mVars == null)
			mVars = new Hashtable();
		mVars.put(varName, value);
	}
}