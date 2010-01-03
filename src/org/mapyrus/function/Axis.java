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

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function calculating axis values for a range of values.
 * For example, axis(2.3, 8.8, 5) returns an array with values
 * 2, 4, 6, 8, 10.
 */
public class Axis implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		Argument retval;
		Argument arg1 = (Argument)args.get(0);
		Argument arg2 = (Argument)args.get(1);
		Argument arg3 = (Argument)args.get(2);
		double minValue = arg1.getNumericValue();
		double maxValue = arg2.getNumericValue();
		int maxIntervals = (int)arg3.getNumericValue();

		/*
		 * Check that all values are reasonable.
		 */
		double diff = maxValue - minValue;
		if (diff <= 0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_RANGE));
		if (diff >= Long.MAX_VALUE)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NUMERIC_OVERFLOW));
		if (maxIntervals <= 0 || maxIntervals > 10000)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_INTERVAL));

		/*
		 * Find magnitude of minimum and maximum values.
		 */
		int minPower10 = lowerLog10(minValue);
		int maxPower10 = lowerLog10(maxValue);
		int power10;

		if ((minPower10 + maxPower10) / 2 < 0)
			power10 = Math.min(minPower10, maxPower10);
		else
			power10 = Math.max(minPower10, maxPower10);

		/*
		 * Start with a large interval size, bigger than the maximum value.
		 */
		power10++;
		int factor = 2;

		double lastSpacing = 0;
		int lastIntervals = 0;
		double lastMinValue = 0;
		int iterationCount = 0;
		while (true)
		{
			/*
			 * Work out next smaller interval, counting down like
			 * 5000, 2000, 1000, 500, 200, 100, 50, ...
			 */
			if (factor == 5)
			{
				factor = 2;
			}
			else if (factor == 2)
			{
				factor = 1;
			}
			else
			{
				factor = 5;
				
				/*
				 * Go down to next lower power of 10.
				 */
				power10--;
			}

			/*
			 * Calculate minimum and maximum values for this
			 * new spacing.
			 */
			double spacing = pow10(power10) * factor;
			double newMinValue = calculateMinValue(minValue, spacing);
			double newMaxValue = calculateMaxValue(maxValue, spacing);
			diff = newMaxValue - newMinValue;
			if (maxIntervals == 1 && newMinValue < 0 && newMaxValue > 0)
			{
				/*
			 	 * Special case for single interval with a negative and
			 	 * positive value that cannot be solved by this algorithm.
				 * Double the spacing to remove zero as an interval. 
			 	 */
				spacing *= 2;
			}
			int nIntervals = (int)Math.round(diff / spacing);
			if (nIntervals > maxIntervals)
			{
				/*
				 * If this spacing is too small then stop and use
				 * last spacing as the best spacing.
				 */
				break;
			}

			lastSpacing = spacing;
			lastIntervals = nIntervals;
			lastMinValue = newMinValue;

			/*
			 * Avoid infinite loop when numerical problems occur.
			 */
			if (iterationCount++ > 10000)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NUMERIC_OVERFLOW));
		}

		/*
		 * Build array of values to use for axis.  Number of values
		 * is one more than the number of intervals.
		 */
		retval = new Argument();
		for (int i = 0; i < lastIntervals + 1; i++)
		{
			double nextValue = lastMinValue + i * lastSpacing;
			retval.addHashMapEntry(Integer.toString(i + 1),
				new Argument(nextValue));
		}
		return(retval);
	}

	/**
	 * Calculate highest power of 10 smaller than a value.
	 * @param d value to calculate log 10 for.
	 * @return log 10 of value.
	 */
	private int lowerLog10(double d)
	{
		int retval = 1;

		d = Math.abs(d);
		if (d > 0)
			retval = (int)Math.log10(d);
		else
			retval = 1;
		return(retval);
	}

	/**
	 * Calculate power of 10.
	 * @param power nth power of 10 to calculate.
	 * @return power of 10.
	 */
	private double pow10(int power)
	{
		double retval;
		long pow = 1;
		int absPower = Math.abs(power);
		for (int i = 0; i < absPower; i++)
			pow *= 10;
		if (power >= 0)
			retval = pow;
		else
			retval = 1.0 / pow;
		return(retval);
	}

	private double calculateMinValue(double d, double spacing)
	{
		double retval = ((long)(d / spacing)) * spacing;
		
		/*
		 * Negative values must be rounded to a lower value.
		 */
		if (retval > d)
			retval -= spacing;
		return(retval);
	}

	private double calculateMaxValue(double d, double spacing)
	{
		double retval = ((long)(d / spacing)) * spacing;

		/*
		 * Negative values must be rounded to higher value.
		 */
		if (retval < d)
			retval += spacing;
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(3);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(3);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("axis");
	}
}
