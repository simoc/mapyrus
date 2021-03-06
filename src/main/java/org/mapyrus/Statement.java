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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A parsed statement.
 * Can be one of several types.  A conditional statement,
 * a block of statements making a procedure or just a plain command.
 */
public class Statement
{
	private StatementType m_type;

	/*
	 * Statements in an if-then-else statement.
	 */
	private ArrayList<Statement> m_thenStatements;
	private ArrayList<Statement> m_elseStatements;

	/*
	 * Statements in a while loop statement.
	 */
	private ArrayList<Statement> m_loopStatements;
		
	/*
	 * Name of procedure block,
	 * variable names of parameters to this procedure
	 * and block of statements in a procedure in order of execution
	 */
	private String m_blockName;
	private ArrayList<Statement> m_statementBlock;
	private ArrayList<String> m_parameters;
	
	private Expression []m_expressions;

	/*
	 * HashMap to walk through for a 'for' loop.
	 */	
	private Expression m_forHashMapExpression;

	/*
	 * Filename and line number within file that this
	 * statement was read from.
	 */
	private String m_filename;
	private int m_lineNumber;

	/*
	 * Static statement type lookup table for fast lookup.
	 */
	private static HashMap<String, StatementType> m_statementTypeLookup;
	
	static
	{
		m_statementTypeLookup = new HashMap<String, StatementType>(100);
		EnumSet<StatementType> statementSet = EnumSet.allOf(StatementType.class);
		Iterator<StatementType> it = statementSet.iterator();
		while (it.hasNext())
		{
			StatementType type = it.next();
			if (!type.isControl())
				m_statementTypeLookup.put(type.getName(), type);
		}
	}

	/**
	 * Looks up identifier for a statement name.
	 * @param s is the name of the statement.
	 * @returns statement type.
	 */
	private StatementType getStatementType(String s)
	{
		StatementType retval = m_statementTypeLookup.get(s.toLowerCase());

		if (retval == null)
			retval = StatementType.CALL;
		return(retval);
	}
	
	/**
	 * Creates a plain statement, either a built-in command or
	 * a call to a procedure block that the user has defined.
	 * @param keyword is the name statement.
	 * @param expressions are the arguments for this statement.
	 */
	public Statement(String keyword, Expression []expressions)
	{
		m_type = getStatementType(keyword);
		if (m_type == StatementType.CALL)
			m_blockName = keyword;
		m_expressions = expressions;
	}

	/**
	 * Creates a procedure, a block of statements to be executed together.
	 * @param blockName is name of procedure block.
	 * @param parameters variable names of parameters to this procedure.
	 * @param statements list of statements that make up this procedure block.
	 */
	public Statement(String blockName, ArrayList<String> parameters, ArrayList<Statement> statements)
	{
		m_blockName = blockName;
		m_parameters = parameters;
		m_statementBlock = statements;
		m_type = StatementType.BLOCK;
	}

	/**
	 * Create an if, then, else, endif block of statements.
	 * @param test is expression to test.
	 * @param thenStatements is statements to execute if expression is true.
	 * @param elseStatements is statements to execute if expression is false,
	 * or null if there is no statement to execute.
	 */
	public Statement(Expression test, ArrayList<Statement> thenStatements,
		ArrayList<Statement> elseStatements)
	{
		m_type = StatementType.CONDITIONAL;
		m_expressions = new Expression[1];
		m_expressions[0] = test;
		m_thenStatements = thenStatements;
		m_elseStatements = elseStatements;
	}
	
	/**
	 * Create a repeat or while loop block of statements.
	 * @param test is expression to test before each iteration of loop.
	 * @param loopStatements is statements to execute for each loop iteration.
	 * @param isWhileLoop true for a while loop, false for a repeat loop.
	 */
	public Statement(Expression test, ArrayList<Statement> loopStatements,
		boolean isWhileLoop)
	{
		m_type = isWhileLoop ? StatementType.WHILE_LOOP : StatementType.REPEAT_LOOP;
		m_expressions = new Expression[1];
		m_expressions[0] = test;
		m_loopStatements = loopStatements;
	}

