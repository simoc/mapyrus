/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.io.IOException;
import java.util.ArrayList;
import au.id.chenery.mapyrus.dataset.GeographicDataset;

/**
 * Maintains state information during interpretation inside a single procedure block. 
 * Holds the graphics attributes (color, line styles, transformations, etc.), the 
 * variables set by the user and connections to external data sources.
 */
public class Context
{
	/*
	 * Units of world coordinate system.
	 */
	public static final int WORLD_UNITS_METRES = 1;
	public static final int WORLD_UNITS_FEET = 2;

	/*
	 * Projection transformation may results in some strange warping.
	 * To get a better estimate of extents when projecting to world coordinate
	 * system we project many points in a grid to find minimum and maximum values.
	 */
	private static final int PROJECTED_GRID_STEPS = 5;

	/*
	 * Page size and resolution to use when no output page defined.
	 */
	private static final int DEFAULT_PAGE_WIDTH = 210;
	private static final int DEFAULT_PAGE_HEIGHT = 297;
	private static final int DEFAULT_RESOLUTION = 96;

	/*
	 * Fixed miter limit for line joins.
	 */
	private static final float MITER_LIMIT = 10.0f;

	/*
	 * Graphical attributes
	 */	
	private Color mColor;
	private BasicStroke mLinestyle;
	private int mJustify;
	private String mFontName;
	private int mFontStyle;
	private double mFontSize;
	private double mFontRotation;

	/*
	 * Have graphical attributes been set in this context?
	 * Have graphical attributes been changed in this context
	 * since something was last drawn?
	 */
	private boolean mAttributesSet;
	private boolean mAttributesChanged;
	
	/*
	 * Transformation matrix and cumulative scaling factors and rotation.
	 */
	private AffineTransform mCtm;
	private double mXScaling;
	private double mYScaling;
	private double mScalingMagnitude;
	private double mRotation;

	/*
	 * Projection transformation from one world coordinate system to another.
	 */
	private WorldCoordinateTransform mProjectionTransform;

	/*
	 * Transformation matrix from world coordinates to page coordinates
	 * and the units of world coordinates.
	 */
	private AffineTransform mWorldCtm;
	private Rectangle2D.Double mWorldExtents;
	private int mWorldUnits;
	
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
	 * Coordinates making up the clipping path.
	 */
	private GeometricPath mClippingPath;
	
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

	/*
	 * Dataset currently being read from, the next row to provide to caller
	 * and the number of rows already fetched from it.
	 */
	private GeographicDataset mDataset;
	private Row mDatasetRow;
	private int mDatasetRowCount;

	/**
	 * Create a new context with reasonable default values.
	 */		
	public Context()
	{
		mColor = Color.GRAY;
		mLinestyle = new BasicStroke();
		mJustify = OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_BOTTOM;
		mFontName = "SansSerif";
		mFontStyle = Font.PLAIN;
		mFontSize = 5;
		mFontRotation = 0;
	
		mCtm = new AffineTransform();
		mProjectionTransform = null;
		mWorldCtm = null;
		mXScaling = mYScaling = mScalingMagnitude = 1.0;
		mRotation = 0.0;
		mVars = null;
		mPath = null;
		mClippingPath = null;
		mOutputFormat = null;
		mOutputDefined = false;
		mAttributesChanged = true;
		mAttributesSet = false;
		mDataset = null;
		mDatasetRow = null;
		mDatasetRowCount = 0;
	}

