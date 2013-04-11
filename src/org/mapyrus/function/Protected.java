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

/*
 * @(#) $Id$
 */
package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function returning 1 if an area of page is protected.
 * This is most useful for determining if displaying a label or symbol
 * will overwrite an existing label or symbol.
 * For example, protected(12.1, 13.7, 13.33, 14.1) = 1.
 */
public class Protected implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;
		if (args.isEmpty())
		{
			retval = context.isProtected() ? Argument.numericOne : Argument.numericZero;
		}
		else if (args.size() == 1)
		{
			Argument arg1 = args.get(0);
			retval = context.isProtected(arg1) ? Argument.numericOne : Argument.numericZero;
		}
		else
		{			
			if (args.size() != 4)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES));

			Argument arg1 = args.get(0);
			Argument arg2 = args.get(1);
			Argument arg3 = args.get(2);
			Argument arg4 = args.get(3);

			double x1 = arg1.getNumericValue();
			double y1 = arg2.getNumericValue();
			double x2 = arg3.getNumericValue();
			double y2 = arg4.getNumericValue();

			double xMin = Math.min(x1, x2);
			double yMin = Math.min(y1, y2);
			double xMax = Math.max(x1, x2);
			double yMax = Math.max(y1, y2);

			if (context.isProtected(xMin, yMin, xMax, yMax))
				retval = Argument.numericOne;
			else
				retval = Argument.numericZero;
		}
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(4);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(0);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("protected");
	}
}
