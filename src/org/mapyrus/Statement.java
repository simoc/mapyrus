/**
 * A parsed statement.
 * Can be one of several types.  An assignment statement, a conditional
 * statement or just a plain command.
 */

/*
 * $Id$
 */
public class Statement
{
	/*
	 * Possible types of statements.
	 */
	public static final int ASSIGN = 1;
	public static final int CONDITION = 2;

	public static final int COLOR = 3;
	public static final int LINEWIDTH = 4;
	public static final int MOVE = 5;
	public static final int DRAW = 6;
	public static final int STROKE = 7;
	public static final int FILL = 8;
	public static final int SCALE = 9;
	public static final int NEWPAGE = 10;
	public static final int PRINT = 11;
	
	private int mType;
	Statement mThenStatement;
	Statement mElseStatement;
	String mAssignedVariable;

	Expression []mExpressions;

	/**
	 * Looks up identifier for a statement name.
	 * @param s is the name of the statement.
	 * @returns numeric code for this statement, or -1 if statement
	 * is unknown.
	 */
	public static int getStatementType(String s)
	{
		int retval = -1;
		String sLower = s.toLowerCase();

		if (sLower.equals("color") || sLower.equals("colour"))
		{
			retval = COLOR;
		}
		else if (sLower.equals("linewidth"))
		{
			retval = LINEWIDTH;
		}
		else if (sLower.equals("move"))
		{
			retval = MOVE;
		}
		else if (sLower.equals("draw"))
		{
			retval = DRAW;
		}
		else if (sLower.equals("stroke"))
		{
			retval = STROKE;
		}
		else if (sLower.equals("fill"))
		{
			retval = FILL;
		}
		else if (sLower.equals("scale"))
		{
			retval = SCALE;
		}
		else if (sLower.equals("newpage"))
		{
			retval = NEWPAGE;
		}
		else if (sLower.equals("print"))
		{
			retval = PRINT;
		}
		return(retval);
	}
	
	/**
	 * Creates a plain statement.
	 * @param type is the type of statement.
	 * @param expressions are the arguments for this statement.
	 */
	public Statement(int type, Expression []expressions)
	{
		mType = type;
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
		mType = CONDITION;
		mExpressions = new Expression[1];
		//mExpressions[0] = value;
	}

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
}
