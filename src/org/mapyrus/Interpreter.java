/**
 * Language interpreter.  Parse and executes commands read from file, or
 * typed by user.
 * 
 * May be called repeatedly to interpret several files in the same context.
 */

/*
 * $Id$
 */

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.awt.Color;

public class Interpreter
{
	/*
	 * Character starting a comment on a line.
	 * Character separating arguments to a statement.
	 * Tokens around definition of a procedure.
	 */
	private static final char COMMENT_CHAR = '#';
	private static final char ARGUMENT_SEPARATOR = ',';
	private static final String BEGIN_BLOCK = "begin";
	private static final String END_BLOCK = "end";
	
	/*
	 * States during expression parsing.
	 */
	private static final int AT_START = 1;		/* at start of a statement */
	private static final int AT_START_ARGS = 2;	/* got statement name, now expecting arguments */
	private static final int AT_ARG = 3;		/* at argument to a statement */
	private static final int AT_ARG_SEPARATOR = 4;	/* at separator between arguments */
	private static final int AT_BLOCK_NAME = 6;	/* expecting procedure block name */
	private static final int AT_BLOCK_PARAM = 7;	/* at parameter to a procedure block */
	private static final int AT_PARAM_SEPARATOR = 8;	/* at separator between parameters */

	private ContextStack mContext;
	
	/*
	 * Blocks of statements for each procedure defined in
	 * this interpreter.
	 */
	private Hashtable mStatementBlocks;

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
	
	/**
	 * Reads, parses and returns next statement.
	 * @return next statement read from file, or null if EOF was reached
	 * before a statement could be read.
	 */
	private Statement parseStatement(Preprocessor preprocessor)
		throws MapyrusException, IOException
	{
		int c;
		int state;
		String keyword = null;
		String blockName = null;
		Vector expressions = null;
		Expression expr;
		Statement retval = null;
		Vector procedureStatements = null;
		Vector procedureParameters = null;
		boolean isAssignmentStatement = false;
		boolean inProcedureBlock = false;
		boolean inComment;
		boolean atEOF = false;

		do
		{
			inComment = false;
			state = AT_START;
			c = preprocessor.read();
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
					inComment = true;
					c = preprocessor.read();
				}
				else if (c == '\n')
				{
					if (keyword != null && keyword.length() > 0)
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
						inComment = false;
					}
					c = preprocessor.read();
				}
				else if (inComment)
				{
					/*
					 * Do nothing -- we are reading a comment.
					 */
					c = preprocessor.read();
				}
				else if (Character.isWhitespace((char)c))
				{
					/*
					 * Skip whitespace
					 */
					c = preprocessor.read();
				}
				else if (state == AT_ARG_SEPARATOR)
				{
					/*
					 * Skip separator between arguments.
					 */
					if (c != ARGUMENT_SEPARATOR)
					{
						throw new MapyrusException("Expecting '" +
							ARGUMENT_SEPARATOR + "' at " +
							preprocessor.getCurrentFilenameAndLineNumber());
					}
					state = AT_ARG;
					c = preprocessor.read();
				}
				else if (state == AT_PARAM_SEPARATOR)
				{
					/*
					 * Skip separator between parameters.
					 */
					if (c != ARGUMENT_SEPARATOR)
					{
						throw new MapyrusException("Expecting '" +
							ARGUMENT_SEPARATOR + "' at " +
							preprocessor.getCurrentFilenameAndLineNumber());
					}
					state = AT_BLOCK_PARAM;
					c = preprocessor.read();
				}
				else if (state == AT_START || state == AT_BLOCK_NAME)
				{
					keyword = parseWord(c, preprocessor);
					c = preprocessor.read();
					
					/*
					 * Is this a procedure block definition?
					 */
					if (state == AT_START && keyword.equalsIgnoreCase(BEGIN_BLOCK))
					{
						state = AT_BLOCK_NAME;
						procedureParameters = new Vector();
					}
					else if (state == AT_BLOCK_NAME)
					{
						/*
						 * After parsing procedure block name we parse parameters for
						 * this procedure block definition.
						 */
						state = AT_BLOCK_PARAM;
					}
					else
					{
						/*
						 * After parsing statement we parse arguments to statement.
						 */
						state = AT_START_ARGS;
						expressions = new Vector();
					}
				}
				else if (state == AT_START_ARGS)
				{
					/*
					 * Is this an assignment statement or
					 * some other kind of statement?
					 */
					isAssignmentStatement = (c == '=');
					if (isAssignmentStatement)
					{
						c = preprocessor.read();
					}
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
				else if (state == AT_BLOCK_PARAM)
				{
					/*
					 * Parse a parameter name for procedure block.
					 */
					procedureParameters.add(parseWord(c, preprocessor));
					state = AT_PARAM_SEPARATOR;
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
			if (atEOF && state == AT_START)
			{
				if (inProcedureBlock)
				{
					throw new MapyrusException("Unfinished procedure " +
						blockName + " at " +
						preprocessor.getCurrentFilenameAndLineNumber());
				}

				/*
				 * Could not parse anything before we got EOF.
				 */
				return(null);
			}
			else if (state == AT_BLOCK_NAME)
			{
				/*
				 * Began a procedure block but didn't find its name.
				 */
				throw new MapyrusException("No procedure name given at " +
					preprocessor.getCurrentFilenameAndLineNumber());
			}
			else if (keyword.equalsIgnoreCase(END_BLOCK))
			{
				/*
				 * Finish procedure currently being defined.
				 */
				if (!inProcedureBlock)
				{
					throw new MapyrusException("Unexpected end of procedure at " +
						preprocessor.getCurrentFilenameAndLineNumber());
				}
				inProcedureBlock = false;
				
				/*
				 * Create single statement for all statements making
				 * up the procedure.
				 */
				retval = new Statement(blockName,
					procedureParameters,
					procedureStatements);
					
				retval.setFilenameAndLineNumber(preprocessor.getCurrentFilename(),
					preprocessor.getCurrentLineNumber());
			}
			else if (state == AT_PARAM_SEPARATOR || state == AT_BLOCK_PARAM)
			{
				/*
				 * Nested procedures are not allowed.
				 */
				if (inProcedureBlock)
				{
					throw new MapyrusException("Procedure definition within " +
						"existing procedure definition of " + blockName + " at " +
						preprocessor.getCurrentFilenameAndLineNumber());
				}
				inProcedureBlock = true;
				procedureStatements = new Vector();
				blockName = keyword;

			}
			else
			{
				Statement statement;

				if (isAssignmentStatement)
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

					statement = new Statement(keyword, (Expression)expressions.elementAt(0));
				}
				else
				{	
					Expression []a = new Expression[expressions.size()];
					
					for (int i = 0; i < a.length; i++)
					{
						a[i] = (Expression)expressions.elementAt(i);
					}
					statement = new Statement(keyword, a);
				}
				
				statement.setFilenameAndLineNumber(preprocessor.getCurrentFilename(),
					preprocessor.getCurrentLineNumber());
				
				/*
				 * Add statement to the procedure we are defining, or
				 * return it for immediate execution if we are not defining
				 * a procedure.
				 */			
				if (inProcedureBlock)
				{
					procedureStatements.add(statement);
				}
				else
				{
					retval = statement;
				}
			}
		}
		while (inProcedureBlock);
		
