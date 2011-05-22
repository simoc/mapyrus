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
	 * @param mISOLatin1EncodedFonts list of fonts being used with ISOLatin1Encoding.
	 */
	public AdobeFontMetricsManager(List<String> afmFilenames,
		HashSet<String> ISOLatin1EncodedFonts)
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
				AdobeFontMetrics afm = new AdobeFontMetrics(r, res, ISOLatin1EncodedFonts);
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
				AdobeFontMetrics afm = new AdobeFontMetrics(r, filename, ISOLatin1EncodedFonts);
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

	/**
	 * Get character encoding definition for ISO Latin2 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getISOLatin2Encoding()
	{
		return "32 /nbspace\n" +
			"130 /quotesinglbase\n" +
			"132 /quotedblbase\n" +
			"133 /ellipsis\n" +
			"134 /dagger\n" +
			"135 /daggerdbl\n" +
			"137 /perthousand\n" +
			"138 /Scaron\n" +
			"139 /guilsinglleft\n" +
			"140 /Sacute\n" +
			"141 /Tcaron\n" +
			"142 /Zcaron\n" +
			"143 /Zacute\n" +
			"145 /quotesinglleft\n" +
			"146 /quotesinglright\n" +
			"147 /quotedblleft\n" +
			"148 /quotedblright\n" +
			"149 /bullet\n" +
			"150 /endash\n" +
			"151 /emdash\n" +
			"153 /trademark\n" +
			"154 /scaron\n" +
			"155 /guilsinglright\n" +
			"156 /sacute\n" +
			"157 /tcaron\n" +
			"158 /zcaron\n" +
			"159 /zacute\n" +
			"160 /nbspace\n" +
			"161 /caron\n" +
			"162 /breve\n" +
			"163 /Lslash\n" +
			"164 /currency\n" +
			"165 /Aogonek\n" +
			"166 /brokenbar\n" +
			"167 /section\n" +
			"168 /dieresis\n" +
			"169 /copyright\n" +
			"170 /Scedilla\n" +
			"171 /guillemotleft\n" +
			"172 /notsign\n" +
			"173 /hyphen\n" +
			"174 /registered\n" +
			"175 /Zdotaccent\n" +
			"176 /degree\n" +
			"177 /plusminus\n" +
			"178 /ogonek\n" +
			"179 /lslash\n" +
			"180 /acute\n" +
			"181 /mu\n" +
			"182 /paragraph\n" +
			"183 /periodcentered\n" +
			"184 /cedilla\n" +
			"185 /aogonek\n" +
			"186 /scedilla\n" +
			"187 /guillemotright\n" +
			"188 /Lcaron\n" +
			"189 /hungarumlaut\n" +
			"190 /lcaron\n" +
			"191 /zdotaccent\n" +
			"192 /Racute\n" +
			"193 /Aacute\n" +
			"194 /Acircumflex\n" +
			"195 /Abreve\n" +
			"196 /Adieresis\n" +
			"197 /Lacute\n" +
			"198 /Cacute\n" +
			"199 /Ccedilla\n" +
			"200 /Ccaron\n" +
			"201 /Eacute\n" +
			"202 /Eogonek\n" +
			"203 /Edieresis\n" +
			"204 /Ecaron\n" +
			"205 /Iacute\n" +
			"206 /Icircumflex\n" +
			"207 /Dcaron\n" +
			"208 /Eth\n" +
			"209 /Nacute\n" +
			"210 /Ncaron\n" +
			"211 /Oacute\n" +
			"212 /Ocircumflex\n" +
			"213 /Ohungarumlaut\n" +
			"214 /Odieresis\n" +
			"215 /multiply\n" +
			"216 /Rcaron\n" +
			"217 /Uring\n" +
			"218 /Uacute\n" +
			"219 /Uhungarumlaut\n" +
			"220 /Udieresis\n" +
			"221 /Yacute\n" +
			"222 /Tcedilla\n" +
			"223 /germandbls\n" +
			"224 /racute\n" +
			"225 /aacute\n" +
			"226 /acircumflex\n" +
			"227 /abreve\n" +
			"228 /adieresis\n" +
			"229 /lacute\n" +
			"230 /cacute\n" +
			"231 /ccedilla\n" +
			"232 /ccaron\n" +
			"233 /eacute\n" +
			"234 /eogonek\n" +
			"235 /edieresis\n" +
			"236 /ecaron\n" +
			"237 /iacute\n" +
			"238 /icircumflex\n" +
			"239 /dcaron\n" +
			"240 /eth\n" +
			"241 /nacute\n" +
			"242 /ncaron\n" +
			"243 /oacute\n" +
			"244 /ocircumflex\n" +
			"245 /ohungarumlaut\n" +
			"246 /odieresis\n" +
			"247 /divide\n" +
			"248 /rcaron\n" +
			"249 /uring\n" +
			"250 /uacute\n" +
			"251 /uhungarumlaut\n" +
			"252 /udieresis\n" +
			"253 /yacute\n" +
			"254 /tcedilla\n" +
			"255 /dotaccent\n";
	}
}
