/**
 * An argument is a literal value or variable name.
 * Several arguments are combined with operators to make an expression.
 */

/*
 * $Id$
 */

package net.sourceforge.mapyrus;

import java.lang.String;

public class Argument
{
	public static final int NUMERIC = 0;
	public static final int STRING = 1;
	public static final int VARIABLE = 2;
	
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
}
