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

import java.io.File;
import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function checking if a file exists and is readable.
 * For example, readable("/etc/passwd") returns 1 on a UNIX machine.
 */
public class Readable implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		String filename = arg1.getStringValue();

		if (!context.getThrottle().isIOAllowed())
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
				": " + filename);
		}

		File f = new File(filename);
		
		Argument retval = Argument.numericZero;
		try
		{
			if (f.isFile() && f.canRead())
				retval = Argument.numericOne;
		}
		catch (SecurityException e)
		{
			/*
			 * No access to file so it is not readable.
			 */
		}

		return(retval);
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
		return("readable");
	}
}


