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
package org.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads Open Streetmap XML format data from URL or local file,
 * using OSM Protocol Version 0.5.
 * @see http://wiki.openstreetmap.org/index.php/OSM_Protocol_Version_0.5
 */
public class OpenStreetMapDataset extends DefaultHandler implements GeographicDataset
{
	private static String[] FIELD_NAMES = new String[]{"TYPE", "ID", "GEOMETRY", "TAGS"};

	private static Argument NODE_TYPE_ARGUMENT = new Argument(Argument.STRING, "node");
	private static Argument WAY_TYPE_ARGUMENT = new Argument(Argument.STRING, "way");

	/*
	 * Rows of data parsed from XML file for nodes and ways.
	 */
	private LinkedList<Row> m_data;

	/*
	 * World extents of data read from XML file.
	 */
	private double m_xMin = Double.MAX_VALUE;
	private double m_yMin = Double.MAX_VALUE;
	private double m_xMax = Double.MIN_VALUE;
	private double m_yMax = Double.MIN_VALUE;

	/*
	 * Lookup table of nodes for construction of ways from a list of node IDs.
	 */
	private HashMap<String, double []> m_allNodes;

	/*
	 * State during XML parsing. 
	 */
	private String m_OSMVersion = "";
	private String m_OSMGenerator = "";
	private String m_nodeId = "";
	private String m_wayId = "";
	private double m_lat = 0;
	private double m_lon = 0;
	private boolean m_visible = true;
	private Argument m_tags = null;
	private ArrayList<String> m_wayNodes = null;

