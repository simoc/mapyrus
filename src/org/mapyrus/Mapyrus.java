/**
 * Main class for Mapyrus, a program for generating plots of points, lines and polygons
 * to various output formats.  Runs as either a GUI, a server or interpeter for files
 * given on the command line.
 */

/*
 * $Id$
 */
import java.lang.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

public class Mapyrus
{
	public static final String PROGRAM_NAME = "Mapyrus";
	private static final int DEFAULT_PORT = 23177;
	private static final String RCS_STATE = "$State$";
	
	/*
	 * Show software version number and usage message.
	 */
	private static void printUsage()
	{		
		String []messages =
		{
			
			"Usage:",
			"java -jar " + PROGRAM_NAME + ".jar [filename|URL] ...",
			"",
			PROGRAM_NAME + " reads each file and URL in turn.",
			"If filename is '-' then reads from standard input.",
			"If no filenames or URLs  are given then a GUI is started",
			"for commands to be entered interactively.",
			"",
			"java -jar " + PROGRAM_NAME + ".jar server [port]",
			"",
			PROGRAM_NAME + " runs as a server, accepting connections",
			"on the given TCP/IP port.",
		};
		
		System.out.println(PROGRAM_NAME + " version " + RCS_STATE.substring(8));
		System.out.println("");
		
		for (int i = 0; i < messages.length; i++)
		{
			System.out.println(messages[i]);
		}
	}
	
	/**
	 * Run an interpreter on a file, wait for it to finish, trap any
	 * exceptions that occur and see if it succeeded.
	 */
	private static boolean processFile(Reader f, Context context)
	{
		Interpreter interpreter;
		interpreter = new Interpreter(f, context);

		try
		{
			/*
			 * Run interpreter, wait for it to finish and then see whether it
			 * succeeded or not.
			 */
			interpreter.start();
			interpreter.join();
			if (interpreter.getReturnStatus() == false)
			{
				System.err.println(interpreter.getErrorMessage());
				return(false);
			}
		}
		catch (InterruptedException e)
		{
			System.err.println(interpreter.getErrorMessage());
			return(false);
		}		
		return(true);
	}
			
	/*
	 *
	 */
	public static void main(String []args)
	{
		BufferedReader f;
		Context context;
		
		args = new String[1];
		args[0] = "/home/simonc/expr.txt";
		
		/*
		 * Parse command line arguments -- these are the files and URLs to read commands from.
		 */
		if (args.length == 0)
		{
			/*
			 * Start GUI for user to enter commands into.
			 */
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
			f = new BufferedReader(new InputStreamReader(System.in));
			context = new Context();
			processFile(f, context);
			
			try
			{
				context.closeOutputFormat();
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
			
			if (args[0].equals("server"))
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

			context = new Context();
			
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
					if (processFile(f, context) == false)
					{
						System.exit(1);
					}
				}
				catch (MalformedURLException e)
				{
					try
					{
						f = new BufferedReader(new FileReader(args[i]));
						if (processFile(f, context) == false)
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
				context.closeOutputFormat();
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
