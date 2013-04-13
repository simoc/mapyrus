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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.mapyrus.geom;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.mapyrus.Constants;

/**
 * Calculates a "sinkhole" for a java.awt.Shape object
 * containing a polygon.  A sinkhole is a point inside
 * the polygon, furthest from the polygon perimeter.  A sinkhole
 * point can be thought of as the middle of the polygon.
 */
public class Sinkhole
{
	/*
	 * Size of bitmap to use for sinkhole search.  A larger bitmap
	 * will produce better results (but more slowly).
	 */
	private static final int BITMAP_SIZE = 64;

	/*
	 * Single bitmap to draw polygon into and pixel buffers to use for
	 * shrinking the polygon one pixel at a time.
	 */
	private static byte []m_pixelBuffer1 = new byte[BITMAP_SIZE * BITMAP_SIZE];
	private static byte []m_pixelBuffer2 = new byte[BITMAP_SIZE * BITMAP_SIZE];
	private static BufferedImage m_bufferedImage = new BufferedImage(BITMAP_SIZE, BITMAP_SIZE,
		BufferedImage.TYPE_BYTE_BINARY);
	private static Graphics2D m_g2 = (Graphics2D)m_bufferedImage.getGraphics();

	/*
	 * Nth last pixel to choose.  Last pixel can be at the end of a long
	 * line of pixels so choosing one before the last should be further
	 * inside polygon.
	 */
	private static final int NTH_LAST_PIXEL = 10;

	/*
	 * Buffer of last cleared pixels.
	 */
	private static int []m_lastClearedX = new int[NTH_LAST_PIXEL];
	private static int []m_lastClearedY = new int[NTH_LAST_PIXEL];
		
