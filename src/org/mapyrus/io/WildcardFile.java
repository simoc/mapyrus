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
package org.mapyrus.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusMessages;

/**
 * Finds files matching a file wildcard pattern containing asterisks.
 * Typically used for exanding patterns like "/usr/local/fonts/*.afm".
 */
public class WildcardFile
{
	/*
	 * Directory contains files being matched.
	 */
	private File mParentDirectory;
	
	/*
	 * Filename being matched, split into parts delimited by asterisks.
	 * For example "h*.afm" is split into "h" and ".afm".
	 */
	private ArrayList mFilenameParts;

	public WildcardFile(String wildcard) throws IOException
	{
		/*
		 * Separate wildcard pattern into directory and filename.
		 */
		File f = new File(wildcard);
		mParentDirectory = f.getParentFile();
		if (mParentDirectory == null)
			mParentDirectory = new File(System.getProperty("user.dir"));
		else if (!mParentDirectory.isDirectory())
		{
			throw new IOException(MapyrusMessages.get(MapyrusMessages.FILE_NOT_FOUND) +
				": " + mParentDirectory.toString());
		}
		String filename = f.getName();

		/*
		 * Split filename into pieces, separated by asterisk characters.
		 */
		mFilenameParts = new ArrayList();
		StringTokenizer st = new StringTokenizer(filename, "*", true);

		String lastToken = "";
		boolean tokenIsAsterisk = false;
		while (st.hasMoreTokens())
		{
			/*
			 * Skip repeated asterisk, for example "foo**bar".
			 */
			String token = st.nextToken();
			tokenIsAsterisk = token.equals("*");
			if (!(tokenIsAsterisk && lastToken.equals("*")))
				mFilenameParts.add(token);
			lastToken = token;
		}

		/*
		 * If no wildcards in filename then check that exact match of file exists.
		 */
		if (mFilenameParts.size() <= 1)
		{
			if (tokenIsAsterisk == false && f.exists() == false)
			{
				throw new IOException(MapyrusMessages.get(MapyrusMessages.FILE_NOT_FOUND) +
					": " + wildcard);
			}
		}
	}

	/**
	 * Returns files matching wildcard pattern in enclosing class.
	 */
	private class WildcardFilter implements FilenameFilter
	{
		public boolean accept(File dir, String name)
		{
			boolean retval = true;

			/*
			 * Check that each part of the pattern matches in
			 * the filename, in sequence.
			 */
			int i = 0;
			while (retval && i < mFilenameParts.size())
			{
				String part = (String)mFilenameParts.get(i);
				if (!part.equals("*"))
				{
					/*
					 * Find where next part of pattern matches in the string.
					 * First part of pattern must match exactly at start of
					 * filename.
					 */
					int index = name.indexOf(part);
					if ((i == 0 && index == 0) || (i > 0 && index >= 0))
					{
						name = name.substring(index + part.length());
						
						/*
						 * Last part of filename must match last pattern exactly.
						 */
						if (i == mFilenameParts.size() - 1 && name.length() != 0)
							retval = false;
					}
					else
						retval = false;
				}
				i++;
			}
			return(retval);
		}
	}

	/**
	 * Returns filenames matching a wildcard pattern.
	 * @param wildcard wildcard pattern to match.
	 * @return iterator providing each matching file.
	 */
	public Iterator getMatchingFiles()
	{
		LinkedList list = new LinkedList();

		String []matches = mParentDirectory.list(new WildcardFilter());
		for (int i = 0; i < matches.length; i++)
			list.add(mParentDirectory + File.separator + matches[i]);
		return(list.iterator());
	}
	
	public static void main(String []args)
	{
		try
		{
			WildcardFile w = new WildcardFile("");
			Iterator it = w.getMatchingFiles();
			while (it.hasNext())
				System.out.println("> " + (String)it.next());
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
		}
	}
}
