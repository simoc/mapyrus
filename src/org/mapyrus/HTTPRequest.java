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

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
	 * Keywords received and sent in HTTP headers.
	 */
	private static final String HTTP_OK_KEYWORD = "HTTP/1.0 200 OK";
	private static final String HTTP_BAD_KEYWORD = "HTTP/1.0 400 Bad Request";
	private static final String CONTENT_TYPE_KEYWORD = "Content-Type";
	private static final String CONTENT_LENGTH_KEYWORD = "Content-Length";
	private static final String SERVER_KEYWORD = "Server";
	private static final String GET_REQUEST_KEYWORD = "GET";
	private static final int GET_REQUEST = 1;
	private static final String POST_REQUEST_KEYWORD = "POST";
	private static final int POST_REQUEST = 2;

	private Pool mPool;
	private Interpreter mInterpreter;
	private Socket mSocket;

	/*
	 * The MIME type, filename, image map coordinates and Mapyrus commands
	 * parsed for this request.
	 */
	private String mMimeType;
	private String mFilename;
	private Point mImagemapPoint;
	private String mCommands;

	/*
	 * Holds return status and any error message from running this thread.
	 */
	private boolean mReturnStatus;
	private String mErrorMessage;

	/*
	 * Logger to write log messages to.
	 */
	private Logger mLogger;

	/**
	 * Create new HTTP request.
	 * @param socket HTTP connection from client.
	 * @param interpreter interpreter to use for handling request.
	 * @param interpreterPool pool of interpreters to put interpreter back into when finished.
	 * @param logger logger to write log messages to.
	 */
	public HTTPRequest(Socket socket, Interpreter interpreter,
		Pool interpreterPool, Logger logger)
	{
		super();
		
		mSocket = socket;
		mInterpreter = interpreter;
		mPool = interpreterPool;
		mImagemapPoint = null;
		mReturnStatus = true;
		mLogger = logger;
	}

	/**
	 * Check whether a word is legal as a variable name.
	 * @param word is word to check.
	 * @return true if word is legal variable name.
	 */
	private boolean isLegalVariable(String word)
	{
		boolean retval = true;
		
		int len = word.length();
		for (int i = 0; i < len && retval; i++)
		{
			char c = word.charAt(i);
			retval = (Character.isLetterOrDigit(c) || (c == '$' && i == 0) || c == '.' || c == '_');
		}
		return(retval);
	}

	/**
	 * Parse variables given in HTML form format: var1=val&va2=val.
	 * @param form HTML form to parse.
	 * @return string containing Mapyrus commands to set each variable.
	 */
	private StringBuffer parseForm(String form) throws MapyrusException, IOException
	{
		StringTokenizer st;
		StringBuffer retval = new StringBuffer(form.length() * 2);

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
					mImagemapPoint = new Point(x, y);
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
				 * Add Mapyrus command to set variable.
				 */
				retval.append("let ");
				retval.append(var);
				retval.append("='");
						
				int valueLength = value.length();
				for (int i = 0; i < valueLength; i++)
				{
					/*
					 * Escape any single quote characters in value.
					 */
					char c = value.charAt(i);
					if (c == '\'')
						retval.append('\\');
					retval.append(c);
				}
				retval.append("'");
				retval.append(Constants.LINE_SEPARATOR);
			}
		}
		return(retval);
	}

	/**
	 * Return filename and options read from header of HTTP request.
	 * @param reader opened socket from which HTTP header is to be read.
	 * @return filename requested in HTTP header.
	 */
	private void parseRequest(BufferedReader reader)
		throws IOException, MapyrusException
	{
		StringBuffer commands = new StringBuffer();
		int postRequestLength = 0;
		int requestType;
		String token;

		/*
		 * Read line and see whether it is a GET or POST request.
		 */
		String firstLine = reader.readLine();
		if (mLogger.isLoggable(Level.FINE))
		{
			mLogger.fine(getName() + ": " +
				MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) + ": " + firstLine);
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
			mFilename = url.substring(1, questionIndex);
		}
		else
		{
			mFilename = url.substring(1);
		}

		/*
		 * Block access to all files except those in current directory.
		 */
		File f = new File(mFilename);
		if(mFilename.indexOf(File.separatorChar) >= 0 || mFilename.indexOf('/') >= 0 ||
			mFilename.indexOf('\\') >= 0 || (!f.exists()))
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.HTTP_NOT_FOUND) +
							": " + mFilename);
		}

		/*
		 * Does file have a known extension like ".html"?  If so,
		 * we should just send back the file instead of trying to
		 * interpret it as commands.
		 */
		int dotIndex = mFilename.lastIndexOf('.');
		if (dotIndex > 0)
		{
			String suffix = mFilename.substring(dotIndex + 1);
			mMimeType = MimeTypes.get(suffix);
		}
		else
		{
			mMimeType = null;
		}

		if (mMimeType == null && questionIndex >= 0 && requestType == GET_REQUEST)
		{
			/*
			 * Parse GET request arguments given after question mark in URL.
			 */
			commands.append(parseForm(url.substring(questionIndex + 1)));
		}

		/*
		 * Read rest of HTTP header.
		 */
		String nextLine = reader.readLine();
		while (nextLine.length() > 0)
		{
			if (mLogger.isLoggable(Level.FINER))
			{
				mLogger.finer(getName() + ": " +
					MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) + ": " + nextLine);
			}

			if (nextLine.startsWith(CONTENT_LENGTH_KEYWORD + ":"))
			{
				try
				{
					String s = nextLine.substring(CONTENT_LENGTH_KEYWORD.length() + 1);
					postRequestLength = Integer.parseInt(s.trim());
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HTTP_REQUEST) +
						": " + nextLine);
				}
			}
			nextLine = reader.readLine();
		}

		if (requestType == POST_REQUEST)
		{
			/*
			 * Read POST request form data that follows the HTTP header.
			 */
			StringBuffer sb = new StringBuffer(postRequestLength);
			for (int i = 0; i < postRequestLength; i++)
			{
				int c = reader.read();
				if (c < 0)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.MISSING_HTTP_POST));
				sb.append((char)c);
			}
			if (mLogger.isLoggable(Level.FINE))
			{
				mLogger.fine(getName() + ": " +
					MapyrusMessages.get(MapyrusMessages.HTTP_HEADER) + ": " + sb.toString());
			}
			commands.append(parseForm(sb.toString()));
		}

		if (mMimeType == null)
		{
			/*
			 * File type not known, so interpret the file with Mapyrus.
			 */
			commands.append("include ");
			commands.append(mFilename);
			commands.append(Constants.LINE_SEPARATOR);
			mCommands = commands.toString();
		}
	}

	public void run()
	{
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		BufferedOutputStream outStream = null;
		BufferedReader inReader;
		BufferedInputStream inStream = null;
		String reply;
		String contentType;

		/*
		 * Read and parse and execute HTTP request from an HTTP client.
		 */
		try
		{
			inReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			parseRequest(inReader);
			if (mMimeType == null)
			{
				/*
				 * Send commands to Mapyrus to interpret and capture their output.
				 */
				FileOrURL f = new FileOrURL(new StringReader(mCommands), getName());
				ContextStack context = new ContextStack();
				if (mImagemapPoint != null)
				{
					context.setImagemapPoint(mImagemapPoint);
				}
				PrintStream printStream = new PrintStream(byteArrayStream);
				mInterpreter.interpret(context, f, printStream);
				context.closeContextStack();
				printStream.flush();
			}
			else
			{
				/*
				 * Open plain file to be returned to client.
				 */
				inStream = new BufferedInputStream(new FileInputStream(mFilename));
			}
		}
		catch (IOException e)
		{
			mReturnStatus = false;
			mErrorMessage = e.getMessage();
		}
		catch (MapyrusException e)
		{
			mReturnStatus = false;
			mErrorMessage = e.getMessage();
		}

		try
		{
			/*
			 * Now send output from request (or an error message
			 * explaining why it could be completed) to the HTTP client.
			 */
			outStream = new BufferedOutputStream(mSocket.getOutputStream());
			if (mReturnStatus)
			{
				if (mMimeType != null)
					contentType = mMimeType;
				else
					contentType = mInterpreter.getContentType();

				if (mLogger.isLoggable(Level.FINE))
				{
					mLogger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " + HTTP_OK_KEYWORD);
					mLogger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " +
						CONTENT_TYPE_KEYWORD + ": " + contentType);
				}

				reply = HTTP_OK_KEYWORD + Constants.LINE_SEPARATOR +
					CONTENT_TYPE_KEYWORD + ": " + contentType +
					Constants.LINE_SEPARATOR +
					SERVER_KEYWORD + ": " + Constants.PROGRAM_NAME + "/" +
					Constants.getVersion() + Constants.LINE_SEPARATOR +
					Constants.LINE_SEPARATOR;

				outStream.write(reply.getBytes());
				
				if (mMimeType == null)
				{
					/*
					 * Write output of interpreter back to HTTP client.
					 */
					byteArrayStream.writeTo(outStream);

					if (mLogger.isLoggable(Level.FINE))
					{
						mLogger.fine(getName() + ": " +
							MapyrusMessages.get(MapyrusMessages.HTTP_RETURNED) +
							": " + byteArrayStream.size());
					}
				}
				else
				{
					/*
					 * Spool requested file back to HTTP client.
					 */
					int c = inStream.read();
					int counter = 0;
					while (c >= 0)
					{
						outStream.write(c);
						counter++;
						c = inStream.read();
					}

					if (mLogger.isLoggable(Level.FINE))
					{
						mLogger.fine(getName() + ": " +
							MapyrusMessages.get(MapyrusMessages.HTTP_RETURNED) + ": " + counter);
					}

					inStream.close();
					inStream = null;
				}
			}
			else
			{
				contentType = MimeTypes.get("txt");

				if (mLogger.isLoggable(Level.FINE))
				{
					mLogger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " + HTTP_BAD_KEYWORD);
					mLogger.fine(getName() + ": " +
						MapyrusMessages.get(MapyrusMessages.HTTP_RETURN) + ": " +
						CONTENT_TYPE_KEYWORD + ": " + contentType);
				}

				reply = HTTP_BAD_KEYWORD + Constants.LINE_SEPARATOR +
					CONTENT_TYPE_KEYWORD + ": " + contentType + Constants.LINE_SEPARATOR +
					SERVER_KEYWORD + ": " + Constants.PROGRAM_NAME + "/" +
					Constants.getVersion() + Constants.LINE_SEPARATOR +
					Constants.LINE_SEPARATOR +
					mErrorMessage + Constants.LINE_SEPARATOR;
				outStream.write(reply.getBytes());
			}

			outStream.flush();
			mSocket.close();
			mSocket = null;
		}
		catch (IOException e)
		{
			if (mReturnStatus)
			{
				mReturnStatus = false;
				mErrorMessage = e.toString();
			}

			/*
			 * Make sure socket to HTTP client is closed in all circumstances.
			 */			
			try
			{
				if (mSocket != null)
					mSocket.close();
			}
			catch (IOException e2)
			{
			}
			
			try
			{
				/*
				 * Make sure any file being read by this request is closed
				 * in all circumstances.
				 */
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
		mPool.put(mInterpreter);
	}

	/**
	 * Indicates whether thread to process HTTP request succeeded or failed.
	 * @return true if thread completed successfully.
	 */
	public boolean getStatus()
	{
		return(mReturnStatus);
	}

	/**
	 * Returns error message describing why processing the HTTP request failed.
	 * @return error message.
	 */
	public String getErrorMessage()
	{
		return(mErrorMessage);
	}
}
