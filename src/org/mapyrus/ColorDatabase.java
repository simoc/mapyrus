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
 
import java.util.HashMap;
import java.awt.Color;
import java.lang.SecurityException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.lang.NumberFormatException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Color names and their RGB color components.
 * A name to RGB value lookup table using colors read from a UNIX-style rgb.txt file.
 * Some basic colors are defined if no file can be found to read.
 */
public class ColorDatabase
{
	static private HashMap mColors;
	
	/**
	 * Load global color name database from a file.
	 */
	public static void load() throws MapyrusException, IOException
	{
		String filename, line;
		StringTokenizer st;
		LineNumberReader reader = null;
		
		mColors = new HashMap();

		/*
		 * If user gave name of file as property then use that.
		 */
		try
		{
			filename = System.getProperty(Mapyrus.PROGRAM_NAME + ".rgbfile");
		}
		catch (SecurityException e)
		{
			filename = null;
		}
		
		try
		{
			/*
			 * Look for a rgb.txt file in current directory if no
			 * filename given.
			 */
			if (filename == null)
				filename = "rgb.txt";		
			reader = new LineNumberReader(new FileReader(filename));
		}
		catch (FileNotFoundException e)
		{
			filename = null;
		}
		
		try
		{
			/*
			 * Otherwise try to read X Windows file /usr/lib/X11/rgb.txt.
			 */
			if (filename == null)
			{
				filename = "/usr/lib/X11/rgb.txt";
				reader = new LineNumberReader(new FileReader(filename));
			}
		}
		catch (FileNotFoundException e)
		{
			/*
			 * No color name database available.  Just define 
			 * named colors available in Java.
			 */
			mColors.put("black", Color.BLACK);
			mColors.put("blue", Color.BLUE);
			mColors.put("cyan", Color.CYAN);
			mColors.put("darkgray", Color.DARK_GRAY);
			mColors.put("gray", Color.GRAY);
			mColors.put("green", Color.GREEN);
			mColors.put("lightgray", Color.LIGHT_GRAY);
			mColors.put("magenta", Color.MAGENTA);
			mColors.put("orange", Color.ORANGE);
			mColors.put("pink", Color.PINK);
			mColors.put("red", Color.RED);
			mColors.put("white", Color.WHITE);
			mColors.put("yellow", Color.YELLOW);
			return;
		}

		while ((line = reader.readLine()) != null)
		{
			/*
			 * Parse RGB values and color name from each line.
			 */
			st = new StringTokenizer(line);
			if (st.countTokens() >= 4)
			{
				String red = st.nextToken();
				String green = st.nextToken();
				String blue = st.nextToken();
				
				/*
				 * Name may be a single word or multiple words.
				 * Both "green" and "dark green" are accepted.
				 */
				String name = st.nextToken();
				while (st.hasMoreTokens())
				{
					name = name.concat(st.nextToken());
				}
				
				/*
				 * Skip lines that begin with comment character.
				 */
				if (!red.startsWith("!"))
				{
					try
					{
						int r = Integer.parseInt(red);
						int g = Integer.parseInt(green);
						int b = Integer.parseInt(blue);
						mColors.put(name.toLowerCase(), new Color(r, g, b));
					}
					catch (NumberFormatException e)
					{
						throw new MapyrusException(filename + ":" +
							reader.getLineNumber() + ": " +
							MapyrusMessages.get(MapyrusMessages.INVALID_COLOR));
					}
				}
			}
		}
	}

	/**
	 * Add new color to color database.  Synchronized to protect against two
	 * threads modifying database at the same time.
	 * @param colorName name of color to add.
	 * @param color color to add to database.
	 */
	private static synchronized void putColor(String colorName, Color color)
	{
		mColors.put(colorName, color);
	}

	/**
	 * Return color structure from named color.
	 * @param colorName is color to lookup.
	 * @return color definition, or null if color not known.
	 */	
	public static Color getColor(String colorName)
	{
		Color retval;

		retval = (Color)(mColors.get(colorName));
		if (retval == null)
		{
			/*
			 * Convert color name to lower case and
			 * strip whitespace, then look it up again.
			 */
			int nChars = colorName.length();
			char c;
			StringBuffer sb = new StringBuffer(nChars);
			
			for (int i = 0; i < nChars; i++)
			{
				c = colorName.charAt(i);
				if (!Character.isWhitespace(c))
				{
					sb.append(Character.toLowerCase(c));
				}
			}
			retval = (Color)mColors.get(sb.toString());
			if (retval != null)
			{
				/*
				 * Add this variation of color name to database so it can
				 * be looked up directly next time.
				 */
				putColor(colorName, retval);
			}
		}
		return(retval);
	}
}
