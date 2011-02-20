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
package org.mapyrus.io;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

import org.mapyrus.FileOrURL;
import org.mapyrus.GeometricPath;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Clipping polygon for a geo-referenced image read from a file containing (X, Y)
 * coordinates of polygon.
 * 
 * By clippping each image in a row of trapezoidal shaped images, each image
 * can be displayed without overwriting the neighbouring images:
 * 
 * <pre>
 *   ___ ___ ___
 *  /  //  //  /
 * /__//__//__/
 * </pre>
 */
public class ImageClippingFile
{
	private GeometricPath m_path;

	/**
	 * Read optional clipping polygon for an image.
	 * @param filename name of file containing clip polygon coordinates.
	 * @param ctm transformation matrix from world coordinates to page coordinates.
	 */
	public ImageClippingFile(String filename, AffineTransform ctm)
		throws MapyrusException, IOException
	{
		LineNumberReader reader = null;
		String line;
		double []pt = new double[2];
		int nPts = 0;

		try
		{	
			FileOrURL f;
			f = new FileOrURL(filename);

			reader = f.getReader();
			m_path = new GeometricPath();

			while ((line = reader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, " \t,");
				if (st.countTokens() >= 2)
				{
					String xs = st.nextToken();
					String ys = st.nextToken();
					if (!xs.startsWith("#"))
					{
						try
						{
							pt[0] = Double.parseDouble(xs);
							pt[1] = Double.parseDouble(ys);
						}
						catch (NumberFormatException e)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
								": " + filename + ":" + reader.getLineNumber() + ": " + line);
						}

						if (ctm != null)
							ctm.transform(pt, 0, pt, 0, 1);

						if (nPts++ == 0)
							m_path.moveTo((float)pt[0], (float)pt[1], 0);
						else
							m_path.lineTo((float)pt[0], (float)pt[1]);
					}
				}
			}
			m_path.closePath();
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

	/**
	 * Get clipping polygon for image.
	 * @return clipping polygon. 
	 */
	public GeometricPath getClippingPolygon()
	{
		return(m_path);
	}
}
