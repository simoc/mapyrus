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

/**
 * Function transforming page coordinates to world coordinates,
 * the inverse of the topage function.
 */
public class Toworlds implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;
		Argument arg1 = args.get(0);
		if (args.size() == 1)
		{
			retval = context.transformToWorlds(arg1);
		}
		else
		{
			Argument arg2 = args.get(1);

			/*
		 	 * Build point geometry and transform it.
		 	 */
			double geometry[] = new double[5];
			geometry[0] = Argument.GEOMETRY_POINT;
			geometry[1] = 1;
			geometry[2] = Argument.MOVETO;
			geometry[3] = arg1.getNumericValue();
			geometry[4] = arg2.getNumericValue();
			Argument arg = new Argument(Argument.GEOMETRY_POINT, geometry);
			retval = context.transformToWorlds(arg);
		}
		return(retval);
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(2);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("toworlds");
	}
}
