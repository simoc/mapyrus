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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A parsed statement.
 * Can be one of several types.  A conditional statement, 
 * a block of statements making a procedure or just a plain command.
 */
public class Statement
{
	/*
	 * Possible types of statements.
	 */
	public static final int CONDITIONAL = 2;
	public static final int WHILE_LOOP = 3;
	public static final int FOR_LOOP = 4;
	public static final int BLOCK = 5;

	public static final int COLOR = 10;
	public static final int LINESTYLE = 11;
	public static final int FONT = 12;
	public static final int JUSTIFY = 13;
	public static final int MOVE = 14;
	public static final int DRAW = 15;
	public static final int ARC = 16;
	public static final int ADDPATH = 17;
	public static final int CLEARPATH = 18;
	public static final int SAMPLEPATH = 19;
	public static final int STRIPEPATH = 20;
	public static final int SHIFTPATH = 21;
	public static final int STROKE = 22;
	public static final int FILL = 23;
	public static final int PROTECT = 24;
	public static final int CLIP = 25;
	public static final int LABEL = 26;
	public static final int SCALE = 27;
	public static final int ROTATE = 28;
	public static final int WORLDS = 29;
	public static final int PROJECT = 30;
	public static final int DATASET = 31;
	public static final int IMPORT = 32;
	public static final int FETCH = 33;
	public static final int NEWPAGE = 34;
	public static final int PRINT = 35;
	public static final int LOCAL = 36;
	public static final int LET = 37;
	public static final int KEY = 38;
	public static final int LEGEND = 39;

	/*
	 * Statement type for call to user defined procedure block.
	 */
	public static final int CALL = 1000;

	private int mType;

	/*
	 * Statements in an if-then-else statement.
	 */
	private ArrayList mThenStatements;
	private ArrayList mElseStatements;

	/*
	 * Statements in a while loop statement.
	 */
	private ArrayList mLoopStatements;
		
	/*
	 * Name of procedure block,
	 * variable names of parameters to this procedure
	 * and block of statements in a procedure in order of execution
	 */
	private String mBlockName;
	private ArrayList mStatementBlock;
	private ArrayList mParameters;
	
	private Expression []mExpressions;

	/*
	 * HashMap to walk through for a 'for' loop.
	 */	
	private Expression mForHashMapExpression;

	/*
	 * Filename and line number within file that this 
	 * statement was read from.
	 */
	private String mFilename;
	private int mLineNumber;

	/*
	 * Static statement type lookup table for fast lookup.
	 */
	private static HashMap mStatementTypeLookup;
	
	static
	{
		mStatementTypeLookup = new HashMap();
		mStatementTypeLookup.put("color", new Integer(COLOR));
		mStatementTypeLookup.put("colour", new Integer(COLOR));
		mStatementTypeLookup.put("linestyle", new Integer(LINESTYLE));
		mStatementTypeLookup.put("font", new Integer(FONT));
		mStatementTypeLookup.put("justify", new Integer(JUSTIFY));
		mStatementTypeLookup.put("move", new Integer(MOVE));
		mStatementTypeLookup.put("draw", new Integer(DRAW));
		mStatementTypeLookup.put("arc", new Integer(ARC));
		mStatementTypeLookup.put("addpath", new Integer(ADDPATH));
		mStatementTypeLookup.put("clearpath", new Integer(CLEARPATH));
		mStatementTypeLookup.put("samplepath", new Integer(SAMPLEPATH));
		mStatementTypeLookup.put("stripepath", new Integer(STRIPEPATH));
		mStatementTypeLookup.put("shiftpath", new Integer(SHIFTPATH));
		mStatementTypeLookup.put("stroke", new Integer(STROKE));
		mStatementTypeLookup.put("fill", new Integer(FILL));
		mStatementTypeLookup.put("protect", new Integer(PROTECT));
		mStatementTypeLookup.put("clip", new Integer(CLIP));
		mStatementTypeLookup.put("label", new Integer(LABEL));
		mStatementTypeLookup.put("scale", new Integer(SCALE));
		mStatementTypeLookup.put("rotate", new Integer(ROTATE));
		mStatementTypeLookup.put("worlds", new Integer(WORLDS));
		mStatementTypeLookup.put("project", new Integer(PROJECT));
		mStatementTypeLookup.put("dataset", new Integer(DATASET));
		mStatementTypeLookup.put("import", new Integer(IMPORT));
		mStatementTypeLookup.put("fetch", new Integer(FETCH));
		mStatementTypeLookup.put("newpage", new Integer(NEWPAGE));
		mStatementTypeLookup.put("print", new Integer(PRINT));
		mStatementTypeLookup.put("local", new Integer(LOCAL));
		mStatementTypeLookup.put("let", new Integer(LET));
		mStatementTypeLookup.put("key", new Integer(KEY));
		mStatementTypeLookup.put("legend", new Integer(LEGEND));
	}
	
