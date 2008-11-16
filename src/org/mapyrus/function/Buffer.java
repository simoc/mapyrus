/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2008 Simon Chenery.
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
import com.vividsolutions.jts.operation.buffer.BufferOp;

/**
 * Function returning geometry containing buffer around existing geometry.
 * For example, buffer("POINT ( 10, 10)", 5, "round") returns geometry containing
 * 5 unit buffer around point (5, 5).
 */
public class Buffer implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		Argument retval;

		Argument arg1 = (Argument)args.get(0);
		Argument arg2 = (Argument)args.get(1);
		Argument arg3 = (Argument)args.get(2);
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
			Geometry buffer = bufOp.getResultGeometry(distance);
			retval = new Argument(buffer.toText());
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
