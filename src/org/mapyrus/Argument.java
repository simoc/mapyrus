/*
 * $Id$
 */

package au.id.chenery.mapyrus;

import java.lang.String;

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
	}

	/**
	 * Create a new numeric argument.
	 * @param d is value for this argument.
	 */
	public Argument(double []coords)
	{
		mType = GEOMETRY;
		mGeometryValue = coords;
	}

	/**
	 * Returns type of argument.
	 * @return either NUMERIC, STRING, VARIABLE, or GEOMETRY.
	 */	
	public int getType()
	{
		return(mType);
	}
	
	/**
	 * Returns value of numeric argument.
	 * @return numeric argument value.
	 */
	public double getNumericValue()
	{
		return(mNumericValue);
	}
	
	/**
	 * Returns value of string argument.
	 * @return string argument value.
	 */
	public String getStringValue()
	{
		return(mStringValue);
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
}