	/**
	 * Create a new context, making a copy from an existing context.
	 * @param existing is context to copy from.
	 */
	public Context(Context existing)
	{
		mColor = existing.mColor;
		mLinestyle = existing.mLinestyle;
		mJustify = existing.mJustify;
		mFontName = existing.mFontName;
		mFontStyle = existing.mFontStyle;
		mFontSize = existing.mFontSize;
		mFontRotation = existing.mFontRotation;

		mCtm = new AffineTransform(existing.mCtm);
		mProjectionTransform = null;
		mWorldCtm = null;
		mXScaling = existing.mXScaling;
		mYScaling = existing.mYScaling;
		mScalingMagnitude = existing.mScalingMagnitude;
		mRotation = existing.mRotation;
		mDataset = null;
		mDatasetRow = null;
		mDatasetRowCount = 0;

		/*
		 * Only create variable lookup table when some values are
		 * defined locally.
		 */
		mVars = null;

		/*
		 * Don't copy path -- it can be large.
		 * Just keep reference to existing path.
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
			
		mClippingPath = existing.mClippingPath;
			
		mOutputFormat = existing.mOutputFormat;
		mOutputDefined = false;

		/*
		 * Save state in parent context so it won't be disturbed by anything
		 * that gets changed in this new context.
		 */		
		if (mOutputFormat != null)
		{
			mOutputFormat.saveState();
		}

		mAttributesChanged = existing.mAttributesChanged;
		mAttributesSet = false;
	}

	private GeometricPath getDefinedPath()
	{
		GeometricPath retval;
		
		/*
		 * Return path defined in this context, or one defined
		 * in previous context if nothing set here.
		 */
		if (mPath != null)
			retval = mPath;
		else
			retval = mExistingPath;
		return(retval);
	}

	/**
	 * Return page width for output we are currently writing.
	 * @return width in millimetres.
	 */
	public double getPageWidth()
	{
		double retval;
		
		if (mOutputFormat == null)
			retval = 0.0;
		else
			retval = mOutputFormat.getPageWidth();
		
		return(retval);
	}

	/**
	 * Return page height for output we are currently writing.
	 * @return height in millimetres.
	 */
	public double getPageHeight()
	{
		double retval;
		
		if (mOutputFormat == null)
			retval = 0.0;
		else
			retval = mOutputFormat.getPageWidth();
		
		return(retval);
	}

	/**
	 * Return resolution of page we are writing to as a distance measurement.
	 * @return distance in millimetres between centres of adjacent pixels.
	 */
	public double getResolution() throws MapyrusException
	{
		double retval;
		
		if (mOutputFormat == null)
			retval = Constants.MM_PER_INCH / DEFAULT_RESOLUTION;
		else
			retval = mOutputFormat.getResolution();

		return(retval);
	}

