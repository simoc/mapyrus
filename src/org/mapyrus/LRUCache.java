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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements a least recently used (LRU) cache.  A cache has space for
 * a fixed number of key-value pairs, like a java.util.HashMap.  If the
 * cache is full when a new object is added, the object with the oldest
 * access time is automatically removed from the cache.
 */
public class LRUCache extends LinkedHashMap
{
	/*
	 * The maximum number of elements to hold in the cache.
	 */
	private int mMaximumSize;
	
	/**
	 * Create a new least recently used (LRU) cache.
	 * @param size is maximum number of objects to hold in cache.
	 */
	public LRUCache(int size)
	{
		super(Math.min(16, size), 0.75f, true);
		mMaximumSize = size;
	}

	/*
	 * Returns true when eldest object should be removed from cache
	 * to make way for a new object.
	 */
	protected boolean removeEldestEntry(Map.Entry eldest)
	{
		return(size() > mMaximumSize);
	}
}
