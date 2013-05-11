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

package org.mapyrus;

import java.awt.color.ColorSpace;

/**
 * Minimal CMYK color space to enable CMYK colors to be used in Mapyrus.
 */
public class CMYKColorSpace extends ColorSpace
{
	public static final long serialVersionUID = 0xee;

	public CMYKColorSpace()
	{
		super(TYPE_CMYK, 4);
	}

	@Override
	public float[] toRGB(float[] colorvalue)
	{
		/*
		 * Use formula from PDF Reference, 6.2.4 Conversion from DeviceCMYK to DeviceRGB
		 */
		float []rgb = new float[3];
		rgb[0] = 1.0f - Math.min(1.0f, colorvalue[0] + colorvalue[3]);
		rgb[1] = 1.0f - Math.min(1.0f, colorvalue[1] + colorvalue[3]);
		rgb[2] = 1.0f - Math.min(1.0f, colorvalue[2] + colorvalue[3]);
		return rgb;
	}

	@Override
	public float[] fromRGB(float[] rgbvalue)
	{
		/*
		 * We do not need this conversion (and no single, correct formula exists anyway).
		 */
		return null;
	}

	@Override
	public float[] toCIEXYZ(float[] colorvalue)
	{
		return null;
	}

	@Override
	public float[] fromCIEXYZ(float[] colorvalue)
	{
		return null;
	}
}
