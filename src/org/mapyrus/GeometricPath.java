/**
 * A geometric path.  A series of coordinates, either separate or joined together
 * as part of a line or polygon.  Coordinate pairs are joined with either
 * straight lines, arcs or bezier curves.
 */

/*
 * $Id$
 */
 
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.lang.*;

public class GeometricPath
{
	/*
	 * Coordinates of path.
	 */
	private GeneralPath mPath;
	
	/**
	 * Create new, empty geometric path.
	 */
	public GeometricPath()
	{
		mPath = new GeneralPath();
	}

	/**
	 * Construct path as a copy of an existing path.
	 * @param path is existing path to copy.
	 */
	public GeometricPath(GeometricPath path)
	{
		try
		{
			mPath = (GeneralPath)path.clone();	
		}
		catch (CloneNotSupportedException e)
		{
			/*
			 * Clone _is_ supported for GeneralPath class.
			 */
		}
	}
		
	public void moveTo(float x, float y)
	{
		mPath.moveTo(x, y);
	}
	
	public void lineTo(float x, float y)
	{
		mPath.lineTo(x, y);
	}
	
	/**
	 * Closes path back to the last point created with moveTo method.
	 */
	public void closePath()
	{
		mPath.closePath();
	}

	/**
	 * Returns bounding box of this geometry.
	 * @return bounding box.
	 */
	public Rectangle2D getBounds2D()
	{
		return mPath.getBounds2D();
	}

	/**
	 * @see java.awt.Shape#getPathIterator(AffineTransform)
	 */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return(mPath.getPathIterator(at));
	}

	/**
	 * @see java.awt.Shape#getPathIterator(AffineTransform, double)
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return(mPath.getPathIterator(at, flatness));
	}

	/**
	 * Returns Shape object of geometry for display.
	 * @return shape object which can be used directly in 2D display methods.
	 */
	public Shape getShape()
	{
		return(mPath);
	}
	
	/**
	 * Clear path, removing all coordinates.
	 */
	public void reset()
	{
		mPath.reset();
	}

	/**
	 * Explode path by replacing path with a path with many points,
	 * equally spaced along old path.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point. 
	 */
	public void explode(float spacing, float offset)
	{
	}
}
