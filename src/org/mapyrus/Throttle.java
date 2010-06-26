/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
	private long m_startTime;

	/*
	 * Number of milliseconds to run in each second.
	 */
	private long m_millisToUse = 1000;

	/*
	 * Total number of milliseconds that thread can run for.
	 */
	private long m_timeLimit = -1;

	public Throttle()
	{
		m_startTime = System.currentTimeMillis();
	}

	/**
	 * Set thread start time to current time again.
	 */
	public void restart()
	{
		m_startTime = System.currentTimeMillis();
	}

	/**
	 * Set maximum percentage of CPU to use.
	 * @param percent maximum percentage to use.
	 */
	public void setMaxLoad(int percent)
	{
		if (percent < 5)
			percent = 5;
		else if (percent > 100)
			percent = 100;
		m_millisToUse = percent * 10;
	}

	/**
	 * Set maximum time that thread may run for.
	 * @param millis number of milliseconds thread may run for.
	 */
	public void setMaxTime(long millis)
	{
		m_timeLimit = millis;
	}

	/**
	 * Get maximum time that a thread may run for.
	 * @return maximum time in milliseconds, or -1 if no limit.
	 */
	public long getMaxTime()
	{
		return(m_timeLimit);
	}

	/**
	 * Sleep to limit CPU usage.
	 * Should be called several times a second so sleeping
	 * can be done accurately.
	 */
	public void sleep() throws MapyrusException
	{
		if (m_millisToUse != 1000 || m_timeLimit > 0)
		{
			long now = System.currentTimeMillis();
			long elapsed = now - m_startTime;
			long millis = (elapsed % 1000);
			if (m_timeLimit > 0 && elapsed > m_timeLimit)
			{
				/*
				 * Thread has run for too long.  Interrupt it.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INTERRUPTED));
			}
			else if (millis > m_millisToUse)
			{
				try
				{
					Thread.sleep(1000 - m_millisToUse);
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

	/**
	 * Make a clone of this throttle.
	 * @return clone of this throttle.
	 */
	public Throttle clone()
	{
		Throttle retval = new Throttle();
		retval.m_timeLimit = m_timeLimit;
		retval.m_millisToUse = m_millisToUse;
		return(retval);
	}
}
