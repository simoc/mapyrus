/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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

package org.mapyrus.dataset;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapyrus.Argument;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Parses OGC Well Known Binary (WKB) geometry structures read from a database
 * into similar geometry structure used by Mapyrus.
 */
public class WKBGeometryParser
{
	/*
	 * Possible byte order of geometries.
	 */
	private static byte BIG_ENDIAN = 0;
	private static byte LITTLE_ENDIAN = 1;

	/*
	 * Geometry types.
	 */
	private static int WKB_POINT = 1;
	private static int WKB_LINESTRING = 2;
	private static int WKB_POLYGON = 3;
	private static int WKB_MULTIPOINT = 4;
	private static int WKB_MULTILINESTRING = 5;
	private static int WKB_MULTIPOLYGON = 6;
	private static int WKB_GEOMETRY_COLLECTION = 7;

	/**
	 * Convert bytes in ByteBuffer to hex digits.
	 * @param b first byte of buffer.
	 * @param byteBuffer remaining bytes in buffer.
	 * @return first few bytes of buffer as a hex string.
	 */
	private static String convertToHexDigits(int b, ByteBuffer byteBuffer)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("0x");
		b = (b & 255);
		String hex = Integer.toHexString(b);
		if (hex.length() == 1)
			sb.append("0");
		sb.append(hex);
		for (int i = 0; i < 6; i++)
		{
			if (byteBuffer.hasRemaining())
			{
				b = byteBuffer.get();
				b = (b & 255);
				hex = Integer.toHexString(b);
				if (hex.length() == 1)
					sb.append("0");
				sb.append(hex);
			}
		}
		if (byteBuffer.hasRemaining())
			sb.append("...");
		return(sb.toString());
	}

	/**
	 * Parse a geometry from WKB buffer.
	 * Called recursively to parse geometries made up of multiple parts.
	 * @param byteBuffer buffer containg WKB geometry.
	 * @param geometry double array to fill with in Mapyrus geometry format.
	 * @param geometryIndex index at which to start filling geometry array.
	 * @return number of elements filled in geometry array.
	 * @throws MapyrusException
	 */
	private static int parseGeometry(ByteBuffer byteBuffer,
		double []geometry, int geometryIndex) throws MapyrusException
	{
		int index = geometryIndex;
		int nPoints, nLines, nRings, nPolygons, nGeometries;

		/*
		 * If buffer is not long enough to hold the shortest geometry (a point)
		 * then blob cannot possibly hold a valid geometry.
		 */
		if (byteBuffer.remaining() < 1 + 4 + 8 + 8)
		{
			String s = "";
			if (byteBuffer.hasRemaining())
				s = convertToHexDigits(byteBuffer.get(), byteBuffer);
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKB) +
				": " + s);
		}

		/*
		 * Parse byte order of this geometry.
		 */
		byte order = byteBuffer.get();
		if (order == BIG_ENDIAN)
			byteBuffer.order(ByteOrder.BIG_ENDIAN);
		else if (order == LITTLE_ENDIAN)
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKB) +
				": " + convertToHexDigits(order, byteBuffer));
		}

		/*
		 * Find type of geometry in buffer, then extract it from buffer.
		 */
		int wkbType = byteBuffer.getInt();
		if (wkbType == WKB_POINT)
		{
			geometry[index++] = Argument.GEOMETRY_POINT;
			geometry[index++] = 1;
			geometry[index++] = Argument.MOVETO;
			geometry[index++] = byteBuffer.getDouble();
			geometry[index++] = byteBuffer.getDouble();
		}
		else if (wkbType == WKB_LINESTRING)
		{
			geometry[index++] = Argument.GEOMETRY_LINESTRING;
			geometry[index++] = nPoints = byteBuffer.getInt();
			int operation = Argument.MOVETO;
			for (int i = 0; i < nPoints; i++)
			{
				geometry[index++] = operation;
				geometry[index++] = byteBuffer.getDouble();
				geometry[index++] = byteBuffer.getDouble();
				operation = Argument.LINETO;
			}
		}
		else if (wkbType == WKB_POLYGON)
		{
			geometry[index++] = Argument.GEOMETRY_POLYGON;
			int nPointsIndex = index++;
			int totalPoints = 0;
			nRings = byteBuffer.getInt();
			for (int j = 0; j < nRings; j++)
			{
				nPoints = byteBuffer.getInt();
				totalPoints += nPoints;
				int operation = Argument.MOVETO;
				for (int i = 0; i < nPoints; i++)
				{
					geometry[index++] = operation;
					geometry[index++] = byteBuffer.getDouble();
					geometry[index++] = byteBuffer.getDouble();
					operation = Argument.LINETO;
				}
			}
			geometry[nPointsIndex] = totalPoints;
		}
		else if (wkbType == WKB_MULTIPOINT)
		{
			geometry[index++] = Argument.GEOMETRY_MULTIPOINT;
			geometry[index++] = nPoints = byteBuffer.getInt();
			for (int i = 0; i < nPoints; i++)
			{
				int nEls = parseGeometry(byteBuffer, geometry, index);
				index += nEls;
			}
		}
		else if (wkbType == WKB_MULTILINESTRING)
		{
			geometry[index++] = Argument.GEOMETRY_MULTILINESTRING;
			geometry[index++] = nLines = byteBuffer.getInt();
			for (int i = 0; i < nLines; i++)
			{
				int nEls = parseGeometry(byteBuffer, geometry, index);
				index += nEls;
			}
		}
		else if (wkbType == WKB_MULTIPOLYGON)
		{
			geometry[index++] = Argument.GEOMETRY_MULTIPOLYGON;
			geometry[index++] = nPolygons = byteBuffer.getInt();
			for (int i = 0; i < nPolygons; i++)
			{
				int nEls = parseGeometry(byteBuffer, geometry, index);
				index += nEls;
			}
		}
		else if (wkbType == WKB_GEOMETRY_COLLECTION)
		{
			geometry[index++] = Argument.GEOMETRY_COLLECTION;
			geometry[index++] = nGeometries = byteBuffer.getInt();
			for (int i = 0; i < nGeometries; i++)
			{
				int nEls = parseGeometry(byteBuffer, geometry, index);
				index += nEls;
			}
		}
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKB) +
				": " + convertToHexDigits(wkbType, byteBuffer));
		}

		/*
		 * Return number of slots filled by this geometry.
		 */
		return(index - geometryIndex);
	}

	/**
	 * Parse WKB geometry into geometry used by Mapyrus.
	 * @param b byte array containing geometry
	 * @return double array containing geometry in Mapyrus format.
	 */
	public static double []parse(byte []b) throws MapyrusException
	{
		double []retval = new double[(b.length + 7) / 8 * 2];
		ByteBuffer byteBuffer = ByteBuffer.wrap(b);

		parseGeometry(byteBuffer, retval, 0);
		return(retval);
	}
}
