/**
 * A statement or other keyword, read from a file and parsed.
 * A statement can be a simple statement, a procedure block or
 * a complete control-flow block such as an if ... then ... else ... endif block.
 *
 * A parsed statement may also be a control-flow keyword that goes between
 * other statements.
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;

import java.io.*;

public class ParsedStatement
{
	/*
	 * Recognised keywords that may be parsed instead of an
	 * statement.
	 */
	public static final int PARSED_END = 1;
	
	public static final int PARSED_THEN = 2;
	public static final int PARSED_ELSE = 3;
	public static final int PARSED_ELSIF = 4;
	public static final int PARSED_ENDIF = 5;

	public static final int PARSED_DO = 7;
	public static final int PARSED_DONE = 8;

	private Statement mStatement;	/* statement that was parsed */
	private int mKeywordType;	/* type of keyword parsed */

	/**
	 * Sets regular statement as parsed statement.
	 * @param statement is parsed statement.
	 */
	public ParsedStatement(Statement statement)
	{
		mStatement = statement;
	}

	/**
	 * Set keyword read instead of statement.
	 * @param keywordType identifier of parsed keyword.
	 */
	public ParsedStatement(int keywordType)
	{
		mStatement = null;
		mKeywordType = keywordType;
	}

	/*
	 * Returns true if a regular statement was parsed.
	 * @return flag indicating whether this is a regular statement.
	 */
	public boolean isStatement()
	{
		return(mStatement != null);
	}

	/**
	 * Returns parsed statement.
	 * @return parsed statement.
	 */
	public Statement getStatement()
	{
		return(mStatement);
	}

	/**
	 * Return parsed keyword.
	 * @return identifier for keyword.
	 */
	public int getKeywordType()
	{
		return(mKeywordType);
	}
}

