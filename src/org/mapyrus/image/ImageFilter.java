/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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

/**
 * Filters an image, changing its brightness.
 */
public class ImageFilter
{
	/**
	 * Modify image by applying brightness filter.
	 * @param image image to be modified in place.
	 * @param factor brightness factor.
	 */
	public static void filter(BufferedImage image, float brightness)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		if (brightness < 0)
			brightness = 0;

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int pixel = image.getRGB(x, y);
				int alpha = (pixel & 0xff000000);
				int red = (pixel & 0xff0000) >> 16;
				int green = (pixel & 0xff00) >> 8;
				int blue = (pixel & 0xff);
				
				red = (int)(red * brightness);
				green = (int)(green * brightness);
				blue = (int)(blue * brightness);
				
				if (red > 255)
					red = 255;
				if (green > 255)
					green = 255;
				if (blue > 255)
					blue = 255;

				pixel = (alpha | (red << 16) | (green << 8) | blue);

				image.setRGB(x, y, pixel);
			}
		}
	}
}