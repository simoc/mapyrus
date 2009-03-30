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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading output from ogrinfo program, part of OGR library.
 * The ogrinfo program writes vector data from many formats
 * in a simple text format.
 * The class reads that output and picks out the geometry, attributes
 * and metadata.
 */
public class OGRDataset implements GeographicDataset
{
	/*
	 * File we are reading from.
	 * Process handle to external process we are reading from.
	 */
	private LineNumberReader m_reader;
	private String m_filename;
	private Process m_process;

	/*
	 * Extents of dataset.  Can only be found by scanning all records in
	 * dataset.
	 */
	private double m_xMin;
	private double m_yMin;
	private double m_xMax;
	private double m_yMax;

	/*
	 * Next line to read from ogrinfo output.
	 */
	private String m_nextLine;

	private String m_projection;
	private String []m_fieldNames;

	/**
	 * Read WKT projection from file.
	 * @param reader file to read from.
	 * @return WKT projection string.
	 */
	private String readWKTProjection(LineNumberReader reader)
		throws IOException, MapyrusException
	{
		String projection = "";
		int nOpenBrackets, nCloseBrackets;
		int i;

		do
		{
			/*
			 * Keep reading lines until the number of opening
			 * and closing brackets match.
			 * Fail if projection string grows ridiculously long.
			 */
			String nextLine = reader.readLine().trim();
			if (nextLine == null || projection.length() > 2048)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGR_PROJECTION) + ": " + projection);
			projection = projection + nextLine;

			nOpenBrackets = nCloseBrackets = 0;
			i = projection.indexOf('[');
			while (i >= 0)
			{
				nOpenBrackets++;
				i = projection.indexOf('[', i + 1);
			}
			i = projection.indexOf(']');
			while (i >= 0)
			{
				nCloseBrackets++;
				i = projection.indexOf(']', i + 1);
			}
		}
		while (nOpenBrackets != nCloseBrackets);

		return(projection);
	}

	/**
	 * Open MapInfo import files containing geographic data for querying.
	 * @param filename name of MapInfo file to open, with or without .mif suffix.
	 * @param extras options specific to MapInfo datasets, given as var=value pairs.
	 */	
	public OGRDataset(String filename, String extras, InputStream stdin)
		throws FileNotFoundException, IOException, MapyrusException
	{
		/*
		 * Open file or process to read from.
		 */
		m_filename = filename;
		if (filename.equals("-"))
		{
			m_reader = new LineNumberReader(new InputStreamReader(stdin));
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
			m_reader = new LineNumberReader(new InputStreamReader(m_process.getInputStream()));
		}
		else
		{
			m_reader = new LineNumberReader(new FileReader(filename));
		}

		/*
		 * Read and parse header of ogrinfo program output.
		 */
		boolean parsedHeader = false;
		String nextLine = m_reader.readLine();
		while (nextLine != null && (!parsedHeader))
		{
			if (nextLine.startsWith("Extent:"))
			{
				StringTokenizer st = new StringTokenizer(nextLine, " (),");
				if (st.countTokens() >= 6)
				{
					st.nextToken();
					m_xMin = Double.parseDouble(st.nextToken());
					m_yMin = Double.parseDouble(st.nextToken());
					String token = st.nextToken();
					while (!token.equals("-"))
						token = st.nextToken();
					m_xMax = Double.parseDouble(st.nextToken());
					m_yMax = Double.parseDouble(st.nextToken());
					
					nextLine = m_reader.readLine();
				}
			}
			else if (nextLine.startsWith("Layer SRS"))
			{
				m_projection = readWKTProjection(m_reader);
				
				/*
				 * Parse attribute field names following projection definition.
				 */
				nextLine = m_reader.readLine();
				ArrayList<String> fieldNames = new ArrayList<String>();
				while (nextLine != null && (!nextLine.startsWith("OGRFeature")))
				{
					int colonIndex = nextLine.indexOf(':');
					if (colonIndex >= 0)
					{
						String fieldName = nextLine.substring(0, colonIndex); 
						fieldNames.add(fieldName);
					}
					nextLine = m_reader.readLine();
				}
				fieldNames.add("STYLE");
				fieldNames.add("GEOMETRY");
				m_fieldNames = new String[fieldNames.size()];
				fieldNames.toArray(m_fieldNames);
				
				parsedHeader = true;
			}
			else
			{
				nextLine = m_reader.readLine();
			}
		}

		/*
		 * Check we successfully parsed file header.
		 */
		if (!parsedHeader)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGR_HEADER) + ": " + m_filename);
		}

		m_nextLine = nextLine;
	}

	/**
	 * Return projection of MIF file.
	 * @return projection string.
	 */
	public String getProjection()
	{
		return(m_projection);
	}

	/**
	 * Get metadata about ogrinfo output.
	 * @return metadata.
	 */
	public Hashtable getMetadata()
	{
		return(new Hashtable());
	}

	/**
	 * Return list of attribute field names.
	 * @return field name list.
	 */
	public String[] getFieldNames()
	{
		return(m_fieldNames);
	}

	/**
	 * Return extents of ogrinfo output.
	 * @return world extents.
	 */
	public Rectangle2D.Double getWorlds()
	{
		return new Rectangle2D.Double(m_xMin, m_yMin, m_xMax - m_xMin, m_yMax - m_yMin);
	}

	/**
	 * Fetch next row from ogrinfo output.
	 * @return row, or null if reached end of file.
	 */
	public Row fetch() throws MapyrusException
	{
		Row row = null;
		int equalsIndex;

		try
		{
			if (m_nextLine != null)
			{
				/*
				 * Skip blank lines.
				 */
				if (m_nextLine.length() == 0)
					m_nextLine = m_reader.readLine();

				if (m_nextLine != null && m_nextLine.startsWith("OGRFeature"))
				{
					row = new Row(m_fieldNames.length);

					/*
					 * Read each field value and add it to row.
					 */
					for (int i = 0; i < m_fieldNames.length - 2; i++)
					{
						m_nextLine = m_reader.readLine();
						if (m_nextLine == null)
						{
							throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
								": " + m_filename);
						}

						if ((equalsIndex = m_nextLine.indexOf('=')) < 0)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGR_FEATURE) + ": " + m_nextLine);
						}

						String field = m_nextLine.substring(equalsIndex + 2);
						if (field.length() == 0)
							row.add(Argument.emptyString);
						else
							row.add(new Argument(Argument.STRING, field));
					}

					/*
					 * If style value is given then add it, otherwise set an empty value.
					 */
					m_nextLine = m_reader.readLine();
					if (m_nextLine == null)
					{
						throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
							": " + m_filename);
					}

					int styleIndex = m_nextLine.indexOf("Style =");
					if (styleIndex >= 0)
					{
						row.add(new Argument(Argument.STRING, m_nextLine.substring(styleIndex + 8)));
						m_nextLine = m_reader.readLine();
					}
					else
					{
						row.add(Argument.emptyString);
					}

					if (m_nextLine == null)
					{
						throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
							": " + m_filename);
					}

					/*
					 * Read geometry and add it to row.
					 */
					row.add(new Argument(Argument.STRING, m_nextLine.trim()));
					
					/*
					 * Read first line of next feature.
					 */
					m_nextLine = m_reader.readLine();
				}
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}

		return(row);
	}

	/**
	 * Close ogrinfo output.
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
