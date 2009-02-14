/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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
	private static final int FULL_CHAR_WIDTH = 1000;

	private String mFontName;

	private short []mCharWidths;
	private int mFirstChar, mLastChar;
	private boolean mIsFixedPitch;
	private int mItalicAngle;
	private int mCapHeight;
	private int mAscender;
	private int mDescender;
	private Rectangle mFontBBox;
	private int mFlags;

	/*
	 * Lookup table of ISOLatin1 character indexes for named extended characters.
	 * Taken from Adobe PostScript Language Reference Manual (2nd Edition) Appendix E,
	 * p. 605.
	 */
	private static HashMap<String, Integer> mISOLatin1CharIndexes;
	
	static
	{
		mISOLatin1CharIndexes = new HashMap<String, Integer>(256);
		mISOLatin1CharIndexes.put("Aacute", Integer.valueOf(193));
		mISOLatin1CharIndexes.put("Acircumflex", Integer.valueOf(194));
		mISOLatin1CharIndexes.put("Adieresis", Integer.valueOf(196));
		mISOLatin1CharIndexes.put("Agrave", Integer.valueOf(192));
		mISOLatin1CharIndexes.put("Aring", Integer.valueOf(197));
		mISOLatin1CharIndexes.put("Atilde", Integer.valueOf(195));
		mISOLatin1CharIndexes.put("Ccedilla", Integer.valueOf(199));
		mISOLatin1CharIndexes.put("Eacute", Integer.valueOf(201));
		mISOLatin1CharIndexes.put("Ecircumflex", Integer.valueOf(202));
		mISOLatin1CharIndexes.put("Edieresis", Integer.valueOf(203));
		mISOLatin1CharIndexes.put("Egrave", Integer.valueOf(200));
		mISOLatin1CharIndexes.put("Eth", Integer.valueOf(0));
		mISOLatin1CharIndexes.put("Iacute", Integer.valueOf(205));
		mISOLatin1CharIndexes.put("Icircumflex", Integer.valueOf(206));
		mISOLatin1CharIndexes.put("Idieresis", Integer.valueOf(207));
		mISOLatin1CharIndexes.put("Igrave", Integer.valueOf(204));
		mISOLatin1CharIndexes.put("Oacute", Integer.valueOf(211));
		mISOLatin1CharIndexes.put("Ocircumflex", Integer.valueOf(212));
		mISOLatin1CharIndexes.put("Odieresis", Integer.valueOf(214));
		mISOLatin1CharIndexes.put("Ograve", Integer.valueOf(210));
		mISOLatin1CharIndexes.put("Oslash", Integer.valueOf(216));
		mISOLatin1CharIndexes.put("Otilde", Integer.valueOf(213));
		mISOLatin1CharIndexes.put("Thorn", Integer.valueOf(222));
		mISOLatin1CharIndexes.put("Uacute", Integer.valueOf(218));
		mISOLatin1CharIndexes.put("Ucircumflex", Integer.valueOf(219));
		mISOLatin1CharIndexes.put("Udieresis", Integer.valueOf(220));
		mISOLatin1CharIndexes.put("Ugrave", Integer.valueOf(217));
		mISOLatin1CharIndexes.put("Yacute", Integer.valueOf(221));
		mISOLatin1CharIndexes.put("acircumflex", Integer.valueOf(226));
		mISOLatin1CharIndexes.put("acute", Integer.valueOf(225));
		mISOLatin1CharIndexes.put("adieresis", Integer.valueOf(228));
		mISOLatin1CharIndexes.put("agrave", Integer.valueOf(224));
		mISOLatin1CharIndexes.put("aring", Integer.valueOf(229));
		mISOLatin1CharIndexes.put("atilde", Integer.valueOf(227));
		mISOLatin1CharIndexes.put("breve", Integer.valueOf(150));
		mISOLatin1CharIndexes.put("brokenbar", Integer.valueOf(166));
		mISOLatin1CharIndexes.put("caron", Integer.valueOf(159));
		mISOLatin1CharIndexes.put("ccedilla", Integer.valueOf(231));
		mISOLatin1CharIndexes.put("cedilla", Integer.valueOf(184));
		mISOLatin1CharIndexes.put("cent", Integer.valueOf(162));
		mISOLatin1CharIndexes.put("circumflex", Integer.valueOf(147));
		mISOLatin1CharIndexes.put("copyright", Integer.valueOf(169));
		mISOLatin1CharIndexes.put("currency", Integer.valueOf(164));
		mISOLatin1CharIndexes.put("degree", Integer.valueOf(176));
		mISOLatin1CharIndexes.put("dieresis", Integer.valueOf(168));
		mISOLatin1CharIndexes.put("divide", Integer.valueOf(247));
		mISOLatin1CharIndexes.put("dotaccent", Integer.valueOf(151));
		mISOLatin1CharIndexes.put("dotlessi", Integer.valueOf(144));
		mISOLatin1CharIndexes.put("eacute", Integer.valueOf(233));
		mISOLatin1CharIndexes.put("ecircumflex", Integer.valueOf(234));
		mISOLatin1CharIndexes.put("edieresis", Integer.valueOf(235));
		mISOLatin1CharIndexes.put("egrave", Integer.valueOf(232));
		mISOLatin1CharIndexes.put("eth", Integer.valueOf(240));
		mISOLatin1CharIndexes.put("exclamdown", Integer.valueOf(161));
		mISOLatin1CharIndexes.put("germandbls", Integer.valueOf(223));
		mISOLatin1CharIndexes.put("grave", Integer.valueOf(145));
		mISOLatin1CharIndexes.put("guillemotleft", Integer.valueOf(171));
		mISOLatin1CharIndexes.put("guillemotright", Integer.valueOf(187));
		mISOLatin1CharIndexes.put("hungarumlaut", Integer.valueOf(157));
		mISOLatin1CharIndexes.put("hyphen", Integer.valueOf(173));
		mISOLatin1CharIndexes.put("iacute", Integer.valueOf(237));
		mISOLatin1CharIndexes.put("icircumflex", Integer.valueOf(238));
		mISOLatin1CharIndexes.put("idieresis", Integer.valueOf(239));
		mISOLatin1CharIndexes.put("igrave", Integer.valueOf(236));
		mISOLatin1CharIndexes.put("logicalnot", Integer.valueOf(172));
		mISOLatin1CharIndexes.put("macron", Integer.valueOf(175));
		mISOLatin1CharIndexes.put("mu", Integer.valueOf(0));
		mISOLatin1CharIndexes.put("multiply", Integer.valueOf(215));
		mISOLatin1CharIndexes.put("ntilde", Integer.valueOf(241));
		mISOLatin1CharIndexes.put("oacute", Integer.valueOf(243));
		mISOLatin1CharIndexes.put("ocircumflex", Integer.valueOf(244));
		mISOLatin1CharIndexes.put("odieresis", Integer.valueOf(246));
		mISOLatin1CharIndexes.put("ogonek", Integer.valueOf(158));
		mISOLatin1CharIndexes.put("ograve", Integer.valueOf(242));
		mISOLatin1CharIndexes.put("onehalf", Integer.valueOf(189));
		mISOLatin1CharIndexes.put("onequarter", Integer.valueOf(188));
		mISOLatin1CharIndexes.put("onesuperior", Integer.valueOf(185));
		mISOLatin1CharIndexes.put("ordfeminine", Integer.valueOf(170));
		mISOLatin1CharIndexes.put("ordmasculine", Integer.valueOf(186));
		mISOLatin1CharIndexes.put("oslash", Integer.valueOf(248));
		mISOLatin1CharIndexes.put("otilde", Integer.valueOf(245));
		mISOLatin1CharIndexes.put("paragraph", Integer.valueOf(182));
		mISOLatin1CharIndexes.put("periodcentered", Integer.valueOf(183));
		mISOLatin1CharIndexes.put("plusminus", Integer.valueOf(177));
		mISOLatin1CharIndexes.put("questiondown", Integer.valueOf(191));
		mISOLatin1CharIndexes.put("registered", Integer.valueOf(174));
		mISOLatin1CharIndexes.put("ring", Integer.valueOf(154));
		mISOLatin1CharIndexes.put("section", Integer.valueOf(167));
		mISOLatin1CharIndexes.put("sterling", Integer.valueOf(163));
		mISOLatin1CharIndexes.put("thorn", Integer.valueOf(254));
		mISOLatin1CharIndexes.put("threequarters", Integer.valueOf(190));
		mISOLatin1CharIndexes.put("threesuperior", Integer.valueOf(179));
		mISOLatin1CharIndexes.put("tilde", Integer.valueOf(148));
		mISOLatin1CharIndexes.put("twosuperior", Integer.valueOf(178));
		mISOLatin1CharIndexes.put("uacute", Integer.valueOf(250));
		mISOLatin1CharIndexes.put("ucircumflex", Integer.valueOf(251));
		mISOLatin1CharIndexes.put("udieresis", Integer.valueOf(252));
		mISOLatin1CharIndexes.put("ugrave", Integer.valueOf(249));
		mISOLatin1CharIndexes.put("yacute", Integer.valueOf(253));
		mISOLatin1CharIndexes.put("ydieresis", Integer.valueOf(255));
		mISOLatin1CharIndexes.put("yen", Integer.valueOf(165));
	}

	/**
	 * Create Adobe Font Metrics for a font by reading an AFM file.
	 * @param r file to read from.
	 * @param afmFilename filename being read (for any error message).
	 * @param ISOLatin1EncodedFonts names of fonts for which to use ISO Latin1 encoding.
	 */	
	public AdobeFontMetrics (BufferedReader r, String afmFilename,
		HashSet<String> ISOLatin1EncodedFonts) throws IOException, MapyrusException
	{
		String line;
		boolean inCharMetrics = false;
		boolean finishedParsing = false;
		StringTokenizer st;
		boolean convertToISOLatin1 = false;

		// TODO handle fonts with more than 256 characters.
		mCharWidths = new short[256];
		mIsFixedPitch = false;
		mFirstChar = Integer.MAX_VALUE;
		mLastChar = Integer.MIN_VALUE;
		mFlags = 32; /* Nonsymbolic font for PDF */

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
					st = new StringTokenizer(line, " ;");
					if (st.countTokens() < 6)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_AFM_FILE) +
							": " + afmFilename + ": " + line);
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
						Integer i = mISOLatin1CharIndexes.get(charName);
						if (i != null)
							charIndex = i.intValue();
					}

					if (charIndex >= 0 && charIndex < mCharWidths.length)
					{
						mCharWidths[charIndex] = charWidth;
						if (charIndex < mFirstChar)
							mFirstChar = charIndex;
						if (charIndex > mLastChar)
							mLastChar = charIndex;
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
					mFontName = st.nextToken();
					convertToISOLatin1 = ISOLatin1EncodedFonts.contains(mFontName);
				}
				else if (line.startsWith("IsFixedPitch") && line.toLowerCase().indexOf("true") >= 0)
				{
					mIsFixedPitch = true;
					mFlags |= 1;
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
					mItalicAngle = (int)Math.round(Double.parseDouble(st.nextToken()));
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
					mCapHeight = Integer.parseInt(st.nextToken());
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
					mAscender = Integer.parseInt(st.nextToken());
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
					mDescender = Integer.parseInt(st.nextToken());
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
					mFontBBox = new Rectangle(x1, y1, x2 - x1, y2 - y1);
				}
				else if (line.startsWith("Weight"))
				{
					st = new StringTokenizer(line);
					while(st.hasMoreTokens())
					{
						String token = st.nextToken();
						if (token.equalsIgnoreCase("italic"))
							mFlags |= 64;
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
		return(mFontName);
	}

	/**
	 * Returns index of first character in font.
	 * @return index.
	 */
	public int getFirstChar()
	{
		return(mFirstChar);
	}

	/**
	 * Returns index of last character in font.
	 * @return index.
	 */
	public int getLastChar()
	{
		return(mLastChar);
	}

	/**
	 * Get width of character.
	 * @param index index of character.
	 * @return width of this character.
	 */
	public int getCharWidth(int index)
	{
		return(mCharWidths[index]);
	}

	/**
	 * Returns italic angle of font.
	 * @return italic angle.
	 */
	public int getItalicAngle()
	{
		return(mItalicAngle);
	}

	/**
	 * Returns height of capital letters in font.
	 * @return cap height.
	 */
	public int getCapHeight()
	{
		return(mCapHeight);
	}

	/**
	 * Returns bounding box of font.
	 * @return font bounding box.
	 */
	public Rectangle getFontBBox()
	{
		return(mFontBBox);
	}

	/**
	 * Returns font type as PDF bit flags.
	 * @return font bit flags.
	 */
	public int getFlags()
	{
		return(mFlags);
	}
	
	/**
	 * Returns maximum height of font above baseline.
	 * @return ascender height.
	 */
	public int getAscender()
	{
		return(mAscender);
	}

	/**
	 * Returns maximum height of font below baseline.
	 * @return descender height.
	 */
	public int getDescender()
	{
		return(mDescender);
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
	public double getStringWidth(String s, double pointSize)
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
			pointLen = s.length() * ((double)mCharWidths[spaceIndex] / FULL_CHAR_WIDTH) * pointSize;
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
					total += FULL_CHAR_WIDTH;
			}
			pointLen = (double)total / FULL_CHAR_WIDTH * pointSize;
		}
		return(pointLen);
	}
}
