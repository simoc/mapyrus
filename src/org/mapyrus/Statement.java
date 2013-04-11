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

/*
 * @(#) $Id$
 */
package org.mapyrus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	public static final int REVERSEPATH = 42;
	public static final int SINKHOLE = 43;
	public static final int GUILLOTINE = 44;
	public static final int STROKE = 45;
	public static final int FILL = 46;
	public static final int GRADIENTFILL = 47;
	public static final int EVENTSCRIPT = 48;
	public static final int PROTECT = 49;
	public static final int UNPROTECT = 50;
	public static final int CLIP = 51;
	public static final int LABEL = 52;
	public static final int FLOWLABEL = 53;
	public static final int TABLE = 54;
	public static final int TREE = 55;
	public static final int ICON = 56;
	public static final int GEOIMAGE = 57;
	public static final int EPS = 58;
	public static final int SVG = 59;
	public static final int SVGCODE = 60;
	public static final int PDF = 61;
	public static final int PDFGROUP = 62;
	public static final int SCALE = 63;
	public static final int ROTATE = 64;
	public static final int WORLDS = 65;
	public static final int DATASET = 66;
	public static final int FETCH = 67;
	public static final int NEWPAGE = 68;
	public static final int ENDPAGE = 69;
	public static final int SETOUTPUT = 70;
	public static final int PRINT = 71;
	public static final int LOCAL = 72;
	public static final int LET = 73;
	public static final int EVAL = 74;
	public static final int KEY = 75;
	public static final int LEGEND = 76;
	public static final int MIMETYPE = 77;
	public static final int HTTPRESPONSE = 78;

	/*
	 * Statement type for call and return to/from user defined procedure block.
	 */
	public static final int CALL = 1000;
	public static final int RETURN = 1001;

	private int m_type;

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
	private static HashMap<String, Integer> m_statementTypeLookup;
	
	static
	{
		m_statementTypeLookup = new HashMap<String, Integer>(100);
		m_statementTypeLookup.put("color", Integer.valueOf(COLOR));
		m_statementTypeLookup.put("colour", Integer.valueOf(COLOR));
		m_statementTypeLookup.put("blend", Integer.valueOf(BLEND));
		m_statementTypeLookup.put("linestyle", Integer.valueOf(LINESTYLE));
		m_statementTypeLookup.put("font", Integer.valueOf(FONT));
		m_statementTypeLookup.put("justify", Integer.valueOf(JUSTIFY));
		m_statementTypeLookup.put("move", Integer.valueOf(MOVE));
		m_statementTypeLookup.put("draw", Integer.valueOf(DRAW));
		m_statementTypeLookup.put("rdraw", Integer.valueOf(RDRAW));
		m_statementTypeLookup.put("arc", Integer.valueOf(ARC));
		m_statementTypeLookup.put("circle", Integer.valueOf(CIRCLE));
		m_statementTypeLookup.put("ellipse", Integer.valueOf(ELLIPSE));
		m_statementTypeLookup.put("cylinder", Integer.valueOf(CYLINDER));
		m_statementTypeLookup.put("raindrop", Integer.valueOf(RAINDROP));
		m_statementTypeLookup.put("bezier", Integer.valueOf(BEZIER));
		m_statementTypeLookup.put("sinewave", Integer.valueOf(SINEWAVE));
		m_statementTypeLookup.put("wedge", Integer.valueOf(WEDGE));
		m_statementTypeLookup.put("spiral", Integer.valueOf(SPIRAL));
		m_statementTypeLookup.put("box", Integer.valueOf(BOX));
		m_statementTypeLookup.put("roundedbox", Integer.valueOf(ROUNDEDBOX));
		m_statementTypeLookup.put("box3d", Integer.valueOf(BOX3D));
		m_statementTypeLookup.put("chessboard", Integer.valueOf(CHESSBOARD));
		m_statementTypeLookup.put("hexagon", Integer.valueOf(HEXAGON));
		m_statementTypeLookup.put("pentagon", Integer.valueOf(PENTAGON));
		m_statementTypeLookup.put("triangle", Integer.valueOf(TRIANGLE));
		m_statementTypeLookup.put("star", Integer.valueOf(STAR));
		m_statementTypeLookup.put("addpath", Integer.valueOf(ADDPATH));
		m_statementTypeLookup.put("clearpath", Integer.valueOf(CLEARPATH));
		m_statementTypeLookup.put("closepath", Integer.valueOf(CLOSEPATH));
		m_statementTypeLookup.put("samplepath", Integer.valueOf(SAMPLEPATH));
		m_statementTypeLookup.put("stripepath", Integer.valueOf(STRIPEPATH));
		m_statementTypeLookup.put("shiftpath", Integer.valueOf(SHIFTPATH));
		m_statementTypeLookup.put("parallelpath", Integer.valueOf(PARALLELPATH));
		m_statementTypeLookup.put("selectpath", Integer.valueOf(SELECTPATH));
		m_statementTypeLookup.put("reversepath", Integer.valueOf(REVERSEPATH));
		m_statementTypeLookup.put("sinkhole", Integer.valueOf(SINKHOLE));
		m_statementTypeLookup.put("guillotine", Integer.valueOf(GUILLOTINE));
		m_statementTypeLookup.put("stroke", Integer.valueOf(STROKE));
		m_statementTypeLookup.put("fill", Integer.valueOf(FILL));
		m_statementTypeLookup.put("gradientfill", Integer.valueOf(GRADIENTFILL));
		m_statementTypeLookup.put("eventscript", Integer.valueOf(EVENTSCRIPT));
		m_statementTypeLookup.put("protect", Integer.valueOf(PROTECT));
		m_statementTypeLookup.put("unprotect", Integer.valueOf(UNPROTECT));
		m_statementTypeLookup.put("clip", Integer.valueOf(CLIP));
		m_statementTypeLookup.put("label", Integer.valueOf(LABEL));
		m_statementTypeLookup.put("flowlabel", Integer.valueOf(FLOWLABEL));
		m_statementTypeLookup.put("table", Integer.valueOf(TABLE));
		m_statementTypeLookup.put("tree", Integer.valueOf(TREE));
		m_statementTypeLookup.put("icon", Integer.valueOf(ICON));
		m_statementTypeLookup.put("geoimage", Integer.valueOf(GEOIMAGE));
		m_statementTypeLookup.put("eps", Integer.valueOf(EPS));
		m_statementTypeLookup.put("svg", Integer.valueOf(SVG));
		m_statementTypeLookup.put("svgcode", Integer.valueOf(SVGCODE));
		m_statementTypeLookup.put("pdf", Integer.valueOf(PDF));
		m_statementTypeLookup.put("pdfgroup", Integer.valueOf(PDFGROUP));
		m_statementTypeLookup.put("scale", Integer.valueOf(SCALE));
		m_statementTypeLookup.put("rotate", Integer.valueOf(ROTATE));
		m_statementTypeLookup.put("worlds", Integer.valueOf(WORLDS));
		m_statementTypeLookup.put("dataset", Integer.valueOf(DATASET));
		m_statementTypeLookup.put("fetch", Integer.valueOf(FETCH));
		m_statementTypeLookup.put("newpage", Integer.valueOf(NEWPAGE));
		m_statementTypeLookup.put("endpage", Integer.valueOf(ENDPAGE));
		m_statementTypeLookup.put("setoutput", Integer.valueOf(SETOUTPUT));
		m_statementTypeLookup.put("print", Integer.valueOf(PRINT));
		m_statementTypeLookup.put("local", Integer.valueOf(LOCAL));
		m_statementTypeLookup.put("let", Integer.valueOf(LET));
		m_statementTypeLookup.put("eval", Integer.valueOf(EVAL));
		m_statementTypeLookup.put("key", Integer.valueOf(KEY));
		m_statementTypeLookup.put("legend", Integer.valueOf(LEGEND));
		m_statementTypeLookup.put("mimetype", Integer.valueOf(MIMETYPE));
		m_statementTypeLookup.put("httpresponse", Integer.valueOf(HTTPRESPONSE));
		m_statementTypeLookup.put("return", Integer.valueOf(RETURN));
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
		Integer type = m_statementTypeLookup.get(s.toLowerCase());

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
		m_type = getStatementType(keyword);
		if (m_type == CALL)
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
		m_type = BLOCK;
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
		m_type = CONDITIONAL;
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
		m_type = isWhileLoop ? WHILE_LOOP : REPEAT_LOOP;
		m_expressions = new Expression[1];
		m_expressions[0] = test;
		m_loopStatements = loopStatements;
	}

	/**
	 * Create a for loop block of statements.
	 * @param test is expression to test before each iteration of loop.
	 * @param loopStatements is statements to execute for each loop iteration.
	 */
	public Statement(Expression var, Expression arrayVar, ArrayList<Statement> loopStatements)
	{
		m_type = FOR_LOOP;
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
	public int getType()
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

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if (m_type == CALL)
			sb.append("CALL ").append(m_blockName);
		else if (m_type == CONDITIONAL)
			sb.append("CONDITIONAL");
		else if (m_type == REPEAT_LOOP)
			sb.append("REPEAT_LOOP");
		else if (m_type == WHILE_LOOP)
			sb.append("WHILE_LOOP");
		else if (m_type == FOR_LOOP)
			sb.append("FOR_LOOP");
		else if (m_type == BLOCK)
			sb.append("BLOCK ").append(m_blockName);
		else
		{
			for (Map.Entry<String, Integer> entry : m_statementTypeLookup.entrySet())
			{
				if (entry.getValue().intValue() == m_type)
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
