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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A request from from an HTTP client that is handled asynchronously.
 * The request is parsed, run in a separate thread and then results
 * are returned to HTTP client.
 */
public class HTTPRequest extends Thread
{
	/*
	 * Keywords and codes received and sent in HTTP headers.
	 */
	public static final String HTTP_KEYWORD = "HTTP/1.0";
	public static final String HTTP_OK_KEYWORD = HTTP_KEYWORD + " 200 OK";
	private static final String HTTP_BAD_KEYWORD = HTTP_KEYWORD + " 400 Bad Request";
	private static final String HTTP_NOT_FOUND_KEYWORD = HTTP_KEYWORD + " 404 Not Found";
	private static final int HTTP_OK_CODE = 200;
	private static final int HTTP_BAD_CODE = 400;
	private static final int HTTP_NOT_FOUND_CODE = 404;

	public static final String CONTENT_TYPE_KEYWORD = "Content-Type";
	private static final String CONTENT_LENGTH_KEYWORD = "Content-Length";
	private static final String GET_REQUEST_KEYWORD = "GET";
	private static final int GET_REQUEST = 1;
	private static final String POST_REQUEST_KEYWORD = "POST";
	private static final int POST_REQUEST = 2;

	private File m_currentDirectory;

	/*
	 * Variable name of array automatically set to contain header
	 * of HTTP request. 
	 */
	public static final String HTTP_HEADER_ARRAY = Constants.PROGRAM_NAME + ".http.header";

	private Pool<Interpreter> m_pool;
	private Interpreter m_interpreter;
	private Socket m_socket;

	/*
	 * The MIME type, filename, image map coordinates and Mapyrus commands
	 * parsed for this request.
	 */
	private String m_mimeType;
	private String m_filename;
	private Point m_imagemapPoint;
	private String m_variables, m_commands;

	/*
	 * Holds return status and any error message from running this thread.
	 */
	private int m_returnStatus;
	private String m_errorMessage;

	/*
	 * Logger to write log messages to.
	 */
	private Logger m_logger;

	/*
	 * Timestamp at which this thread was created.
	 */
	private long m_creationTimeMillis;

	/**
	 * Create new HTTP request.
	 * @param socket HTTP connection from client.
	 * @param interpreter interpreter to use for handling request.
	 * @param interpreterPool pool of interpreters to put interpreter back into when finished.
	 * @param logger logger to write log messages to.
	 */
	public HTTPRequest(Socket socket, Interpreter interpreter,
		Pool<Interpreter> interpreterPool, Logger logger)
	{
		super();
		
		m_socket = socket;
		m_interpreter = interpreter;
		m_pool = interpreterPool;
		m_imagemapPoint = null;
		m_returnStatus = HTTP_OK_CODE;
		m_logger = logger;
		m_creationTimeMillis = System.currentTimeMillis();
		m_currentDirectory = new File(System.getProperty("user.dir"));
	}

	/**
	 * Check whether a word is legal as a variable name.
	 * @param word is word to check.
	 * @return true if word is legal variable name.
	 */
	public static boolean isLegalVariable(String word)
	{
		boolean retval = true;
		
		int len = word.length();
		for (int i = 0; i < len && retval; i++)
		{
			char c = word.charAt(i);
			retval = (Character.isLetterOrDigit(c) || (c == '$' && i == 0) || ((c == '.' || c == '_' || c == ':') && i > 0));
		}
		return(retval);
	}

	/**
	 * Append variable definition to string.
	 * @param sb buffer to append string to.
	 * @param varName name of variable.
	 * @param value variable value.
	 * @return string with variable appended.
	 */
	public static StringBuilder addVariable(StringBuilder sb, String varName, String value)
	{
		/*
		 * Add Mapyrus command to set variable,
		 * using uppercase for all variable names.
		 */
		sb.append("let ");
		sb.append(varName);
		sb.append("='");

		int valueLength = value.length();
		for (int i = 0; i < valueLength; i++)
		{
			/*
			 * Escape any single quote characters in value.
			 */
			char c = value.charAt(i);
			if (c == '\'')
				sb.append('\\');
			sb.append(c);

			/*
			 * Escape any 'include' word at start of next line so it does not result
			 * in a file being included.
			 */
			if (c == '\r' || c == '\n')
			{
				if (i + 1 < valueLength && value.charAt(i + 1) == 'i')
					sb.append('\\');
			}
		}
		sb.append("'");
		sb.append(Constants.LINE_SEPARATOR);
		return(sb);
	}

