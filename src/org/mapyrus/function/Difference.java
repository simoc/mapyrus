/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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
package org.mapyrus.function;

import java.awt.geom.Rectangle2D;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Function returning difference between two geometries.
 * For example, the difference of two overlapping polygons is a smaller polygon
 * containing the parts of the first polygon that are not in the second.
 */
public class Difference extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		Argument retval;

		/*
		 * If bounding rectangles of geometries do not overlap
		 * then difference of arg1 and arg2 must be arg1.
		 */
		Rectangle2D.Double rect1 = arg1.getGeometryBoundingBox();
		Rectangle2D.Double rect2 = arg2.getGeometryBoundingBox();
		if (rect1 == null || rect2 == null)
			return(arg1);
		if (rect2.getMaxX() < rect1.getMinX() || rect2.getMinX() > rect1.getMaxX() ||
			rect2.getMaxY() < rect1.getMinY() || rect2.getMinY() > rect1.getMaxY())
		{
			return(arg1);
		}

		String wkt1 = arg1.toString();
		String wkt2 = arg2.toString();

		try
		{
			Geometry g1 = new WKTReader().read(wkt1);
			Geometry g2 = new WKTReader().read(wkt2);
			Geometry difference = g1.difference(g2);
			retval = new Argument(difference.toText());
		}
		catch (ParseException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		catch (IllegalArgumentException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(2);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(2);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("difference");
	}
}
