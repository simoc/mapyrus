/**
 * An expression tree.  Parser for numeric or string expression that builds
 * a binary tree holding the expression.  The expression can be later 
 * be evaluated and the tree is evaluated to a single value.
 *
 */

/*
 * $Id$
 */
import java.lang.String;
import java.util.Hashtable;
import java.io.*;

public class Expression
{
	/*
	 * Types of operations allowed between two numbers or strings
	 * (or two expressions).
	 */
	private static final int PLUS_OPERATION = 1;
	private static final int MINUS_OPERATION = 2;
	private static final int MULTIPLY_OPERATION = 3;	/* 'qw' * 2 = 'qwqw' */
	private static final int DIVIDE_OPERATION = 4;

	private static final int CONTAINS_OPERATION = 6; /* 'foo' ~ 'o' = 1 */
	private static final int EQUALS_OPERATION = 7;
	private static final int GREATER_THAN_OPERATION = 8;
	private static final int LESS_THAN_OPERATION = 9;

	private static final int AND_OPERATION = 10;
	private static final int OR_OPERATION = 11;
	
	/*
	 * Nodes in binary tree describing an arithmetic expression.
	 */
	private class ExpressionTreeNode
	{
		boolean mIsLeaf;
		Argument mLeafArg;

		int mOperation;
		ExpressionTreeNode mLeftBranch, mRightBranch;

		/*
		 * Create a leaf value containing either a number,
		 * string or variable name.
		 */
		public ExpressionTreeNode(Argument arg)
		{
			mIsLeaf = true;
			mLeafArg = arg;
		}
	
		/*
		 * Create a node joining two sub-expressions with an
		 * operation between them.
		 */
		public ExpressionTreeNode(ExpressionTreeNode left,
			int operation,
			ExpressionTreeNode right)
		{
			mIsLeaf = false;			
			mLeftBranch = left;
			mRightBranch = right;
			mOperation = operation;
		}

		/**
		 * Evaluate binary tree expression..
		 * @param context variable definitions and other context information.
		 * @return numeric or string value of the expression.
		 */
		public Argument evaluate(ContextStack context) throws MapyrusException
		{
			return(traverse(this, context));
		}

		/*
		 * Recursively traverse binary expression tree to
		 * determine its value.
		 */
		private Argument traverse(ExpressionTreeNode t, ContextStack context)
			throws MapyrusException
		{
			Argument retval;
			Argument leftValue, rightValue;
			int op;
			double d;

			if (t.mIsLeaf)
			{
				/*
				 * Evaluate any variable name.
				 */
				if (t.mLeafArg.getType() == Argument.VARIABLE)
				{
					retval = context.getVariableValue(t.mLeafArg.getVariableName());
					if (retval == null)
					{
						throw new MapyrusException("Variable " +
							t.mLeafArg.getVariableName() + " not defined");
					}
				}
				else
				{
					retval = t.mLeafArg;
				}
			}
			else
			{
				/*
				 * Either expressions can be numeric or string.
				 */
				leftValue = traverse(t.mLeftBranch, context);
				rightValue = traverse(t.mRightBranch, context);

				/*
				 * Evaluate any variable names.
				 */
				if (leftValue.getType() == Argument.VARIABLE)
				{
					Argument leftVarValue = context.getVariableValue(leftValue.getVariableName());
					if (leftVarValue == null)
					{
						throw new MapyrusException("Variable " +
							leftValue.getVariableName() + " not defined");
					}
					leftValue = leftVarValue;
				}
				if (rightValue.getType() == Argument.VARIABLE)
				{
					Argument rightVarValue = context.getVariableValue(rightValue.getVariableName());
					if (rightVarValue == null)
					{
						throw new MapyrusException("Variable " +
							rightValue.getVariableName() + " not defined");
					}
					rightValue = rightVarValue;
				}

				/*
				 * Check types for operation.  Multiplying a string by a number is OK
				 * but everything else requires matching types.
				 */
				op = t.mOperation;
				if (op == MULTIPLY_OPERATION && leftValue.getType() == Argument.STRING)
				{
					if (rightValue.getType() != Argument.NUMERIC)
					{
						throw new MapyrusException("Wrong types for mulitplication");
					}
				}
				else if (leftValue.getType() != rightValue.getType())
				{
					throw new MapyrusException("Types do match in expression");
				}
				
				/*
				 * Do string and numeric operations separately.
				 */
				if (leftValue.getType() == Argument.NUMERIC)
				{
					double l = leftValue.getNumericValue();
					double r = rightValue.getNumericValue();
					
					if (op == PLUS_OPERATION)
						d = l + r;
					else if (op == MINUS_OPERATION)
						d = l - r;
					else if (op == MULTIPLY_OPERATION)
						d = l * r;
					else if (op == DIVIDE_OPERATION)
						d = l / r;
					else if (op == GREATER_THAN_OPERATION)
						d = (l > r) ? 1 : 0;
					else if (op == LESS_THAN_OPERATION)
						d = (l < r) ? 1 : 0;
					else if (op == EQUALS_OPERATION)
						d = (l == r) ? 1 : 0;
					else if (op == AND_OPERATION)
					{
						d = (l != 0.0 &&
							r != 0.0) ? 1 : 0;
					}
					else if (op == OR_OPERATION)
					{
						d = (l != 0.0 ||
							r != 0.0) ? 1 : 0;
					}
					else
					{
						throw new MapyrusException("Operation not permitted between numbers");
					}
					retval = new Argument(d);
				}
				else
				{
					String s;
					String l = leftValue.getStringValue();
					if (op == MULTIPLY_OPERATION)
					{
						/*
						 * Repeat string N times.
						 */
						s = new String();
						for (int i = 0; i < rightValue.getNumericValue(); i++)
						{
							s = s + l;
						}
						retval = new Argument(Argument.STRING, s);
					}
					else if (op == PLUS_OPERATION)
					{
						String r = rightValue.getStringValue();
						retval = new Argument(Argument.STRING, l + r);
					}
					else
					{
						String r = rightValue.getStringValue();
						if (op == CONTAINS_OPERATION)
							d = (l.indexOf(r) > 0) ? 1 : 0;
						else if (op == GREATER_THAN_OPERATION)
							d = (l.compareTo(r) > 0) ? 1 : 0;
						else if (op == LESS_THAN_OPERATION)
							d = (l.compareTo(r) < 0) ? 1 : 0;
						else if (op == EQUALS_OPERATION)
							d = l.equals(r) ? 1 : 0;
						else
						{
							throw new MapyrusException("Operation not permitted between strings");
						}
						retval = new Argument(d);
					}
				}
			}
			return(retval);
		}
	}

