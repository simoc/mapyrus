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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function replacing all instances of a regular expression in a string with another string.
 * For example, replace('foobar', 'o+', '_') = 'f_bar'.
 */
public class Replace extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack,
	 * 	org.mapyrus.Argument, org.mapyrus.Argument, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2, Argument arg3)
		throws MapyrusException
	{
		Argument retval;

		/*
		 * Replace all occurrences of pattern given in second string
		 * with the third string.
		 */
		Pattern pattern = Match.compileRegex(arg2.toString());
		Matcher matcher = pattern.matcher(arg1.toString());
		if (matcher.find())
		{
			/*
			 * Replace all matching patterns.
			 */
			retval = new Argument(Argument.STRING, matcher.replaceAll(arg3.toString()));
		}
		else
		{
			/*
			 * No match so return original string.
			 */
			retval = arg1;
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
		return("replace");
	}
}