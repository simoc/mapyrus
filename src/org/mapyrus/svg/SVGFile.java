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
package org.mapyrus.svg;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapyrus.Constants;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Provides functions for parsing Scalable Vector Graphics (SVG) format files.
 */
public class SVGFile extends DefaultHandler
{
	private Rectangle2D m_boundingBox;
	private StringBuffer m_contents;
	private StringBuffer m_SVGAttributes;
	private int m_SVGTagCount;

	/**
	 * Open PostScript file and parse header information.
	 * @param filename name of PostScript file to read.
	 */
	public SVGFile(String filename) throws IOException, MapyrusException
	{
		InputStream stream = null;
		try
		{
			XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			xmlReader.setContentHandler(this);
			xmlReader.setEntityResolver(this);
			stream = new FileOrURL(filename).getInputStream();
			xmlReader.parse(new InputSource(stream));
		}
		catch (ParserConfigurationException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SVG) +
				": " + filename + ": " + e.getMessage());
		}
		catch (SAXException e2)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SVG) +
				": " + filename + ": " + e2.getMessage());
		}
		finally
		{
			try
			{
				if (stream != null)
					stream.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Return bounding rectangle of SVG file.
	 * @return bounding box.
	 */
	public Rectangle2D getBoundingBox()
	{
		return(m_boundingBox);
	}

	public void startDocument()
	{
		m_contents = new StringBuffer(1024);
		m_SVGAttributes = new StringBuffer(256);
	}

	public InputSource resolveEntity(String publicId, String systemId)
	{
		return new InputSource(new StringReader(""));
	}

	/**
	 * Parse size from SVG file.
	 * @param s size with optional units.
	 * @return size in points (72 per inch).
	 */
	private double parseSize(String s)
	{
		double retval, factor = 1;
		if (s.endsWith("pt"))
		{
			s = s.substring(0, s.length() - 2);
		}
		else if (s.endsWith("px"))
		{
			s = s.substring(0, s.length() - 2);
			factor = (double)Constants.POINTS_PER_INCH / Constants.getScreenResolution();
		}
		else if (s.endsWith("mm"))
		{
			s = s.substring(0, s.length() - 2);
			factor = Constants.POINTS_PER_INCH / Constants.MM_PER_INCH;
		}
		else if (s.endsWith("cm"))
		{
			s = s.substring(0, s.length() - 2);
			factor = 10 * Constants.POINTS_PER_INCH / Constants.MM_PER_INCH;
		}
		else if (s.endsWith("in"))
		{
			s = s.substring(0, s.length() - 2);
			factor = Constants.POINTS_PER_INCH;
		}
		retval = java.lang.Double.parseDouble(s);
		return(retval * factor);
	}

	public void startElement(String uri, String localName,
		String qName, Attributes attributes)
	{
		if (qName.equals("svg"))
			m_SVGTagCount++;
		if (qName.equals("svg") && m_SVGTagCount == 1)
		{
			/*
			 * Parse width and height of SVG file from outermost SVG tag.
			 */
			String width = attributes.getValue("width");
			String height = attributes.getValue("height");
			m_boundingBox = new Rectangle2D.Double(0, 0,
				parseSize(width), parseSize(height));

			/*
			 * Save attributes of SVG element defining XML namespaces.
			 */
			for (int i = 0; i < attributes.getLength(); i++)
			{
				if ((!attributes.getQName(i).equals("width")) &&
					(!attributes.getQName(i).equals("height")))
				{
					m_SVGAttributes.append(attributes.getQName(i));
					m_SVGAttributes.append("=\"");
					m_SVGAttributes.append(attributes.getValue(i));
					m_SVGAttributes.append("\"");
					m_SVGAttributes.append(Constants.LINE_SEPARATOR);
				}
			}
		}
		else
		{
			m_contents.append("<").append(qName).append(" ");
			for (int i = 0; i < attributes.getLength(); i++)
			{
				m_contents.append(attributes.getQName(i));
				m_contents.append("=\"");
				m_contents.append(attributes.getValue(i));
				m_contents.append("\"");
				m_contents.append(Constants.LINE_SEPARATOR);
				
			}
			m_contents.append(">");
		}
	}

	public void endElement(String uri, String localName, String qName)
	{
		if (qName.equals("svg"))
		{
			m_SVGTagCount--;	
			if (m_SVGTagCount > 0)
				m_contents.append("</svg>");
		}
		else
		{
			m_contents.append("</").append(qName).append(">");
		}
	}

	public void characters(char[] ch, int start, int length)
	{
		for (int i = 0; i < length; i++)
		{
			char c = ch[start + i];
			if (c == '&' || c == '<' || c == '>' || c == '"' || c > 127)
			{
					/*
					 * Give character codes for special XML characters.
					 */
				m_contents.append("&#").append(Integer.toString(c)).append(";");
			}
			else
			{
				m_contents.append(c);
			}
		}
	}

	/**
	 * Return contents of SVG file as a string.
	 * @return contents of SVG file.
	 */	
	public String toString()
	{
		return(m_contents.toString());
	}

	/**
	 * Return attributes in svg tag of SVG file.
	 * @return attributes as XML attribute string.
	 */	
	public String getSVGAttributes()
	{
		return(m_SVGAttributes.toString());
	}
}
