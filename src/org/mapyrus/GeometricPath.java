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
import java.util.Vector;

public class GeometricPath
{
	/*
	 * Coordinates of path and moveto points with rotation angles.
	 */
	private GeneralPath mPath;
	private Vector mMoveTos;

	/*
	 * Count of number of lineTos in path.
	 */
	private int mNLineTos;

	/**
	 * Create new, empty geometric path.
	 */
	public GeometricPath()
	{
		mPath = new GeneralPath();
		mMoveTos = new Vector();
		mNLineTos = 0;
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
			mNLineTos = path.mNLineTos;
			mMoveTos = (Vector)path.mMoveTos.clone();
		}
		catch (CloneNotSupportedException e)
		{
			/*
			 * Clone _is_ supported for GeneralPath and Vector classes.
			 */
		}
	}

	/**
	 * Add point to path.
	 * @param coords three element array containing x and y coordinates
	 * and rotation angle to use for a symbol at this point.
	 */
	public void moveTo(float []coords)
	{
		mPath.moveTo(coords[0], coords[1]);
		mMoveTos.add(coords);
	}
	
	/**
	 * Add point to path with straight line segment from last point.
	 * @param coords two element array containing x and y coordinates of point.
	 */	
	public void lineTo(float []coords)
	{
		mPath.lineTo(coords[0], coords[1]);
		mNLineTos++;
	}
	
	/**
	 * Closes path back to the last point created with moveTo method.
	 */
	public void closePath()
	{
		mPath.closePath();
		mNLineTos++;
	}

	/**
	 * Returns the number of moveTo's in this path.
	 * @return count of moveTo calls made for this path.
	 */
	public int getMoveToCount()
	{
		return(mMoveTos.size());
	}

	/**
	 * Returns the number of lineTo's in this path.
	 * @return count of lineTo calls made for this path.
	 */
	public int getLineToCount()
	{
		return(mNLineTos);
	}

	/**
	 * Returns moveTo points in current path.
	 * @return list of three element float arrays containing moveTo points
	 * in path.
	 */
	public Vector getMoveTos()
	{
		return(mMoveTos);
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
		mMoveTos.clear();
		mNLineTos = 0;
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
