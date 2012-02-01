/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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
package org.mapyrus.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.mapyrus.Constants;

/**
 * Finds files matching a file wildcard pattern containing asterisks.
 * Typically used for exanding patterns like "/usr/local/fonts/*.afm".
 */
public class WildcardFile
{
	private static boolean m_filenamesCaseInsensitive;

	static
	{
		/*
		 * If running on Windows then filenames are case insensitive.
		 * So the pattern "*.txt" will match filenames "foo.txt" and "BAR.TXT".
		 */
		m_filenamesCaseInsensitive = (Constants.getOSName().indexOf("WIN") >= 0);
	}

	/*
	 * Base directory of all matching files.
	 */
	private File m_baseDirectory;
	
	/*
	 * File and directory names being matched, split into parts delimited
	 * by file separators and asterisks.
	 * For example "/etc/rc*.d/S*" is split into "rc", "*", ".d", "/", "S", "*".
	 */
	private ArrayList<String> m_patternParts;

	public WildcardFile(String wildcard)
	{
		/*
		 * Separate wildcard pattern into directory and filename.
		 */
		String base, pattern;
		
		if (m_filenamesCaseInsensitive)
			wildcard = wildcard.toUpperCase();

		int wildcardIndex = wildcard.indexOf('*');
		if (wildcardIndex < 0)
			wildcardIndex = wildcard.length();

		int fileSeparatorIndex = wildcard.lastIndexOf(File.separatorChar,
			wildcardIndex);

		if (fileSeparatorIndex < 0)
		{
			base = "";
			pattern = wildcard;
		}
		else
		{
			base = wildcard.substring(0, fileSeparatorIndex + 1);
			pattern = wildcard.substring(fileSeparatorIndex + 1);
		}

		if (base.length() == 0)
			m_baseDirectory = new File(System.getProperty("user.dir"));
		else
			m_baseDirectory = new File(base);

		/*
		 * Split filename into pieces, separated by asterisk characters and "/".
		 */
		m_patternParts = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(pattern, "*" + File.separator, true);

		String lastToken = "";
		boolean tokenIsAsterisk = false;
		boolean tokenIsFileSeparator = false;
		while (st.hasMoreTokens())
		{
			/*
			 * Skip repeated asterisks or slashes, for example "foo**bar".
			 */
			String token = st.nextToken();
			tokenIsAsterisk = token.equals("*");
			tokenIsFileSeparator = token.equals(File.separator);
			if ((!(tokenIsAsterisk && lastToken.equals("*"))) &&
				(!(tokenIsFileSeparator && lastToken.equals(File.separator))))
			{
				m_patternParts.add(token);
			}
			lastToken = token;
		}
	}

	/**
	 * Match files, recursively searching subdirectories if necessary.
	 * @param directory directory in which to search.
	 * @param index index in mPatternParts to start matching.
	 * @return list of matching files.
	 */
	private List<String> recursivelyMatchFiles(File directory, int index)
	{
		LinkedList<String> retval = new LinkedList<String>();

		int nFilenameParts = m_patternParts.size();
		String []filenames = directory.list();

		if (filenames == null)
			return(retval);

		/*
		 * On operating systems where filenames not case sensitive,
		 * convert all filenames to upper case and match against uppercase
		 * wildcard pattern.
		 */
		if (m_filenamesCaseInsensitive)
		{
			for (int i = 0; i < filenames.length; i++)
				filenames[i] = filenames[i].toUpperCase();
		}

		Arrays.sort(filenames);
		for (int i = 0; i < filenames.length; i++)
		{
			boolean matched = true;

			String filename = filenames[i];

			for (int j = index; j < nFilenameParts && matched; j++)
			{
				String part = (String)m_patternParts.get(j);
				if (part.equals("*"))
				{
				}
				else if (part.equals(File.separator))
				{
					/*
					 * Search down through subdirectories for files that
					 * match the rest of the pattern.
					 */
					File subDirectory = new File(directory, filenames[i]);
					if (subDirectory.isDirectory())
					{
						List<String> list = recursivelyMatchFiles(subDirectory, j + 1);
						retval.addAll(list);
					}

					matched = false;
				}
				else
				{
					if (j == nFilenameParts - 1)
					{
						/*
						 * Check that end of filename matches final string in pattern.
						 * For example, for pattern "*.afm" filename must end with ".afm".
						 */
						if (!filename.endsWith(part))
							matched = false; 
					}
					else
					{
						/*
						 * If next part of pattern is a "/" then end of filename must
						 * match current string in pattern.
						 */
						String nextPart = (String)m_patternParts.get(j + 1);
						if (nextPart.equals(File.separator) &&
							(!filename.endsWith(part)))
						{
							matched = false;
						}

						/*
						 * Find matching string in filename.  The first part
						 * of the pattern must match at the start of the string.
						 */
						int k = filename.indexOf(part);
						if (k < 0 || (j == index && k != 0))
						{
							matched = false;
						}
						else
						{
							/*
							 * Use remainder of filename after match to
							 * match against the rest of the pattern.
							 */
							filename = filename.substring(k + part.length());
						}
					}
				}
			}

			if (matched)
			{
				if (new File(directory, filenames[i]).isFile())
					retval.add(directory.toString() + File.separator + filenames[i]);
			}
		}
		return(retval);
	}

	/**
	 * Returns filenames matching a wildcard pattern.
	 * @param wildcard wildcard pattern to match.
	 * @return list of matching files in sorted order.
	 */
	public List<String> getMatchingFiles()
	{
		List<String> retval;
		if (m_patternParts.size() == 0)
		{
			/*
			 * An empty filename cannot match anything.
			 */
			retval = new LinkedList<String>();
		}
		else
		{
			boolean isSingleFile = false;

			if (m_patternParts.size() == 1)
			{
				String first = (String)m_patternParts.get(0);
				if (!first.equals("*"))
					isSingleFile = true;
			}

			if (isSingleFile)
			{
				/*
				 * For a pattern with no '*' we can simply check for existence of
				 * the single file.
				 */
				retval = new LinkedList<String>();
	
				File f = new File(m_baseDirectory + File.separator + (String)m_patternParts.get(0));
				if (f.exists() && f.isFile())
					retval.add(f.toString());
			}
			else
			{
				retval = recursivelyMatchFiles(m_baseDirectory, 0);
			}
		}
		return(retval);
	}
}
