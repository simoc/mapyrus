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

package org.mapyrus.function;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function splitting string into array, delimited by a regular expression.
 * For example, split("foo:bar", ":") = [1] -> "foo", [2] -> "bar".
 */
public class Split implements Function
{
	/*
	 * Pre-defined hashmap keys for split function.
	 */
	private static String m_splitIndexes[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};

	private static Argument WHITESPACE_PATTERN = new Argument(Argument.STRING, " ");

	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		Argument arg2;
		if (args.size() == 2)
			arg2 = args.get(1);
		else
			arg2 = WHITESPACE_PATTERN;

		/*
		 * Split string on regular expression and assign as hashmap entries
		 * with keys, "1", "2", "3", ...
		 */
		Argument retval = new Argument();
		String []split;
		String key;
		String delimiter = arg2.getStringValue();
		if (delimiter.equals(WHITESPACE_PATTERN.toString()))
		{
			/*
			 * Simple split on whitespace.
			 */
			StringTokenizer st = new StringTokenizer(arg1.toString());
			split = new String[st.countTokens()];
			for (int i = 0; i < split.length; i++)
				split[i] = st.nextToken();
		}
		else
		{
			/*
			 * Split using regular expression.
			 */
			split = arg1.toString().split(delimiter);
		}

		for (int i = 0; i < split.length; i++)
		{
			/*
			 * Use pre-allocated strings to reduce object creation.
			 */
			if (i < m_splitIndexes.length)
				key = m_splitIndexes[i];
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
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("split");
	}
}
