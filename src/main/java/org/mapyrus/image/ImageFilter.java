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

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.mapyrus.MapyrusException;
import org.mapyrus.Throttle;

/**
 * Filters an image, changing its hue, saturation or brightness.
 */
public class ImageFilter
{
	/**
	 * Modify image by applying HSB filter.
	 * @param image image to be modified in place.
	 * @param hue hue factor.
	 * @param saturation saturation factor.
	 * @param brightness brightness factor.
	 * @param throttle throttle limiting CPU usage.
	 * @throws MapyrusException if throttle limits CPU usage during filtering.
	 */
	public static void filter(BufferedImage image, float hue,
		float saturation, float brightness, Throttle throttle) throws MapyrusException
	{
		int width = image.getWidth();
		int height = image.getHeight();

		if (hue < 0)
			hue = 0;
		if (saturation < 0)
			saturation = 0;
		if (brightness < 0)
			brightness = 0;

		float hsb[] = new float[3];
		for (int y = 0; y < height; y++)
		{
			throttle.sleep();
			for (int x = 0; x < width; x++)
			{
				int pixel = image.getRGB(x, y);
				int alpha = (pixel & 0xff000000);

				int red = (pixel & 0xff0000) >> 16;
				int green = (pixel & 0xff00) >> 8;
				int blue = (pixel & 0xff);
				Color.RGBtoHSB(red, green, blue, hsb);
				hsb[0] *= hue;
				hsb[1] *= saturation;
				if (hsb[1] > 1)
					hsb[1] = 1;
				hsb[2] *= brightness;
				if (hsb[2] > 1)
					hsb[2] = 1;
				pixel = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
				pixel = (alpha | pixel);
				image.setRGB(x, y, pixel);
			}
		}
	}
}
