/*
 * $Id$
 */
package au.id.chenery.mapyrus;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * A parsed statement.
 * Can be one of several types.  An assignment statement, a conditional
 * statement, a block of statements making a procedure or just a plain command.
 */
public class Statement
{
	/*
	 * Possible types of statements.
	 */
	public static final int ASSIGN = 1;
	public static final int CONDITIONAL = 2;
	public static final int LOOP = 3;
	public static final int BLOCK = 4;
	
	public static final int COLOR = 10;
	public static final int LINEWIDTH = 11;
	public static final int MOVE = 12;
	public static final int DRAW = 13;
	public static final int ARC = 14;
	public static final int CLEARPATH = 15;
	public static final int SLICEPATH = 16;
	public static final int STRIPEPATH = 17;
	public static final int STROKE = 18;
	public static final int FILL = 19;
	public static final int CLIP = 20;
	public static final int SCALE = 21;
	public static final int ROTATE = 22;
	public static final int WORLDS = 23;
	public static final int PROJECT = 24;
	public static final int DATASET = 25;
	public static final int IMPORT = 26;
	public static final int FETCH = 27;
	public static final int NEWPAGE = 28;
	public static final int PRINT = 29;

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
	
	/*
	 * Name of variable in assignment.
	 */
	private String mAssignedVariable;

	private Expression []mExpressions;
	
	/*
	 * Filename and line number within file that this 
	 * statement was read from.
	 */
	private String mFilename;
	private int mLineNumber;

	/*
	 * Static statement type lookup table for fast lookup.
	 */
	private static Hashtable mStatementTypeLookup;
	
	static
	{
		mStatementTypeLookup = new Hashtable();
		mStatementTypeLookup.put("color", new Integer(COLOR));
		mStatementTypeLookup.put("colour", new Integer(COLOR));
		mStatementTypeLookup.put("linewidth", new Integer(LINEWIDTH));
		mStatementTypeLookup.put("move", new Integer(MOVE));
		mStatementTypeLookup.put("draw", new Integer(DRAW));
		mStatementTypeLookup.put("clearpath", new Integer(CLEARPATH));
		mStatementTypeLookup.put("slicepath", new Integer(SLICEPATH));
		mStatementTypeLookup.put("stripepath", new Integer(STRIPEPATH));
		mStatementTypeLookup.put("stroke", new Integer(STROKE));
		mStatementTypeLookup.put("fill", new Integer(FILL));
		mStatementTypeLookup.put("clip", new Integer(CLIP));
		mStatementTypeLookup.put("scale", new Integer(SCALE));
		mStatementTypeLookup.put("rotate", new Integer(ROTATE));
		mStatementTypeLookup.put("worlds", new Integer(WORLDS));
		mStatementTypeLookup.put("project", new Integer(PROJECT));
		mStatementTypeLookup.put("dataset", new Integer(DATASET));
		mStatementTypeLookup.put("import", new Integer(IMPORT));
		mStatementTypeLookup.put("fetch", new Integer(FETCH));
		mStatementTypeLookup.put("newpage", new Integer(NEWPAGE));
		mStatementTypeLookup.put("print", new Integer(PRINT));
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
	 * Create an assignment statement.
	 * @param variableName is the name of the variable being assigned.
	 * @param value is the value being assigned to this variable.
	 */
	public Statement(String variableName, Expression value)
	{
		mType = ASSIGN;
		mAssignedVariable = variableName;
		mExpressions = new Expression[1];
		mExpressions[0] = value;
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
		mType = LOOP;
		mExpressions = new Expression[1];
		mExpressions[0] = test;
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

	public String getAssignedVariable()
	{
		return(mAssignedVariable);
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
	 * Returns list of statements in while loop statement.
	 * @return list of statements.
	 */	
	public ArrayList getLoopStatements()
	{
		return(mLoopStatements);
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
