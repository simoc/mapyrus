/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004 Simon Chenery.
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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Factory class returning images containing gradiated fill patterns
 * fading from one color to another.
 * Fixed colors at corners and centre of image are given as follows:
 * <pre>
 * c3      c4
 *  +------+
 *  |      |
 *  |  c5  |
 *  |      |
 *  +------+
 * c1      c2
 * </pre>
 *
 * The image is calculated to make a smooth pattern, gradually fading
 * from each color to the neighbouring colors.
 */
public class GradientFillFactory
{
	/*
	 * Size of image returned by getImage() methods.
	 */
	private static final int IMAGE_SIZE = 300;

	/*
	 * Last image returned and the colors it contains.
	 */
	private static BufferedImage mLastImage = null;
	private static Color mLastC1 = Color.BLACK;
	private static Color mLastC2 = Color.BLACK;
	private static Color mLastC3 = Color.BLACK;
	private static Color mLastC4 = Color.BLACK;
	private static Color mLastC5 = Color.BLACK;

	private static short c1Weightings[];
	private static short c2Weightings[];
	private static short c3Weightings[];
	private static short c4Weightings[];
	private static short c5Weightings[];

	/*
	 * Precalculate weighting of colors in each pixel in the image
	 * so that we can quickly calculate colors for each pixel.
	 */
	static
	{
		c1Weightings = new short[IMAGE_SIZE * IMAGE_SIZE];
		c2Weightings = new short[IMAGE_SIZE * IMAGE_SIZE];
		c3Weightings = new short[IMAGE_SIZE * IMAGE_SIZE];
		c4Weightings = new short[IMAGE_SIZE * IMAGE_SIZE];
		c5Weightings = new short[IMAGE_SIZE * IMAGE_SIZE];

		int maxDistance = IMAGE_SIZE * IMAGE_SIZE * 2;
		for (int y = 0; y < IMAGE_SIZE; y++)
		{
			for (int x = 0; x < IMAGE_SIZE; x++)
			{
				/*
				 * Calculate distance to each point in the image at which
				 * a color is defined.
				 */
				int c3Distance = x * x + y * y;
				int c4Distance = (IMAGE_SIZE - x) * (IMAGE_SIZE - x) + y * y;
				int c1Distance = x * x + (IMAGE_SIZE - y) * (IMAGE_SIZE - y);
				int c2Distance = (IMAGE_SIZE - x) * (IMAGE_SIZE - x) +
					(IMAGE_SIZE - y) * (IMAGE_SIZE - y);
				int c5Distance = (IMAGE_SIZE / 2 - x) * (IMAGE_SIZE / 2 - x) +
					(IMAGE_SIZE / 2 - y) * (IMAGE_SIZE / 2 - y);

				int sumDistances = c1Distance + c2Distance + c3Distance +
					c4Distance + c5Distance;
				int index = y * IMAGE_SIZE + x;

				/*
				 * Set weighting so that color at each pixel is most influenced
				 * by the closest fixed colors.
				 */
				c1Weightings[index] = (short)Math.round(Short.MAX_VALUE * power8(1 - (double)c1Distance / maxDistance));
				c2Weightings[index] = (short)Math.round(Short.MAX_VALUE * power8(1 - (double)c2Distance / maxDistance));
				c3Weightings[index] = (short)Math.round(Short.MAX_VALUE * power8(1 - (double)c3Distance / maxDistance));
				c4Weightings[index] = (short)Math.round(Short.MAX_VALUE * power8(1 - (double)c4Distance / maxDistance));
				c5Weightings[index] = (short)Math.round(Short.MAX_VALUE * power8(1 - (double)c5Distance / maxDistance));
			}
		}
	}

	/**
	 * Calculate 8th power of number.
	 * @param d value to calculate.
	 * @return d to the power 8.
	 */
	private static double power8(double d)
	{
		return(d * d * d * d * d * d * d * d);
	}

	/**
	 * Calculate image containing smooth pattern calculated from five colors, in each corner of image
	 * and in the center.
	 * @param c1 color for lower-left corner of image.
	 * @param c2 color for lower-right corner of image.
	 * @param c3 color for top-left corner of image.
	 * @param c4 color for top-right corner of image.
	 * @param c5 color for center of image, if null then not used.
	 * @return image containing pattern.
	 */
	public static synchronized BufferedImage getImage(Color c1,
		Color c2, Color c3, Color c4, Color c5)
	{
		BufferedImage retval;

		/*
		 * If colors are the same as the last call then we
		 * can return the same image again.
		 */
		if (c1.equals(mLastC1) && c2.equals(mLastC2) &&
			c3.equals(mLastC3) && c4.equals(mLastC4) &&
			((c5 == null && mLastC5 == null) || (c5 != null && mLastC5 != null && c5.equals(mLastC5))))
		{
			retval = mLastImage;
		}
		else
		{
			retval = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE,
				BufferedImage.TYPE_INT_RGB);

			float c5wRed = 0, c5wGreen = 0, c5wBlue = 0;

			for (int y = 0; y < IMAGE_SIZE; y++)
			{
				for (int x = 0; x < IMAGE_SIZE; x++)
				{
					int index = y * IMAGE_SIZE + x;
					int c1w = c1Weightings[index];
					int c2w = c2Weightings[index];
					int c3w = c3Weightings[index];
					int c4w = c4Weightings[index];
					int c5w = c5Weightings[index];

					float sumWeightings = c1w + c2w + c3w + c4w;
					if (c5 != null)
					{
						/*
						 * Use fifth optional color if provided.
						 */
						sumWeightings += c5w;
						c5wRed = c5w / sumWeightings * c5.getRed();
						c5wGreen = c5w / sumWeightings * c5.getGreen();
						c5wBlue = c5w / sumWeightings * c5.getBlue();
					}

					/*
					 * Calculate RGB values for pixel from five fixed colors,
					 * with colors closest to the current pixel weighted more heavily.
					 */
					float red = c1w / sumWeightings * c1.getRed() +
						c2w / sumWeightings * c2.getRed() +
						c3w / sumWeightings * c3.getRed() +
						c4w / sumWeightings * c4.getRed() +
						c5wRed;

					float green = c1w / sumWeightings * c1.getGreen() +
						c2w / sumWeightings * c2.getGreen() +
						c3w / sumWeightings * c3.getGreen() +
						c4w / sumWeightings * c4.getGreen() +
						c5wGreen;

					float blue = c1w / sumWeightings * c1.getBlue() +
						c2w / sumWeightings * c2.getBlue() +
						c3w / sumWeightings * c3.getBlue() +
						c4w / sumWeightings * c4.getBlue() +
						c5wBlue;

					int rgb = ((int)red) << 16 | ((int)green) << 8 | (int)blue;
					retval.setRGB(x, y, rgb);
				}
			}

			/*
			 * Remember image being returned so that if caller asks
			 * for the same image again next time we can return it again.
			 */
			mLastImage = retval;
			mLastC1 = c1;
			mLastC2 = c2;
			mLastC3 = c3;
			mLastC4 = c4;
			mLastC5 = c5;
		}
		return(mLastImage);
	}

	public static void main(String []args)
	{
		try
		{
			
			BufferedImage image = getImage(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.RED);
			ImageIO.write(image, "png", new File("/tmp/a.png"));
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
		}
		System.out.println("Finished");
	}
}
