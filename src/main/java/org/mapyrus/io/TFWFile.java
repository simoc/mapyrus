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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.LineNumberReader;

import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Extents for an a geo-referenced image read from a '.tfw' file
 * for an image.
 */
public class TFWFile implements GeoImageBoundingBox
{
	private Rectangle2D.Double m_bounds;

	/**
	 * Read tfw file for an image.
	 * @param filename filename of image or tfw file.
	 * @param image image read from file.
	 * @throws IOException if file cannot be opened or read.
	 * @throws MapyrusException if file is not a TFW file.
	 */
	public TFWFile(String filename, BufferedImage image)
		throws MapyrusException, IOException
	{
		double []values = new double[6];
		int nValuesRead = 0;
		String line;

		String basename;
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex < 0)
			basename = filename;
		else
			basename = filename.substring(0, dotIndex);

		/*
		 * Check both upper and lowercase file extension.
		 */
		String tfwLower = basename + ".tfw";
		String tfwUpper = basename + ".TFW";
		String tfwFilename;
		FileOrURL f;
		try
		{
			f = new FileOrURL(tfwLower);
			tfwFilename = tfwLower;
		}
		catch (IOException e)
		{
			f = new FileOrURL(tfwUpper);
			tfwFilename = tfwUpper;
		}

		try (LineNumberReader reader = f.getReader())
		{
			/*
			 * Read six numbers from file giving bounding rectangle coordinates of image.
			 */
			while (nValuesRead < values.length)
			{
				line = reader.readLine();
				if (line == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
						": " + tfwFilename);
				}

				line = line.trim();
				try
				{
					values[nValuesRead] = Double.parseDouble(line);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + tfwFilename + ":" + reader.getLineNumber() + ": " + line);
				}
				nValuesRead++;
			}

			double pixelWidth = values[0];
			double pixelHeight = -values[3];

			/*
			 * TFW file contains position of centre of pixel at top-left corner of image.
			 * Shift position half a pixel so we have top-left corner of image.
			 */
			double xMin = values[4] - pixelWidth / 2;
			double yMax = values[5] + pixelHeight / 2;

			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();
			m_bounds = new Rectangle2D.Double(xMin, yMax - imageHeight * pixelHeight,
				imageWidth * pixelWidth, imageHeight * pixelHeight);
		}
	}

	/**
	 * Get bounding rectangle for image given in tfw file.
	 * @return bounding rectangle.
	 */
	public Rectangle2D getBounds()
	{
		return(m_bounds);
	}

	public String toString()
	{
		return(m_bounds.toString());
	}
}
