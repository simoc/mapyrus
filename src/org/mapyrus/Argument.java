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
	
	private int mType;
	private double mNumericValue;
	private String mStringValue;
	private String mVarname;
	
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
	 * Returns type of argument.
	 * @return either NUMERIC, STRING, or VARIABLE.
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
	 * Set argument to a numeric value.
	 * @param d value to set.
	 */	
	public void setNumericValue(double d)
	{
		mType = NUMERIC;
		mNumericValue = d;
	}
	
	/**
	 * Set argument to a string.
	 * @param s value to set.
	 */	
	public void setStringValue(String s)
	{
		mType = STRING;
		mStringValue = s;
	}
}
