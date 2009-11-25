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
package org.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Dataset containing a map of the world included inside of Mapyrus.
 */
public class InternalDataset implements GeographicDataset
{
	private String m_filename;
	private LineNumberReader m_reader;
	private String []m_fieldNames;

	/**
	 * Open dataset stored inside this software.
	 * @param filename is name of data to read.
	 * @param extras options for reading data.
	 */
	public InternalDataset(String filename, String extras) throws MapyrusException, IOException
	{
		URL url = this.getClass().getResource(filename + ".txt");
		if (url == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.OPEN_DATASET_ERROR) + ": " + filename);
		m_reader = new LineNumberReader(new InputStreamReader(url.openStream()));
		m_filename = filename;
		String headerLine = m_reader.readLine();
		if (headerLine != null)
		{
			int hashIndex = headerLine.indexOf('#');
			if (hashIndex >= 0)
				headerLine = headerLine.substring(hashIndex + 1);

			StringTokenizer st = new StringTokenizer(headerLine);
			int nFieldNames = st.countTokens();
			m_fieldNames = new String[nFieldNames + 1];
			m_fieldNames[0] = "GEOMETRY";
			for (int i = 0; i < nFieldNames; i++)
			{
				m_fieldNames[i + 1] = st.nextToken();
			}
		}
	}

	public String getProjection()
	{
		return "GEOGCS[\"wgs84\",DATUM[\"WGS_1984\",SPHEROID[\"wgs84\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]";
	}

	public Hashtable getMetadata()
	{
		Hashtable<String, String> retval = new Hashtable<String, String>();
		return retval;
	}

	public String[] getFieldNames()
	{
		return(m_fieldNames);
	}

	public Rectangle2D.Double getWorlds()
	{
		return(new Rectangle2D.Double(-180, -90, 360, 180));
	}

	/**
	 * Read next line from file, skipping comment lines.
	 * @param reader file to read from.
	 * @return line read from file, or null if EOF.
	 */
	private String readLine(LineNumberReader reader) throws IOException
	{
		String line = reader.readLine();
		while (line != null && line.startsWith("#"))
		{
			line = reader.readLine();
		}
		return line;
	}

	private Argument readGeometry(String geometryType, int nPoints, int []islandIndexes) throws IOException
	{
		double []geometry = new double[(nPoints * 2 + nPoints) + 2];
		if (geometryType.equals("POLYGON"))
			geometry[0] = Argument.GEOMETRY_POLYGON;
		else if (geometryType.equals("LINESTRING"))
			geometry[0] = Argument.GEOMETRY_LINESTRING;
		else
			geometry[0] = Argument.GEOMETRY_POINT;
		geometry[1] = nPoints;

		int geometryIndex = 2;
		int nextIslandIndex = 0;
		for (int i = 0; i < nPoints; i++)
		{
			int segment;
			if (i == 0)
			{
				/*
				 * First segment is an island.
				 */
				segment = Argument.MOVETO;
			}
			else if (islandIndexes != null && nextIslandIndex < islandIndexes.length &&
				i == islandIndexes[nextIslandIndex])
			{
				/*
				 * Start of a new island.
				 */
				segment = Argument.MOVETO;
				nextIslandIndex++;
			}
			else
			{
				segment = Argument.LINETO;
			}
			geometry[geometryIndex++] = segment;
			String line = readLine(m_reader);
			int spaceIndex = line.indexOf(' ');
			int x = Integer.parseInt(line.substring(0, spaceIndex));
			int y = Integer.parseInt(line.substring(spaceIndex + 1));
			geometry[geometryIndex++] = x / 100.0;
			geometry[geometryIndex++] = y / 100.0;
		}
		Argument retval = new Argument((int)geometry[0], geometry);
		return(retval);
	}

	private Row readRow(String headerLine) throws MapyrusException, IOException
	{
		Row retval = new Row();
		StringTokenizer st = new StringTokenizer(headerLine);
		String geometryType = st.nextToken();
		String pointCountToken = "1";
		if (st.hasMoreTokens())
			pointCountToken = st.nextToken();
		int nPoints;
		int []islandIndexes;
		if (pointCountToken.indexOf('/') < 0)
		{
			nPoints = Integer.parseInt(pointCountToken);
			islandIndexes = null;
		}
		else
		{
			/*
			 * Make list of indexes at which each island starts.
			 */
			StringTokenizer st2 = new StringTokenizer(pointCountToken, "/");
			nPoints = Integer.parseInt(st2.nextToken());
			islandIndexes = new int[st2.countTokens()];
			int i = 0;
			while (st2.hasMoreTokens())
			{
				islandIndexes[i] = nPoints;
				nPoints += Integer.parseInt(st2.nextToken());
				i++;
			}
		}

		/*
		 * Set empty geometry.  We'll read it after field values.
		 */
		retval.add(null);
		
		/*
		 * Read field values for row.
		 */
		String line = readLine(m_reader);
		String []fieldValues = line.split(",");
		if (fieldValues.length != m_fieldNames.length - 1)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.MISSING_FIELD) +
				": " + m_filename + ":" + m_reader.getLineNumber());
		}
		for (int i = 0; i < fieldValues.length; i++)
		{
			retval.add(new Argument(Argument.STRING, fieldValues[i]));
		}

		Argument geometry = readGeometry(geometryType, nPoints, islandIndexes);
		retval.set(0, geometry);
		return(retval);
	}

	public Row fetch() throws MapyrusException
	{
		Row retval = null;
		try
		{
			String headerLine = readLine(m_reader);
			if (headerLine != null)
				retval = readRow(headerLine);
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return(retval);
	}

	public void close() throws MapyrusException
	{
		try
		{
			if (m_reader != null)
				m_reader.close();
		}
		catch (IOException e)
		{
		}
		m_reader = null;
	}
}
