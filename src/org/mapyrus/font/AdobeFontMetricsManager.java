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
		HashSet<String> ISOLatin1EncodedFonts, String glyphFilename)
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
				AdobeFontMetrics afm = new AdobeFontMetrics(r, res, ISOLatin1EncodedFonts, glyphFilename);
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
				AdobeFontMetrics afm = new AdobeFontMetrics(r, filename, ISOLatin1EncodedFonts, glyphFilename);
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

	/**
	 * Get character encoding definition for ISO Latin9 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getISOLatin9Encoding()
	{
		return "161 /exclamdown\n" +
			"162 /cent\n" +
			"163 /sterling\n" +
			"164 /Euro\n" +
			"165 /yen\n" +
			"166 /Scaron\n" +
			"167 /section\n" +
			"168 /scaron\n" +
			"169 /copyright\n" +
			"170 /ordfeminine\n" +
			"171 /guillemotleft\n" +
			"172 /logicalnot\n" +
			"173 /hyphen\n" +
			"174 /registered\n" +
			"175 /macron\n" +
			"176 /degree\n" +
			"177 /plusminus\n" +
			"178 /twosuperior\n" +
			"179 /threesuperior\n" +
			"180 /Zcaron\n" +
			"181 /mu\n" +
			"182 /paragraph\n" +
			"183 /periodcentered\n" +
			"184 /zcaron\n" +
			"185 /onesuperior\n" +
			"186 /ordmasculine\n" +
			"187 /guillemotright\n" +
			"188 /OE\n" +
			"189 /oe\n" +
			"190 /Ydieresis\n" +
			"191 /questiondown\n" +
			"192 /Agrave\n" +
			"193 /Aacute\n" +
			"194 /Acircumflex\n" +
			"195 /Atilde\n" +
			"196 /Adieresis\n" +
			"197 /Aring\n" +
			"198 /AE\n" +
			"199 /Ccedilla\n" +
			"200 /Egrave\n" +
			"201 /Eacute\n" +
			"202 /Ecircumflex\n" +
			"203 /Edieresis\n" +
			"204 /Igrave\n" +
			"205 /Iacute\n" +
			"206 /Icircumflex\n" +
			"207 /Idieresis\n" +
			"208 /Eth\n" +
			"209 /Ntilde\n" +
			"210 /Ograve\n" +
			"211 /Oacute\n" +
			"212 /Ocircumflex\n" +
			"213 /Otilde\n" +
			"214 /Odieresis\n" +
			"215 /multiply\n" +
			"216 /Oslash\n" +
			"217 /Ugrave\n" +
			"218 /Uacute\n" +
			"219 /Ucircumflex\n" +
			"220 /Udieresis\n" +
			"221 /Yacute\n" +
			"222 /Thorn\n" +
			"223 /germandbls\n" +
			"224 /agrave\n" +
			"225 /aacute\n" +
			"226 /acircumflex\n" +
			"227 /atilde\n" +
			"228 /adieresis\n" +
			"229 /aring\n" +
			"230 /ae\n" +
			"231 /ccedilla\n" +
			"232 /egrave\n" +
			"233 /eacute\n" +
			"234 /ecircumflex\n" +
			"235 /edieresis\n" +
			"236 /igrave\n" +
			"237 /iacute\n" +
			"238 /icircumflex\n" +
			"239 /idieresis\n" +
			"240 /eth\n" +
			"241 /ntilde\n" +
			"242 /ograve\n" +
			"243 /oacute\n" +
			"244 /ocircumflex\n" +
			"245 /otilde\n" +
			"246 /odieresis\n" +
			"247 /divide\n" +
			"248 /oslash\n" +
			"249 /ugrave\n" +
			"250 /uacute\n" +
			"251 /ucircumflex\n" +
			"252 /udieresis\n" +
			"253 /yacute\n" +
			"254 /thorn\n" +
			"255 /ydieresis\n";
	}

	/**
	 * Get character encoding definition for ISO Latin10 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getISOLatin10Encoding()
	{
		return "161 /Aogonek\n" +
			"162 /aogonek\n" +
			"163 /Lslash\n" +
			"164 /Euro\n" +
			"165 /quotedblbase\n" +
			"166 /Scaron\n" +
			"167 /section\n" +
			"168 /scaron\n" +
			"169 /copyright\n" +
			"170 /Scommaaccent\n" +
			"171 /guillemotleft\n" +
			"172 /Zacute\n" +
			"173 /hyphen\n" +
			"174 /zacute\n" +
			"175 /Zdotaccent\n" +
			"176 /degree\n" +
			"177 /plusminus\n" +
			"178 /Ccaron\n" +
			"179 /lslash\n" +
			"180 /Zcaron\n" +
			"181 /quotedblright\n" +
			"182 /paragraph\n" +
			"183 /periodcentered\n" +
			"184 /zcaron\n" +
			"185 /ccaron\n" +
			"186 /scommaaccent\n" +
			"187 /guillemotright\n" +
			"188 /OE\n" +
			"189 /oe\n" +
			"190 /Ydieresis\n" +
			"191 /zdotaccent\n" +
			"192 /Agrave\n" +
			"193 /Aacute\n" +
			"194 /Acircumflex\n" +
			"195 /Abreve\n" +
			"196 /Adieresis\n" +
			"197 /Cacute\n" +
			"198 /AE\n" +
			"199 /Ccedilla\n" +
			"200 /Egrave\n" +
			"201 /Eacute\n" +
			"202 /Ecircumflex\n" +
			"203 /Edieresis\n" +
			"204 /Igrave\n" +
			"205 /Iacute\n" +
			"206 /Icircumflex\n" +
			"207 /Idieresis\n" +
			"208 /Dcroat\n" +
			"209 /Nacute\n" +
			"210 /Ograve\n" +
			"211 /Oacute\n" +
			"212 /Ocircumflex\n" +
			"213 /Ohungarumlaut\n" +
			"214 /Odieresis\n" +
			"215 /Sacute\n" +
			"216 /Uhungarumlaut\n" +
			"217 /Ugrave\n" +
			"218 /Uacute\n" +
			"219 /Ucircumflex\n" +
			"220 /Udieresis\n" +
			"221 /Eogonek\n" +
			"222 /Tcommaaccent\n" +
			"223 /germandbls\n" +
			"224 /agrave\n" +
			"225 /aacute\n" +
			"226 /acircumflex\n" +
			"227 /abreve\n" +
			"228 /adieresis\n" +
			"229 /cacute\n" +
			"230 /ae\n" +
			"231 /ccedilla\n" +
			"232 /egrave\n" +
			"233 /eacute\n" +
			"234 /ecircumflex\n" +
			"235 /edieresis\n" +
			"236 /igrave\n" +
			"237 /iacute\n" +
			"238 /icircumflex\n" +
			"239 /idieresis\n" +
			"240 /dcroat\n" +
			"241 /nacute\n" +
			"242 /ograve\n" +
			"243 /oacute\n" +
			"244 /ocircumflex\n" +
			"245 /ohungarumlaut\n" +
			"246 /odieresis\n" +
			"247 /sacute\n" +
			"248 /uhungarumlaut\n" +
			"249 /ugrave\n" +
			"250 /uacute\n" +
			"251 /ucircumflex\n" +
			"252 /udieresis\n" +
			"253 /eogonek\n" +
			"254 /tcommaaccent\n" +
			"255 /ydieresis\n";
	}

	/**
	 * Get character encoding definition for Windows 1250 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getWindows1250Encoding()
	{
		return "128 /Euro\n" +
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
			"145 /quoteleft\n" +
			"146 /quoteright\n" +
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
			"160 /space\n" +
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
			"172 /logicalnot\n" +
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
			"208 /Dcroat\n" +
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
			"222 /Tcommaaccent\n" +
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
			"240 /dcroat\n" +
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
			"254 /tcommaaccent\n" +
			"255 /dotaccent\n";
	}

	/**
	 * Get character encoding definition for Windows 1251 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getWindows1251Encoding()
	{
		return "128 /afii10051\n" +
			"129 /afii10052\n" +
			"130 /quotesinglbase\n" +
			"131 /afii10100\n" +
			"132 /quotedblbase\n" +
			"133 /ellipsis\n" +
			"134 /dagger\n" +
			"135 /daggerdbl\n" +
			"136 /Euro\n" +
			"137 /perthousand\n" +
			"138 /afii10058\n" +
			"139 /guilsinglleft\n" +
			"140 /afii10059\n" +
			"141 /afii10061\n" +
			"142 /afii10060\n" +
			"143 /afii10145\n" +
			"144 /afii10099\n" +
			"145 /quoteleft\n" +
			"146 /quoteright\n" +
			"147 /quotedblleft\n" +
			"148 /quotedblright\n" +
			"149 /bullet\n" +
			"150 /endash\n" +
			"151 /emdash\n" +
			"153 /trademark\n" +
			"154 /afii10106\n" +
			"155 /guilsinglright\n" +
			"156 /afii10107\n" +
			"157 /afii10109\n" +
			"158 /afii10108\n" +
			"159 /afii10193\n" +
			"160 /space\n" +
			"161 /afii10062\n" +
			"162 /afii10110\n" +
			"163 /afii10057\n" +
			"164 /currency\n" +
			"165 /afii10050\n" +
			"166 /brokenbar\n" +
			"167 /section\n" +
			"168 /afii10023\n" +
			"169 /copyright\n" +
			"170 /afii10053\n" +
			"171 /guillemotleft\n" +
			"172 /logicalnot\n" +
			"173 /hyphen\n" +
			"174 /registered\n" +
			"175 /afii10056\n" +
			"176 /degree\n" +
			"177 /plusminus\n" +
			"178 /afii10055\n" +
			"179 /afii10103\n" +
			"180 /afii10098\n" +
			"181 /mu\n" +
			"182 /paragraph\n" +
			"183 /periodcentered\n" +
			"184 /afii10071\n" +
			"185 /afii61352\n" +
			"186 /afii10101\n" +
			"187 /guillemotright\n" +
			"188 /afii10105\n" +
			"189 /afii10054\n" +
			"190 /afii10102\n" +
			"191 /afii10104\n" +
			"192 /afii10017\n" +
			"193 /afii10018\n" +
			"194 /afii10019\n" +
			"195 /afii10020\n" +
			"196 /afii10021\n" +
			"197 /afii10022\n" +
			"198 /afii10024\n" +
			"199 /afii10025\n" +
			"200 /afii10026\n" +
			"201 /afii10027\n" +
			"202 /afii10028\n" +
			"203 /afii10029\n" +
			"204 /afii10030\n" +
			"205 /afii10031\n" +
			"206 /afii10032\n" +
			"207 /afii10033\n" +
			"208 /afii10034\n" +
			"209 /afii10035\n" +
			"210 /afii10036\n" +
			"211 /afii10037\n" +
			"212 /afii10038\n" +
			"213 /afii10039\n" +
			"214 /afii10040\n" +
			"215 /afii10041\n" +
			"216 /afii10042\n" +
			"217 /afii10043\n" +
			"218 /afii10044\n" +
			"219 /afii10045\n" +
			"220 /afii10046\n" +
			"221 /afii10047\n" +
			"222 /afii10048\n" +
			"223 /afii10049\n" +
			"224 /afii10065\n" +
			"225 /afii10066\n" +
			"226 /afii10067\n" +
			"227 /afii10068\n" +
			"228 /afii10069\n" +
			"229 /afii10070\n" +
			"230 /afii10072\n" +
			"231 /afii10073\n" +
			"232 /afii10074\n" +
			"233 /afii10075\n" +
			"234 /afii10076\n" +
			"235 /afii10077\n" +
			"236 /afii10078\n" +
			"237 /afii10079\n" +
			"238 /afii10080\n" +
			"239 /afii10081\n" +
			"240 /afii10082\n" +
			"241 /afii10083\n" +
			"242 /afii10084\n" +
			"243 /afii10085\n" +
			"244 /afii10086\n" +
			"245 /afii10087\n" +
			"246 /afii10088\n" +
			"247 /afii10089\n" +
			"248 /afii10090\n" +
			"249 /afii10091\n" +
			"250 /afii10092\n" +
			"251 /afii10093\n" +
			"252 /afii10094\n" +
			"253 /afii10095\n" +
			"254 /afii10096\n" +
			"255 /afii10097\n";
	}
	
	/**
	 * Get character encoding definition for Windows 1252 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getWindows1252Encoding()
	{
		return "128 /Euro\n" +
			"130 /quotesinglbase\n" +
			"131 /florin\n" +
			"132 /quotedblbase\n" +
			"133 /ellipsis\n" +
			"134 /dagger\n" +
			"135 /daggerdbl\n" +
			"136 /circumflex\n" +
			"137 /perthousand\n" +
			"138 /Scaron\n" +
			"139 /guilsinglleft\n" +
			"140 /OE\n" +
			"142 /Zcaron\n" +
			"145 /quoteleft\n" +
			"146 /quoteright\n" +
			"147 /quotedblleft\n" +
			"148 /quotedblright\n" +
			"149 /bullet\n" +
			"150 /endash\n" +
			"151 /emdash\n" +
			"152 /tilde\n" +
			"153 /trademark\n" +
			"154 /scaron\n" +
			"155 /guilsinglright\n" +
			"156 /oe\n" +
			"158 /zcaron\n" +
			"159 /Ydieresis\n" +
			"160 /space\n" +
			"161 /exclamdown\n" +
			"162 /cent\n" +
			"163 /sterling\n" +
			"164 /currency\n" +
			"165 /yen\n" +
			"166 /brokenbar\n" +
			"167 /section\n" +
			"168 /dieresis\n" +
			"169 /copyright\n" +
			"170 /ordfeminine\n" +
			"171 /guillemotleft\n" +
			"172 /logicalnot\n" +
			"173 /hyphen\n" +
			"174 /registered\n" +
			"175 /macron\n" +
			"176 /degree\n" +
			"177 /plusminus\n" +
			"178 /twosuperior\n" +
			"179 /threesuperior\n" +
			"180 /acute\n" +
			"181 /mu\n" +
			"182 /paragraph\n" +
			"183 /periodcentered\n" +
			"184 /cedilla\n" +
			"185 /onesuperior\n" +
			"186 /ordmasculine\n" +
			"187 /guillemotright\n" +
			"188 /onequarter\n" +
			"189 /onehalf\n" +
			"190 /threequarters\n" +
			"191 /questiondown\n" +
			"192 /Agrave\n" +
			"193 /Aacute\n" +
			"194 /Acircumflex\n" +
			"195 /Atilde\n" +
			"196 /Adieresis\n" +
			"197 /Aring\n" +
			"198 /AE\n" +
			"199 /Ccedilla\n" +
			"200 /Egrave\n" +
			"201 /Eacute\n" +
			"202 /Ecircumflex\n" +
			"203 /Edieresis\n" +
			"204 /Igrave\n" +
			"205 /Iacute\n" +
			"206 /Icircumflex\n" +
			"207 /Idieresis\n" +
			"208 /Eth\n" +
			"209 /Ntilde\n" +
			"210 /Ograve\n" +
			"211 /Oacute\n" +
			"212 /Ocircumflex\n" +
			"213 /Otilde\n" +
			"214 /Odieresis\n" +
			"215 /multiply\n" +
			"216 /Oslash\n" +
			"217 /Ugrave\n" +
			"218 /Uacute\n" +
			"219 /Ucircumflex\n" +
			"220 /Udieresis\n" +
			"221 /Yacute\n" +
			"222 /Thorn\n" +
			"223 /germandbls\n" +
			"224 /agrave\n" +
			"225 /aacute\n" +
			"226 /acircumflex\n" +
			"227 /atilde\n" +
			"228 /adieresis\n" +
			"229 /aring\n" +
			"230 /ae\n" +
			"231 /ccedilla\n" +
			"232 /egrave\n" +
			"233 /eacute\n" +
			"234 /ecircumflex\n" +
			"235 /edieresis\n" +
			"236 /igrave\n" +
			"237 /iacute\n" +
			"238 /icircumflex\n" +
			"239 /idieresis\n" +
			"240 /eth\n" +
			"241 /ntilde\n" +
			"242 /ograve\n" +
			"243 /oacute\n" +
			"244 /ocircumflex\n" +
			"245 /otilde\n" +
			"246 /odieresis\n" +
			"247 /divide\n" +
			"248 /oslash\n" +
			"249 /ugrave\n" +
			"250 /uacute\n" +
			"251 /ucircumflex\n" +
			"252 /udieresis\n" +
			"253 /yacute\n" +
			"254 /thorn\n" +
			"255 /ydieresis\n";
	}
	
	/**
	 * Get character encoding definition for Windows 1253 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getWindows1253Encoding()
	{
		return "128 /Euro\n" +
		"130 /quotesinglbase\n" +
		"131 /florin\n" +
		"132 /quotedblbase\n" +
		"133 /ellipsis\n" +
		"134 /dagger\n" +
		"135 /daggerdbl\n" +
		"137 /perthousand\n" +
		"139 /guilsinglleft\n" +
		"145 /quoteleft\n" +
		"146 /quoteright\n" +
		"147 /quotedblleft\n" +
		"148 /quotedblright\n" +
		"149 /bullet\n" +
		"150 /endash\n" +
		"151 /emdash\n" +
		"153 /trademark\n" +
		"155 /guilsinglright\n" +
		"160 /space\n" +
		"161 /dieresistonos\n" +
		"162 /Alphatonos\n" +
		"163 /sterling\n" +
		"164 /currency\n" +
		"165 /yen\n" +
		"166 /brokenbar\n" +
		"167 /section\n" +
		"168 /dieresis\n" +
		"169 /copyright\n" +
		"171 /guillemotleft\n" +
		"172 /logicalnot\n" +
		"173 /hyphen\n" +
		"174 /registered\n" +
		"175 /afii00208\n" +
		"176 /degree\n" +
		"177 /plusminus\n" +
		"178 /twosuperior\n" +
		"179 /threesuperior\n" +
		"180 /tonos\n" +
		"181 /mu\n" +
		"182 /paragraph\n" +
		"183 /periodcentered\n" +
		"184 /Epsilontonos\n" +
		"185 /Etatonos\n" +
		"186 /Iotatonos\n" +
		"187 /guillemotright\n" +
		"188 /Omicrontonos\n" +
		"189 /onehalf\n" +
		"190 /Upsilontonos\n" +
		"191 /Omegatonos\n" +
		"192 /iotadieresistonos\n" +
		"193 /Alpha\n" +
		"194 /Beta\n" +
		"195 /Gamma\n" +
		"196 /Delta\n" +
		"197 /Epsilon\n" +
		"198 /Zeta\n" +
		"199 /Eta\n" +
		"200 /Theta\n" +
		"201 /Iota\n" +
		"202 /Kappa\n" +
		"203 /Lambda\n" +
		"204 /Mu\n" +
		"205 /Nu\n" +
		"206 /Xi\n" +
		"207 /Omicron\n" +
		"208 /Pi\n" +
		"209 /Rho\n" +
		"211 /Sigma\n" +
		"212 /Tau\n" +
		"213 /Upsilon\n" +
		"214 /Phi\n" +
		"215 /Chi\n" +
		"216 /Psi\n" +
		"217 /Omega\n" +
		"218 /Iotadieresis\n" +
		"219 /Upsilondieresis\n" +
		"220 /alphatonos\n" +
		"221 /epsilontonos\n" +
		"222 /etatonos\n" +
		"223 /iotatonos\n" +
		"224 /upsilondieresistonos\n" +
		"225 /alpha\n" +
		"226 /beta\n" +
		"227 /gamma\n" +
		"228 /delta\n" +
		"229 /epsilon\n" +
		"230 /zeta\n" +
		"231 /eta\n" +
		"232 /theta\n" +
		"233 /iota\n" +
		"234 /kappa\n" +
		"235 /lambda\n" +
		"236 /mu\n" +
		"237 /nu\n" +
		"238 /xi\n" +
		"239 /omicron\n" +
		"240 /pi\n" +
		"241 /rho\n" +
		"242 /sigma1\n" +
		"243 /sigma\n" +
		"244 /tau\n" +
		"245 /upsilon\n" +
		"246 /phi\n" +
		"247 /chi\n" +
		"248 /psi\n" +
		"249 /omega\n" +
		"250 /iotadieresis\n" +
		"251 /upsilondieresis\n" +
		"252 /omicrontonos\n" +
		"253 /upsilontonos\n" +
		"254 /omegatonos\n";
	}
	
	/**
	 * Get character encoding definition for Windows 1254 fonts in PostScript and PDF output.
	 * @return encoding string.
	 */
	public static String getWindows1254Encoding()
	{
		return "128 /Euro\n" +
		"130 /quotesinglbase\n" +
		"131 /florin\n" +
		"132 /quotedblbase\n" +
		"133 /ellipsis\n" +
		"134 /dagger\n" +
		"135 /daggerdbl\n" +
		"136 /circumflex\n" +
		"137 /perthousand\n" +
		"138 /Scaron\n" +
		"139 /guilsinglleft\n" +
		"140 /OE\n" +
		"145 /quoteleft\n" +
		"146 /quoteright\n" +
		"147 /quotedblleft\n" +
		"148 /quotedblright\n" +
		"149 /bullet\n" +
		"150 /endash\n" +
		"151 /emdash\n" +
		"152 /tilde\n" +
		"153 /trademark\n" +
		"154 /scaron\n" +
		"155 /guilsinglright\n" +
		"156 /oe\n" +
		"159 /Ydieresis\n" +
		"160 /space\n" +
		"161 /exclamdown\n" +
		"162 /cent\n" +
		"163 /sterling\n" +
		"164 /currency\n" +
		"165 /yen\n" +
		"166 /brokenbar\n" +
		"167 /section\n" +
		"168 /dieresis\n" +
		"169 /copyright\n" +
		"170 /ordfeminine\n" +
		"171 /guillemotleft\n" +
		"172 /logicalnot\n" +
		"173 /hyphen\n" +
		"174 /registered\n" +
		"175 /macron\n" +
		"176 /degree\n" +
		"177 /plusminus\n" +
		"178 /twosuperior\n" +
		"179 /threesuperior\n" +
		"180 /acute\n" +
		"181 /mu\n" +
		"182 /paragraph\n" +
		"183 /periodcentered\n" +
		"184 /cedilla\n" +
		"185 /onesuperior\n" +
		"186 /ordmasculine\n" +
		"187 /guillemotright\n" +
		"188 /onequarter\n" +
		"189 /onehalf\n" +
		"190 /threequarters\n" +
		"191 /questiondown\n" +
		"192 /Agrave\n" +
		"193 /Aacute\n" +
		"194 /Acircumflex\n" +
		"195 /Atilde\n" +
		"196 /Adieresis\n" +
		"197 /Aring\n" +
		"198 /AE\n" +
		"199 /Ccedilla\n" +
		"200 /Egrave\n" +
		"201 /Eacute\n" +
		"202 /Ecircumflex\n" +
		"203 /Edieresis\n" +
		"204 /Igrave\n" +
		"205 /Iacute\n" +
		"206 /Icircumflex\n" +
		"207 /Idieresis\n" +
		"208 /Gbreve\n" +
		"209 /Ntilde\n" +
		"210 /Ograve\n" +
		"211 /Oacute\n" +
		"212 /Ocircumflex\n" +
		"213 /Otilde\n" +
		"214 /Odieresis\n" +
		"215 /multiply\n" +
		"216 /Oslash\n" +
		"217 /Ugrave\n" +
		"218 /Uacute\n" +
		"219 /Ucircumflex\n" +
		"220 /Udieresis\n" +
		"221 /Idotaccent\n" +
		"222 /Scedilla\n" +
		"223 /germandbls\n" +
		"224 /agrave\n" +
		"225 /aacute\n" +
		"226 /acircumflex\n" +
		"227 /atilde\n" +
		"228 /adieresis\n" +
		"229 /aring\n" +
		"230 /ae\n" +
		"231 /ccedilla\n" +
		"232 /egrave\n" +
		"233 /eacute\n" +
		"234 /ecircumflex\n" +
		"235 /edieresis\n" +
		"236 /igrave\n" +
		"237 /iacute\n" +
		"238 /icircumflex\n" +
		"239 /idieresis\n" +
		"240 /gbreve\n" +
		"241 /ntilde\n" +
		"242 /ograve\n" +
		"243 /oacute\n" +
		"244 /ocircumflex\n" +
		"245 /otilde\n" +
		"246 /odieresis\n" +
		"247 /divide\n" +
		"248 /oslash\n" +
		"249 /ugrave\n" +
		"250 /uacute\n" +
		"251 /ucircumflex\n" +
		"252 /udieresis\n" +
		"253 /dotlessi\n" +
		"254 /scedilla\n" +
		"255 /ydieresis\n";
	}
}
