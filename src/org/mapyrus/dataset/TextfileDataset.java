/*
 * This file is part of Mapyrus.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.ArrayList;
import au.id.chenery.mapyrus.*;

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
	private LineNumberReader mReader;
	private String mFilename;
	private Process mProcess;
	
	/*
	 * Field separators.  Normally a comma or keyword 'whitespace' (meaning anything
	 * blank).
	 */
	private String mDelimiters;

	/*
	 * String that denotes comment lines in text file.  These lines
	 * are ignored.
	 */
	private String mComment;

	/*
	 * Read next line from file, skipping comment lines.
	 */
	private String readLine() throws IOException
	{
		String s;

		do
		{
			s = mReader.readLine();
		}
		while (s != null && s.startsWith(mComment));

		return(s);
	}

	/**
	 * Create string tokenizer to split line using delimiters set for this file.
	 * @param str is string to be split into tokens.
	 * @param delimiters are field delimiters, if null then whitespace is used.
	 */
	private StringTokenizer createStringTokenizer(String str, String delimiters)
	{
		StringTokenizer retval;
		
		if (delimiters == null)
			retval = new StringTokenizer(str);
		else
			retval = new StringTokenizer(str, delimiters);
		return(retval);
	}

	/**
	 * Open text file, possibly containing geographic data for querying.
	 * @param filename name of text file to open.
	 * @param extras options specific to text file datasets, given as var=value pairs.
	 * @param geometryFieldNames ignored, text datasets do not contain geometry.
	 */	
	public TextfileDataset(String filename, String extras, String []geometryFieldNames)
		throws FileNotFoundException, IOException, MapyrusException
	{
		String header, fieldType;
		StringTokenizer st;
		ArrayList list;
		int i, j;
		Integer fType;
		String token;
		boolean foundGeometryField;
		String nextLine;
		BufferedReader bufferedReader;

		/*
		 * Check if we should start a program and read its output instead
		 * of just reading from a file.
		 */
		if (filename.endsWith("|"))
		{
			String command = filename.substring(0, filename.length() - 1).trim();
			mProcess = Runtime.getRuntime().exec(command);
			bufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
		}
		else
		{
			bufferedReader = new BufferedReader(new FileReader(filename));
		}
		mReader = new LineNumberReader(bufferedReader);
		mFilename = filename;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		mDelimiters = null;
		mComment = "#";

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.startsWith("comment="))
				mComment = token.substring(8);
			else if (token.startsWith("delimiters="))
				mDelimiters = token.substring(11);
		}
	}

	/**
	 * Returns projection of dataset, which is not defined for a text file.
	 * @return string "undef".
	 */
	public String getProjection()
	{
		return("");
	}

	/**
	 * @see au.id.chenery.mapyrus.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata()
	{
		return(new Hashtable());
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
	 * Return null, no geometry fields in text file.
	 * @return null.
	 */
	public int[] getGeometryFieldIndexes()
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
	 * Begins a query on a text file dataset.
	 * @param extents the area of interest for the query.
	 * @param resolution is hint for minimum distance between coordinate values.
	 * Text file contains no geometry, so a fetch returns every line in turn.
	 */
	public void query(Rectangle2D.Double extents, double resolution)
		throws MapyrusException
	{
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
			throw new MapyrusException(e.getMessage() + ": " + mFilename);
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
		row.add(new Argument(Argument.STRING, nextLine));

		/*
		 * Split line into fields and build a row to be returned.
		 */
		st = createStringTokenizer(nextLine, mDelimiters);
		while (st.hasMoreTokens())
		{
			fieldValue = st.nextToken();
			row.add(new Argument(Argument.STRING, fieldValue));
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

		/*
		 * Try to read next row.
		 */
		if (readNextRow(row))
		{
			return(row);
		}
		else
		{
			/*
			 * We've read all of external program's output, now wait for
			 * it to terminate.
			 */
			if (mProcess != null)
			{
				try
				{
					mProcess.waitFor();
				}
				catch (InterruptedException e)
				{
					throw new MapyrusException(e.getMessage());
				}
			}
			return(null);
		}
	}
}