	/**
	 * Calculate a sinkhole point inside a polygon.
	 * Uses an iterative raster algorithm to reduce the polygon
	 * one pixel at a time around boundary until only a single pixel remains.
	 * @param s polygon to calculate sinkhole for.
	 * @return sinkhole points.
	 */
	public static synchronized Point2D calculate(Shape s)
	{
		int x, y;
		int xMin, yMin, xMax, yMax;
		int lastXMin, lastYMin, lastXMax, lastYMax;

		int lastClearedIndex = 0;
		int nPixelsSet, nPixelsCleared;
		Point2D.Double retval;
		Rectangle2D bounds = s.getBounds2D();
		double maxBounds = Math.max(bounds.getWidth(), bounds.getHeight());
		maxBounds += 0.0001;		/* protect against divide by zero */
		AffineTransform affine =
			AffineTransform.getScaleInstance(BITMAP_SIZE / maxBounds, BITMAP_SIZE / maxBounds);		
		affine.translate(-bounds.getMinX(), -bounds.getMinY());

		/*
		 * Clear image and draw shape into it, filling the entire image.
		 */
		m_g2.setTransform(Constants.IDENTITY_MATRIX);
		m_g2.setColor(Color.BLACK);
		m_g2.fillRect(0, 0, BITMAP_SIZE, BITMAP_SIZE);
		m_g2.setColor(Color.WHITE);
		m_g2.setTransform(affine);
		m_g2.fill(s);

		/*
		 * Create a 0/1 bitmap from the image.
		 */
		for (y = 0; y < BITMAP_SIZE; y++)
		{
			for (x = 0; x < BITMAP_SIZE; x++)
			{
				int pixval = (m_bufferedImage.getRGB(x, y) & 0xffffff);
				if (pixval != 0)
					m_pixelBuffer1[y * BITMAP_SIZE + x] = 1;
				else
					m_pixelBuffer1[y * BITMAP_SIZE + x] = 0;
			}
		}

		lastXMin = lastYMin = 0;
		lastXMax = lastYMax = BITMAP_SIZE - 1;
		
		/*
		 * Search through bitmap and clear pixels that are on the boundary
		 * of the shape.  Continue doing this until all pixels are cleared from
		 * the bitmap.
		 */
		do
		{
			/*
			 * Walk through bitmap, copying pixel values to a second bitmap.
			 * Pixels on the boundary of the shape are set to 0 in second bitmap.
			 */
			xMin = yMin = BITMAP_SIZE;
			xMax = yMax = -1;
			nPixelsSet = nPixelsCleared = 0;
			lastClearedIndex = 0;
			for (y = lastYMin; y <= lastYMax; y++)
			{
				for (x = lastXMin; x <= lastXMax; x++)
				{
					int index = y * BITMAP_SIZE + x;
					if (m_pixelBuffer1[index] != 0)
					{
						/*
						 * Count how many neighbouring pixels are set.
						 */
						int nNeighboursSet = 0;
						if (y > lastYMin && m_pixelBuffer1[index - BITMAP_SIZE] != 0)
							nNeighboursSet++;
						if (y < lastYMax && m_pixelBuffer1[index + BITMAP_SIZE] != 0)
							nNeighboursSet++;
						if (x > lastXMin && m_pixelBuffer1[index - 1] != 0)
							nNeighboursSet++;
						if (x < lastXMax && m_pixelBuffer1[index + 1] != 0)
							nNeighboursSet++;

						/*
						 * If pixel is surrounded by 4 set pixels
						 * then leave it set.
						 */
						if (nNeighboursSet == 4)
						{
							m_pixelBuffer2[index] = 1;
							nPixelsSet++;

							/*
							 * Keep track of area still containing set pixels so we
							 * can reduce next loop through bitmap to only area where
							 * some pixels are set.
							 */
							if (x < xMin)
								xMin = x;
							if (x > xMax)
								xMax = x;
							if (y < yMin)
								yMin = y;
							if (y > yMax)
								yMax = y;
						}
						else
						{
							m_pixelBuffer2[index] = 0;
							nPixelsCleared++;

							/*
							 * Save pixel position we've just cleared to circular
							 * queue of N last points.
							 */
							lastClearedIndex++;
							if (lastClearedIndex == NTH_LAST_PIXEL)
								lastClearedIndex = 0;

							m_lastClearedX[lastClearedIndex] = x;
							m_lastClearedY[lastClearedIndex] = y;
						}
					}
					else
					{
						m_pixelBuffer2[index] = 0;
					}
				}
			}

			/*
			 * Move second buffer to first buffer and repeat process if some
			 * pixels are still set.
			 */
			System.arraycopy(m_pixelBuffer2, 0, m_pixelBuffer1, 0, m_pixelBuffer2.length);

			/*
			 * Shrink area of bitmap to loop through for next iteration.
			 */
			lastXMin = xMin;
			lastYMin = yMin;
			lastXMax = xMax;
			lastYMax = yMax;
		}
		while (nPixelsSet > 0);

		if (nPixelsCleared == 0)
		{
			/*
			 * Could not find a pixel position.  Must be a very odd shape.
			 * Just return middle of bounding box.
			 */
			retval = new Point2D.Double((bounds.getMinX() + bounds.getMaxX()) / 2,
				(bounds.getMinY() + bounds.getMaxY()) / 2);
		}
		else
		{
			/*
			 * Last cleared point in bitmap is closest to the middle of the polygon.
			 * Return it.  Choose Nth last point to reduce chances of getting point
			 * that is at end of a thin line of pixels. 
			 */
			if (nPixelsCleared > NTH_LAST_PIXEL)
			{
				/*
				 * Take Nth last value from circular queue.
				 */
				lastClearedIndex++;
				if (lastClearedIndex == NTH_LAST_PIXEL)
					lastClearedIndex = 0;
			}

			retval = new Point2D.Double(m_lastClearedX[lastClearedIndex],
				m_lastClearedY[lastClearedIndex]);
			try
			{
				affine.inverseTransform(retval, retval);
			}
			catch (NoninvertibleTransformException e)
			{
				/*
				 * Scale-and-translate transformation will always be invertible.
				 */
			}
		}
		return(retval);
	}
}
