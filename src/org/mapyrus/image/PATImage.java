/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005, 2006 Simon Chenery.
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
package org.mapyrus.image;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/*
 * Holds an image read from a GIMP pattern file.
 */
public class PATImage
{
	private BufferedImage mImage;

	/**
	 * Read GIMP image pattern from URL.
	 * @param url URL.
	 */
	public PATImage(URL url) throws MapyrusException, IOException
	{
		DataInputStream stream = new DataInputStream(new BufferedInputStream(url.openStream()));
		init(stream, url.toString());
	}

	/**
	 * Read GIMP image pattern from file.
	 * @param filename name of file.
	 */
	public PATImage(String filename) throws MapyrusException, IOException
	{
		DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
		init(stream, filename);
	}

	/**
	 * Read GIMP image pattern from open stream.
	 * @param stream open stream to read from.
	 * @param filename name of file.
	 */
	public PATImage(InputStream stream, String filename) throws MapyrusException, IOException
	{
		DataInputStream stream2 = new DataInputStream(new BufferedInputStream(stream));
		init(stream2, filename);
	}
	
	private void init(DataInputStream stream, String filename) throws MapyrusException, IOException
	{
		try
		{
			/*
			 * Read pattern header.
			 */
			int headerLength = stream.readInt();
			stream.readInt();
			int width = stream.readInt();
			int height = stream.readInt();
			int bytesPerPixel = stream.readInt();
			byte []magicBuf = new byte[4];
			stream.read(magicBuf);
			
			if ((!(magicBuf[0] == 'G' && magicBuf[1] == 'P' &&
				magicBuf[2] == 'A' && magicBuf[3] == 'T')) ||
				width <= 0 || height <= 0)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PAT_FILE) +
					": " + filename);
			}

			/*
			 * Skip rest of file header.
			 */
			int offset = 24;
			while (offset++ < headerLength)
				stream.read();

			/*
			 * Read image pixels.
			 */
			mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					int red, green, blue;
					if (bytesPerPixel == 1)
					{
						/*
						 * Grayscale pixel values.
						 */
						red = green = blue = stream.read();
					}
					else
					{
						/*
						 * RGB pixel values.
						 */
						red = stream.read();
						green = stream.read();
						blue = stream.read();
					}
					int pixel = (red << 16) | (green << 8) | blue;
					mImage.setRGB(x, y, pixel);
				}
			}
		}
		finally
		{
			try
			{
				stream.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Get GIMP pattern as buffered image.
	 * @return image.
	 */
	public BufferedImage getBufferedImage()
	{
		return(mImage);
	}
}
