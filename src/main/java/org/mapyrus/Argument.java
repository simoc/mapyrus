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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * An argument is a literal value, array of literal values, or a variable name.
 * Each field read from a dataset is an argument.
 * Several arguments can be combined with operators to make an expression
 * For example, '2 * a + 7' and 'prefix["DE"] . "11823"' both contain three
 * arguments.
 */
public class Argument implements Comparable<Argument>, Cloneable
{
	public static final int NUMERIC = 0;
	public static final int STRING = 1;
	public static final int HASHMAP = 2;
	public static final int VARIABLE = 3;
	public static final int GEOMETRY = 4;

	public static final int GEOMETRY_POINT = 100;
	public static final int GEOMETRY_LINESTRING = 101;
	public static final int GEOMETRY_POLYGON = 102;
	public static final int GEOMETRY_MULTIPOINT = 103;
	public static final int GEOMETRY_MULTILINESTRING = 104;
	public static final int GEOMETRY_MULTIPOLYGON = 105;
	public static final int GEOMETRY_COLLECTION = 106;

	/*
	 * Markers in geometry array marking each segment of a geometry.
	 */
	public static final int MOVETO = 0;
	public static final int LINETO = 1;

	/**
	 * Constant for numeric value zero.
	 * Avoids allocating many arguments for this commonly used value.
	 */
	public static final Argument numericZero = new Argument(0.0);

	/**
	 * Constant for numeric value one.
	 * Avoids allocating many arguments for this commonly used value.
	 */
	public static final Argument numericOne = new Argument(1.0);

	/**
	 * Constant for numeric value minus one.
	 * Avoids allocating many arguments for this commonly used value.
	 */
	public static final Argument numericMinusOne = new Argument(-1.0);

	/**
	 * Constant for empty string.
	 * Avoids allocating many arguments for this commonly used value.
	 */
	public static final Argument emptyString = new Argument(Argument.STRING, "");

	/**
	 * Constant for empty geometry.
	 * Avoids allocating many arguments for null geometries.
	 */
	public static final Argument emptyGeometry =
		new Argument(Argument.GEOMETRY_POINT,
			new double[]{Argument.GEOMETRY_POINT, 0});

	private int m_type;
	private double m_numericValue;
	private String m_stringValue;
	private String m_varname;
	private double []m_geometryValue;
	private Rectangle2D.Double m_geometryBoundingBox;
	private HashMap<String, Argument> m_hashMap;

	/**
	 * Create a new numeric argument.
	 * @param d is value for this argument.
	 */
	public Argument(double d)
	{
		m_type = NUMERIC;
		m_numericValue = d;
	}

	/**
	 * Create a new string argument, or variable name.
	 * @param type is STRING to create a string argument,
	 * or a VARIABLE to create
	 * a reference to a variable.
	 * @param s is string, or variable name.
	 */
	public Argument(int type, String s)
	{
		m_type = type;

		if (type == STRING)
			m_stringValue = s;
		else
			m_varname = s;

		/*
		 * We don't know the numeric or geometry value of this argument.
		 */
		m_numericValue = Double.NaN;
		m_geometryValue = null;
	}

	/**
	 * Create new, empty hashmap argument.
	 * Use addHashMapEntry() method to add values to the hash map.
	 */
	public Argument()
	{
		m_type = HASHMAP;
		m_hashMap = new HashMap<String, Argument>();	
	}

	/**
	 * Create a new geometry argument for all types of geometry.
	 * @param geometryType OGC geometry type.
	 * @param coords is array containing (X, Y) coordinates of point(s),
	 * line(s), polygon(s).
	 *
	 * For simple geometry, array elements are of the form:
	 * <pre>
	 * GEOMETRY_POINT, 1, MOVE, x, y
	 * 
	 * GEOMETRY_LINESTRING, 2, MOVE, x0, y0, DRAW, x1, y1
	 * 
	 * GEOMETRY_POLYGON, 4, MOVE, x0, y0, DRAW, x1, y1, DRAW, x2, y2, DRAW, x3, y3
	 * </pre>
	 * 
	 * where second element gives number of coordinates in geometry.
	 * 
	 * For multiple geometry types, array elements are of the form:
	 * <pre>
	 * GEOMETRY_MULTIPOINT, 2, GEOMETRY_POINT 1, MOVE, x, y, GEOMETRY_POINT, 1, MOVE, x, y
	 * 
	 * GEOMETRY_MULTILINESTRING, 2, GEOMETRY_LINESTRING, 2, MOVE, x0, y0, DRAW x1, y1,
	 * GEOMETRY_LINESTRING, 3, MOVE, x0, y0, DRAW x1, y1, DRAW x2, y2
	 * 
	 * GEOMETRY_MULTIPOLYGON, 2, GEOMETRY_POLYGON, 4, MOVE, x0, y0, DRAW, x1, y1,
	 * DRAW, x2, y2, DRAW, x3, y3, GEOMETRY_POLYGON, 4, MOVE, x0, y0, DRAW, x1, y1,
	 * DRAW, x2, y2, DRAW, x3, y3
	 * 
	 * GEOMETRY_COLLECTION, 2, GEOMETRY_POINT, 1, MOVE, x, y,
	 * GEOMETRY_LINESTRING, 2, MOVE, x0, y0, DRAW, x1, y1
	 * </pre>
	 * 
	 * where the second element gives the number of geometries included in
	 * the multi-geometry.
	 * 
	 */
	public Argument(int geometryType, double []coords)
	{
		m_type = geometryType;
		m_geometryValue = coords;
	}

