/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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
package org.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading of geographic datasets from a delimited text file.
 * Suitable for reading comma separated file or other simplistic file formats.
 */
public class TextfileDataset implements GeographicDataset
{
	/*
	 * File we are reading from.
	 * Process handle to external process we are reading from.
	 */
	private LineNumberReader m_reader;
	private String m_filename;
	private Process m_process;
	
	/*
	 * Field separator.  Normally a comma or keyword 'whitespace' (meaning anything
	 * blank).
	 */
	private Character m_delimiter;

	/*
	 * String that denotes comment lines in text file.  These lines
	 * are ignored.
	 */
	private String m_comment;

	/*
	 * Maximum number of fields found on one line so far.
	 * Later lines with fewer fields will be padded to this number of fields
	 * with empty fields.
	 */
	private int m_maxFields;

	/*
	 * Read next line from file, skipping comment lines.
	 */
	private String readLine() throws IOException
	{
		String s;

		do
		{
			s = m_reader.readLine();
		}
		while (s != null && (m_comment.length() > 0 && s.startsWith(m_comment)));

		return(s);
	}

	/**
	 * Open text file, possibly containing geographic data for querying.
	 * @param filename name of text file to open.
	 * @param extras options specific to text file datasets, given as var=value pairs.
	 * @param stdin standard input stream of interpreter.
	 */	
	public TextfileDataset(String filename, String extras, InputStream stdin)
		throws FileNotFoundException, IOException, MapyrusException
	{
		StringTokenizer st;
		String token;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		m_delimiter = null;
		m_comment = "#";
		m_maxFields = 0;
		String encoding = null;

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.startsWith("comment="))
				m_comment = token.substring(8);
			else if (token.startsWith("delimiter=") && token.length() == 11)
				m_delimiter = new Character(token.charAt(10));
			if (token.startsWith("encoding="))
				encoding = token.substring(9);
		}

		/*
		 * Check if we should read standard input, start a program and
		 * read its output, or just read a plain file.
		 */
		if (filename.equals("-"))
		{
			InputStreamReader reader;
			if (encoding != null)
				reader = new InputStreamReader(stdin, encoding);
			else
				reader = new InputStreamReader(stdin);
			m_reader = new LineNumberReader(reader);
		}
		else if (filename.endsWith("|"))
		{
			String command = filename.substring(0, filename.length() - 1).trim();
			String []cmdArray;
			if (Constants.getOSName().indexOf("WIN") >= 0)
				cmdArray = new String[]{command};
			else
				cmdArray = new String[]{"sh", "-c", command};
			m_process = Runtime.getRuntime().exec(cmdArray);

			InputStreamReader reader;
			if (encoding != null)
				reader = new InputStreamReader(m_process.getInputStream(), encoding);
			else
				reader = new InputStreamReader(m_process.getInputStream());
			m_reader = new LineNumberReader(reader);
		}
		else
		{
			FileOrURL f;
			if (encoding != null)
				f = new FileOrURL(filename, encoding);
			else
				f = new FileOrURL(filename);
			m_reader = f.getReader();
		}
		m_filename = filename;
	}

	/**
	 * Returns projection of dataset, which is not defined for a text file.
	 * @return empty string.
	 */
	public String getProjection()
	{
		return("");
	}

	/**
	 * @see org.mapyrus.GeographicDataset#getMetadata()
	 */
	public Hashtable<String, String> getMetadata()
	{
		return(new Hashtable<String, String>());
	}

	/**
	 * Return names of fields in this text file.
	 * @return null, no fieldnames known.
	 */
	public String[] getFieldNames()
	{
		return(null);
	}

	/**
	 * Return extents of text file.  We do not know this.
	 * @return degree values covering the whole world
	 */
	public Rectangle2D.Double getWorlds()
	{
		return new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0);
	}

	/**
	 * Read next row from file and split it into fields.
	 * Build fields into Row structure.
	 */
	private boolean readNextRow(Row row) throws MapyrusException
	{
		StringTokenizer st;
		String fieldValue, nextLine;

		/*
		 * Need next line from file.
		 */
		try
		{
			nextLine = readLine();
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage() + ": " + m_filename);
		}

		/*
		 * Return EOF status if no more lines available in file.
		 */
		if (nextLine == null)
			return(false);

		/*
		 * First field is entire line to go in the $0 variable.
		 */
		row.clear();
		Argument firstField = new Argument(Argument.STRING, nextLine);
		row.add(firstField);

		/*
		 * Split line into fields and build a row to be returned.
		 */
		if (m_delimiter == null)
		{
			st = new StringTokenizer(nextLine);
			while (st.hasMoreTokens())
			{
				fieldValue = st.nextToken();
				row.add(new Argument(Argument.STRING, fieldValue));
			}
		}
		else
		{
			char delim = m_delimiter.charValue();
			int lastIndex = 0;
			int nextIndex = nextLine.indexOf(delim);
			if (nextIndex < 0)
			{
				/*
				 * No delimiters found, whole line is field.
				 */
				row.add(firstField);
			}
			else
			{
				while (nextIndex >= 0)
				{
					if (lastIndex == nextIndex)
						row.add(Argument.emptyString);
					else
						row.add(new Argument(Argument.STRING, nextLine.substring(lastIndex, nextIndex)));

					lastIndex = nextIndex + 1;
					nextIndex = nextLine.indexOf(delim, lastIndex);
				}
				row.add(new Argument(Argument.STRING, nextLine.substring(lastIndex)));
			}
		}
		
		/*
		 * Pad lines containing fewer fields than previous lines with empty fields.
		 * This ensures that fields from earlier lines are overwritten by fields
		 * from later lines.
		 */
		int nPaddingFields = m_maxFields - row.size();
		if (nPaddingFields > 0)
		{
			while (nPaddingFields-- > 0)
				row.add(Argument.emptyString);
		}
		else if (nPaddingFields < 0)
		{
			/*
			 * This is the longest
			 */
			m_maxFields = row.size();
		}
		return(true);
	}

	/**
	 * Gets next row from file.
	 * @return next row read, or null if no row found.
	 */
	public Row fetch() throws MapyrusException
	{
		Row row = new Row();
		Row retval;

		/*
		 * Try to read next row.
		 */
		if (readNextRow(row))
			retval = row;
		else
			retval = null;
		return(retval);
	}

	/**
	 * Closes dataset.
	 */
	public void close() throws MapyrusException
	{
		try
		{
			/*
			 * Read any remaining output from external program.
			 */
			if (m_process != null)
			{
				while (m_reader.read() > 0)
					;
			}

			/*
			 * We've read all of external program's output, now wait for
			 * it to terminate.
			 */
			if (m_process != null)
			{
				try
				{
					int status = m_process.waitFor();
					if (status != 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.PROCESS_ERROR) +
							": " + m_filename);
					}
				}
				catch (InterruptedException e)
				{
					throw new MapyrusException(e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		finally
		{
			/*
			 * Ensure that file being read is always closed.
			 */
			try
			{
				m_reader.close();
			}
			catch (IOException e)
			{
			}
		}
	}
}
