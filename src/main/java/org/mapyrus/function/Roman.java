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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function returning number converted to Roman numerals.
 * For example, roman(24) = "XXIV"
 */
public class Roman implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;
		StringBuilder roman = new StringBuilder();
		Argument arg1 = args.get(0);
		int n = (int)arg1.getNumericValue();

		if (n < 1)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.ROMAN_CONVERSION) +
				": " + arg1.toString());
		}

		/*
		 * Convert value to Roman numeral string.
		 */
		n = addRomanDigits(n, 1000, "M", roman);
		n = addRomanDigits(n, 900, "CM", roman);
		n = addRomanDigits(n, 500, "D", roman);
		n = addRomanDigits(n, 400, "CD", roman);
		n = addRomanDigits(n, 100, "C", roman);
		n = addRomanDigits(n, 90, "XC", roman);
		n = addRomanDigits(n, 50, "L", roman);
		n = addRomanDigits(n, 40, "XL", roman);
		n = addRomanDigits(n, 10, "X", roman);
		n = addRomanDigits(n, 9, "IX", roman);
		n = addRomanDigits(n, 5, "V", roman);
		n = addRomanDigits(n, 4, "IV", roman);
		n = addRomanDigits(n, 1, "I", roman);

		retval = new Argument(Argument.STRING, roman.toString());
		return(retval);
	}

	/**
	 * Append digits for a value to a Roman numeral string.
	 * @param n value being converted to Roman numeral string.
	 * @param romanUnits decimal value Roman numeral units.
	 * @param romanDigits Roman numeral digits to add for each multiple
	 * of romanUnits.
	 * @return value n after subtracting Roman numeral digits added
	 * to Roman numeral string.
	 */
	private int addRomanDigits(int n, int romanUnits, String romanDigits, StringBuilder buf)
	{
		while (n >= romanUnits)
		{
			buf.append(romanDigits);
			n -= romanUnits;
		}
		return(n);
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(1);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("roman");
	}
}
