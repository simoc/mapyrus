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

/**
 * A single entry for a legend, to be stored in a au.id.chenery.mapyrus.LegendEntryList list
 * and used after 
 */
public class LegendEntry
{
	/*
	 * Available types of legend entries.
	 */
	public static final int POINT_ENTRY = 1;
	public static final int LINE_ENTRY = 2;
	public static final int BOX_ENTRY = 3;

	private String mBlockName;
	private Argument[] mBlockArgs;
	private int mType;
	private String mDescription;

	/**
	 * Create new legend entry.
	 * @param blockName procedure block name for legend entry.
	 * @param blockArgs arguments to procedure block for legend entry.
	 * @param type legend type, either POINT_ENTRY, LINE_ENTRY or BOX_ENTRY. 
	 * @param description label for legend entry.
	 */
	public LegendEntry(String blockName, Argument []blockArgs, int type, String description)
	{
		mBlockName = blockName;
		mBlockArgs = blockArgs;
		mType = type;
		mDescription = description;
	}
	
	/**
	 * Return procedure block name for a legend entry.
	 * @return block name.
	 */
	public String getBlockName()
	{
		return(mBlockName);
	}

	/**
	 * Return arguments for procedure block for a legend entry.
	 * @return block arguments.
	 */
	public Argument []getBlockArgs()
	{
		return(mBlockArgs);
	}

	/**
	 * Return type of legend entry.
	 * @return type, either POINT_ENTRY, LINE_ENTRY, or BOX_ENTRY.
	 */
	public int getType()
	{
		return(mType);
	}

	/**
	 * Return description for a legend entry.
	 * @return description.
	 */
	public String getDescription()
	{
		return(mDescription);
	}
	
	/**
	 * Convert object to string.
	 * @return string representation.
	 */
	public String toString()
	{
		return(mBlockName + " " + mDescription);
	}
}