		return(retval);
	}

	/*
	 * Exceute a single statement, changing the path, context or generating
	 * some output.
	 */
	private void execute(Statement st, ContextStack context)
		throws MapyrusException, IOException
	{
		Expression []expr;
		int nExpressions;
		int type;
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
				System.out.println("");
				break;
				
			case Statement.ASSIGN:
				context.defineVariable(st.getAssignedVariable(), args[0]);
				break;
		}		
	}
		
		
	/**
	 * Reads commands from file and interprets them.
	 * @param f is open file or URL to read from.
	 * @param filename is name of file or URL (for use in error messages).
	 */
	public void interpret(Reader f, String filename)
		throws IOException, MapyrusException
	{
		Statement st;
		Preprocessor preprocessor = new Preprocessor(f, filename);
				
		/*
		 * Keep parsing until we get EOF.
		 */
		while ((st = parseStatement(preprocessor)) != null)
		{
			/*
			 * Store procedure blocks away for later execution,
			 * execute any other statements immediately.
			 */
			if (st.getType() == Statement.BLOCK)
			{
				mStatementBlocks.put(st.getBlockName(), st);
			}
			else if (st.getType() == Statement.CALL)
			{
				/*
				 * Find the statements for the procedure block we are calling.
				 */
				Statement block = (Statement)mStatementBlocks.get(st.getBlockName());
				if (block == null)
				{
					throw new MapyrusException("Procedure not defined: " +
						st.getBlockName());
				}
				
				/*
				 * Check that correct number of parameters are being passed.
				 */
				Vector formalParameters = block.getBlockParameters();
				Expression []actualParameters = st.getExpressions();
				if (actualParameters.length != formalParameters.size())
				{
					throw new MapyrusException("Wrong number of parameters " +
						"in call to " + st.getBlockName());
				}

				/*
				 * Save state and set parameters passed to the procedure.
				 */
				Argument []args = new Argument[actualParameters.length];
				for (int i = 0; i < args.length; i++)
					args[i] = actualParameters[i].evaluate(mContext);
				
				mContext.saveState();
				for (int i = 0; i < args.length; i++)
				{
					mContext.defineVariable((String)formalParameters.elementAt(i), args[i]);
				}
				 				
				/*
				 * Execute each of the statements in the procedure block.
				 */
				Vector v = block.getStatementBlock();
				for (int i = 0; i < v.size(); i++)
				{
					st = (Statement)v.elementAt(i);
					execute(st, mContext);
				}
				
				mContext.restoreState();
			}
			else
			{
				execute(st, mContext);
			}
		}
	}

	/**
	 * Create new language interpreter.
	 * @param context is the context to use during interpretation.  This may be in
	 * a changed state by the time interpretation is finished.
	 */
	public Interpreter(ContextStack context)
	{
		super();
		mContext = context;
		mStatementBlocks = new Hashtable();
	}
}
