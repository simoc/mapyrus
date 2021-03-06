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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.ps;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Provides functions for parsing PostScript format files.
 */
public class PostScriptFile
{
	private int m_nPages;
	Rectangle m_boundingBox;

	/**
	 * Open PostScript file and parse header information.
	 * @param filename name of PostScript file to read.
	 * @throws IOException if PostScript file cannot be opened or read.
	 * @throws MapyrusException if PostScript file cannot be parsed.
	 */
	public PostScriptFile(String filename)
		throws IOException, MapyrusException
	{
		String firstBoundingBox = null;
		String lastBoundingBox = null;
		String firstPages = null;
		String lastPages = null;
		String line;
		int x1, y1, x2, y2;
		StringTokenizer st;
		MapyrusException e = new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_PS_FILE) + ": " + filename);

		try (BufferedReader reader = new BufferedReader(new FileReader(filename)))
		{
			line = reader.readLine();
			if (line == null || (!line.startsWith("%!")))
				throw e;

			/*
			 * Parse bounding box and number of pages from file header.
			 * Remember first and last values so that we known values if
			 * they are defined as '(atend)' in the header.
			 */
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("%%Pages:"))
				{
					if (firstPages == null)
						firstPages = line;
					lastPages = line;
				}
				else if (line.startsWith("%%BoundingBox:"))
				{
					if (firstBoundingBox == null)
						firstBoundingBox = line;
					lastBoundingBox = line;
				}
			}

			if (firstPages != null && firstPages.indexOf("(atend)") >= 0)
				line = lastPages;
			else
				line = firstPages;

			if (line == null)
			{
				/*
				 * Assume a single page if not defined in PostScript file.
				 */
				m_nPages = 0;
			}
			else
			{
				st = new StringTokenizer(line);
				if (st.countTokens() == 2)
				{
					st.nextToken();
					m_nPages = Integer.parseInt(st.nextToken());
				}
				else
				{
					throw e;
				}
			}

			if (firstBoundingBox != null && firstBoundingBox.indexOf("(atend)") >= 0)
				line = lastBoundingBox;
			else
				line = firstBoundingBox;

			if (line == null)
				throw e;

			st = new StringTokenizer(line);
			if (st.countTokens() == 5)
			{
				st.nextToken();
				x1 = Integer.parseInt(st.nextToken());
				y1 = Integer.parseInt(st.nextToken());
				x2 = Integer.parseInt(st.nextToken());
				y2 = Integer.parseInt(st.nextToken());

				m_boundingBox = new Rectangle(x1, y1, x2 - x1, y2 - y1);
			}
			else
			{
				throw e;
			}
		}
		catch (NumberFormatException nfe)
		{
			throw e;
		}
		catch (SecurityException se)
		{
			throw new IOException(se.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Get number of pages in this PostScript fie.
	 * @return number of pages.
	 */
	public int getNumberOfPages()
	{
		return(m_nPages);
	}

	/**
	 * Parse bounding box from a PostScript file.
	 * @return bounding box.
	 */
	public Rectangle getBoundingBox()
	{
		return(m_boundingBox);
	}
}
