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
package org.mapyrus;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Wrapper around java.io.FileInputStream and java.net.URL classes to present
 * uniform interface for opening, closing and getting InputStream to read from.
 */
public class FileOrURL
{
	private String m_name;
	private URL m_URL;
	private BufferedInputStream m_inputStream;
	private LineNumberReader m_reader;
	private boolean m_isURL;

	/**
	 * Open file or URL.
	 * @param name name of file or URL to open.
	 * @throws IOException
	 */
	public FileOrURL(String name) throws IOException, MapyrusException
	{
		init(name, null);
	}

	/**
	 * Open file or URL.
	 * @param name name of file or URL to open.
	 * @param charsetName character set encoding of file or URL.
	 * @throws IOException
	 */
	public FileOrURL(String name, String charsetName) throws IOException, MapyrusException
	{
		init(name, charsetName);
	}

	private void init(String name, String charsetName) throws IOException, MapyrusException
	{
		InputStream in;

		try
		{
			/*
			 * First try opening as an URL.
			 */
			m_URL = new URL(name);
			try
			{
				in = m_URL.openStream();
			}
			catch (IOException e)
			{
				/*
				 * The IOException error message is not very helpful.  Throw our own exception.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CANNOT_OPEN_URL) +
					": " +	m_URL.toString() + Constants.LINE_SEPARATOR + e.getMessage());
			}
			m_isURL = true;
		}
		catch (MalformedURLException e)
		{
			/*
			 * It is not a valid URL, try opening it as a plain file instead.
			 */
			try
			{
				in = new FileInputStream(name);
			}
			catch (SecurityException e2)
			{
				throw new IOException(e2.getClass().getName() + ": " + e2.getMessage() + ": " + name);
			}

			m_isURL = false;
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + name);
		}

		/*
		 * Is the file or URL compressed?  If so, we need to add a filter to
		 * uncompress it.
		 */
		try
		{
			String lowerName = name.toLowerCase();
			boolean isGzipped = lowerName.endsWith(".gz") ||
				lowerName.endsWith(".svgz");
			boolean isZipped = lowerName.endsWith(".zip");

			if (isGzipped)
				m_inputStream = new BufferedInputStream(new GZIPInputStream(in));
			else if (isZipped)
				m_inputStream = new BufferedInputStream(new ZipInputStream(in));
			else
				m_inputStream = new BufferedInputStream(in);

			InputStreamReader inputStreamReader;
			if (charsetName != null)
				inputStreamReader = new InputStreamReader(m_inputStream, charsetName);
			else
				inputStreamReader = new InputStreamReader(m_inputStream);
			m_reader = new LineNumberReader(inputStreamReader);
			m_name = name;
		}
		catch (IOException e)
		{
			/*
			 * Ensure any file we opened is closed on error.
			 */
			in.close();
			throw e;
		}
	}

	/**
	 * Open file or URL.
	 * @param name filename or URL to open.
	 * @param contextURL parent URL from which this URL is being loaded, or null
	 * if we are not loading from within the context of another URL.
	 */
	public FileOrURL(String name, FileOrURL contextURL)
		throws IOException, MapyrusException
	{
		InputStream in = null;
		
		try
		{
			if (contextURL != null)
				m_URL = new URL(contextURL.m_URL, name);
			else
				m_URL = new URL(name);

			try
			{
				in = m_URL.openStream();
			}
			catch (IOException e)
			{
				
				/*
				 * The IOException error message is not very helpful.  Throw our own exception.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CANNOT_OPEN_URL) + ": " +
					m_URL.toString() + Constants.LINE_SEPARATOR + e.getMessage());
			}
			m_isURL = true;
			m_name = m_URL.toString();
		}
		catch (MalformedURLException e)
		{
			m_URL = null;
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + name);
		}

		if (m_URL == null)
		{
			/*
			 * It is not a valid URL, try opening it as a plain file instead.
			 */
			try
			{
				in = new FileInputStream(name);
			}
			catch (SecurityException e)
			{
				throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + name);
			}
			m_isURL = false;
			m_name = name;
		}

		try
		{
			/*
			 * Is the file or URL compressed?  If so, we need to add a filter to
			 * uncompress it.
			 */
			boolean isGzipped = name.endsWith(".gz") || name.endsWith(".GZ");
			boolean isZipped = name.endsWith(".zip") || name.endsWith(".ZIP");

			/*
			 * Create buffered stream and reader from this input stream.
			 */
			if (isGzipped)
				m_inputStream = new BufferedInputStream(new GZIPInputStream(in));
			else if (isZipped)
				m_inputStream = new BufferedInputStream(new ZipInputStream(in));
			else
				m_inputStream = new BufferedInputStream(in);
	
			m_reader = new LineNumberReader(new InputStreamReader(m_inputStream));
		}
		catch (IOException e)
		{
			/*
			 * Ensure any file we opened is closed on error.
			 */
			in.close();
			throw e;
		}
	}

	/**
	 * Create object from file that is already open.
	 * @param reader file opened for reading.
	 * @param name name of file.
	 */
	public FileOrURL(Reader reader, String name)
	{
		m_isURL = false;
		m_name = name;
		m_reader = new LineNumberReader(reader);
		
		/*
		 * Text file reader cannot be converted for reading as a java.io.InputStream.
		 */
		m_inputStream = null;
		
	}

	/**
	 * Returns true if reading a URL, not a plain file.
	 * @return true if reading URL, false if reading a plain file.
	 */	
	public boolean isURL()
	{
		return(m_isURL);
	}

	/**
	 * Returns content type of URL being read.
	 * @return content type for URL being read.
	 */
	public String getURLContentType() throws IOException
	{	
		return(m_URL.openConnection().getContentType());
	}

	/**
	 * Returns java.io.BufferedInputStream for reading from this file or URL.
	 * @return stream to read from.
	 */
	public BufferedInputStream getInputStream()
	{
		return(m_inputStream);
	}

	/**
	 * Returns java.io.LineNumberReader for reading from this file or URL.
	 * @return reader to read from.
	 */
	public LineNumberReader getReader()
	{
		return(m_reader);
	}
	
	/**
	 * Returns name of file or URL we are reading from.
	 * @return filename or URL.
	 */
	public String toString()
	{
		return(m_name);
	}
}
