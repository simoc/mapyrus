/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.mapyrus.*;

/**
 * Implements reading of geographic dataset from GRASS sites file.
 * This is a simple ASCII format with one record per line.
 * Each record is an (X, Y) or (X, Y, Z) point and attribute strings
 * separated by pipe character delimiters.
 */
public class GrassDataset implements GeographicDataset
{
	/*
	 * GRASS sites file being read.
	 */
	private String mFilename;

	private Hashtable mMetadata;
	private String[] mFieldNames;
	private Rectangle2D.Double mExtents;

	/*
	 * Indicates whether file containing 2D or 3D point data.
	 */
	private boolean mIsThreed;

	private ArrayList mAllRows;
	int mRowFetchIndex;

	/*
	 * Buffer to copy variable length attribute values into.
	 */
	private StringBuffer mAttributeBuffer;

	/*
	 * Read next line from file, skipping comment lines.
	 */
	private String readLine(LineNumberReader reader) throws IOException
	{
		String s;

		do
		{
			s = reader.readLine();
		}
		while (s != null && s.length() > 0 && s.charAt(0) == '#');

		return(s);
	}

	/**
	 * Open GRASS sites file containing points.
	 * @param filename name of sites file.
	 * @param extras options specific to GRASS datasets, given as var=value pairs.
	 */	
	public GrassDataset(String filename, String extras)
		throws FileNotFoundException, IOException, MapyrusException
	{
		LineNumberReader reader = null;

		try
		{	
			FileOrURL f = new FileOrURL(filename);
			reader = f.getReader();
			mFilename = filename;
			mMetadata = new Hashtable();
			mAttributeBuffer = new StringBuffer();
			mRowFetchIndex = 0;

			/*
			 * Parse all of the optional header lines in sites file.
			 * All header lines are of the form "desc|Potential historic sites".
			 */
			String line = readLine(reader);
			while (line != null &&
				line.length() > 0 && Character.isLetter(line.charAt(0)))
			{
	
				StringTokenizer st = new StringTokenizer(line, "|");
				if (st.countTokens() > 1)
				{
					String keyword = st.nextToken();
					String value = st.nextToken();
					mMetadata.put(keyword, value);
				}
				line = readLine(reader);
			}

			if (line == null || line.length() == 0)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GRASS_FILE) +
					": " + filename);
			}

			/*
			 * Find out if this file has (X, Y) or (X, Y, Z) values.
			 */
			int nDimensions = 0, index = 0;
			char c = line.charAt(index++);
			while (Character.isDigit(c) || c == '.' || c == ':' || c == '-' ||
				c == 'N' || c == 'S' || c == 'E' || c == 'W' || c == '|')
			{
				if (c == '|')
					nDimensions++;
				index++;
				if (index == line.length())
					break;
				c = line.charAt(index);
			}
			mIsThreed = (nDimensions > 2);

			/*
			 * GRASS sites lists are normally not too large so read it all
			 * into memory.  Then we can determine the bounding rectangle
			 * of all points in the dataset.
			 */
			double xMin = Float.MAX_VALUE;
			double yMin = Float.MAX_VALUE;
			double xMax = Float.MIN_VALUE;
			double yMax = Float.MIN_VALUE;

			Row row;
			mAllRows = new ArrayList();
			do
			{
				row = parseRow(reader, line);
				mAllRows.add(row);

				Argument pt = (Argument)row.get(0);
				double[] els = pt.getGeometryValue();
				double x = els[3];
				double y = els[4];
				if (x < xMin)
					xMin = x;
				if (x > xMax)
					xMax = x;
				if (y < yMin)
					yMin = y;
				if (y > yMax)
					yMax = y;
	
				line = readLine(reader);
			}
			while (line != null);

			mExtents = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);

			mFieldNames = new String[row.size()];
			int i = 0, fieldCounter = 1;
			mFieldNames[i++] = "GEOMETRY";
			if (mIsThreed)
				mFieldNames[i++] = "Z";

			while (i < mFieldNames.length)
			{
				mFieldNames[i++] = DefaultFieldNames.get(fieldCounter++);
			}
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Returns projection of dataset, not defined for GRASS sites file.
	 * @return empty string.
	 */
	public String getProjection()
	{
		return("");
	}

	/**
	 * @see org.mapyrus.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata()
	{
		return(mMetadata);
	}

	/**
	 * Return names of fields in this text file.
	 * @return null, no fieldnames known.
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * Return extents of sites file.
	 * @return area covered by sites file.
	 */
	public Rectangle2D.Double getWorlds()
	{
		return(mExtents);
	}

	/**
	 * Parse degrees, minutes, seconds value.
	 * @param dms value to parse.
	 * @return decimal degree value.
	 */
	private double parseDMS(String dms)
	{
		double retval;

		int len = dms.length();
		char lastChar = dms.charAt(len - 1);
		if (lastChar == 'N' || lastChar == 'E' || lastChar == 'S' || lastChar == 'W')
		{
			/*
			 * Convert value like '48:01:12.22N' to decimal degree value.
			 */
			int colon1Index = dms.indexOf(':');
			String degrees = dms.substring(0, colon1Index);
			int colon2Index = dms.indexOf(':', colon1Index + 1);
			String minutes = dms.substring(colon1Index, colon2Index - colon1Index);
			String seconds = dms.substring(colon2Index + 1, len - colon2Index);
			retval = Double.parseDouble(degrees) + Double.parseDouble(minutes) / 60.0 +
				Double.parseDouble(seconds) / 3600.0;

			/*
			 * Degree values in southern and western hemispheres are negative.
			 */
			if (lastChar == 'S' || lastChar == 'W')
				retval = -retval;
		}
		else
		{
			retval = Double.parseDouble(dms);
		}
		return retval;
	}

	/**
	 * Parse point coordinates and attributes from a row read from GRASS site file. 
	 * @param reader file being read from.
	 * @param line line to parse.
	 * @return parsed field values.
	 */
	private Row parseRow(LineNumberReader reader, String line) throws MapyrusException
	{
		Row row = new Row();

		double x, y, z;
		int lineLength = line.length();
		int pipe1Index, pipe2Index = 0, pipe3Index = 0;
		int nextIndex;

		pipe1Index = line.indexOf('|');

		if (pipe1Index >= 0)
		{
			pipe2Index = line.indexOf('|', pipe1Index + 1);
			if (mIsThreed && pipe2Index >= 0)
			{
				pipe3Index = line.indexOf('|', pipe2Index + 1);
			}
		}

		if (pipe1Index < 0 || pipe2Index < 0 || pipe3Index < 0)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GRASS_FILE) +
				": " + mFilename + ":" + reader.getLineNumber() + ": " + line);
		}

		/*
		 * Add (X, Y) as point geometry.
		 */
		try
		{
			String xs = line.substring(0, pipe1Index);
			x = parseDMS(xs);
			String ys = line.substring(pipe1Index + 1, pipe2Index);
			y = parseDMS(ys);
			double []els = {Argument.GEOMETRY_POINT, 1, Argument.MOVETO, x, y};
			row.add(new Argument(Argument.GEOMETRY_POINT, els));

			if (mIsThreed)
			{
				String zs = line.substring(pipe2Index + 1, pipe3Index);
				z = parseDMS(zs);
				row.add(new Argument(z));
				nextIndex = pipe3Index + 1;
			}
			else
			{
				nextIndex = pipe2Index + 1;
			}

			/*
			 * Parse each of the attribute field values too.
			 */
			while (nextIndex < lineLength)
			{
				mAttributeBuffer.setLength(0);
				char c;
				char firstChar = line.charAt(nextIndex);
				nextIndex++;
				if (firstChar == '#' || firstChar == '%' || Character.isDigit(firstChar))
				{
					/*
					 * Got a category integer or floating point value.
					 */
					if (Character.isDigit(firstChar))
						mAttributeBuffer.append(firstChar);

					while (nextIndex < lineLength)
					{
						c = line.charAt(nextIndex);
						nextIndex++;
						if (Character.isWhitespace(c))
							break;
						mAttributeBuffer.append(c);
					}
					row.add(new Argument(Double.parseDouble(mAttributeBuffer.toString())));
				}
				else if (firstChar == '@')
				{
					/*
					 * Got a string.
					 */
					c = line.charAt(nextIndex);
					boolean isQuoted = (c == '"');
					if (isQuoted)
					{
						nextIndex++;
						char lastC = 0;
	
						while (nextIndex < lineLength)
						{
							c = line.charAt(nextIndex);
							nextIndex++;
	
							/*
							 * Stop when we find closing quote but allow
							 * \" as embedded quotes in string.
							 */
							if (c == '"')
							{
								if (lastC == '\\')
									mAttributeBuffer.deleteCharAt(mAttributeBuffer.length() - 1);
								else
									break;
							}
							mAttributeBuffer.append(c);
						}
					}
					else
					{
						/*
						 * Append characters until we reach a space or the end of the line.
						 */
						while (nextIndex < lineLength)
						{
							c = line.charAt(nextIndex);
							nextIndex++;
							if (Character.isWhitespace(c))
								break;
							mAttributeBuffer.append(c);
						}
					}
					row.add(new Argument(Argument.STRING, mAttributeBuffer.toString()));
				}		
			}
		}
		catch (NumberFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GRASS_FILE) +
				": " + mFilename + ":" + reader.getLineNumber() + ": " + line);
		}

		return(row);
	}

	/**
	 * Gets next row from file.
	 * @return next row read, or null if no row found.
	 */
	public Row fetch() throws MapyrusException
	{
		Row retval;

		if (mRowFetchIndex < mAllRows.size())
			retval = (Row)mAllRows.get(mRowFetchIndex++);
		else
			retval = null;

		return(retval);
	}

	/**
	 * Close dataset.
	 */
	public void close() throws MapyrusException
	{
		mAllRows = null;
	}
}
