/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2011 Simon Chenery.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function returning contents of a text file.
 * For example, spool("/etc/motd") could return a string containing "Have a lot of fun...". 
 */
public class Spool implements Function
{
	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
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

		ByteArrayOutputStream buf = new ByteArrayOutputStream(8 * 1024);
		InputStream stream = null;
		int nBytes;

		try
		{
			/*
			 * Flush any file we are writing to so that we get all
			 * of the file if it happens to be the same file that
			 * we are to read from.
			 */
			context.getStdout().flush();

			/*
			 * Read complete file into memory and return it as a single string.
			 */
			FileOrURL f = new FileOrURL(filename);
			stream = f.getInputStream();
			byte b[] = new byte[1024];
			while ((nBytes = stream.read(b)) > 0)
			{
				buf.write(b, 0, nBytes);
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		finally
		{
			try
			{
				if (stream != null)
					stream.close();
			}
			catch (IOException e)
			{
			}
		}
		Argument retval = new Argument(Argument.STRING, new String(buf.toByteArray()));
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
		return("spool");
	}
}

