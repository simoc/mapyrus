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

/**
 * Manages a pool (or 'bag') of objects of the same type for use in
 * connection pooling.  When an object is requested, one of the objects
 * in the pool is returned, with the request blocking until an object
 * becomes available if the pool is currently empty. 
 */
public class Pool
{
	private LinkedList mList;

	/**
	 * Create new pool.
	 */
	public Pool()
	{
		mList = new LinkedList();
	}

	/**
	 * Add object to pool
	 * @param o object to add to pool.
	 */
	public synchronized void put(Object o)
	{
		/*
		 * Add object to pool and notify anyone who is waiting on this pool for
		 * an object that a new one is available.
		 */
		mList.add(o);
		notifyAll();
	}

	/**
	 * Take an object from the pool.
	 * @param timeout length of time in milliseconds to wait for an
	 * object if pool is empty.
	 * @return one object from the pool, or null if none available
	 * and none become available within timeout.
	 */
	public synchronized Object get(long timeout)
	{
		Object retval;
		while (mList.size() == 0)
		{
			try
			{
				/*
				 * Pool is empty, wait for someone to add an object.
				 */
				wait(timeout);
			}
			catch(InterruptedException e)
			{
			}
		}

		if (mList.size() > 0)
			retval = mList.removeLast();
		else
			retval = null;
		return(retval);
	}
}
