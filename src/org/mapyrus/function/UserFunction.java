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
package org.mapyrus.function;

import java.io.IOException;
import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.Interpreter;
import org.mapyrus.MapyrusException;
import org.mapyrus.Statement;

/**
 * A function defined by the user to do whatever they want.
 */
public class UserFunction implements Function
{
	private String mFuncName;
	private ArrayList mFormalParameters;
	private ArrayList mStatements;
	private Interpreter mInterpreter;

	/**
	 * Create user-defined function.
	 * @param funcName name of function.
	 * @param formalParameters formal paramters for function.
	 * @param statements statements to execute for function.
	 * @param interpreter interpreter in which statements will execute.
	 * @return function function that can be executed just like internal functions.
	 */
	public UserFunction(String funcName, ArrayList formalParameters,
		ArrayList statements, Interpreter interpreter)
	{
		mFuncName = funcName;
		mFormalParameters = formalParameters;
		mStatements = statements;
		mInterpreter = interpreter;
	}

	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException
	{
		Argument retval = null;
		boolean savedState = false;

		try
		{
			context.saveState(mFuncName);
			savedState = true;
			for (int i = 0; i < args.size(); i++)
			{
				String parameterName = (String)mFormalParameters.get(i);
				context.setLocalScope(parameterName);
				Argument arg = (Argument)args.get(i);
				context.defineVariable(parameterName, arg);
			}
		
			/*
			 * Execute each of the statements.
			 */	
			for (int i = 0; i < mStatements.size(); i++)
			{
				Statement statement = (Statement)mStatements.get(i);
				retval = mInterpreter.executeStatement(statement);
		
				/*
				 * Found return statement so stop executing.
				 */
				if (retval != null)
					break;
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		finally
		{
			try
			{
				if (savedState)
					context.restoreState();
			}
			catch (IOException e)
			{
				throw new MapyrusException(e.getMessage());
			}
		}

		if (retval == null)
			retval = Argument.emptyString;
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(mFormalParameters.size());
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(mFormalParameters.size());
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return(mFuncName);
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer(mFuncName);
		sb.append(" ");
		for (int i = 0; i < mFormalParameters.size(); i++)
		{
			if (i > 0)
				sb.append(", ");
			sb.append(mFormalParameters.get(i));
		}
		return(sb.toString());
	}

	/**
	 * Set statements to execute for this function.
	 * @param statements statements to execute.
	 */
	public void setStatements(ArrayList statements)
	{
		mStatements = statements;
	}

	/**
	 * Make a copy of this function that can be used in another interpreter.
	 * @param interpreter interpreter that will use this function.
	 * @return copy of this function.
	 */
	public UserFunction clone(Interpreter interpreter)
	{
		UserFunction retval = new UserFunction(mFuncName, mFormalParameters,
			mStatements, interpreter);
		return(retval);
	}
}
