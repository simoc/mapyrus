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

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	private static HashMap<Integer, ArrayList<String>> m_defaultGlyphNames;
	
	private HashMap<Integer, ArrayList<String>> m_customGlyphNames;

	/*
	 * Lookup table of ISOLatin1 (ISO-8859-1) character indexes for named extended characters.
	 * Taken from Adobe PostScript Language Reference Manual (2nd Edition) Appendix E,
	 * p. 605.
	 */
	private static HashMap<String, Integer> m_ISOLatin1CharIndexes;
	
	static
	{
		m_ISOLatin1CharIndexes = new HashMap<String, Integer>(256);
		m_ISOLatin1CharIndexes.put("Aacute", Integer.valueOf(193));
		m_ISOLatin1CharIndexes.put("Acircumflex", Integer.valueOf(194));
		m_ISOLatin1CharIndexes.put("Adieresis", Integer.valueOf(196));
		m_ISOLatin1CharIndexes.put("Agrave", Integer.valueOf(192));
		m_ISOLatin1CharIndexes.put("Aring", Integer.valueOf(197));
		m_ISOLatin1CharIndexes.put("Atilde", Integer.valueOf(195));
		m_ISOLatin1CharIndexes.put("Ccedilla", Integer.valueOf(199));
		m_ISOLatin1CharIndexes.put("Eacute", Integer.valueOf(201));
		m_ISOLatin1CharIndexes.put("Ecircumflex", Integer.valueOf(202));
		m_ISOLatin1CharIndexes.put("Edieresis", Integer.valueOf(203));
		m_ISOLatin1CharIndexes.put("Egrave", Integer.valueOf(200));
		m_ISOLatin1CharIndexes.put("Eth", Integer.valueOf(0));
		m_ISOLatin1CharIndexes.put("Iacute", Integer.valueOf(205));
		m_ISOLatin1CharIndexes.put("Icircumflex", Integer.valueOf(206));
		m_ISOLatin1CharIndexes.put("Idieresis", Integer.valueOf(207));
		m_ISOLatin1CharIndexes.put("Igrave", Integer.valueOf(204));
		m_ISOLatin1CharIndexes.put("Oacute", Integer.valueOf(211));
		m_ISOLatin1CharIndexes.put("Ocircumflex", Integer.valueOf(212));
		m_ISOLatin1CharIndexes.put("Odieresis", Integer.valueOf(214));
		m_ISOLatin1CharIndexes.put("Ograve", Integer.valueOf(210));
		m_ISOLatin1CharIndexes.put("Oslash", Integer.valueOf(216));
		m_ISOLatin1CharIndexes.put("Otilde", Integer.valueOf(213));
		m_ISOLatin1CharIndexes.put("Thorn", Integer.valueOf(222));
		m_ISOLatin1CharIndexes.put("Uacute", Integer.valueOf(218));
		m_ISOLatin1CharIndexes.put("Ucircumflex", Integer.valueOf(219));
		m_ISOLatin1CharIndexes.put("Udieresis", Integer.valueOf(220));
		m_ISOLatin1CharIndexes.put("Ugrave", Integer.valueOf(217));
		m_ISOLatin1CharIndexes.put("Yacute", Integer.valueOf(221));
		m_ISOLatin1CharIndexes.put("acircumflex", Integer.valueOf(226));
		m_ISOLatin1CharIndexes.put("acute", Integer.valueOf(225));
		m_ISOLatin1CharIndexes.put("adieresis", Integer.valueOf(228));
		m_ISOLatin1CharIndexes.put("agrave", Integer.valueOf(224));
		m_ISOLatin1CharIndexes.put("aring", Integer.valueOf(229));
		m_ISOLatin1CharIndexes.put("atilde", Integer.valueOf(227));
		m_ISOLatin1CharIndexes.put("breve", Integer.valueOf(150));
		m_ISOLatin1CharIndexes.put("brokenbar", Integer.valueOf(166));
		m_ISOLatin1CharIndexes.put("caron", Integer.valueOf(159));
		m_ISOLatin1CharIndexes.put("ccedilla", Integer.valueOf(231));
		m_ISOLatin1CharIndexes.put("cedilla", Integer.valueOf(184));
		m_ISOLatin1CharIndexes.put("cent", Integer.valueOf(162));
		m_ISOLatin1CharIndexes.put("circumflex", Integer.valueOf(147));
		m_ISOLatin1CharIndexes.put("copyright", Integer.valueOf(169));
		m_ISOLatin1CharIndexes.put("currency", Integer.valueOf(164));
		m_ISOLatin1CharIndexes.put("degree", Integer.valueOf(176));
		m_ISOLatin1CharIndexes.put("dieresis", Integer.valueOf(168));
		m_ISOLatin1CharIndexes.put("divide", Integer.valueOf(247));
		m_ISOLatin1CharIndexes.put("dotaccent", Integer.valueOf(151));
		m_ISOLatin1CharIndexes.put("dotlessi", Integer.valueOf(144));
		m_ISOLatin1CharIndexes.put("eacute", Integer.valueOf(233));
		m_ISOLatin1CharIndexes.put("ecircumflex", Integer.valueOf(234));
		m_ISOLatin1CharIndexes.put("edieresis", Integer.valueOf(235));
		m_ISOLatin1CharIndexes.put("egrave", Integer.valueOf(232));
		m_ISOLatin1CharIndexes.put("eth", Integer.valueOf(240));
		m_ISOLatin1CharIndexes.put("exclamdown", Integer.valueOf(161));
		m_ISOLatin1CharIndexes.put("germandbls", Integer.valueOf(223));
		m_ISOLatin1CharIndexes.put("grave", Integer.valueOf(145));
		m_ISOLatin1CharIndexes.put("guillemotleft", Integer.valueOf(171));
		m_ISOLatin1CharIndexes.put("guillemotright", Integer.valueOf(187));
		m_ISOLatin1CharIndexes.put("hungarumlaut", Integer.valueOf(157));
		m_ISOLatin1CharIndexes.put("hyphen", Integer.valueOf(173));
		m_ISOLatin1CharIndexes.put("iacute", Integer.valueOf(237));
		m_ISOLatin1CharIndexes.put("icircumflex", Integer.valueOf(238));
		m_ISOLatin1CharIndexes.put("idieresis", Integer.valueOf(239));
		m_ISOLatin1CharIndexes.put("igrave", Integer.valueOf(236));
		m_ISOLatin1CharIndexes.put("logicalnot", Integer.valueOf(172));
		m_ISOLatin1CharIndexes.put("macron", Integer.valueOf(175));
		m_ISOLatin1CharIndexes.put("mu", Integer.valueOf(0));
		m_ISOLatin1CharIndexes.put("multiply", Integer.valueOf(215));
		m_ISOLatin1CharIndexes.put("ntilde", Integer.valueOf(241));
		m_ISOLatin1CharIndexes.put("oacute", Integer.valueOf(243));
		m_ISOLatin1CharIndexes.put("ocircumflex", Integer.valueOf(244));
		m_ISOLatin1CharIndexes.put("odieresis", Integer.valueOf(246));
		m_ISOLatin1CharIndexes.put("ogonek", Integer.valueOf(158));
		m_ISOLatin1CharIndexes.put("ograve", Integer.valueOf(242));
		m_ISOLatin1CharIndexes.put("onehalf", Integer.valueOf(189));
		m_ISOLatin1CharIndexes.put("onequarter", Integer.valueOf(188));
		m_ISOLatin1CharIndexes.put("onesuperior", Integer.valueOf(185));
		m_ISOLatin1CharIndexes.put("ordfeminine", Integer.valueOf(170));
		m_ISOLatin1CharIndexes.put("ordmasculine", Integer.valueOf(186));
		m_ISOLatin1CharIndexes.put("oslash", Integer.valueOf(248));
		m_ISOLatin1CharIndexes.put("otilde", Integer.valueOf(245));
		m_ISOLatin1CharIndexes.put("paragraph", Integer.valueOf(182));
		m_ISOLatin1CharIndexes.put("periodcentered", Integer.valueOf(183));
		m_ISOLatin1CharIndexes.put("plusminus", Integer.valueOf(177));
		m_ISOLatin1CharIndexes.put("questiondown", Integer.valueOf(191));
		m_ISOLatin1CharIndexes.put("registered", Integer.valueOf(174));
		m_ISOLatin1CharIndexes.put("ring", Integer.valueOf(154));
		m_ISOLatin1CharIndexes.put("section", Integer.valueOf(167));
		m_ISOLatin1CharIndexes.put("sterling", Integer.valueOf(163));
		m_ISOLatin1CharIndexes.put("thorn", Integer.valueOf(254));
		m_ISOLatin1CharIndexes.put("threequarters", Integer.valueOf(190));
		m_ISOLatin1CharIndexes.put("threesuperior", Integer.valueOf(179));
		m_ISOLatin1CharIndexes.put("tilde", Integer.valueOf(148));
		m_ISOLatin1CharIndexes.put("twosuperior", Integer.valueOf(178));
		m_ISOLatin1CharIndexes.put("uacute", Integer.valueOf(250));
		m_ISOLatin1CharIndexes.put("ucircumflex", Integer.valueOf(251));
		m_ISOLatin1CharIndexes.put("udieresis", Integer.valueOf(252));
		m_ISOLatin1CharIndexes.put("ugrave", Integer.valueOf(249));
		m_ISOLatin1CharIndexes.put("yacute", Integer.valueOf(253));
		m_ISOLatin1CharIndexes.put("ydieresis", Integer.valueOf(255));
		m_ISOLatin1CharIndexes.put("yen", Integer.valueOf(165));

		/*
		 * Read lookup table from resource file.
		 */
		String res = "org/mapyrus/font/glyphlist.txt";
		InputStream inStream = AdobeFontMetrics.class.getClassLoader().getResourceAsStream(res);

		m_defaultGlyphNames = new HashMap<Integer, ArrayList<String>>(); 
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(inStream));
			m_defaultGlyphNames = readGlyphNames(reader);
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
	 * @param ISOLatin1EncodedFonts names of fonts for which to use ISO Latin1 encoding.
	 * @param glyphFilename optional Adobe Glyph List filename.
	 */	
	public AdobeFontMetrics (BufferedReader r, String afmFilename,
		HashSet<String> ISOLatin1EncodedFonts,
		String glyphFilename) throws IOException, MapyrusException
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
		boolean convertToISOLatin1 = false;

		BufferedReader glyphReader = null;
		try
		{
			if (glyphFilename != null)
			{
				glyphReader = new BufferedReader(new FileReader(glyphFilename));
				m_customGlyphNames = readGlyphNames(glyphReader);
			}
		}
		finally
		{
			try
			{
				if (glyphReader != null)
					glyphReader.close();
			}
			catch (IOException e)
			{
			}
		}

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

					if (convertToISOLatin1)
					{
						/*
						 * Use ISO-8859-1 character code for glyph regardless of code in AFM file.
						 */
						Integer i = m_ISOLatin1CharIndexes.get(charName);
						if (i != null)
							charIndex = i.intValue();
					}
					if (charIndex >= 0)
					{
						CharacterMetrics metrics;
						metrics = new CharacterMetrics((char)charIndex, charWidth, charAscent, charDescent);
						while (m_charMetrics.containsKey(charName))
						{
							/*
							 * Some fonts define the same character name twice.
							 * For example, 'space' as char index 32 and 160 and
							 * 'hyphen' as 45 and 173.
							 * Include both values with different names so character
							 * width table will be correct.
							 */
							charName = charName + "X";
						}
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
					convertToISOLatin1 = ISOLatin1EncodedFonts.contains(m_fontName);
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
	 * @return string representation.
	 */
	public String toString()
	{
		return("Adobe Font Metrics for " + m_fontName);
	}

	/**
	 * Read Adobe glyphlist.txt file giving PostScript glyph names for each unicode character.
	 * @param reader glyphlist.txt file.
	 * @return lookup table.
	 */
	private static HashMap<Integer, ArrayList<String>> readGlyphNames(BufferedReader reader)
		throws IOException
	{
		HashMap<Integer, ArrayList<String>> glyphNameMap = new HashMap<Integer, ArrayList<String>>();

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
						ArrayList<String> glyphNames = glyphNameMap.get(Integer.valueOf(unicode));
						if (glyphNames == null)
						{
							glyphNames = new ArrayList<String>(1);
							glyphNameMap.put(Integer.valueOf(unicode), glyphNames);
							
						}
						glyphNames.add(glyphName);
					}
				}
			}
		}
		return glyphNameMap;
	}

	/**
	 * Get PostScript glyph names for a unicode character.
	 * @param unicode unicode character.
	 * @return list of glyph names, or null if not found.
	 */
	private ArrayList<String> getGlyphNames(char unicode)
	{
		ArrayList<String> retval;

		if (m_customGlyphNames != null)
			retval = m_customGlyphNames.get(Integer.valueOf(unicode));
		else
			retval = m_defaultGlyphNames.get(Integer.valueOf(unicode));
		return retval;
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
		char c;
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
			ArrayList<String> glyphNames = getGlyphNames(c);
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
		ArrayList<String> glyphNames = getGlyphNames(c);
		if (glyphNames != null)
		{
			for (int i = 0; i < glyphNames.size() && metrics == null; i++)
				metrics = m_charMetrics.get(glyphNames.get(i));
		}
		char retval = (metrics != null) ? metrics.getCode() : c;
		return retval;
	}
}
