/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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
	private double m_A, m_B, m_C;

	/*
	 * Two points given to define line.
	 */
	private double m_X1, m_X2, m_Y1, m_Y2;

	/**
	 * Create new line equation from two points on line.
	 * @param x1 X coordinate of point on line.
	 * @param y1 Y coordinate of point on line.
	 * @param x2 X coordinate of point on line.
	 * @param y2 Y coordinate of point on line.
	 */
	public LineEquation(double x1, double y1, double x2, double y2)
	{
		m_A = y2 - y1;
		m_B = x1 - x2;
		m_C = x2 * y1 - x1 * y2;

		m_X1 = x1;
		m_Y1 = y1;
		m_X2 = x2;
		m_Y2 = y2;
	}

	/**
	 * Create new line equation parallel to existing line.
	 * @param parallelDistance parallel distance to the right of line
	 * to create new line. 
	 */
	public LineEquation createParallel(double parallelDistance)
	{
		double xDiff = m_X2 - m_X1;
		double yDiff = m_Y2 - m_Y1;
		double theta = Math.atan2(yDiff, xDiff);
		theta -= Math.PI / 2;
		double x1 = m_X1 + Math.cos(theta) * parallelDistance;
		double y1 = m_Y1 + Math.sin(theta) * parallelDistance;

		LineEquation retval = new LineEquation(x1, y1, x1 + xDiff, y1 + yDiff);
		return(retval);
	}

	/**
	 * Get angle of line.
	 * @return angle of line in radians.
	 */
	public double getAngle()
	{
		double retval = Math.atan2(m_Y2 - m_Y1, m_X2 - m_X1);
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

		if (eq.m_X1 <= eq.m_X2)
		{
			if (x < eq.m_X1 || x > eq.m_X2)
				retval = false;
		}
		else
		{
			if (x > eq.m_X1 || x < eq.m_X2)
				retval = false;
		}

		if (eq.m_Y1 <= eq.m_Y2)
		{
			if (y < eq.m_Y1 || y > eq.m_Y2)
				retval = false;
		}
		else
		{
			if (y > eq.m_Y1 || y < eq.m_Y2)
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
		Point2D.Double retval = new Point2D.Double(m_X1, m_Y1);
		return(retval);
	}

	/**
	 * Return end point given for line equation.
	 * return end point.
	 */
	public Point2D.Double getEndPoint()
	{
		Point2D.Double retval = new Point2D.Double(m_X2, m_Y2);
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
		double determinant = this.m_A * eq2.m_B - eq2.m_A * this.m_B;
		if (NumericalAnalysis.equals(determinant, 0))
		{
			retval = null;
		}
		else
		{
			double x = (this.m_B * eq2.m_C - eq2.m_B * this.m_C) / determinant;
			double y = -(this.m_A * eq2.m_C - eq2.m_A * this.m_C) / determinant;
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
		return(m_A + "x + " + m_B + "y + " + m_C + " = 0");
	}

	public static void main(String []args)
	{
		LineEquation eq1 = new LineEquation(5, 5, 10, 6);
		LineEquation eq2 = new LineEquation(8, 4, 7, 17);
		Point2D pt = eq1.intersect(eq2, false);
		System.out.println(pt);
	}
}
