/**
 * Language interpreter.  Parse and executes commands read from file, or
 * typed by user.
 * 
 * Interpretation runs as a separate thread.
 */

/*
 * $Id$
 */

import java.io.*;
import java.lang.String;
import java.util.Vector;

public class Interpreter extends Thread
{
	/*
	 * Character starting a comment on a line.
	 */
	private static final int COMMENT_CHAR = '#';
	
	/*
	 * States during expression parsing.
	 */
	private static final int AT_START = 1;
	private static final int AT_START_ARGS = 2;
	private static final int IN_ARGS = 3;
	private static final int IN_COMMENT = 4;

	Preprocessor mPre;
	Context mContext;
	
	/*
	 * Return status of interpreter and error message when something has gone wrong.
	 */
	boolean mReturnStatus;
	String mErrorMessage;

	/**
	 * Reads, parses and returns next statement.
	 * @return next statement read from file, or null if EOF was reached
	 * before a statement could be read.
	 */
	private Statement parseStatement() throws MapyrusException, IOException
	{
		int c;
		int state = AT_START;
		StringBuffer keyword = new StringBuffer();
		Vector expressions = new Vector();
		Expression expr;
		Statement retval = null;
		boolean isAssignment = false;
		boolean atEOF = false;

		c = mPre.read();
		while (true)
		{
			if (c == -1)
			{
				/*
				 * Reached EOF.
				 */
				atEOF = true;
				break;
			}
			else if (c == COMMENT_CHAR)
			{
				/*
				 * Found the start of a comment, ignore everything else on line.
				 */
				state = IN_COMMENT;
				c = mPre.read();
			}
			else if (c == '\n')
			{
				if (keyword.length() > 0)
				{
					/*
					 * End of line signals
					 * end of statement.
					 */
					break;
				}
				else
				{
					/*
					 * We did not even begin reading
					 * a statement so expect to
					 * read one on the next line.
					 */
					state = AT_START;
				}
				c = mPre.read();
			}
			else if (state == IN_COMMENT)
			{
				/*
				 * Do nothing -- we are reading a comment.
				 */
				c = mPre.read();
			}
			else if (Character.isWhitespace((char)c))
			{
				/*
				 * Skip whitespace
				 */
				c = mPre.read();
			}
			else if (state == AT_START)
			{
				/*
				 * A statement begins with a keyword
				 * which must begin with a letter.
				 */
				if (!Character.isLetter((char)c))
				{
					throw new MapyrusException("Invalid statement at " + mPre.getCurrentFilenameAndLine());
				}

				/*
				 * Read in whole keyword.
				 */
				do
				{
					keyword.append((char)c);
					c = mPre.read();
				}
				while (Character.isLetterOrDigit((char)c) || c == '.' || c == '_');
				state = AT_START_ARGS;
			}
			else if (state == AT_START_ARGS)
			{
				/*
				 * Is this an assignment statement or
				 * some other kind of statement?
				 */
				isAssignment = (c == '=');
				if (isAssignment)
				{
					c = mPre.read();
				}
				else
				{
					mPre.unread(c);
				}
				state = IN_ARGS;
			}
			
			if (state == IN_ARGS)
			{
				/*
				 * Parse an expression.
				 */
				expr = new Expression(mPre);
				expressions.add(expr);
				/*
				 * After an expression we expect a ',' and then another expression,
				 * or a newline.
				 */
				c = mPre.read();
				if (c != '\n' && c != ',' && c!= COMMENT_CHAR)
				{
					throw new MapyrusException("Expecting ',' between expressions at " +
						mPre.getCurrentFilenameAndLine());
				}
			}
		}

		/*
		 * Build a statement structure for what we just parsed.
		 */
		if (atEOF && (state == AT_START || state == IN_COMMENT))
		{
			/*
			 * Could not parse anything before we got EOF.
			 */
			return(null);
		}
		else if (isAssignment)
		{
			if (expressions.size() > 1)
			{
				throw new MapyrusException("Too many expressions in assignment at " +
					mPre.getCurrentFilenameAndLine());
			}
			else if (expressions.size() == 0)
			{
				throw new MapyrusException("No expression in assignment at " +
					mPre.getCurrentFilenameAndLine());
			}
			retval = new Statement(keyword.toString(),
				(Expression)expressions.elementAt(0));
		}
		else
		{
			int statementType;
			Expression []a = new Expression[expressions.size()];

			statementType = Statement.getStatementType(keyword.toString());
			if (statementType < 0)
			{
				throw new MapyrusException("Keyword " + keyword +
					" not recognized " + mPre.getCurrentFilenameAndLine());
			}
			for (int i = 0; i < a.length; i++)
			{
				a[i] = (Expression)expressions.elementAt(i);
			}
			retval = new Statement(statementType, a);
		}
		return(retval);
	}

