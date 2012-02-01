/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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

import java.awt.Font;

/**
 * Least recently used cache of java.awt.Font objects.
 */
public class FontCache extends LRUCache<String, Font>
{
	private static final long serialVersionUID = 0x4a510003;

	/*
	 * Number of fonts to cache in memory.
	 */
	private static final int FONT_CACHE_SIZE = 10;

	/**
	 * Allocate new font cache with space for 10 fonts.
	 */
	public FontCache()
	{
		super(FONT_CACHE_SIZE);
	}

	/**
	 * Hashes font name, style, size and rotation angle into a string.
	 * @param name is name of font as defined in java.awt.Font class.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param size is size for labelling.
	 * @param rotation is rotation angle for text in radians.
	 * @return desired font, or a default if font could not be found.
	 */
	private String hash(String name, int style, float pointSize, double rotation)
	{
		String retval = name + "," + style + "," + Math.round(pointSize * 1000) + "," +
			Math.round(rotation * 1000.0);
		return(retval);
	}

	/**
	 * Gets font with given name, style and size from cache.
	 * @param name is name of font as defined in java.awt.Font class.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param size is size for labelling in whatever units caller wants
	 * (as long as caller is consistent).
	 * @param rotation is rotation angle for text in radians.
	 * @return font from cache, or null if font is not in cache.
	 */
	public Font get(String name, int style, float size, double rotation)
	{
		String key = hash(name, style, size, rotation);
		Font retval = get(key);
		return(retval);
	}

	/**
	 * Stores font with given name, style and size in the cache.
	 * If the cache is full then a font that was not recently used
	 * is removed from the cache.
	 * @param name is name of font as defined in java.awt.Font class.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param pointSize is size for labelling in whatever units caller wants
	 * (as long as caller is consistent).
	 * @param rotation is rotation angle for text in radians.
	 * @param font is font store in cache.
	 */	
	public void put(String name, int style, float size, double rotation, Font font)
	{
		String key = hash(name, style, size, rotation);
		put(key, font);
	}
}
