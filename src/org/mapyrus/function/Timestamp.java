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

package org.mapyrus.function;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function returning a time and date string.
 * For example, timestamp(60) returns the time one minute in the future.
 */
public class Timestamp implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		double offsetSeconds = 0;
		if (!args.isEmpty())
		{
			Argument arg1 = args.get(0);
			offsetSeconds = arg1.getNumericValue();
		}
		long now = System.currentTimeMillis();
		Date d = new Date(Math.round(now + offsetSeconds * 1000));
		SimpleDateFormat f = new SimpleDateFormat("EEE, MMM d HH:mm:ss z");
		f.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		Argument retval = new Argument(Argument.STRING, f.format(d));
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
		return(0);
	}

	@Override
	public String getName()
	{
		return("timestamp");
	}
}


