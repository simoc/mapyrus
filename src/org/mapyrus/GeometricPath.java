/*
 * $Id$
 */
package au.id.chenery.mapyrus;
 
import java.awt.Shape;
import java.awt.geom.*;
import java.util.Vector;

/**
 * A geometric path.  A series of coordinates, either separate or joined together
 * as part of a line or polygon.  Coordinate pairs are joined with either
 * straight lines, arcs or bezier curves.
 */
public class GeometricPath
{
	/*
	 * What to calculate when walking through path?
	 */
	static final int CALCULATE_LENGTHS = 1;
	static final int CALCULATE_AREAS = 2;
	static final int CALCULATE_CENTROID = 3;
	static final int CALCULATE_PIP = 4;	/* point in polygon */

	/*
	 * An identity matrix results in no transformation.
	 */
	static AffineTransform mIdentityMatrix = new AffineTransform();

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
		mPath = (GeneralPath)(path.mPath.clone());	
		mNLineTos = path.mNLineTos;
		mMoveTos = (Vector)path.mMoveTos.clone();
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
	 * Returns geometric area of full path.
	 * @return path area.
	 */
	public double getArea()
	{
		double totalArea = 0.0;
		double areas[] = walkPath(CALCULATE_AREAS);

		for (int i = 0; i < areas.length; i++)
			totalArea += areas[i];
		return(Math.abs(totalArea));
	}

	/**
	 * Returns geometric length of full path.
	 * @return path length.
	 */
	public double getLength()
	{
		double totalLength = 0.0;
		double lengths[] = walkPath(CALCULATE_LENGTHS);

		for (int i = 0; i < lengths.length; i++)
			totalLength += lengths[i];
		return(totalLength);
	}
	
	/**
	 * Returns geometric centroid of full closed path.
	 * @return path centroid.
	 */
	public Point2D.Double getCentroid()
	{
		double pt[] = walkPath(CALCULATE_CENTROID);
		return(new Point2D.Double(pt[0], pt[1]));
	}

	/**
	 * Walks path, calculating length, area or centroid.  Length or area
	 * for each moveTo, lineTo, ... part is calculated separately.
	 * If the path is not closed then the calculated area is meaningless.
	 * @return array with length or area of each part of the path.
	 */
	private double []walkPath(int attributeToCalculate)
	{
		int segmentType;
		PathIterator pi = mPath.getPathIterator(mIdentityMatrix);
		float coords[] = new float[6];
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd, yEnd;
		float xMoveTo = 0.0f, yMoveTo =0.0f;
		double partLengths[], partAreas[], centroid[];
		double len;
		int moveToCount = 0;
		double ai, aSum, xSum, ySum;

		/*
		 * Create array to hold length and area of each part of path.
		 */
		aSum = xSum = ySum = 0.0;
		int nEls = (attributeToCalculate == CALCULATE_CENTROID) ? 2 : getMoveToCount();
		centroid = partAreas = partLengths = new double[nEls];
		for (int i = 0; i < partLengths.length; i++)
			partLengths[i] = 0.0;

		/*
		 * Walk through path, summing up length and area of segments.
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
				 * Sum up length or area of this segment.
				 */
				if (attributeToCalculate == CALCULATE_LENGTHS)
				{
					partLengths[moveToCount - 1] +=
						Math.sqrt((xEnd - xStart) * (xEnd - xStart) +
						(yEnd - yStart) * (yEnd - yStart));
				}
				else if (attributeToCalculate == CALCULATE_AREAS)
				{
					partAreas[moveToCount - 1] +=
						(xStart * yEnd - xEnd * yStart) / 2.0;
				}
				else if (attributeToCalculate == CALCULATE_CENTROID)
				{
					ai = xStart * yEnd - xEnd * yStart;
					aSum += ai;
					xSum += (xEnd + xStart) * ai;
					ySum += (yEnd + yStart) * ai;
				}

				xStart = xEnd;
				yStart = yEnd;
			}
			pi.next();
		}

		if (attributeToCalculate == CALCULATE_CENTROID)
		{
			centroid[0] = xSum / (3.0 * aSum);
			centroid[1] = ySum / (3.0 * aSum);
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
		 * we can calculate a starting offset and then step
		 * forwards through the path.
		 */
		if (spacing < 0.0)
		{
			stepDirection = -1;
			spacing = -spacing;
			partLengths = walkPath(CALCULATE_LENGTHS);
		}
		else
		{
			stepDirection = 1;
		}
		
		pi = mPath.getPathIterator(mIdentityMatrix);	
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
	
	/**
	 * Replace path defining a polygon with striped lines covering
	 * the polygon.
	 * @param spacing is distance between parallel lines.
	 * @param angle is angle of stripes, in radians, with zero being horizontal.
	 */
	public void stripePath(double spacing, double angle)
	{
		GeometricPath newPath = new GeometricPath();
		Rectangle2D bounds = getBounds2D();
		int nPts = 4;
		double pts[] = new double[nPts * 2];
		AffineTransform rotateTransform = new AffineTransform();
		AffineTransform inverseRotateTransform = new AffineTransform();
		double xMin, yMin, xMax, yMax, y;
		float startCoords[];
		float endCoords[] = new float[2];

		/*
		 * Create bounding box of polygon at origin, rotated so that
		 * stripes will be horizontal.
		 */
		pts[0] = 0.0;
		pts[1] = 0.0;
		
		pts[2] = 0.0;
		pts[3] = bounds.getHeight();
		
		pts[4] = bounds.getWidth();
		pts[5] = pts[3];
		
		pts[6] = pts[4];
		pts[7] = 0.0;

		inverseRotateTransform.rotate(-angle);
		inverseRotateTransform.transform(pts, 0, pts, 0, nPts);

		/*
		 * Find area covered by rotated rectangle.
		 */
		xMin = xMax = pts[0];
		yMin = yMax = pts[1];
		for (int i = 1; i < nPts; i++)
		{
			xMin = Math.min(xMin, pts[i * 2]);
			yMin = Math.min(yMin, pts[i * 2 + 1]);
			xMax = Math.max(xMax, pts[i * 2]);
			yMax = Math.max(yMax, pts[i * 2 + 1]);
		}

		/*
		 * Create stripes horizontally through rotated rectangle.
		 */
		rotateTransform.translate(bounds.getMinX(), bounds.getMinY());
		rotateTransform.rotate(angle);
		y = yMin;
		while (y < yMax)
		{
			pts[0] = xMin;
			pts[1] = pts[3] = y;
			pts[2] = xMax;
			
			/*
			 * Transform each stripe back to coordinate system of
			 * original polygon.
			 */
			rotateTransform.transform(pts, 0, pts, 0, 2);

			startCoords = new float[3];
			startCoords[0] = (float)(pts[0]);
			startCoords[1] = (float)(pts[1]);
			startCoords[2] = 0.0f;

			endCoords[0] = (float)(pts[2]);
			endCoords[1] = (float)(pts[3]);

			newPath.moveTo(startCoords);
			newPath.lineTo(endCoords);

			y += spacing;
		}
	
		/*
		 * Replace path with the new path containing the stripes
		 * we have just calculated.
		 */		
		mPath = (GeneralPath)newPath.getShape();	
		mNLineTos = newPath.getLineToCount();
		mMoveTos = newPath.getMoveTos();
	}
}
