/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

import java.util.Hashtable;
import java.awt.Color;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Color names and their RGB color components.
 * A name to RGB value lookup table using colors read from a UNIX-style rgb.txt file.
 * Some basic colors are defined if no file can be found to read.
 */
public class ColorDatabase
{
	static private Hashtable mColors = null;
	
	/**
	 * Load global color name database from a file.
	 */
	public static synchronized void load() throws MapyrusException, IOException
	{
		String filename, line;
		StringTokenizer st;
		LineNumberReader reader = null;

		/*
		 * Only load colors once.
		 */
		if (mColors != null)
			return;

		/*
		 * Define basic set of commonly used colors here (values taken
		 * from color name list in SVG specification).
		 */
		mColors = new Hashtable();
		mColors.put("aliceblue", new Color(240, 248, 255));
		mColors.put("antiquewhite", new Color(250, 235, 215));
		mColors.put("aqua", new Color(0, 255, 255));
		mColors.put("aquamarine", new Color(127, 255, 212));
		mColors.put("azure", new Color(240, 255, 255));
		mColors.put("beige", new Color(245, 245, 220));
		mColors.put("bisque", new Color(255, 228, 196));
		mColors.put("black", new Color(0, 0, 0));
		mColors.put("blanchedalmond", new Color(255, 235, 205));
		mColors.put("blue", new Color(0, 0, 255));
		mColors.put("blueviolet", new Color(138, 43, 226));
		mColors.put("brown", new Color(165, 42, 42));
		mColors.put("burlywood", new Color(222, 184, 135));
		mColors.put("cadetblue", new Color(95, 158, 160));
		mColors.put("chartreuse", new Color(127, 255, 0));
		mColors.put("chocolate", new Color(210, 105, 30));
		mColors.put("coral", new Color(255, 127, 80));
		mColors.put("cornflowerblue", new Color(100, 149, 237));
		mColors.put("cornsilk", new Color(255, 248, 220));
		mColors.put("crimson", new Color(220, 20, 60));
		mColors.put("cyan", new Color(0, 255, 255));
		mColors.put("darkblue", new Color(0, 0, 139));
		mColors.put("darkcyan", new Color(0, 139, 139));
		mColors.put("darkgoldenrod", new Color(184, 134, 11));
		mColors.put("darkgray", new Color(169, 169, 169));
		mColors.put("darkgreen", new Color(0, 100, 0));
		mColors.put("darkgrey", new Color(169, 169, 169));
		mColors.put("darkkhaki", new Color(189, 183, 107));
		mColors.put("darkmagenta", new Color(139, 0, 139));
		mColors.put("darkolivegreen", new Color(85, 107, 47));
		mColors.put("darkorange", new Color(255, 140, 0));
		mColors.put("darkorchid", new Color(153, 50, 204));
		mColors.put("darkred", new Color(139, 0, 0));
		mColors.put("darksalmon", new Color(233, 150, 122));
		mColors.put("darkseagreen", new Color(143, 188, 143));
		mColors.put("darkslateblue", new Color(72, 61, 139));
		mColors.put("darkslategray", new Color(47, 79, 79));
		mColors.put("darkslategrey", new Color(47, 79, 79));
		mColors.put("darkturquoise", new Color(0, 206, 209));
		mColors.put("darkviolet", new Color(148, 0, 211));
		mColors.put("deeppink", new Color(255, 20, 147));
		mColors.put("deepskyblue", new Color(0, 191, 255));
		mColors.put("dimgray", new Color(105, 105, 105));
		mColors.put("dimgrey", new Color(105, 105, 105));
		mColors.put("dodgerblue", new Color(30, 144, 255));
		mColors.put("firebrick", new Color(178, 34, 34));
		mColors.put("floralwhite", new Color(255, 250, 240));
		mColors.put("forestgreen", new Color(34, 139, 34));
		mColors.put("fuchsia", new Color(255, 0, 255));
		mColors.put("gainsboro", new Color(220, 220, 220));
		mColors.put("ghostwhite", new Color(248, 248, 255));
		mColors.put("gold", new Color(255, 215, 0));
		mColors.put("goldenrod", new Color(218, 165, 32));
		mColors.put("gray", new Color(128, 128, 128));
		mColors.put("green", new Color(0, 128, 0));
		mColors.put("greenyellow", new Color(173, 255, 47));
		mColors.put("grey", new Color(128, 128, 128));
		mColors.put("honeydew", new Color(240, 255, 240));
		mColors.put("hotpink", new Color(255, 105, 180));
		mColors.put("indianred", new Color(205, 92, 92));
		mColors.put("indigo", new Color(75, 0, 130));
		mColors.put("ivory", new Color(255, 255, 240));
		mColors.put("khaki", new Color(240, 230, 140));
		mColors.put("lavender", new Color(230, 230, 250));
		mColors.put("lavenderblush", new Color(255, 240, 245));
		mColors.put("lawngreen", new Color(124, 252, 0));
		mColors.put("lemonchiffon", new Color(255, 250, 205));
		mColors.put("lightblue", new Color(173, 216, 230));
		mColors.put("lightcoral", new Color(240, 128, 128));
		mColors.put("lightcyan", new Color(224, 255, 255));
		mColors.put("lightgoldenrodyellow", new Color(250, 250, 210));
		mColors.put("lightgray", new Color(211, 211, 211));
		mColors.put("lightgreen", new Color(144, 238, 144));
		mColors.put("lightgrey", new Color(211, 211, 211));
		mColors.put("lightpink", new Color(255, 182, 193));
		mColors.put("lightsalmon", new Color(255, 160, 122));
		mColors.put("lightseagreen", new Color(32, 178, 170));
		mColors.put("lightskyblue", new Color(135, 206, 250));
		mColors.put("lightslategray", new Color(119, 136, 153));
		mColors.put("lightslategrey", new Color(119, 136, 153));
		mColors.put("lightsteelblue", new Color(176, 196, 222));
		mColors.put("lightyellow", new Color(255, 255, 224));
		mColors.put("lime", new Color(0, 255, 0));
		mColors.put("limegreen", new Color(50, 205, 50));
		mColors.put("linen", new Color(250, 240, 230));
		mColors.put("magenta", new Color(255, 0, 255));
		mColors.put("maroon", new Color(128, 0, 0));
		mColors.put("mediumaquamarine", new Color(102, 205, 170));
		mColors.put("mediumblue", new Color(0, 0, 205));
		mColors.put("mediumorchid", new Color(186, 85, 211));
		mColors.put("mediumpurple", new Color(147, 112, 219));
		mColors.put("mediumseagreen", new Color(60, 179, 113));
		mColors.put("mediumslateblue", new Color(123, 104, 238));
		mColors.put("mediumspringgreen", new Color(0, 250, 154));
		mColors.put("mediumturquoise", new Color(72, 209, 204));
		mColors.put("mediumvioletred", new Color(199, 21, 133));
		mColors.put("midnightblue", new Color(25, 25, 112));
		mColors.put("mintcream", new Color(245, 255, 250));
		mColors.put("mistyrose", new Color(255, 228, 225));
		mColors.put("moccasin", new Color(255, 228, 181));
		mColors.put("navajowhite", new Color(255, 222, 173));
		mColors.put("navy", new Color(0, 0, 128));
		mColors.put("oldlace", new Color(253, 245, 230));
		mColors.put("olive", new Color(128, 128, 0));
		mColors.put("olivedrab", new Color(107, 142, 35));
		mColors.put("orange", new Color(255, 165, 0));
		mColors.put("orangered", new Color(255, 69, 0));
		mColors.put("orchid", new Color(218, 112, 214));
		mColors.put("palegoldenrod", new Color(238, 232, 170));
		mColors.put("palegreen", new Color(152, 251, 152));
		mColors.put("paleturquoise", new Color(175, 238, 238));
		mColors.put("palevioletred", new Color(219, 112, 147));
		mColors.put("papayawhip", new Color(255, 239, 213));
		mColors.put("peachpuff", new Color(255, 218, 185));
		mColors.put("peru", new Color(205, 133, 63));
		mColors.put("pink", new Color(255, 192, 203));
		mColors.put("plum", new Color(221, 160, 221));
		mColors.put("powderblue", new Color(176, 224, 230));
		mColors.put("purple", new Color(128, 0, 128));
		mColors.put("red", new Color(255, 0, 0));
		mColors.put("rosybrown", new Color(188, 143, 143));
		mColors.put("royalblue", new Color(65, 105, 225));
		mColors.put("saddlebrown", new Color(139, 69, 19));
		mColors.put("salmon", new Color(250, 128, 114));
		mColors.put("sandybrown", new Color(244, 164, 96));
		mColors.put("seagreen", new Color(46, 139, 87));
		mColors.put("seashell", new Color(255, 245, 238));
		mColors.put("sienna", new Color(160, 82, 45));
		mColors.put("silver", new Color(192, 192, 192));
		mColors.put("skyblue", new Color(135, 206, 235));
		mColors.put("slateblue", new Color(106, 90, 205));
		mColors.put("slategray", new Color(112, 128, 144));
		mColors.put("slategrey", new Color(112, 128, 144));
		mColors.put("snow", new Color(255, 250, 250));
		mColors.put("springgreen", new Color(0, 255, 127));
		mColors.put("steelblue", new Color(70, 130, 180));
		mColors.put("tan", new Color(210, 180, 140));
		mColors.put("teal", new Color(0, 128, 128));
		mColors.put("thistle", new Color(216, 191, 216));
		mColors.put("tomato", new Color(255, 99, 71));
		mColors.put("turquoise", new Color(64, 224, 208));
		mColors.put("violet", new Color(238, 130, 238));
		mColors.put("wheat", new Color(245, 222, 179));
		mColors.put("white", new Color(255, 255, 255));
		mColors.put("whitesmoke", new Color(245, 245, 245));
		mColors.put("yellow", new Color(255, 255, 0));
		mColors.put("yellowgreen", new Color(154, 205, 50));

		/*
		 * If user gave name of color file as property then use that.
		 */
		try
		{
			filename = System.getProperty(Constants.PROGRAM_NAME + ".rgb.file");
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
				String osname = System.getProperty("os.name");
				if (osname != null)
				{
					osname = osname.toLowerCase();
					if (osname.indexOf("sunos") > 0)
						filename = "/usr/openwin/lib/X11/rgb.txt";
				}
				reader = new LineNumberReader(new FileReader(filename));
			}
		}
		catch (FileNotFoundException e)
		{
			/*
			 * No color file available, just use basic set.
			 */
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
		reader.close();
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
				mColors.put(colorName, retval);
			}
		}
		return(retval);
	}
}