	ExpressionTreeNode mExprTree;

	/*
	 * Parse a conditional expression.
	 */
	private ExpressionTreeNode parseCondition(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, cond;
		int op;

		cond = parseExpression(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '~' || op == '<' || op == '=' || op == '>')
			{
				int opType;

				expr = parseExpression(p);
				
				if (op == '~')
					opType = CONTAINS_OPERATION;
				else if (op == '<')
					opType = LESS_THAN_OPERATION;
				else if (op == '=')
					opType = EQUALS_OPERATION;
				else
					opType = GREATER_THAN_OPERATION;

				cond = new ExpressionTreeNode(cond, opType, expr);
			}
			else				
			{
				p.unread(op);
				break;
			}
		}
		return(cond);
	}
	
	/*
	 * Parse expression.
	 */
	private ExpressionTreeNode parseExpression(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, term;
		int op;

		expr = parseTerm(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '+' || op == '-')
			{
				term = parseTerm(p);
				int opType = (op == '+') ? PLUS_OPERATION : MINUS_OPERATION;

				expr = new ExpressionTreeNode(expr, opType, term);
			}
			else				
			{
				p.unread(op);
				break;
			}
		}
		return(expr);
	}

	/*
	 * Parse term.
	 */
	private ExpressionTreeNode parseTerm(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode term, factor;
		int op;

		term = parseFactor(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '*' || op == '/')
			{
				int opType;
				
				if (op == '*')
					opType = MULTIPLY_OPERATION;
				else
					opType = DIVIDE_OPERATION;

				factor = parseFactor(p);
				term = new ExpressionTreeNode(term, opType, factor);
			}
			else
			{
				p.unread(op);
				break;
			}
		}
		return(term);
	}

	/*
	 * Parse a single number, string or variable name.
	 */
	private ExpressionTreeNode parseFactor(Preprocessor p) throws IOException, MapyrusException
	{
		boolean hasUnaryMinus = false;
		boolean parsedDecimalPoint = false;
		StringBuffer buf = new StringBuffer();
		ExpressionTreeNode nestedExpression;
		int c, lastC, quote;
		int type;

		c = p.readNonSpace();
		if (c == '\'' || c == '"')
		{
			/*
			 * It's a quoted string.  Keep reading up until matching quote.
			 */
			lastC = quote = c;
			while ((c = p.read()) != quote || lastC == '\\')
			{
				if (c == -1)
				{
					throw new MapyrusException("Unexpected end of file in string at " +
						p.getCurrentFilenameAndLineNumber());
				}

				if (c == '\\' && lastC != '\\')
				{
					/*
					 * Ignore all escaped characters except '\\'.
					 */
				}
				else
				{
					/*
					 * Compress '\\' in string into
					 * a single backslash.
					 */
					buf.append((char)c);
				}
				lastC = c;
			}
			return(new ExpressionTreeNode(new Argument(Argument.STRING, buf.toString())));
		}

		if (c == '+')
		{
			/*
			 * Skip unary plus.
			 */
			c = p.readNonSpace();
		}
		if (c == '-')
		{
			/*
			 * Note unary minus and continue.
			 */
			hasUnaryMinus = true;
			c = p.readNonSpace();
		}

		if (Character.isDigit((char)c) || c == '.')
		{
			/*
			 * Parse a decimal number and return it as a leaf node.
			 */
			while (Character.isDigit((char)c) || c == '.')
			{
				buf.append((char)c);
				if (c == '.')
				{
					/*
					 * Expect only one decimal point in a number.
					 */
					if (parsedDecimalPoint)
					{
						throw new MapyrusException("Invalid number in expression at " +
							p.getCurrentFilenameAndLineNumber());
					}
					parsedDecimalPoint = true;
				}
				c = p.read();
			}
			p.unread(c);

			double d = 0.0;
			try
			{
				d = Double.parseDouble(buf.toString());
			}
			catch (NumberFormatException e)
			{
			}

			if (hasUnaryMinus)
				d = -d;

			return(new ExpressionTreeNode(new Argument(d)));
		}

		if (c == -1)
		{
			throw new MapyrusException("Unexpected end of file at " + p.getCurrentFilenameAndLineNumber());
		}

		if (c != '(')
		{
			/*
			 * It does not look like a numeric expression or a string
			 * expression so maybe it is a variable name.
			 */
			if (Character.isLetter((char)c))
			{
				buf.append((char)c);
				c = p.read();
				while (c != -1 && (c == '.' || c == '_' ||
					Character.isLetterOrDigit((char)c)))
				{
					buf.append((char)c);
					c = p.read();
				}
				p.unread(c);
				return(new ExpressionTreeNode(new Argument(Argument.VARIABLE, buf.toString())));	
			}
			else
			{
				/*
				 * It's nothing that we understand.
				 */
				throw new MapyrusException("Invalid expression at " +
					p.getCurrentFilenameAndLineNumber());
			}
		}

		nestedExpression = parseCondition(p);

		c = p.readNonSpace();
		if (c != ')')
		{
			throw new MapyrusException("Unmatched '(' in expression at " +
				p.getCurrentFilenameAndLineNumber());
		}

		if (hasUnaryMinus)
		{
			/*
			 * Expand expression to negate value.
			 */
			ExpressionTreeNode left = new ExpressionTreeNode(new Argument(-1.0));
			nestedExpression = new ExpressionTreeNode(left,
				MULTIPLY_OPERATION, nestedExpression);
		}

		return(nestedExpression);
	}

	/**
	 * Read an arithmetic or string expression and create a tree from it
	 * that can later be evaluated.
	 * As many characters as possible are read that can be interpreted
	 * as part of an expression.
	 * @param p is the preprocessed output to read from.
	 */
	public Expression(Preprocessor p) throws IOException, MapyrusException
	{
		mExprTree = parseCondition(p);
	}

	/**
	 * Create an expression containing a simple numeric value.
	 * @param d is value to store as expression.
	 */
	public Expression(double d)
	{
		mExprTree = new ExpressionTreeNode(new Argument(d));
	}

	/**
	 * Create an expression containing a simple string value.
	 * @param s is string to store as expression.
	 */
	public Expression(String s)
	{
		mExprTree = new ExpressionTreeNode(new Argument(Argument.STRING, s));
	}

	/**
	 * Evaluate an expression.
	 * @param vars are all currently defined variables and their values.
	 * @return the evaluated expression, either a string or a number.
	 */
	public Argument evaluate(ContextStack context) throws MapyrusException
	{
		return(mExprTree.evaluate(context));
	}

	public static void main(String []args)
	{
		try
		{
			Preprocessor p;
			Expression e1, e2;
			ContextStack context = new ContextStack();
			Argument a1, a2;

			context.defineVariable("pi", new Argument(3.1415));
			
			/*
			 * Read two expressions separated by a comma or newline.
			 */
			p = new Preprocessor(args[0]);
			e1 = new Expression(p);
			p.read();
			e2 = new Expression(p);
			a1 = e1.evaluate(context);
			a2 = e2.evaluate(context);
			if (a1.getType() == Argument.NUMERIC)
			{
				System.out.println("a1=" + a1.getNumericValue());
			}
			else
			{
				System.out.println("a1 = " + a1.getStringValue());
			}
			
			if (a2.getType() == Argument.NUMERIC)
			{
				System.out.println("a2=" + a2.getNumericValue());
			}
			else
			{
				System.out.println("a2 = " + a2.getStringValue());
			}
		}
		catch (FileNotFoundException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (MapyrusException e)
		{
		}
	}
}

