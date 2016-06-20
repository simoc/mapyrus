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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Function returning geometry containing buffer around existing geometry.
 * For example, buffer("POINT ( 10, 10)", 5, "round") returns geometry containing
 * 5 unit buffer around point (5, 5).
 */
public class Buffer implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;

		Argument arg1 = args.get(0);
		Argument arg2 = args.get(1);
		Argument arg3 = args.get(2);
		String wkt = arg1.toString();
		double distance = arg2.getNumericValue();

		String cap = arg3.toString();
		BufferParameters bufferParams = new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_ROUND);
		if (cap.equalsIgnoreCase("butt"))
			bufferParams = new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_FLAT);
		else if (cap.equalsIgnoreCase("square"))
			bufferParams = new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_SQUARE);

		try
		{
			/*
			 * Use JTS function to calculate buffer, then convert it back to a
			 * geometry argument.
			 */
			Geometry g = new WKTReader().read(wkt);
			BufferOp bufOp = new BufferOp(g, bufferParams);
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

	@Override
	public int getMaxArgumentCount()
	{
		return(3);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(3);
	}

	@Override
	public String getName()
	{
		return("buffer");
	}
}
