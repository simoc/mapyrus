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

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/*
 * Holds bitmap image created from a string of hexadecimal or binary digits.
 */
public class Bitmap
{
	public static final int HEX_DIGIT_BITMAP = 0;
	public static final int BINARY_DIGIT_BITMAP = 1;

	private BufferedImage mBitmap;

	/**
	 * Create new bitmap from hex or binary digit string.
	 * @param definition string containing digits
	 * @param digitsType either HEX_DIGIT_BITMAP or BINARY_DIGIT_BITMAP.
	 * @param c color for bitmap.
	 */
	public Bitmap(String definition, int digitsType, Color c) throws MapyrusException
	{
		int index = 0;
		int nameLength = definition.length();
		byte []bits = new byte[nameLength * 4];
		int nBits = 0;

		/*
		 * Convert all hex digits into a list of bits from which we
		 * can make an image.
		 */
		while (index < nameLength)
		{
			int c1 = definition.charAt(index);
			int c2 = 0;

			c1 = hexValue(c1);

			/*
			 * Check for "0x" sequence and ignore it if found.
			 */
			if (digitsType == HEX_DIGIT_BITMAP &&
				c1 == 0 && index + 1 < nameLength)
			{
				c2 = definition.charAt(index + 1);
				if (c2 == 'x' || c2 == 'X')
					c1 = -1;
			}

			if (digitsType == BINARY_DIGIT_BITMAP && (c1 == 0 || c1 == 1))
			{
				/*
				 * Add 1 bit from this binary digit.
				 */
				bits[nBits] = (byte)(c1);
				nBits++;
			}
			else if (digitsType == HEX_DIGIT_BITMAP && c1 >= 0)
			{
				/*
				 * Add 4 bits from this hex digit.
				 */
				bits[nBits] = (byte)(c1 & 8);
				bits[nBits + 1] = (byte)(c1 & 4);
				bits[nBits + 2] = (byte)(c1 & 2);
				bits[nBits + 3] = (byte)(c1 & 1);
				nBits += 4;
			}
			index++;
		}

		/*
		 * Calculate size of square image from number of bits.
		 */
		int iconSize = (int)Math.round(Math.sqrt(nBits));
		if (nBits == 0 || iconSize * iconSize != nBits)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HEX_ICON) +
				": " + definition);
		}

		/*
		 * Create image and set pixels on/off as defined in hex digits.
		 */
		mBitmap = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_4BYTE_ABGR);
		int rgbPixel = c.getRGB();
		int bitIndex = 0;
		for (int y = 0; y < iconSize; y++)
		{
			for (int x = 0; x < iconSize; x++)
			{
				if (bits[bitIndex++] != 0)
					mBitmap.setRGB(x, y, rgbPixel);
			}
		}
	}

	/**
	 * Convert hexadecimal character to integer value.
	 * @param hexDigit hex character to convert
	 * @return value of hex digit, or -1 if character is not a hex digit. 
	 */
	private int hexValue(int hexDigit)
	{
		int retval = -1;

		if (hexDigit >= '0' && hexDigit <= '9')
		{
			retval = hexDigit - '0';
		}
		else if (hexDigit >= 'a' && hexDigit <= 'f')
		{
			retval = hexDigit - 'a' + 10;
		}
		else if (hexDigit >= 'A' && hexDigit <= 'F')
		{
			retval = hexDigit - 'A' + 10;
		}
		return(retval);
	}

	/**
	 * Get bitmap as an image.
	 * @return buffered image.
	 */
	public BufferedImage getBufferedImage()
	{
		return(mBitmap);
	}
}
