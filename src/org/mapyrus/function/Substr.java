/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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
 * Function returning substring of a string.
 * For example, substr('foobar', 2, 3) = 'oob'
 */
public class Substr implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		int startIndex, extractLen, len;
		Argument retval;

		Argument arg1 = args.get(0);
		Argument arg2 = args.get(1);
		Argument arg3;
		if (args.size() == 3)
		{
			arg3 = args.get(2);
		}
		else
		{
			/*
		 	 * Set extract length long enough for all remaining characters in string.
		 	 */
			String s = arg1.toString();
			arg3 = new Argument(s.length());
		}
		String s = arg1.toString();

		/*
		 * Convert to zero-based indexing used by java.
		 */
		startIndex = (int)(Math.floor(arg2.getNumericValue()));
		startIndex--;
		if (startIndex < 0)
			startIndex = 0;
		extractLen = (int)(Math.floor(arg3.getNumericValue()));

		len = s.length();
		if (extractLen < 1 || startIndex >= len)
		{
			/*
			 * Substring is totally to the left or right of
			 * the string.  So substring is empty.
			 */
			retval = Argument.emptyString;
		}
		else
		{
			if (startIndex + extractLen > len)
				extractLen = len - startIndex;
	
			retval = new Argument(Argument.STRING,
				s.substring(startIndex, startIndex + extractLen));
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
		return(2);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("substr");
	}
}
