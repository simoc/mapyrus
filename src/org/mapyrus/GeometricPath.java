/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;
 
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;

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
	 * Coordinates of path.
	 */
	private GeneralPath mPath;
	
	/*
	 * Coordinates and rotation angle at each moveTo point.  GeneralPath
	 * ignores successive moveTo points so we _must_ save them ourselves.
	 */
	private ArrayList mRotations;
	private ArrayList mMoveTos;

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
		mRotations = new ArrayList();
		mMoveTos = new ArrayList();
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

		/*
		 * Copy the list of moveTo points and rotations.
		 */
		mMoveTos = (ArrayList)(path.mMoveTos.clone());
		mRotations = (ArrayList)(path.mRotations.clone());
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to move to
	 * @param y Y coordinate to move to
	 * @param rotation angle to use for a symbol at this point.
	 */
	public void moveTo(float x, float y, double rotation)
	{
		mPath.moveTo(x, y);
		mMoveTos.add(new Point2D.Float(x, y));
		mRotations.add(new Double(rotation));
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param coords two element array containing x and y coordinates of point.
	 */	
	public void lineTo(float x, float y)
	{
		mPath.lineTo(x, y);
		mNLineTos++;
	}

	/**
	 * Add circular arc to path from last point to a new point, given centre and direction.
	 * @param direction positive for clockwise, negative for anti-clockwise. 
	 * @param xCentre X coordinate of centre point of arc.
	 * @param yCentre Y coordinate of centre point of arc.
	 * @param xEnd X coordinate of end point of arc.
	 * @param yEnd Y coordinate of end point of arc.
	 */
	public void arcTo(int direction, float xCentre, float yCentre,
		float xEnd, float yEnd) throws MapyrusException
	{
		double radius;
		Point2D lastPt = mPath.getCurrentPoint();
		if (lastPt == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_ARC_START));

		float lastX = (float)(lastPt.getX());
		float lastY = (float)(lastPt.getY());
	
		/*
		 * If arc end point is very close to arc start point then make them
		 * the same point to force a full circle.  New arc coordinates are passed
		 * as double precision numbers but coordinates already in path are only 
		 * stored with floating precision so some round-off can occur.
		 */
		if (NumericalAnalysis.equals(lastX, xEnd) &&
			NumericalAnalysis.equals(lastY, yEnd))
		{
			xEnd = lastX;
			yEnd = lastY;
		}

		radius = Point2D.distance(xCentre, yCentre, xEnd, yEnd);

		Arc2D.Float arc = new Arc2D.Float();
		arc.setArcByCenter(xCentre, yCentre, radius, 0.0, 1.0, Arc2D.OPEN);
		arc.setAngles(lastX, lastY, xEnd, yEnd);
		if (direction < 0.0)
		{
			/*
			 * Force arc to go anti-clockwise.
			 */
			arc.setAngleExtent(arc.getAngleExtent() - 360.0);
		}
	
		mPath.append(arc, true);
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
	 * Appends another path to current path.
	 * @param path is path to append
	 * @param connect when true initial moveTo in path is turned into a lineTo.
	 */
	public void append(GeometricPath path, boolean connect)
	{
		mPath.append(path.getShape(), connect);
		ArrayList moveTos = path.getMoveTos();
		ArrayList rotations = path.getMoveToRotations();

		for (int i = 0; i < moveTos.size(); i++)
		{		
			mMoveTos.add(moveTos.get(i));
			mRotations.add(rotations.get(i));
		}
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
	 * @return list of Point2D.Float objects of moveTo points in path.
	 */
	public ArrayList getMoveTos()
	{
		return(mMoveTos);
	}

	/**
	 * Returns rotation angle at each moveTo point in current path.
	 * @return list containing rotation at each moveTo point.
	 */
	public ArrayList getMoveToRotations()
	{
		return(mRotations);
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
		mRotations.clear();
		mMoveTos.clear();
		mNLineTos = 0;
	}

	/**
	 * Returns geometric area of full path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return path area.
	 */
	public double getArea(double resolution)
	{
		double totalArea = 0.0;
		double areas[] = walkPath(CALCULATE_AREAS, resolution);

		for (int i = 0; i < areas.length; i++)
			totalArea += areas[i];
		return(Math.abs(totalArea));
	}

	/**
	 * Returns geometric length of full path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return path length.
	 */
	public double getLength(double resolution)
	{
		double totalLength = 0.0;
		double lengths[] = walkPath(CALCULATE_LENGTHS, resolution);

		for (int i = 0; i < lengths.length; i++)
			totalLength += lengths[i];
		return(totalLength);
	}
	
	/**
	 * Returns geometric centroid of full closed path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return path centroid.
	 */
	public Point2D.Double getCentroid(double resolution)
	{
		double pt[] = walkPath(CALCULATE_CENTROID, resolution);
		return(new Point2D.Double(pt[0], pt[1]));
	}

	/**
	 * Returns direction of full closed path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return true when path is closed and follows a clockwise direction.
	 */
	public boolean isClockwise(double resolution)
	{
		/*
		 * Path is clockwise if area of outer ring is negative.
		 */
		double areas[] = walkPath(CALCULATE_AREAS, resolution);
		return(areas[0] < 0.0);
	}

	/**
	 * Walks path, calculating length, area or centroid.  Length or area
	 * for each moveTo, lineTo, ... part is calculated separately.
	 * If the path is not closed then the calculated area is meaningless.
	 * @param attributeToCalculate is type of calculation to make
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return array with length or area of each part of the path.
	 */
	private double []walkPath(int attributeToCalculate, double resolution)
	{
		int segmentType;
		PathIterator pi = mPath.getPathIterator(mIdentityMatrix, resolution);
		float coords[] = new float[6];
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd, yEnd;
		float xMoveTo = 0.0f, yMoveTo =0.0f;
		double partLengths[], partAreas[], centroid[];
		int moveToCount = 0;
		double ai, aSum, xSum, ySum;
		int nEls;

		/*
		 * Create array to hold length and area of each part of path.
		 */
		aSum = xSum = ySum = 0.0;
		if (attributeToCalculate == CALCULATE_CENTROID)
			nEls = 2;
		else
			nEls = getMoveToCount(); 

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
	 * Create new path with regularly spaced points along it.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return new path containing sample points.
	 * If spacing is positive then points are placed beginning
	 * at start of path.  If spacing is negative then points
	 * are placed beginning at end of the path, moving towards
	 * start of path.
	 */
	public GeometricPath samplePath(double spacing, double offset, double resolution)
	{
		PathIterator pi;

		float coords[] = new float[6];
		int segmentType;
		double nextOffset = 0.0, segmentLength, segmentAngle;
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd, yEnd;
		float xMoveTo = 0.0f, yMoveTo = 0.0f;
		float x = 0.0f, y = 0.0f;
		GeometricPath retval = new GeometricPath();
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
			partLengths = walkPath(CALCULATE_LENGTHS, resolution);
		}
		else
		{
			stepDirection = 1;
		}
		
		pi = mPath.getPathIterator(mIdentityMatrix, resolution);	
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
					nextOffset = NumericalAnalysis.fmod(len, spacing);
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
					x = (float)(xStart + nextOffset * cosAngle);
					y = (float)(yStart + nextOffset * sinAngle);

					retval.moveTo(x, y, segmentAngle);

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
		return(retval);
	}

	/**
	 * Create new path defining a polygon with striped lines covering
	 * the polygon.
	 * @param spacing is distance between parallel lines.
	 * @param angle is angle of stripes, in radians, with zero being horizontal.
	 * @return new path with stripes.
	 */
	public GeometricPath stripePath(double spacing, double angle)
	{
		GeometricPath retval = new GeometricPath();
		Rectangle2D bounds = getBounds2D();
		int nPts = 4;
		double pts[] = new double[nPts * 2];
		AffineTransform rotateTransform = new AffineTransform();
		AffineTransform inverseRotateTransform = new AffineTransform();
		double xMin, yMin, xMax, yMax, y;

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

			retval.moveTo((float)(pts[0]), (float)(pts[1]), 0.0f);
			retval.lineTo((float)(pts[2]), (float)(pts[3]));

			y += spacing;
		}
		return(retval);
	}
}
