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

package org.mapyrus.image;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusException;

/*
 * Holds a monochrome image read from an X Windows XBM file.
 */
public class XBMImage
{
	private BufferedImage m_image;

	/**
	 * Read XBM image from file.
	 * @param filename name of file.
	 * @param rgb RGB color for pixel values in image.
	 * @throws IOException if file cannot be opened.
	 * @throws MapyrusException if file does not contain a XBM image.
	 */
	public XBMImage(String filename, int rgb) throws MapyrusException, IOException
	{
		InputStream stream = new FileInputStream(filename);
		init(stream, filename, rgb);
	}

	/**
	 * Read XBM image from open stream.
	 * @param stream open stream to read from.
	 * @param filename name of file.
	 * @param rgb RGB color for pixel values in image.
	 * @throws IOException if stream cannot be read.
	 * @throws MapyrusException if stream does not contain a XBM image.
	 */
	public XBMImage(InputStream stream, String filename, int rgb)
		throws MapyrusException, IOException
	{
		init(stream, filename, rgb);
	}

	private void init(InputStream stream, String filename, int rgb)
		throws IOException
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			int imageWidth = 0, imageHeight = 0;
			int x = 0, y = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.indexOf("#define") >= 0)
				{
					/*
					 * Extract image dimensions from file header.
					 */
					try
					{
						int index = line.indexOf("_width");
						if (index > 0)
						{
							imageWidth = Integer.parseInt(line.substring(index + 6).trim());
						}
						index = line.indexOf("_height");
						if (index > 0)
						{
							imageHeight = Integer.parseInt(line.substring(index + 7).trim());
						}
					}
					catch (NumberFormatException e)
					{
					}
					if (imageWidth > 0 && imageHeight > 0 && m_image == null)
					{
						m_image = new BufferedImage(imageWidth, imageHeight,
							BufferedImage.TYPE_INT_ARGB);
					}
				}
				else if (m_image != null)
				{
					StringTokenizer st = new StringTokenizer(line, " \t,{};");
					while (st.hasMoreTokens())
					{
						String token = st.nextToken();
						if (token.startsWith("0x"))
						{
							/*
							 * Set pixels in image for each pair of hex digits in XBM file.
							 */
							token = token.substring(2);
							int bits = Integer.parseInt(token, 16);
							for (int i = 0; i < 8; i++)
							{
								if (x + i < imageWidth && ((bits >> i) & 1) != 0)
									m_image.setRGB(x + i, y, rgb);
							}

							/*
							 * Move through image left to right, from top to bottom.
							 */
							x += 8;
							if (x >= imageWidth)
							{
								x = 0;
								y++;
							}
						}
					}
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
	 * Get XBM image as buffered image.
	 * @return image.
	 */
	public BufferedImage getBufferedImage()
	{
		return(m_image);
	}
}
