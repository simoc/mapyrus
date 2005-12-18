/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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

/**
 * Function transforming page 
 */
public class Toworlds extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1)
		throws MapyrusException
	{
		Argument retval = context.transformToWorlds(arg1);
		return(retval);
	}
	
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
	throws MapyrusException
	{
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
		Argument retval = context.transformToWorlds(arg);
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
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("toworlds");
	}
}
