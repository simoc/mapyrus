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
	 * Flags that we want full length of path, not just part of it.
	 */
	private static final int FULL_PATH_LENGTH = -1;
	
	/*
	 * An identity matrix results in no transformation.
	 */
	static AffineTransform identityMatrix = new AffineTransform();

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
	 * Returns geometric length of full path.
	 * @return path length.
	 */
	public double getLength()
	{
		double totalLength = 0.0;
		double lengths[] = calculateLengths();
		
		for (int i = 0; i < lengths.length; i++)
			totalLength += lengths[i];
		return(totalLength);
	}
	
	/**
	 * Returns geometric length of path.  Length for each moveTo, lineTo, ...
	 * part is calculated separately.
	 * @return length of each part of the path.
	 */
	private double []calculateLengths()
	{
		int segmentType;
		PathIterator pi = mPath.getPathIterator(identityMatrix);
		float coords[] = new float[6];
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd, yEnd;
		float xMoveTo = 0.0f, yMoveTo =0.0f;
		double partLengths[];
		double len;
		int moveToCount = 0;

		/*
		 * Create array to hold lengths of each part of path.
		 */
		partLengths = new double[getMoveToCount()];
		for (int i = 0; i < partLengths.length; i++)
			partLengths[i] = 0.0;

		/*
		 * Walk through path, summing up length of segments.
		 */
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				xStart = xMoveTo = coords[0];
				yStart = yMoveTo = coords[1];
				moveToCount++;
			}
			else
			{
				if (segmentType == PathIterator.SEG_CLOSE)
				{
					xEnd = xMoveTo;
					yEnd = yMoveTo;
				}
				else
				{
					xEnd = coords[0];
					yEnd = coords[1];
				}

				/*
				 * Sum up length of this segment.
				 */
				partLengths[moveToCount - 1] +=
					Math.sqrt((xEnd - xStart) * (xEnd - xStart) +
					(yEnd - yStart) * (yEnd - yStart));

				xStart = xEnd;
				yStart = yEnd;
			}
			pi.next();
		}
		return(partLengths);
	}

	/**
	 * Calculate a modulo b for real numbers.  For example,
	 * 6.7 modulo 2.2 is 0.1.
	 * @return a modulo b
	 */
	private double fmod(double a, double b)
	{
		double retval = Math.IEEEremainder(a, b);
		if (retval < 0)
			retval += b;
		return(retval);
	}
	
	/**
	 * Replace path with regularly spaced points along it.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point.
	 * If spacing is positive then points are placed beginning
	 * at start of path.  If spacing is negative then points
	 * are placed beginning at end of the path, moving towards
	 * start of path.
	 */
	public void slicePath(double spacing, double offset)
	{
		PathIterator pi;

		float coords[] = new float[6];
		float nextPoint[];
		int segmentType;
		double nextOffset = 0.0, segmentLength, segmentAngle;
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd, yEnd;
		float xMoveTo = 0.0f, yMoveTo = 0.0f;
		float x = 0.0f, y = 0.0f;
		GeometricPath newPath = new GeometricPath();
		double sinAngle, cosAngle;
		double partLengths[] = null;
		int stepDirection;
		int moveToCount = 0;

		/*
		 * A negative spacing means that we must start at end
		 * of line and step towards beginning.  To do this,
		 * we find the length of each part of the path so
		 * we can calculate a starting offset and step
		 * forwards through the path.
		 */
		if (spacing < 0.0)
		{
			stepDirection = -1;
			spacing = -spacing;
			partLengths = calculateLengths();
		}
		else
		{
			stepDirection = 1;
		}
		
		pi = mPath.getPathIterator(identityMatrix);	
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				/*
				 * Start of a new path segment, reset offset.
				 */
				if (stepDirection > 0)
				{
					nextOffset = offset;
				}
				else
				{
					/*
					 * Offset is from end of path.  Calculate
					 * where to begin so we end up at correct
					 * offset at end of path.
					 */
					double len = partLengths[moveToCount] - offset;
					nextOffset = fmod(len, spacing);
				}
				xStart = xMoveTo = coords[0];
				yStart = yMoveTo = coords[1];
			}
			else
			{
				if (segmentType == PathIterator.SEG_CLOSE)
				{
					xEnd = xMoveTo;
					yEnd = yMoveTo;
				}
				else
				{
					xEnd = coords[0];
					yEnd = coords[1];
				}
				
				segmentLength = Math.sqrt((xEnd - xStart) * (xEnd - xStart) +
					(yEnd - yStart) * (yEnd - yStart));
					
				segmentAngle = Math.atan2(yEnd - yStart, xEnd - xStart);
	
				cosAngle = Math.cos(segmentAngle);
				sinAngle = Math.sin(segmentAngle);

				/*
				 * Calculate equally spaced points along this line segment.
				 */
				while (nextOffset < segmentLength)
				{
					nextPoint = new float[3];
					nextPoint[0] = (float)(xStart + nextOffset * cosAngle);
					nextPoint[1] = (float)(yStart + nextOffset * sinAngle);
					nextPoint[2] = (float)segmentAngle;

					newPath.moveTo(nextPoint);

					nextOffset += spacing;
				}

				/*
				 * Get ready for next line segment.
				 */
				nextOffset -= segmentLength;
				xStart = xEnd;
				yStart = yEnd;
			}
			pi.next();			
		}

		/*
		 * Replace path with the new path containing points that we have just calculated.
		 */		
		mPath = (GeneralPath)newPath.getShape();	
		mNLineTos = newPath.getLineToCount();
		mMoveTos = newPath.getMoveTos();
	}
}
