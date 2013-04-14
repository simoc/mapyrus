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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Connects Mapyrus to Tomcat web server, enabling Mapyrus to run as a servlet in
 * a web application.
 */
public class MapyrusServlet extends HttpServlet
{
	static final long serialVersionUID = 0x3303;
	private static final String COMMANDS_PARAM_NAME = "commands";

	/**
	 * Handle HTTP GET request from web browser.
	 * @param request HTTP request
	 * @param response HTTP response
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		/*
		 * Parameter in the HTTP request containing Mapyrus commands.
		 */
		String paramName = COMMANDS_PARAM_NAME;
		String paramValue = null;

		/*
		 * Generate Mapyrus commands to set variables from HTTP request parameters,
		 * using uppercase for all variable names.
		 */
		StringBuffer variables = new StringBuffer(512);
		Map<String, String[]> parameters = request.getParameterMap();
		Iterator<String> it = parameters.keySet().iterator();

		while (it.hasNext())
		{
			String var = it.next().toString();
			String []value = ((String [])parameters.get(var));
			if (!HTTPRequest.isLegalVariable(var))
			{
				throw new ServletException(MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED) +
					": " + var);
			}
			variables = HTTPRequest.addVariable(variables, var.toUpperCase(), value[0]);

			if (var.equalsIgnoreCase(paramName))
				paramValue = value[0];
		}

		/*
		 * Check that we have some Mapyrus commands to run.
		 */
		if (paramValue == null || paramValue.length() == 0)
		{
			throw new ServletException(MapyrusMessages.get(MapyrusMessages.NO_COMMANDS) +
				": " + paramName);
		}

		/*
		 * Create array containing HTTP request header information.
		 */
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements())
		{
			String var = (String)headerNames.nextElement();
			String value = request.getHeader(var);
			if (HTTPRequest.isLegalVariable(var))
				HTTPRequest.addVariable(variables, HTTPRequest.HTTP_HEADER_ARRAY + "['" + var + "']", value);
		}

		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(byteArrayStream);

		String servletPath = request.getServletPath();
		FileOrURL f1 = new FileOrURL(new StringReader(variables.toString()), servletPath);
		FileOrURL f2 = new FileOrURL(new StringReader(paramValue), servletPath);
		ContextStack context = new ContextStack();
		byte []emptyBuffer = new byte[0];
		ByteArrayInputStream emptyStdin = new ByteArrayInputStream(emptyBuffer);

		try
		{
			Interpreter interpreter = new Interpreter();
			
			/*
			 * Run commands to set variables, then run commands to generate output.
			 */
			Throttle throttle = new Throttle();
			throttle.setMaxTime(Constants.MAX_HTTP_REQUEST_TIME);

			/*
			 * Disable file access from servlet for better security unless
			 * servlet init-param "io" is set.
			 */
			String s = getInitParameter("io");
			boolean isIOAllowed = Boolean.valueOf(s);
			throttle.setIOAllowed(isIOAllowed);
			interpreter.setThrottle(throttle);
			interpreter.interpret(context, f1, emptyStdin, null);
			interpreter.interpret(context, f2, emptyStdin, printStream);
			String responseHeader = context.getHTTPResponse().trim();
			context.closeContextStack();
			context = null;

			/*
			 * Send HTTP response header back to client, followed by content.
			 */
			String contentType = null;
			BufferedReader reader = new BufferedReader(new StringReader(responseHeader));
			String nextLine;
			while ((nextLine = reader.readLine()) != null)
			{
				int index = 0;
				while (index < nextLine.length() && !Character.isWhitespace(nextLine.charAt(index)))
					index++;
				if (index < nextLine.length())
				{
					String var = nextLine.substring(0, index);
					String value = nextLine.substring(index).trim();

					if (var.endsWith(":"))
						var = var.substring(0, var.length() - 1);
					if (var.equals(HTTPRequest.CONTENT_TYPE_KEYWORD))
					{
						/*
						 * A special method exists for setting content type. 
						 */
						contentType = value;
					}
					else if (!var.startsWith(HTTPRequest.HTTP_KEYWORD))
					{
						/*
						 * Do not set "HTTP/1.0 OK" line.  Tomcat will set this itself.
						 */
						response.setHeader(var, value);
					}
				}
			}
			if (contentType != null)
				response.setContentType(contentType);
			byteArrayStream.writeTo(response.getOutputStream());
		}
		catch (MapyrusException e)
		{
			throw new ServletException(e.getMessage());
		}
		catch (InterruptedException e)
		{
			throw new ServletException(e.getMessage());
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
	}

	/**
	 * Handle HTTP POST request from web browser.
	 * @param request HTTP request
	 * @param response HTTP response
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		doGet(request, response);
	}	
}