	/**
	 * Gets status of completed interpreter.
	 * @return true if interpreter finished successfully.
	 */
	public boolean getReturnStatus()
	{
		return(mReturnStatus);
	}
	
	/**
	 * Gets error message of completed interpreter that has failed.
	 * @return error message.
	 */
	public String getErrorMessage()
	{
		return(mErrorMessage);
	}

	/*
	 * Exceute a single statement, changing the path, context or generating
	 * some output.
	 */
	private void execute(Statement st, Context context) throws MapyrusException
	{
		Expression []expr;
		int nExpressions;
		Argument []args = null;

		expr = st.getExpressions();
		nExpressions = expr.length;
		
		/*
		 * Evaluate each of the expressions for this statement.
		 */
		if (nExpressions > 0)
		{
			args = new Argument[nExpressions];
			for (int i = 0; i < nExpressions; i++)
			{
				args[i] = expr[i].evaluate(context);
			}
		}

		switch (st.getType())
		{
			case Statement.COLOR:
				if (nExpressions == 1 && args[0].getType() == Argument.STRING)
				{
					/*
					 * Find named color in color name database.
					 */
				}
				else if (nExpressions == 4 && args[0].getType() == Argument.STRING &&
					args[0].getStringValue().equalsIgnoreCase("rgb"))
				{
					/*
					 * Set RGB color.
					 */
				}
				break;
			case Statement.LINEWIDTH:
				if (nExpressions == 1 && args[0].getType() == Argument.NUMERIC)
				{
					/*
					 * Set new line width.
					 */
					context.setLineWidth(args[0].getNumericValue());
				}
				else
				{
				}
				break;
			case Statement.MOVE:
				if (nExpressions == 2 && args[0].getType() == Argument.NUMERIC &&
					args[1].getType() == Argument.NUMERIC)
				{
					/*
					 * Add point to path.
					 */
					context.moveTo((float)args[0].getNumericValue(),
						(float)args[1].getNumericValue());
				}
			case Statement.PRINT:
				/*
				 * Print to stdout each of the expressions passed.
				 */
				for (int i = 0; i <nExpressions; i++)
				{
					if (i > 0)
					{
						System.out.print(" ");
					}
					if (args[i].getType() == Argument.STRING)
					{
						System.out.print(args[i].getStringValue());
					}
					else
					{
						System.out.print(args[i].getNumericValue());
					}
				}
				
				try
				{
					System.out.print(System.getProperty("line.separator"));
				}
				catch (SecurityException e)
				{
				}
				break;
				
			case Statement.ASSIGN:
				context.defineVariable(st.getAssignedVariable(), args[0]);
				break;
		}		
	}
		
		
	/**
	 * Begins interpretation of commands.
	 */
	public void run()
	{
		Statement st;

		try
		{
			/*
			 * Keep parsing until we get EOF.
			 */
			while ((st = parseStatement()) != null)
			{
				execute(st, mContext);
			}
		}
		catch (MapyrusException e)
		{
			mErrorMessage = e.getMessage();
			mReturnStatus = false;
		}
		catch (IOException e)
		{
			mErrorMessage = e.getMessage();
			mReturnStatus = false;
		}
	}

	/**
	 * Create new language interpreter.
	 * @param in is opened Reader to read from.
	 * @param context is the context to use during interpretation.  This may be in
	 * a changed state by the time the interpretation is finished.
	 */
	public Interpreter(Reader in, Context context)
	{
		super();
		mContext = context;
		mPre = new Preprocessor(in);
		mReturnStatus = true;
	}
}
