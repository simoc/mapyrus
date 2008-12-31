/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Function returning a convex hull around a geometry.
 * For example, the convex hull around a group of points is a polygon
 * containing all points. 
 */
public class ConvexHull implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		Argument retval;

		try
		{
			/*
			 * Calculate convex hull for geometry.
			 */
			Argument arg1 = (Argument)args.get(0);
			String wkt1 = arg1.toString();
			Geometry g1 = new WKTReader().read(wkt1);
			com.vividsolutions.jts.algorithm.ConvexHull hull = new
				com.vividsolutions.jts.algorithm.ConvexHull(g1);
			retval = new Argument(hull.getConvexHull().toText());
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
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("convexhull");
	}
}