	/**
	 * Open file or URL to OpenStreetmap XML data and read data.
	 * @param filename is file or URL to read data from.
	 * @param extras options for reading data.
	 * @throws MapyrusException
	 */
	public OpenStreetMapDataset(String filename, String extras, InputStream stdin)
		throws IOException, MapyrusException
	{
		InputStream inStream = null;
		Process process = null;

		try
		{
			m_data = new LinkedList<Row>();
			m_allNodes = new HashMap<String, double []>();

			/*
			 * Check if we should read standard input, start a program and
			 * read its output, or just read a plain file.
			 */
			if (filename.equals("-"))
			{
				inStream = stdin;
			}
			else if (filename.endsWith("|"))
			{
				String command = filename.substring(0, filename.length() - 1).trim();
				String []cmdArray;
				if (Constants.getOSName().indexOf("WIN") >= 0)
					cmdArray = new String[]{command};
				else
					cmdArray = new String[]{"sh", "-c", command};
				process = Runtime.getRuntime().exec(cmdArray);
				inStream = new BufferedInputStream(process.getInputStream());
			}
			else
			{	

				FileOrURL url = new FileOrURL(filename);
				inStream = url.getInputStream();
			}

			/*
			 * Parse XML file into memory.
			 */
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(inStream, this);
		}
		catch (SAXException e)
		{
			throw new MapyrusException(e.getMessage() + ": " + filename);
		}
		catch (ParserConfigurationException e)
		{
			throw new MapyrusException(e.getMessage() + ": " + filename);
		}
		finally
		{
			try
			{
				/*
				 * Read any remaining output from external program.
				 */
				if (process != null)
				{
					while (inStream.read() > 0)
						;
				}
			}
			catch(IOException e)
			{
			}				

			try
			{
				if (process != null)
				{
					/*
					 * We've read all of external program's output, now wait for
					 * it to terminate.
					 */
					int status = process.waitFor();
					if (status != 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.PROCESS_ERROR) + ": " + filename);
					}
				}
			}
			catch (InterruptedException e)
			{
				throw new MapyrusException(e.getMessage()  + ": " + filename);
			}
			finally
			{
				try
				{
					/*
					 * Ensure that file being read is always closed.
					 */
					if (inStream != null)
						inStream.close();
				}
				catch (IOException e)
				{
				}
			}
		}
	}

	/**
	 * Get attribute value.
	 * @param qName name of XML tag.
	 * @param attributes attributes of XML tag.
	 * @param attrName name of attribute to fetch.
	 * @return attribute value.
	 */
	private String getAttribute(String qName, Attributes attributes, String attrName) throws SAXException
	{
		String attrValue = attributes.getValue(attrName);
		if (attrValue == null)
		{
			throw new SAXException(MapyrusMessages.get(MapyrusMessages.MISSING_XML_ATTRIBUTE) +
				": <" + qName + "> " + attrName);
		}
		return(attrValue);
	}

	public void startElement(String uri, String localName,
			String qName, Attributes attributes) throws SAXException
	{
		if (qName.equals("node"))
		{
			String visible = getAttribute(qName, attributes, "visible");
			m_visible = Boolean.parseBoolean(visible);
			if (m_visible)
			{
				/*
				 * Encountered a node, a single (X, Y) value.
				 */
				m_nodeId = getAttribute(qName, attributes, "id");
				String lat = getAttribute(qName, attributes, "lat");
				try
				{
					m_lat = Double.parseDouble(lat);
				}
				catch (NumberFormatException e)
				{
					throw new SAXException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": <node> lat: " + lat);
				}

				String lon = getAttribute(qName, attributes, "lon");
				try
				{
					m_lon = Double.parseDouble(lon);
				}
				catch (NumberFormatException e)
				{
					throw new SAXException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": <node> lon: " + lon);
				}
				m_tags = null;

				/*
				 * Build bounding box of data in XML file.
				 */
				if (m_lat < m_yMin)
					m_yMin = m_lat;
				if (m_lat > m_yMax)
					m_yMax = m_lat;
				if (m_lon < m_xMin)
					m_xMin = m_lon;
				if (m_lon > m_xMax)
					m_xMax = m_lon;
			}
		}
		else if (qName.equals("way"))
		{
			/*
			 * Encountered a way, referring to a list of nodes.
			 */
			String visible = getAttribute(qName, attributes, "visible");
			m_visible = Boolean.parseBoolean(visible);
			if (m_visible)
			{
				m_wayId = getAttribute(qName, attributes, "id");
				m_wayNodes = new ArrayList<String>();
				m_tags = null;
			}
		}
		else if (qName.equals("tag") && m_visible)
		{
			/*
			 * Create hash table entry for key-value pairs for node or way.
			 */
			if (m_tags == null)
				m_tags = new Argument();
			String k = getAttribute(qName, attributes, "k");
			String v = getAttribute(qName, attributes, "v");
			m_tags.addHashMapEntry(k, new Argument(Argument.STRING, v));
		}
		else if (qName.equals("nd") && m_visible)
		{
			/*
			 * Add node to list of nodes for a way.
			 */
			String ref = getAttribute(qName, attributes, "ref");
			m_wayNodes.add(ref);
		}
		else if (qName.equals("osm"))
		{
			/*
			 * Get header information.
			 */
			m_OSMVersion = getAttribute(qName, attributes, "version");
			m_OSMGenerator = getAttribute(qName, attributes, "generator");
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if (qName.equals("node"))
		{
			if (m_visible)
			{
				/*
				 * Build a dataset row for node we have finished reading.
				 */
				Row row = new Row(FIELD_NAMES.length);
				row.add(NODE_TYPE_ARGUMENT);
				row.add(new Argument(Argument.STRING, m_nodeId));
				double []els = new double[]{Argument.GEOMETRY_POINT, 1, Argument.MOVETO, m_lon, m_lat};
				Argument geometryArgument = new Argument(Argument.GEOMETRY_POINT, els);
				row.add(geometryArgument);
				if (m_tags != null)
					row.add(m_tags);
				else
					row.add(Argument.emptyString);
				m_tags = null;
				m_data.add(row);
				m_allNodes.put(m_nodeId, els);
			}
		}
		else if (qName.equals("way"))
		{
			if (m_visible)
			{
				/*
				 * Build a dataset row for the way we have finished reading.
				 */
				Row row = new Row(FIELD_NAMES.length);
				row.add(WAY_TYPE_ARGUMENT);
				row.add(new Argument(Argument.STRING, m_wayId));
				
				/*
				 * Determine if way is a closed polygon.
				 */
				boolean isClosed = false;
				int nNodes = m_wayNodes.size();
				if (nNodes > 1)
				{
					String startNodeId = m_wayNodes.get(0);
					String endNodeId = m_wayNodes.get(nNodes - 1);
					double []startNodeEls = m_allNodes.get(startNodeId);
					double []endNodeEls = m_allNodes.get(endNodeId);
					isClosed = (startNodeEls[3] == endNodeEls[3] && startNodeEls[4] == endNodeEls[4]);
				}

				/*
				 * Build line or polygon geometry.
				 */
				double []els = new double[nNodes * 3 + 2];
				els[0] = (isClosed) ? Argument.GEOMETRY_POLYGON : Argument.GEOMETRY_LINESTRING;
				els[1] = nNodes;
				int elsIndex = 2;
				int segType = Argument.MOVETO;
				Iterator<String> it = m_wayNodes.iterator();
				while (it.hasNext())
				{
					String nodeId = it.next();
					double []nodeEls = m_allNodes.get(nodeId);
					els[elsIndex] = segType;
					els[elsIndex + 1] = nodeEls[3];
					els[elsIndex + 2] = nodeEls[4];
					elsIndex += 3;
					segType = Argument.LINETO;
				}
				Argument geometryArgument = new Argument((int)els[0], els);
				row.add(geometryArgument);
				if (m_tags != null)
					row.add(m_tags);
				else
					row.add(Argument.emptyString);
				m_tags = null;
				m_data.add(row);
			}
		}
	}

	public String getProjection()
	{
		return "GEOGCS[\"wgs84\",DATUM[\"WGS_1984\",SPHEROID[\"wgs84\",6378137,298.257223563],TOWGS84[0.000,0.000,0.000]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]";
	}

	public Hashtable<String, String> getMetadata()
	{
		Hashtable<String, String> retval = new Hashtable<String, String>();
		retval.put("version", m_OSMVersion);
		retval.put("generator", m_OSMGenerator);
		return retval;
	}

	public String[] getFieldNames()
	{
		return(FIELD_NAMES);
	}

	public Rectangle2D.Double getWorlds()
	{
		return(new Rectangle2D.Double(m_xMin, m_yMin, m_xMax - m_xMin, m_yMax - m_yMin));
	}

	public Row fetch() throws MapyrusException
	{
		Row retval;
		if (m_data == null || m_data.isEmpty())
			retval = null;
		else
			retval = m_data.removeFirst();
		return(retval);
	}

	public void close() throws MapyrusException
	{
		m_data = null;
	}
}
