/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004 Simon Chenery.
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
	public static final int REPEAT_LOOP = 3;
	public static final int WHILE_LOOP = 4;
	public static final int FOR_LOOP = 5;
	public static final int BLOCK = 6;

	public static final int COLOR = 10;
	public static final int LINESTYLE = 11;
	public static final int FONT = 12;
	public static final int JUSTIFY = 13;
	public static final int MOVE = 14;
	public static final int DRAW = 15;
	public static final int RDRAW = 16;
	public static final int ARC = 17;
	public static final int CIRCLE = 18;
	public static final int ELLIPSE = 19;
	public static final int WEDGE = 20;
	public static final int BOX = 21;
	public static final int ROUNDEDBOX = 22;
	public static final int HEXAGON = 23;
	public static final int TRIANGLE = 24;
	public static final int STAR = 25;
	public static final int ADDPATH = 26;
	public static final int CLEARPATH = 27;
	public static final int CLOSEPATH = 28;
	public static final int SAMPLEPATH = 29;
	public static final int STRIPEPATH = 30;
	public static final int SHIFTPATH = 31;
	public static final int PARALLELPATH = 32;
	public static final int SINKHOLE = 33;
	public static final int GUILLOTINE = 34;
	public static final int STROKE = 35;
	public static final int FILL = 36;
	public static final int GRADIENTFILL = 37;
	public static final int PROTECT = 38;
	public static final int UNPROTECT = 39;
	public static final int CLIP = 40;
	public static final int LABEL = 41;
	public static final int FLOWLABEL = 42;
	public static final int ICON = 43;
	public static final int SCALE = 44;
	public static final int ROTATE = 45;
	public static final int WORLDS = 46;
	public static final int PROJECT = 47;
	public static final int DATASET = 48;
	public static final int FETCH = 49;
	public static final int NEWPAGE = 50;
	public static final int PRINT = 51;
	public static final int LOCAL = 52;
	public static final int LET = 53;
	public static final int EVAL = 54;
	public static final int KEY = 55;
	public static final int LEGEND = 56;
	public static final int MIMETYPE = 57;

	/*
	 * Statement type for call and return to/from user defined procedure block.
	 */
	public static final int CALL = 1000;
	public static final int RETURN = 1001;

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
		mStatementTypeLookup.put("rdraw", new Integer(RDRAW));
		mStatementTypeLookup.put("arc", new Integer(ARC));
		mStatementTypeLookup.put("circle", new Integer(CIRCLE));
		mStatementTypeLookup.put("ellipse", new Integer(ELLIPSE));
		mStatementTypeLookup.put("wedge", new Integer(WEDGE));
		mStatementTypeLookup.put("box", new Integer(BOX));
		mStatementTypeLookup.put("roundedbox", new Integer(ROUNDEDBOX));
		mStatementTypeLookup.put("hexagon", new Integer(HEXAGON));
		mStatementTypeLookup.put("triangle", new Integer(TRIANGLE));
		mStatementTypeLookup.put("star", new Integer(STAR));
		mStatementTypeLookup.put("addpath", new Integer(ADDPATH));
		mStatementTypeLookup.put("clearpath", new Integer(CLEARPATH));
		mStatementTypeLookup.put("closepath", new Integer(CLOSEPATH));
		mStatementTypeLookup.put("samplepath", new Integer(SAMPLEPATH));
		mStatementTypeLookup.put("stripepath", new Integer(STRIPEPATH));
		mStatementTypeLookup.put("shiftpath", new Integer(SHIFTPATH));
		mStatementTypeLookup.put("parallelpath", new Integer(PARALLELPATH));
		mStatementTypeLookup.put("sinkhole", new Integer(SINKHOLE));
		mStatementTypeLookup.put("guillotine", new Integer(GUILLOTINE));
		mStatementTypeLookup.put("stroke", new Integer(STROKE));
		mStatementTypeLookup.put("fill", new Integer(FILL));
		mStatementTypeLookup.put("gradientfill", new Integer(GRADIENTFILL));
		mStatementTypeLookup.put("protect", new Integer(PROTECT));
		mStatementTypeLookup.put("unprotect", new Integer(UNPROTECT));
		mStatementTypeLookup.put("clip", new Integer(CLIP));
		mStatementTypeLookup.put("label", new Integer(LABEL));
		mStatementTypeLookup.put("flowlabel", new Integer(FLOWLABEL));
		mStatementTypeLookup.put("icon", new Integer(ICON));
		mStatementTypeLookup.put("scale", new Integer(SCALE));
		mStatementTypeLookup.put("rotate", new Integer(ROTATE));
		mStatementTypeLookup.put("worlds", new Integer(WORLDS));
		mStatementTypeLookup.put("project", new Integer(PROJECT));
		mStatementTypeLookup.put("dataset", new Integer(DATASET));
		mStatementTypeLookup.put("fetch", new Integer(FETCH));
		mStatementTypeLookup.put("newpage", new Integer(NEWPAGE));
		mStatementTypeLookup.put("print", new Integer(PRINT));
		mStatementTypeLookup.put("local", new Integer(LOCAL));
		mStatementTypeLookup.put("let", new Integer(LET));
		mStatementTypeLookup.put("eval", new Integer(EVAL));
		mStatementTypeLookup.put("key", new Integer(KEY));
		mStatementTypeLookup.put("legend", new Integer(LEGEND));
		mStatementTypeLookup.put("mimetype", new Integer(MIMETYPE));
	}

	/**
	 * Constant for 'return' statement.
	 */
	public static final Statement RETURN_STATEMENT;

	static
	{
		RETURN_STATEMENT = new Statement("", null);
		RETURN_STATEMENT.mType = RETURN;
	};

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
	 * Create a repeat or while loop block of statements.
	 * @param test is expression to test before each iteration of loop.
	 * @param loopStatements is statements to execute for each loop iteration.
	 * @param isWhileLoop true for a while loop, false for a repeat loop.
	 */
	public Statement(Expression test, ArrayList loopStatements,
		boolean isWhileLoop)
	{
		mType = isWhileLoop ? WHILE_LOOP : REPEAT_LOOP;
		mExpressions = new Expression[1];
		mExpressions[0] = test;
		mLoopStatements = loopStatements;
	}

	/**
	 * Create a for loop block of statements.
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
