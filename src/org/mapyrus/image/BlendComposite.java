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

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Implements several blending modes for colors.
 */
public class BlendComposite implements Composite
{
	private interface Blender
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel);
	}

	/*
	 * List of available blending modes and the formulas for blending
	 * pixel values.
	 */
	public static BlendComposite MULTIPLY = new BlendComposite("multiply", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
				resultPixel[i] = (srcPixel[i] * dstPixel[i]) / 256;
		}
	});

	public static BlendComposite SCREEN = new BlendComposite("screen", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
				resultPixel[i] = 255 - ((255 - srcPixel[i]) * (255 - dstPixel[i]) / 256);
		}
	});

	public static BlendComposite OVERLAY = new BlendComposite("overlay", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				if (dstPixel[i] < 128)
					resultPixel[i] = (dstPixel[i] * srcPixel[i]) >> 7;
				else
					resultPixel[i] = 255 - ((255 - dstPixel[i]) * (255 - srcPixel[i]) / 128);
			}
		}
	});

	public static BlendComposite DARKEN = new BlendComposite("darken", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				resultPixel[i] = Math.min(dstPixel[i], srcPixel[i]);
			}
		}
	});

	public static BlendComposite LIGHTEN = new BlendComposite("lighten", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				resultPixel[i] = Math.max(dstPixel[i], srcPixel[i]);
			}
		}
	});

	public static BlendComposite COLORDODGE = new BlendComposite("colordodge", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				if (srcPixel[i] == 255)
				{
					resultPixel[i] = 255;
				}
				else
				{
					resultPixel[i] = (dstPixel[i] * 256) / (255 - srcPixel[i]);
					if (resultPixel[i] > 255)
						resultPixel[i] = 255;
				}
			}
		}
	});

	public static BlendComposite COLORBURN = new BlendComposite("colorburn", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				if (srcPixel[i] == 0)
				{
					resultPixel[i] = 0;
				}
				else
				{
					resultPixel[i] = 255 - (((255 - dstPixel[i]) * 256) / srcPixel[i]);
					if (resultPixel[i] < 0)
						resultPixel[i] = 0;
				}
			}
		}
	});

	public static BlendComposite HARDLIGHT = new BlendComposite("hardlight", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				if (srcPixel[i] < 128)
					resultPixel[i] = (srcPixel[i] * dstPixel[i]) / 128;
				else
					resultPixel[i] = 255 - ((255 - srcPixel[i]) * (255 - dstPixel[i]) / 128);
			}
		}
	});

	public static BlendComposite SOFTLIGHT = new BlendComposite("softlight", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
			{
				if (srcPixel[i] < 128)
				{
					/*
					 * Same as color burn.
					 */
					resultPixel[i] = 255 - (((255 - dstPixel[i]) * 256) / srcPixel[i]);
					if (resultPixel[i] < 0)
						resultPixel[i] = 0;
				}
				else
				{
					/*
					 * Same as color dodge.
					 */
					resultPixel[i] = (dstPixel[i] * 256) / (255 - srcPixel[i]);
				}
			}
		}
	});

	public static BlendComposite DIFFERENCE = new BlendComposite("difference", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
				resultPixel[i] = Math.abs(dstPixel[i] - srcPixel[i]);
		}
	});

	public static BlendComposite EXCLUSION = new BlendComposite("exclusion", new Blender()
	{
		public void blend(int []srcPixel, int []dstPixel, int []resultPixel)
		{
			for (int i = 0; i < 3; i++)
				resultPixel[i] = (dstPixel[i] + srcPixel[i]) - (dstPixel[i] * srcPixel[i] / 128);
		}
	});

	private String mName;
	private Blender mBlender;

	public BlendComposite(String name, Blender blender)
	{
		mName = name;
		mBlender = blender;
	}

	public CompositeContext createContext(ColorModel srcColorModel,
		ColorModel dstColorModel, RenderingHints hints)
	{
		CompositeContext retval = new BlendCompositeContext();
		return(retval);
	}

	/**
	 * Get blend composite.
	 * @param name name of blend composite.
	 * @return blend composite, or null if name is not known.
	 */
	public static BlendComposite getBlendComposite(String name)
	{
		BlendComposite retval = null;
		if (name.equals(BlendComposite.MULTIPLY.getName()))
			retval = BlendComposite.MULTIPLY;
		else if (name.equals(BlendComposite.SCREEN.getName()))
			retval = BlendComposite.SCREEN;
		else if (name.equals(BlendComposite.OVERLAY.getName()))
			retval = BlendComposite.OVERLAY;
		else if (name.equals(BlendComposite.DARKEN.getName()))
			retval = BlendComposite.DARKEN;
		else if (name.equals(BlendComposite.LIGHTEN.getName()))
			retval = BlendComposite.LIGHTEN;
		else if (name.equals(BlendComposite.COLORDODGE.getName()))
			retval = BlendComposite.COLORDODGE;
		else if (name.equals(BlendComposite.COLORBURN.getName()))
			retval = BlendComposite.COLORBURN;
		else if (name.equals(BlendComposite.HARDLIGHT.getName()))
			retval = BlendComposite.HARDLIGHT;
		else if (name.equals(BlendComposite.SOFTLIGHT.getName()))
			retval = BlendComposite.SOFTLIGHT;
		else if (name.equals(BlendComposite.DIFFERENCE.getName()))
			retval = BlendComposite.DIFFERENCE;
		else if (name.equals(BlendComposite.EXCLUSION.getName()))
			retval = BlendComposite.EXCLUSION;
		return(retval);
	}

	/**
	 * Get name of blend.
	 * @return name of blend.
	 */
	public String getName()
	{
		return(mName);
	}

	private class BlendCompositeContext implements CompositeContext
	{
		public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
		{
			int width = Math.min(src.getWidth(), dstIn.getWidth());
			int height = Math.min(src.getHeight(), dstIn.getHeight());

			int []srcPixel = new int[4];
			int []dstPixel = new int[4];
			int []resultPixel = new int[4];
			int alpha;

			/*
			 * Blend each pixel in the image.
			 */
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					src.getPixel(x, y, srcPixel);
					alpha = srcPixel[3];
					dstIn.getPixel(x, y, dstPixel);
					mBlender.blend(srcPixel, dstPixel, resultPixel);

					/*
					 * Blend the alpha channel too.
					 */
					resultPixel[3] = srcPixel[3] + dstPixel[3];

					for (int i = 0; i < 4; i++)
					{
						if (alpha != 255)
						{
							resultPixel[i] = dstPixel[i] +
								(int)((resultPixel[i] - dstPixel[i]) * alpha / 256);
						}
						if (resultPixel[i] > 255)
							resultPixel[i] = 255;
					}

					dstOut.setPixel(x, y, resultPixel);
				}
			}
		}

		public void dispose()
		{
		}
	}
}
