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

package org.mapyrus.geom;

import java.awt.geom.Rectangle2D;

/**
 * Geometrical test functions.
 */
public class Geometry
{
	/**
	 * Find whether two rectangles overlap.
	 * @param r1 first rectangle.
	 * @param xMin minimum X coordinate of second rectangle.
	 * @param yMin minimum Y coordinate of second rectangle.
	 * @param xMax maximum X coordinate of second rectangle.
	 * @param yMax maximum Y coordinate of second rectangle.
	 * @return true if rectangles overlap.
	 */
	public static boolean overlaps(Rectangle2D.Double r,
		double xMin, double yMin, double xMax, double yMax)
	{
		boolean retval = (xMin >= r.getMinX() && xMin <= r.getMaxX()) ||
			(xMax >= r.getMinX() && xMax <= r.getMaxX()) ||
			(r.getMaxX() >= xMin && r.getMaxX() <= xMax) ||
			(r.getMinX() >= xMin && r.getMinX() <= xMax);
		if (retval)
		{
			retval = (yMin >= r.getMinY() && yMin <= r.getMaxY()) ||
				(yMax >= r.getMinY() && yMax <= r.getMaxY()) ||
				(r.getMaxY() >= yMin && r.getMaxY() <= yMax) ||
				(r.getMinY() >= yMin && r.getMinY() <= yMax);
		}
		return(retval);
	}
}
