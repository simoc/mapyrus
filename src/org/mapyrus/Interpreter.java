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
	 * Characters starting a comment on a line.
	 * Tokens around definition of a procedure.
	 */
	private static final int COMMENT_CHAR = '#';
	private static final String BEGIN_BLOCK = "begin";
	private static final String END_BLOCK = "end";
	/*
	 * States during expression parsing.
	 */
	private static final int AT_START = 1;
	private static final int AT_START_ARGS = 2;
	private static final int IN_ARGS = 3;
	private static final int IN_COMMENT = 4;
	private static final int AT_BLOCK_NAME = 5;

	private Context mContext;
	
	/*
	 * Blocks of statements for each procedure defined in
	 * this interpreter.
	 */
	private Hashtable mStatementBlocks;
	
	/**
	 * Reads, parses and returns next statement.
	 * @return next statement read from file, or null if EOF was reached
	 * before a statement could be read.
	 */
	private Statement parseStatement(Preprocessor preprocessor)
		throws MapyrusException, IOException
	{
		int c;
		int state = AT_START;
		StringBuffer keyword = new StringBuffer();
		Vector expressions = new Vector();
		Expression expr;
		Statement retval = null;
		Vector procedureStatements = null;
		Expression []procedureParameters = null;
		boolean isAssignmentStatement = false;
		boolean isProcedureStatement = false;
		boolean inProcedureBlock = false;
		boolean atEOF = false;

		do
		{
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
					state = IN_COMMENT;
					c = preprocessor.read();
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
					c = preprocessor.read();
				}
				else if (state == IN_COMMENT)
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
				else if (state == AT_START || state == AT_BLOCK_NAME)
				{
					/*
					 * A statement or procedure name begins with a keyword
					 * which must begin with a letter.
					 */
					if (!Character.isLetter((char)c))
					{
						throw new MapyrusException("Invalid statement at " +
							preprocessor.getCurrentFilenameAndLine());
					}
	
					/*
					 * Read in whole keyword.
					 */
					do
					{
						keyword.append((char)c);
						c = preprocessor.read();
					}
					while (Character.isLetterOrDigit((char)c) || c == '.' || c == '_');
					
					/*
					 * Is this a subroutine definition?
					 */
					if (state == AT_START && keyword.toString().equalsIgnoreCase(BEGIN_BLOCK))
					{
						state = AT_BLOCK_NAME;
						isProcedureStatement = true;
					}
					else
					{
						state = AT_START_ARGS;
					}
				}
				else if (state == AT_START_ARGS)
				{
					/*
					 * Is this an assignment statement or
					 * some other kind of statement?
					 */
					isAssignmentStatement = (!isProcedureStatement && c == '=');
					if (isAssignmentStatement)
					{
						c = preprocessor.read();
					}
					else
					{
						preprocessor.unread(c);
					}
					state = IN_ARGS;
				}
				
				if (state == IN_ARGS)
				{
					/*
					 * Parse an expression.
					 */
					expr = new Expression(preprocessor);
					expressions.add(expr);
					/*
					 * After an expression we expect a ',' and then another expression,
					 * or a newline.
					 */
					c = preprocessor.read();
					if (c != '\n' && c != ',' && c!= COMMENT_CHAR)
					{
						throw new MapyrusException("Expecting ',' between expressions at " +
							preprocessor.getCurrentFilenameAndLine());
					}
				}
			}
	
			/*
			 * Build a statement structure for what we just parsed.
			 */
			if (atEOF && (state == AT_START || state == IN_COMMENT))
			{
				if (inProcedureBlock)
				{
					throw new MapyrusException("Unfinished procedure " +
						keyword.toString() + " at " +
						preprocessor.getCurrentFilenameAndLine());
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
					preprocessor.getCurrentFilenameAndLine());
			}
			else if (isAssignmentStatement)
			{
				if (expressions.size() > 1)
				{
					throw new MapyrusException("Too many expressions in assignment at " +
						preprocessor.getCurrentFilenameAndLine());
				}
				else if (expressions.size() == 0)
				{
					throw new MapyrusException("No expression in assignment at " +
						preprocessor.getCurrentFilenameAndLine());
				}
				retval = new Statement(keyword.toString(),
					(Expression)expressions.elementAt(0));
			}
			else if (keyword.toString().equalsIgnoreCase(END_BLOCK))
			{
				/*
				 * Finish procedure currently being defined.
				 */
				if (!inProcedureBlock)
				{
					throw new MapyrusException("Unexpected end of procedure at " +
						preprocessor.getCurrentFilenameAndLine());
				}
				inProcedureBlock = false;
				
				/*
				 * Create single statement for all statements making
				 * up the procedure.
				 */
				retval = new Statement(keyword.toString(),
					procedureParameters,
					procedureStatements);
			}
			else
			{
				int statementType;
				Expression []a = new Expression[expressions.size()];
				Statement statement;
				
				for (int i = 0; i < a.length; i++)
				{
					a[i] = (Expression)expressions.elementAt(i);
				}

				if (isProcedureStatement)
				{
					/*
					 * Nested procedures are not allowed.
					 */
					if (inProcedureBlock)
					{
						throw new MapyrusException("Procedure definition within " +
							"existing procedure at " +
							preprocessor.getCurrentFilenameAndLine());
					}
					inProcedureBlock = true;
					procedureStatements = new Vector();
					procedureParameters = a;
				}
				else
				{
					statementType = Statement.getStatementType(keyword.toString());
		
					if (statementType < 0)
					{
						throw new MapyrusException("Keyword " + keyword +
							" not recognized " + preprocessor.getCurrentFilenameAndLine());
					}
				
					statement = new Statement(statementType, a);

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
		}
		while (inProcedureBlock);
		
		return(retval);
	}

	/*
	 * Exceute a single statement, changing the path, context or generating
	 * some output.
	 */
	private void execute(Statement st, Context context)
		throws MapyrusException, IOException
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
					context.moveTo(args[0].getNumericValue(),
						args[1].getNumericValue());
				}
				break;
				
			case Statement.DRAW:
				if (nExpressions == 2 && args[0].getType() == Argument.NUMERIC &&
					args[1].getType() == Argument.NUMERIC)
				{
					/*
					 * Add point to path.
					 */
					context.lineTo(args[0].getNumericValue(),
						args[1].getNumericValue());
				}
				break;
				
			case Statement.STROKE:
				context.stroke();
				break;
				
			case Statement.FILL:
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
	 */
	public void interpret(Reader f)
		throws IOException, MapyrusException
	{
		Statement st;
		Preprocessor preprocessor = new Preprocessor(f);
				
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
	public Interpreter(Context context)
	{
		super();
		mContext = context;
		mStatementBlocks = new Hashtable();
	}
}
