/**
 * An arithmetic expression tree.
 */

import java.lang.String;
import java.util.Hashtable;
import java.io.IOException;

public class Expression
{
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
		boolean mIsLeaf;
		double mLeafValue;

		int mOperation;
		ExpressionTreeNode mLeftBranch, mRightBranch;

		/*
		 * Create a leaf value containing just a number.
		 */
		public ExpressionTreeNode(double leafValue)
		{
			mIsLeaf = true;
			mLeafValue = leafValue;
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


		/*
		 * Evaluate binary tree containing an expression.
		 * Returns the value of the expression.
		 */
		public double evaluate(Hashtable h1, Hashtable h2)
		{
			return(traverse(this, h1, h2));
		}

		/*
		 * Recursively traverse binary expression tree to
		 * calculate its value.
		 */
		private double traverse(ExpressionTreeNode t,
			Hashtable h1, Hashtable h2)
		{
			double retval;
			double leftValue, rightValue;
			int op;

			if (t.mIsLeaf)
			{
				retval = t.mLeafValue;
			}
			else
			{
				leftValue = traverse(t.mLeftBranch, h1, h2);
				rightValue = traverse(t.mRightBranch, h1, h2);
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
					retval = (leftValue > rightValue) ? 1 : 0;
				else if (op == LESS_OPERATION)
					retval = (leftValue < rightValue) ? 1 : 0;
				else if (op == EQUALS_OPERATION)
					retval = (leftValue == rightValue) ? 1 : 0;
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
	}

	ExpressionTreeNode mExprTree;

	/*
	 * Parse expression.
	 */
	private ExpressionTreeNode parseExpression(Preprocessor p) throws IOException, MapyrusException
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
				int opType = (op == '+') ? PLUS_OPERATION :
					MINUS_OPERATION;

				expr = new ExpressionTreeNode(expr,
					opType, term);
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
	private ExpressionTreeNode parseTerm(Preprocessor p) throws IOException, MapyrusException
	{
		ExpressionTreeNode term, factor;
		int op;

		term = parseFactor(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '*' || op == '/')
			{
				factor = parseFactor(p);
				int opType = (op == '*') ? MULTIPLY_OPERATION :
					DIVIDE_OPERATION;
				term = new ExpressionTreeNode(term,
					opType, factor);
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
	 * Parse a single number.
	 */
	private ExpressionTreeNode parseFactor(Preprocessor p) throws IOException, MapyrusException
	{
		boolean hasUnaryMinus = false;
		boolean parsedDecimalPoint = false;
		StringBuffer numBuffer = new StringBuffer();
		ExpressionTreeNode nestedExpression;
		int c;

		c = p.readNonSpace();
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
				numBuffer.append((char)c);
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
				d = Double.parseDouble(numBuffer.toString());
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
			throw new MapyrusException("Invalid expression at " + p.getCurrentFilenameAndLine());
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
				MULTIPLY_OPERATION, nestedExpression);
		}

		return(nestedExpression);
	}

	/**
	 * Read an arithmetic expression and create a tree from it
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
	 */
	public Expression(double d)
	{
		mExprTree = new ExpressionTreeNode(d);
	}

	/**
	 * Evaluate an arithmetic expression.
	 * @param systemVariables are the currently defined internal variables
	 * and their values.
	 * @param userVariables are variables defined by user and their values.
	 * @return the evaluated expression.
	 */
	public double evaluate(Hashtable systemVariables, Hashtable userVariables)
	{
		return(mExprTree.evaluate(systemVariables, userVariables));
	}
}

