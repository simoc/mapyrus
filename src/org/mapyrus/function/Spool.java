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

import java.io.IOException;
import java.io.LineNumberReader;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.ContextStack;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;

/**
 * Function returning contents of a text file.
 * For example, spool("/etc/motd") could return a string containing "Have a lot of fun...". 
 */
public class Spool extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1)
		throws MapyrusException
	{
		String filename = arg1.getStringValue();
		StringBuffer sb = new StringBuffer();
		String nextLine;
		LineNumberReader reader = null;

		try
		{
			/*
			 * Read complete file into memory and return it as a single string.
			 */
			FileOrURL f = new FileOrURL(filename);
			reader = f.getReader();
			while ((nextLine = reader.readLine()) != null)
			{
				sb.append(nextLine);
				sb.append(Constants.LINE_SEPARATOR);
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		finally
		{
			try
			{
				if (reader != null)
					reader.close();
			}
			catch (IOException e)
			{
			}
		}
		Argument retval = new Argument(Argument.STRING, sb.toString());
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(1);
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
		return("spool");
	}
}