	/**
	 * Parse parenthesised coordinate list from OGC geometry string
	 * into array.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry string as tokens.
	 * @param geometryIndex index into geometry array to add coordinates.
	 * @param isMultiPoint flag true when all coordinates to be added as
	 * MULTIPOINT geometry.
	 * @return number of coordinate pairs added to geometry array.
	 */
	private int parseCoordinateList(String wktGeometry, StringTokenizer st,
		int geometryIndex, boolean isMultiPoint)
		throws MapyrusException
	{
		String token;
		boolean foundOpenParen, foundCloseParen, foundEmpty;
		boolean foundNestedOpenParen, foundNestedCloseParen;
		boolean foundX, foundY;
		int index = geometryIndex;
		int counter = 0;

		/*
		 * First expect a '(' or the keyword 'EMPTY'.
		 */
		foundOpenParen = foundCloseParen = foundEmpty = false;
		foundNestedOpenParen = foundNestedCloseParen = false;
		foundX = foundY = false;
		while (foundOpenParen == false && foundEmpty == false && st.hasMoreTokens())
		{
			token = st.nextToken();
			foundOpenParen = token.equals("(");
			foundEmpty = token.equals("EMPTY");
		}

		/*
		 * Then expect a series of X and Y coordinate value separated
		 * by commas, and then a ')' to finish the coordinate string.
		 */
		if (foundOpenParen)
		{
			while (foundCloseParen == false && st.hasMoreTokens())
			{
				token = st.nextToken();
				char c = token.charAt(0);
				if (Character.isDigit(c) || c == '-' || c == '.')
				{
					try
					{
						if (foundX == false)
						{
							if (isMultiPoint)
							{
								/*
								 * Set each point in multipoint geometry as
								 * a separate point geometry.
								 */
								m_geometryValue[index] = GEOMETRY_POINT;
								m_geometryValue[index + 1] = 1;
								m_geometryValue[index + 2] = MOVETO;
								index += 3;
							}
							else if (counter == 0)
							{
								/*
								 * Move to first coordinate pair, then add a
								 * line to each subsequent pair.
								 */
								m_geometryValue[index] = MOVETO;
								index++;
							}
							else
							{
								m_geometryValue[index] = LINETO;
								index++;
							}

							m_geometryValue[index] = Double.parseDouble(token);
							index++;
							foundX = true;
						}
						else if (foundY == false)
						{
							m_geometryValue[index] = Double.parseDouble(token);
							index++;
							foundY = true;
							counter++;
						}
						else
						{
							/*
							 * Not expecting an X or Y coordinate.
							 * The OGC string must be invalid.
							 */
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
								": " + wktGeometry);
						}
					}
					catch (NumberFormatException e)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
							": " + wktGeometry);
					}
				}
				else if (isMultiPoint && c == '(' && foundX == false && foundNestedOpenParen == false)
				{
					/*
					 * Allow nested parentheses for second form of MULTIPOINT geometry like
					 * MULTIPOINT ( ( 1 2 ) , ( 3 4 ) )
					 */
					foundNestedOpenParen = true;
					foundNestedCloseParen = false;
				}
				else if (c == ')' && foundY)
				{
					/*
					 * Found closing parenthesis marking end of coordinates.
					 */
					if (foundNestedOpenParen && foundNestedCloseParen == false)
					{
						foundNestedOpenParen = false;
						foundNestedCloseParen = true;
					}
					else
					{
						foundCloseParen = true;
					}
				}
				else if (c == ',' && foundY)
				{
					/*
					 * Found comma separating coordinate pairs
					 */
					foundX = foundY = false;
				}
				else if (c != ' ')
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
						": " + wktGeometry);
				}
			}
		}

		/*
		 * Did we successively parse something?
		 */
		if ((foundOpenParen && foundCloseParen == false) ||
			(foundNestedOpenParen && foundNestedCloseParen == false) ||
			(foundX && (!foundY)))
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
				": " + wktGeometry);
		}
		return(counter);
	}

	/**
	 * Parse polygon from OGC geometry string into array.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry coordinates as tokens.
	 * @param geometryIndex index into geometry array to add coordinates.
	 * @param isMultiLinestring flag true when all coordinates to be added
	 * as MULTILINESTRING geometries
	 * @return index of next free position in geometry array after
	 * adding coordinates.
	 */
	private int parseRing(String wktGeometry, StringTokenizer st,
		int geometryIndex, boolean isMultiLinestring)
		throws MapyrusException
	{
		String token;
		int counter = 0;
		int nCoords;
		int index = geometryIndex + 1;

		/*
		 * Parse opening '('.  Then one or more lists of coordinates surrounded
		 * by parentheses and separated by commas.  Then parse closing ')'.
		 */
		boolean foundCloseParen = false;
		boolean foundCoordList = false;
		boolean foundEmptyList = false;

		foundCoordList = foundEmptyList = false;
		while (foundCoordList == false && foundEmptyList == false && st.hasMoreTokens())
		{
			token = st.nextToken();
			foundCoordList = token.equals("(");
			foundEmptyList = token.equals("EMPTY");
		}

		if (foundEmptyList == false)
		{
			while (foundCoordList && foundCloseParen == false && st.hasMoreTokens())
			{
				/*
				 * Parse coordinate list like '(10 20, 30 50)'.
				 */
				if (isMultiLinestring)
				{
					m_geometryValue[index] = GEOMETRY_LINESTRING;
					index++;
					nCoords = parseCoordinateList(wktGeometry, st, index + 1, false);
					m_geometryValue[index] = nCoords;
					index++;
					counter++;
				}
				else
				{
					nCoords = parseCoordinateList(wktGeometry, st, index, false);
					counter += nCoords;
				}

				index += nCoords * 3;

				/*
				 * Is there another coordinate list following this one?
				 */
				foundCoordList = false;
				while (foundCoordList == false && foundCloseParen == false && st.hasMoreTokens())
				{
					token = st.nextToken();
					foundCoordList = token.equals(",");
					foundCloseParen = token.equals(")");
				}
			}

			/*
			 * If we did not parse right through to the ')' then there is
			 * something wrong with the geometry string.
			 */
			if (!foundCloseParen)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
					": " + wktGeometry);
			}
		}

		/*
		 * Set number of coordinate pairs for POLYGON,
		 * number of sub-geometries for MULTILINESTRING. 
		 */
		m_geometryValue[geometryIndex] = counter;  
		return(index);
	}

	/**
	 * Parse OGC geometry string.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry coordinates as tokens.
	 * @param index index into geometry array to add geometry type
	 * and coordinates.
	 * @return index of next free position in geometry array after
	 * adding coordinates.
	 */
	private int parseGeometry(String wktGeometry, StringTokenizer st, int index)
		throws MapyrusException
	{
		String token;
		String ogcType;
		int nCoords;
		boolean foundOpenParen, foundCloseParen, foundComma, foundEmptyList;

		if (st.hasMoreTokens())
		{
			/*
			 * Find geometry type, skipping any leading spaces.
			 */
			ogcType = st.nextToken();
			while (ogcType.equals(" ") && st.hasMoreTokens())
				ogcType = st.nextToken();

			/*
			 * What type of geometry is this?
			 */
			if (ogcType.equals("POINT"))
			{
				m_geometryValue[index] = GEOMETRY_POINT;
				index++;
				nCoords = parseCoordinateList(wktGeometry, st, index + 1, false);
				m_geometryValue[index] = nCoords;
				index++;
				index += nCoords * 3;
			}
			else if (ogcType.equals("LINESTRING"))
			{
				m_geometryValue[index] = GEOMETRY_LINESTRING;
				index++;
				nCoords = parseCoordinateList(wktGeometry, st, index + 1, false);
				m_geometryValue[index] = nCoords;
				index++;
				index += nCoords * 3;
			}
			else if (ogcType.equals("MULTIPOINT"))
			{
				m_geometryValue[index] = GEOMETRY_MULTIPOINT;
				index++;
				nCoords = parseCoordinateList(wktGeometry, st, index + 1, true);
				m_geometryValue[index] = nCoords;
				index++;
				index += nCoords * 5; /* 5 values for each POINT sub-geometry */
			}
			else
			{
				if (ogcType.equals("POLYGON"))
					m_geometryValue[index] = GEOMETRY_POLYGON;
				else if (ogcType.equals("MULTILINESTRING"))
					m_geometryValue[index] = GEOMETRY_MULTILINESTRING;
				else if (ogcType.equals("MULTIPOLYGON"))
					m_geometryValue[index] = GEOMETRY_MULTIPOLYGON;
				else if (ogcType.equals("GEOMETRYCOLLECTION"))
					m_geometryValue[index] = GEOMETRY_COLLECTION;
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
						": " + wktGeometry);
				}

				if (m_geometryValue[index] == GEOMETRY_POLYGON)
				{
					index = parseRing(wktGeometry, st, index + 1, false);
				}
				else if (m_geometryValue[index] == GEOMETRY_MULTILINESTRING)
				{
					index = parseRing(wktGeometry, st, index + 1, true);
				}
				else
				{
					/*
					 * Parse any number of polygons or geometries surrounded
					 * by parentheses.
					 */
					foundOpenParen = foundCloseParen = foundEmptyList = false;
					while (foundOpenParen == false && foundEmptyList == false && st.hasMoreTokens())
					{
						token = st.nextToken();
						foundOpenParen = token.equals("(");
						foundEmptyList = token.equals("EMPTY");
					}

					int geometryType = (int)m_geometryValue[index];
					int counter = 0;
					int counterIndex = index + 1;
					index += 2;

					while (foundOpenParen && foundCloseParen == false && st.hasMoreTokens())
					{
						if (geometryType == GEOMETRY_MULTIPOLYGON)
						{
							m_geometryValue[index] = GEOMETRY_POLYGON;
							index = parseRing(wktGeometry, st, index + 1, false);
						}
						else
						{
							index = parseGeometry(wktGeometry, st, index);
						}

						counter++;

						foundComma = false;
						while (foundComma == false &&
							foundCloseParen == false && st.hasMoreTokens())
						{
							token = st.nextToken();
							if (token.equals(","))
								foundComma = true;
							else if (token.equals(")"))
								foundCloseParen = true;
						}
					}

					/*
					 * Set number of sub-geometries in
					 * MULTIPOLYGON or GEOMETRYCOLLECTION. 
					 */
					m_geometryValue[counterIndex] = counter;

					if (foundOpenParen && foundCloseParen == false)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
							": " + wktGeometry);
					}
				}
			}
		}
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
				": " + wktGeometry);
		}
		return(index);
	}

	/**
	 * Create new geometry argument from OGC well known geometry string.
	 * @param wktGeometry OGC geometry string.
	 * @throws MapyrusException if string is not a valid OGC geometry string.
	 */
	public Argument(String wktGeometry) throws MapyrusException
	{
		StringTokenizer st = new StringTokenizer(wktGeometry, ",() ", true);
		m_geometryValue = new double[st.countTokens() + 1];
		parseGeometry(wktGeometry, st, 0);
		m_type = (int)m_geometryValue[0];
	}

	/**
	 * Returns type of argument.
	 * @return either NUMERIC, STRING, VARIABLE, or GEOMETRY.
	 */	
	public int getType()
	{
		int retval;
		if (m_type == NUMERIC || m_type == STRING || m_type == VARIABLE || m_type == HASHMAP)
			retval = m_type;
		else
			retval = GEOMETRY;
		return(retval);
	}

	/**
	 * Returns numeric value of argument.
	 * @return numeric argument value,
	 * or zero if it cannot be converted to a number.
	 * @throws MapyrusException if argument is an array or geometry.
	 */
	public double getNumericValue() throws MapyrusException
	{
		if (m_type == STRING)
		{
			if (Double.isNaN(m_numericValue))
			{
				/*
				 * Argument is a string that we've not tried
				 * converting to a number before.  Find it's numeric
				 * value now.
				 */
				try
				{
					m_numericValue = Double.parseDouble(m_stringValue);
				}
				catch (NumberFormatException e)
				{
					m_numericValue = 0;
				}
			}
		}
		else if (m_type == HASHMAP)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.HASHMAP_NOT_NUMERIC));
		}
		else if (m_type != NUMERIC)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.GEOMETRY_NOT_NUMERIC));
		}
		return(m_numericValue);
	}

	/**
	 * Returns value of argument as a string.
	 * @return argument value as a string.
	 */
	public String getStringValue()
	{
		return(toString());
	}

	/**
	 * Returns variable name reference of argument.
	 * @return name of variable containing value for this argument.
	 */	
	public String getVariableName()
	{
		return(m_varname);
	}

	/**
	 * Returns value of geometry argument.
	 * @return geometry argument value.
	 * @throws MapyrusException if argument is not geometry and cannot be converted to geometry.
	 */
	public double []getGeometryValue() throws MapyrusException
	{
		if (m_type == NUMERIC || m_type == HASHMAP || m_type == VARIABLE)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOMETRY));

		if (m_geometryValue == null)
		{
			/*
			 * Try to convert string argument to a geometry, then store it so
			 * we don't have to do conversion again for this argument.
			 */
			StringTokenizer st = new StringTokenizer(m_stringValue, ",() ", true);
			m_geometryValue = new double[st.countTokens() + 1];
			parseGeometry(m_stringValue, st, 0);
		}
		return(m_geometryValue);
	}

	/**
	 * Returns bounding box of a geometry argument.
	 * @param geometry array containing geometry coordinates.
	 * @param index index at which to start parsing geometry, returns
	 * array index beyond geometry that was scanned.
	 * @param boundingBox current bounding box of geometry already scanned.
	 * @return minimum bounding rectangle, or null if geometry is empty.
	 */
	private Rectangle2D.Double getGeometryBoundingBox(double []geometry,
		int index[], Rectangle2D.Double boundingBox)
	{
		int i = index[0];
		int geometryType = (int)geometry[i];
		int count = (int)geometry[i + 1];
		double x, y;

		/*
		 * Return nothing if geometry is empty.
		 */
		if (count == 0)
		{
			i += 2;
		}
		else
		{
			switch (geometryType)
			{
				case GEOMETRY_POINT:
					x = geometry[i + 3];
					y = geometry[i + 4];
					if (boundingBox == null)
						boundingBox = new Rectangle2D.Double(x, y, 0, 0);
					else
						boundingBox.add(x, y);
					i += 5;
					break;
				case GEOMETRY_LINESTRING:
				case GEOMETRY_POLYGON:
					x = geometry[i + 3];
					y = geometry[i + 4];

					if (boundingBox == null)
						boundingBox = new Rectangle2D.Double(x, y, 0, 0);
					else
						boundingBox.add(x, y);

					int nextIndex = 6;
					for (int j = 1; j < count; j++)
					{
						x = geometry[i + nextIndex];
						y = geometry[i + nextIndex + 1];
						boundingBox.add(x, y);
						nextIndex += 3;
					}
					i += 2 + count * 3;
					break;
				case GEOMETRY_MULTIPOINT:
				case GEOMETRY_MULTILINESTRING:
				case GEOMETRY_MULTIPOLYGON:
				case GEOMETRY_COLLECTION:
					i += 2;
					index[0] = i;

					for (int j = 0; j < count; j++)
					{
						/*
						 * Add bounding box of each sub-geometry
						 * to our geometry.
						 */
						boundingBox = getGeometryBoundingBox(geometry,
							index, boundingBox);
					}
					i = index[0];
					break;
			}
		}

		/*
		 * Return index of where parsing of next geometry should
		 * begin.
		 */
		index[0] = i;

		return(boundingBox);
	}

	/**
	 * Returns bounding box of a geometry argument.
	 * @return minimum bounding rectangle, or null if geometry is empty.
	 * @throws MapyrusException if argument is not geometry and cannot be converted to geometry.
	 */
	public Rectangle2D.Double getGeometryBoundingBox() throws MapyrusException
	{
		Rectangle2D.Double retval = m_geometryBoundingBox;

		if (retval == null)
		{
			/*
			 * Calculate the bounding box the first time it is requested,
			 * then remember it for later.
			 * Don't remember bounding rectangle for points -- we can
			 * easily calculate this each time it is needed.
			 */
			double geometry[] = getGeometryValue();
			int index[] = new int[1];
			index[0] = 0;
			retval = getGeometryBoundingBox(geometry, index, null);
			if (geometry[0] != GEOMETRY_POINT)
				m_geometryBoundingBox = retval;
		}
		return(retval);
	}

	/**
	 * Transform coordinates in geometry to new geometry.
	 * @param affine transform.
	 * @param srcGeometry coordinates of geometry.
	 * @param destGeometry array to write transformed coordinates into.
	 * @param index index in srcGeometry at which to start transforming.
	 * @return index in srcGeometry after parsed geometry.
	 */
	private int transformGeometry(AffineTransform affine,
		double []srcGeometry, double []destGeometry, int index)
		throws MapyrusException
	{
		int geometryType = (int)srcGeometry[index];
		int count = (int)srcGeometry[index + 1];

		/*
		 * Return nothing if geometry is empty.
		 */
		if (count == 0)
		{
			index += 2;
		}
		else
		{
			switch (geometryType)
			{
				case GEOMETRY_POINT:
					affine.transform(srcGeometry, index + 3, destGeometry, index + 3, 1);
					index += 5;
					break;
				case GEOMETRY_LINESTRING:
				case GEOMETRY_POLYGON:
					int nextIndex = index + 3;
					for (int j = 0; j < count; j++)
					{
						affine.transform(srcGeometry, nextIndex,
							destGeometry, nextIndex, 1);
						nextIndex += 3;
					}
					index += 2 + count * 3;
					break;
				case GEOMETRY_MULTIPOINT:
				case GEOMETRY_MULTILINESTRING:
				case GEOMETRY_MULTIPOLYGON:
				case GEOMETRY_COLLECTION:
					index += 2;
					for (int j = 0; j < count; j++)
					{
						/*
						 * Transform each sub-geometry.
						 */
						index = transformGeometry(affine,
							srcGeometry, destGeometry, index);
					}
					break;
			}
		}
		return(index);
	}

	/**
	 * Transform geometry through affine transformation and
	 * return new geometry argument.
	 * @param affine transformation.
	 * @return transformed geometry.
	 * @throws MapyrusException if argument is not geometry and cannot be converted to geometry.
	 */
	public Argument transformGeometry(AffineTransform affine)
		throws MapyrusException
	{
		double []geometry = getGeometryValue();
		double []copy = new double[geometry.length];
		System.arraycopy(geometry, 0, copy, 0, geometry.length);
		transformGeometry(affine, geometry, copy, 0);
		Argument retval = new Argument((int)copy[0], copy);
		return(retval);
	}

	/**
	 * Returns value of one entry in a hashmap.
	 * @param key is key to lookup.
	 * @return value associated with this key, or empty string argument
	 * if this key is not in hashmap.
	 */	
	public Argument getHashMapEntry(String key)
	{
		Argument retval = m_hashMap.get(key);
		if (retval == null)
			retval = emptyString;
		return(retval);
	}
	
	/**
	 * Add new key-value pair to a hash map.
	 * @param key key to add to hash map.
	 * @param value value to add for this key.
	 */
	public void addHashMapEntry(String key, Argument value)
	{
		m_hashMap.put(key, value);
	}

	/**
	 * Comparator for string lists.
	 * Orders list elements that contain numbers numerically, other
	 * elements alphabetically.
	 */
	private class NumericAndStringComparator implements Comparator<String>
	{
		@Override
		public int compare(String s1, String s2)
		{
			int retval;

			try
			{
				/*
				 * If both strings contain numbers then compare them
				 * numerically.
				 */
				int i1 = Integer.parseInt(s1);
				int i2 = Integer.parseInt(s2);
				retval = i1 - i2;
			}  
			catch (NumberFormatException e)
			{
				retval = s1.compareTo(s2);
			}
			return(retval);
		}
	}

	/**
	 * Get array of keys in hash map.
	 * @return keys in this hash map, each object being a string.
	 */
	public Object[] getHashMapKeys()
	{
		String []keys = new String[m_hashMap.size()];
		Iterator<String> it = m_hashMap.keySet().iterator();
		int i = 0;
		while (it.hasNext())
			keys[i++] = it.next();

		/*
		 * Return keys in sorted order, either numerically or alphabetically.
		 */
		Arrays.sort(keys, new NumericAndStringComparator());
		return(keys);
	}

	/**
	 * Get array of keys in hash map sorted by hash map values.
	 * @return keys in this hash map, each object being a string.
	 */
	public Object[] getHashMapKeysSortedByValue()
	{
		Object []keys = m_hashMap.keySet().toArray();

		/*
		 * Bubble sort keys into order based on value stored for each key.
		 */
		for (int i = 0; i < keys.length; i++)
		{
			for (int j = i + 1; j < keys.length; j++)
			{
				Argument arg1 = m_hashMap.get(keys[i]);
				Argument arg2 = m_hashMap.get(keys[j]);
				if (arg1.compareTo(arg2) > 0)
				{
					/*
					 * Swap values.
					 */
					Object swap = keys[i];
					keys[i] = keys[j];
					keys[j] = swap;
				}
			}
		}
		return(keys);
	}

	/**
	 * Get number of elements in hash map.
	 * @return size of hashmap.
	 */
	public int getHashMapSize()
	{
		return(m_hashMap.size());
	}

	/**
	 * Creates OGC WKT geometry string from geometry array.
	 * @param coords geometry type, count and move/draw coordinates.
	 * @param startIndex index in coords array to start converting.
	 * @param s buffer to append geometry to.
	 * @param addGeometryType if true then geometry type added to
	 * geometry string.
	 * @return index of next element in coords array to be parsed.
	 */
	private int createOGCWKT(double []coords, int startIndex, StringBuilder s, boolean addGeometryType)
	{
		int geometryType = (int)coords[startIndex];
		int nElements = (int)coords[startIndex + 1];
		int nextIndex = startIndex + 2;

		if (addGeometryType)
		{
			if (geometryType == GEOMETRY_POINT)
				s.append("POINT ");
			else if (geometryType == GEOMETRY_LINESTRING)
				s.append("LINESTRING ");
			else if (geometryType == GEOMETRY_POLYGON)
				s.append("POLYGON ");
			else if (geometryType == GEOMETRY_MULTIPOINT)
				s.append("MULTIPOINT ");
			else if (geometryType == GEOMETRY_MULTILINESTRING)
				s.append("MULTILINESTRING ");
			else if (geometryType == GEOMETRY_MULTIPOLYGON)
				s.append("MULTIPOLYGON ");
			else
				s.append("GEOMETRYCOLLECTION ");
		}

		if (nElements == 0)
		{
			/*
			 * Geometry is empty.
			 */
			s.append("EMPTY ");
		}
		else if (geometryType == GEOMETRY_POINT)
		{
			/*
			 * Convert point to OGC Well Known Text representation.
			 */
			if (addGeometryType)
				s.append("(");
			s.append(coords[nextIndex + 1]);
			s.append(" ");
			s.append(coords[nextIndex + 2]);
			if (addGeometryType)
				s.append(")");
			nextIndex += 3;
		}
		else if (geometryType == GEOMETRY_LINESTRING || geometryType == GEOMETRY_POLYGON)
		{
			/*
			 * Convert line or polygon to OGC Well Known Text representation.
			 */
			if (geometryType == GEOMETRY_POLYGON)
				s.append("(");
			for (int i = 0; i < nElements; i++)
			{
				if (coords[nextIndex] == MOVETO)
				{
					/*
					 * End last polygon ring and begin next ring.
					 */
					if (i > 0)
						s.append("), ");
					s.append("(");
				}
				else if (i > 0)
				{
					s.append(", ");
				}
				s.append(m_geometryValue[nextIndex + 1]);
				s.append(" ");
				s.append(m_geometryValue[nextIndex + 2]);
				nextIndex += 3;
			}

			if (geometryType == GEOMETRY_LINESTRING)
				s.append(")");
			else
				s.append("))");
		}
		else /* GEOMETRY_MULTIPOINT, GEOMETRY_MULTILINESTRING, GEOMETRY_MULTIPOLYGON, GEOMETRYCOLLECTION */
		{
			/*
			 * Expand each geometry in the multiple geometry.
			 */
			s.append("( ");
			boolean isGeometryCollection = (geometryType == GEOMETRY_COLLECTION);
			for (int i = 0; i < nElements; i++)
			{
				if (i > 0)
					s.append(", ");
				nextIndex = createOGCWKT(coords, nextIndex, s, isGeometryCollection);
			}
			s.append(")");
		}
		return(nextIndex);
	}

	/**
	 * Get geometry in GeoJSON format.
	 * @return geometry in GeoJSON format.
	 * @throws MapyrusException if argument is not geometry and cannot be converted to geometry.
	 */
	public String getGeoJSONValue() throws MapyrusException
	{
		double []coords = getGeometryValue();
		StringBuilder sb = new StringBuilder();
		createGeoJSON(coords, 0, sb, true);
		String retval = sb.toString().trim();
		return(retval);
	}

	/**
	 * Creates GeoJSON geometry string from geometry array.
	 * @param coords geometry type, count and move/draw coordinates.
	 * @param startIndex index in coords array to start converting.
	 * @param sb buffer to append geometry to.
	 * @param addGeometryType if true then geometry type added to
	 * geometry string.
	 * @return index of next element in coords array to be parsed.
	 */
	private int createGeoJSON(double []coords, int startIndex, StringBuilder sb, boolean addGeometryType)
	{
		int geometryType = (int)coords[startIndex];
		int nElements = (int)coords[startIndex + 1];
		int nextIndex = startIndex + 2;

		if (addGeometryType)
		{
			sb.append("{\"type\": ");
			if (geometryType == GEOMETRY_POINT)
				sb.append("\"Point\"");
			else if (geometryType == GEOMETRY_LINESTRING)
				sb.append("\"LineString\"");
			else if (geometryType == GEOMETRY_POLYGON)
				sb.append("\"Polygon\"");
			else if (geometryType == GEOMETRY_MULTIPOINT)
				sb.append("\"MultiPoint\"");
			else if (geometryType == GEOMETRY_MULTILINESTRING)
				sb.append("\"MultiLineString\"");
			else if (geometryType == GEOMETRY_MULTIPOLYGON)
				sb.append("\"MultiPolygon\"");
			else
				sb.append("\"GeometryCollection\", \"geometries\": ");

			if (geometryType != GEOMETRY_COLLECTION)
				sb.append(", \"coordinates\": ");
		}

		if (nElements == 0)
		{
			/*
			 * Geometry is empty.
			 */
			sb.append("null");
		}
		else if (geometryType == GEOMETRY_POINT)
		{
			sb.append("[");
			sb.append(coords[nextIndex + 1]);
			sb.append(", ");
			sb.append(coords[nextIndex + 2]);
			sb.append("]");
			nextIndex += 3;
		}
		else if (geometryType == GEOMETRY_LINESTRING || geometryType == GEOMETRY_POLYGON)
		{
			/*
			 * Convert line or polygon to GeoJSON.
			 */
			if (geometryType == GEOMETRY_POLYGON)
				sb.append("[");
			for (int i = 0; i < nElements; i++)
			{
				if (coords[nextIndex] == MOVETO)
				{
					/*
					 * End last polygon ring and begin next ring.
					 */
					if (i > 0)
						sb.append("], ");
					sb.append("[");
				}
				else if (i > 0)
				{
					sb.append(", ");
				}
				sb.append("[");
				sb.append(m_geometryValue[nextIndex + 1]);
				sb.append(", ");
				sb.append(m_geometryValue[nextIndex + 2]);
				sb.append("]");
				nextIndex += 3;
			}

			if (geometryType == GEOMETRY_LINESTRING)
				sb.append("]");
			else
				sb.append("]]");
		}
		else /* GEOMETRY_MULTIPOINT, GEOMETRY_MULTILINESTRING, GEOMETRY_MULTIPOLYGON, GEOMETRYCOLLECTION */
		{
			/*
			 * Expand each geometry in the multiple geometry.
			 */
			boolean isGeometryCollection = (geometryType == GEOMETRY_COLLECTION);
			sb.append("[");
			for (int i = 0; i < nElements; i++)
			{
				if (i > 0)
					sb.append(", ");
				nextIndex = createGeoJSON(coords, nextIndex, sb, isGeometryCollection);
			}
			sb.append("]");
		}

		if (addGeometryType)
		{
			sb.append("}");
		}
		return(nextIndex);
	}

	/**
	 * Add backslashes to escape any special characters in string.
	 * @param s string to escape.
	 * @return string with backslashes added.
	 */
	private String escapeQuotes(String s)
	{
		StringBuilder sb = new StringBuilder(s.length());
		int c;
		for (int j = 0; j < s.length(); j++)
		{
			c = s.charAt(j);
			if (c == '"')
				sb.append("\\\"");
			else if (c == '\\')
				sb.append("\\\\");
			else if (c == '\n')
				sb.append("\\n");
			else if (c == '\r')
				sb.append("\\r");
			else if (c == '\t')
				sb.append("\\t");
			else if (c > 127)
				sb.append("\\u").append(String.format("%04X", Integer.valueOf(c)));
			else
				sb.append((char)c);
		}
		return(sb.toString());
	}

	/**
	 * Return string representation of this argument.
	 * @return string representation of argument value.
	 */
	@Override
	public String toString()
	{
		String retval = null;
		DecimalFormat format;
		StringBuilder sb;

		if (m_type == STRING)
			retval = m_stringValue;
		else if (m_type == VARIABLE)
			retval = m_varname;
		else if (m_type == NUMERIC)
		{
			double absValue = (m_numericValue >= 0) ? m_numericValue : -m_numericValue;

			/*
			 * Print large or small numbers in scientific notation
			 * to give more significant digits.
			 */				
			if (absValue != 0 && (absValue < 0.01 || absValue > 10000000.0))
				format = new DecimalFormat("#.################E0", Constants.US_DECIMAL_FORMAT_SYMBOLS);
			else
				format = new DecimalFormat("#.################", Constants.US_DECIMAL_FORMAT_SYMBOLS);

			retval = format.format(m_numericValue);
		}
		else if (m_type == HASHMAP)
		{
			/*
			 * Build string of all key, value pairs in the hash map, in a
			 * format that could be re-interpreted by Mapyrus as an array.
			 */
			sb = new StringBuilder();
			Object []keys = getHashMapKeys();
			boolean isSequential = isSequentialKeys(keys);
			if (isSequential)
				sb.append("[");
			else
				sb.append("{");
			for (int i = 0; i < keys.length; i++)
			{
				if (i > 0)
					sb.append(", ");
				if (!isSequential)
				{
					sb.append("\"");
					String key = keys[i].toString();
					sb.append(escapeQuotes(key));
					sb.append("\": ");
				}

				Argument value = m_hashMap.get(keys[i]);
				String sValue = value.getStringValue();
				int valueType = value.getType();
				if (valueType == Argument.STRING)
					sValue = escapeQuotes(sValue);
				if (valueType != Argument.HASHMAP && valueType != Argument.NUMERIC)
					sb.append("\"");
				sb.append(sValue);
				if (valueType != Argument.HASHMAP && valueType != Argument.NUMERIC)
					sb.append("\"");
			}
			if (isSequential)
				sb.append("]");
			else
				sb.append("}");
			retval = sb.toString();
		}
		else
		{
			sb = new StringBuilder();
			createOGCWKT(m_geometryValue, 0, sb, true);
			retval = sb.toString().trim();
		}
		return(retval);
	}

	/**
	 * Check if list of keys are sequential array indexes starting at 1.
	 * @param keys array index keys.
	 * @return true if array indexes are sequential.
	 */
	private boolean isSequentialKeys(Object []keys)
	{
		boolean retval = true;
		for (int i = 0; i < keys.length && retval; i++)
		{
			if (!keys[i].toString().equals(Integer.toString(i + 1)))
				retval = false;
		}
		return(retval);
	}

	/**
	 * Clones an Argument object.
	 * @return clone of this argument.
	 */
	@Override
	public Object clone()
	{
		Argument retval;
		
		/*
		 * Create new argument that is a copy of existing one.
		 */
		if (m_type == STRING || m_type == VARIABLE)
			retval = new Argument(m_type, m_stringValue);
		else if (m_type == NUMERIC)
			retval = new Argument(m_numericValue);
		else if (m_type == HASHMAP)
		{
			retval = new Argument();
			
			/*
			 * Make a copy of the list of entries in the hash map --
			 * we do not want changes in the hashmap copy appearing
			 * in the original hashmap.
			 */
			retval.m_hashMap = new HashMap<String, Argument>(m_hashMap.size());
			Iterator<String> it = m_hashMap.keySet().iterator();
			while (it.hasNext())
			{
				String key = it.next();
				retval.m_hashMap.put(key, m_hashMap.get(key));
			}
		}
		else
		{
			retval = new Argument(m_type, m_geometryValue);
		}
		return(retval);
	}

	/**
	 * Compare this argument with another argument.
	 * @param arg other argument.
	 * @return -1, 0, 1 depending on comparison between arguments.
	 */
	@Override
	public int compareTo(Argument arg)
	{
		int retval;

		if (getType() == Argument.NUMERIC &&
			arg.getType() == Argument.NUMERIC)
		{
			/*
			 * Both arguments are numbers so compare them numerically.
			 */
			double a = 1, b = 0;
			try
			{
				a = getNumericValue();
				b = arg.getNumericValue();
			}
			catch (MapyrusException ignore)
			{
			}
			if (NumericalAnalysis.equals(a, b))
				retval = 0;
			else if (a > b)
				retval = 1;
			else
				retval = -1;
		}
		else
		{
			/*
			 * Compare other arguments as strings.
			 */
			String a = getStringValue();
			String b = arg.getStringValue();
			retval = a.compareTo(b);
		}
		return(retval);
	}
}
