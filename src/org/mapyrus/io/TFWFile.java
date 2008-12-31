/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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
	Rectangle2D.Double mBounds;

	/**
	 * Read tfw file for an image.
	 * @param filename filename of image or tfw file.
	 * @param image image read from file.
	 */
	public TFWFile(String filename, BufferedImage image)
		throws MapyrusException, IOException
	{
		LineNumberReader reader = null;
		double []values = new double[6];
		int nValuesRead = 0;
		String line;

		try
		{
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
			reader = f.getReader();

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
			mBounds = new Rectangle2D.Double(xMin, yMax - imageHeight * pixelHeight,
				imageWidth * pixelWidth, imageHeight * pixelHeight);
		}
		finally
		{
			try
			{
				if (reader != null)
					reader.close();
			}
			catch (IOException ignore)
			{
			}
		}
	}

	/**
	 * Get bounding rectangle for image given in tfw file.
	 * @return bounding rectangle.
	 */
	public Rectangle2D getBounds()
	{
		return(mBounds);
	}
}
