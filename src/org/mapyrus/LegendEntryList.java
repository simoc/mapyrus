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
package org.mapyrus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * All entries for a legend accumulated during plotting, with duplicates removed.
 * Used for generating a legend when plotting is complete.
 * Contains name of procedure block and parameters, and a
 * legend entry type and description for each legend entry.
 */
public class LegendEntryList
{
	/*
	 * All legend entries.
	 */
	private HashMap mLegendHashMap;

	/*
	 * Flag true when further additions to list are accepted.
	 */
	private boolean mAcceptAdditions;

	/**
	 * Create new legend entry list.
	 */
	public LegendEntryList()
	{
		mLegendHashMap = new HashMap();
		mAcceptAdditions = true;
	}

	/**
	 * Ignore further additions to this legend list.
	 */
	public void ignoreAdditions()
	{
		mAcceptAdditions = false;
	}

	/**
	 * Accept further additions to this legend list.
	 */
	public void acceptAdditions()
	{
		mAcceptAdditions = true;
	}

	/**
	 * Hash legend entry to a string.
	 * @param blockName procedure block.
	 * @param description legend entry description.
	 * @return hash string.
	 */
	private String hash(String blockName, String description)
	{
		return(blockName + " " + description);
	}

	/**
	 * Add new legend entry.
	 * @param blockName procedure block name for this legend entry.
	 * @param blockArgs arguments to procedure block for this legend entry.
	 * @param blockArgIndex start index of arguments in blockArgs.
	 * @param nBlockArgs number of arguments in blockArgs.
	 * @param type legend type (POINT|LINE|BOX)_ENTRY.
	 * @param description description for legend.
	 */
	public void add(String blockName, Argument []blockArgs, int blockArgIndex,
		int nBlockArgs, int type, String description)
	{
		if (mAcceptAdditions)
		{
			/*
			 * If we've already added this legend entry then increment the reference count,
			 * otherwise make a new legend entry.
			 */
			String hashValue = hash(blockName, description);
			LegendEntry entry = (LegendEntry)mLegendHashMap.get(hashValue);
			if (entry != null)
			{
				entry.addReference();
			}
			else
			{
				Argument args[] = new Argument[nBlockArgs];
				for (int i = 0; i < nBlockArgs; i++)
					args[i] = blockArgs[blockArgIndex + i];
				mLegendHashMap.put(hashValue, new LegendEntry(blockName, args, type, description));
			}
		}
	}

	/**
	 * Returns number of legend entries.
	 * @return count.
	 */
	public int size()
	{
		return(mLegendHashMap.size());
	}

	/**
	 * Pop first legend entry from list and return it.
	 * @return next legend entry, or null if list is empty.
	 */
	public LegendEntry pop()
	{
		LegendEntry retval = null;

		/*
		 * Ensure that entries are returned in alphabetical order.
		 */
		if (mLegendHashMap.size() > 0)
		{
			Collection values = mLegendHashMap.values();
			ArrayList list = new ArrayList(values);
			Collections.sort(list);

			retval = (LegendEntry)list.get(0);
			mLegendHashMap.remove(hash(retval.getBlockName(), retval.getDescription()));
		}
		return(retval);
	}

	/**
	 * Return first legend entry in list.
	 * @return first legend entry, or null if list is empty.
	 */
	public LegendEntry first()
	{
		LegendEntry retval = null;

		/*
		 * Ensure that entries are returned in alphabetical order.
		 */
		if (mLegendHashMap.size() > 0)
		{
			Collection values = mLegendHashMap.values();
			ArrayList list = new ArrayList(values);
			Collections.sort(list);
			retval = (LegendEntry)list.get(0);
		}
		return(retval);
	}
}
