/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2008 Simon Chenery.
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
	private LinkedList<Row> mData;

	/*
	 * World extents of data read from XML file.
	 */
	private double mXMin = Double.MAX_VALUE;
	private double mYMin = Double.MAX_VALUE;
	private double mXMax = Double.MIN_VALUE;
	private double mYMax = Double.MIN_VALUE;

	/*
	 * Lookup table of nodes for construction of ways from a list of node IDs.
	 */
	private HashMap<String, double []> mAllNodes;

	/*
	 * State during XML parsing. 
	 */
	private String mOSMVersion = "";
	private String mOSMGenerator = "";
	private String mNodeId = "";
	private String mWayId = "";
	private double mLat = 0;
	private double mLon = 0;
	private boolean mVisible = true;
	private Argument mTags = null;
	private ArrayList<String> mWayNodes = null;

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
			mData = new LinkedList<Row>();
			mAllNodes = new HashMap<String, double []>();

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
			mVisible = Boolean.parseBoolean(visible);
			if (mVisible)
			{
				/*
				 * Encountered a node, a single (X, Y) value.
				 */
				mNodeId = getAttribute(qName, attributes, "id");
				String lat = getAttribute(qName, attributes, "lat");
				try
				{
					mLat = Double.parseDouble(lat);
				}
				catch (NumberFormatException e)
				{
					throw new SAXException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": <node> lat: " + lat);
				}

				String lon = getAttribute(qName, attributes, "lon");
				try
				{
					mLon = Double.parseDouble(lon);
				}
				catch (NumberFormatException e)
				{
					throw new SAXException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": <node> lon: " + lon);
				}
				mTags = null;

				/*
				 * Build bounding box of data in XML file.
				 */
				if (mLat < mYMin)
					mYMin = mLat;
				if (mLat > mYMax)
					mYMax = mLat;
				if (mLon < mXMin)
					mXMin = mLon;
				if (mLon > mXMax)
					mXMax = mLon;
			}
		}
		else if (qName.equals("way"))
		{
			/*
			 * Encountered a way, referring to a list of nodes.
			 */
			String visible = getAttribute(qName, attributes, "visible");
			mVisible = Boolean.parseBoolean(visible);
			if (mVisible)
			{
				mWayId = getAttribute(qName, attributes, "id");
				mWayNodes = new ArrayList<String>();
				mTags = null;
			}
		}
		else if (qName.equals("tag") && mVisible)
		{
			/*
			 * Create hash table entry for key-value pairs for node or way.
			 */
			if (mTags == null)
				mTags = new Argument();
			String k = getAttribute(qName, attributes, "k");
			String v = getAttribute(qName, attributes, "v");
			mTags.addHashMapEntry(k, new Argument(Argument.STRING, v));
		}
		else if (qName.equals("nd") && mVisible)
		{
			/*
			 * Add node to list of nodes for a way.
			 */
			String ref = getAttribute(qName, attributes, "ref");
			mWayNodes.add(ref);
		}
		else if (qName.equals("osm"))
		{
			/*
			 * Get header information.
			 */
			mOSMVersion = getAttribute(qName, attributes, "version");
			mOSMGenerator = getAttribute(qName, attributes, "generator");
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		if (qName.equals("node"))
		{
			if (mVisible)
			{
				/*
				 * Build a dataset row for node we have finished reading.
				 */
				Row row = new Row(FIELD_NAMES.length);
				row.add(NODE_TYPE_ARGUMENT);
				row.add(new Argument(Argument.STRING, mNodeId));
				double []els = new double[]{Argument.GEOMETRY_POINT, 1, Argument.MOVETO, mLon, mLat};
				Argument geometryArgument = new Argument(Argument.GEOMETRY_POINT, els);
				row.add(geometryArgument);
				if (mTags != null)
					row.add(mTags);
				else
					row.add(Argument.emptyString);
				mTags = null;
				mData.add(row);
				mAllNodes.put(mNodeId, els);
			}
		}
		else if (qName.equals("way"))
		{
			if (mVisible)
			{
				/*
				 * Build a dataset row for the way we have finished reading.
				 */
				Row row = new Row(FIELD_NAMES.length);
				row.add(WAY_TYPE_ARGUMENT);
				row.add(new Argument(Argument.STRING, mWayId));
				
				/*
				 * Determine if way is a closed polygon.
				 */
				boolean isClosed = false;
				int nNodes = mWayNodes.size();
				if (nNodes > 1)
				{
					String startNodeId = mWayNodes.get(0);
					String endNodeId = mWayNodes.get(nNodes - 1);
					double []startNodeEls = mAllNodes.get(startNodeId);
					double []endNodeEls = mAllNodes.get(endNodeId);
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
				Iterator<String> it = mWayNodes.iterator();
				while (it.hasNext())
				{
					String nodeId = it.next();
					double []nodeEls = mAllNodes.get(nodeId);
					els[elsIndex] = segType;
					els[elsIndex + 1] = nodeEls[3];
					els[elsIndex + 2] = nodeEls[4];
					elsIndex += 3;
					segType = Argument.LINETO;
				}
				Argument geometryArgument = new Argument((int)els[0], els);
				row.add(geometryArgument);
				if (mTags != null)
					row.add(mTags);
				else
					row.add(Argument.emptyString);
				mTags = null;
				mData.add(row);
			}
		}
	}

	public String getProjection()
	{
		return "GEOGCS[\"wgs84\",DATUM[\"WGS_1984\",SPHEROID[\"wgs84\",6378137,298.257223563],TOWGS84[0.000,0.000,0.000]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]";
	}

	public Hashtable getMetadata()
	{
		Hashtable<String, String> retval = new Hashtable<String, String>();
		retval.put("version", mOSMVersion);
		retval.put("generator", mOSMGenerator);
		return retval;
	}

	public String[] getFieldNames()
	{
		return(FIELD_NAMES);
	}

	public Rectangle2D.Double getWorlds()
	{
		return(new Rectangle2D.Double(mXMin, mYMin, mXMax - mXMin, mYMax - mYMin));
	}

	public Row fetch() throws MapyrusException
	{
		Row retval;
		if (mData == null || mData.isEmpty())
			retval = null;
		else
			retval = mData.removeFirst();
		return(retval);
	}

	public void close() throws MapyrusException
	{
		mData = null;
	}
}
