/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.util.LinkedList;
import java.util.HashSet;

/**
 * All entries for a legend accumulated during plotting, with duplicates removed.
 * Used for generating a legend when plotting is complete.
 * Contains name of procedure block and parameters, and a
 * legend entry type and description for each legend entry.
 */
public class LegendEntryList
{
	/*
	 * List of entries in legend.  A set is used to
	 * efficiently avoid adding duplicate entries to list.
	 */
	private LinkedList mLegendList;
	private HashSet mLegendSet;

	/*
	 * Flag true when further additions to list are accepted.
	 */
	private boolean mAcceptAdditions;

	/**
	 * Create new legend entry list.
	 */
	public LegendEntryList()
	{
		mLegendSet = new HashSet();
		mLegendList = new LinkedList();
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
			 * Skip legend entries that we've already saved.
			 */
			String hashValue = hash(blockName, description);
			if (!mLegendSet.contains(hashValue))
			{
				Argument args[] = new Argument[nBlockArgs];
				for (int i = 0; i < nBlockArgs; i++)
					args[i] = blockArgs[blockArgIndex + i];
				mLegendList.add(new LegendEntry(blockName, args, type, description));
				mLegendSet.add(hashValue);
			}
		}
	}

	/**
	 * Returns number of legend entries.
	 * @return count.
	 */
	public int size()
	{
		return(mLegendList.size());
	}

	/**
	 * Pop first legend entry from list and return it.
	 * @return next legend entry.
	 */
	public LegendEntry pop()
	{
		LegendEntry retval = (LegendEntry)mLegendList.removeFirst();
		mLegendSet.remove(hash(retval.getBlockName(), retval.getDescription()));
		return(retval);
	}
}
