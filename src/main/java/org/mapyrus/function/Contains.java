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
 * one geometry contains another. 
 */
public class Contains implements Function
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
			throw new MapyrusException(e.getClass().getName() + ": " + e.getMessage());
		}
		catch (IllegalArgumentException e)
		{
			throw new MapyrusException(e.getClass().getName() + ": " + e.getMessage());
		}
		return(retval);
	}

	private Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		/*
		 * First geometry cannot contain second if it's bounding
		 * box does not contain bounding box of second geometry.
		 */
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
		return(isContaining(wkt1, wkt2));
	}

	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		Argument arg2 = args.get(1);
		if (args.size() == 2)
			return(evaluate(context, arg1, arg2));

		Argument arg3 = args.get(2);
		double x = arg2.getNumericValue();
		double y = arg3.getNumericValue();

		/*
		 * Geometry cannot possibly contain point if it's
		 * bounding box does not contain the point.
		 */
		Rectangle2D.Double rect = arg1.getGeometryBoundingBox();
		if (rect == null || (!rect.contains(x, y)))
			return(Argument.numericZero);

		String wkt1 = arg1.toString();
		String wkt2 = "POINT (" + x + " " + y + ")";
		return(isContaining(wkt1, wkt2));
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(3);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(2);
	}

	@Override
	public String getName()
	{
		return("contains");
	}
}
