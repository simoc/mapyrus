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
	private String m_funcName;
	private ArrayList<String> m_formalParameters;
	private ArrayList<Statement> m_statements;
	private Interpreter m_interpreter;

	/**
	 * Create user-defined function.
	 * @param funcName name of function.
	 * @param formalParameters formal paramters for function.
	 * @param statements statements to execute for function.
	 * @param interpreter interpreter in which statements will execute.
	 * @return function function that can be executed just like internal functions.
	 */
	public UserFunction(String funcName, ArrayList<String> formalParameters,
		ArrayList<Statement> statements, Interpreter interpreter)
	{
		m_funcName = funcName;
		m_formalParameters = formalParameters;
		m_statements = statements;
		m_interpreter = interpreter;
	}

	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException, InterruptedException
	{
		Argument retval = null;
		boolean savedState = false;

		try
		{
			context.saveState(m_funcName);
			savedState = true;
			for (int i = 0; i < args.size(); i++)
			{
				String parameterName = m_formalParameters.get(i);
				context.setLocalScope(parameterName);
				Argument arg = args.get(i);
				context.defineVariable(parameterName, arg);
			}
		
			/*
			 * Execute each of the statements.
			 */	
			for (int i = 0; i < m_statements.size(); i++)
			{
				Statement statement = m_statements.get(i);
				retval = m_interpreter.executeStatement(statement);
		
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

	@Override
	public int getMaxArgumentCount()
	{
		return(m_formalParameters.size());
	}

	@Override
	public int getMinArgumentCount()
	{
		return(m_formalParameters.size());
	}

	@Override
	public String getName()
	{
		return(m_funcName);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder(m_funcName);
		sb.append(" ");
		for (int i = 0; i < m_formalParameters.size(); i++)
		{
			if (i > 0)
				sb.append(", ");
			sb.append(m_formalParameters.get(i));
		}
		return(sb.toString());
	}

	/**
	 * Set statements to execute for this function.
	 * @param statements statements to execute.
	 */
	public void setStatements(ArrayList<Statement> statements)
	{
		m_statements = statements;
	}

	/**
	 * Make a copy of this function that can be used in another interpreter.
	 * @param interpreter interpreter that will use this function.
	 * @return copy of this function.
	 */
	public UserFunction clone(Interpreter interpreter)
	{
		UserFunction retval = new UserFunction(m_funcName, m_formalParameters,
			m_statements, interpreter);
		return(retval);
	}
}
