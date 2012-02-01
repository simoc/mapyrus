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
package org.mapyrus;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * Stores long string, being a combination of strings and data read
 * from big text files.
 */
public class BigString
{
	private ArrayList<Object> m_items;
	
	public BigString()
	{
		m_items = new ArrayList<Object>();
	}

	/**
	 * Append a string.
	 * @sb string buffer to append.
	 */
	public void append(StringBuffer sb)
	{
		m_items.add(sb);
	}

	/**
	 * Append contents of file to string.
	 * @file file to be appended.
	 */
	public void append(File file)
	{
		m_items.add(file);
	}

	/**
	 * Write all strings to a writer.
	 * @param filename name of file being written to.
	 * @param writer writer to append strings to.
	 * @return number of characters written.
	 */
	public int writeTo(String filename, PrintWriter writer)
		throws IOException, MapyrusException
	{
		int totalChars = 0;
		Iterator<Object> it = m_items.iterator();
		while (it.hasNext())
		{
			Object obj = it.next();
			if (obj instanceof StringBuffer)
			{
				StringBuffer sb = (StringBuffer)obj;
				int len = sb.length();
				for (int i = 0; i < len; i++)
				{
					writer.write(sb.charAt(i));
				}
				totalChars += len;
			}
			else
			{
				File file = (File)obj;
				FileReader reader = null;
				try
				{
					reader = new FileReader(file);
					char []cbuf = new char[512];
					int nBytes;
					while ((nBytes = reader.read(cbuf)) > 0)
					{
						writer.write(cbuf, 0, nBytes);
						if (writer.checkError())
						{
							throw new MapyrusException(filename +
								": " + MapyrusMessages.get(MapyrusMessages.ERROR_FILE));
						}
						totalChars += nBytes;
					}
				}
				finally
				{
					try
					{
						if (reader != null)
							reader.close();
					}
					catch (IOException e)
					{
					}
				}
			}
		}
		return(totalChars);
	}

	/**
	 * Delete any files that are used in the big string.
	 */
	public void deleteFiles()
	{
		Iterator<Object> it = m_items.iterator();
		while (it.hasNext())
		{
			Object obj = it.next();
			if (obj instanceof File)
			{
				File file = (File)obj;
				file.delete();
			}
		}
	}
}
