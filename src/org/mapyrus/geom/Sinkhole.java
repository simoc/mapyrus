/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
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
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Calculates a "sinkhols" for a java.awt.Shape object
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
	static final int BITMAP_SIZE = 64;

	/*
	 * Nth last pixel to choose.  Last pixel can be at the end of a long
	 * line of pixels
	 */
	static final int NTH_LAST_PIXEL = 12;

	/**
	 * Calculate a sinkhole point inside a polygon.
	 * Uses an iterative raster algorithm to reduce the polygon
	 * one pixel at a time around boundary until only a single pixel remains.
	 * @param s polygon to calculate sinkhole for.
	 * @return sinkhole points.
	 */
	public static Point2D calculate(Shape s)
	{
		int x, y;
		int xMin, yMin, xMax, yMax;
		int lastXMin, lastYMin, lastXMax, lastYMax;
		int lastXMid, lastYMid;
		int lastClearedX = -1;
		int lastClearedY = -1;
		int nPixelsSet;
		Point2D.Double retval;
		Rectangle2D bounds = s.getBounds2D();
		AffineTransform affine =
			AffineTransform.getScaleInstance(BITMAP_SIZE / bounds.getWidth(), BITMAP_SIZE / bounds.getHeight());		
		affine.translate(-bounds.getMinX(), -bounds.getMinY());

		/*
		 * Create image and draw shape into it, filling the entire image.
		 */
		BufferedImage bufferedImage = new BufferedImage(BITMAP_SIZE, BITMAP_SIZE,
			BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g2 = (Graphics2D)bufferedImage.getGraphics();
		g2.setColor(Color.WHITE);
		g2.setTransform(affine);
		g2.fill(s);
		
		try
		{
		FileOutputStream f = new FileOutputStream("/tmp/a.png");
		ImageIO.write(bufferedImage, "png", f);
		f.close();
		}
		catch (IOException e)
		{
		}

//		Point2D.Double dp = new Point2D.Double(194 + 16, 201 + 21);
//		affine.transform(dp, dp);
//		dp = new Point2D.Double(194, 201);
//		affine.transform(dp, dp);
		int iter = 0;



		/*
		 * Create a 0/1 bitmap from the image.
		 */
		byte []pixelBuffer1 = new byte[BITMAP_SIZE * BITMAP_SIZE];
		byte []pixelBuffer2 = new byte[BITMAP_SIZE * BITMAP_SIZE];
		for (y = 0; y < BITMAP_SIZE; y++)
		{
			for (x = 0; x < BITMAP_SIZE; x++)
			{
				int pixval = (bufferedImage.getRGB(x, y) & 0xffffff);
				if (pixval != 0)
					pixelBuffer1[y * BITMAP_SIZE + x] = 1;
				else
					pixelBuffer1[y * BITMAP_SIZE + x] = 0;
			}
		}
		bufferedImage = null;

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
			nPixelsSet = 0;
			lastXMid = (lastYMin + lastYMax) / 2;
			lastYMid = (lastXMin + lastXMax) / 2;
			for (y = lastYMin; y <= lastYMax; y++)
			{
				for (x = lastXMin; x <= lastXMax; x++)
				{
					int index = y * BITMAP_SIZE + x;
					if (pixelBuffer1[index] != 0)
					{
						/*
						 * Count how many neighbouring pixels are set.
						 */
						int nNeighboursSet = 0;
						if (y > lastYMin && pixelBuffer1[index - BITMAP_SIZE] != 0)
							nNeighboursSet++;
						if (y < lastYMax && pixelBuffer1[index + BITMAP_SIZE] != 0)
							nNeighboursSet++;
						if (x > lastXMin && pixelBuffer1[index - 1] != 0)
							nNeighboursSet++;
						if (x < lastXMax && pixelBuffer1[index + 1] != 0)
							nNeighboursSet++;

						/*
						 * If pixel is surrounded by 4 set pixels
						 * then leave it set.
						 */
						if (nNeighboursSet == 4)
						{
							pixelBuffer2[index] = 1;
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
							lastClearedX = x;
							lastClearedY = y;
							pixelBuffer2[index] = 0;
						}
					}
					else
					{
						pixelBuffer2[index] = 0;
					}
				}
			}
			
			/*
			 * Move second buffer to first buffer and repeat process if some
			 * pixels are still set.
			 */
			System.arraycopy(pixelBuffer2, 0, pixelBuffer1, 0, pixelBuffer2.length);
			System.out.println("iter=" + (++iter) + " nPixelsSet=" + nPixelsSet + " lastCleared=(" + lastClearedX + "," + lastClearedY + ")");
			for (y = lastYMin; y <= lastYMax; y++)
			{
				for (x = lastXMin; x <= lastXMax; x++)
					System.out.print((pixelBuffer2[y * BITMAP_SIZE + x] != 0) ? "X" : ".");
				System.out.println("");
			}

			/*
			 * Shrink area of bitmap to loop through for next iteration.
			 */
			lastXMin = xMin;
			lastYMin = yMin;
			lastXMax = xMax;
			lastYMax = yMax;
		}
		while (nPixelsSet > 0);
	
		if (lastClearedX < 0)
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
			 * Return it.
			 */
			retval = new Point2D.Double(lastClearedX, lastClearedY);
			try
			{
				affine.inverseTransform(retval, retval);
			}
			catch (NoninvertibleTransformException e)
			{
				/*
				 * Transformation will always be invertible.
				 */
			}
		}
		System.out.println("retval=" + retval);
		return(retval);
	}
}
