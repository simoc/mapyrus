/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

package org.mapyrus;

import java.awt.geom.PathIterator;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * An argument is a literal value, array of literal values, or a variable name.
 * Each field read from a dataset is an argument.
 * Several arguments can be combined with operators to make an expression
 * For example, '2 * a + 7' and 'prefix["DE"] . "11823"' both contain three arguments.
 */
public class Argument
{
	public static final int NUMERIC = 0;
	public static final int STRING = 1;
	public static final int HASHMAP = 2;
	public static final int VARIABLE = 3;
	public static final int GEOMETRY = 4;

	public static final int GEOMETRY_POINT = 1000;
	public static final int GEOMETRY_LINESTRING = 1001;
	public static final int GEOMETRY_POLYGON = 1002;
	public static final int GEOMETRY_MULTIPOINT = 1003;
	public static final int GEOMETRY_MULTILINESTRING = 1004;
	public static final int GEOMETRY_MULTIPOLYGON = 1005;
	public static final int GEOMETRY_COLLECTION = 1006;

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

	private int mType;
	private double mNumericValue;
	private String mStringValue;
	private String mVarname;
	private double []mGeometryValue;
	private HashMap mHashMap;

	/**
	 * Create a new numeric argument.
	 * @param d is value for this argument.
	 */
	public Argument(double d)
	{
		mType = NUMERIC;
		mNumericValue = d;
	}

	/**
	 * Create a new string argument, or variable name.
	 * @param type is STRING to create a string argument, or a VARIABLE to create
	 * a reference to a variable.
	 * @param s is string, or variable name.
	 */
	public Argument(int type, String s)
	{
		mType = type;

		if (type == STRING)
			mStringValue = s;
		else
			mVarname = s;

		/*
		 * We don't know the numeric value of this argument.
		 */
		mNumericValue = Double.NaN;
	}

	/**
	 * Create new, empty hashmap argument.
	 * Use addHashMapEntry() method to add values to the hash map.
	 */
	public Argument()
	{
		mType = HASHMAP;
		mHashMap = new HashMap();	
	}

	/**
	 * Create a new geometry argument for all types of geometry.
	 * @param geometryType is OGC geometry type of point(s), line(s),
	 * or polygon(s).
	 * @param coords is array containing (X, Y) coordinates of point(s),
	 * line(s), polygon(s).
	 *
	 * Array is of the form:
	 * <pre>
	 * +--------+-------+---+---+-------+---+---+
	 * | length | move/ | x | y | move/ | x | y | ...
	 * |        | line  |   |   | line  |   |   |
	 * +--------+-------+---+---+-------+---+---+
	 * </pre>
	 *
	 * where length is the number of slots used in the array,
	 * a moveto point is flagged as PathIterator.SEG_MOVETO,
	 * a lineto point is PathIterator.SEG_LINETO.
	 */
	public Argument(int geometryType, double []coords)
	{
		mType = geometryType;
		mGeometryValue = coords;
	}

	/**
	 * Parse parenthesised coordinate list from OGC geometry string into array.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry coordinates as tokens.
	 * @param geometryIndex index into geometry array to add coordinates.
	 * @param isPoints flag true when all coordinates to be added as MOVETO points
	 * @return index of next free position in geometry array after adding coordinates.
	 */
	private int parseCoordinateList(String wktGeometry, StringTokenizer st,
		int geometryIndex, boolean isPoints)
		throws MapyrusException
	{
		String token;
		boolean foundOpenParen, foundCloseParen, foundEmpty;
		boolean foundX, foundY;
		int index = geometryIndex;

		/*
		 * First expect a '(' or the keyword 'EMPTY'.
		 */
		foundOpenParen = foundCloseParen = foundEmpty = false;
		while (foundOpenParen == false && foundEmpty == false
			&& st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.equals("("))
				foundOpenParen = true;
			else if (token.equals("EMPTY"))
				foundEmpty = true;
		}

