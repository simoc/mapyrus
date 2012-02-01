/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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
 * Function parsing degrees-minutes-seconds value from various
 * formats and returning a numeric value.
 * For example, parsegeo('151d 30m') = 151.5
 */
public class Parsegeo implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval;
		double degrees = 0, minutes = 0, seconds = 0;
		int sign = 1;
		Argument arg1 = args.get(0);
		String geo = arg1.toString().toLowerCase().trim();

		try
		{
			/*
			 * Parse any leading N, S, E, W letter.
			 */
			if (geo.startsWith("n") || geo.startsWith("e"))
			{
				geo = geo.substring(1);
			}
			else if (geo.startsWith("s") || geo.startsWith("w"))
			{
				geo = geo.substring(1);
				sign = -1;
			}

			/*
			 * Search for end of degree value.
			 */
			int dIndex = geo.indexOf('d');
			if (dIndex < 0)
				dIndex = geo.indexOf('�');
			if (dIndex < 0)
				dIndex = geo.indexOf('\ufffd');
			if (dIndex >= 0)
			{
				String geo2 = geo.substring(0, dIndex).trim();
				degrees = Double.parseDouble(geo2);

				/*
				 * Search for end of minutes value, then parse it.
				 */
				int mIndex = geo.indexOf('m', dIndex);
				if (mIndex < 0)
					mIndex = geo.indexOf('\'', dIndex);
				if (mIndex > 0)
				{
					String geo3 = geo.substring(dIndex + 1, mIndex);
					if (geo3.startsWith("eg"))
						geo3 = geo3.substring(2);
					minutes = Double.parseDouble(geo3.trim());

					/*
				 	 * Search for end of seconds value.
				 	 */
					int sIndex = geo.indexOf('s', mIndex);
					if (sIndex < 0)
						sIndex = geo.indexOf('\"', mIndex);
					if (sIndex > 0)
					{
						String geo4 = geo.substring(mIndex + 1, sIndex);
						if (geo4.startsWith("in"))
							geo4 = geo4.substring(2);
						seconds = Double.parseDouble(geo4.trim());

						/*
						 * Parsed seconds value, with an 's'.
						 * Remove parts of string we've parsed to avoid
						 * interpreting it as meaning south.
						 */
						geo = geo.substring(sIndex + 1).trim();
					}
				}
			}

			/*
			 * Parse any trailing N, S, E, W letter.
			 */
			if (geo.endsWith("n") || geo.endsWith("e"))
			{
				geo = geo.substring(0, geo.length() - 1);
			}
			else if (geo.endsWith("s") || geo.endsWith("w"))
			{
				geo = geo.substring(0, geo.length() - 1);
				sign = -1;
			}

			if (dIndex < 0)
			{
				/*
				 * Parse degrees as a plain number.
				 */
				degrees = Double.parseDouble(geo);
			}

			degrees *= sign;

			if (degrees >= 0)
				degrees = degrees + (minutes / 60) + (seconds / 3600);
			else
				degrees = degrees - (minutes / 60) - (seconds / 3600);
		}
		catch (NumberFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOGRAPHIC) +
				": " + geo);
		}

		retval = new Argument(degrees);
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(1);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("parsegeo");
	}
}
