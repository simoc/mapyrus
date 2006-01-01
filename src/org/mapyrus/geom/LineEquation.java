/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005, 2006 Simon Chenery.
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
package org.mapyrus.geom;

import java.awt.geom.Point2D;

import org.mapyrus.NumericalAnalysis;

/**
 * A line stored as an equation in the form ax+by+c=0.
 */
public class LineEquation
{
	/*
	 * Equation of line in form: ax + by + c = 0.
	 */
	private double mA, mB, mC;

	/*
	 * Two points given to define line.
	 */
	private double mX1, mX2, mY1, mY2;

	/**
	 * Create new line equation from two points on line.
	 * @param x1 X coordinate of point on line.
	 * @param y1 Y coordinate of point on line.
	 * @param x2 X coordinate of point on line.
	 * @param y2 Y coordinate of point on line.
	 */
	public LineEquation(double x1, double y1, double x2, double y2)
	{
		mA = y2 - y1;
		mB = x1 - x2;
		mC = x2 * y1 - x1 * y2;

		mX1 = x1;
		mY1 = y1;
		mX2 = x2;
		mY2 = y2;
	}

	/**
	 * Create new line equation parallel to existing line.
	 * @param parallelDistance parallel distance to the right of line
	 * to create new line. 
	 */
	public LineEquation createParallel(double parallelDistance)
	{
		double xDiff = mX2 - mX1;
		double yDiff = mY2 - mY1;
		double theta = Math.atan2(yDiff, xDiff);
		theta -= Math.PI / 2;
		double x1 = mX1 + Math.cos(theta) * parallelDistance;
		double y1 = mY1 + Math.sin(theta) * parallelDistance;

		LineEquation retval = new LineEquation(x1, y1, x1 + xDiff, y1 + yDiff);
		return(retval);
	}

	/**
	 * Determine whether a point on line is between the two points
	 * given to define the line.
	 * @param eq equation for line.
	 * @param x X coordinate of point on line.
	 * @param y Y coordinate of point on line.
	 * @return true if point is between two points of line.
	 */
	private boolean isBetweenPoints(LineEquation eq, double x, double y)
	{
		boolean retval = true;

		if (eq.mX1 <= eq.mX2)
		{
			if (x < eq.mX1 || x > eq.mX2)
				retval = false;
		}
		else
		{
			if (x > eq.mX1 || x < eq.mX2)
				retval = false;
		}

		if (eq.mY1 <= eq.mY2)
		{
			if (y < eq.mY1 || y > eq.mY2)
				retval = false;
		}
		else
		{
			if (y > eq.mY1 || y < eq.mY2)
				retval = false;
		}

		return(retval);
	}

	/**
	 * Return start point given for line equation.
	 * return start point.
	 */
	public Point2D.Double getStartPoint()
	{
		Point2D.Double retval = new Point2D.Double(mX1, mY1);
		return(retval);
	}

	/**
	 * Return end point given for line equation.
	 * return end point.
	 */
	public Point2D.Double getEndPoint()
	{
		Point2D.Double retval = new Point2D.Double(mX2, mY2);
		return(retval);
	}

	/**
	 * Calculate intersection of this line with another line.
	 * @param eq2 equation of line to intersect with.
	 * @param betweenPoints if true then only return intersection if it is
	 * between two points given for both lines. 
	 * @return intersection point, or null if lines do not intersect.
	 */
	public Point2D.Double intersect(LineEquation eq2, boolean betweenPoints)
	{
		Point2D.Double retval;
		double determinant = this.mA * eq2.mB - eq2.mA * this.mB;
		if (NumericalAnalysis.equals(determinant, 0))
		{
			retval = null;
		}
		else
		{
			double x = (this.mB * eq2.mC - eq2.mB * this.mC) / determinant;
			double y = -(this.mA * eq2.mC - eq2.mA * this.mC) / determinant;
			retval = new Point2D.Double(x, y);

			if (betweenPoints)
			{
				/*
				 * Return no intersection if intersection is outside
				 * two points given for each line.
				 */
				if (!isBetweenPoints(this, x, y))
						retval = null;
				if (!isBetweenPoints(eq2, x, y))
						retval = null;
			}
		}
		return(retval);
	}

	/**
	 * Return string representation of line equation.
	 * @return line equation as a string.
	 */
	public String toString()
	{
		return(mA + "x + " + mB + "y + " + mC + " = 0");
	}

	public static void main(String []args)
	{
		LineEquation eq1 = new LineEquation(5, 5, 10, 6);
		LineEquation eq2 = new LineEquation(8, 4, 7, 17);
		Point2D pt = eq1.intersect(eq2, false);
		System.out.println(pt);
	}
}
