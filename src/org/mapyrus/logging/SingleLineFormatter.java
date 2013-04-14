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

package org.mapyrus.logging;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.mapyrus.Constants;

/**
 * Formatter for log messages to make them appear in simple, single line format
 * that can easily be searched with grep(1).
 */
public class SingleLineFormatter extends Formatter
{
	@Override
	public String format(LogRecord record)
	{
		StringBuffer sb = new StringBuffer(128);

		String level = record.getLevel().getName();
		String message = record.getMessage();

		long millis = record.getMillis();
		GregorianCalendar g = new GregorianCalendar();
		g.setTimeInMillis(millis);

		/*
		 * Create message of the form:
		 * 2004.05.24 22:31:55.231 SEVERE: File not found: foo.txt
		 */
		sb.append(g.get(Calendar.YEAR));
		sb.append('.');

		int month = g.get(Calendar.MONTH) + 1;
		if (month < 10)
			sb.append('0');
		sb.append(month);

		sb.append('.');

		int day = g.get(Calendar.DAY_OF_MONTH);
		if (day < 10)
			sb.append('0');
		sb.append(day);

		sb.append(' ');
		
		int hour = g.get(Calendar.HOUR_OF_DAY);
		if (hour < 10)
			sb.append('0');
		sb.append(hour);
		
		sb.append(':');

		int minute = g.get(Calendar.MINUTE);
		if (minute < 10)
			sb.append('0');
		sb.append(minute);
		
		sb.append(':');
		
		int second = g.get(Calendar.SECOND);
		if (second < 10)
			sb.append('0');
		sb.append(second);

		sb.append('.');
		
		millis = g.get(Calendar.MILLISECOND);
		if (millis < 100)
			sb.append('0');
		if (millis < 10)
			sb.append('0');
		sb.append(millis);

		sb.append(' ');
		sb.append(level);
		sb.append(':');

		if (message != null)
		{
			sb.append(' ');
			sb.append(message);
		}

		sb.append(Constants.LINE_SEPARATOR);

		return(sb.toString());
	}
}
