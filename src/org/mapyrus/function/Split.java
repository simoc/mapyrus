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
 * Function splitting string into array, delimited by a regular expression.
 * For example, split("foo:bar", ":") = [1] -> "foo", [2] -> "bar".
 */
public class Split extends Function
{
	/*
	 * Pre-defined hashmap keys for split function.
	 */
	private static String mSplitIndexes[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};

	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		/*
		 * Split string on regular expression and assign as hashmap entries
		 * with keys, "1", "2", "3", ...
		 */
		String []split = arg1.toString().split(arg2.toString());
		String key;
		Argument retval = new Argument();
		for (int i = 0; i < split.length; i++)
		{
			/*
			 * Use pre-allocated strings to reduce object creation.
			 */
			if (i < mSplitIndexes.length)
				key = mSplitIndexes[i];
			else
				key = String.valueOf(i + 1);
			retval.addHashMapEntry(key, new Argument(Argument.STRING, split[i]));
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
		return("split");
	}
}
