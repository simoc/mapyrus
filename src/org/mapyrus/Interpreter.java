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

	private Statement parseStatement() throws MapyrusException, IOException
	{
		int c;
		int state = AT_START;
		int type;
		StringBuffer keyword = new StringBuffer();
		Vector expressions = new Vector();
		Expression expr;
		Statement retval = null;
		boolean isAssign = false;

		c = mPre.read();
		while (true)
		{
			if (c == -1)
			{
				/*
				 * Reached EOF.
				 */
				break;
			}
			else if (c == '#')
			{
				state = IN_COMMENT;
				c = mPre.read();
			}
			else
			{
				type = Character.getType((char)c);
				if (c == Character.LINE_SEPARATOR)
				{
					if (state == AT_START_ARGS || state == IN_ARGS)
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
				else if (Character.isWhitespace((char)c))
				{
					/*
					 * Skip whitespace
					 */
					c = mPre.read();
				}
				else if (state == IN_COMMENT)
				{
					/*
					 * Do nothing -- we are reading a comment.
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
					while (Character.isLetterOrDigit((char)c));
					state = AT_START_ARGS;
				}
				else if (state == AT_START_ARGS)
				{
					/*
					 * Is this an assignment statement or
					 * some other kind of statement?
					 */
					isAssign = (c == '=');
					if (isAssign)
					{
						c = mPre.read();
					}
					state = IN_ARGS;
				}
				else if (state == IN_ARGS)
				{
					/*
					 * Parse an expression.
					 */
					expr = new Expression(mPre);
					expressions.add(expr);

					/*
					 * Gobble up any ',' separator too.
					 */
					c = mPre.read();
					if (c == '\n')
					{
						mPre.unread(c);
					}
					else if (c != ',')
					{
						throw new MapyrusException("Expecting ',' at " + mPre.getCurrentFilenameAndLine());
					}
				}
			}
		}

		/*
		 * Build a statement structure for what we just parsed.
		 */
		if (isAssign)
		{
			if (expressions.size() > 1)
			{
				throw new MapyrusException("Too many expressions in assignment at " + mPre.getCurrentFilenameAndLine());
			}
			else if (expressions.size() == 0)
			{
				throw new MapyrusException("No expression in assignment at " + mPre.getCurrentFilenameAndLine());
			}
			retval = new Statement(keyword.toString(),
				(Expression)expressions.elementAt(0));
		}
		else
		{
			int statementType;
			Expression []a;

			statementType = Statement.getStatementType(keyword.toString());
			a = (Expression [])expressions.toArray();
			if (statementType < 0)
			{
				throw new MapyrusException("Keyword " + keyword + " not recognized " + mPre.getCurrentFilenameAndLine());
			}
			retval = new Statement(statementType, a);
		}
		return(retval);
	}

	/**
	 * Gets status of completed interpreter.
	 * @return true if interpreter finished successfully.
	 */
	boolean getReturnStatus()
	{
		return(mReturnStatus);
	}
	
	/**
	 * Gets error message of completed interpreter that has failed.
	 * @return error message.
	 */
	String getErrorMessage()
	{
		return(mErrorMessage);
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
				System.out.println("Parsed a statement");
				System.out.println(st.getType());
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
	 * @param context is the context to use during interpretation.  This may be changed
	 * at the end of the interpretation.
	 */
	public Interpreter(Reader in, Context context)
	{
		super();
		mContext = context;
		mPre = new Preprocessor(in);
		mReturnStatus = true;
	}
}

