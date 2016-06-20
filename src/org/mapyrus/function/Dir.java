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

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.io.WildcardFile;

/**
 * Function returning list of files matching a pattern.
 * For example, dir("/etc/p*") returns an array containing /etc/passwd,
 * /etc/pam.conf, etc.
 */
public class Dir implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		String pattern = arg1.toString();
		Argument retval = new Argument();

		try
		{
			WildcardFile wildcard = new WildcardFile(pattern);
			int counter = 1;
	
			/*
			 * Add each matching file to the array.
			 */
			for (String next : wildcard.getMatchingFiles())
			{
				retval.addHashMapEntry(Integer.toString(counter),
					new Argument(Argument.STRING, next));
				counter++;
			}
		}
		catch (SecurityException e)
		{
			throw new MapyrusException(e.getClass().getName() + ": " + e.getMessage());
		}

		return(retval);
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(1);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("dir");
	}
}
