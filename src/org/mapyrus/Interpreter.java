/**
 * Language interpreter.  Parse and executes commands read from file, or
 * typed by user.
 * 
 * May be called repeatedly to interpret several files in the same context.
 */

/*
 * $Id$
 */
import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Vector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

public class Interpreter
{
	/*
	 * Character starting a comment on a line.
	 * Character separating arguments to a statement.
	 * Tokens around definition of a procedure.
	 */
	private static final char COMMENT_CHAR = '#';
	private static final char ARGUMENT_SEPARATOR = ',';
	private static final char PARAM_SEPARATOR = ',';
	private static final String BEGIN_KEYWORD = "begin";
	private static final String END_KEYWORD = "end";

	/*
	 * Keywords for if ... then ... else ... endif block.
	 */
	private static final String IF_KEYWORD = "if";
	private static final String THEN_KEYWORD = "then";
	private static final String ELSE_KEYWORD = "else";
	private static final String ELSIF_KEYWORD = "elsif";
	private static final String ENDIF_KEYWORD = "endif";
	
	/*
	 * Keywords for while ... do ... done block.
	 */
	private static final String WHILE_KEYWORD = "while";
	private static final String DO_KEYWORD = "do";
	private static final String DONE_KEYWORD = "done";

	/*
	 * States during parsing statements.
	 */
	private static final int AT_STATEMENT = 1;		/* at start of a statement */
	private static final int AT_ARG = 2;		/* at argument to a statement */
	private static final int AT_ARG_SEPARATOR = 3;	/* at separator between arguments */

	private static final int AT_PARAM = 4;	/* at parameter to a procedure block */
	private static final int AT_PARAM_SEPARATOR = 5;	/* at separator between parameters */


	private static final int AT_BLOCK_NAME = 6;	/* expecting procedure block name */
	private static final int AT_BLOCK_PARAM = 7;
	
	private static final int AT_IF_TEST = 8;	/* at expression to test in if ... then block */
	private static final int AT_THEN_KEYWORD = 9;	/* at "then" keyword in if ... then block */
	private static final int AT_ELSE_KEYWORD = 10;	/* at "else" keyword in if ... then block */
	private static final int AT_ENDIF_KEYWORD = 11;	/* at "endif" keyword in if ... then block */

	private static final int AT_WHILE_TEST = 8;	/* at expression to test in while loop block */
	private static final int AT_DO_KEYWORD = 9;	/* at "do" keyword in while loop block */
	private static final int AT_DONE_KEYWORD = 10;	/* at "done" keyword in while loop block */

	private ContextStack mContext;
	
	/*
	 * Blocks of statements for each procedure defined in
	 * this interpreter.
	 */
	private Hashtable mStatementBlocks;
	
