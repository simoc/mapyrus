/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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
package org.mapyrus;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapyrus.logging.SingleLineFormatter;

/**
 * Main class for Mapyrus, a program for generating plots of points,
 * lines and polygons to various output formats.
 * Runs as either an interpreter for files given on the command
 * line, as a library embedded in a Java application, or as an HTTP server.
 *
 * An interpreter is not thread-safe.
 *
 * When all work with an interpreter is complete call the close() method
 * to flush and close any output page and datasets still in use.
 */
public class Mapyrus
{
	private Interpreter mInterpreter;
	private ContextStack mContext;

	/**
	 * Create new interpreter.
	 */
	public Mapyrus()
	{
		mInterpreter = new Interpreter();
		mContext = new ContextStack();
	}

	/**
	 * Create string reader from string array.
	 * @param s string to create reader from.
	 * @return string reader.
	 */
	private StringReader makeStringReader(String []s)
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length; i++)
		{
			sb.append(s[i]);
			sb.append(Constants.LINE_SEPARATOR);
		}
		StringReader retval = new StringReader(sb.toString());
		return(retval);
	}

	/**
	 * Read, parse and execute commands.
	 * Can be called repeatedly to interpret many files.
	 * Graphics state and variables are retained between calls.
	 * An interpreter cannot be used again if it throws an exception.
	 * @param commands lines of commands to interpret.
	 * @param stdout stream to write stdout of interpreter into.
	 * @throws IOException if reading or writing files fails.
	 * @throws MapyrusException if there is an error interpreting commands.
	 */
	public void interpret(String []commands, PrintStream stdout)
		throws IOException, MapyrusException
	{
		/*
		 * Convert commands into a reader that can be parsed one
		 * character at a time.
		 */
		StringReader sr = makeStringReader(commands);
		FileOrURL f = new FileOrURL(sr, "commands");

		ColorDatabase.load();
		mInterpreter.interpret(mContext, f, stdout);
	}

	/**
	 * Set output page for Mapyrus interpreter.
	 * This enables Mapyrus to draw into a buffer that an application
	 * later displays in a window.
	 * @param image buffered image to use as initial output page.
	 * @param extras extras settings for output page.
	 * @throws IOException
	 * @throws MapyrusException if there is an error accessing image.
	 */
	public void setPage(BufferedImage image, String extras)
		throws IOException, MapyrusException
	{
		if (extras == null)
			extras = "";
		mContext.setOutputFormat(image, extras);
	}

	/**
	 * Flush any pending output to an output file and close output file.
	 * Close any dataset being accessed.
	 * @throws IOException
	 * @throws MapyrusException
	 */
	public void close() throws IOException, MapyrusException
	{
		try
		{
			if (mContext != null)
				mContext.closeContextStack();
		}
		catch (IOException e)
		{
			throw e;
		}
		catch (MapyrusException e)
		{
			throw e;
		}
		finally
		{
			/*
			 * Always clears fields so we only attempt a close once.
			 */	
			mContext = null;
			mInterpreter = null;
		}
	}

	/**
	 * Show software version number and usage message, then exit.
	 */
	private static void printUsageAndExit()
	{		
		String []usage =
		{
			"Usage:",
			"java [-Dvariable=value] ... -classpath " + Constants.PROGRAM_NAME.toLowerCase() + ".jar org.mapyrus.Mapyrus",
			"        [options] filename ...",
			"",
			Constants.PROGRAM_NAME + " reads each file or URL in turn.",
			"If filename is '-' then standard input is read.",
			"",
			"Variables and configuration are passed to " + Constants.PROGRAM_NAME + " using the",
			"Java -D option.",
			"",
			"Options:",
			"  -s <port> starts " + Constants.PROGRAM_NAME + " as a self-contained HTTP server on the",
			"            given port.  Refer to manual for detailed instructions.",
			"  -v        print version information and exit",
			"  -h        print this message"
		};

		String []license =
		{
			Constants.PROGRAM_NAME + " comes with ABSOLUTELY NO WARRANTY, not even for",
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
			"You may redistribute copies of " + Constants.PROGRAM_NAME + " under the terms",
			"of the GNU Lesser General Public License.  For more information",
			"about these matters, see the file named COPYING."
		};

		System.out.println(Constants.PROGRAM_NAME + " version " +
			Constants.getVersion() + " " +
			Constants.getReleaseDate() +
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
		
		System.out.println("");
		System.out.println("Report bugs to <" + Constants.PROGRAM_NAME.toLowerCase() +
			"@chenery.id.au>.");
		System.exit(1);
	}

	/**
	 * Parse and interpret commands from a file.  Trap any exceptions.
	 * @param context is context to use during interpretation.
	 * @param f open file or URL to read.
	 * @param interpreter interpreter in which to run commands.
	 * @param closeFile if set to true file is closed after we finish reading it.
	 * @return flag indicating whether interpretation succeeeded.
	 */
	private static boolean processFile(ContextStack context, FileOrURL f,
		Interpreter interpreter, boolean closeFile)
	{
		try
		{
			interpreter.interpret(context, f, System.out);
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
	 * Initialise global settings, color name lookup tables.
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

	/**
	 * Listen on a server socket, accepting and processing HTTP requests.
	 * @param interpreter interpreter to use for
	 * @param port port on which to create socket and listen on.
	 * This function normally runs forever and will only return if server
	 * cannot be started.
	 */
	private static void serveHttp(Interpreter interpreter, int port)
	{
		ServerSocket serverSocket = null;
		Pool interpreterPool;
		HashSet activeThreads;

		/*
		 * Make pool of interpreters available to threads that
		 * handle HTTP requests.
		 */
		interpreterPool = new Pool();
		interpreterPool.put(interpreter);
		for (int i = 1; i < Constants.MAX_HTTP_THREADS; i++)
			interpreterPool.put(interpreter.clone());

		/*
		 * Initialise set of threads that have been started.
		 */
		activeThreads = new HashSet();

		/*
		 * Create a logger for writing errors and information whilst
		 * running as an HTTP server.
		 */
		String className = Mapyrus.class.getName();
		Logger logger = Logger.getLogger(className);
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(new SingleLineFormatter());
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);

		try
		{
			/*
			 * Create socket on given port.  If port was 0 then it
			 * is assigned to any free port number.
			 */
			serverSocket = new ServerSocket(port);
			port = serverSocket.getLocalPort();
		}
		catch (IOException e)
		{
			System.err.println(MapyrusMessages.get(MapyrusMessages.INIT_HTTP_FAILED) +
				": " + e.getMessage());
			return;
		}

		/*
		 * Log startup configuration information or write it to the terminal.
		 */
		String versionMessage = Constants.PROGRAM_NAME + " " +
			Constants.getVersion() + " " +
			Constants.getReleaseDate();
		String acceptingMessage = MapyrusMessages.get(MapyrusMessages.ACCEPTING_HTTP) + ": " + port;
		logger.config(versionMessage);
		logger.config(acceptingMessage);
		if (!logger.isLoggable(Level.CONFIG))
		{
			System.out.println(versionMessage);
			System.out.println(acceptingMessage);
		}

		while (true)
		{
			try
			{
				/*
				 * Listen on socket for next client connection.
				 */
				Socket socket = serverSocket.accept();
				socket.setSoTimeout(Constants.HTTP_SOCKET_TIMEOUT);

				/*
				 * Take a intepreter to handle this request (waiting
				 * until one becomes available, if necessary).
				 * Then start new thread to handle this request.
				 */
				interpreter = (Interpreter)(interpreterPool.get(Constants.HTTP_TIMEOUT));
				if (interpreter == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.HTTP_TIMEOUT));
				}

				HTTPRequest request = new HTTPRequest(socket,
					interpreter, interpreterPool, logger);
				activeThreads.add(request);
				logger.fine(MapyrusMessages.get(MapyrusMessages.STARTED_THREAD) +
					": " + request.getName());
				request.start();

				/*
				 * Join any threads we started that have now finished.
				 */
				Iterator iterator = activeThreads.iterator();
				while (iterator.hasNext())
				{
					HTTPRequest active = (HTTPRequest)(iterator.next());
					if (!active.isAlive())
					{
						/*
						 * Wait for thread to complete, then check if it succeeded.
						 */
						active.join();
						logger.fine(MapyrusMessages.get(MapyrusMessages.JOINED_THREAD) +
							": " + active.getName());
						if (!active.getStatus())
							logger.severe(active.getName() + ": " + active.getErrorMessage());
						iterator.remove();
					}
				}
			}
			catch (IOException e)
			{
				logger.severe(e.getMessage());
			}
			catch (InterruptedException e)
			{
				logger.severe(e.getMessage());
			}
			catch (MapyrusException e)
			{
				logger.severe(e.getMessage());
			}
		}
	}

	/*
	 * Return java version info and it's capabilities.
	 * @return version details.
	 */
	private static String getJavaConfiguration()
	{
		String vendor = System.getProperty("java.vendor");
		if (vendor == null)
			vendor = "null";

		String version = System.getProperty("java.version");
		if (version == null)
			version = "null";

		String javaHome = System.getProperty("java.home");
		if (javaHome == null)
			javaHome = "null";

		return("Java version " + version + " (" + vendor + ") in " + javaHome);
	}

	/**
	 * Parse command line arguments and start processing.
	 * Called when Mapyrus is run as a stand-alone interpreter.
	 * @param args command line arguments.
	 */
	public static void main(String []args)
	{
		FileOrURL f = null;
		ContextStack context;
		int i;
		boolean readingStdin;
		boolean isHttpServer = false;
		int argStartIndex = 0;
		int port = 0;

		/*
		 * Parse command line arguments -- these are the files and URLs
		 * to read commands from.
		 */
		if (args.length == 0 || (args.length == 1 && (args[0].equals("-h") ||
			args[0].equals("--help") || args[0].equals("-?"))))
		{
			/*
			 * Show usage message and quit.
			 */
			printUsageAndExit();
		}
		else if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version")))
		{
			/*
			 * Show version number and quit.
			 */
			System.out.println(Constants.PROGRAM_NAME + " " +
				Constants.getVersion() + " " +
				Constants.getReleaseDate());
			System.out.println(getJavaConfiguration());
			System.exit(1);
		}
		else if (args[0].equals("-s"))
		{
			if (args.length < 2)
			{
				printUsageAndExit();
			}
			try
			{
				port = Integer.parseInt(args[1]);
			}
			catch (NumberFormatException e)
			{
				printUsageAndExit();
			}
			argStartIndex = 2;
			isHttpServer = true;
		}

		initialise();

		context = new ContextStack();
		Interpreter interpreter = new Interpreter();

		i = argStartIndex;
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

	
			if (!processFile(context, f, interpreter, !readingStdin))
				System.exit(1);

			i++;
		}

		/*
		 * Finished off anything being created in this context.
		 */
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

		/*
		 * If we're running as an HTTP server we are now ready to
		 * accept connections and respond to requests from HTTP clients.
		 */
		if (isHttpServer)
		{
			serveHttp(interpreter, port);
			System.exit(1);
		}
		System.exit(0);
	}
}
