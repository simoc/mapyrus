/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Function returning a zero or one value depending on whether
 * one geometry overlaps another.  Overlapping test is only defined
 * for two geometries of the same type.  If geometry types differ then
 * test always returns zero. 
 */
public class Overlaps implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		/*
		 * Geometries cannot possibly overlap if their
		 * bounding rectangles do not overlap.
		 */
		Argument arg1 = (Argument)args.get(0);
		Argument arg2 = (Argument)args.get(1);
		Rectangle2D.Double rect1 = arg1.getGeometryBoundingBox();
		Rectangle2D.Double rect2 = arg2.getGeometryBoundingBox();
		if (rect1 == null || rect2 == null)
			return(Argument.numericZero);
		if (rect2.getMaxX() < rect1.getMinX() || rect2.getMinX() > rect1.getMaxX() ||
			rect2.getMaxY() < rect1.getMinY() || rect2.getMinY() > rect1.getMaxY())
		{
			return(Argument.numericZero);
		}

		String wkt1 = arg1.toString();
		String wkt2 = arg2.toString();

		Argument retval;
		try
		{
			Geometry g1 = new WKTReader().read(wkt1);
			Geometry g2 = new WKTReader().read(wkt2);
			if (g2.overlaps(g1))
				retval = Argument.numericOne;
			else
				retval = Argument.numericZero;
		}
		catch (ParseException e)
		{
			throw new MapyrusException(e.getClass().getName() + ": " + e.getMessage());
		}
		catch (IllegalArgumentException e)
		{
			throw new MapyrusException(e.getClass().getName() + ": " + e.getMessage());
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
		return("overlaps");
	}
}
