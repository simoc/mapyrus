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
package org.mapyrus.font;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
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
	private static String []mStdFontMetricResources;

	static
	{
		mStdFontMetricResources = new String []
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
	private HashMap mFontMetrics;

	/**
	 * Create font metrics information for PostScript fonts.
	 * @param afmFilenames names of user-provided .afm file.
	 * @param mISOLatin1EncodedFonts list of fonts being used with ISOLatin1Encoding.
	 */
	public AdobeFontMetricsManager(List afmFilenames, HashSet ISOLatin1EncodedFonts)
		throws IOException, MapyrusException
	{
		BufferedReader r;

		mFontMetrics = new HashMap();

		/*
		 * Load font metrics information for standard PostScript fonts
		 * from .afm files included as resources.
		 */
		for (int i = 0; i < mStdFontMetricResources.length; i++)
		{
			String res = "org/mapyrus/font/" + mStdFontMetricResources[i];
			InputStream inStream =
				this.getClass().getClassLoader().getResourceAsStream(res);
			r = new BufferedReader(new InputStreamReader(inStream));

			AdobeFontMetrics afm = new AdobeFontMetrics(r, res, ISOLatin1EncodedFonts);
			mFontMetrics.put(afm.getFontName(), afm);
			r.close();
		}
		
		/*
		 * Load font metrics information from .afm files provided by caller.
		 */
		Iterator it = afmFilenames.iterator();
		while (it.hasNext())
		{
			String filename = (String)it.next();
			r = new BufferedReader(new FileReader(filename));
			
			AdobeFontMetrics afm = new AdobeFontMetrics(r, filename, ISOLatin1EncodedFonts);
			mFontMetrics.put(afm.getFontName(), afm);
			r.close();
		}
	}

	/**
	 * Calculate the displayed width of a string.
	 * @param fontName font for display.
	 * @param pointSize point size in which string is displayed.
	 * @param s string to calculate width for.
	 * @return width of string in points. 
	 */
	public int getStringWidth(String fontName, int pointSize, String s)
	{
		int retval = 0;

		AdobeFontMetrics afm = (AdobeFontMetrics)mFontMetrics.get(fontName);
		if (afm != null)
		{
			retval = afm.getStringWidth(s, pointSize);
		}
		else
		{
			/*
			 * No Font Metric information given for this font.  Just
			 * calculate approximate length assuming fixed with characters.
			 */
			retval = s.length() * pointSize;
		}

		return(retval);
	}
}
