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
import java.io.IOException;

public class Expression
{
	/*
	 * Types of operations allowed between two numbers or strings
	 * (or two expressions).
	 */
	private static final int PLUS_OPERATION = 1;
	private static final int MINUS_OPERATION = 2;
	private static final int MULTIPLY_OPERATION = 3;
	private static final int DIVIDE_OPERATION = 4;
	private static final int REPETITION_OPERATION = 5; /* 'qw' x 2 = 'qwqw' */
	private static final int CONTAINS_OPERATION = 6; /* 'foo' ~ 'o' = 1 */

	private static final int EQUALS_OPERATION = 7;
	private static final int GREATER_OPERATION = 8;
	private static final int LESS_OPERATION = 9;

	private static final int AND_OPERATION = 10;
	private static final int OR_OPERATION = 11;

	/*
	 * Nodes in binary tree describing an arithmetic expression.
	 */
	class ExpressionTreeNode
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
		 * @return numeric or string value of the expression.
		 */
		public Argument evaluate(Hashtable h) throws MapyrusException
		{
			return(traverse(this, h));
		}

		/*
		 * Recursively traverse binary expression tree to
		 * determine its value.
		 */
		private Argument traverse(ExpressionTreeNode t, Hashtable h)
			throws MapyrusException
		{
			Argument retval;
			Argument leftValue, rightValue;
			int op;
			double d;

			if (t.mIsLeaf)
			{
				retval = t.mLeafArg;
			}
			else
			{
				/*
				 * Either expressions can be numeric or string.
				 */
				leftValue = traverse(t.mLeftBranch, h);
				rightValue = traverse(t.mRightBranch, h);

				/*
				 * Evaluate any variable names.
				 */
				if (leftValue.getType() == Argument.VARIABLE)
				{
					Argument leftVarValue = (Argument)h.get(leftValue.getVariableName());
					if (leftVarValue == null)
					{
						throw new MapyrusException("Variable " +
							leftValue.getVariableName() + " not defined");
					}
					leftValue = leftVarValue;
				}
				if (rightValue.getType() == Argument.VARIABLE)
				{
					Argument rightVarValue = (Argument)h.get(rightValue.getVariableName());
					if (rightVarValue == null)
					{
						throw new MapyrusException("Variable " +
							rightValue.getVariableName() + " not defined");
					}
					rightValue = rightVarValue;
				}

				/*
				 * Check types for operation.  Repetition requires a string and a number
				 * but everything else requires matching types.
				 */
				op = t.mOperation;
				if (op == REPETITION_OPERATION)
				{
					if (leftValue.getType() != Argument.STRING ||
						rightValue.getType() != Argument.STRING)
					{
						throw new MapyrusException("Wrong types for repetition");
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
					else if (op == GREATER_OPERATION)
						d = (l > r) ? 1 : 0;
					else if (op == LESS_OPERATION)
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
					if (op == REPETITION_OPERATION)
					{
						/*
						 * Repeat string N times.
						 */
						s = new String();
						for (int i = 0; i < rightValue.getNumericValue(); i++)
						{
							s.concat(l);
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
						if (op == GREATER_OPERATION)
							d = (l.compareTo(r) > 0) ? 1 : 0;
						else if (op == LESS_OPERATION)
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
			if (op == '*' || op == '/' || op == 'x')
			{
				int opType;
				
				if (op == '*')
					opType = MULTIPLY_OPERATION;
				else if (op == '/')
					opType = DIVIDE_OPERATION;
				else
					opType = REPETITION_OPERATION;

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
						p.getCurrentFilenameAndLine());
				}

				if (c == '\\' && lastC == '\\')
				{
					/*
					 * Compress '\\' in string into
					 * a single backslash.
					 */
				}
				else
				{
					buf.append((char)c);
				}
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
						throw new MapyrusException("Invalid number in expression at " + p.getCurrentFilenameAndLine());
					}
					parsedDecimalPoint = true;
				}
				c = p.readNonSpace();
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
			throw new MapyrusException("Unexpected end of file at " + p.getCurrentFilenameAndLine());
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
					p.getCurrentFilenameAndLine());
			}
		}

		nestedExpression = parseExpression(p);

		c = p.readNonSpace();
		if (c != ')')
		{
			throw new MapyrusException("Unmatched '(' in expression at " + p.getCurrentFilenameAndLine());
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
		mExprTree = parseExpression(p);
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
	public Argument evaluate(Hashtable vars) throws MapyrusException
	{
		return(mExprTree.evaluate(vars));
	}
}

