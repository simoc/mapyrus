/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
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

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/*
 * Font metrics for a PostScript font, read from an Adobe Font Metrics (AFM) file.
 * Font metrics define the width of each character in a font.
 */
public class AdobeFontMetrics
{
	/*
	 * Full width of a character defined in an AFM file.
	 * Widths of each character are defined relative to this width.
	 */
	private static final int FULL_CHAR_SIZE = 1000;

	private static final String SPACE_GLYPH_NAME = "space";

	private String m_fontName;

	private HashMap<String, CharacterMetrics> m_charMetrics;
	private int m_firstChar, m_lastChar;
	private boolean m_isFixedPitch;
	private int m_italicAngle;
	private int m_capHeight;
	private int m_ascender;
	private int m_descender;
	private Rectangle m_fontBBox;
	private int m_flags;

	/*
	 * Lookup table from Unicode to PostScript glyph name. 
	 */
	private static HashMap<Integer, ArrayList<String>> m_glyphNames;

	static
	{
		/*
		 * Read lookup table from resource file.
		 */
		String res = "org/mapyrus/font/glyphlist.txt";
		InputStream inStream = AdobeFontMetrics.class.getClassLoader().getResourceAsStream(res);

		m_glyphNames = new HashMap<Integer, ArrayList<String>>(4400); 
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(inStream));
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (!line.startsWith("#"))
				{
					int index = line.indexOf(';');
					if (index >= 0)
					{
						String glyphName = line.substring(0, index);
						String hex = line.substring(index + 1);
						StringTokenizer st = new StringTokenizer(hex);
						while (st.hasMoreTokens())
						{
							String token = st.nextToken();
							int unicode = Integer.parseInt(token, 16);
							ArrayList<String> glyphNames = m_glyphNames.get(Integer.valueOf(unicode));
							if (glyphNames == null)
							{
								glyphNames = new ArrayList<String>(1);
								m_glyphNames.put(Integer.valueOf(unicode), glyphNames);
								
							}
							glyphNames.add(glyphName);
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			try
			{
				if (reader != null)
					reader.close();
			}
			catch (IOException e2)
			{
			}
		}
	}

	/**
	 * Create Adobe Font Metrics for a font by reading an AFM file.
	 * @param r file to read from.
	 * @param afmFilename filename being read (for any error message).
	 */	
	public AdobeFontMetrics (BufferedReader r, String afmFilename) throws IOException, MapyrusException
	{
		String line;
		boolean inCharMetrics = false;
		boolean finishedParsing = false;
		StringTokenizer st;

		m_charMetrics = new HashMap<String, CharacterMetrics>();
		m_isFixedPitch = false;
		m_firstChar = Integer.MAX_VALUE;
		m_lastChar = Integer.MIN_VALUE;
		m_flags = 32; /* Nonsymbolic font for PDF */

		try
		{
			line = r.readLine();
			if (line == null || (!line.startsWith("StartFontMetrics")))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
					": " + afmFilename);
			}

			while ((!finishedParsing) && line != null)
			{
				if (inCharMetrics && line.startsWith("C"))
				{
					st = new StringTokenizer(line);

					String token;
					int charIndex = -1;
					short charWidth = FULL_CHAR_SIZE;
					short charAscent = FULL_CHAR_SIZE;
					short charDescent = 0;
					String charName = "";

					while (st.hasMoreTokens())
					{
						/*
						 * Extract information that is of interest to us.
						 */
						token = st.nextToken();
						if (token.equals("C"))
						{
							charIndex = Integer.parseInt(st.nextToken());
						}
						else if (token.equals("CH"))
						{
							/*
							 * Parse hex value in format <2126>
							 */
							token = st.nextToken();
							if (token.length() >= 2)
								token = token.substring(1, token.length() - 1);
							charIndex = Integer.parseInt(token, 16);
						}
						else if (token.equals("WX"))
						{
							charWidth = Short.parseShort(st.nextToken());
						}
						else if (token.equals("N"))
						{
							charName = st.nextToken();
						}
						else if (token.equals("B"))
						{
							st.nextToken();
							charDescent = (short)Math.round(Double.parseDouble(st.nextToken()));
							st.nextToken();
							charAscent = (short)Math.round(Double.parseDouble(st.nextToken()));
						}

						token = st.nextToken();
						while ((!token.equals(";")) && st.hasMoreTokens())
						{
							/*
							 * Skip any unknown information until ';' end marker.
							 */
							token = st.nextToken();
						}
					}

					if (charIndex >= 0)
					{
						CharacterMetrics metrics;
						metrics = new CharacterMetrics((char)charIndex, charWidth, charAscent, charDescent);
						m_charMetrics.put(charName, metrics);
						if (charIndex == 32)
							m_charMetrics.put(SPACE_GLYPH_NAME, metrics);

						if (charIndex < m_firstChar)
							m_firstChar = charIndex;
						if (charIndex > m_lastChar)
							m_lastChar = charIndex;
					}
				}
				else if (line.startsWith("FontName"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken();	/* FontName */
					m_fontName = st.nextToken();
				}
				else if (line.startsWith("IsFixedPitch") && line.toLowerCase().indexOf("true") >= 0)
				{
					m_isFixedPitch = true;
					m_flags |= 1;
				}
				else if (line.startsWith("ItalicAngle"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken(); /* "ItalicAngle" */
					m_italicAngle = (int)Math.round(Double.parseDouble(st.nextToken()));
				}
				else if (line.startsWith("CapHeight"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken(); /* "CapHeight" */
					m_capHeight = Integer.parseInt(st.nextToken());
				}
				else if (line.startsWith("Ascender"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken(); /* "Ascender" */
					m_ascender = Integer.parseInt(st.nextToken());
				}
				else if (line.startsWith("Descender"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken(); /* "Descender" */
					m_descender = Integer.parseInt(st.nextToken());
				}
				else if (line.startsWith("FontBBox"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 5)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
					}
					st.nextToken(); /* "FontBBox" */
					int x1 = Integer.parseInt(st.nextToken());
					int y1 = Integer.parseInt(st.nextToken());
					int x2 = Integer.parseInt(st.nextToken());
					int y2 = Integer.parseInt(st.nextToken());
					m_fontBBox = new Rectangle(x1, y1, x2 - x1, y2 - y1);
				}
				else if (line.startsWith("Weight"))
				{
					st = new StringTokenizer(line);
					while(st.hasMoreTokens())
					{
						String token = st.nextToken();
						if (token.equalsIgnoreCase("italic"))
							m_flags |= 64;
					}
				}
				else if (line.startsWith("StartCharMetrics"))
				{
					inCharMetrics = true;
				}
				else if (line.startsWith("EndCharMetrics"))
				{
					inCharMetrics = false;
					finishedParsing = true;
				}

				line = r.readLine();
			}
		}
		catch (NumberFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
				": " + afmFilename + ": " + e.getMessage());
		}
	}

	/**
	 * Returns name of font.
	 * @return font name
	 */
	public String getFontName()
	{
		return(m_fontName);
	}

	/**
	 * Returns index of first character in font.
	 * @return index.
	 */
	public int getFirstChar()
	{
		return(m_firstChar);
	}

	/**
	 * Returns index of last character in font.
	 * @return index.
	 */
	public int getLastChar()
	{
		return(m_lastChar);
	}

	/**
	 * Get width of character.
	 * @param index index of character.
	 * @return width of this character.
	 */
	public int getCharWidth(int index)
	{
		for (CharacterMetrics metrics : m_charMetrics.values())
		{
			if (metrics.getCode() == index)
				return metrics.getWidth();
		}
		return 0;
	}

	/**
	 * Returns italic angle of font.
	 * @return italic angle.
	 */
	public int getItalicAngle()
	{
		return(m_italicAngle);
	}

	/**
	 * Returns height of capital letters in font.
	 * @return cap height.
	 */
	public int getCapHeight()
	{
		return(m_capHeight);
	}

	/**
	 * Returns bounding box of font.
	 * @return font bounding box.
	 */
	public Rectangle getFontBBox()
	{
		return(m_fontBBox);
	}

	/**
	 * Returns font type as PDF bit flags.
	 * @return font bit flags.
	 */
	public int getFlags()
	{
		return(m_flags);
	}
	
	/**
	 * Returns maximum height of font above baseline.
	 * @return ascender height.
	 */
	public int getAscender()
	{
		return(m_ascender);
	}

	/**
	 * Returns maximum height of font below baseline.
	 * @return descender height.
	 */
	public int getDescender()
	{
		return(m_descender);
	}

	/**
	 * Return string representation of object.
	 * @param string representation.
	 */
	public String toString()
	{
		return("Adobe Font Metrics for " + m_fontName);
	}

	/**
	 * Calculate size of string displayed using this font.
	 * @param s string to calculate size for.
	 * @param pointSize point size in which string is displayed.
	 * @return size of string in points.
	 */
	public StringDimension getStringDimension(String s, double pointSize)
	{
		int total = 0;
		int sLength = s.length();
		int c;
		double pointLen;
		double maxAscent = 0, minDescent = FULL_CHAR_SIZE;
		StringDimension retval = new StringDimension();

		/*
		 * Add up widths of all characters in string and
		 * find biggest ascent and descent.
		 */
		for (int i = 0; i < sLength; i++)
		{
			CharacterMetrics metrics = null;

			c = s.charAt(i);
			ArrayList<String> glyphNames = m_glyphNames.get(Integer.valueOf(c));
			if (glyphNames != null)
			{
				for (int j = 0; j < glyphNames.size() && metrics == null; j++)
					metrics = m_charMetrics.get(glyphNames.get(j));
			}
			if (metrics != null)
			{
				total += metrics.getWidth();
				if (metrics.getAscent() > maxAscent)
					maxAscent = metrics.getAscent();
				if (metrics.getDescent() < minDescent)
					minDescent = metrics.getDescent();
			}
			else
			{
				total += FULL_CHAR_SIZE;
			}
		}
		pointLen = (double)total / FULL_CHAR_SIZE * pointSize;
		maxAscent = (double)maxAscent / FULL_CHAR_SIZE * pointSize;
		minDescent = (double)minDescent / FULL_CHAR_SIZE * pointSize;

		if (m_isFixedPitch)
		{
			/*
			 * All characters are same width so width of string
			 * depends only on length of string.
			 */
			CharacterMetrics metrics = m_charMetrics.get(SPACE_GLYPH_NAME);
			if (metrics != null)
			{
				pointLen = s.length() * ((double)metrics.getWidth() / FULL_CHAR_SIZE) * pointSize;
			}
		}
		retval.setSize(pointLen, pointSize, maxAscent, minDescent);
		return(retval);
	}

	/**
	 * Get character in PostScript font that character maps to.
	 * @param c character (in Unicode).
	 * @return character in PostScript font. 
	 */
	public char getEncodedChar(char c)
	{
		CharacterMetrics metrics = null;
		ArrayList<String> glyphNames = m_glyphNames.get(Integer.valueOf(c));
		if (glyphNames != null)
		{
			for (int i = 0; i < glyphNames.size() && metrics == null; i++)
				metrics = m_charMetrics.get(glyphNames.get(i));
		}
		char retval = (metrics != null) ? metrics.getCode() : c;
		return retval;
	}
}
