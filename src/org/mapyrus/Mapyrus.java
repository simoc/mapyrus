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

package org.mapyrus;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapyrus.gui.MapyrusFrame;
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
	private static final String OUT_OF_MEMORY_MESSAGE = "Out of memory.  Use Java -Xmx option to increase memory\navailable to Mapyrus.  For example, java -Xmx256m -classpath ...\n";

	private Interpreter m_interpreter;
	private ContextStack m_context;

	/**
	 * Create new interpreter.
	 */
	public Mapyrus()
	{
		m_interpreter = new Interpreter();
		m_context = new ContextStack();
	}

	/**
	 * Create string reader from string array.
	 * @param s string to create reader from.
	 * @return string reader.
	 */
	private StringReader makeStringReader(String []s)
	{
		StringBuilder sb = new StringBuilder();
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
	 * An interpreter cannot be used again after throwing an exception.
	 * @param commands lines of commands to interpret.
	 * @param stdin standard input stream for interpreter.
	 * @param stdout stream to write stdout of interpreter into.
	 * @throws IOException if reading or writing files fails.
	 * @throws InterruptedException if this thread is interrupted.
	 * @throws MapyrusException if there is an error interpreting commands.
	 */
	public void interpret(String []commands, InputStream stdin,
		PrintStream stdout)
		throws IOException, InterruptedException, MapyrusException
	{
		/*
		 * Convert commands into a reader that can be parsed one
		 * character at a time.
		 */
		StringReader sr = makeStringReader(commands);
		FileOrURL f = new FileOrURL(sr, "commands");

		m_interpreter.interpret(m_context, f, stdin, stdout);
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
		m_context.setOutputFormat(image, extras);
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
			if (m_context != null)
				m_context.closeContextStack();
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
			m_context = null;
			m_interpreter = null;
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
			"java [-D<variable>=<value>] ... -classpath " + Constants.PROGRAM_NAME.toLowerCase() + ".jar org.mapyrus.Mapyrus",
			"        [options] filename ...",
			"",
			Constants.PROGRAM_NAME + " reads each file or URL in turn.",
			"If filename is '-' then standard input is read.",
			"",
			"Variables are passed to " + Constants.PROGRAM_NAME + " using the Java -D option.",
			"",
			"Options:",
			"  -e <commands> runs given commands instead of reading commands from a file",
			"  -g            starts Mapyrus GUI for each filename",
			"  -h            print this message",
			"  -l <level>    sets logging level for HTTP server.  One of ",
			"                FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE.",
			"  -r <percent>:<maxtime> restricts CPU usage.  percent value in",
			"                range 1-100 gives maximum CPU load, a lower value gives",
			"                other processes more time to run.  maxtime defines",
			"                the maximum number of seconds to run for.  " + Constants.PROGRAM_NAME + " will",
			"                exit with error if it runs for longer.",
			"  -s <port>     starts " + Constants.PROGRAM_NAME + " as a self-contained HTTP server on the",
			"                given port.  Refer to manual for detailed instructions.",
			"  -v            print version information and exit",
		};

		String []license = Constants.getLicense();

		for (int i = 0; i < usage.length; i++)
		{
			System.out.println(usage[i]);
		}

		System.out.println("");
		for (int i = 0; i < license.length; i++)
		{
			System.out.println(license[i]);
		}
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
			interpreter.interpret(context, f, System.in, System.out);
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
		catch (OutOfMemoryError e)
		{
			/*
			 * Tell user to make more memory available.
			 * Use literal strings, do not look them up in property file as
			 * this may fail if no more memory is available.
			 */
			System.err.println(OUT_OF_MEMORY_MESSAGE);
			e.printStackTrace();
			return(false);
		}
		return(true);
	}

	/**
	 * Wait for a client connection on HTTP server socket.
	 * @param serverSocket socket listening for HTTP requests.
	 * @param logger logger.
	 * @return HTTP client socket connection.
	 */
	private static Socket acceptConnection(ServerSocket serverSocket,
		Logger logger) throws IOException
	{
		Socket clientSocket = null;
		while (clientSocket == null)
		{
			try
			{
				clientSocket = serverSocket.accept();
			}
			catch (SocketTimeoutException e)
			{
				logger.info(MapyrusMessages.get(MapyrusMessages.IDLE));
			}
		}
		return(clientSocket);
	}

	/**
	 * Listen on a server socket, accepting and processing HTTP requests.
	 * @param interpreter interpreter to use for
	 * @param port port on which to create socket and listen on.
	 * @param logLevel logging level for server, or null for default level.
	 * This function normally runs forever and will only return if server
	 * cannot be started.
	 */
	private static void serveHttp(Interpreter interpreter,
		int port, Level logLevel)
	{
		ServerSocket serverSocket = null;
		Pool<Interpreter> interpreterPool;
		HashSet<HTTPRequest> activeThreads;

		/*
		 * Make pool of interpreters available to threads that
		 * handle HTTP requests.
		 */
		interpreterPool = new Pool<Interpreter>();
		interpreterPool.put(interpreter);
		for (int i = 1; i < Constants.MAX_HTTP_THREADS; i++)
			interpreterPool.put((Interpreter)interpreter.clone());

		/*
		 * Initialise set of threads that have been started.
		 */
		activeThreads = new HashSet<HTTPRequest>();

		/*
		 * Create a logger for writing errors and information whilst
		 * running as an HTTP server.
		 */
		String className = Mapyrus.class.getName();
		Logger logger = Logger.getLogger(className);
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(new SingleLineFormatter());
		if (logLevel != null)
		{
			logger.setLevel(logLevel);
			consoleHandler.setLevel(logLevel);
		}
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
		String threadMessage = MapyrusMessages.get(MapyrusMessages.HTTP_THREADED_SERVER) + ": " + Constants.MAX_HTTP_THREADS;
		String acceptingMessage = MapyrusMessages.get(MapyrusMessages.ACCEPTING_HTTP) + ": " + port;
		logger.config(versionMessage);
		logger.config(threadMessage);
		logger.config(acceptingMessage);
		if (!logger.isLoggable(Level.CONFIG))
		{
			System.out.println(versionMessage);
			System.out.println(threadMessage);
			System.out.println(acceptingMessage);
		}

		while (true)
		{
			Socket socket = null;
			try
			{
				/*
				 * Listen on socket for next client connection.
				 */
				socket = acceptConnection(serverSocket, logger);
				socket.setSoTimeout(Constants.HTTP_SOCKET_TIMEOUT);

				/*
				 * Take a intepreter to handle this request (waiting
				 * until one becomes available, if necessary).
				 * Then start new thread to handle this request.
				 */
				interpreter = interpreterPool.get(Constants.HTTP_TIMEOUT);
				if (interpreter == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.HTTP_TIMEOUT));
				}

				HTTPRequest request = new HTTPRequest(socket,
					interpreter, interpreterPool, logger);

				activeThreads.add(request);
				logger.fine(MapyrusMessages.get(MapyrusMessages.STARTED_THREAD) +
					": " + request.getName());

				/*
				 * Forget about socket, the request thread guarantees that it
				 * will be closed.
				 */
				socket = null;
				request.start();

				/*
				 * Join any threads we started that have now finished.
				 */
				Iterator<HTTPRequest> iterator = activeThreads.iterator();
				while (iterator.hasNext())
				{
					HTTPRequest active = (HTTPRequest)(iterator.next());
					if (!active.isAlive())
					{
						/*
						 * Wait for thread to complete,
						 * then check if it succeeded.
						 */
						active.join();
						logger.fine(MapyrusMessages.get(MapyrusMessages.JOINED_THREAD) +
							": " + active.getName());
						if (!active.getStatus())
							logger.severe(active.getName() + ": " + active.getErrorMessage());
						iterator.remove();
					}
					else
					{
						/*
						 * Interrupt any requests that have run for too long.
						 */
						long now = System.currentTimeMillis();
						long age = now - active.getCreationTime();
						if (age > Constants.MAX_HTTP_REQUEST_TIME)
						{
							active.interrupt();
						}
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
			catch (OutOfMemoryError e)
			{
				logger.severe(OUT_OF_MEMORY_MESSAGE);
			}
			finally
			{
				/*
				 * Ensure that socket is always closed.
				 */
				try
				{
					if (socket != null)
						socket.close();
				}
				catch (IOException e)
				{
				}
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
		int argIndex = 0;
		int port = 0;
		Level logLevel = null;
		StringBuilder commandsToExecute = new StringBuilder();
		boolean startGui = false;
		Throttle throttle = null;

		if (args.length == 0)
			startGui = true;

		/*
		 * Parse command line arguments -- these are the files and URLs
		 * to read commands from.
		 */
		while (argIndex < args.length &&
			args[argIndex].startsWith("-") &&
			args[argIndex].length() > 1)
		{
			String arg = args[argIndex];
			if (arg.equals("-h") || arg.equals("--help") ||
				arg.equals("-?"))
			{
				/*
			 	* Show usage message and quit.
			 	*/
				printUsageAndExit();
			}
			else if (arg.equals("-v") || arg.equals("--version"))
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
			else if (arg.equals("-s"))
			{
				if (argIndex + 1 == args.length)
					printUsageAndExit();

				try
				{
					port = Integer.parseInt(args[argIndex + 1]);
				}
				catch (NumberFormatException e)
				{
					System.err.println(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + args[argIndex + 1]);
					printUsageAndExit();
				}

				argIndex += 2;
				isHttpServer = true;
			}
			else if (arg.equals("-e"))
			{
				if (argIndex + 1 == args.length)
					printUsageAndExit();

				/*
				 * Commands to be executed given on command line.
				 */
				commandsToExecute.append(args[argIndex + 1]);
				commandsToExecute.append(Constants.LINE_SEPARATOR);
				argIndex += 2;
			}
			else if (arg.equals("-g"))
			{
				startGui = true;
				argIndex++;
			}
			else if (arg.equals("-l"))
			{
				/*
				 * Set logging level.
				 */
				if (argIndex + 1 == args.length)
					printUsageAndExit();

				try
				{
					logLevel = Level.parse(args[argIndex + 1]);
				}
				catch (IllegalArgumentException e)
				{
					System.err.println(e.getMessage());
					System.exit(1);
				}
				argIndex += 2;
			}
			else if (arg.equals("-r"))
			{
				/*
				 * Set CPU load reduction.
				 */
				if (argIndex + 1 == args.length)
					printUsageAndExit();

				try
				{
					int percentage = 100;
					int maxTime = -1;
					StringTokenizer st = new StringTokenizer(args[argIndex + 1], ":");
					if (st.hasMoreTokens())
						percentage = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens())
						maxTime = Integer.parseInt(st.nextToken()) * 1000;
					throttle = new Throttle();
					if (percentage != 100)
						throttle.setMaxLoad(percentage);
					if (maxTime != -1)
						throttle.setMaxTime(maxTime);
				}
				catch (NumberFormatException e)
				{
					System.err.println(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + args[argIndex + 1]);
					printUsageAndExit();
				}
				argIndex += 2;
			}
			else if (arg.equals("--"))
			{
				/*
				 * "--" marks end of options.
				 */
				argIndex++;
				break;
			}
			else
			{
				/*
				 * Unknown option.
				 */
				System.err.println(MapyrusMessages.get(MapyrusMessages.INVALID_OPTION) + ": " + arg);
				System.exit(1);
			}
		}

		if (startGui)
		{
			/*
			 * Don't run any commands, show GUI instead.
			 */
			String []filenames = null; 
			if (args != null && argIndex < args.length)
			{
				filenames = new String[args.length - argIndex];
				System.arraycopy(args, argIndex, filenames, 0, filenames.length);
			}
			try
			{
				new MapyrusFrame(filenames);
			}
			catch (Exception e)
			{
				System.err.println(e.getMessage());
				System.exit(1);
			}
			System.exit(0);
		}

		context = new ContextStack();
		Interpreter interpreter = new Interpreter();
		if (throttle != null)
		{
			if (isHttpServer && throttle.getMaxTime() < 0)
			{
				/*
				 * Set a reasonable default maximum time for HTTP requests.
				 */
				throttle.setMaxTime(Constants.MAX_HTTP_REQUEST_TIME);
			}
			interpreter.setThrottle(throttle);
		}

		if (commandsToExecute.length() > 0)
		{
			/*
			 * Run commands given as a command line argument.
			 */
			f = new FileOrURL(new StringReader(commandsToExecute.toString()), "-e");
			if (!processFile(context, f, interpreter, false))
			{
				try
				{
					context.closeContextStack();
				}
				catch (Exception e)
				{
					/*
					 * Something was already reported as going wrong.
					 * Do not report any further problems.
					 */
				}
				System.exit(1);
			}
		}
		else
		{
			i = argIndex;
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
				{
					try
					{
						context.closeContextStack();
					}
					catch (Exception e)
					{
						/*
						 * Something was already reported as going wrong.
						 * Do not report any further problems.
						 */
					}
					System.exit(1);
				}

				i++;
			}
		}

		/*
		 * Finish off anything being created in this context.
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
			serveHttp(interpreter, port, logLevel);
			System.exit(1);
		}
		System.exit(0);
	}
}
