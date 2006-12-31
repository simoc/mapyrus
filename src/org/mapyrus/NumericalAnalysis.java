/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2007 Simon Chenery.
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

/**
 * Provides ststic methods for solving numerical analysis problems
 * (problems with accuracy of floating point numbers).
 */
public class NumericalAnalysis
{
	/**
	 * Compares two double precision numbers for equality with some tolerance.
	 * @param a first number
	 * @param b second number
	 * @return true if numbers are equal or very, very close to being equal.
	 */
	public static boolean equals(double a, double b)
	{
		boolean retval;
		double absA = Math.abs(a);
		double absB = Math.abs(b);
		double magnitude = Math.max(absA, absB);
		double diff = Math.abs(a - b);

		/*
		 * Double precision numbers are accurate to 15-16
		 * significant digits.
		 *
		 * Values are considered equal if first twelve significant
		 * digits are the same.
		 */
		retval = (diff <= magnitude * 1e-12);
// TODO read my numerical analysis book to find better method
		return(retval);
	}
	
	/**
	 * Compares two floating point numbers for equality with some tolerance.
	 * @param a first number
	 * @param b second number
	 * @return true if numbers are equal or very, very close to being equal.
	 */
	public static boolean equals(float a, float b)
	{
		boolean retval;
		float absA = Math.abs(a);
		float absB = Math.abs(b);
		float magnitude = Math.max(absA, absB);
		float diff = Math.abs(a - b);

		/*
		 * Floating point numbers are accurate to 7 significant digits.
		 *
		 * Values are considered equal if first five significant
		 * digits are the same.
		 */
		retval = (diff <= magnitude * 1e-5);
		return(retval);
	}

	/**
	 * Calculate a modulo b for real numbers.  For example,
	 * 6.7 modulo 2.2 is 0.1.
	 * @param a value to be divided.
	 * @param b divisor.
	 * @return a modulo b
	 */
	public static double fmod(double a, double b)
	{
		double retval = Math.IEEEremainder(a, b);
		if (retval < 0)
			retval += b;
		return(retval);
	}
}
