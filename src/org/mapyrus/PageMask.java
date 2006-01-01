/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005, 2006 Simon Chenery.
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

import java.util.Arrays;

/**
 * Manages a bitmap mask for a page.  Rectangular regions of the page can
 * be marked as "0" or "1".  rectangular regions can then be queried to determine
 * whether they contain all 0's, 1's or a mixture.
 */
public class PageMask
{
	/*
	 * The mask as a 0/1 array (corresponding to values false/true)
	 * and its width and height.
	 */
	private boolean []mMask;
	private int mMaskWidth;
	private int mMaskHeight;

	/**
	 * Create new mask, with all values initially zero.
	 * @param maskWidth width of mask in pixels.
	 * @param maskHeight height of mask in pixels.
	 */
	public PageMask(int maskWidth, int maskHeight)
	{
		mMask = new boolean[maskWidth * maskHeight];
		Arrays.fill(mMask, false);

		mMaskWidth = maskWidth;
		mMaskHeight = maskHeight;
	}

	/**
	 * Set all values inside rectangular area in mask to 0 or to 1. 
	 * @param x1 X coordinate of one corner of area.
	 * @param y1 Y coordinate of one corner of area.
	 * @param x2 X coordinate of opposite corner of area.
	 * @param y2 Y coordinate of opposite corner of area.
	 * @param value 0 or 1 value to set.
	 */
	public void setValue(int x1, int y1, int x2, int y2, int value)
	{
		int xMin = Math.min(x1, x2);
		int xMax = Math.max(x1, x2);
		int yMin = Math.min(y1, y2);
		int yMax = Math.max(y1, y2);
		
		/*
		 * Avoid running off edge of mask array.
		 */
		if (xMin < 0)
			xMin = 0;
		if (yMin < 0)
			xMax = 0;
		if (xMax >= mMaskWidth)
			xMax = mMaskWidth - 1;
		if (yMax >= mMaskHeight)
			yMax = mMaskHeight - 1;

		boolean booleanValue = (value != 0);
		for (int y = yMin; y <= yMax; y++)
		{
			for (int x = xMin; x <= xMax; x++)
				mMask[y * mMaskWidth + x] = booleanValue;
		}
	}

	/**
	 * Find whether all values inside rectangulare area are zero.
	 * @param x1 X coordinate of one corner of area.
	 * @param y1 Y coordinate of one corner of area.
	 * @param x2 X coordinate of opposite corner of area.
	 * @param y2 Y coordinate of opposite corner of area.
	 * @return true if all values are zero.
	 */
	public boolean isAllZero(int x1, int y1, int x2, int y2)
	{
		int xMin = Math.min(x1, x2);
		int xMax = Math.max(x1, x2);
		int yMin = Math.min(y1, y2);
		int yMax = Math.max(y1, y2);

		/*
		 * Interpret values outside mask area as having value 1.
		 * Therefore if any part of the rectangle falls outside the
		 * mask then the test for all zeroes fails.
		 */
		boolean foundNonZero = (xMin < 0 || yMin < 0 || xMax >= mMaskWidth || yMax >= mMaskHeight);

		int y = yMin;
		while ((!foundNonZero) && y <= yMax)
		{
			int x = xMin;
			int index = y * mMaskWidth + x;
			while ((!foundNonZero) && x <= xMax)
			{
				foundNonZero = mMask[index];
				index++;
				x++;
			}
			y++;
		}
		return(!foundNonZero);
	}
}