	/**
	 * Parse variables given in HTML form format: var1=val&va2=val.
	 * @param form HTML form to parse.
	 * @return string containing Mapyrus commands to set each variable.
	 */
	private StringBuilder parseForm(String form) throws MapyrusException, IOException
	{
		StringTokenizer st;
		StringBuilder retval = new StringBuilder(form.length() * 2);

		/*
		 * Parse any imagemap coordinates like foo.map?144,75
		 * from the end of the URL string.
		 */
		int questionIndex = form.lastIndexOf('?');
		if (questionIndex >= 0)
		{
			try
			{
				String imageMapCoords = form.substring(questionIndex + 1);
				form = form.substring(0, questionIndex);

				st = new StringTokenizer(imageMapCoords, ",");
				if (st.countTokens() == 2)
				{
					int x = Integer.parseInt(st.nextToken());
					int y = Integer.parseInt(st.nextToken());
					m_imagemapPoint = new Point(x, y);
				}
			}
			catch (NumberFormatException e)
			{
				/*
				 * Just ignore garbled imagemap coordinates that do not make sense.
				 */
			}
		}

		st = new StringTokenizer(form, "&");

		/*
		 * From a request like: x1=11&y1=48&x2=12&y2=49&label=on
		 * create a string of commands for Mapyrus to interpret:
		 * x1='11'
		 * y1='48'
		 * x2='12'
		 * y2='49'
		 * label='on'
		 */
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			token = URLDecoder.decode(token, "UTF-8");
	
			int equalsIndex = token.indexOf('=');
			if (equalsIndex > 0)
			{
				String var = token.substring(0, equalsIndex);
				String value = token.substring(equalsIndex + 1);
	
				if (!isLegalVariable(var))
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED) +
						": " + token);
				}

				/*
				 * Add Mapyrus command to set variable,
				 * using uppercase for all variable names.
				 */
				addVariable(retval, var.toUpperCase(), value);
			}
		}
		return(retval);
	}

	/**
	 * Return filename and options read from header of HTTP request.
	 * @param reader opened socket from which HTTP header is to be read.
	 */
	private void parseRequest(BufferedReader reader)
		throws IOException, MapyrusException
	{
		StringBuilder variables = new StringBuilder();
		StringBuilder commands = new StringBuilder();
		int postRequestLength = 0;
		int requestType;
		String token;

		/*
		 * Read line and see whether it is a GET or POST request.
		 */
		String firstLine = reader.readLine();
		if (firstLine == null)
			firstLine = "";
		if (m_logger.isLoggable(Level.INFO))
		{
			String logMessage = getName() + ": " +
				MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) +
				": " + firstLine;
			m_logger.info(logMessage);
		}

		StringTokenizer st = new StringTokenizer(firstLine);
		if (st.countTokens() < 3)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HTTP_REQUEST) +
				": " + firstLine);
		}

		token = st.nextToken();
		if (token.equals(GET_REQUEST_KEYWORD))
		{
			requestType = GET_REQUEST;
		}
		else if (token.equals(POST_REQUEST_KEYWORD))
		{
			requestType = POST_REQUEST;
		}
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HTTP_REQUEST) +
				": " + firstLine);
		}

		/*
		 * Extract filename to execute and and form variables to be set.
		 */
		String url = st.nextToken();
		if (!url.startsWith("/"))
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HTTP_REQUEST) +
				": " + firstLine);
		}

		int questionIndex = url.indexOf('?');
		if (questionIndex >= 0)
		{	
			m_filename = url.substring(1, questionIndex);
		}
		else
		{
			m_filename = url.substring(1);
		}

		/*
		 * Block access to all files except those in current directory and subdirectories.
		 */
		String filenameSlash = m_filename + "/";
		if (filenameSlash.indexOf("../") >= 0 || filenameSlash.indexOf("..\\") >= 0)
		{
			throw new FileNotFoundException(MapyrusMessages.get(MapyrusMessages.HTTP_NOT_FOUND) +
				": " + m_filename);
		}
		File f = new File(m_currentDirectory, m_filename);
		if (f.isDirectory())
		{
			/*
			 * Try to load index.html in the directory.
			 */
			f = new File(f, "index.html");
			m_filename = f.getPath();
		}

		if ((!f.exists()) || f.isDirectory())
		{
			throw new FileNotFoundException(MapyrusMessages.get(MapyrusMessages.HTTP_NOT_FOUND) +
				": " + m_filename);
		}

		/*
		 * Does file have a known extension like ".html"?  If so,
		 * we should just send back the file instead of trying to
		 * interpret it as commands.
		 */
		int dotIndex = m_filename.lastIndexOf('.');
		if (dotIndex > 0)
		{
			String suffix = m_filename.substring(dotIndex + 1);
			m_mimeType = MimeTypes.get(suffix);
		}
		else
		{
			m_mimeType = null;
		}

		if (m_mimeType == null && questionIndex >= 0 && requestType == GET_REQUEST)
		{
			/*
			 * Parse GET request arguments given after question mark in URL.
			 */
			variables.append(parseForm(url.substring(questionIndex + 1)));
		}

		/*
		 * Read rest of HTTP header.
		 */
		String nextLine = reader.readLine();
		while (nextLine != null && nextLine.length() > 0)
		{
			if (m_logger.isLoggable(Level.FINER))
			{
				m_logger.finer(getName() + ": " +
					MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) + ": " + nextLine);
			}

			int colonIndex = nextLine.indexOf(':');
			if (colonIndex >= 0)
			{
				/*
				 * Create array containing HTTP request header information.
				 */
				String keyword = nextLine.substring(0, colonIndex);
				String value = nextLine.substring(colonIndex + 1).trim();
				int keywordLength = keyword.length();
				int i = 0;
				boolean isValidKeyword = true;
				while (i < keywordLength)
				{
					char c = keyword.charAt(i);
					if (!(Character.isLetterOrDigit(c) || c == '-'))
						isValidKeyword = false;
					i++;
				}
				if (isValidKeyword)
					addVariable(variables, HTTP_HEADER_ARRAY + "['" + keyword + "']", value);

				if (keyword.equals(CONTENT_LENGTH_KEYWORD))
				{
					try
					{
						postRequestLength = Integer.parseInt(value);
					}
					catch (NumberFormatException e)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HTTP_REQUEST) +
							": " + nextLine);
					}
				}
			}
			nextLine = reader.readLine();
		}

		if (requestType == POST_REQUEST)
		{
			/*
			 * Read POST request form data that follows the HTTP header.
			 */
			StringBuilder sb = new StringBuilder(postRequestLength);
			for (int i = 0; i < postRequestLength; i++)
			{
				int c = reader.read();
				if (c < 0)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.MISSING_HTTP_POST));
				sb.append((char)c);
			}
			if (m_logger.isLoggable(Level.FINE))
			{
				m_logger.fine(getName() + ": " +
					MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) + ": " + sb.toString());
			}
			variables.append(parseForm(sb.toString()));
		}
		m_variables = variables.toString();

		if (m_mimeType == null)
		{
			/*
			 * File type not known, so interpret the file with Mapyrus.
			 */
			commands.append("include ");
			commands.append(m_filename);
			commands.append(Constants.LINE_SEPARATOR);
			m_commands = commands.toString();
		}
	}

	/**
	 * Convert exception to a string.
	 * @param e exception to convert.
	 * @return exception as a string with exception message, type
	 * and stack trace.
	 */
	private String exceptionToString(Exception e)
	{
		StringBuilder sb = new StringBuilder();

		String msg = e.getMessage();
		if (msg != null)
			sb.append(e.getMessage()).append(Constants.LINE_SEPARATOR);

		sb.append(e.getClass().getName());
		sb.append(Constants.LINE_SEPARATOR);
		StackTraceElement []stack = e.getStackTrace();
		for (int i = 0; i < stack.length; i++)
		{
			msg = stack[i].toString();
			if (msg != null)
				sb.append(msg).append(Constants.LINE_SEPARATOR);
		}
		return(sb.toString());
	}

	@Override
	public void run()
	{
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		BufferedOutputStream outStream = null;
		BufferedReader inReader = null;
		BufferedInputStream inStream = null;
		String reply;
		String httpResponse = null;

		/*
		 * Read and parse and execute HTTP request from an HTTP client.
		 */
		try
		{
			inReader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
			try
			{
				parseRequest(inReader);
			}
			catch (FileNotFoundException e)
			{
				/*
				 * User asked for a file that does not exist, or is
				 * outside directories being served.
				 */
				m_returnStatus = HTTP_NOT_FOUND_CODE;
				m_errorMessage = e.getMessage();
			}

			if (m_returnStatus == HTTP_NOT_FOUND_CODE)
			{

			}
			else if (m_mimeType == null)
			{
				/*
				 * Send commands to Mapyrus to interpret and capture their output.
				 */
				FileOrURL f1 = new FileOrURL(new StringReader(m_variables), getName());
				FileOrURL f2 = new FileOrURL(new StringReader(m_commands), getName());
				ContextStack context = new ContextStack();
				if (m_imagemapPoint != null)
				{
					context.setImagemapPoint(m_imagemapPoint);
				}
				PrintStream printStream = new PrintStream(byteArrayStream);

				try
				{
					byte []emptyBuffer = new byte[0];
					ByteArrayInputStream emptyStdin = new ByteArrayInputStream(emptyBuffer);
					
					/*
					 * Run commands to set variables, then run commands to generate output.
					 */
					m_interpreter.getThrottle().restart();
					m_interpreter.interpret(context, f1, emptyStdin, null);
					m_interpreter.interpret(context, f2, emptyStdin, printStream);
					httpResponse = context.getHTTPResponse().trim() +
						Constants.LINE_SEPARATOR + Constants.LINE_SEPARATOR;
					context.closeContextStack();
					context = null;
				}
				finally
				{
					/*
					 * Ensure that context is always closed.
					 */
					try
					{
						if (context != null)
							context.closeContextStack();
					}
					catch (IOException e)
					{
					}
					catch (MapyrusException e)
					{
					}
				}

				printStream.flush();
			}
			else
			{
				/*
				 * Open plain file to be returned to client.
				 */
				inStream = new BufferedInputStream(new FileInputStream(m_filename));
			}
		}
		catch (IOException e)
		{
			m_returnStatus = HTTP_BAD_CODE;
			m_errorMessage = e.getMessage();
		}
		catch (MapyrusException e)
		{
			m_returnStatus = HTTP_BAD_CODE;
			m_errorMessage = e.getMessage();
		}
		catch (Exception e)
		{
			/*
			 * Any other type of exception (such as a NullPointerException)
			 * indicates a bug.  Return all information to client so
			 * problem can be pinpointed.
			 */
			m_returnStatus = HTTP_BAD_CODE;
			m_errorMessage = exceptionToString(e);
		}

		try
		{
			/*
			 * Now send output from request (or an error message
			 * explaining why it could be completed) to the HTTP client.
			 */
			outStream = new BufferedOutputStream(m_socket.getOutputStream());
			if (m_returnStatus == HTTP_OK_CODE)
			{
				if (m_mimeType == null)
				{
					reply = httpResponse;
				}
				else
				{
					reply = HTTP_OK_KEYWORD + Constants.LINE_SEPARATOR +
						CONTENT_TYPE_KEYWORD + ": " + m_mimeType +
						Constants.LINE_SEPARATOR +
						Constants.LINE_SEPARATOR;
				}

				if (m_logger.isLoggable(Level.FINE))
				{
					/*
					 * Log each line of HTTP header.
					 */
					StringTokenizer st = new StringTokenizer(reply, Constants.LINE_SEPARATOR);
					while (st.hasMoreTokens())
					{
						String token = st.nextToken();
						m_logger.fine(getName() + ": " +
							MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " + token);
					}
				}

				outStream.write(reply.getBytes());

				if (m_mimeType == null)
				{
					/*
					 * Write output of interpreter back to HTTP client.
					 */
					byteArrayStream.writeTo(outStream);

					if (m_logger.isLoggable(Level.FINE))
					{
						m_logger.fine(getName() + ": " +
							MapyrusMessages.get(MapyrusMessages.HTTP_RETURNED) +
							": " + byteArrayStream.size());
					}
				}
				else
				{
					/*
					 * Spool requested file back to HTTP client.
					 */
					int counter = 0;
					byte []buf = new byte[512];
					int nBytes = inStream.read(buf);

					while (nBytes > 0)
					{
						outStream.write(buf, 0, nBytes);
						counter += nBytes;
						nBytes = inStream.read(buf);
					}

					if (m_logger.isLoggable(Level.FINE))
					{
						m_logger.fine(getName() + ": " +
							MapyrusMessages.get(MapyrusMessages.HTTP_RETURNED) + ": " + counter);
					}
				}
			}
			else
			{
				String contentType = MimeTypes.get("txt");

				String result = (m_returnStatus == HTTP_NOT_FOUND_CODE) ? HTTP_NOT_FOUND_KEYWORD : HTTP_BAD_KEYWORD;
				if (m_logger.isLoggable(Level.FINE))
				{
					m_logger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " + result);
					m_logger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " +
						CONTENT_TYPE_KEYWORD + ": " + contentType);
				}

				reply = result + Constants.LINE_SEPARATOR +
					CONTENT_TYPE_KEYWORD + ": " + contentType + Constants.LINE_SEPARATOR +
					Constants.LINE_SEPARATOR +
					m_errorMessage + Constants.LINE_SEPARATOR;
				outStream.write(reply.getBytes());
			}
			outStream.flush();
		}
		catch (IOException e)
		{
			if (m_returnStatus == HTTP_OK_CODE)
			{
				m_returnStatus = HTTP_BAD_CODE;
				m_errorMessage = e.toString();
			}
		}
		finally
		{
			/*
			 * Make sure socket to HTTP client is closed in all circumstances.
			 */
			try
			{
				if (outStream != null)
					outStream.close();
			}
			catch (IOException e2)
			{
			}

			try
			{
				if (inReader != null)
					inReader.close();
			}
			catch (IOException e2)
			{
			}

			try
			{
				if (m_socket != null)
					m_socket.close();
			}
			catch (IOException e2)
			{
			}

			/*
			 * Make sure any file being read by this request is closed
			 * in all circumstances.
			 */
			try
			{
				if (inStream != null)
					inStream.close();
			}
			catch (IOException e2)
			{
			}
		}

		/*
		 * Return interpreter to the pool for use by someone else.
		 */
		m_pool.put(m_interpreter);
	}

	/**
	 * Indicates whether thread to process HTTP request succeeded or failed.
	 * @return true if thread completed successfully.
	 */
	public boolean getStatus()
	{
		return(m_returnStatus == HTTP_OK_CODE);
	}

	/**
	 * Returns error message describing why processing the HTTP request failed.
	 * @return error message.
	 */
	public String getErrorMessage()
	{
		return(m_errorMessage);
	}


	/**
	 * Return timestamp at which thread was created.
	 * @return time stamp in milliseconds.
	 */
	public long getCreationTime()
	{
		return(m_creationTimeMillis);
	}
}
