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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.mapyrus.MapyrusException;

/*
 * Font metrics for all PostScript fonts, read from Adobe Font Metrics (AFM) files.
 * Provides methods to find the exact displayed length of a string for any known font.
 */
public class AdobeFontMetricsManager
{
	/*
	 * List of AFM files included as java resources
	 * for the 35 standard PostScript fonts.
	 */
	private static String []m_stdFontMetricResources;

	static
	{
		m_stdFontMetricResources = new String []
		{
			"agd.afm",
			"agdo.afm",
			"agw.afm",
			"agwo.afm",
			"bkd.afm",
			"bkdi.afm",
			"bkl.afm",
			"bkli.afm",
			"cob.afm",
			"cobo.afm",
			"com.afm",
			"coo.afm",
			"hv.afm",
			"hvb.afm",
			"hvbo.afm",
			"hvn.afm",
			"hvnb.afm",
			"hvnbo.afm",
			"hvno.afm",
			"hvo.afm",
			"ncb.afm",
			"ncbi.afm",
			"nci.afm",
			"ncr.afm",
			"pob.afm",
			"pobi.afm",
			"poi.afm",
			"por.afm",
			"sy.afm",
			"tib.afm",
			"tibi.afm",
			"tii.afm",
			"tir.afm",
			"zcmi.afm",
			"zd.afm"
		};
	}

	/*
	 * Font metrics for all fonts.
	 */
	private HashMap<String, AdobeFontMetrics> m_fontMetrics;

	/**
	 * Create font metrics information for PostScript fonts.
	 * @param afmFilenames names of user-provided .afm file.
	 */
	public AdobeFontMetricsManager(List<String> afmFilenames)
		throws IOException, MapyrusException
	{
		m_fontMetrics = new HashMap<String, AdobeFontMetrics>();

		/*
		 * Load font metrics information for standard PostScript fonts
		 * from .afm files included as resources.
		 */
		for (int i = 0; i < m_stdFontMetricResources.length; i++)
		{
			String res = "org/mapyrus/font/" + m_stdFontMetricResources[i];
			InputStream inStream =
				this.getClass().getClassLoader().getResourceAsStream(res);

			BufferedReader r = null;
			try
			{
				r = new BufferedReader(new InputStreamReader(inStream));
				AdobeFontMetrics afm = new AdobeFontMetrics(r, res);
				m_fontMetrics.put(afm.getFontName(), afm);
			}
			finally
			{
				if (r != null)
					r.close();
			}
		}

		/*
		 * Load font metrics information from .afm files provided by caller.
		 */
		Iterator<String> it = afmFilenames.iterator();
		while (it.hasNext())
		{
			String filename = it.next();
			BufferedReader r = null;

			try
			{
				r = new BufferedReader(new FileReader(filename));
				AdobeFontMetrics afm = new AdobeFontMetrics(r, filename);
				m_fontMetrics.put(afm.getFontName(), afm);
			}
			finally
			{
				if (r != null)
					r.close();
			}
		}
	}

	/**
	 * Calculate the displayed size of a string.
	 * @param fontName font for display.
	 * @param pointSize point size in which string is displayed.
	 * @param s string to calculate size for.
	 * @return size of string in points.
	 */
	public StringDimension getStringDimension(String fontName, double pointSize, String s)
	{
		StringDimension retval = new StringDimension();

		AdobeFontMetrics afm = m_fontMetrics.get(fontName);
		if (afm != null)
		{
			retval = afm.getStringDimension(s, pointSize);
		}
		else
		{
			/*
			 * No Font Metric information given for this font.  Just
			 * calculate approximate length assuming fixed with characters.
			 */
			double width = s.length() * pointSize;
			retval.setSize(width, pointSize, pointSize, 0);
		}

		return(retval);
	}

	/**
	 * Get encoded character in PostScript font.
	 * @param fontName name of PostScript font.
	 * @param c character (in Unicode) to encode.
	 * @return character from PostScript font.
	 */
	public char getEncodedChar(String fontName, char c)
	{
		char retval;
		AdobeFontMetrics afm = m_fontMetrics.get(fontName);

		if (afm != null)
			retval = afm.getEncodedChar(c);
		else
			retval = c;
		return retval;
	}
}
