/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Function returning union of two geometries.
 * For example, the union of two overlapping polygons is a single large polygon. 
 */
public class Union extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		Argument retval;

		String wkt1 = arg1.toString();
		String wkt2 = arg2.toString();

		/*
		 * If one of the geometries is nothing, then just return the other
		 * value.
		 */
		if (wkt1.length() == 0)
		{
			retval = arg2;
		}
		else if (wkt2.length() == 0)
		{
			retval = arg1;
		}
		else
		{
			try
			{
				Geometry g1 = new WKTReader().read(wkt1);
				Geometry g2 = new WKTReader().read(wkt2);
				Geometry union = g1.union(g2);
				retval = new Argument(union.toText());
			}
			catch (ParseException e)
			{
				throw new MapyrusException(e.getMessage());
			}
			catch (IllegalArgumentException e)
			{
				throw new MapyrusException(e.getMessage());
			}
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
		return("union");
	}
}
