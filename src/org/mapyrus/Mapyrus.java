/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.io.*;

/**
 * Main class for Mapyrus, a program for generating plots of points, lines and polygons
 * to various output formats.  Runs as either an interpreter for files
 * given on the command line or as a Servlet.
 */
public class Mapyrus
{
	public static final String PROGRAM_NAME = "Mapyrus";
	
	/**
	 * Get software version information.
	 * @return version string.
	 */
	public static String getVersion()
	{
		/*
		 * Current software version number set by ant Replace task during build.
		 */
		return("@software_version_token@");
	}

	/*
	 * Show software version number and usage message.
	 */
	private static void printUsage()
	{		
		String []usage =
		{
			
			"Usage:",
			"java [-Dvar=val] ... -jar " + PROGRAM_NAME.toLowerCase() + ".jar filename ...",
			"",
			PROGRAM_NAME + " reads each file or URL in turn.",
			"If filename is '-' then standard input is read.",
			"",
			"Variables are passed into " + PROGRAM_NAME + " using the Java -D",
			"option.",
			"",
			PROGRAM_NAME + " can also run as a Java Servlet.  Refer to manual for",
			"instructions on incorporating " + PROGRAM_NAME + " into a web application."
		};

		String []license =
		{
			PROGRAM_NAME + " comes with ABSOLUTELY NO WARRANTY, not even for",
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
			"You may redistribute copies of " + PROGRAM_NAME + " under the terms",
			"of the GNU General Public License.  For more information",
			"about these matters, see the file named COPYING."
		};

		System.out.println(PROGRAM_NAME + " version " + getVersion() +
			" Copyright (C) 2003 Simon Chenery");
		System.out.println("");

		for (int i = 0; i < usage.length; i++)
		{
			System.out.println(usage[i]);
		}

		System.out.println("");
		for (int i = 0; i < license.length; i++)
		{
			System.out.println(license[i]);
		}

	}
	
	/**
	 * Parse and interpret commands from a file.  Trap any exceptions.
	 * @param f open file or URL to read.
	 * @param interpreter interpreter in which to run commands.
	 * @param closeFile if set to true file is closed after we finish reading it.
	 * @return flag indicating whether interpretation succeeeded.
	 */
	private static boolean processFile(FileOrURL f, Interpreter interpreter,
		boolean closeFile)
	{
		try
		{
			interpreter.interpret(f, System.out);
			if (closeFile)
				f.getReader().close();
		}
		catch (MapyrusException e)
		{
			System.err.println(e.getMessage());
			return(false);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
			return(false);
		}		
		return(true);
	}
	
	/*
	 * Initialise global settings, color name lookup table.
	 */
	private static void initialise()
	{
		try
		{
			ColorDatabase.load();
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		catch (MapyrusException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
			
	/*
	 * Parse command line arguments and the start processing.
	 */
	public static void main(String []args)
	{
		FileOrURL f = null;
		ContextStack context;
		int i;
		boolean readingStdin;
		
		/*
		 * Parse command line arguments -- these are the files and URLs
		 * to read commands from.
		 */
		if (args.length == 0 || (args.length == 1 && (args[0].equals("-h") ||
			args[0].equals("-help") || args[0].equals("-?") ||
			args[0].equals("-v") || args[0].equals("-version"))))
		{
			/*
			 * Show usage message and quit.
			 */
			printUsage();
			System.exit(1);
		}

		initialise();
		context = new ContextStack();
		Interpreter interpreter = new Interpreter(context);

		i = 0;
		while (i < args.length)
		{
			readingStdin = args[i].equals("-");
			if (readingStdin)
			{
				/*
				 * Read from standard input.
				 */
				f = new FileOrURL(new InputStreamReader(System.in), "standard input");
			}
			else
			{
				/*
				 * Read from a file or URL.
				 */
				try
				{
					f = new FileOrURL(args[i]);
				}
				catch (IOException e)
				{
					System.err.println(e.getMessage());
					System.exit(1);
				}
				catch (MapyrusException e)
				{
					System.err.println(e.getMessage());
					System.exit(1);
				}
			}

			if (!processFile(f, interpreter, !readingStdin))
				System.exit(1);

			i++;
		}
		System.exit(0);
	}
}
