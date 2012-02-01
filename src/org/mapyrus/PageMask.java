/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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
package org.mapyrus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Manages a bitmap mask for a page.  Rectangular regions of the page can
 * be marked as "0" or "1".  rectangular regions can then be queried to determine
 * whether they contain all 0's, 1's or a mixture.
 */
public class PageMask
{
	/*
	 * The mask as a 0/1 array (corresponding to values false/true)
	 * and its width and height.
	 */
	BufferedImage m_mask;
	private Graphics2D m_maskGraphics;
	private int m_maskWidth;
	private int m_maskHeight;

	/*
	 * Number of pixels in mask for each millimetre on page.
	 * A higher value results in a more accurate mask but needs more memory.
	 */
	private static final int PIXELS_PER_MM = 3;

	/**
	 * Create new mask, with all values initially zero.
	 * @param maskWidth width of mask in pixels.
	 * @param maskHeight height of mask in pixels.
	 */
	public PageMask(int maskWidth, int maskHeight)
	{
		m_mask = new BufferedImage(maskWidth * PIXELS_PER_MM,
			maskHeight * PIXELS_PER_MM,
			BufferedImage.TYPE_BYTE_BINARY);
		m_maskGraphics = (Graphics2D)m_mask.getGraphics();
		m_maskGraphics.scale(PIXELS_PER_MM, PIXELS_PER_MM);
		m_maskGraphics.setColor(Color.BLACK);
		m_maskGraphics.fillRect(0, 0, maskWidth, maskHeight);

		m_maskWidth = maskWidth;
		m_maskHeight = maskHeight;
	}

	/**
	 * Set all values inside rectangular area in mask to 0 or to 1. 
	 * @param x1 X coordinate of one corner of area.
	 * @param y1 Y coordinate of one corner of area.
	 * @param x2 X coordinate of opposite corner of area.
	 * @param y2 Y coordinate of opposite corner of area.
	 * @param value 0 or 1 value to set.
	 */
	public void setValue(int x1, int y1, int x2, int y2, int value)
	{
		int xMin = Math.min(x1, x2);
		int xMax = Math.max(x1, x2);
		int yMin = Math.min(y1, y2);
		int yMax = Math.max(y1, y2);
		int width = Math.max(xMax - xMin, 1);
		int height = Math.max(yMax - yMin, 1);
		m_maskGraphics.setColor(value != 0 ? Color.WHITE : Color.BLACK);
		m_maskGraphics.fillRect(xMin, yMin, width, height);
	}

	/**
	 * Set all values inside shape in mask to 0 or to 1. 
	 * @param shape area to set in mask.
	 * @param value 0 or 1 value to set.
	 */
	public void setValue(Shape s, int value)
	{
		m_maskGraphics.setColor(value != 0 ? Color.WHITE : Color.BLACK);
		Rectangle2D bounds = s.getBounds2D();
		if (bounds.getWidth() == 0 && bounds.getHeight() == 0)
			m_maskGraphics.fillRect((int)bounds.getMinX(), (int)bounds.getMinY(), 1, 1);
		else
			m_maskGraphics.fill(s);
	}

	/**
	 * Find whether all values inside rectangular area are zero.
	 * @param x1 X coordinate of one corner of area.
	 * @param y1 Y coordinate of one corner of area.
	 * @param x2 X coordinate of opposite corner of area.
	 * @param y2 Y coordinate of opposite corner of area.
	 * @return true if all values are zero.
	 */
	public boolean isAllZero(int x1, int y1, int x2, int y2)
	{
		int xMin = Math.min(x1, x2);
		int xMax = Math.max(x1, x2);
		int yMin = Math.min(y1, y2);
		int yMax = Math.max(y1, y2);

		/*
		 * Interpret values outside mask area as having value 1.
		 * Therefore if any part of the rectangle falls outside the
		 * mask then the test for all zeroes fails.
		 */
		boolean foundNonZero = (xMin < 0 || yMin < 0 || xMax >= m_maskWidth || yMax >= m_maskHeight);

		int y = yMin * PIXELS_PER_MM;
		while ((!foundNonZero) && y <= yMax * PIXELS_PER_MM)
		{
			int x = xMin * PIXELS_PER_MM;
			while ((!foundNonZero) && x <= xMax * PIXELS_PER_MM)
			{
				int pixel = (m_mask.getRGB(x, y) & 0xffffff);
				foundNonZero = (pixel != 0); 
				x++;
			}
			y++;
		}
		return(!foundNonZero);
	}

	/**
	 * Find whether all values inside area of mask are zero.
	 * @param s shape in mask.
	 * @return true if all values are zero.
	 */
	public boolean isAllZero(Shape s)
	{
		Rectangle2D bounds = s.getBounds2D();
		boolean foundNonZero = (bounds.getMinX() < 0 || bounds.getMinY() < 0 ||
			bounds.getMaxX() >= m_maskWidth || bounds.getMaxY() >= m_maskHeight);
		if (!foundNonZero)
		{
			/*
			 * Make another buffer covering the same area as the mask.
			 * Draw the shape into this buffer.
			 * If any pixels are set in this buffer and in the mask then
			 * there is an overlap.
			 */
			BufferedImage shapeBuffer = new BufferedImage(m_maskWidth * PIXELS_PER_MM,
				m_maskHeight * PIXELS_PER_MM,
				BufferedImage.TYPE_BYTE_BINARY);
			Graphics2D shapeGraphics = (Graphics2D)shapeBuffer.getGraphics();
			shapeGraphics.scale(PIXELS_PER_MM, PIXELS_PER_MM);
			shapeGraphics.setColor(Color.BLACK);
			shapeGraphics.fillRect(0, 0, m_maskWidth, m_maskHeight);
			shapeGraphics.setColor(Color.WHITE);

			if (bounds.getWidth() == 0 && bounds.getHeight() == 0)
				shapeGraphics.fillRect((int)bounds.getMinX(), (int)bounds.getMinY(), 1, 1);
			else
				shapeGraphics.fill(s);

			int y = (int)(bounds.getMinY() * PIXELS_PER_MM);
			int xMax = (int)(bounds.getMaxX() * PIXELS_PER_MM);
			int yMax = (int)(bounds.getMaxY() * PIXELS_PER_MM);
			while (y <= yMax && (!foundNonZero))
			{
				int x = (int)(bounds.getMinX() * PIXELS_PER_MM);
				while (x <= xMax && (!foundNonZero))
				{
					int shapePixel = (shapeBuffer.getRGB(x, y) & 0xffffff);
					if (shapePixel != 0)
					{
						/*
						 * Is shape overlapping part of the mask that is set to 1?
						 */
						int maskPixel = (m_mask.getRGB(x, y) & 0xffffff);
						foundNonZero = (maskPixel != 0);
					}
					x++;
				}
				y++;
			}
		}
		return(!foundNonZero);
	}
}