	/**
	 * Create a for loop block of statements.
	 * @param var is variable to use in each iteration of loop.
	 * @param arrayVar is array to loop through.
	 * @param loopStatements is statements to execute for each loop iteration.
	 */
	public Statement(Expression var, Expression arrayVar, ArrayList<Statement> loopStatements)
	{
		m_type = StatementType.FOR_LOOP;
		m_expressions = new Expression[1];
		m_expressions[0] = var;
		m_forHashMapExpression = arrayVar;
		m_loopStatements = loopStatements;
	}

	/**
	 * Sets the filename and line number that this statement was read from.
	 * This is for use in any error message for this statement.
	 * @param filename is name of file this statement was read from.
	 * @param lineNumber is line number within file containing this statement.
	 */
	public void setFilenameAndLineNumber(String filename, int lineNumber)
	{
		m_filename = filename;
		m_lineNumber = lineNumber;
	}

	/**
	 * Returns filename and line number that this statement was read from.
	 * @return string containing filename and line number.
	 */
	public String getFilenameAndLineNumber()
	{
		return(m_filename + ":" + m_lineNumber);
	}

	/**
	 * Returns filename that this statement was read from.
	 * @return string containing filename.
	 */
	public String getFilename()
	{
		return(m_filename);
	}
	
	/**
	 * Returns the type of this statement.
	 * @return statement type.
	 */
	public StatementType getType()
	{
		return(m_type);
	}

	public Expression []getExpressions()
	{
		return(m_expressions);
	}

	/**
	 * Returns list of statements in "then" section of "if" statement.
	 * @return list of statements.
	 */		
	public ArrayList<Statement> getThenStatements()
	{
		return(m_thenStatements);
	}

	/**
	 * Returns list of statements in "else" section of "if" statement.
	 * @return list of statements.
	 */	
	public ArrayList<Statement> getElseStatements()
	{
		return(m_elseStatements);
	}

	/**
	 * Returns list of statements in while or for loop statement.
	 * @return list of statements.
	 */	
	public ArrayList<Statement> getLoopStatements()
	{
		return(m_loopStatements);
	}

	/**
	 * Returns hashmap expression to walk through in a for loop statement.
	 * @return expression evaluating to a hashmap.
	 */	
	public Expression getForHashMap()
	{
		return(m_forHashMapExpression);
	}

	/**
	 * Return name of procedure block.
	 * @return name of procedure.
	 */
	public String getBlockName()
	{
		return(m_blockName);
	}
	
	/**
	 * Return variable names of parameters to a procedure.
	 * @return list of parameter names.
	 */
	public ArrayList<String> getBlockParameters()
	{
		return(m_parameters);
	}
	
	/**
	 * Return statements in a procedure.
	 * @return ArrayList of statements that make up the procedure.
	 */
	public ArrayList<Statement> getStatementBlock()
	{
		return(m_statementBlock);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (m_type == StatementType.CALL)
			sb.append("CALL ").append(m_blockName);
		else if (m_type == StatementType.CONDITIONAL)
			sb.append("CONDITIONAL");
		else if (m_type == StatementType.REPEAT_LOOP)
			sb.append("REPEAT_LOOP");
		else if (m_type == StatementType.WHILE_LOOP)
			sb.append("WHILE_LOOP");
		else if (m_type == StatementType.FOR_LOOP)
			sb.append("FOR_LOOP");
		else if (m_type == StatementType.BLOCK)
			sb.append("BLOCK ").append(m_blockName);
		else
		{
			for (Map.Entry<String, StatementType> entry : m_statementTypeLookup.entrySet())
			{
				if (entry.getValue() == m_type)
					sb.append(entry.getKey());
			}
		}
		if (m_expressions != null)
		{
			for (int i = 0; i < m_expressions.length; i++)
			{
				sb.append(" ").append(m_expressions[i]);
			}
		}
		return sb.toString();
	}
}
