/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
	private String m_fontName;

	/*
	 * Font filename.
	 */
	private String m_filename;

	/**
	 * Create TrueType 1 font from a .ttf file.
	 * @param filename name of .ttf file.
	 * @throws IOException if font file cannot be read.
	 * @throws MapyrusException if font file has wrong format.
	 */
	public TrueTypeFont(String filename) throws IOException, MapyrusException
	{
		/*
		 * Only accept filenames with .ttf suffix.
		 */
		if (!filename.toLowerCase().endsWith(".ttf"))
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_TTF_FILE) +
				": " + filename);

		m_filename = filename;
		Font f = getFont();

		/*
		 * Use PostScript name for this font so we have a name without spaces.
		 */
		m_fontName = f.getPSName();
	}

	/**
	 * Return name of font, parsed from .ttf file.
	 * @return font name.
	 */
	public String getName()
	{
		return(m_fontName);
	}

	/**
	 * String representation of TrueType font.
	 * @return font name.
	 */
	@Override
	public String toString()
	{
		return("TrueType Font " + m_fontName);
	}

	/**
	 * Return definition of font read from .ttf file that can be used
	 * to derive font of any size.
	 * @return font definition.
	 * @throws IOException if font file cannot be read.
	 * @throws MapyrusException if font file has wrong format.
	 */
	public Font getFont() throws IOException, MapyrusException
	{
		try (FileInputStream f = new FileInputStream(m_filename))
		{
			return Font.createFont(Font.TRUETYPE_FONT, f);
		}
		catch (FontFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_TTF_FILE) +
				": " + m_filename);
		}
	}
}
