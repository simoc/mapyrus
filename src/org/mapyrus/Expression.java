/**
 * An arithmetic expression tree.
 */

import java.lang.String;
import java.util.Hashtable;
import java.io.IOException;

public class Expression
{
	/*
	 * Types of expressions.
	 */
	public static final int NUMERIC = 0;
	public static final int STRING = 1;
	
	/*
	 * Types of operations allowed between two numbers (or two expressions).
	 */
	private static final int PLUS_OPERATION = 1;
	private static final int MINUS_OPERATION = 2;
	private static final int MULTIPLY_OPERATION = 3;
	private static final int DIVIDE_OPERATION = 4;

	private static final int EQUALS_OPERATION = 5;
	private static final int GREATER_OPERATION = 6;
	private static final int LESS_OPERATION = 7;

	private static final int AND_OPERATION = 8;
	private static final int OR_OPERATION = 9;

	/*
	 * Nodes in binary tree describing an arithmetic expression.
	 */
	class ExpressionTreeNode
	{
		int mType;
		boolean mIsLeaf;
		double mLeafNumber;
		String mLeafString;

		int mOperation;
		ExpressionTreeNode mLeftBranch, mRightBranch;

		/*
		 * Create a leaf value containing just a number.
		 */
		public ExpressionTreeNode(double leafValue)
		{
			mIsLeaf = true;
			mType = Expression.NUMERIC;
			mLeafNumber = leafValue;
		}
		
		/*
		 * Create a leaf value containing just a string.
		 */
		public ExpressionTreeNode(String leafValue)
		{
			mIsLeaf = true;
			mType = Expression.STRING;
			mLeafString = leafValue;
		}
		/*
		 * Create a node joining two sub-expressions with an
		 * operation between them.
		 */
		public ExpressionTreeNode(ExpressionTreeNode left,
			int operation,
			ExpressionTreeNode right, int type)
		{
			mIsLeaf = false;			
			mLeftBranch = left;
			mRightBranch = right;
			mOperation = operation;
			mType = type;
		}

		/**
		 * Evaluate binary tree containing a numeric expression.
		 * @return numeric value of the expression.
		 */
		public double evaluateNumeric(Hashtable h)
		{
			return(traverseNumeric(this, h));
		}

		/*
		 * Recursively traverse binary expression tree to
		 * calculate its numeric value.
		 */
		private double traverseNumeric(ExpressionTreeNode t, Hashtable h)
		{
			double retval;

			if (t.mIsLeaf)
			{
				retval = t.mLeafNumber;
			}
			else
			{
				double leftValue = 0.0, rightValue = 0.0;
				String leftString = null, rightString = null;
				int op;

				/*
				 * Both expressions can be either numeric or string.
				 */
				if (t.mLeftBranch.mType == NUMERIC)
				{
					leftValue = traverseNumeric(t.mRightBranch, h);
				}
				else
				{
					leftString = traverseString(t.mRightBranch, h);
				}

				if (t.mRightBranch.mType == NUMERIC)
				{
					rightValue = traverseNumeric(t.mRightBranch, h);
				}
				else
				{
					rightString = traverseString(t.mRightBranch, h);
				}

				/*
				 * Do operation and get a numeric value as a result.
				 */
				op = t.mOperation;
				if (op == PLUS_OPERATION)
					retval = leftValue + rightValue;
				else if (op == MINUS_OPERATION)
					retval = leftValue - rightValue;
				else if (op == MULTIPLY_OPERATION)
					retval = leftValue * rightValue;
				else if (op == DIVIDE_OPERATION)
					retval = leftValue / rightValue;
				else if (op == GREATER_OPERATION)
				{
					if (mLeftBranch.mType == Expression.NUMERIC)
					{
						retval = (leftValue > rightValue) ? 1 : 0;
					}
					else
					{
						retval = (leftString.compareTo(rightString) > 0) ? 1 : 0;
					}
				}
				else if (op == LESS_OPERATION)
				{
					if (mLeftBranch.mType == Expression.NUMERIC)
					{
						retval = (leftValue < rightValue) ? 1 : 0;
					}
					else
					{
						retval = (leftString.compareTo(rightString) < 0) ? 1 : 0;
					}
				}
				else if (op == EQUALS_OPERATION)
				{
					if (mLeftBranch.mType == Expression.NUMERIC)
					{
						retval = (leftValue == rightValue) ? 1 : 0;
					}
					else
					{
						retval = (leftString.equals(rightString)) ? 1 : 0;
					}
				}
				else if (op == AND_OPERATION)
				{
					retval = (leftValue != 0.0 &&
						rightValue != 0.0) ? 1 : 0;
				}
				else /* OR_OPERATION */
				{
					retval = (leftValue != 0.0 ||
						rightValue != 0.0) ? 1 : 0;
				}
			}
			return(retval);
		}
		
