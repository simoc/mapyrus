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

/**
 * A single entry for a legend, to be stored in a
 * org.mapyrus.LegendEntryList list and used after map display
 * for generating a legend.
 */
public class LegendEntry implements Comparable
{
	/*
	 * Available types of legend entries.
	 */
	public static final int POINT_ENTRY = 1;
	public static final int LINE_ENTRY = 2;
	public static final int ZIGZAG_ENTRY = 3;
	public static final int BOX_ENTRY = 4;

	private String mBlockName;
	private Argument[] mBlockArgs;
	private int mType;
	private String mDescription;

	/**
	 * Create new legend entry.
	 * @param blockName procedure block name for legend entry.
	 * @param blockArgs arguments to procedure block for legend entry.
	 * @param type legend type, either POINT_ENTRY, LINE_ENTRY, ZIGZAG_ENTRY or BOX_ENTRY.
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
	 * Parse legend entry type.
	 * @param typeString legend entry string.
	 * @return legend type, or -1 if legend entry type not valid.
	 */
	public static int parseTypeString(String typeString)
	{
		int retval = -1;

		if (typeString.equalsIgnoreCase("point"))
			retval = LegendEntry.POINT_ENTRY;
		else if (typeString.equalsIgnoreCase("line"))
			retval = LegendEntry.LINE_ENTRY;
		else if (typeString.equalsIgnoreCase("zigzag"))
			retval = LegendEntry.ZIGZAG_ENTRY;
		else if (typeString.equalsIgnoreCase("box"))
			retval = LegendEntry.BOX_ENTRY;
		return(retval);
	}

	/**
	 * Compare legend entry with another.
	 * @param o legend entry to compare with.
	 * @return -1, 0, 1, depending whether this object is less, equal, or greater than o.
	 */
	public int compareTo(Object o)
	{
		LegendEntry entry = (LegendEntry)o;
		
		/*
		 * Sort by legend type, then by description.
		 */
		int retval = this.mType - entry.mType;
		if (retval == 0)
			retval = this.mDescription.compareTo(entry.mDescription);
		return(retval);
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
	 * Return type of legend entry as a string.
	 * @return legend type.
	 */
	public String getTypeString()
	{
		String retval;
		if (mType == POINT_ENTRY)
			retval = "point";
		else if (mType == LINE_ENTRY)
			retval = "line";
		else if (mType == ZIGZAG_ENTRY)
			retval = "zigzag";
		else
			retval = "box";

		return(retval);
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
