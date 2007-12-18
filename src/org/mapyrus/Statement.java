/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2007 Simon Chenery.
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

	public static final int COLOR = 9;
	public static final int BLEND = 10;
	public static final int LINESTYLE = 11;
	public static final int FONT = 12;
	public static final int JUSTIFY = 13;
	public static final int MOVE = 14;
	public static final int DRAW = 15;
	public static final int RDRAW = 16;
	public static final int ARC = 17;
	public static final int CIRCLE = 18;
	public static final int ELLIPSE = 19;
	public static final int CYLINDER = 20;
	public static final int RAINDROP = 21;
	public static final int BEZIER = 22;
	public static final int SINEWAVE = 23;
	public static final int WEDGE = 24;
	public static final int SPIRAL = 25;
	public static final int BOX = 26;
	public static final int ROUNDEDBOX = 27;
	public static final int BOX3D = 28;
	public static final int CHESSBOARD = 29;
	public static final int HEXAGON = 30;
	public static final int PENTAGON = 31;
	public static final int TRIANGLE = 32;
	public static final int STAR = 33;
	public static final int ADDPATH = 34;
	public static final int CLEARPATH = 35;
	public static final int CLOSEPATH = 36;
	public static final int SAMPLEPATH = 37;
	public static final int STRIPEPATH = 38;
	public static final int SHIFTPATH = 39;
	public static final int PARALLELPATH = 40;
	public static final int SELECTPATH = 41;
	public static final int SINKHOLE = 42;
	public static final int GUILLOTINE = 43;
	public static final int STROKE = 44;
	public static final int FILL = 45;
	public static final int GRADIENTFILL = 46;
	public static final int EVENTSCRIPT = 47;
	public static final int PROTECT = 48;
	public static final int UNPROTECT = 49;
	public static final int CLIP = 50;
	public static final int LABEL = 51;
	public static final int FLOWLABEL = 52;
	public static final int TABLE = 53;
	public static final int TREE = 54;
	public static final int ICON = 55;
	public static final int GEOIMAGE = 56;
	public static final int EPS = 57;
	public static final int SVG = 58;
	public static final int SVGCODE = 59;
	public static final int PDF = 60;
	public static final int SCALE = 61;
	public static final int ROTATE = 62;
	public static final int WORLDS = 63;
	public static final int DATASET = 64;
	public static final int FETCH = 65;
	public static final int NEWPAGE = 66;
	public static final int ENDPAGE = 67;
	public static final int SETOUTPUT = 68;
	public static final int PRINT = 69;
	public static final int LOCAL = 70;
	public static final int LET = 71;
	public static final int EVAL = 72;
	public static final int KEY = 73;
	public static final int LEGEND = 74;
	public static final int MIMETYPE = 75;
	public static final int HTTPRESPONSE = 76;

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
	private static HashMap<String, Integer> mStatementTypeLookup;
	
	static
	{
		mStatementTypeLookup = new HashMap<String, Integer>(100);
		mStatementTypeLookup.put("color", Integer.valueOf(COLOR));
		mStatementTypeLookup.put("colour", Integer.valueOf(COLOR));
		mStatementTypeLookup.put("blend", Integer.valueOf(BLEND));
		mStatementTypeLookup.put("linestyle", Integer.valueOf(LINESTYLE));
		mStatementTypeLookup.put("font", Integer.valueOf(FONT));
		mStatementTypeLookup.put("justify", Integer.valueOf(JUSTIFY));
		mStatementTypeLookup.put("move", Integer.valueOf(MOVE));
		mStatementTypeLookup.put("draw", Integer.valueOf(DRAW));
		mStatementTypeLookup.put("rdraw", Integer.valueOf(RDRAW));
		mStatementTypeLookup.put("arc", Integer.valueOf(ARC));
		mStatementTypeLookup.put("circle", Integer.valueOf(CIRCLE));
		mStatementTypeLookup.put("ellipse", Integer.valueOf(ELLIPSE));
		mStatementTypeLookup.put("cylinder", Integer.valueOf(CYLINDER));
		mStatementTypeLookup.put("raindrop", Integer.valueOf(RAINDROP));
		mStatementTypeLookup.put("bezier", Integer.valueOf(BEZIER));
		mStatementTypeLookup.put("sinewave", Integer.valueOf(SINEWAVE));
		mStatementTypeLookup.put("wedge", Integer.valueOf(WEDGE));
		mStatementTypeLookup.put("spiral", Integer.valueOf(SPIRAL));
		mStatementTypeLookup.put("box", Integer.valueOf(BOX));
		mStatementTypeLookup.put("roundedbox", Integer.valueOf(ROUNDEDBOX));
		mStatementTypeLookup.put("box3d", Integer.valueOf(BOX3D));
		mStatementTypeLookup.put("chessboard", Integer.valueOf(CHESSBOARD));
		mStatementTypeLookup.put("hexagon", Integer.valueOf(HEXAGON));
		mStatementTypeLookup.put("pentagon", Integer.valueOf(PENTAGON));
		mStatementTypeLookup.put("triangle", Integer.valueOf(TRIANGLE));
		mStatementTypeLookup.put("star", Integer.valueOf(STAR));
		mStatementTypeLookup.put("addpath", Integer.valueOf(ADDPATH));
		mStatementTypeLookup.put("clearpath", Integer.valueOf(CLEARPATH));
		mStatementTypeLookup.put("closepath", Integer.valueOf(CLOSEPATH));
		mStatementTypeLookup.put("samplepath", Integer.valueOf(SAMPLEPATH));
		mStatementTypeLookup.put("stripepath", Integer.valueOf(STRIPEPATH));
		mStatementTypeLookup.put("shiftpath", Integer.valueOf(SHIFTPATH));
		mStatementTypeLookup.put("parallelpath", Integer.valueOf(PARALLELPATH));
		mStatementTypeLookup.put("selectpath", Integer.valueOf(SELECTPATH));
		mStatementTypeLookup.put("sinkhole", Integer.valueOf(SINKHOLE));
		mStatementTypeLookup.put("guillotine", Integer.valueOf(GUILLOTINE));
		mStatementTypeLookup.put("stroke", Integer.valueOf(STROKE));
		mStatementTypeLookup.put("fill", Integer.valueOf(FILL));
		mStatementTypeLookup.put("gradientfill", Integer.valueOf(GRADIENTFILL));
		mStatementTypeLookup.put("eventscript", Integer.valueOf(EVENTSCRIPT));
		mStatementTypeLookup.put("protect", Integer.valueOf(PROTECT));
		mStatementTypeLookup.put("unprotect", Integer.valueOf(UNPROTECT));
		mStatementTypeLookup.put("clip", Integer.valueOf(CLIP));
		mStatementTypeLookup.put("label", Integer.valueOf(LABEL));
		mStatementTypeLookup.put("flowlabel", Integer.valueOf(FLOWLABEL));
		mStatementTypeLookup.put("table", Integer.valueOf(TABLE));
		mStatementTypeLookup.put("tree", Integer.valueOf(TREE));
		mStatementTypeLookup.put("icon", Integer.valueOf(ICON));
		mStatementTypeLookup.put("geoimage", Integer.valueOf(GEOIMAGE));
		mStatementTypeLookup.put("eps", Integer.valueOf(EPS));
		mStatementTypeLookup.put("svg", Integer.valueOf(SVG));
		mStatementTypeLookup.put("svgcode", Integer.valueOf(SVGCODE));
		mStatementTypeLookup.put("pdf", Integer.valueOf(PDF));
		mStatementTypeLookup.put("scale", Integer.valueOf(SCALE));
		mStatementTypeLookup.put("rotate", Integer.valueOf(ROTATE));
		mStatementTypeLookup.put("worlds", Integer.valueOf(WORLDS));
		mStatementTypeLookup.put("dataset", Integer.valueOf(DATASET));
		mStatementTypeLookup.put("fetch", Integer.valueOf(FETCH));
		mStatementTypeLookup.put("newpage", Integer.valueOf(NEWPAGE));
		mStatementTypeLookup.put("endpage", Integer.valueOf(ENDPAGE));
		mStatementTypeLookup.put("setoutput", Integer.valueOf(SETOUTPUT));
		mStatementTypeLookup.put("print", Integer.valueOf(PRINT));
		mStatementTypeLookup.put("local", Integer.valueOf(LOCAL));
		mStatementTypeLookup.put("let", Integer.valueOf(LET));
		mStatementTypeLookup.put("eval", Integer.valueOf(EVAL));
		mStatementTypeLookup.put("key", Integer.valueOf(KEY));
		mStatementTypeLookup.put("legend", Integer.valueOf(LEGEND));
		mStatementTypeLookup.put("mimetype", Integer.valueOf(MIMETYPE));
		mStatementTypeLookup.put("httpresponse", Integer.valueOf(HTTPRESPONSE));
		mStatementTypeLookup.put("return", Integer.valueOf(RETURN));
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
		Integer type = mStatementTypeLookup.get(s.toLowerCase());

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