	/*
	 * Execute a single statement, changing the path, context or generating
	 * some output.
	 */
	private void execute(Statement st, ContextStack context)
		throws MapyrusException, IOException
	{
		Expression []expr;
		int nExpressions;
		int type;
		Argument []args = null;
		double degrees;

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
		
		type = st.getType();
		switch (type)
		{
			case Statement.COLOR:
				if (nExpressions == 1 && args[0].getType() == Argument.STRING)
				{
					/*
					 * Find named color in color name database.
					 */
					Color c = ColorDatabase.getColor(args[0].getStringValue());
					if (c == null)
					{
						throw new MapyrusException("Color not found: " +
							args[0].getStringValue());
					}
					context.setColor(c); 
				}
				else if (nExpressions == 4 &&
					args[0].getType() == Argument.STRING &&
					args[1].getType() == Argument.NUMERIC &&
					args[2].getType() == Argument.NUMERIC &&
					args[3].getType() == Argument.NUMERIC)
				{
					String colorType = args[0].getStringValue();
					float c1 = (float)args[1].getNumericValue();
					float c2 = (float)args[2].getNumericValue();
					float c3 = (float)args[3].getNumericValue();
					
					if (colorType.equalsIgnoreCase("hsb"))
					{
						/*
						 * Set HSB color.
						 */
						int rgb = Color.HSBtoRGB(c1, c2, c3);
						context.setColor(new Color(rgb));
					}
					else if (colorType.equalsIgnoreCase("rgb"))
					{		
						/*
						 * Set RGB color.
						 */
						context.setColor(new Color(c1, c2, c3));
					}
					else
					{
						throw new MapyrusException("Unknown color type: " +
							colorType);
					}
				}
				else
				{
					throw new MapyrusException("Invalid color at " +
						st.getFilenameAndLineNumber());
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
					throw new MapyrusException("Invalid line width at " +
							st.getFilenameAndLineNumber());
				}
				break;
			
			case Statement.MOVE:
			case Statement.DRAW:
				if (nExpressions > 0 && nExpressions % 2 == 0)
				{
					/*
					 * Check that all coordindate values are numbers.
					 */
					for (int i = 0; i < nExpressions; i++)
					{
						if (args[0].getType() != Argument.NUMERIC)
						{
							throw new MapyrusException("Invalid coordinate value at " +
								st.getFilenameAndLineNumber());
						}
					}
					
					for (int i = 0; i < nExpressions; i += 2)
					{
						/*
						 * Add point to path.
						 */
						if (type == Statement.MOVE)
						{
							context.moveTo(args[i].getNumericValue(),
								args[i + 1].getNumericValue());
						}
						else
						{
							context.lineTo(args[i].getNumericValue(),
								args[i + 1].getNumericValue());
						}
					}
				}
				else
				{
					throw new MapyrusException("Wrong number of coordinate values at " +
						st.getFilenameAndLineNumber());
				}
				break;
				
			case Statement.CLEAR:
				if (nExpressions > 0)
				{
					throw new MapyrusException("Unexpected arguments at " +
						st.getFilenameAndLineNumber());
				}
				context.clearPath();
				break;

			case Statement.SLICEPATH:
				if (nExpressions == 2 && args[0].getType() == Argument.NUMERIC &&
					args[1].getType() == Argument.NUMERIC)
				{
					context.slicePath(args[0].getNumericValue(), args[1].getNumericValue());
				}
				else
				{
					throw new MapyrusException("Invalid path slice values at " +
						st.getFilenameAndLineNumber());
				}
				break;
				
			case Statement.STRIPEPATH:
				if (nExpressions == 2 && args[0].getType() == Argument.NUMERIC &&
					args[1].getType() == Argument.NUMERIC)
				{
					degrees = args[1].getNumericValue();
					context.stripePath(args[0].getNumericValue(), Math.toRadians(degrees));
				}
				else
				{
					throw new MapyrusException("Invalid path stripe values at " +
						st.getFilenameAndLineNumber());
				}
				break;

			case Statement.STROKE:
				if (nExpressions > 0)
				{
					throw new MapyrusException("Unexpected arguments at " +
						st.getFilenameAndLineNumber());
				}
				context.stroke();
				break;
				
			case Statement.FILL:
				if (nExpressions > 0)
				{
					throw new MapyrusException("Unexpected arguments at " +
						st.getFilenameAndLineNumber());
				}
				context.fill();
				break;
				
			case Statement.SCALE:
				if (nExpressions == 2 &&
					args[0].getType() == Argument.NUMERIC &&
					args[1].getType() == Argument.NUMERIC)
				{
					context.setScaling(args[0].getNumericValue(),
						args[1].getNumericValue());
				}
				else
				{
					throw new MapyrusException("Invalid scaling values at " +
						st.getFilenameAndLineNumber());
				}
				break;

			case Statement.ROTATE:
				if (nExpressions == 1 && args[0].getType() == Argument.NUMERIC)
				{
					degrees = args[0].getNumericValue();
					context.setRotation(Math.toRadians(degrees));
				}
				else
				{
					throw new MapyrusException("Invalid rotation value at " +
						st.getFilenameAndLineNumber());
				}
				break;
					
			case Statement.NEWPAGE:
				if (nExpressions == 3 &&
					args[0].getType() == Argument.STRING &&
					args[1].getType() == Argument.NUMERIC &&
					args[2].getType() == Argument.NUMERIC)
				{
					context.setOutputFormat(args[0].getStringValue(),
						(int)args[1].getNumericValue(),
						(int)args[2].getNumericValue(), "extras");
				}
				else
				{
					throw new MapyrusException("Invalid page values at " +
						st.getFilenameAndLineNumber());
				}
				break;	
							
			case Statement.PRINT:
				/*
				 * Print to stdout each of the expressions passed.
				 */
				for (int i = 0; i <nExpressions; i++)
				{
					if (args[i].getType() == Argument.STRING)
					{
						System.out.print(args[i].getStringValue());
					}
					else
					{
						System.out.print(args[i].getNumericValue());
					}
				}
				System.out.println("");
				break;
				
			case Statement.ASSIGN:
				context.defineVariable(st.getAssignedVariable(), args[0]);
				break;
		}		
	}

