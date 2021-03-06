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

/**
 * The image for an icon plus the color if the icon is monochrome (1-bit-per-pixel).
 * The color may be a CMYK color.
 */
public class ColorIcon
{
	private BufferedImage m_image;
	private Color m_color;

	public ColorIcon(BufferedImage image, Color color)
	{
		m_image = image;
		m_color = color;
	}

	public BufferedImage getImage()
	{
		return m_image;
	}

	public Color getColor()
	{
		return m_color;
	}
}
