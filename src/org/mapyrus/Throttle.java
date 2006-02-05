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

/**
 * Limits CPU usage by occasionally sleeping when called.
 */
public class Throttle
{
	static private long mStartTime;
	static
	{
		mStartTime = System.currentTimeMillis();
	};

	/*
	 * Number of milliseconds to run in each second.
	 */
	static private long mMillisToUse = 1000;

	/**
	 * Set maximum percentage of CPU to use.
	 * @param percent maximum percentage to use.
	 */
	public static void setMaxLoad(int percent)
	{
		if (percent < 5)
			percent = 5;
		else if (percent > 100)
			percent = 100;
		mMillisToUse = percent * 10;
	}

	/**
	 * Sleep to limit CPU usage.
	 * Should be called several times a second so sleeping
	 * can be done accurately.
	 */
	public static void sleep() throws MapyrusException
	{
		if (mMillisToUse != 1000)
		{
			long now = System.currentTimeMillis();
			long elapsed = now - mStartTime;
			long millis = (elapsed % 1000);
			if (millis > mMillisToUse)
			{
				try
				{
					Thread.sleep(1000 - mMillisToUse);
				}
				catch (InterruptedException e)
				{
					/*
					 * Stop if interrupted.
					 */
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INTERRUPTED));
				}
			}
		}
	}
}

