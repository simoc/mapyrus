/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Main class for Mapyrus, a program for generating plots of points, lines and polygons
 * to various output formats.  Runs as either a GUI, a server or interpreter for files
 * given on the command line.
 */
public class Mapyrus
{
	public static final String PROGRAM_NAME = "Mapyrus";
	private static final int DEFAULT_PORT = 23177;
	private static final String RCS_STATE = "$State$";

	/**
	 * Get software version information.
	 * @return version string.
	 */
	public static String getVersion()
	{
		/*
		 * Strip off RCS stuff around state.
		 */
		String rcsState = RCS_STATE.substring(8).replace('$', ' ');
		return(rcsState.trim());
	}

	/*
	 * Show software version number and usage message.
	 */
	private static void printUsage()
	{		
		String []messages =
		{
			
			"Usage:",
			"java -jar " + PROGRAM_NAME.toLowerCase() + ".jar [filename | URL] ...",
			"",
			PROGRAM_NAME + " reads each file and URL in turn.",
			"If filename is '-' then reads from standard input.",
			"If no filenames or URLs  are given then a GUI is started",
			"for commands to be entered interactively.",
			"",
			"java -jar " + PROGRAM_NAME.toLowerCase() + ".jar httpserver [port]",
			"",
			PROGRAM_NAME + " runs as an HTTP server, accepting HTTP GET",
			"requests on the given TCP/IP port.",
		};
		
		System.out.println(PROGRAM_NAME + " version " + getVersion());
		System.out.println("");
		
		for (int i = 0; i < messages.length; i++)
		{
			System.out.println(messages[i]);
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
			ColorDatabase.initialise();
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
		 * Parse command line arguments -- these are the files and URLs to read commands from.
		 */
		if (args.length == 0)
		{
			/*
			 * Start GUI for user to enter commands into.
			 */
			initialise();
			MapyrusInputFrame m = new MapyrusInputFrame();
			
		}
		else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("-help") ||
			args[0].equals("-?") || args[0].equals("-v") || args[0].equals("-version")))
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
			boolean isServer = false;
			int startIndex = 0;
			
			if (args[0].equals("httpserver"))
			{
				int port;
				isServer = true;
					
				/*
				 * Run as a server.
				 */
				if (args.length == 1)
				{
					port = DEFAULT_PORT;
					startIndex = 1;
				}
				else
				{
					port = Integer.parseInt(args[1]);
					startIndex = 2;
				}
			}

			initialise();
			context = new ContextStack();
			Interpreter interpreter = new Interpreter(context);
			
			/*
			 * Process each file and URL given as command line argument.
			 */
			for (int i = startIndex; i < args.length; i++)
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
