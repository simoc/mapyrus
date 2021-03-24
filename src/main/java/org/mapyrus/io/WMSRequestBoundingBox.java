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

package org.mapyrus.io;

import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Holds bounding box information about geo-referenced image being
 * fetched from an OGC Web Mapping Server (WMS).
 */
public class WMSRequestBoundingBox implements GeoImageBoundingBox
{
	private Rectangle2D.Double m_bounds;

	/**
	 * Create geo-referenced image from WMS request.
	 * @param urlQuery query part of URL after '?' with var=value settings.
	 */
	public WMSRequestBoundingBox(URL url) throws MapyrusException
	{
		String query = url.getQuery().toUpperCase();
		StringTokenizer st = new StringTokenizer(query, "&");
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("BBOX="))
			{
				StringTokenizer st2 =
					new StringTokenizer(token.substring(5), ",");
				if (st2.countTokens() != 4)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WMS_REQUEST) + ": " + url);
				try
				{
					double x1 = Double.parseDouble(st2.nextToken());
					double y1 = Double.parseDouble(st2.nextToken());
					double x2 = Double.parseDouble(st2.nextToken());
					double y2 = Double.parseDouble(st2.nextToken());
					double swap;

					if (y1 > y2)
					{
						swap = y1;
						y1 = y2;
						y2 = swap;
					}
					if (x1 > x2)
					{
						swap = x1;
						x1 = x2;
						x2 = swap;
					}

					m_bounds = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WMS_REQUEST) + ": " + url);
				}
			}
		}

		if (m_bounds == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WMS_REQUEST) + ": " + url);
	}

	/**
	 * Get bounding rectangle for image being returned by request.
	 * @return bounding rectangle.
	 */
	public Rectangle2D getBounds()
	{
		return(m_bounds);
	}
}