	/**
	 * Set graphics attributes (color, line width, etc.) if they
	 * have changed since the last time we drew something.
	 */
	private void setGraphicsAttributes()
	{
		if (mAttributesChanged)
		{
			Shape clip;
			
			if (mClippingPath != null)
				clip = mClippingPath.getShape();
			else
				clip = null;
				
			mOutputFormat.setAttributes(mColor, mLinestyle, mJustify,
				mFontName, mFontStyle, mFontSize, mFontRotation, clip);
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
	 * @param filename name of image file output will be saved to.
	 * @param format is image format for saving output.
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param resolution is resolution for output in dots per inch (DPI)
	 * @param extras contains extra settings for this output.
	 */
	public void setOutputFormat(String format, String filename,
		int width, int height, int resolution, String extras)
		throws IOException, MapyrusException
	{
		mOutputFormat = new OutputFormat(filename, format,
			width, height, resolution, extras);
		mAttributesChanged = true;
		mOutputDefined = true;
	}

	/**
	 * Closes a context.  Any output started in this context is completed,
	 * memory used for context is released.
	 * A context cannot be used again after this call.
	 * @return flag true if graphical attributes were set in this context
	 * and cannot be restored.
	 */
	public boolean closeContext() throws IOException, MapyrusException
	{
		boolean restoredState;

		if (mOutputFormat != null && !mOutputDefined)
		{
			/*
			 * If state could be restored then no need for caller set
			 * graphical attributes back to their old values again.
			 */
			restoredState = mOutputFormat.restoreState();
			if (restoredState)
				mAttributesSet = false;
		}
		
		if (mOutputDefined)
		{
			mOutputFormat.closeOutputFormat();
			mOutputFormat = null;
			mOutputDefined = false;
		}
		mPath = null;
		mClippingPath = null;
		mVars = null;
		return(mAttributesSet);
	}
					
	/**
	 * Sets linestyle.
	 * @param width is width for lines in millimetres.
	 * @param cap is a BasicStroke end cap value.
	 * @param join is a BasicStroke line join value.
	 * @param phase is offset at which pattern is started.
	 * @param dashes list of dash pattern lengths.
	 */
	public void setLinestyle(double width, int cap, int join,
		double phase, float []dashes)
	{
		/*
		 * Adjust width and dashes by current scaling factor.
		 */
		if (dashes == null)
		{
			mLinestyle = new BasicStroke((float)(width * mScalingMagnitude),
				cap, join, MITER_LIMIT);
		}
		else
		{
			for (int i = 0; i < dashes.length; i++)
				dashes[i] *= mScalingMagnitude;

			mLinestyle = new BasicStroke((float)(width * mScalingMagnitude), cap, join,
				MITER_LIMIT, dashes, (float)phase);
		}
		mAttributesChanged = mAttributesSet = true;
	}

	/**
	 * Sets color.
	 * @param c is new color for drawing.
	 */
	public void setColor(Color color)
	{
		mColor = color;
		mAttributesChanged = mAttributesSet = true;
	}

	/**
	 * Sets font for labelling with.
	 * @param fontName is name of font as defined in java.awt.Font class.
	 * @param fontStyle is a style as defined in java.awt.Font class.
	 * @param fontSize is size for labelling in millimetres.
	 */
	public void setFont(String fontName, int fontStyle, double fontSize)
	{
		mFontName = fontName;
		mFontStyle = fontStyle;
		mFontSize = fontSize * mScalingMagnitude;
		mFontRotation = mRotation;
		mAttributesChanged = mAttributesSet = true;
	}

	/**
	 * Sets horizontal and vertical justification for labelling.
	 * @param code is bit flags of JUSTIFY_* constant values for justification.
	 */
	public void setJustify(int code)
	{
		mJustify = code;
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
		mXScaling *= x;
		mYScaling *= y;
		mScalingMagnitude = Math.max(Math.abs(mXScaling), Math.abs(mYScaling));
		mAttributesChanged = mAttributesSet = true;
	}
	
	/**
	 * Sets translation for subsequent coordinates.
	 * @param x is new point for origin on X axis.
	 * @param y is new point for origin on Y axis.
	 */
	public void setTranslation(double x, double y)
	{
		mCtm.translate(x, y);
		mAttributesChanged = mAttributesSet = true;
	}
	
	/**
	 * Sets rotation for subsequent coordinates.
	 * @param angle is rotation angle in radians, measured counter-clockwise.
	 */
	public void setRotation(double angle)
	{
		mCtm.rotate(angle);
		mRotation += angle;
		mRotation = Math.IEEEremainder(mRotation, Math.PI * 2);
		mAttributesChanged = mAttributesSet = true;
	}

	/**
	 * Sets transformation from real world coordinates to page coordinates.
	 * @param x1 minimum X world coordinate.
	 * @param y1 minimum Y world coordinate.
	 * @param x2 maximum X world coordinate.
	 * @param y2 maximum Y world coordinate.
	 * @param units units of world coordinates (WORLD_UNITS_METRES,WORLD_UNITS_FEET, etc.)
	 */
	public void setWorlds(double x1, double y1, double x2, double y2, int units)
	{
		double xDiff = x2 - x1;
		double yDiff = y2 - y1;
		double xMid, yMid;
		double worldAspectRatio = yDiff / xDiff;
		double pageAspectRatio =
			mOutputFormat.getPageHeight() / mOutputFormat.getPageWidth();

		/*
		 * Expand world coordinate range in either X or Y axis so
		 * it has same aspect ratio as page.
		 */
		if (worldAspectRatio > pageAspectRatio)
		{
			/*
			 * World coordinate range is taller than page coordinate
			 * system.  Expand X axis range to compensate:
			 * 
			 *  PAGE    WORLDS    EXPANDED WORLDS
			 *  +---+   +---+     +-+---+-+
			 *  |   |   |   |     |<|   |>|
			 * 	|___|   |   |  => |<|   |>|
			 *          |   |     |<|   |>|
			 *          +---+     +-+---+-+
			 */
			xMid = (x1 + x2) / 2.0;
			x1 = xMid - (xDiff / 2.0) * (worldAspectRatio / pageAspectRatio);
			x2 = xMid + (xDiff / 2.0) * (worldAspectRatio / pageAspectRatio);
		}
		else if (worldAspectRatio < pageAspectRatio)
		{
			/*
			 * World coordinate range is wider than page coordinate system.
			 * Expand Y axis range.
			 */
			yMid = (y1 + y2) / 2.0;
			y1 = yMid - (yDiff / 2.0) * (pageAspectRatio / worldAspectRatio);
			y2 = yMid + (yDiff / 2.0) * (pageAspectRatio / worldAspectRatio);
		}
		
		/*
		 * Setup CTM from world coordinates to page coordinates.
		 */
		mWorldCtm = new AffineTransform();
		mWorldCtm.scale(mOutputFormat.getPageWidth() / (x2 - x1),
			mOutputFormat.getPageHeight() / (y2 - y1));
		mWorldCtm.translate(-x1, -y1);
		mWorldExtents = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
		mWorldUnits = units;
	}

	/**
	 * Sets reprojection between two world coordinate systems.
	 * @param sourceSystem description of coordinate system coordinates transformed form.
	 * @param destinationSystem description of coordinate system coordinates
	 * are transformed to.
	 */
	public void setReprojection(String sourceSystem, String destinationSystem)
		throws MapyrusException
	{
		mProjectionTransform = new WorldCoordinateTransform(sourceSystem,
			destinationSystem);
	}

	/**
	 * Set current dataset that can be queried and fetched from.
	 * @param dataset opened dataset for subsequent queries.
	 */
	public void setDataset(GeographicDataset dataset) throws MapyrusException
	{
		mDataset = dataset;
		mDatasetRow = null;
		mDatasetRowCount = 0;
	}

	/**
	 * Returns X scaling value in current transformation.
	 * @return X scale value.
	 */
	public double getScalingX()
	{
		return(mXScaling);
	}

	/**
	 * Returns Y scaling value in current transformation.
	 * @return Y scale value
	 */
	public double getScalingY()
	{
		return(mYScaling);
	}

	/**
	 * Returns rotation angle in current transformation.
	 * @return rotation in radians.
	 */
	public double getRotation()
	{
		return(mRotation);
	}

	/**
	 * Returns world coordinate extents being shown on page.
	 * @return rectangular area covered by extents.
	 */
	public Rectangle2D.Double getWorldExtents()
	{
		Rectangle2D.Double retval;
		
		if (mWorldExtents != null)
		{
			retval = mWorldExtents;
		}
		else if (mOutputFormat != null)
		{
			retval = new Rectangle2D.Double(0, 0,
				mOutputFormat.getPageWidth(), mOutputFormat.getPageHeight());
				
		}
		else
		{
			retval = new Rectangle2D.Double(0, 0, 1, 1);
		}
		return(mWorldExtents);
	}

	/**
	 * Return scale of world coordinates.  The world coordinate range divided
	 * by the page size.
	 * @return scale, (1:2000) is returned as value 2000. 
	 */
	public double getWorldScale()
	{
		double scale;
		double worldWidthInMM;

		if (mOutputFormat != null && mWorldCtm != null)
		{
			worldWidthInMM = mWorldExtents.width;
			if (mWorldUnits == WORLD_UNITS_METRES)
				worldWidthInMM *= 1000.0;
			else
				worldWidthInMM *= 1000.0 / 0.3048;

			scale = worldWidthInMM / mOutputFormat.getPageWidth();
		}
		else
		{
			scale = 1.0;
		}
		return(scale);
	}

	/**
	 * Returns bounding that when transformed through projection results
	 * in same bounding box as current world coordinate system.
	 * @return bounding box.
	 */
	public Rectangle2D.Double getUnprojectedExtents() throws MapyrusException
	{
		Rectangle2D.Double retval;
		double xMin, yMin, xMax, yMax;
		int i, j;
		double coords[] = new double[2];
		
		xMin = yMin = Float.MAX_VALUE;
		xMax = yMax = Float.MIN_VALUE;

		if (mWorldExtents != null)
		{
			if (mProjectionTransform != null)
			{
				/*
				 * Transform points around boundary of world coordinate extents
				 * backwards through projection transformation.
				 * Find minimum and maximum values.
				 */
				for (i = 0; i <= PROJECTED_GRID_STEPS; i++)
				{
					for (j = 0; j <= PROJECTED_GRID_STEPS; j++)
					{
						/*
						 * Only transform points around boundary.
						 */
						if ((i == 0 || i == PROJECTED_GRID_STEPS) &&
							(j == 0 || j == PROJECTED_GRID_STEPS))
						{
							coords[0] = mWorldExtents.x +
								((double)i / PROJECTED_GRID_STEPS) * mWorldExtents.width;
							coords[1] = mWorldExtents.y +
								((double)j / PROJECTED_GRID_STEPS) * mWorldExtents.height;	
						
							mProjectionTransform.backwardTransform(coords);
							if (coords[0] < xMin)
								xMin = coords[0];
							if (coords[1] < yMin)
								yMin = coords[1];
							if (coords[0] > xMax)
								xMax = coords[0];
							if (coords[1] > yMax)
								yMax = coords[1];
						}
					}
				}
				retval = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
			}
			else
			{
				/*
				 * No projection transformation set so just return plain world
				 * coordinate extents.
				 */
				retval = mWorldExtents;
			}
		}
		else if (mOutputFormat != null)
		{
			/*
			 * No world coordinate system set, just return page coordinate.
			 */
			retval = new Rectangle2D.Double(0, 0,
				mOutputFormat.getPageWidth(), mOutputFormat.getPageHeight());
		}
		else
		{
			retval = new Rectangle2D.Double(0, 0, 1, 1);
		}
		return(retval);
	}
						
	/**
	 * Get current dataset being used for queries.
	 * @return current dataset, or null if none is set.
	 */
	public GeographicDataset getDataset() throws MapyrusException
	{
		return(mDataset);
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void moveTo(double x, double y) throws MapyrusException
	{
		double srcPts[] = new double[2];
		float dstPts[] = new float[2];

		srcPts[0] = x;
		srcPts[1] = y;

		/*
		 * Transform to correct world coordinate system.
		 */
		if (mProjectionTransform != null)
		{
			mProjectionTransform.forwardTransform(srcPts);
		}
		
		/*
		 * Transform point from world coordinates
		 * to millimetre position on page.
		 */		
		if (mWorldCtm != null)
			mWorldCtm.transform(srcPts, 0, srcPts, 0, 1);
		mCtm.transform(srcPts, 0, dstPts, 0, 1);
		if (mPath == null)
			mPath = new GeometricPath();

		/*
		 * Set no rotation for point.
		 */
		mPath.moveTo(dstPts[0], dstPts[1], 0.0f);
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void lineTo(double x, double y) throws MapyrusException
	{
		double srcPts[] = new double[2];
		float dstPts[] = new float[2];

		srcPts[0] = x;
		srcPts[1] = y;
		
		/*
		 * Transform to correct world coordinate system.
		 */
		if (mProjectionTransform != null)
		{
			mProjectionTransform.forwardTransform(srcPts);
		}

		/*
		 * Transform point from world coordinates
		 * to millimetre position on page.
		 */
		if (mWorldCtm != null)
			mWorldCtm.transform(srcPts, 0, srcPts, 0, 1);
		mCtm.transform(srcPts, 0, dstPts, 0, 1);
		if (mPath == null)
			mPath = new GeometricPath();
		mPath.lineTo(dstPts[0], dstPts[1]);
	}

	/**
	 * Add circular arc to path from last point to a new point, given centre and direction.
	 * @param direction positive for clockwise, negative for anti-clockwise. 
	 * @param xCentre X coordinate of centre point of arc.
	 * @param yCentre Y coordinate of centre point of arc.
	 * @param xEnd X coordinate of end point of arc.
	 * @param yEnd Y coordinate of end point of arc.
	 */
	public void arcTo(int direction, double xCentre, double yCentre,
		double xEnd, double yEnd) throws MapyrusException
	{
		double centrePts[] = new double[2];
		double endPts[] = new double[2];
		float dstPts[] = new float[4];

		centrePts[0] = xCentre;
		centrePts[1] = yCentre;
		endPts[0] = xEnd;
		endPts[1] = yEnd;
		
		/*
		 * Transform to correct world coordinate system.
		 */
		if (mProjectionTransform != null)
		{
			mProjectionTransform.forwardTransform(centrePts);
			mProjectionTransform.forwardTransform(endPts);
		}

		/*
		 * Transform points from world coordinates
		 * to millimetre position on page.
		 */
		if (mWorldCtm != null)
		{
			mWorldCtm.transform(centrePts, 0, centrePts, 0, 1);
			mWorldCtm.transform(endPts, 0, endPts, 0, 1);
		}
		mCtm.transform(centrePts, 0, dstPts, 0, 1);
		mCtm.transform(endPts, 0, dstPts, 2, 1);

		if (mPath == null)
			mPath = new GeometricPath();
		mPath.arcTo(direction, dstPts[0], dstPts[1], dstPts[2], dstPts[3]);
	}
	
	/**
	 * Clears currently defined path.
	 */
	public void clearPath()
	{
		if (mPath != null)
			mPath.reset();
	}

	/**
	 * Replace path with regularly spaced points along it.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point.
	 */
	public void samplePath(double spacing, double offset) throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();

		if (path != null)
		{
			path.samplePath(spacing * mScalingMagnitude, offset * mScalingMagnitude,
				resolution);
		}
	}

	/**
	 * Replace path defining polygon with parallel stripe
	 * lines covering the polygon.
	 * @param spacing is distance between stripes.
	 * @param angle is angle of stripes, in radians, with zero horizontal.
	 */
	public void stripePath(double spacing, double angle)
	{
		GeometricPath path = getDefinedPath();

		if (path != null)
			path.stripePath(spacing * mScalingMagnitude, angle);
	}

	/**
	 * Draw currently defined path.
	 */
	public void stroke()
	{
		GeometricPath path = getDefinedPath();

		if (path != null && mOutputFormat != null)
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
		GeometricPath path = getDefinedPath();
		
		if (path != null && mOutputFormat != null)
		{	
			setGraphicsAttributes();
			mOutputFormat.fill(path.getShape());
		}
	}

	/**
	 * Set clipping to show only inside of currently defined path.
	 */
	public void clip()
	{
		GeometricPath path = getDefinedPath();

		if (path != null && mOutputFormat != null)
		{
			mClippingPath = new GeometricPath(path);
			mAttributesChanged = mAttributesSet = true;
			if (mOutputFormat != null)
			{
				mOutputFormat.clip(mClippingPath.getShape());
			}
		}
	}

	/**
	 * Draw label positioned at (or along) currently defined path.
	 * @param label is string to draw on page.
	 */
	public void label(String label)
	{
		GeometricPath path = getDefinedPath();
		
		if (path != null && mOutputFormat != null)
		{	
			setGraphicsAttributes();
			mOutputFormat.label(path.getMoveTos(), label);
		}
	}
	
	/**
	 * Returns the number of moveTo's in path defined in this context.
	 * @return count of moveTo calls made.
	 */
	public int getMoveToCount()
	{
		int retval;
		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = 0;
		else
			retval = path.getMoveToCount();
		return(retval);
	}

	/**
	 * Returns the number of lineTo's in path defined in this context.
	 * @return count of lineTo calls made for this path.
	 */
	public int getLineToCount()
	{
		int retval;
		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = 0;
		else
			retval = path.getLineToCount();
		return(retval);
	}

	/**
	 * Returns geometric length of current path.
	 * @return length of current path.
	 */
	public double getPathLength() throws MapyrusException
	{
		double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = 0.0;
		else
			retval = path.getLength(resolution);
		return(retval);
	}

	/**
	 * Returns geometric area of current path.
	 * @return area of current path.
	 */
	public double getPathArea() throws MapyrusException
	{
		double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = 0.0;
		else
			retval = path.getArea(resolution);
		return(retval);
	}

	/**
	 * Returns geometric centroid of current path.
	 * @return centroid of current path.
	 */
	public Point2D.Double getPathCentroid() throws MapyrusException
	{
		Point2D.Double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = new Point2D.Double();
		else
			retval = path.getCentroid(resolution);
		return(retval);
	}

	/**
	 * Returns rotation angle for each each moveTo point in current path
	 * @return array of rotation angles relative to rotation in current 
	 * transformation matrix. 
	 */
	public ArrayList getMoveToRotations()
	{
		ArrayList retval;
		ArrayList list;
		double rotation;
		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = null;
		else
			retval = path.getMoveToRotations();
		return(retval);
	}

	/**
	 * Returns coordinate for each each moveTo point in current path.
	 * @return array of Point2D.Float objects relative to current transformation matrix.
	 */
	public ArrayList getMoveTos() throws MapyrusException
	{
		ArrayList retval = null;
		GeometricPath path = getDefinedPath();
		AffineTransform inverse;
		ArrayList moveTos;

		try
		{
			if (path != null)
			{
				/*
				 * If there is no transformation matrix then we can return original
				 * coordinates, otherwise we must convert all coordinates to be relative
				 * to the current transformation matrix and build a new list.
				 */
				if (mCtm.isIdentity())
				{
					retval = path.getMoveTos();
				}
				else
				{
					inverse = mCtm.createInverse();
					moveTos = path.getMoveTos();
					retval = new ArrayList(moveTos.size());

					for (int i = 0; i < moveTos.size(); i++)
					{
						Point2D.Float pt = (Point2D.Float)(moveTos.get(i));
						retval.add(inverse.transform(pt, null));
					}
				}
			}
		}
		catch (NoninvertibleTransformException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return(retval);
	}

	/**
	 * Returns bounding box of this geometry.
	 * @return bounding box, or null if no path is defined.
	 */
	public Rectangle2D getBounds2D()
	{
		Rectangle2D bounds;
		GeometricPath path = getDefinedPath();

		if (path == null)
			bounds = null;
		else
			bounds = path.getBounds2D();

		return(bounds);
	}

	/**
	 * Returns true if dataset being read has another row available.
	 * @return true if another row available.
	 */
	public boolean datasetHasMoreRows() throws MapyrusException
	{
		return(mDataset != null && mDatasetRow != null);
	}
	
	/**
	 * Return next row from dataset.
	 * @return field values for next row.
	 */
	public Row fetchDatasetRow() throws MapyrusException
	{
		if (mDataset == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_DATASET));

		if (mDatasetRow == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_ROWS));

		/*
		 * Return row we've already fetched.
		 */		
		Row retval = mDatasetRow;
		mDatasetRow = mDataset.fetch();
		mDatasetRowCount++;
		return(retval);
	}
	
	/**
	 * Begin query on current dataset.  Geometry inside the current world extents
	 * is fetched. 
	 */
	public void queryDataset() throws MapyrusException
	{
		if (mDataset == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_DATASET));
		mDataset.query(getUnprojectedExtents(), 1.0);
		
		/*
		 * Fetch first row so we know if there are any more records available.
		 */
		mDatasetRow = mDataset.fetch();
	}

	/**
	 * Return the number of rows already fetched from dataset for current query.
	 * @return count of rows fetched.
	 */
	public int getDatasetQueryCount()
	{
		return(mDatasetRowCount);
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
	 * variable with the same name.
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
