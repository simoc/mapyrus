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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function replacing all instances of a regular expression in a string with another string.
 * For example, replace('foobar', 'o+', '_') = 'f_bar'.
 */
public class Replace implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;
		Argument arg1 = args.get(0);
		Argument arg2 = args.get(1);
		Argument arg3 = args.get(2);

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
		return("replace");
	}
}
