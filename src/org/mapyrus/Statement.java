/**
 * A parsed statement.
 * Can be one of several types.  An assignment statement, a conditional
 * statement, a block of statements making a procedure or just a plain command.
 */

/*
 * $Id$
 */
import java.util.Vector;
import java.util.Hashtable;

public class Statement
{
	/*
	 * Possible types of statements.
	 */
	public static final int ASSIGN = 1;
	public static final int CONDITIONAL = 2;
	public static final int BLOCK = 3;
	
	public static final int COLOR = 4;
	public static final int LINEWIDTH = 5;
	public static final int MOVE = 6;
	public static final int DRAW = 7;
	public static final int CLEAR = 8;
	public static final int STROKE = 9;
	public static final int FILL = 10;
	public static final int SCALE = 11;
	public static final int NEWPAGE = 12;
	public static final int PRINT = 13;
	
	/*
	 * Statement type for call to user defined procedure block.
	 */
	public static final int CALL = 14;
	
	private int mType;
	
	/*
	 * Statements in an if-then-else statement.
	 */
	private Statement mThenStatement;
	private Statement mElseStatement;
	
	/*
	 * Name of procedure block,
	 * variable names of parameters to this procedure
	 * and block of statements in a procedure in order of execution
	 */
	private String mBlockName;
	private Vector mStatementBlock;
	private Vector mParameters;
	
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
		mStatementTypeLookup.put("clear", new Integer(CLEAR));
		mStatementTypeLookup.put("stroke", new Integer(STROKE));
		mStatementTypeLookup.put("fill", new Integer(FILL));
		mStatementTypeLookup.put("scale", new Integer(SCALE));
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
	 * Creates a conditional statement.
	 * @param type is the type of statement.
	 * @param expressions are the arguments for this statement.
	 */
	public Statement(Expression condition,
		Statement thenStatement, Statement elseStatement)
	{
		mType = CONDITIONAL;
		mExpressions = new Expression[1];
	}

	/**
	 * Creates a procedure, a block of statements to be executed together.
	 * @param blockName is name of procedure block.
	 * @param parameters variable names of parameters to this procedure.
	 * @param statements list of statements that make up this procedure block.
	 */
	public Statement(String blockName, Vector parameters, Vector statements)
	{
		mBlockName = blockName;
		mParameters = parameters;
		mStatementBlock = statements;
		mType = BLOCK;
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
		return(mFilename + " line " + mLineNumber);
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
		
	public Statement getThenStatement()
	{
		return(mThenStatement);
	}

	public Statement getElseStatement()
	{
		return(mElseStatement);
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
	public Vector getBlockParameters()
	{
		return(mParameters);
	}
	
	/**
	 * Return statements in a procedure.
	 * @return vector of statements that make up the procedure.
	 */
	public Vector getStatementBlock()
	{
		return(mStatementBlock);
	}
}