		/*
		 * Then expect a series of X and Y coordinate value separated by commas,
		 * and then a ')' to finish the coordinate string.
		 */
		if (foundOpenParen)
		{
			foundX = foundY = false;
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
							/*
							 * Move to first coordinate pair, then add a
							 * line to each subsequent pair.
							 */
							if (isPoints || index == geometryIndex)
								mGeometryValue[index] = PathIterator.SEG_MOVETO;
							else
								mGeometryValue[index] = PathIterator.SEG_LINETO;
							index++;

							mGeometryValue[index] = Double.parseDouble(token);
							index++;
							foundX = true;
						}
						else if (foundY == false)
						{
							mGeometryValue[index] = Double.parseDouble(token);
							index++;
							foundY = true;
						}
						else
						{
							/*
							 * Not expecting an X or Y coordinate.  The OGC string must be invalid.
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
				else if (c == ')' && foundY)
				{
					/*
					 * Found closing parenthesis marking end of coordinates.
					 */
					foundCloseParen = true;
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
		if (foundOpenParen && foundCloseParen == false)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
				": " + wktGeometry);
		}
		return(index);
	}

	/**
	 * Parse polygon from OGC geometry string into array.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry coordinates as tokens.
	 * @param geometryIndex index into geometry array to add coordinates.
	 * @return index of next free position in geometry array after adding coordinates.
	 */
	private int parsePolygon(String wktGeometry, StringTokenizer st,
		int geometryIndex)
		throws MapyrusException
	{
		String token;

		/*
		 * Parse opening '('.  Then one or more lists of coordinates surrounded
		 * by parentheses and separated by commas.  Then parse closing ')'.
		 */
		boolean foundCloseParen = false;
		boolean foundCoordList = false;

		foundCoordList = false;
		while (foundCoordList == false && st.hasMoreTokens())
		{
			token = st.nextToken();
			foundCoordList = token.equals("(");
		}

		while (foundCoordList && foundCloseParen == false && st.hasMoreTokens())
		{
			/*
			 * Parse coordinate list like '(10 20, 30 50)'.
			 */
			geometryIndex = parseCoordinateList(wktGeometry, st, geometryIndex, false);

			/*
			 * Is there another coordinate list following this one?
			 */
			foundCoordList = false;
			while (foundCoordList == false && foundCloseParen == false && st.hasMoreTokens())
			{
				token = st.nextToken();
				if (token.equals(","))
					foundCoordList = true;
				else if (token.equals(")"))
					foundCloseParen = true;
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
		return(geometryIndex);
	}

	/**
	 * Parse OGC geometry string.
	 * @param wktGeometry original geometry string.
	 * @param st OGC geometry coordinates as tokens.
	 * @param geometryIndex index into geometry array to add coordinates.
	 * @return index of next free position in geometry array after adding coordinates.
	 */
	private int parseGeometry(String wktGeometry, StringTokenizer st, int index)
		throws MapyrusException
	{
		String token;
		String ogcType;
		boolean foundOpenParen, foundCloseParen, foundComma;

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
				mType = GEOMETRY_POINT;
				index = parseCoordinateList(wktGeometry, st, index, true);
			}
			else if (ogcType.equals("LINESTRING"))
			{
				mType = GEOMETRY_LINESTRING;	
				index = parseCoordinateList(wktGeometry, st, index, false);
			}
			else if (ogcType.equals("MULTIPOINT"))
			{
				mType = GEOMETRY_MULTIPOINT;
				index = parseCoordinateList(wktGeometry, st, index, true);
			}
			else
			{
				if (ogcType.equals("POLYGON"))
					mType = GEOMETRY_POLYGON;
				else if (ogcType.equals("MULTILINESTRING"))
					mType = GEOMETRY_MULTILINESTRING;
				else if (ogcType.equals("MULTIPOLYGON"))
					mType = GEOMETRY_MULTIPOLYGON;
				else if (ogcType.equals("GEOMETRYCOLLECTION"))
					mType = GEOMETRY_COLLECTION;
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
						": " + wktGeometry);
				}

				if (mType == GEOMETRY_POLYGON || mType == GEOMETRY_MULTILINESTRING)
				{
					/*
					 * Parse one or more coordinate lists surrounded by parentheses.
					 */
					index = parsePolygon(wktGeometry, st, index);
				}
				else
				{
					/*
					 * Parse any number of polygons or geometries surrounded
					 * by parentheses.
					 */
					foundOpenParen = foundCloseParen = false;
					while (foundOpenParen == false && st.hasMoreTokens())
					{
						token = st.nextToken();
						foundOpenParen = token.equals("(");
					}
					while (foundOpenParen && foundCloseParen == false && st.hasMoreTokens())
					{
						if (mType == GEOMETRY_MULTIPOLYGON)
							index = parsePolygon(wktGeometry, st, index);
						else
							index = parseGeometry(wktGeometry, st, index);

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
					if (foundOpenParen && foundCloseParen == false)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OGC_WKT) +
							": " + wktGeometry);
					}
					
					/*
					 * Nested geometries changed geometry type.  Set it back to a
					 * collection here.
					 */
					mType = GEOMETRY_COLLECTION;
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
	 */
	public Argument(String wktGeometry) throws MapyrusException
	{
		StringTokenizer st = new StringTokenizer(wktGeometry, ",() ", true);
		int index = 1;
		mGeometryValue = new double[st.countTokens()];
		index = parseGeometry(wktGeometry, st, index);

		/*
		 * Set number of elements in geometry array, or 0 if geometry is empty.
		 */
		if (index > 1)
			mGeometryValue[0] = index;
		else
			mGeometryValue[0] = 0;
	}

	/**
	 * Returns type of argument.
	 * @return either NUMERIC, STRING, VARIABLE, or GEOMETRY.
	 */	
	public int getType()
	{
		if (mType == NUMERIC || mType == STRING || mType == VARIABLE || mType == HASHMAP)
			return(mType);
		else
			return(GEOMETRY);
	}

	/**
	 * Returns numeric value of argument.
	 * @return numeric argument value,
	 * or zero if it cannot be converted to a number.
	 */
	public double getNumericValue() throws MapyrusException
	{
		if (mType == STRING)
		{
			if (Double.isNaN(mNumericValue))
			{
				/*
				 * Argument is a string that we've not tried
				 * converting to a number before.  Find it's numeric
				 * value now.
				 */
				try
				{
					mNumericValue = Double.parseDouble(mStringValue);
				}
				catch (NumberFormatException e)
				{
					mNumericValue = 0;
				}
			}
		}
		else if (mType == HASHMAP)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.HASHMAP_NOT_NUMERIC));
		}
		else if (mType != NUMERIC)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.GEOMETRY_NOT_NUMERIC));
		}
		return(mNumericValue);
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
		return(mVarname);
	}

	/**
	 * Returns value of geometry argument.
	 * @return geometry argument value.
	 */	
	public double []getGeometryValue()
	{
		return(mGeometryValue);
	}
	
	/**
	 * Returns value of one entry in a hashmap.
	 * @param key is key to lookup.
	 * @return value associated with this key, or empty string argument
	 * if this key is not in hashmap.
	 */	
	public Argument getHashMapEntry(String key)
	{
		Argument retval = (Argument)mHashMap.get(key);
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
		mHashMap.put(key, value);
	}

	/**
	 * Get array of keys in hash map.
	 * @return keys in this hash map, each object being a string.
	 */
	public Object[] getHashMapKeys()
	{
		return(mHashMap.keySet().toArray());
	}

	/**
	 * Get number of elements in hash map.
	 * @return size of hashmap.
	 */
	public int getHashMapSize()
	{
		return(mHashMap.size());
	}
	
	/**
	 * Return string representation of this argument.
	 * @return string representation of argument value.
	 */
	public String toString()
	{
		String retval = null;
		DecimalFormat format;
		StringBuffer s;

		if (mType == STRING)
			retval = mStringValue;
		else if (mType == VARIABLE)
			retval = mVarname;
		else if (mType == NUMERIC)
		{
			double absValue = (mNumericValue >= 0) ? mNumericValue : -mNumericValue;

			/*
			 * Print large or small numbers in scientific notation
			 * to give more significant digits.
			 */				
			if (absValue != 0 && (absValue < 0.01 || absValue > 10000000.0))
				format = new DecimalFormat("#.################E0", Constants.US_DECIMAL_FORMAT_SYMBOLS);
			else
				format = new DecimalFormat("#.################", Constants.US_DECIMAL_FORMAT_SYMBOLS);

			retval = format.format(mNumericValue);
		}
		else if (mType == HASHMAP)
		{
			/*
			 * Build string of all key, value pairs in the hash map.
			 */
			s = new StringBuffer();
			Iterator it = mHashMap.keySet().iterator();
			while (it.hasNext())
			{
				if (s.length() > 0)
					s.append(' ');

				String key = (String)it.next();
				s.append(key);
				s.append(' ');
				Argument value = (Argument)mHashMap.get(key);
				s.append(value.getStringValue());
			}
			retval = s.toString();
		}
		else if (mGeometryValue[0] == 0)
		{
			/*
			 * Geometry is empty.
			 */
			if (mType == GEOMETRY_POINT)
				retval = "POINT EMPTY";
			else if (mType == GEOMETRY_LINESTRING)
				retval = "LINESTRING EMPTY";
			else if (mType == GEOMETRY_POLYGON)
				retval = "POLYGON EMPTY";
			else if (mType == GEOMETRY_MULTIPOINT)
				retval = "MULTIPOINT EMPTY";
			else if (mType == GEOMETRY_MULTILINESTRING)
				retval = "MULTILINESTRING EMPTY";
			else if (mType == GEOMETRY_MULTIPOLYGON)
				retval = "MULTIPOLYGON EMPTY";
			else
				retval = "GEOMETRYCOLLECTION EMPTY";
		}
		else if (mType == GEOMETRY_POINT)
		{
			/*
			 * Convert point to OGC Well Known Text representation.
			 */
			retval = "POINT (" + mGeometryValue[2] + " " + mGeometryValue[3] + ")";
		}
		else if (mType == GEOMETRY_LINESTRING || mType == GEOMETRY_POLYGON ||
			mType == GEOMETRY_MULTIPOINT)
		{
			/*
			 * Convert line, polygon or series of points to
			 * OGC Well Known Text representation.
			 */
			s = new StringBuffer();
			if (mType == GEOMETRY_LINESTRING)
				s.append("LINESTRING (");
			else if (mType == GEOMETRY_POLYGON)
				s.append("POLYGON (");
			else
				s.append("MULTIPOINT (");

			for (int i = 1; i < mGeometryValue[0]; i += 3)
			{
				if (mType == GEOMETRY_POLYGON && mGeometryValue[i] == PathIterator.SEG_MOVETO)
				{
					/*
					 * End last polygon ring and begin next ring.
					 */
					if (i > 1)
						s.append("), ");
					s.append("(");
				}
				else if (i > 1)
				{
					s.append(", ");
				}
				s.append(mGeometryValue[i + 1]);
				s.append(" ");
				s.append(mGeometryValue[i + 2]);
			}

			if (mType == GEOMETRY_LINESTRING || mType == GEOMETRY_MULTIPOINT)
				s.append(")");
			else
				s.append("))");

			retval = s.toString();
		}
		else if (mType == GEOMETRY_MULTILINESTRING || mType == GEOMETRY_MULTIPOLYGON)
		{
			/*
			 * Convert multiple lines or polygons to
			 * OGC Well Known Text representation.
			 */
			if (mType == GEOMETRY_MULTILINESTRING)
				s = new StringBuffer("MULTILINESTRING ((");
			else
				s = new StringBuffer("MULTIPOLYGON (((");

			for (int i = 1; i < mGeometryValue[0]; i += 3)
			{
				if (mGeometryValue[i] == PathIterator.SEG_MOVETO)
				{
					if (i > 1)
					{
						if (mType == GEOMETRY_MULTILINESTRING)
							s.append("), (");
						else
							s.append(")), ((");
					}
				}
				else
				{
					s.append(", ");
				}

				s.append(mGeometryValue[i + 1]);
				s.append(" ");
				s.append(mGeometryValue[i + 2]);
			}		

			if (mType == GEOMETRY_MULTILINESTRING)
				s.append("))");
			else
				s.append(")))");
			
			retval = s.toString();
		}
		else
		{
			s = new StringBuffer("GEOMETRYCOLLECTION (");
			int i = 1;
			while (i < mGeometryValue[0])
			{
				if (i > 1)
					s.append(",");

				if (mGeometryValue[i] == PathIterator.SEG_MOVETO &&
					i + 3 < mGeometryValue[0] &&
					mGeometryValue[i + 3] != PathIterator.SEG_MOVETO)
				{
					s.append(" LINESTRING (");
					String separator = "";
					do
					{
						s.append(separator);
						s.append(mGeometryValue[i + 1]);
						s.append(" ");
						s.append(mGeometryValue[i + 2]);
						i += 3;
						separator = ",";
					}
					while (i < mGeometryValue[0] && mGeometryValue[i] != PathIterator.SEG_MOVETO);
					s.append(")");
				}
				else
				{
					s.append(" POINT (");
					s.append(mGeometryValue[i + 1]);
					s.append(" ");
					s.append(mGeometryValue[i + 2]);
					s.append(")");
					i += 3;
				}
			}
			s.append(")");
			retval = s.toString();
		}
		return(retval);
	}

	/**
	 * Clones an Argument object.
	 * @return clone of this argument.
	 */	
	public Object clone()
	{
		Argument retval;
		
		/*
		 * Create new argument that is a copy of existing one.
		 */
		if (mType == STRING || mType == VARIABLE)
			retval = new Argument(mType, mStringValue);
		else if (mType == NUMERIC)
			retval = new Argument(mNumericValue);
		else if (mType == HASHMAP)
		{
			retval = new Argument();
			
			/*
			 * Make a copy of the list of entries in the hash map --
			 * we do not want changes in the hashmap copy appearing
			 * in the original hashmap.
			 */
			retval.mHashMap = (HashMap)mHashMap.clone();
		}
		else
		{
			retval = new Argument(mType, mGeometryValue);
		}
		return((Object)retval);
	}
}
