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
import org.mapyrus.MapyrusMessages;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Function returning a zero or one value depending on whether
 * one geometry contains another. 
 */
public class Contains extends Function
{
	/**
	 * Calculates whether one geometry contains another.
	 * @param wkt1 OGC WKT string for first geometry.
	 * @param wkt2 OGC WKT string for second geometry.
	 * @return argument with non-zero value if wkt1 contains wkt2, zero value otherwise.
	 * @throws MapyrusException
	 */
	private Argument isContaining(String wkt1, String wkt2) throws MapyrusException
	{
		Argument retval;
		try
		{
			Geometry g1 = new WKTReader().read(wkt1);
			Geometry g2 = new WKTReader().read(wkt2);
			if (g1.contains(g2))
				retval = Argument.numericOne;
			else
				retval = Argument.numericZero;
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
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		if (arg1.getType() != Argument.GEOMETRY)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOMETRY) +
				": " + arg1.toString());

		if (arg2.getType() != Argument.GEOMETRY)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOMETRY) +
				": " + arg2.toString());

		String wkt1 = arg1.toString();
		String wkt2 = arg2.toString();
		return(isContaining(wkt1, wkt2));
	}

	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2, Argument arg3)
		throws MapyrusException
	{
		if (arg1.getType() != Argument.GEOMETRY)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOMETRY) +
				": " + arg1.toString());

		String wkt1 = arg1.toString();
		String wkt2 = "POINT (" + arg2.getNumericValue() + " " + arg3.getNumericValue() + ")";
		return(isContaining(wkt1, wkt2));
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(3);
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
		return("contains");
	}
}
