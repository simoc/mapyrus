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
import java.net.URL;
import java.net.MalformedURLException;

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
	 * Parse and interpret a file.  Trap any exceptions.
	 * @return flag indicating whether interpretation succeeeded.
	 */
	private static boolean processFile(Reader f, String filename, Interpreter interpreter)
	{
		try
		{
			interpreter.interpret(f, filename);
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
		BufferedReader f;
		Reader []readers;
		ContextStack context;
		
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
		else if (args[0].equals("-"))
		{
			/*
			 * Read from standard input.
			 */
			initialise();
			f = new BufferedReader(new InputStreamReader(System.in));
			context = new ContextStack();
			Interpreter interpreter = new Interpreter(context);

			processFile(f, "standard input", interpreter);
			
			try
			{
				context.closeContextStack();
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
		else
		{	
			initialise();
			context = new ContextStack();
			Interpreter interpreter = new Interpreter(context);

			/*
			 * Process each file and URL given as command line argument.
			 */
			for (int i = 0; i < args.length; i++)
			{
				/*
				 * Try it as a URL.  If that does not work then open it as a file
				 */
				try
				{
					URL url = new URL(args[i]);
					f = new BufferedReader(new InputStreamReader(url.openStream()));
					if (processFile(f, args[i], interpreter) == false)
					{
						System.exit(1);
					}
				}
				catch (MalformedURLException e)
				{
					try
					{
						f = new BufferedReader(new FileReader(args[i]));
						if (processFile(f, args[i], interpreter) == false)
						{
							System.exit(1);
						}
					}
					catch (FileNotFoundException e2)
					{
						System.err.println(e2.getMessage());
						System.exit(1);
					}
				}
				catch (IOException e)
				{
					System.err.println(e.getMessage());
					System.exit(1);
				}
			}
			
			try
			{
				context.closeContextStack();
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
		System.exit(0);
	}
}
