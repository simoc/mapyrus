/*
 * $Id$
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
