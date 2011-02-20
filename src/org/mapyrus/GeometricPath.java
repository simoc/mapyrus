/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package org.mapyrus;

import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Stack;
import java.awt.geom.Ellipse2D;

import org.mapyrus.geom.LineEquation;

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
	static final int CALCULATE_START_ANGLE = 5; /* angle of first line segment */
	static final int CALCULATE_END_ANGLE = 6; /* angle of last line segment */

	/*
	 * Mathematical constant 1/sqrt(2).
	 */
	private static final double ONE_OVER_SQRT_TWO = 0.707106781186547;

	/*
	 * An identity matrix results in no transformation.
	 */
	private static AffineTransform m_identityMatrix = new AffineTransform();

	/*
	 * Coordinates of path.
	 */
	private GeneralPath m_path;
	
	/*
	 * Coordinates and rotation angle at each moveTo point.  GeneralPath
	 * ignores successive moveTo points so we _must_ save them ourselves.
	 */
	private ArrayList<Double> m_rotations;
	private ArrayList<Point2D> m_moveTos;

	/*
	 * Count of number of lineTos in path.
	 */
	private int m_nLineTos;

	/**
	 * Create new, empty geometric path.
	 */
	public GeometricPath()
	{
		m_path = new GeneralPath();
		m_rotations = new ArrayList<Double>();
		m_moveTos = new ArrayList<Point2D>();
		m_nLineTos = 0;
	}

	/**
	 * Construct path as a copy of an existing path.
	 * @param path is existing path to copy.
	 */
	public GeometricPath(GeometricPath path)
	{
		m_path = (GeneralPath)(path.m_path.clone());	
		m_nLineTos = path.m_nLineTos;

		/*
		 * Copy the list of moveTo points and rotations.
		 */
		m_moveTos = new ArrayList<Point2D>(path.m_moveTos.size());
		for (Point2D p : path.m_moveTos)
			m_moveTos.add(p);
		m_rotations = new ArrayList<Double>(path.m_rotations.size());
		for (Double d : path.m_rotations)
			m_rotations.add(d);
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to move to
	 * @param y Y coordinate to move to
	 * @param rotation angle to use for a symbol at this point.
	 */
	public void moveTo(float x, float y, double rotation)
	{
		m_path.moveTo(x, y);
		m_moveTos.add(new Point2D.Float(x, y));
		m_rotations.add(new Double(rotation));
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param coords two element array containing x and y coordinates of point.
	 */	
	public void lineTo(float x, float y)
	{
		m_path.lineTo(x, y);
		m_nLineTos++;
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
		Point2D lastPt = m_path.getCurrentPoint();
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
	
		m_path.append(arc, true);
		m_nLineTos++;
	}

	/**
	 * Add Bezier curve to path from last point to a new point.
	 * @param xControl1 X coordinate of first Bezier control point.
	 * @param yControl1 Y coordinate of first Bezier control point.
	 * @param xControl2 X coordinate of second Bezier control point.
	 * @param yControl2 Y coordinate of second Bezier control point.
	 * @param xEnd X coordinate of end point of curve.
	 * @param yEnd Y coordinate of end point of curve.
	 */
	public void curveTo(float xControl1, float yControl1,
		float xControl2, float yControl2, float xEnd, float yEnd) throws MapyrusException
	{
		Point2D lastPt = m_path.getCurrentPoint();
		if (lastPt == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_BEZIER_START));

		m_path.curveTo(xControl1, yControl1, xControl2, yControl2, xEnd, yEnd);
		m_nLineTos++;
	}

	/**
	 * Add ellipse to path.
	 * @param xCenter X coordinate of center of ellipse.
	 * @param yCenter Y coordinate of center of ellipse.
	 * @param xDiameter width of ellipse.
	 * @param yDiameter height of ellipse.
	 * @param rotation rotation angle of ellipse, in radians.
	 */
	public void ellipseTo(double xCenter, double yCenter, double xDiameter, double yDiameter, double rotation)
	{
		Point2D pt;

		/*
		 * Create elliptical shape and add it to path.
		 */
		Ellipse2D.Double ellipse = new Ellipse2D.Double(xCenter - xDiameter / 2, yCenter - yDiameter / 2,
			xDiameter, yDiameter);
		if (rotation != 0)
		{
			/*
			 * Rotate ellipse to correct angle before adding it to path.
			 */
			GeneralPath g = new GeneralPath(ellipse);
			AffineTransform affine = AffineTransform.getRotateInstance(rotation, xCenter, yCenter);
			g.transform(affine);
			m_path.append(g, false);
			pt = g.getCurrentPoint();
		}
		else
		{
			m_path.append(ellipse, false); 
			pt = new Point2D.Float((float)(xCenter + xDiameter), (float)yCenter);
		}

		m_moveTos.add(pt);
		m_nLineTos++;
	}

	/**
	 * Closes path back to the last point created with moveTo method.
	 */
	public void closePath()
	{
		m_path.closePath();
		m_nLineTos++;
	}

	/**
	 * Appends another path to current path.
	 * @param path is path to append
	 * @param connect when true initial moveTo in path is turned into a lineTo.
	 */
	public void append(GeometricPath path, boolean connect)
	{
		m_path.append(path.getShape(), connect);
		ArrayList<Point2D> moveTos = path.getMoveTos();
		ArrayList<Double> rotations = path.getMoveToRotations();

		for (int i = 0; i < moveTos.size(); i++)
		{		
			m_moveTos.add(moveTos.get(i));
			m_rotations.add(rotations.get(i));
		}
	}

	/**
	 * Returns the number of moveTo's in this path.
	 * @return count of moveTo calls made for this path.
	 */
	public int getMoveToCount()
	{
		return(m_moveTos.size());
	}

	/**
	 * Returns the number of lineTo's in this path.
	 * @return count of lineTo calls made for this path.
	 */
	public int getLineToCount()
	{
		return(m_nLineTos);
	}

	/**
	 * Returns moveTo points in current path.
	 * @return list of Point2D.Float objects of moveTo points in path.
	 */
	public ArrayList<Point2D> getMoveTos()
	{
		return(m_moveTos);
	}

	/**
	 * Returns rotation angle at each moveTo point in current path.
	 * @return list containing rotation at each moveTo point.
	 */
	public ArrayList<Double> getMoveToRotations()
	{
		return(m_rotations);
	}

	/**
	 * Returns bounding box of this geometry.
	 * @return bounding box.
	 */
	public Rectangle2D getBounds2D()
	{
		Rectangle2D retval;
		
		if (m_moveTos.size() > 0 && m_nLineTos == 0)
		{
			/*
			 * Path is just a series of points.
			 * Find rectangle containing all points.
			 */
			double xMin, yMin, xMax, yMax;
			Point2D pt = m_moveTos.get(0);
			xMin = xMax = pt.getX();
			yMin = yMax = pt.getY();
			for (int i = 1; i < m_moveTos.size(); i++)
			{
				pt = m_moveTos.get(i);
				if (pt.getX() < xMin)
					xMin = pt.getX();
				if (pt.getY() < yMin)
					yMin = pt.getY();
				if (pt.getX() > xMax)
					xMax = pt.getX();
				if (pt.getY() > yMax)
					yMax = pt.getY();
			}
			retval = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
		}
		else
		{
			retval = m_path.getBounds2D();
		}
		return retval;
	}

	/**
	 * Returns Shape object of geometry for display.
	 * @return shape object which can be used directly in 2D display methods.
	 */
	public GeneralPath getShape()
	{
		return(m_path);
	}

	/**
	 * Return points in sub path of this path.
	 * @param offset offset at which new path is to begin.
	 * @param length length of new path.
	 * @return new path containing part of this path.
	 */
	public ArrayList<Point2D> getSubPathPoints(double offset, double length)
	{
		float coords[] = new float[6];
		int segmentType;
		double sumDistances = 0.0;
		double xStart = 0.0, yStart = 0.0;
		double xEnd = 0.0, yEnd = 0.0;
		double xMoveTo = 0.0, yMoveTo = 0.0;
		boolean addingPoints = false;
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		double angle = 0.0;

		PathIterator pi = m_path.getPathIterator(Constants.IDENTITY_MATRIX,
			Constants.MM_PER_INCH / Constants.getScreenResolution());

		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
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
				double distance = Math.sqrt((xEnd - xStart) * (xEnd - xStart) +
					(yEnd - yStart) * (yEnd - yStart));

				if (sumDistances + distance >= offset)
				{
					int nPoints = points.size();
					if (nPoints == 0)
					{
						/*
						 * Found line segment on which sub path begins.
						 * Calculate exact point at start offset on line.
						 */
						double d;
						if (distance > 0)
						{
							d = (offset - sumDistances) / distance;
							if (d < 0)
								distance += -d * distance;
						}
						else
						{
							d = 0.0;
						}

						xStart += d * (xEnd - xStart);
						yStart += d * (yEnd - yStart);
						points.add(new Point2D.Double(xStart, yStart));
						addingPoints = true;
					}

					if (addingPoints)
					{
						if (sumDistances + distance > offset + length)
						{
							/*
							 * Current point is beyond end.
							 * Find exact point at distance length on line.
							 */
							double d;
							if (distance > 0) 
								d = ((offset + length) - sumDistances) / distance;
							else
								d = 0.0;

							xStart += d * (xEnd - xStart);
							yStart += d * (yEnd - yStart);
							points.add(new Point2D.Double(xStart, yStart));

							addingPoints = false;
						}
						else
						{
							points.add(new Point2D.Double(xEnd, yEnd));
							angle = Math.atan2(yEnd - yStart, xEnd - xStart);
						}
					}
				}
				sumDistances += distance;

				xStart = xEnd;
				yStart = yEnd;
			}
			pi.next();
		}

		/*
		 * Extend final segment, if necessary, to be long enough to contain
		 * whole length.
		 */
		if (addingPoints)
		{
			double remainingDistance = offset + length - sumDistances;
			xEnd += Math.cos(angle) * remainingDistance;
			yEnd += Math.sin(angle) * remainingDistance;
			points.add(new Point2D.Double(xEnd, yEnd));
		}
		return(points);
	}

	/**
	 * Clear path, removing all coordinates.
	 */
	public void reset()
	{
		m_path.reset();
		m_rotations.clear();
		m_moveTos.clear();
		m_nLineTos = 0;
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
	 * Returns angle of first line segment in path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return angle of first line segment in radians.
	 */
	public double getStartAngle(double resolution)
	{
		double angles[] = walkPath(CALCULATE_START_ANGLE, resolution);
		return(angles[0]);
	}

	/**
	 * Returns angle of last line segment in path.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return angle of last line segment in radians.
	 */
	public double getEndAngle(double resolution)
	{
		double angles[] = walkPath(CALCULATE_END_ANGLE, resolution);
		return(angles[0]);
	}

	/**
	 * Returns first point in path.
	 * @return first point or null if path is empty.
	 */
	public Point2D getStartPoint()
	{
		/*
		 * If path is only points then return the last point.
		 */
		if (m_moveTos.size() > 0 && m_nLineTos == 0) 
			return(m_moveTos.get(0));

		PathIterator pi = m_path.getPathIterator(m_identityMatrix);
		float coords[] = new float[6];
		Point2D retval = null;
		if (!pi.isDone())
		{
			/*
			 * Take first point of path.
			 */
			pi.currentSegment(coords);
			retval = new Point2D.Float(coords[0], coords[1]);
		}
		return(retval);
	}

	/**
	 * Returns last point in path.
	 * @return last point or null if path is empty.
	 */
	public Point2D getEndPoint()
	{
		/*
		 * If path is only points then return the last point.
		 */
		if (m_moveTos.size() > 0 && m_nLineTos == 0) 
			return(m_moveTos.get(m_moveTos.size() - 1));

		Point2D retval = null;
		PathIterator pi = m_path.getPathIterator(m_identityMatrix);
		float coords[] = new float[6];
		int segmentType;
		float xMoveTo = 0, yMoveTo = 0;

		boolean isPathEmpty = true;
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				xMoveTo = coords[0];
				yMoveTo = coords[1];
			}
			else if (segmentType == PathIterator.SEG_CLOSE)
			{
				coords[0] = xMoveTo;
				coords[1] = yMoveTo;
			}
			else if (segmentType == PathIterator.SEG_QUADTO)
			{
				coords[1] = coords[3];
				coords[0] = coords[2];
			}
			else if (segmentType == PathIterator.SEG_CUBICTO)
			{
				coords[1] = coords[5];
				coords[0] = coords[4];
			}
			isPathEmpty = false;
			pi.next();
		}
		if (!isPathEmpty)
			retval = new Point2D.Double(coords[0], coords[1]);
		return(retval);
	}

	/**
	 * Walks path, calculating length, area, centroid or angle.  Length or area
	 * for each moveTo, lineTo, ... part is calculated separately.
	 * If the path is not closed then the calculated area is meaningless.
	 * @param attributeToCalculate is type of calculation to make
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return array with length, angle or area of each part of the path.
	 */
	private double []walkPath(int attributeToCalculate, double resolution)
	{
		int segmentType;
		PathIterator pi = m_path.getPathIterator(m_identityMatrix, resolution);
		float coords[] = new float[6];
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd = 0, yEnd = 0;
		float xPrevious = 0, yPrevious = 0;
		float xMoveTo = 0.0f, yMoveTo =0.0f;
		double partLengths[], partAreas[], centroid[], angles[];
		int moveToCount = 0;
		int lineToCount = 0;
		double ai, aSum, xSum, ySum;
		int nEls;

		/*
		 * Create array to hold length and area of each part of path.
		 */
		aSum = xSum = ySum = 0.0;
		if (attributeToCalculate == CALCULATE_START_ANGLE || attributeToCalculate == CALCULATE_END_ANGLE)
			nEls = 1;
		else if (attributeToCalculate == CALCULATE_CENTROID)
			nEls = 2;
		else
			nEls = getMoveToCount();

		angles = centroid = partAreas = partLengths = new double[nEls];
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
				lineToCount++;
				if (attributeToCalculate == CALCULATE_START_ANGLE)
				{
					/*
					 * First two points are enough to determine angle of first line segment.
					 */
					break;
				}

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
				
				xPrevious = xStart;
				yPrevious = yStart;

				xStart = xEnd;
				yStart = yEnd;
			}
			pi.next();
		}

		if (attributeToCalculate == CALCULATE_START_ANGLE)
		{
			if (lineToCount > 0)
				angles[0] = Math.atan2(coords[1] - yStart, coords[0] - xStart);
		}
		else if (attributeToCalculate == CALCULATE_END_ANGLE)
		{
			if (lineToCount > 0)
				angles[0] = Math.atan2(yEnd - yPrevious, xEnd - xPrevious);
		}
		else if (attributeToCalculate == CALCULATE_CENTROID)
		{
			centroid[0] = xSum / (3.0 * aSum);
			centroid[1] = ySum / (3.0 * aSum);
		}
		return(partLengths);
	}

	/**
	 * Return current path as a geometry argument.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return current path.
	 */
	public Argument toArgument(double resolution)
	{
		Argument retval;
		int moveCount = m_moveTos.size();
		double []coords;

		if (moveCount == 0)
		{
			/*
			 * Empty path.
			 */
			retval = Argument.emptyGeometry;
		}
		else if (m_nLineTos == 0)
		{
			/*
			 * Path with only move points.
			 */
			Point2D.Float pt;
			if (moveCount == 1)
			{
				pt = (Point2D.Float)m_moveTos.get(0);
				coords = new double[]{Argument.GEOMETRY_POINT, 1,
					Argument.MOVETO, pt.x, pt.y};
			}
			else
			{
				/*
				 * Multiple move points so make a MULTIPOINT geometry.
				 */
				coords = new double[2 + moveCount * 5];
				coords[0] = Argument.GEOMETRY_MULTIPOINT;
				coords[1] = moveCount;
				int index = 2;
				for (int i = 0; i < moveCount; i++)
				{
					pt = (Point2D.Float)m_moveTos.get(i);
					coords[index++] = Argument.GEOMETRY_POINT;
					coords[index++] = 1;
					coords[index++] = Argument.MOVETO;
					coords[index++] = pt.x;
					coords[index++] = pt.y;
				}
			}
			retval = new Argument((int)coords[0], coords);
		}
		else
		{
			/*
			 * Path contains move and draw points.
			 */
			int segmentType;
			float pathCoords[] = new float[6];
			PathIterator pi = m_path.getPathIterator(m_identityMatrix, resolution);

			/*
			 * Walk through path and find number of slots needed
			 * for complete geometry.
			 */
			int coordCounter = 2;
			while (!pi.isDone())
			{
				segmentType = pi.currentSegment(pathCoords);
				if (segmentType == PathIterator.SEG_MOVETO)
					coordCounter += 5;
				else
					coordCounter += 3;
				pi.next();
			}
			coords = new double[coordCounter];

			/*
			 * Walk through path again, building geometry.
			 */
			boolean isClosed = false;
			double xMove = Double.MAX_VALUE;
			double yMove = Double.MAX_VALUE;
			double x = xMove;
			double y = yMove;
			int nPolygons = 0;
			int nLinestrings = 0;
			coordCounter = 0;
			int index = 0, startIndex = 0;
			pi = m_path.getPathIterator(m_identityMatrix, resolution);
			while (!pi.isDone())
			{
				segmentType = pi.currentSegment(pathCoords);
				if (segmentType == PathIterator.SEG_MOVETO)
				{
					/*
					 * Starting new line or polygon, add any previous
					 * line or polygon to path.
					 */
					if (coordCounter > 0)
					{
						if (x == xMove && y == yMove)
							isClosed = true;

						if (isClosed)
						{
							coords[startIndex] = Argument.GEOMETRY_POLYGON;
							nPolygons++;
						}
						else
						{
							coords[startIndex] = Argument.GEOMETRY_LINESTRING;
							nLinestrings++;
						}
						coords[startIndex + 1] = coordCounter;
						startIndex = index;
					}
					coordCounter = 1;
					isClosed = false;

					/*
					 * Skip two slots with geometry type and geometry count --
					 * we will fill them in later when we know their values.
					 */
					index += 2;

					coords[index] = Argument.MOVETO;
					coords[index + 1] = xMove = pathCoords[0];
					coords[index + 2] = yMove = pathCoords[1];
					index += 3;
				}
				else if (segmentType == PathIterator.SEG_CLOSE)
				{
					isClosed = true;
					coords[index] = Argument.LINETO;
					coords[index + 1] = xMove;
					coords[index + 2] = yMove;
					index += 3;
					coordCounter++;
				}
				else
				{
					coords[index] = Argument.LINETO;
					coords[index + 1] = pathCoords[0];
					coords[index + 2] = pathCoords[1];
					index += 3;
					coordCounter++;
				}
				x = pathCoords[0];
				y = pathCoords[1];
				pi.next();
			}

			if (coordCounter > 1)
			{
				/*
				 * Finish off last geometry we were adding.
				 */
				if (x == xMove && y == yMove)
					isClosed = true;

				if (isClosed)
				{
					coords[startIndex] = Argument.GEOMETRY_POLYGON;
					nPolygons++;
				}
				else
				{
					coords[startIndex] = Argument.GEOMETRY_LINESTRING;
					nLinestrings++;
				}
				coords[startIndex + 1] = coordCounter;
			}
			if (nPolygons + nLinestrings > 1)
			{
				/*
				 * Shift array along and make it into a multiple geometry.
				 */
				System.arraycopy(coords, 0, coords, 2, coords.length - 2);
				if (nLinestrings == 0)
					coords[0] = Argument.GEOMETRY_MULTIPOLYGON;
				else if (nPolygons == 0)
					coords[0] = Argument.GEOMETRY_MULTILINESTRING;
				else
					coords[0] = Argument.GEOMETRY_COLLECTION;
				coords[1] = nPolygons + nLinestrings;
			}
			retval = new Argument((int)coords[0], coords);
		}
		return(retval);
	}

	/**
	 * Return new path, with all coordinates in path shifted by a fixed amount.
	 * @param xShift distance in millimetres to shift X coordinate values.
	 * @param yShift distance in millimetres to shift Y coordinate values.
	 */
	public GeometricPath translatePath(double xShift, double yShift)
	{
		GeometricPath retval = new GeometricPath();

		/*
		 * Create translated copy of path.
		 */
		retval.m_path = (GeneralPath)(m_path.clone());
		AffineTransform translateTransform =
			AffineTransform.getTranslateInstance(xShift, yShift);
		retval.m_path.transform(translateTransform);
		
		/*
		 * Replace list of moveto points and rotations too.
		 */
		for (int i = 0; i < m_moveTos.size(); i++)
		{
			Point2D.Float pt = (Point2D.Float)m_moveTos.get(i);
			pt = new Point2D.Float((float)(pt.x + xShift), (float)(pt.y + yShift));
			retval.m_moveTos.add(pt);
		}
		retval.m_nLineTos = m_nLineTos;
		retval.m_rotations = new ArrayList<Double>(m_rotations.size());
		for (Double d : m_rotations)
			retval.m_rotations.add(d);

		return(retval);
	}

	/**
	 * Remove line segments that would result in loops in the
	 * parallel path.
	 * @param lineEquations equations of each line segment of path.
	 * @param distance parallel distance for new path.
	 * @param isClosed when true, original path interpreted as closed path.
	 * @return list of line equation with segments that make a loop removed.
	 */
	private ArrayList<LineEquation> eliminateParallelLoops(ArrayList<LineEquation> lineEquations,
		double distance, boolean isClosed)
	{
		ArrayList<LineEquation> retval;
		ArrayList<LineEquation> parallelEquations;
		LineEquation eq = null, lastEq = null;
		LineEquation lastParallelEq = null, parallelEq = null;
		Point2D.Double intersectionPt = null, lastIntersectionPt = null;
		Point2D.Double closePt = null;
		int nEquations = lineEquations.size();

		if (nEquations < 3)
		{
			/*
			 * Simple paths cannot have any loops.
			 */
			retval = lineEquations;
		}
		else
		{
			retval = new ArrayList<LineEquation>(nEquations);
			parallelEquations = new ArrayList<LineEquation>(nEquations);
			for (int i = 0; i <= nEquations; i++)
			{
				if (i < nEquations)
				{
					eq = lineEquations.get(i);
					parallelEq = eq.createParallel(distance);
				}

				if (i == 0)
				{
					if (isClosed)
					{
						LineEquation p = lineEquations.get(nEquations - 1);
						closePt = parallelEq.intersect(p.createParallel(distance),
							false);

						/*
						 * Path closes with a line parallel to first line.
						 */
						if (closePt == null)
							closePt = parallelEq.getStartPoint();
						intersectionPt = closePt;
					}
					else
					{
						intersectionPt = parallelEq.getStartPoint();
					}
				}
				else
				{
					/*
					 * Find intersection of last two parallel lines.
					 */
					if (i == nEquations)
					{
						if (isClosed)
							intersectionPt = closePt;
						else
							intersectionPt = parallelEq.getEndPoint();
					}
					else
					{
						intersectionPt = parallelEq.intersect(lastParallelEq, false);
					}
					if (intersectionPt == null)
					{
						/*
						 * Line continues in same direction.  Add segment.
						 */
						retval.add(lastEq);
						parallelEquations.add(lastParallelEq);
						intersectionPt = lastIntersectionPt;
					}
					else
					{
						double xDiff = intersectionPt.x - lastIntersectionPt.x;
						double yDiff = intersectionPt.y - lastIntersectionPt.y;
						double intersectionAngle = Math.atan2(yDiff, xDiff);
						double lastIntersectionAngle = lastParallelEq.getAngle();

						/*
						 * Skip line segment if the parallel line goes in
						 * opposite direction to original line because this
						 * would create a loop.
						 * Use approximate test to avoid rounding problems.
						 */
						double angleDiff = Math.abs(intersectionAngle - lastIntersectionAngle);
						if (angleDiff < 0.1 || angleDiff > Math.PI * 2 - 0.1)
						{
							retval.add(lastEq);
							parallelEquations.add(lastParallelEq);
						}
						else
						{
							int nParallel = parallelEquations.size();
							if (nParallel > 0)
							{
								/*
								 * Continue from the intersection of this line
								 * segment with the last line segment.
								 */
								LineEquation p1 = parallelEquations.get(nParallel - 1);
								intersectionPt = p1.intersect(parallelEq, false);
								if (intersectionPt == null)
									intersectionPt = p1.getStartPoint();
							}
						}
					}
				}

				lastEq = eq;
				lastParallelEq = parallelEq;
				lastIntersectionPt = intersectionPt;
			}
		}
		return(retval);
	}

	/**
	 * Create paths parallel to original path.
	 * @param distances list of parallel distances for new paths.
	 * @param lineEquations equations of each line segment of original path.
	 * @param path path to add to.
	 * @param isClosed when true, original path interpreted as closed path.
	 */
	private GeometricPath createParallelPath(double []distances,
		ArrayList<LineEquation> lineEquations, GeometricPath path, boolean isClosed)
	{
		/*
		 * Create parallel path at each distance given.
		 */
		LineEquation eq, lastParallelEq;
		for (int i = 0; i < distances.length; i++)
		{
			boolean addedMoveTo = false;
			LineEquation parallelEq = null;

			ArrayList<LineEquation> checkedEquations;
			checkedEquations = eliminateParallelLoops(lineEquations, distances[i],
				isClosed);
			int nEquations = checkedEquations.size();

			/*
			 * For closed paths, first segment must be intersected
			 * with last segment.
			 */
			if (isClosed && nEquations > 0)
			{
				lastParallelEq = checkedEquations.get(nEquations - 1);
				lastParallelEq = lastParallelEq.createParallel(distances[i]);
			}
			else
			{
				lastParallelEq = null;
			}

			for (int j = 0; j < nEquations; j++)
			{
				eq = checkedEquations.get(j);
				parallelEq = eq.createParallel(distances[i]);

				/*
				 * Find intersection of line parallel to current line segment and
				 * parallel line segment.  Add this point to path.
				 */
				Point2D.Double pt;
				if (lastParallelEq == null)
					pt = parallelEq.getStartPoint();
				else
					pt = parallelEq.intersect(lastParallelEq, false);

				/*
				 * Skip parallel line segments that do not intersect.
				 */
				if (pt != null)
				{
					if (addedMoveTo)
					{
							path.lineTo((float)pt.x, (float)pt.y);
					}
					else
					{
							path.moveTo((float)pt.x, (float)pt.y, 0);
							addedMoveTo = true;
					}
				}
				lastParallelEq = parallelEq;
			}

			/*
			 * Add final point to path, closing the path if original path
			 * was also closed. 
			 */
			if (isClosed)
			{
				if (addedMoveTo)
					path.closePath();
			}
			else if (parallelEq != null)
			{
				Point2D.Double pt = parallelEq.getEndPoint();
				path.lineTo((float)pt.x, (float)pt.y);
			}
		}
		return(path);
	}

	/**
	 * Return new paths at parallel distances to original path.
	 * @param distances list of parallel distances for new paths.
	 * @param resolution is size of a pixel in mm, curves are expanded
	 * to be no less accurate than this value.
	 * @return new path, parallel to original path.
	 */
	public GeometricPath parallelPath(double []distances, double resolution)
	{
		PathIterator pi;
		int segmentType;
		GeometricPath retval = new GeometricPath();
		float coords[] = new float[6];
		float xMoveTo = 0.0f, yMoveTo = 0.0f;
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd = 0.0f, yEnd = 0.0f;
		boolean isPathClosed = false;
		ArrayList<LineEquation> lineEquations = new ArrayList<LineEquation>();

		/*
		 * Flatten arcs in path and make list of line equations
		 * for each segment of path.
		 */
		pi = m_path.getPathIterator(m_identityMatrix, resolution);	
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				/*
				 * Create parallel paths for last sub-path,
				 * then begin a new path.
				 */
				if (!lineEquations.isEmpty())
				{
					if (xEnd == xMoveTo && yEnd == yMoveTo)
						isPathClosed = true;
					createParallelPath(distances, lineEquations,
						retval, isPathClosed);
					lineEquations.clear();
					isPathClosed = false;
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
					isPathClosed = true;
				}
				else
				{
					xEnd = coords[0];
					yEnd = coords[1];
				}

				if (xStart != xEnd || yStart != yEnd)
				{
					LineEquation eq = new LineEquation(xStart, yStart, xEnd, yEnd);
					lineEquations.add(eq);
				}
				xStart = xEnd;
				yStart = yEnd;
			}

			/*
			 * Move on to next segment in path.
			 */
			pi.next();
		}

		/*
		 * Add parallel path for final sub-path.
		 */
		if (!lineEquations.isEmpty())
		{
			if (xEnd == xMoveTo && yEnd == yMoveTo)
				isPathClosed = true;
			createParallelPath(distances, lineEquations, retval, isPathClosed);
		}

		return(retval);
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
		
		pi = m_path.getPathIterator(m_identityMatrix, resolution);	
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
				int i = 0;
				while (nextOffset + i * spacing < segmentLength ||
					NumericalAnalysis.equals(nextOffset + i * spacing, segmentLength))
				{
					x = (float)(xStart + (nextOffset + i * spacing) * cosAngle);
					y = (float)(yStart + (nextOffset + i * spacing) * sinAngle);

					retval.moveTo(x, y, segmentAngle);
					i++;
				}

				/*
				 * Get ready for next line segment.
				 */
				nextOffset += spacing * i;
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
		double xMin, yMin, xMax, yMax, x, y;

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
		 * Align stripes so that stripes in neighbouring polygons match.
		 */
		double cosAngle = Math.cos(angle);
		double xOrigin = bounds.getMinX();
		double yOrigin = bounds.getMinY();
		if (Math.abs(cosAngle) > ONE_OVER_SQRT_TWO)
		{
			/*
			 * Hatch lines are closer to horizontal.
			 * Move origin of hatch lines up or down so that they will
			 * pass through (0, 0).
			 */
			double verticalSpacing = spacing / cosAngle;
			y = Math.tan(angle) * bounds.getMinX();
			long nStripes = Math.round((yOrigin - y) / verticalSpacing);
			yOrigin = y + nStripes * verticalSpacing;
		}
		else
		{
			/*
			 * Hatch lines are closer to vertical.
			 * Move origin of hatch lines left or right so that they will
			 * pass through (0, 0).
			 */
			double horizontalSpacing = spacing / Math.sin(angle);
			x = bounds.getMinY() / Math.tan(angle);
			long nStripes = Math.round((xOrigin - x) / horizontalSpacing);
			xOrigin = x + nStripes * horizontalSpacing;
		}

		/*
		 * Create stripes horizontally through rotated rectangle.
		 */
		rotateTransform.translate(xOrigin, yOrigin);
		rotateTransform.rotate(angle);

		/*
		 * Start stripes just below polyon, continue to above the polygon
		 * and extend each stripe by half of spacing so that symbols
		 * drawn along stripes do not stop before the boundary.
		 */
		double halfSpacing = spacing / 2;
		y = Math.floor((yMin - spacing) / spacing) * spacing;
		while (y <= yMax + spacing)
		{
			pts[0] = xMin - halfSpacing;
			pts[1] = pts[3] = y;
			pts[2] = xMax + halfSpacing;
			
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

	/**
	 * Sort list of offset, length pairs into increasing order based on offset.
	 * @param offsets array of offsets.
	 * @param lengths array of lengths at each each offset.
	 */
	private void sortParts(double []offsets, double []lengths)
	{
		/*
		 * Bubblesort elements into correct order.
		 */
		int nParts = offsets.length;
		for (int i = 1; i < nParts; i++)
		{
			for (int j = i; j < nParts; j++)
			{
				if (offsets[j] < offsets[j - 1])
				{
					double swap = offsets[j];
					offsets[j] = offsets[j - 1];
					offsets[j - 1] = swap;
					swap = lengths[j];
					lengths[j] = lengths[j - 1];
					lengths[j - 1] = swap;
				}
			}
		}
	}

	/**
	 * Merge overlapping parts of path to be selected.
	 * @param offsets offsets along existing path to select.
	 * @param lengths length of existing path to select at each offset.
	 * @retval number of parts after merging.
	 */
	private int mergeSelectedParts(double []offsets, double []lengths)
	{
		double []mergedOffsets = new double[offsets.length];
		double []mergedLengths = new double[lengths.length];
		int nMergedParts = 1;
		double start;
		double end, lastEnd;
		int i;

		/*
		 * Chop lines that begin before start of line.
		 */
		for (i = 0; i < offsets.length; i++)
		{
			if (offsets[i] < 0)
			{
				lengths[i] += offsets[i];
				offsets[i] = 0;
			}
			if (lengths[i] < 0)
				lengths[i] = 0;
		}
		
		/*
		 * Sort parts of path to select into increasing offset order.
		 */
		sortParts(offsets, lengths);

		mergedOffsets[0] = offsets[0];
		mergedLengths[0] = lengths[0];

		i = 1;
		while (i < offsets.length)
		{
			start = offsets[i];
			end = offsets[i] + lengths[i];
			lastEnd = mergedOffsets[nMergedParts - 1] + mergedLengths[nMergedParts - 1];
			if (start < lastEnd)
			{
				if (end > lastEnd)
					mergedLengths[nMergedParts - 1] = end - mergedOffsets[nMergedParts - 1];
			}
			else
			{
				mergedOffsets[nMergedParts] = offsets[i];
				mergedLengths[nMergedParts] = lengths[i];
				nMergedParts++;
			}
			i++;
		}
		System.arraycopy(mergedOffsets, 0, offsets, 0, nMergedParts);
		System.arraycopy(mergedLengths, 0, lengths, 0, nMergedParts);
		return(nMergedParts);
	}

	/**
	 * Create new path with selected parts of existing path.
	 * @param offsets offsets along existing path to select.
	 * @param lengths length of existing path to select at each offset.
	 * @return new path containing selected parts of existing path.
	 */
	public GeometricPath selectPath(double []offsets, double []lengths,
		double resolution)
	{
		PathIterator pi;
		GeometricPath retval = new GeometricPath();

		float coords[] = new float[6];
		int segmentType;
		double segmentLength = 0.0, segmentAngle;
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd = 0.0f, yEnd = 0.0f;
		float xMoveTo = 0.0f, yMoveTo = 0.0f;
		float x = 0.0f, y = 0.0f;
		double sinAngle, cosAngle;
		double pathLength = 0;
		int partIndex = 0;
		boolean selectingPath = false;
		int segmentCounter = 0;

		/*
		 * Merge any overlapping parts of path to be selected.
		 */
		int nParts = mergeSelectedParts(offsets, lengths);

		pi = m_path.getPathIterator(m_identityMatrix, resolution);	
		while (!pi.isDone() && partIndex < nParts)
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				xStart = xMoveTo = coords[0];
				yStart = yMoveTo = coords[1];
				if (selectingPath)
					retval.moveTo(xStart, yStart, 0);
			}
			else
			{
				if (segmentCounter > 0)
				{
					/*
				 	 * End point of last segment becomes start point of this segment.
				 	 */
					xStart = xEnd;
					yStart = yEnd;
				}

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

				/*
				 * Does current line segment overlap part of path currently being selected?
				 */
				while (partIndex < nParts && pathLength + segmentLength > offsets[partIndex])
				{
					if (selectingPath)
					{
						if (pathLength + segmentLength <= offsets[partIndex] + lengths[partIndex])
						{
							/*
							 * Add whole line segment to path.
							 */
							retval.lineTo(xEnd, yEnd);
							break;
						}
						else
						{
							/*
							 * Selection ends in this line segment.
							 * Cut out piece required and then stop selecting path. 
							 */
							segmentAngle = Math.atan2(yEnd - yStart, xEnd - xStart);
							cosAngle = Math.cos(segmentAngle);
							sinAngle = Math.sin(segmentAngle);

							double d = offsets[partIndex] + lengths[partIndex] - pathLength;
							x = (float)(xStart + cosAngle * d);
							y = (float)(yStart + sinAngle * d);
							retval.lineTo(x, y);

							selectingPath = false;
							partIndex++;
						}
					}
					else
					{
						segmentAngle = Math.atan2(yEnd - yStart, xEnd - xStart);
						cosAngle = Math.cos(segmentAngle);
						sinAngle = Math.sin(segmentAngle);

						/*
						 * Start selecting new part of existing path.
						 */
						x = (float)(xStart + cosAngle * (offsets[partIndex] - pathLength));
						y = (float)(yStart + sinAngle * (offsets[partIndex] - pathLength));
						retval.moveTo(x, y, 0);
						selectingPath = true;
					}
				}

				pathLength += segmentLength;
				segmentCounter++;
			}
			pi.next();
		}

		if (selectingPath)
		{
			if (offsets[partIndex] + lengths[partIndex] >= pathLength)
			{
				retval.lineTo(xEnd, yEnd);
			}
			else
			{
				segmentAngle = Math.atan2(yEnd - yStart, xEnd - xStart);
				cosAngle = Math.cos(segmentAngle);
				sinAngle = Math.sin(segmentAngle);

				double d = offsets[partIndex] + lengths[partIndex] -
					(pathLength - segmentLength);
				x = (float)(xStart + cosAngle * d);
				y = (float)(yStart + sinAngle * d);

				retval.lineTo(x, y);
			}
		}
		return(retval);
	}

	private class PathElement
	{
		public int mSegmentType;
		public float mX, mY;
		
		public PathElement(int segmentType, float x, float y)
		{
			mSegmentType = segmentType;
			mX = x;
			mY = y;
		}
	}

	/**
	 * Create new path in reverse direction.
	 * @param resolution is size of a pixel in mm, curves are expanded to be no
	 * less accurate than this value.
	 * @return new path in opposite direction to existing path.
	 */
	public GeometricPath reversePath(double resolution)
	{
		PathIterator pi;
		float coords[] = new float[6];
		float xMoveTo = 0, yMoveTo = 0;
		int segmentType;
		GeometricPath retval = new GeometricPath();
		Stack<PathElement> path = new Stack<PathElement>();

		if (getMoveToCount() > 0 && getLineToCount() == 0)
		{
			/*
			 * Path is just a series of points so we can simply
			 * reverse the point order.
			 */
			for (int i = m_moveTos.size() - 1; i >= 0; i--)
			{
				Point2D pt = m_moveTos.get(i);
				Double rotation = m_rotations.get(i);
				retval.moveTo((float)pt.getX(), (float)pt.getY(), rotation.doubleValue());
			}
			return(retval);
		}

		/*
		 * Build stack with current path.
		 */
		pi = m_path.getPathIterator(m_identityMatrix, resolution);	
		while (!pi.isDone())
		{
			segmentType = pi.currentSegment(coords);
			if (segmentType == PathIterator.SEG_MOVETO)
			{
				xMoveTo = coords[0];
				yMoveTo = coords[1];
				path.push(new PathElement(segmentType, xMoveTo, yMoveTo));
			}
			else if (segmentType == PathIterator.SEG_CLOSE)
			{
				/*
				 * Remember point that path closes back to.
				 */
				path.push(new PathElement(segmentType, xMoveTo, yMoveTo));
			}
			else
			{
				path.push(new PathElement(segmentType, coords[0], coords[1]));
			}
			pi.next();
		}

		/*
		 * Build new path from stack with reverse order of coordinates.
		 */
		boolean isClosedPath = false;
		boolean isMovePoint = true;
		while (!path.isEmpty())
		{
			PathElement el = path.pop();

			if (isMovePoint || el.mSegmentType == PathIterator.SEG_CLOSE)
			{
				/*
				 * The last point of the path (or of a subpath) becomes the
				 * first point of the reversed path.
				 */
				xMoveTo = el.mX;
				yMoveTo = el.mY;
				isClosedPath = (el.mSegmentType == PathIterator.SEG_CLOSE);
				retval.moveTo(xMoveTo, yMoveTo, 0);
				isMovePoint = false;
			}
			else if (el.mSegmentType == PathIterator.SEG_LINETO)
			{
				retval.lineTo(el.mX, el.mY);
			}
			else
			{
				/*
				 * A moveto point.
				 */
				if (isClosedPath)
					retval.closePath();
				else
					retval.lineTo(el.mX, el.mY);

				isClosedPath = false;

				/*
				 * Next point is the last point of a sub path.
				 */
				isMovePoint = true;
			}			
		}
		return(retval);
	}
}
