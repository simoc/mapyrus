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
package au.id.chenery.mapyrus;
 
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * PostScript font definitions.
 * Provides lookup table, returning a PostScript font definitions for a named font.
 * Font definitions are read from .pfa files in search path given as system property.
 */
public class PostScriptFontDatabase
{
	/*
	 * Font name to font filename lookup table.
	 */
	static private HashMap mFontFiles;

	/**
	 * Add .pfa font files found in directory to lookup table
	 * @param dir directory to search.
	 */	
	private static void searchDirectory(String dir) throws IOException
	{
		File f = new File(dir);
		if (f.isDirectory())
		{
			/*
			 * Search all files ending with .pfa 
			 */
			File []entries = f.listFiles();
			if (entries != null)
			{
				for (int i = 0; i < entries.length; i++)
				{
					File entry = entries[i];
					
					/*
					 * Skip directories and files with wrong suffix.
					 */
					if (!entry.toString().endsWith(".pfa"))
						continue;
					if (!entry.isFile())
						continue;

					try
					{
						BufferedReader bufferedReader = new BufferedReader(new FileReader(entry));

						/*
						 * First line of file contains font name.  For example,
						 * %!PS-AdobeFont-1.0: LuxiSerif 1.1000
						 */
						String firstLine = bufferedReader.readLine();
						if (firstLine != null)
						{
							StringTokenizer st = new StringTokenizer(firstLine);
							if (st.countTokens() > 1)
							{
								/*
								 * Add font name and file to lookup table.
								 * But don't add fonts we've already found
								 * earlier in the search path.
								 */
								String magicToken = st.nextToken();
								String fontName = st.nextToken();
								if (magicToken.startsWith("%!PS-AdobeFont") && (!mFontFiles.containsKey(fontName)))
								{
									mFontFiles.put(fontName, entry);
								}
							}
						}
						bufferedReader.close();
					}				
					catch (FileNotFoundException e)
					{
						/*
				 	 	 * Ignore files that we cannot open.
				 	 	 */
					}
				}
			}
		}
	}	
	
	/**
	 * Search directories containing PostScript fonts and note which fonts are
	 * available in each directory.
	 */
	public static void load() throws MapyrusException, IOException
	{
		String searchPath;
		
		mFontFiles = new HashMap();

		/*
		 * Use search path given as system property, or just current directory
		 * if not given.
		 */
		try
		{
			searchPath = System.getProperty(Constants.PROGRAM_NAME + ".postscript.font.path");
		}
		catch (SecurityException e)
		{
			searchPath = System.getProperty("user.dir");
		}

		/*
		 * Search each directory in turn, adding font definitions we find to our list.
		 */		
		StringTokenizer st = new StringTokenizer(searchPath, File.pathSeparator);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			searchDirectory(token);
		}
	}
	
	/**
	 * Checks whether definition is available for a font. 
	 * @param fontName name of font to lookup.
	 * @return true if font definition available for this font.
	 */
	public static boolean contains(String fontName)
	{
		return(mFontFiles.containsKey(fontName));
	}

	/**
	 * Return font definition from named font.
	 * @param fontName is font to lookup.
	 * @return font definition, or null if font not known.
	 */	
	public static String getFontDefinition(String fontName) throws IOException
	{
		String retval;
		String nextLine;
		StringBuffer sb = new StringBuffer();

		File f = (File)(mFontFiles.get(fontName));
		
		if (f != null)
		{
			try
			{
				/*
				 * Read PostScript font definition file and return it's contents
				 * to caller.
				 */
				BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
				while ((nextLine = bufferedReader.readLine()) != null)
				{
					sb.append(nextLine);
					sb.append(Constants.LINE_SEPARATOR);
				}
				retval = sb.toString();
				bufferedReader.close();
			}
			catch (FileNotFoundException e)
			{
				retval = null;
			}
		}
		else
		{
			retval = null;
		}
		return(retval);
	}
}
