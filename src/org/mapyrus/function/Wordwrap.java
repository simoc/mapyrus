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

import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function breaking string into several lines at word boundaries to
 * avoid string exceeding a given line length.
 * For example, wordwrap('the quick brown fox', 20) = 'the quick\nbrown fox'.
 */
public class Wordwrap extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1, Argument arg2)
		throws MapyrusException
	{
		String s = arg1.getStringValue();
		double maxWidth = arg2.getNumericValue();

		StringTokenizer st = new StringTokenizer(s);
		String token;
		double lineWidth = 0;
		double wordWidth;
		StringBuffer sb = new StringBuffer(s.length() + 5);

		/*
		 * Break string into lines, not exceeding maxWidth in length.
		 */
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (lineWidth > 0)
				token = " " + token;

			/*
			 * Calculate width of next word.  If word is too long to
			 * add to current line then start a new line, then add word.
			 */
			wordWidth = context.getStringWidth(token);
			if (lineWidth > 0 && lineWidth + wordWidth > maxWidth)
			{
				sb.append(Constants.LINE_SEPARATOR);
				token = token.trim();
				lineWidth = 0;
			}

			/*
			 * If this word alone is too long for a line, then split it into
			 * a hyphenated word (like "compli-cations") over two lines.
			 */
			while (lineWidth + wordWidth > maxWidth)
			{
				int tokenLength = token.length();
				int i = 1;
				String lastPartToken = null;
				String partToken = "";
				double lastWordWidth = 0.0;
				wordWidth = 0;

				while (i < tokenLength && lineWidth + wordWidth <= maxWidth)
				{
					lastPartToken = partToken;
					partToken = token.substring(0, i);
					lastWordWidth = wordWidth;
					wordWidth = context.getStringWidth(partToken + "-");
					i++;
				}

				sb.append(lastPartToken);
				sb.append("-");
				sb.append(Constants.LINE_SEPARATOR);
				token = token.substring(lastPartToken.length());
				wordWidth = context.getStringWidth(token);
			}

			sb.append(token);
			lineWidth += wordWidth;
		}

		Argument retval = new Argument(Argument.STRING, sb.toString());
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
		return("wordwrap");
	}
}
