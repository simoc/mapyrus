/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
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

/**
 * Function returning minimum of all values.
 * For example, min(1, 3, 5) = 1.
 */
public class Min implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		double minValue = Double.MAX_VALUE;
		Argument minArg = null;
		
		/*
		 * Find minimum of all numbers and arrays of numbers.
		 */
		for (int i = 0; i < args.size(); i++)
		{
			Argument arg = args.get(i);
			if (arg.getType() == Argument.HASHMAP)
			{
				Object []keys = arg.getHashMapKeys();
				for (int j = 0; j < keys.length; j++)
				{
					Argument entry = arg.getHashMapEntry(keys[j].toString());
					double d = entry.getNumericValue();
					if (d < minValue)
					{
						minArg = entry;
						minValue = d;
					}
				}
			}
			else
			{
				double d = arg.getNumericValue();
				if (d < minValue)
				{
					minArg = arg;
					minValue = d;
				}
			}
		}
		return(minArg);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(Integer.MAX_VALUE);
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
		return("min");
	}
}
