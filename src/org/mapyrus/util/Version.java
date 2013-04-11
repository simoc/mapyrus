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
package org.mapyrus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Returns version number of Mapyrus software.
 */
public class Version
{
	public String getVersion()
	{
		return(readResource("version.txt"));
	}

	public String getReleaseDate()
	{
		return(readResource("timestamp.txt"));
	}

	private String readResource(String filename)
	{
		BufferedReader reader = null;
		String retval = null;
		try
		{
			URL url = this.getClass().getResource(filename);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			retval = reader.readLine();
		}
		catch (IOException e)
		{
		}
		finally
		{
			if (retval == null)
				retval = "";

			try
			{
				if (reader != null)
					reader.close();
			}
			catch (IOException e)
			{
			}
		}
		return retval;
	}
}
