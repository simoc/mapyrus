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
import com.vividsolutions.jts.operation.buffer.BufferOp;

/**
 * Function returning geometry containing buffer around existing geometry.
 * For example, buffer("POINT ( 10, 10)", 5, "round") returns geometry containing
 * 5 unit buffer around point (5, 5).
 */
public class Buffer extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * org.mapyrus.Argument, org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2, Argument arg3)
		throws MapyrusException
	{
		Argument retval;

		if (arg1.getType() != Argument.GEOMETRY)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOMETRY) +
				": " + arg1.toString());

		String wkt = arg1.toString();
		double distance = arg2.getNumericValue();

		String cap = arg3.toString();
		int capType = BufferOp.CAP_ROUND;
		if (cap.equalsIgnoreCase("butt"))
			capType = BufferOp.CAP_BUTT;
		else if (cap.equalsIgnoreCase("square"))
			capType = BufferOp.CAP_SQUARE;

		try
		{
			/*
			 * Use JTS function to calculate buffer, then convert it back to a
			 * geometry argument.
			 */
			Geometry g = new WKTReader().read(wkt);
			BufferOp bufOp = new BufferOp(g);
			bufOp.setEndCapStyle(capType);
			Geometry buffer = g.buffer(distance);
			retval = new Argument(buffer.toText());
		}
		catch (ParseException e)
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
		return(3);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(3);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("buffer");
	}
}