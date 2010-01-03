/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
package org.mapyrus.function;

import java.awt.Color;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.ColorDatabase;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function returning value calculated using linear interpolation.
 * For example, interpolate("0 black 10 white", 4) = a shade of grey.
 */
public class Interpolate implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		Argument arg1 = (Argument)args.get(0);
		Argument arg2 = (Argument)args.get(1);
		String interpolationString = arg1.getStringValue();
		double d = arg2.getNumericValue();
		double upperLimit, lowerLimit = -Float.MAX_VALUE;
		Color upperColor, lowerColor;
		double upperValue, lowerValue;
		String token = "", lastToken = null;
		Argument retval = null;

		/*
		 * Walk through interpolation string until we find upper and lower values
		 * to interpolate between.
		 */
		StringTokenizer st = new StringTokenizer(interpolationString);
		while (st.hasMoreTokens() && retval == null)
		{
			token = st.nextToken();
			if (!st.hasMoreTokens())
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.MISSING_VALUE));
			}

			try
			{
				upperLimit = Double.parseDouble(token);
				
				/*
				 * Make sure that range is going in ascending order.
				 */
				if (upperLimit <= lowerLimit)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_ASCENDING) +
						": " + lowerLimit + " " + upperLimit);
				}
			}
			catch (NumberFormatException e)
			{
				throw new MapyrusException(e.getMessage());
			}

			token = st.nextToken();
			if (d >= lowerLimit && d < upperLimit)
			{
				/*
				 * Found range to interpolate in, interpolate between this value
				 * and previous value.
				 */
				if (lastToken == null)
				{
					/*
					 * Value to interpolate is lower than first range.
					 * Use first value.
					 */
					retval = new Argument(Argument.STRING, token);
				}
				else
				{
					double fraction = (d - lowerLimit) / (upperLimit - lowerLimit);

					try
					{
						/*
						 * Make linear interpolation of numeric values, or named colors.
						 */
						lowerValue = Double.parseDouble(lastToken);
						upperValue = Double.parseDouble(token);
						d = lowerValue + fraction * (upperValue - lowerValue);
						retval = new Argument(d);
					}
					catch (NumberFormatException e)
					{
						lowerColor = ColorDatabase.getColor(lastToken, 255, context.getColor());
						if (lowerColor == null)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLOR) +
								": " + lastToken);
						}
						upperColor = ColorDatabase.getColor(token, 255, context.getColor());
						if (upperColor == null)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLOR) +
								": " + token);
						}
						float []lowerHSB = new float[3];
						float []upperHSB = new float[3];
						Color.RGBtoHSB(lowerColor.getRed(), lowerColor.getGreen(),
							lowerColor.getBlue(), lowerHSB);
						Color.RGBtoHSB(upperColor.getRed(), upperColor.getGreen(),
							upperColor.getBlue(), upperHSB);
						float h = (float)(lowerHSB[0] + fraction * (upperHSB[0] - lowerHSB[0]));
						float s = (float)(lowerHSB[1] + fraction * (upperHSB[1] - lowerHSB[1]));
						float b = (float)(lowerHSB[2] + fraction * (upperHSB[2] - lowerHSB[2]));

						/*
						 * Create interpolated color as hex string.
						 */
						Color c = Color.getHSBColor(h, s, b);
						StringBuffer sb = new StringBuffer("0x");
						String hexDigits = Integer.toHexString(c.getRGB() & 0xffffff);
						int padding = 6 - hexDigits.length();
						while (padding-- > 0)
							sb.append('0');
						sb.append(hexDigits);
						retval = new Argument(Argument.STRING, sb.toString());
					}
				}
			}
			lowerLimit = upperLimit;
			lastToken = token;
		}

		/*
		 * Use highest value if value to interpolate is above all ranges.
		 */
		if (retval == null)
		{
			retval = new Argument(Argument.STRING, token);
		}
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(2);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(2);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("interpolate");
	}
}
