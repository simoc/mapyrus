/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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
	private String mName;
	private URL mURL;
	private BufferedInputStream mInputStream;
	private LineNumberReader mReader;
	private boolean mIsURL;

	/**
	 * Open file or URL.
	 * @param name name of file or URL to open.
	 * @throws IOException
	 */
	public FileOrURL(String name) throws IOException, MapyrusException
	{
		InputStream in;

		try
		{
			/*
			 * First try opening as an URL.
			 */
			mURL = new URL(name);
			try
			{
				in = mURL.openStream();
			}
			catch (IOException e)
			{
				/*
				 * The IOException error message is not very helpful.  Throw our own exception.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CANNOT_OPEN_URL) +
					": " +	mURL.toString() + Constants.LINE_SEPARATOR + e.getMessage());
			}
			mIsURL = true;
		}
		catch (MalformedURLException e)
		{
			/*
			 * It is not a valid URL, try opening it as a plain file instead.
			 */
			in = new FileInputStream(name);
			mIsURL = false;
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
				mInputStream = new BufferedInputStream(new GZIPInputStream(in));
			else if (isZipped)
				mInputStream = new BufferedInputStream(new ZipInputStream(in));
			else
				mInputStream = new BufferedInputStream(in);
	
			mReader = new LineNumberReader(new InputStreamReader(mInputStream));
			mName = name;
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
				mURL = new URL(contextURL.mURL, name);
			else
				mURL = new URL(name);

			try
			{
				in = mURL.openStream();
			}
			catch (IOException e)
			{
				
				/*
				 * The IOException error message is not very helpful.  Throw our own exception.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CANNOT_OPEN_URL) + ": " +
					mURL.toString() + Constants.LINE_SEPARATOR + e.getMessage());
			}
			mIsURL = true;
			mName = mURL.toString();
		}
		catch (MalformedURLException e)
		{
			mURL = null;
		}

		if (mURL == null)
		{
			/*
			 * It is not a valid URL, try opening it as a plain file instead.
			 */
			in = new FileInputStream(name);
			mIsURL = false;
			mName = name;
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
				mInputStream = new BufferedInputStream(new GZIPInputStream(in));
			else if (isZipped)
				mInputStream = new BufferedInputStream(new ZipInputStream(in));
			else
				mInputStream = new BufferedInputStream(in);
	
			mReader = new LineNumberReader(new InputStreamReader(mInputStream));
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
		mIsURL = false;
		mName = name;
		mReader = new LineNumberReader(reader);
		
		/*
		 * Text file reader cannot be converted for reading as a java.io.InputStream.
		 */
		mInputStream = null;
		
	}

	/**
	 * Returns true if reading a URL, not a plain file.
	 * @return true if reading URL, false if reading a plain file.
	 */	
	public boolean isURL()
	{
		return(mIsURL);
	}

	/**
	 * Returns content type of URL being read.
	 * @return content type for URL being read.
	 */
	public String getURLContentType() throws IOException
	{	
		return(mURL.openConnection().getContentType());
	}

	/**
	 * Returns java.io.BufferedInputStream for reading from this file or URL.
	 * @return stream to read from.
	 */
	public BufferedInputStream getInputStream()
	{
		return(mInputStream);
	}

	/**
	 * Returns java.io.LineNumberReader for reading from this file or URL.
	 * @return reader to read from.
	 */
	public LineNumberReader getReader()
	{
		return(mReader);
	}
	
	/**
	 * Returns name of file or URL we are reading from.
	 * @return filename or URL.
	 */
	public String toString()
	{
		return(mName);
	}
}
