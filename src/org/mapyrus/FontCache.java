/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.awt.Font;

/**
 * Least recently used cache of java.awt.Font objects.
 */
public class FontCache extends LRUCache
{
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
	 * @param pointSize is size for labelling in points (1/72 inch).
	 * @param rotation is rotation angle for text in radians, 0 is horizontal, increasing
	 * counter-clockwise.
	 * @return desired font, or a default if font could not be found. 
	 */
	private String hash(String name, int style, int pointSize, double rotation)
	{
		String retval = name + "," + style + "," + pointSize + "," +
			Math.round(rotation * 1000.0);
		return(retval);
	}

	/**
	 * Gets font with given name, style and size from cache.
	 * @param name is name of font as defined in java.awt.Font class.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param pointSize is size for labelling in points (1/72 inch).
	 * @param rotation is rotation angle for text in radians, 0 is horizontal, increasing
	 * counter-clockwise.
	 * @return font from cache, or null if font is not in cache.
	 */
	public Font get(String name, int style, int pointSize, double rotation)
	{
		String key = hash(name, style, pointSize, rotation);
		Font retval = (Font)get(key);
		return(retval);
	}

	/**
	 * Stores font with given name, style and size in the cache.
	 * If the cache is full then a font that was not recently used
	 * is removed from the cache.
	 * @param name is name of font as defined in java.awt.Font class.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param pointSize is size for labelling in points (1/72 inch).
	 * @param rotation is rotation angle for text in radians, 0 is horizontal, increasing
	 * counter-clockwise.
	 * @param font is font store in cache.
	 */	
	public void put(String name, int style, int pointSize, double rotation, Font font)
	{
		String key = hash(name, style, pointSize, rotation);
		put(key, font);
	}
}
