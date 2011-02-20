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
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.font.StringDimension;

/**
 * Function breaking string into several lines at word boundaries to
 * avoid string exceeding a given line length.
 * For example, wordwrap('the quick brown fox', 20) = 'the quick\nbrown fox'.
 */
public class Wordwrap implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		Argument arg2 = args.get(1);
		String s = arg1.getStringValue();
		double maxWidth = arg2.getNumericValue();
		String hyphenation = null;
		boolean adjustSpacing = false;
		if (args.size() == 3)
		{
			StringTokenizer st = new StringTokenizer(args.get(2).toString());
			while (st.hasMoreTokens())
			{
				String token = st.nextToken();
				if (token.startsWith("hyphenation="))
				{
					hyphenation = token.substring(12);
				}
				else if (token.startsWith("adjustspacing="))
				{
					String flag = token.substring(14);
					adjustSpacing = flag.equalsIgnoreCase("true");
				}
			} 
		}

		StringTokenizer st = new StringTokenizer(s);
		String token;
		StringDimension dim;
		double lineWidth = 0;
		double wordWidth;
		StringBuffer sb = new StringBuffer(s.length() + 5);
		int lineStartIndex = 0;
		double spaceWidth = context.getStringDimension(" ").getWidth();

		/*
		 * Break string into lines, not exceeding maxWidth in length.
		 */
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (lineWidth > 0)
				token = " " + token;

			/*
			 * Split word on hyphenation characters too.
			 */
			LinkedList<String> splitList;
			if (hyphenation != null)
			{
				splitList = split(token, hyphenation);
			}
			else
			{
				splitList = new LinkedList<String>();
				splitList.add(token);
			}

			while (!splitList.isEmpty())
			{
				int nElementsToAdd = splitList.size();
				while (nElementsToAdd > 0 && !splitList.isEmpty())
				{
					/*
					 * Progressively try adding less and less of word until
					 * it will fit on line.
					 */
					StringBuffer joined = new StringBuffer(token.length());
					for (int i = 0; i < nElementsToAdd; i++)
						joined.append(splitList.get(i));
					if (nElementsToAdd <  splitList.size())
						joined.append('-');

					dim = context.getStringDimension(joined.toString());
					wordWidth = dim.getWidth();
					if (lineWidth + wordWidth <= maxWidth)
					{
						sb.append(joined);
						lineWidth += wordWidth;
						for (int i = 0; i < nElementsToAdd; i++)
							splitList.removeFirst();
						nElementsToAdd = splitList.size();
					}
					else
					{
						/*
						 * Try adding a bit less of word to see if that fits.
						 */
						nElementsToAdd--;
					}
				}

				/*
				 * If we were not able to add anything then add at least
				 * a single letter, regardless of whether it fits or not.
				 */
				if (nElementsToAdd == 0 && lineWidth == 0)
				{
					String firstElement = (String)splitList.removeFirst();
					int nChars = firstElement.length();
					while (nChars > 0)
					{
						String sub = firstElement.substring(0, nChars) + "-";
						dim = context.getStringDimension(sub);
						wordWidth = dim.getWidth();
						if (wordWidth <= maxWidth || nChars == 1)
						{
							sb.append(sub);
							lineWidth += wordWidth;

							/*
							 * Add remaining letters back to list so as
							 * they still have to be added.
							 */
							splitList.addFirst(firstElement.substring(nChars));
							break;
						}
						nChars--;
					}
				}

				/*
				 * Add a newline, then continue adding rest of word.
				 */
				if (!splitList.isEmpty())
				{
					if (adjustSpacing)
					{
						/*
						 * Add more spaces between words to make each
						 * line as close as possible to the same length.
						 */
						int spaceIndex = lineStartIndex;
						boolean addedSpace = false;
						while (lineWidth + spaceWidth <= maxWidth)
						{
							spaceIndex = sb.indexOf(" ", spaceIndex);
							if (spaceIndex >= 0)
							{
								/*
								 * Skip over multiple spaces.
								 */
								while (spaceIndex + 1 < sb.length() &&
									sb.charAt(spaceIndex + 1) == ' ')
								{
									spaceIndex++;
								}
								sb.insert(spaceIndex, ' ');
								spaceIndex += 2;
								lineWidth += spaceWidth;
								addedSpace = true;
							}
							else if (addedSpace)
							{
								/*
								 * Go back to start of line and add
								 * some more spaces.
								 */
								spaceIndex = lineStartIndex;
							}
							else
							{
								/*
								 * No spaces in line so nowhere to add any more spaces.
								 */
								break;
							}
						}
					}
					sb.append(Constants.LINE_SEPARATOR);
					lineStartIndex = sb.length();
					lineWidth = 0;

					/*
					 * Trim any leading space we added before starting new line.
					 */
					String firstElement = (String)splitList.removeFirst();
					splitList.addFirst(firstElement.trim());
				}
			}
		}

		Argument retval = new Argument(Argument.STRING, sb.toString());
		return(retval);
	}

	/**
	 * Split string into list.
	 * @param s string to split.
	 * @param sequence sequence to split on.
	 * @return list of substrings.
	 */
	private LinkedList<String> split(String s, String sequence)
	{
		LinkedList<String> retval = new LinkedList<String>();
		int i = 0;
		int lastI = 0;
		while ((i = s.indexOf(sequence, lastI)) >= 0)
		{
			if (i > lastI)
				retval.add(s.substring(lastI, i));
			lastI = i + sequence.length();
		}
		if (lastI < s.length())
			retval.add(s.substring(lastI));
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
		return("wordwrap");
	}
}