	/**
	 * Looks up identifier for a statement name.
	 * @param s is the name of the statement.
	 * @returns numeric code for this statement, or -1 if statement
	 * is unknown.
	 */
	private int getStatementType(String s)
	{
		int retval;
		Integer type = (Integer)mStatementTypeLookup.get(s.toLowerCase());
		
		if (type == null)
			retval = CALL;
		else
			retval = type.intValue();
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
		mType = getStatementType(keyword);
		if (mType == CALL)
			mBlockName = keyword;
		mExpressions = expressions;
	}

	/**
	 * Creates a procedure, a block of statements to be executed together.
	 * @param blockName is name of procedure block.
	 * @param parameters variable names of parameters to this procedure.
	 * @param statements list of statements that make up this procedure block.
	 */
	public Statement(String blockName, ArrayList parameters, ArrayList statements)
	{
		mBlockName = blockName;
		mParameters = parameters;
		mStatementBlock = statements;
		mType = BLOCK;
	}

	/**
	 * Create an if, then, else, endif block of statements.
	 * @param test is expression to test.
	 * @param thenStatements is statements to execute if expression is true.
	 * @param elseStatements is statements to execute if expression is false,
	 * or null if there is no statement to execute.
	 */
	public Statement(Expression test, ArrayList thenStatements,
		ArrayList elseStatements)
	{
		mType = CONDITIONAL;
		mExpressions = new Expression[1];
		mExpressions[0] = test;
		mThenStatements = thenStatements;
		mElseStatements = elseStatements;
	}
	
	/**
	 * Create a while loop block of statements.
	 * @param test is expression to test before each iteration of loop.
	 * @param loopStatements is statements to execute for each loop iteration.
	 */
	public Statement(Expression test, ArrayList loopStatements)
	{
		mType = WHILE_LOOP;
		mExpressions = new Expression[1];
		mExpressions[0] = test;
		mLoopStatements = loopStatements;
	}

	/**
	 * Create a while loop block of statements.
	 * @param test is expression to test before each iteration of loop.
	 * @param loopStatements is statements to execute for each loop iteration.
	 */
	public Statement(Expression var, Expression arrayVar, ArrayList loopStatements)
	{
		mType = FOR_LOOP;
		mExpressions = new Expression[1];
		mExpressions[0] = var;
		mForHashMapExpression = arrayVar;
		mLoopStatements = loopStatements;
	}

	/**
	 * Sets the filename and line number that this statement was read from.
	 * This is for use in any error message for this statement.
	 * @param filename is name of file this statement was read from.
	 * @param lineNumber is line number within file containing this statement.
	 */
	public void setFilenameAndLineNumber(String filename, int lineNumber)
	{
		mFilename = filename;
		mLineNumber = lineNumber;
	}

	/**
	 * Returns filename and line number that this statement was read from.
	 * @return string containing filename and line number.
	 */
	public String getFilenameAndLineNumber()
	{
		return(mFilename + ":" + mLineNumber);
	}

	/**
	 * Returns filename that this statement was read from.
	 * @return string containing filename.
	 */
	public String getFilename()
	{
		return(mFilename);
	}
	
	/**
	 * Returns the type of this statement.
	 * @return statement type.
	 */
	public int getType()
	{
		return(mType);
	}

	public Expression []getExpressions()
	{
		return(mExpressions);
	}

	/**
	 * Returns list of statements in "then" section of "if" statement.
	 * @return list of statements.
	 */		
	public ArrayList getThenStatements()
	{
		return(mThenStatements);
	}

	/**
	 * Returns list of statements in "else" section of "if" statement.
	 * @return list of statements.
	 */	
	public ArrayList getElseStatements()
	{
		return(mElseStatements);
	}

	/**
	 * Returns list of statements in while or for loop statement.
	 * @return list of statements.
	 */	
	public ArrayList getLoopStatements()
	{
		return(mLoopStatements);
	}

	/**
	 * Returns hashmap expression to walk through in a for loop statement.
	 * @return expression evaluating to a hashmap.
	 */	
	public Expression getForHashMap()
	{
		return(mForHashMapExpression);
	}

	/**
	 * Return name of procedure block.
	 * @return name of procedure.
	 */
	public String getBlockName()
	{
		return(mBlockName);
	}
	
	/**
	 * Return variable names of parameters to a procedure.
	 * @return list of parameter names.
	 */
	public ArrayList getBlockParameters()
	{
		return(mParameters);
	}
	
	/**
	 * Return statements in a procedure.
	 * @return ArrayList of statements that make up the procedure.
	 */
	public ArrayList getStatementBlock()
	{
		return(mStatementBlock);
	}
}
