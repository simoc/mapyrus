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

/**
 * A statement or other keyword, read from a file and parsed.
 * A statement can be a simple statement, a procedure block or
 * a complete control-flow block such as an if ... then ... else ... endif block.
 *
 * A parsed statement may also be a control-flow keyword that goes between
 * other statements.
 */
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
	public static final int PARSED_IN = 9;

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

