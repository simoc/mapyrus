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
package org.mapyrus.font;

import java.io.BufferedReader;
import java.io.IOException;

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
	private String mFontName;

	private int mCharWidth;
	private short []mCharWidths;
	private boolean mIsFixedPitch;

	/*
	 * Lookup table of ISOLatin1 character indexes for named extended characters.
	 * Taken from Adobe PostScript Language Reference Manual (2nd Edition) Appendix E,
	 * p. 605.
	 */
	private static HashMap mISOLatin1CharIndexes;
	
	static
	{
		mISOLatin1CharIndexes = new HashMap(256);
		mISOLatin1CharIndexes.put("Aacute", new Integer(193));
		mISOLatin1CharIndexes.put("Acircumflex", new Integer(194));
		mISOLatin1CharIndexes.put("Adieresis", new Integer(196));
		mISOLatin1CharIndexes.put("Agrave", new Integer(192));
		mISOLatin1CharIndexes.put("Aring", new Integer(197));
		mISOLatin1CharIndexes.put("Atilde", new Integer(195));
		mISOLatin1CharIndexes.put("Ccedilla", new Integer(199));
		mISOLatin1CharIndexes.put("Eacute", new Integer(201));
		mISOLatin1CharIndexes.put("Ecircumflex", new Integer(202));
		mISOLatin1CharIndexes.put("Edieresis", new Integer(203));
		mISOLatin1CharIndexes.put("Egrave", new Integer(200));
		mISOLatin1CharIndexes.put("Eth", new Integer(0));
		mISOLatin1CharIndexes.put("Iacute", new Integer(205));
		mISOLatin1CharIndexes.put("Icircumflex", new Integer(206));
		mISOLatin1CharIndexes.put("Idieresis", new Integer(207));
		mISOLatin1CharIndexes.put("Igrave", new Integer(204));
		mISOLatin1CharIndexes.put("Oacute", new Integer(211));
		mISOLatin1CharIndexes.put("Ocircumflex", new Integer(212));
		mISOLatin1CharIndexes.put("Odieresis", new Integer(214));
		mISOLatin1CharIndexes.put("Ograve", new Integer(210));
		mISOLatin1CharIndexes.put("Oslash", new Integer(216));
		mISOLatin1CharIndexes.put("Otilde", new Integer(213));
		mISOLatin1CharIndexes.put("Thorn", new Integer(222));
		mISOLatin1CharIndexes.put("Uacute", new Integer(218));
		mISOLatin1CharIndexes.put("Ucircumflex", new Integer(219));
		mISOLatin1CharIndexes.put("Udieresis", new Integer(220));
		mISOLatin1CharIndexes.put("Ugrave", new Integer(217));
		mISOLatin1CharIndexes.put("Yacute", new Integer(221));
		mISOLatin1CharIndexes.put("acircumflex", new Integer(226));
		mISOLatin1CharIndexes.put("acute", new Integer(225));
		mISOLatin1CharIndexes.put("adieresis", new Integer(228));
		mISOLatin1CharIndexes.put("agrave", new Integer(224));
		mISOLatin1CharIndexes.put("aring", new Integer(229));
		mISOLatin1CharIndexes.put("atilde", new Integer(227));
		mISOLatin1CharIndexes.put("breve", new Integer(150));
		mISOLatin1CharIndexes.put("brokenbar", new Integer(166));
		mISOLatin1CharIndexes.put("caron", new Integer(159));
		mISOLatin1CharIndexes.put("ccedilla", new Integer(231));
		mISOLatin1CharIndexes.put("cedilla", new Integer(184));
		mISOLatin1CharIndexes.put("cent", new Integer(162));
		mISOLatin1CharIndexes.put("circumflex", new Integer(147));
		mISOLatin1CharIndexes.put("copyright", new Integer(169));
		mISOLatin1CharIndexes.put("currency", new Integer(164));
		mISOLatin1CharIndexes.put("degree", new Integer(176));
		mISOLatin1CharIndexes.put("dieresis", new Integer(168));
		mISOLatin1CharIndexes.put("divide", new Integer(247));
		mISOLatin1CharIndexes.put("dotaccent", new Integer(151));
		mISOLatin1CharIndexes.put("dotlessi", new Integer(144));
		mISOLatin1CharIndexes.put("eacute", new Integer(233));
		mISOLatin1CharIndexes.put("ecircumflex", new Integer(234));
		mISOLatin1CharIndexes.put("edieresis", new Integer(235));
		mISOLatin1CharIndexes.put("egrave", new Integer(232));
		mISOLatin1CharIndexes.put("eth", new Integer(240));
		mISOLatin1CharIndexes.put("exclamdown", new Integer(161));
		mISOLatin1CharIndexes.put("germandbls", new Integer(223));
		mISOLatin1CharIndexes.put("grave", new Integer(145));
		mISOLatin1CharIndexes.put("guillemotleft", new Integer(171));
		mISOLatin1CharIndexes.put("guillemotright", new Integer(187));
		mISOLatin1CharIndexes.put("hungarumlaut", new Integer(157));
		mISOLatin1CharIndexes.put("hyphen", new Integer(173));
		mISOLatin1CharIndexes.put("iacute", new Integer(237));
		mISOLatin1CharIndexes.put("icircumflex", new Integer(238));
		mISOLatin1CharIndexes.put("idieresis", new Integer(239));
		mISOLatin1CharIndexes.put("igrave", new Integer(236));
		mISOLatin1CharIndexes.put("logicalnot", new Integer(172));
		mISOLatin1CharIndexes.put("macron", new Integer(175));
		mISOLatin1CharIndexes.put("mu", new Integer(0));
		mISOLatin1CharIndexes.put("multiply", new Integer(215));
		mISOLatin1CharIndexes.put("ntilde", new Integer(241));
		mISOLatin1CharIndexes.put("oacute", new Integer(243));
		mISOLatin1CharIndexes.put("ocircumflex", new Integer(244));
		mISOLatin1CharIndexes.put("odieresis", new Integer(246));
		mISOLatin1CharIndexes.put("ogonek", new Integer(158));
		mISOLatin1CharIndexes.put("ograve", new Integer(242));
		mISOLatin1CharIndexes.put("onehalf", new Integer(189));
		mISOLatin1CharIndexes.put("onequarter", new Integer(188));
		mISOLatin1CharIndexes.put("onesuperior", new Integer(185));
		mISOLatin1CharIndexes.put("ordfeminine", new Integer(170));
		mISOLatin1CharIndexes.put("ordmasculine", new Integer(186));
		mISOLatin1CharIndexes.put("oslash", new Integer(248));
		mISOLatin1CharIndexes.put("otilde", new Integer(245));
		mISOLatin1CharIndexes.put("paragraph", new Integer(182));
		mISOLatin1CharIndexes.put("periodcentered", new Integer(183));
		mISOLatin1CharIndexes.put("plusminus", new Integer(177));
		mISOLatin1CharIndexes.put("questiondown", new Integer(191));
		mISOLatin1CharIndexes.put("registered", new Integer(174));
		mISOLatin1CharIndexes.put("ring", new Integer(154));
		mISOLatin1CharIndexes.put("section", new Integer(167));
		mISOLatin1CharIndexes.put("sterling", new Integer(163));
		mISOLatin1CharIndexes.put("thorn", new Integer(254));
		mISOLatin1CharIndexes.put("threequarters", new Integer(190));
		mISOLatin1CharIndexes.put("threesuperior", new Integer(179));
		mISOLatin1CharIndexes.put("tilde", new Integer(148));
		mISOLatin1CharIndexes.put("twosuperior", new Integer(178));
		mISOLatin1CharIndexes.put("uacute", new Integer(250));
		mISOLatin1CharIndexes.put("ucircumflex", new Integer(251));
		mISOLatin1CharIndexes.put("udieresis", new Integer(252));
		mISOLatin1CharIndexes.put("ugrave", new Integer(249));
		mISOLatin1CharIndexes.put("yacute", new Integer(253));
		mISOLatin1CharIndexes.put("ydieresis", new Integer(255));
		mISOLatin1CharIndexes.put("yen", new Integer(165));
	}

	/**
	 * Create Adobe Font Metrics for a font by reading an AFM file.
	 * @param r file to read from.
	 * @param filename filename being read (for any error message).
	 * @param ISOLatin1EncodedFonts names of fonts for which to use ISO Latin1 encoding.
	 */	
	public AdobeFontMetrics (BufferedReader r, String filename, HashSet ISOLatin1EncodedFonts)
		throws IOException, MapyrusException
	{
		String line;
		boolean inCharMetrics = false;
		boolean finishedParsing = false;
		StringTokenizer st;
		boolean convertToISOLatin1 = false;

		// TODO handle fonts with more than 256 characters. 
		mCharWidths = new short[256];
		mIsFixedPitch = false;

		try
		{
			line = r.readLine();
			if (line == null || (!line.startsWith("StartFontMetrics")))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
					": " + filename);
			}

			while ((!finishedParsing) && line != null)
			{
				if (line.startsWith("FontName"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + filename);
					}
					st.nextToken();	/* FontName */
					mFontName = st.nextToken();
					convertToISOLatin1 = ISOLatin1EncodedFonts.contains(mFontName);
				}
				else if (line.startsWith("FontBBox"))
				{
					st = new StringTokenizer(line);
					if (st.countTokens() < 5)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + filename);
					}

					st.nextToken();
					st.nextToken(); /* xMin */
					st.nextToken();	/* yMin */
					int xMax = Integer.parseInt(st.nextToken());
					mCharWidth = xMax;
				}
				else if (line.startsWith("IsFixedPitch") && line.toLowerCase().indexOf("true") >= 0)
				{
					mIsFixedPitch = true;
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
				else if (inCharMetrics && line.startsWith("C"))
				{
					st = new StringTokenizer(line, " ;");
					if (st.countTokens() < 6)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + filename);
					}

					st.nextToken();	/* "C" */
					int charIndex = Integer.parseInt(st.nextToken());
					st.nextToken();	/* "WX" */
					short charWidth = Short.parseShort(st.nextToken());
					st.nextToken(); /* N */
					String charName = st.nextToken();
					
					/*
					 * Lookup index in ISOLatin1 encoding for this character.
					 */
					if (convertToISOLatin1 && (charIndex < 0 || charIndex > 127))
					{
						Integer i = (Integer)mISOLatin1CharIndexes.get(charName);
						if (i != null)
							charIndex = i.intValue();
					}

					if (charIndex >= 0 && charIndex < mCharWidths.length)
					{
						mCharWidths[charIndex] = charWidth;
					}
				}
				line = r.readLine();
			}
		}
		catch (NumberFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
				": " + filename + ": " + e.getMessage());
		}
	}

	/**
	 * Returns name of font.
	 * @return font name
	 */
	public String getFontName()
	{
		return(mFontName);
	}

	/**
	 * Return string representation of object.
	 * @param string representation.
	 */
	public String toString()
	{
		return("Adobe Font Metrics for " + mFontName);
	}

	/**
	 * Calculate the width of string displayed using this font.
	 * @param s string to calculate width for.
	 * @param pointSize point size in which string is displayed.
	 * @return width of string in points. 
	 */
	public int getStringWidth(String s, int pointSize)
	{
		int total = 0;
		int sLength = s.length();
		int c;
		double pointLen;

		if (mIsFixedPitch)
		{
			/*
			 * All characters are same width so width of string
			 * depends only on length of string.
			 */
			int spaceIndex = 32;
			// TODO read AFM standard to check that dividing by 1000 is correct.
			pointLen = s.length() * (mCharWidths[spaceIndex] / 1000.0) * pointSize;
		}
		else
		{
			/*
			 * Add up widths of all characters in string.
			 */
			for (int i = 0; i < sLength; i++)
			{
				c = s.charAt(i);
				if (c >= 0 && c < mCharWidths.length)
					total += mCharWidths[c];
				else
					total += mCharWidth;
			}
			pointLen = (double)total / mCharWidth * pointSize;
		}
		return((int)pointLen);
	}
}
