/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004 Simon Chenery.
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

import java.util.Iterator;
import java.util.List;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.io.WildcardFile;

/**
 * Function returning list of files matching a pattern.
 * For example, listfiles("/etc/p*") returns an array containing /etc/passwd,
 * /etc/pam.conf, etc.
 */
public class Listfiles extends Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, org.mapyrus.Argument)
	 */
	public Argument evaluate(ContextStack context, Argument arg1)
		throws MapyrusException
	{
		String pattern = arg1.toString();
		Argument retval = new Argument();

		WildcardFile wildcard = new WildcardFile(pattern);
		List list = wildcard.getMatchingFiles();
		Iterator it = list.iterator();
		int counter = 1;

		/*
		 * Add each matching file to the array.
		 */
		while (it.hasNext())
		{
			String next = (String)it.next();
			retval.addHashMapEntry(Integer.toString(counter),
				new Argument(Argument.STRING, next));
			counter++;
		}

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
		return("listfiles");
	}
}