	/**
	 * Parse a statement name or variable name.
	 * @param c is first character of name.
	 * @param preprocessor is source to continue reading from.
	 * @return word parsed from preprocessor.
	 */
	private String parseWord(int c, Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		StringBuffer word = new StringBuffer();

		/*
		 * A statement or procedure name begins with a keyword
		 * which must begin with a letter.
		 */
		if (!Character.isLetter((char)c))
		{
			throw new MapyrusException("Invalid keyword at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		
		/*
		 * Read in whole word.
		 */
		do
		{
			word.append((char)c);
			c = preprocessor.read();
		}
		while (Character.isLetterOrDigit((char)c) || c == '.' || c == '_');

		/*
		 * Put back the character we read that is not part of the word.	
		 */	
		preprocessor.unread(c);
		return(word.toString());
	}

	/*
	 * Are we currently reading a comment?
	 */
	private boolean mInComment = false;

	/*
	 * Read next character, ignoring comments.
	 */
	private int readSkipComments(Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		int c;

		c = preprocessor.read();
		while (mInComment == true || c == COMMENT_CHAR)
		{
			if (c == COMMENT_CHAR)
			{
				/*
				 * Start of comment, skip characters until the end of the line.
				 */
				mInComment = true;
				c = preprocessor.read();
			}
			else if (c == '\n' || c == -1)
			{
				/*
				 * End of file or end of line is end of comment.
				 */
				mInComment = false;
			}
			else
			{
				/*
				 * Skip character in comment.
				 */
				c = preprocessor.read();
			}
		}
		return(c);
	}

	/**
	 * Reads, parses and returns next statement.
	 * @param preprocessor is source to read statement from.
	 * @param keyword is first token that has already been read.
	 * @return next statement read from file, or null if EOF was reached
	 * before a statement could be read.
	 */
	private Statement parseSimpleStatement(String keyword, Preprocessor preprocessor)
		throws MapyrusException, IOException
	{
		int state;
		Vector expressions = new Vector();
		Expression expr;
		Statement retval = null;
		boolean isAssignmentStatement = false;
		boolean finishedStatement = false;
		int c;

		state = AT_STATEMENT;
		c = readSkipComments(preprocessor);
		finishedStatement = false;
		while (!finishedStatement)
		{
			if (c == -1 || c == '\n')
			{
				/*
				 * End of line or end of file signifies end of statement.
				 */
				finishedStatement = true;
			}
			else if (Character.isWhitespace((char)c))
			{
				/*
				 * Ignore any whitespace.
				 */
				c = readSkipComments(preprocessor);
			}
			else if (state == AT_STATEMENT)
			{
				/*
				 * Is this an assignment statement of the form: var = value
				 */
				isAssignmentStatement = (c == '=');
				if (isAssignmentStatement)
					c = readSkipComments(preprocessor);
				state = AT_ARG;
			}
			else if (state == AT_ARG_SEPARATOR)
			{
				/*
				 * Expect a ',' between arguments and parameters to
				 * procedure block.
				 */ 
				if (c != ARGUMENT_SEPARATOR)
				{
					throw new MapyrusException("Expecting '" +
						ARGUMENT_SEPARATOR + "' at " +
						preprocessor.getCurrentFilenameAndLineNumber());
				}
				c = readSkipComments(preprocessor);
				state = AT_ARG;
			}
			else if (state == AT_ARG)
			{
					/*
					 * Parse an expression.
					 */
					preprocessor.unread(c);
					expr = new Expression(preprocessor);
					expressions.add(expr);

					state = AT_ARG_SEPARATOR;
					c = preprocessor.read();
			}
			else
			{
				/*
				 * Parsing is lost.  Don't know what is wrong.
				 */
				throw new MapyrusException("Error at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
		}

		/*
		 * Build a statement structure for what we just parsed.
		 */
		if (c == -1 && state == AT_STATEMENT)
		{
			/*
			 * Could not parse anything before we got EOF.
			 */
			retval = null;
		}
		else if (isAssignmentStatement)
		{
			/*
			 * Exactly one expression is assigned in a statement.
			 */
			if (expressions.size() > 1)
			{
				throw new MapyrusException("Too many expressions in assignment at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
			else if (expressions.size() == 0)
			{
				throw new MapyrusException("No expression in assignment at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}

			retval = new Statement(keyword, (Expression)expressions.elementAt(0));

			retval.setFilenameAndLineNumber(preprocessor.getCurrentFilename(),
					preprocessor.getCurrentLineNumber());
		}
		else
		{
			Expression []a = new Expression[expressions.size()];

			for (int i = 0; i < a.length; i++)
			{
				a[i] = (Expression)expressions.elementAt(i);
			}
			retval = new Statement(keyword, a);

			retval.setFilenameAndLineNumber(preprocessor.getCurrentFilename(),
					preprocessor.getCurrentLineNumber());
		}

		return(retval);
	}

	/**
	 * Parse paramters in a procedure block definition.
	 * Reads comma separated list of parameters
	 * @param preprocessor is source to read from.
	 * @return list of parameter names.
	 */
	private Vector parseParameters(Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		int c;
		Vector parameters = new Vector();
		int state;

		/*
		 * Read parameter names separated by ',' characters.
		 */
		state = AT_PARAM;
		c = readSkipComments(preprocessor);
		while (c != -1 && c != '\n')
		{
			if (Character.isWhitespace((char)c))
			{
				/*
				 * Ignore whitespace.
				 */
				c = readSkipComments(preprocessor);
			}
			else if (state == AT_PARAM)
			{
				/*
				 * Expect a parameter name.
				 */
				parameters.add(parseWord(c, preprocessor));
				state = AT_PARAM_SEPARATOR;
				c = readSkipComments(preprocessor);
			}
			else
			{
				/*
				 * Expect a ',' between parameter names.
				 */
				if (c != PARAM_SEPARATOR)
				{
					throw new MapyrusException("Expected '" + PARAM_SEPARATOR +
						"' at " + preprocessor.getCurrentFilenameAndLineNumber());
				}
				state = AT_PARAM;
				c = readSkipComments(preprocessor);
			}
		}
		return(parameters);
	}

	/**
	 * Reads and parses a procedure block, several statements
	 * grouped together between "begin" and "end" keywords.
	 * @param preprocessor is source to read from.
	 * @retval parsed procedure block as single statement.
	 */
	private ParsedStatement parseProcedureBlock(Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		String blockName;
		Vector parameters;
		Vector procedureStatements = new Vector();
		ParsedStatement st;
		Statement retval;
		boolean parsedEndKeyword = false;
		int c;

		/*
		 * Skip whitespace between "begin" and block name.
		 */		
		c = readSkipComments(preprocessor);
		while (Character.isWhitespace((char)c))
			c = readSkipComments(preprocessor);
		
		blockName = parseWord(c, preprocessor);
		parameters = parseParameters(preprocessor);

		/*
		 * Keep reading statements until we get matching "end"
		 * keyword.
		 */
		do
		{
			st = parseStatementOrKeyword(preprocessor, true);
			if (st == null)
			{
				/*
				 * Should not reach end of file inside a procedure block.
				 */
				throw new MapyrusException("Unexpected end of file at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}

			if (st.isStatement())
			{
				/*
				 * Accumulate statements for this procedure block.
				 */
				procedureStatements.add(st.getStatement());
			}
			else if (st.getKeywordType() == ParsedStatement.PARSED_END)
			{
				/*
				 * Found matching "end" keyword for this procedure block.
				 */
				parsedEndKeyword = true;
			}
			else
			{
				/*
				 * Found some other sort of control-flow keyword
				 * that we did not expect.
				 */
				throw new MapyrusException("Expected '" +
					END_KEYWORD + "' at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
		}
		while(!parsedEndKeyword);

		/*
		 * Return procedure block as a single statement.
		 */
		retval = new Statement(blockName, parameters, procedureStatements);
		return(new ParsedStatement(retval));
	}
	
	/**
	 * Reads and parses while loop statement.
	 * Parses test expression, "do" keyword, some
	 * statements, and then "done" keyword.
	 * @param preprocessor is source to read from.
	 * @return parsed loop as single statement.
	 */
	private ParsedStatement parseWhileStatement(Preprocessor preprocessor,
		boolean inProcedureDefn)
		throws IOException, MapyrusException
	{
		ParsedStatement st;
		Expression test;
		Vector loopStatements = new Vector();
		Statement statement;
		
		test = new Expression(preprocessor);
		
		/*
		 * Expect to parse "do" keyword.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside while loop.
			 */
			throw new MapyrusException("Unexpected end of file at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_DO)
		{
			throw new MapyrusException("Expected '" + DO_KEYWORD +
				"' at " + preprocessor.getCurrentFilenameAndLineNumber());
		}
		
		/*
		 * Now we want some statements to execute each time through the loop.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside loop.
			 */
			throw new MapyrusException("Unexpected end of file at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		while (st.isStatement())
		{
			loopStatements.add(st.getStatement());
			st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
			if (st == null)
			{
				/*
			 	* Should not reach end of file inside loop
			 	*/
				throw new MapyrusException("Unexpected end of file at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
		}
		
		/*
		 * Expect "done" after statements.
		 */
		if (st.getKeywordType() != ParsedStatement.PARSED_DONE)
		{
			throw new MapyrusException("Expected '" + DONE_KEYWORD + "' at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}

		statement = new Statement(test, loopStatements);
		return(new ParsedStatement(statement));		 
	}
	
	/**
	 * Reads and parses conditional statement.
	 * Parses test expression, "then" keyword, some
	 * statements, an "else" keyword, some statements and
	 * "endif" keyword.
	 * @param preprocessor is source to read from.
	 * @return parsed if block as single statement.
	 */
	private ParsedStatement parseIfStatement(Preprocessor preprocessor,
		boolean inProcedureDefn)
		throws IOException, MapyrusException
	{
		ParsedStatement st;
		Expression test;
		Vector thenStatements = new Vector();
		Vector elseStatements = new Vector();
		Statement statement;
		boolean checkForEndif = true;	/* do we need to check for "endif" keyword at end of statement? */

		test = new Expression(preprocessor);

		/*
		 * Expect to parse "then" keyword.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside if statement.
			 */
			throw new MapyrusException("Unexpected end of file at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_THEN)
		{
			throw new MapyrusException("Expected '" + THEN_KEYWORD +
				"' at " + preprocessor.getCurrentFilenameAndLineNumber());
		}

		/*
		 * Now we want some statements for when the expression is true.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside if statement.
			 */
			throw new MapyrusException("Unexpected end of file at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		while (st.isStatement())
		{
			thenStatements.add(st.getStatement());
			st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
			if (st == null)
			{
				/*
			 	* Should not reach end of file inside if statement.
			 	*/
				throw new MapyrusException("Unexpected end of file at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
		}

		/*
		 * There may be an "else" part to the statement too.
		 */
		if (st.getKeywordType() == ParsedStatement.PARSED_ELSE)
		{
			/*
			 * Now get the statements for when the expression is false.
			 */
			st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
			if (st == null)
			{
				/*
			 	* Should not reach end of file inside if statement.
			 	*/
				throw new MapyrusException("Unexpected end of file at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
			while (st.isStatement())
			{
				elseStatements.add(st.getStatement());
				st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
				if (st == null)
				{
					/*
			 		 * Should not reach end of file inside if statement.
			 		 */
					throw new MapyrusException("Unexpected end of file at " +
						preprocessor.getCurrentFilenameAndLineNumber());
				}
			}
		}
		else if (st.getKeywordType() == ParsedStatement.PARSED_ELSIF)
		{
			/*
			 * Parse "elsif" block as a single, separate "if" statement
			 * that is part of the "else" case.
			 */
			st = parseIfStatement(preprocessor, inProcedureDefn);
			if (!st.isStatement())
			{
				throw new MapyrusException("Expecting '" + ENDIF_KEYWORD + "' at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
			elseStatements.add(st.getStatement());
			checkForEndif = false;
		}

		/*
		 * Expect "endif" after statements.
		 */
		if (checkForEndif && st.getKeywordType() != ParsedStatement.PARSED_ENDIF)
		{
			throw new MapyrusException("Expected '" + ENDIF_KEYWORD + "' at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}

		statement = new Statement(test, thenStatements, elseStatements);
		return(new ParsedStatement(statement));
	}

	/*
	 * Static keyword lookup table for fast keyword lookup.
	 */
	private static Hashtable mKeywordLookup;

	static
	{
		mKeywordLookup = new Hashtable();
		mKeywordLookup.put(END_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_END));
		mKeywordLookup.put(THEN_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_THEN));
		mKeywordLookup.put(ELSE_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ELSE));
		mKeywordLookup.put(ELSIF_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ELSIF));
		mKeywordLookup.put(ENDIF_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ENDIF));
		mKeywordLookup.put(DO_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_DO));
		mKeywordLookup.put(DONE_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_DONE));						
	}

	/**
	 * Reads, parses and returns next statement, or block of statements.
	 * @param preprocessor source to read from.
	 * @param inProcedureDefn true if currently parsing inside an
	 * procedure block.
	 * @return next statement read from file, or null if EOF was reached
	 * before a statement could be read.
	 */
	private ParsedStatement parseStatementOrKeyword(Preprocessor preprocessor,
		boolean inProcedureDefn)
		throws MapyrusException, IOException
	{
		int c;
		ParsedStatement retval = null;
		Statement statement;
		Vector procedureStatements = null;
		int state;
		boolean finishedStatement = false;

		state = AT_STATEMENT;
		c = readSkipComments(preprocessor);
		finishedStatement = false;
		while (!finishedStatement)
		{
			if (c == -1)
			{
				/*
				 * Reached EOF.
				 */
				finishedStatement = true;
				break;
			}
			else if (Character.isWhitespace((char)c))
			{
				/*
				 * Skip whitespace
				 */
				c = readSkipComments(preprocessor);
			}
			else
			{
				String keyword = parseWord(c, preprocessor);
				String lower = keyword.toLowerCase();

				/*
				 * Is this the start or end of a procedure block definition?
				 */
				if (lower.equals(BEGIN_KEYWORD))
				{
					/*
					 * Nested procedure blocks not allowed.
					 */
					if (inProcedureDefn)
					{
						throw new MapyrusException("Procedure block " +
							"definition inside procedure block at " +
							preprocessor.getCurrentFilenameAndLineNumber());
					}
					retval = parseProcedureBlock(preprocessor);
				}
				else if (lower.equals(IF_KEYWORD))
				{
					retval = parseIfStatement(preprocessor, inProcedureDefn);
				}
				else if (lower.equals(WHILE_KEYWORD))
				{
					retval = parseWhileStatement(preprocessor, inProcedureDefn);
				}
				else
				{
					/*
					 * Does keyword match a control-flow keyword?
				 	 * like "then", or "else"?
					 */
					retval = (ParsedStatement)mKeywordLookup.get(lower);
					if (retval == null)
					{
						/*
						 * It must be a regular type of statement if we
						 * can't match any special words.
						 */
						Statement st = parseSimpleStatement(keyword, preprocessor);
						retval = new ParsedStatement(st);
					}
				}
				finishedStatement = true;
			}
		}

		return(retval);
	}

	/*
	 * Reads and parses a single statement.
	 * @param preprocessor is source to read statement from.
	 * @return next statement read and parsed.
	 */
	private Statement parseStatement(Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		ParsedStatement st = parseStatementOrKeyword(preprocessor, false);
		if (st == null)
		{
			return(null);
		}
		else if (!st.isStatement())
		{
			throw new MapyrusException("Invalid keyword at " +
				preprocessor.getCurrentFilenameAndLineNumber());
		}
		return(st.getStatement());
	}

	/**
	 * Reads and parses commands from file and executes them.
	 * @param f is open file or URL to read from.
	 * @param filename is name of file or URL (for use in error messages).
	 */
	public void interpret(Reader f, String filename)
		throws IOException, MapyrusException
	{
		Statement st;
		Preprocessor preprocessor = new Preprocessor(f, filename);
		mInComment = false;
		
		/*
		 * Keep parsing until we get EOF.
		 */
		while ((st = parseStatement(preprocessor)) != null)
		{
			executeStatement(st);
		}
	}

	private void makeCall(Statement block, Vector parameters, Argument []args)
		throws IOException, MapyrusException
	{
		Statement statement;

		for (int i = 0; i < args.length; i++)
		{
			mContext.defineVariable((String)parameters.elementAt(i), args[i]);
		}

		/*
		 * Execute each of the statements in the procedure block.
		 */
		Vector v = block.getStatementBlock();
		for (int i = 0; i < v.size(); i++)
		{
			statement = (Statement)v.elementAt(i);
			executeStatement(statement);
		}
	}

	/**
	 * Recursive function for executing statements.
	 * @param preprocessor is source to read statements from.
	 */
	private void executeStatement(Statement statement)
		throws IOException, MapyrusException
	{
		Argument []args;

		/*
		 * Store procedure blocks away for later execution,
		 * execute any other statements immediately.
		 */
		if (statement.getType() == Statement.BLOCK)
		{
			mStatementBlocks.put(statement.getBlockName(), statement);
		}
		else if (statement.getType() == Statement.CONDITIONAL)
		{
			/*
			 * Execute correct part of if statement depending on value of expression.
			 */
			Expression []expr = statement.getExpressions();
			Argument test = expr[0].evaluate(mContext);
			Vector v;
			
			if (test.getType() != Argument.NUMERIC)
			{
				throw new MapyrusException("Invalid expression at " +
					statement.getFilenameAndLineNumber());
			}
			
			if (test.getNumericValue() != 0.0)
				v = statement.getThenStatements();
			else
				v = statement.getElseStatements();

			if (v != null)
			{			
				/*
				 * Execute each of the statements.
				 */	
				for (int i = 0; i < v.size(); i++)
				{
					statement = (Statement)v.elementAt(i);
					executeStatement(statement);
				}
			}
		}
		else if (statement.getType() == Statement.LOOP)
		{
			/*
			 * Find expression to test and loop statements to execute.
			 */
			Expression []expr = statement.getExpressions();
			
			Vector v = statement.getLoopStatements();
			Argument test = expr[0].evaluate(mContext);
			
			if (test.getType() != Argument.NUMERIC)
			{
				throw new MapyrusException("Invalid expression at " +
					statement.getFilenameAndLineNumber());
			}
			
			/*
			 * Execute loop while expression remains true.
			 */			
			while (test.getNumericValue() != 0.0)
			{
				/*
				 * Execute each of the statements.
				 */	
				for (int i = 0; i < v.size(); i++)
				{
					statement = (Statement)v.elementAt(i);
					executeStatement(statement);
				}
				
				test = expr[0].evaluate(mContext);
				if (test.getType() != Argument.NUMERIC)
				{
					throw new MapyrusException("Invalid expression at " +
						statement.getFilenameAndLineNumber());
				}
			}
		}
		else if (statement.getType() == Statement.CALL)
		{
			/*
			 * Find the statements for the procedure block we are calling.
			 */
			Statement block =
				(Statement)mStatementBlocks.get(statement.getBlockName());
			if (block == null)
			{
				throw new MapyrusException("Procedure '" +
					statement.getBlockName() + "' not defined at " +
					statement.getFilenameAndLineNumber());
			}
			
			/*
			 * Check that correct number of parameters are being passed.
			 */
			Vector formalParameters = block.getBlockParameters();
			Expression []actualParameters = statement.getExpressions();
			if (actualParameters.length != formalParameters.size())
			{
				throw new MapyrusException("Wrong number of parameters " +
					"in call to " + statement.getBlockName() +
					" at " + statement.getFilenameAndLineNumber());
			}

			try
			{
				/*
				 * Save state and set parameters passed to the procedure.
				 */
				args = new Argument[actualParameters.length];
				for (int i = 0; i < args.length; i++)
				{
					args[i] = actualParameters[i].evaluate(mContext);
				}
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(e.getMessage() + " at " +
					statement.getFilenameAndLineNumber());
			}

			/*
			 * If one or more "move" points are defined without
			 * any lines then call the procedure block repeatedly
			 * with the origin transformed to each of move points
			 * in turn.
			 */
			int moveToCount = mContext.getMoveToCount();
			int lineToCount = mContext.getLineToCount();
			if (moveToCount > 0 && lineToCount == 0)
			{
				/*
				 * Step through path, setting origin and rotation for each
				 * point and then calling procedure block.
				 */
				float coords[];
				Vector moveTos = mContext.getMoveTos();
				
				for (int i = 0; i < moveToCount; i++)
				{
					mContext.saveState();
					coords = (float [])moveTos.elementAt(i);
					mContext.setTranslation(coords[0], coords[1]);
					mContext.setRotation(coords[2]);
					makeCall(block, formalParameters, args);
					mContext.restoreState();
				}
			}
			else
			{
				/*
				 * Execute statements in procedure block.  Surround statments
				 * with a save/restore so nothing can be changed by accident.
				 */
				mContext.saveState();
				makeCall(block, formalParameters, args);
				mContext.restoreState();
			}
		}
		else
		{
			execute(statement, mContext);
		}
	}

	/**
	 * Create new language interpreter.
	 * @param context is the context to use during interpretation.
	 * This may be in a changed state by the time interpretation
	 * is finished.
	 */
	public Interpreter(ContextStack context)
	{
		mContext = context;
		mStatementBlocks = new Hashtable();
	}
}
