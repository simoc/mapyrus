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
package org.mapyrus.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * A TrueType font, read from a .ttf font definition file.
 * Provides methods to parse the font file and create a Java Font object.
 */
public class TrueTypeFont
{
	/*
	 * Name of font given in TTF font file.
	 */
	private String mFontName;

	/*
	 * Font filename.
	 */
	private String mFilename;

	/**
	 * Create TrueType 1 font from a .ttf file.
	 * @param filename name of .ttf file.
	 */
	public TrueTypeFont(String filename) throws IOException, MapyrusException
	{
		/*
		 * Only accept filenames with .ttf suffix.
		 */
		if (!filename.toLowerCase().endsWith(".ttf"))
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_TTF_FILE) +
				": " + filename);

		mFilename = filename;
		Font f = getFont();
		
		/*
		 * Use PostScript name for this font so we have a name without spaces.
		 */
		mFontName = f.getPSName();
	}

	/**
	 * Return name of font, parsed from .ttf file.
	 * @return font name.
	 */
	public String getName()
	{
		return(mFontName);
	}

	/**
	 * String representation of TrueType font.
	 * @return font name.
	 */
	public String toString()
	{
		return("TrueType Font " + mFontName);
	}

	/**
	 * Return definition of font read from .ttf file that can be used
	 * to derive font of any size.
	 * @return font definition.
	 */	
	public Font getFont() throws IOException, MapyrusException
	{
		Font font = null;
		FileInputStream f = null;

		try
		{
			f = new FileInputStream(mFilename);
			font = Font.createFont(Font.TRUETYPE_FONT, f);
		}
		catch (FontFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_TTF_FILE) +
				": " + mFilename);
		}
		finally
		{
			if (f != null)
				f.close();
		}
		return(font);
	}
}
