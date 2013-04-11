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

/*
 * @(#) $Id$
 */

package org.mapyrus;

/**
 * Provides default field names for datasets that do not provide their own.
 * Uses awk(1) standard of $0, $1, $2, $3, ...
 */
public class DefaultFieldNames
{
	/*
	 * Static list of fieldnames saves allocating them each time they
	 * are requested.
	 */
	private static String mDefaultNames[] = {"$0", "$1", "$2", "$3",
		"$4", "$5", "$6", "$7", "$8", "$9", "$10", "$11", "$12",
		"$13", "$14", "$15", "$16", "$17", "$18", "$19"};

	/**
	 * Returns default name for i'th field in a dataset.
	 * @param i index of field, 0 based.
	 * @return a default name for this field.
	 */
	public static String get(int i)
	{
		if (i < mDefaultNames.length)
			return(mDefaultNames[i]);
		else
			return("$" + i);
	}
}
