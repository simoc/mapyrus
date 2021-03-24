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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		Argument arg2;
		boolean includeDelimiters = false;
		if (args.size() >= 2)
		{
			arg2 = args.get(1);
			if (args.size() >= 3)
			{
				StringTokenizer st = new StringTokenizer(args.get(2).toString());
				while (st.hasMoreTokens())
				{
					String token = st.nextToken();
					if (token.startsWith("includedelimiters="))
					{
						String flag = token.substring(18);
						includeDelimiters = flag.equalsIgnoreCase("true");
					}
				}
			}
		}
		else
		{
			arg2 = WHITESPACE_PATTERN;
		}

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
			String str = arg1.toString();
			if (includeDelimiters)
			{
				/*
				 * Caller wants delimiters too, so we have to walk the string adding
				 * each token and delimiter.
				 */
				Pattern p = Pattern.compile(delimiter);
				Matcher m = p.matcher(str);
				int prevIndex = 0;
				ArrayList<String> splitList = new ArrayList<String>();
				while (m.find())
				{
					int startIndex = m.start();
					int endIndex = m.end();
					splitList.add(str.substring(prevIndex, startIndex));
					splitList.add(str.substring(startIndex, endIndex));
					prevIndex = endIndex;
					
				}
				if (prevIndex == 0 || prevIndex < str.length())
					splitList.add(str.substring(prevIndex));
				split = new String[splitList.size()];
				splitList.toArray(split);
			}
			else
			{
				split = str.split(delimiter);
			}
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

	@Override
	public int getMaxArgumentCount()
	{
		return(3);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("split");
	}
}
