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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.mapyrus.font;

/**
 * Metrics for a character read from an Adobe Font Metrics (AFM) file.
 */
public class CharacterMetrics
{
	private char m_code; 
	private short m_width;
	private short m_ascent;
	private short m_descent;

	/**
	 * Create metrics for a character.
	 * @param code character code in AFM file.
	 * @param width width of character.
	 * @param ascent ascent of character.
	 * @param descent descent of character.
	 */
	public CharacterMetrics(char code, short width, short ascent, short descent)
	{
		m_code = code;
		m_width = width;
		m_ascent = ascent;
		m_descent = descent;
	}

	public char getCode()
	{
		return m_code;
	}

	public short getWidth()
	{
		return m_width;
	}

	public short getAscent()
	{
		return m_ascent;
	}

	public short getDescent()
	{
		return m_descent;
	}
}
