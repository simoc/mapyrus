/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

package au.id.chenery.mapyrus;

import java.awt.geom.PathIterator;
import java.text.DecimalFormat;

/**
 * An argument is a literal value or variable name.
 * Each field read from a dataset is an argument.
 * Several arguments can be combined with operators to make an expression
 * For example, '2 * a + 7' contains three arguments.
 */
public class Argument
{
	public static final int NUMERIC = 0;
	public static final int STRING = 1;
	public static final int VARIABLE = 2;
	public static final int GEOMETRY = 3;

	public static final int GEOMETRY_POINT = 1000;
	public static final int GEOMETRY_LINESTRING = 1001;
	public static final int GEOMETRY_POLYGON = 1002;
	public static final int GEOMETRY_MULTIPOINT = 1003;
	public static final int GEOMETRY_MULTILINESTRING = 1004;
	public static final int GEOMETRY_MULTIPOLYGON = 1005;

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
	 * Constant for empty string.
	 * Avoids allocating many arguments for this commonly used value.
	 */
	public static final Argument emptyString = new Argument(Argument.STRING, "");

	private int mType;
	private double mNumericValue;
	private String mStringValue;
	private String mVarname;
	private double []mGeometryValue;
	
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
	 * Create a new string argument or variable name.
	 * @param type is STRING to create a string argument, or VARIABLE to create
	 * a reference to a variable.
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
	 * Returns type of argument.
	 * @return either NUMERIC, STRING, VARIABLE, or GEOMETRY.
	 */	
	public int getType()
	{
		if (mType == NUMERIC || mType == STRING || mType == VARIABLE)
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
		else if (mType != NUMERIC)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_NUMERIC_VALUE));
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
				format = new DecimalFormat("#.################E0");
			else
				format = new DecimalFormat("#.################");

			retval = format.format(mNumericValue);
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
				s.append("POLYGON ((");
			else
				s.append("MULTIPOINT (");

			for (int i = 1; i < mGeometryValue[0]; i += 3)
			{
				if (i > 1)
					s.append(", ");
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
		else
		{
			/*
			 * Convert multiple lines or polygons to
			 * OGC Well Known Text representation.
			 */
			if (mType == GEOMETRY_MULTILINESTRING)
				s = new StringBuffer("MULTILINESTRING (");
			else
				s = new StringBuffer("MULTIPOLYGON ((");

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
				s.append(")");
			else
				s.append("))");
			
			retval = s.toString();
		}

		return(retval);
	}
}