		/**
		 * Evaluate binary tree containing a string expression.
		 * @return string value of the expression.
		 */
		public String evaluateString(Hashtable h)
		{
			return(traverseString(this, h));
		}

		/*
		 * Recursively traverse binary expression tree to
		 * calculate its string value.
		 */
		private String traverseString(ExpressionTreeNode t, Hashtable h)
		{
			String retval;

			if (t.mIsLeaf)
			{
				retval = t.mLeafString;
			}
			else
			{
				double leftValue = 0.0, rightValue = 0.0;
				String leftString = null, rightString = null;
				int op;

				if (t.mLeftBranch.mType == NUMERIC)
				{
					leftValue = traverseNumeric(t.mRightBranch, h);
				}
				else
				{
					leftString = traverseString(t.mRightBranch, h);
				}

				if (t.mRightBranch.mType == NUMERIC)
				{
					rightValue = traverseNumeric(t.mRightBranch, h);
				}
				else
				{
					rightString = traverseString(t.mRightBranch, h);
				}

				/*
				 * Do operation and get a string as a result.
				 */
				op = t.mOperation;
				if (op == PLUS_OPERATION)
				{
					retval = leftString + rightString;
				}
				else /* MULTIPLY_OPERATION */
				{
					int count = (int)Math.round(rightValue);
					retval = new String();

					/*
					 * Repeat string N times.
					 */
					for (int i = 0; i < count; i++)
					{
						retval.concat(leftString);
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
			if (expr.mType == NUMERIC && (op == '+' || op == '-'))
			{
				term = parseTerm(p);
				if (term.mType != NUMERIC)
				{
					throw new MapyrusException("Expected number in expression at " +
						p.getCurrentFilenameAndLine());
				}
				int opType = (op == '+') ? PLUS_OPERATION : MINUS_OPERATION;

				expr = new ExpressionTreeNode(expr, opType, term, NUMERIC);
			}
			else if (expr.mType == STRING && op == '+')
			{
				term = parseTerm(p);
				if (term.mType != NUMERIC)
				{
					throw new MapyrusException("Expected number in expression at " +
						p.getCurrentFilenameAndLine());
				}
				expr = new ExpressionTreeNode(expr,	PLUS_OPERATION, term, STRING);
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
			if (term.mType == NUMERIC && (op == '*' || op == '/'))
			{
				factor = parseFactor(p);
				if (factor.mType != NUMERIC)
				{
					throw new MapyrusException("Expected number in expression at " +
						p.getCurrentFilenameAndLine());
				}
				int opType = (op == '*') ? MULTIPLY_OPERATION : DIVIDE_OPERATION;
				term = new ExpressionTreeNode(term,	opType, factor, NUMERIC);
			}
			else if (term.mType == STRING && op == 'x')
			{
				factor = parseFactor(p);
				if (factor.mType != NUMERIC)
				{
					throw new MapyrusException("Expected number in expression at " +
						p.getCurrentFilenameAndLine());
				}
				term = new ExpressionTreeNode(term,
					MULTIPLY_OPERATION, factor, STRING);
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
	 * Parse a single number or string.
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
			return(new ExpressionTreeNode(buf.toString()));	
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

			return(new ExpressionTreeNode(d));
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
				return(new ExpressionTreeNode(buf.toString()));	
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
			ExpressionTreeNode left = new ExpressionTreeNode(-1.0);
			nestedExpression = new ExpressionTreeNode(left,
				MULTIPLY_OPERATION, nestedExpression, NUMERIC);
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
		mExprTree = new ExpressionTreeNode(d);
	}
	
	/**
	 * Create an expression containing a simple string value.
	 * @param s is string to store as expression.
	 */
	public Expression(String s)
	{
		mExprTree = new ExpressionTreeNode(s);
	}
	
	/**
	 * Returns the type of an expression.
	 * @return either NUMERIC or STRING.
	 */
	public int getType()
	{
		return(mExprTree.mType);
	}
	
	/**
	 * Evaluate a numeric expression.
	 * @param vars are all currently defined internal variables and their values.
	 * @return the evaluated expression.
	 */
	public double evaluateNumeric(Hashtable vars)
	{
		return(mExprTree.evaluateNumeric(vars));
	}
	
	/**
	 * Evaluate a string expression.
	 * @param vars are all currently defined internal variables and their values.
	 * @return the evaluated expression.
	 */
	public String evaluateString(Hashtable vars)
	{
		return(mExprTree.evaluateString(vars));
	}
}

