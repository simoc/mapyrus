/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005, 2006 Simon Chenery.
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

import java.io.IOException;
import java.io.FileNotFoundException;

import org.mapyrus.function.*;

/**
 * An expression tree.  Parser for numeric or string expression that builds
 * a binary tree holding the expression.  The expression can be later
 * be evaluated and the tree is evaluated to a single value.
 *
 * For example, the expression '(a * 2) + 7' is represented by the tree:
 * <pre>
 *       +
 *      / \
 *     *   7
 *    / \
 *   a   2
 * </pre>
 */
public class Expression
{
	/*
	 * Types of operations allowed between two numbers or strings
	 * (or two expressions).
	 */
	private static final int NO_OPERATION = 0;
	private static final int PLUS_OPERATION = 1;
	private static final int CONCAT_OPERATION = 2; /* 'qw' . 'er' . 7 = 'qwer7' */
	private static final int MINUS_OPERATION = 3;
	private static final int MULTIPLY_OPERATION = 4;
	private static final int REPEAT_OPERATION = 5;	/* 'qw' x 2 = 'qwqw' */
	private static final int DIVIDE_OPERATION = 6;
	private static final int MODULO_OPERATION = 7;

	private static final int LEXICAL_EQUALS_OPERATION = 100;	/* 'foo' eq 'foo' */
	private static final int LEXICAL_NOT_EQUALS_OPERATION = 101;	/* 'foo' ne 'qw' */
	private static final int LEXICAL_GREATER_THAN_OPERATION = 102;	/* 'qw' gt 'foo' */
	private static final int LEXICAL_LESS_THAN_OPERATION = 103;	/* 'foo' lt 'qw' */
	private static final int LEXICAL_GREATER_EQUAL_OPERATION = 104;	/* 'qw' ge 'foo' */
	private static final int LEXICAL_LESS_EQUAL_OPERATION = 105;	/* 'foo' le 'qw' */

	private static final int NUMERIC_EQUALS_OPERATION = 200;	/* 77 == 77 */
	private static final int NUMERIC_NOT_EQUALS_OPERATION = 201;	/* 7 != 77 */
	private static final int NUMERIC_GREATER_THAN_OPERATION = 202;	/* 77 > 7 */
	private static final int NUMERIC_LESS_THAN_OPERATION = 203;	/* 7 < 77 */
	private static final int NUMERIC_GREATER_EQUAL_OPERATION = 204;	/* 77 >= 7 */
	private static final int NUMERIC_LESS_EQUAL_OPERATION = 205;	/* 7 <= 77 */

	private static final int CONDITIONAL_OPERATION = 300;	/* a == 77 ? "yes" : "no" */

	private static final int ASSIGN_OPERATION = 400;	/* a = 77 */

	private static final int AND_OPERATION = 500;
	private static final int OR_OPERATION = 501;
	private static final int NOT_OPERATION = 502;
	
	private static final int HASHMAP_REFERENCE = 600;		/* a[77] */

	/*
	 * Nodes in binary tree describing an arithmetic expression.
	 */
	private class ExpressionTreeNode
	{
		boolean mIsLeaf;
		Argument mLeafArg;

		int mOperation;
		ExpressionTreeNode mLeftBranch, mRightBranch;

		boolean mIsFunction;
		Function mFunction;
		ExpressionTreeNode mThirdFunctionExpression;
		ExpressionTreeNode mFourthFunctionExpression;

		/**
		 * Create a leaf value containing either a number,
		 * string or variable name.
		 * @param arg is leaf argument value.
		 */
		public ExpressionTreeNode(Argument arg)
		{
			mIsLeaf = true;
			mIsFunction = false;
			mLeafArg = arg;
		}
	
		/**
		 * Create a node joining two sub-expressions with an
		 * operation between them.
		 * @param left is left hand side of expression.
		 * @param operation is operation between left and right handside expressions.
		 * @param right is right hand side of expression.
		 */
		public ExpressionTreeNode(ExpressionTreeNode left,
			int operation,
			ExpressionTreeNode right)
		{
			mIsLeaf = mIsFunction = false;			
			mLeftBranch = left;
			mRightBranch = right;
			mOperation = operation;
		}

		/**
		 * Create a node joining three sub-expressions with an
		 * operation between them.
		 * @param left is left hand side of expression.
		 * @param operation is operation between expressions.
		 * @param right is right hand side of expression.
		 * @param third is third sub-expression in operation.
		 */
		public ExpressionTreeNode(ExpressionTreeNode left,
			int operation,
			ExpressionTreeNode right, ExpressionTreeNode third)
		{
			mIsLeaf = mIsFunction = false;			
			mLeftBranch = left;
			mRightBranch = right;
			mOperation = operation;
			mThirdFunctionExpression = third;
		}

		/**
		 * Create a node containing a call to a function.
		 * @param functionType is identifier of function being called.
		 * @param arg1 is first argument to function.
		 * @param arg2 is second argument to function (or null for single
		 * argument functions).
		 * @param arg3 is third argument to function (or null for functions
		 * with less than 3 arguments)
		 */
		public ExpressionTreeNode(Function func, ExpressionTreeNode arg1,
			ExpressionTreeNode arg2, ExpressionTreeNode arg3, ExpressionTreeNode arg4)
		{
			mIsLeaf = false;
			mIsFunction = true;
			mFunction = func;
			mLeftBranch = arg1;
			mRightBranch = arg2;
			mThirdFunctionExpression = arg3;
			mFourthFunctionExpression = arg4;
		}

		/**
		 * Evaluate binary tree expression..
		 * @param context variable definitions and other context information.
		 * @param interpreterFilename name of file being interpreted.
		 * @return numeric or string value of the expression.
		 */
		public Argument evaluate(ContextStack context, String interpreterFilename)
			throws MapyrusException
		{
			return(traverse(this, context, interpreterFilename));
		}

		/**
		 * Evalute internal function.
		 * @param context is context containing variable values
		 * @param interpreterFilename name of file being interpreted.
		 * @return result of function
		 */
		private Argument evaluateFunction(ContextStack context,
			String interpreterFilename)
			throws MapyrusException
		{
			Argument leftValue = null;
			Argument rightValue = null;
			Argument thirdValue = null;
			Argument fourthValue = null;
			Argument retval = null;
			int nArgs = 0;

			/*
			 * Evaluate each of the arguments being passed to the function.
			 */
			if (mLeftBranch != null)
			{
				leftValue = traverse(mLeftBranch, context, interpreterFilename);
				nArgs = 1;
			}
			if (mRightBranch != null)
			{
				rightValue = traverse(mRightBranch, context, interpreterFilename);
				nArgs = 2;
			}
			if (mThirdFunctionExpression != null)
			{
				thirdValue = traverse(mThirdFunctionExpression, context, interpreterFilename);
				nArgs = 3;
			}
			if (mFourthFunctionExpression != null)
			{
				fourthValue = traverse(mFourthFunctionExpression, context, interpreterFilename);
				nArgs = 4;
			}

			/*
			 * Evaluate function.
			 */
			try
			{
				if (nArgs == 0)
					retval = mFunction.evaluate(context);
				else if (nArgs == 1)
					retval = mFunction.evaluate(context, leftValue);
				else if (nArgs == 2)
					retval = mFunction.evaluate(context, leftValue, rightValue);
				else if (nArgs == 3)
					retval = mFunction.evaluate(context, leftValue, rightValue, thirdValue);
				else if (nArgs == 4)
					retval = mFunction.evaluate(context, leftValue, rightValue, thirdValue, fourthValue);
			}
			catch (MapyrusException e)
			{
				/*
				 * Prepend function name to error message.
				 */
				throw new MapyrusException(mFunction.getName() + ": " + e.getMessage());
			}

			return(retval);
		}

		/*
		 * Recursively traverse binary expression tree to
		 * determine its value.
		 */
		private Argument traverse(ExpressionTreeNode t, ContextStack context,
			String interpreterFilename) throws MapyrusException
		{
			Argument retval;
			Argument leftValue, rightValue;
			int returnType = Argument.NUMERIC;
			double l, r, d = 0.0;
			String s = null;

			if (t.mIsLeaf)
			{
				/*
				 * Evaluate any variable name.  Variables that are not assigned
				 * are given the value of an empty string (which converts to the
				 * numeric value 0), like in awk(1) and Perl.
				 */
				if (t.mLeafArg.getType() == Argument.VARIABLE)
				{
					retval = context.getVariableValue(t.mLeafArg.getVariableName(), interpreterFilename);
					if (retval == null)
						retval = Argument.emptyString;
				}
				else
				{
					retval = t.mLeafArg;
				}
			}
			else if (t.mIsFunction)
			{
				retval = t.evaluateFunction(context, interpreterFilename);
			}
			else if (t.mOperation == NOT_OPERATION)
			{
				/*
				 * Negation operates on a single expression.
				 */
				leftValue = traverse(t.mLeftBranch, context, interpreterFilename);
				if (leftValue.getType() == Argument.NUMERIC)
				{
					l = leftValue.getNumericValue();
					retval = NumericalAnalysis.equals(l, 0.0) ?
						Argument.numericOne : Argument.numericZero;
				}
				else
				{
					retval = (leftValue.getStringValue().length() == 0) ?
						Argument.numericOne : Argument.numericZero;
				}
			}
			else if (t.mOperation == ASSIGN_OPERATION)
			{
				rightValue = traverse(t.mRightBranch, context, interpreterFilename);
				ExpressionTreeNode leftBranch = t.mLeftBranch;
				if (leftBranch.mIsLeaf)
				{
					/*
					 * Simple assignment: a = b.
					 */
					String varName = t.mLeftBranch.mLeafArg.getVariableName();
					context.defineVariable(varName, rightValue);
				}
				else if (leftBranch.mOperation == HASHMAP_REFERENCE &&
					leftBranch.mLeftBranch.mIsLeaf)
				{
					/*
					 * Assign value as entry in a hashmap: a[55] = "foo".
					 */
					String hashMapName = leftBranch.mLeftBranch.mLeafArg.getVariableName();
					if (hashMapName == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_VARIABLE));
					}
					Argument key = traverse(t.mLeftBranch.mRightBranch, context,
						interpreterFilename);
					if (key.getType() != Argument.NUMERIC && key.getType() != Argument.STRING)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HASHMAP_KEY));
					}
					context.defineHashMapEntry(hashMapName, key.getStringValue(),
						rightValue);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_VARIABLE));
				}

				/*
				 * Return value assigned.
				 */
				retval = rightValue;
			}
			else if (t.mOperation == HASHMAP_REFERENCE)
			{
				Argument hashMapVar;

				/*
				 * Lookup an individual entry in a hash map from a hash map
				 * variable name and key.
				 */
				if (t.mLeftBranch.mIsLeaf && t.mLeftBranch.mLeafArg.getType() == Argument.VARIABLE)
				{
					String varName = t.mLeftBranch.mLeafArg.getVariableName();
					hashMapVar = context.getVariableValue(varName, interpreterFilename);
				}
				else if (t.mLeftBranch.mIsFunction)
				{
					hashMapVar = traverse(t.mLeftBranch, context, interpreterFilename);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
				}

				Argument key = traverse(t.mRightBranch, context, interpreterFilename);
				if (key.getType() != Argument.NUMERIC && key.getType() != Argument.STRING)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HASHMAP_KEY));
				}
				
				if (hashMapVar == null || hashMapVar.getType() != Argument.HASHMAP)
				{
					/*
					 * No hash map exists with this name so return empty string.
					 */
					retval = Argument.emptyString;
				}
				else
				{
					/*
					 * Return value assigned to this key value in hash map.
					 */
					retval = hashMapVar.getHashMapEntry(key.getStringValue());
				}
			}
			else if (t.mOperation == CONDITIONAL_OPERATION)
			{
				/*
				 * Test condition and return value for true, or value for false.
				 */
				leftValue = traverse(t.mLeftBranch, context, interpreterFilename);
				if (leftValue.getNumericValue() != 0)
					retval = traverse(t.mRightBranch, context, interpreterFilename);
				else
					retval = traverse(t.mThirdFunctionExpression, context, interpreterFilename);
			}
			else
			{
				/*
				 * Either expression can be a number or a string.
				 */
				leftValue = traverse(t.mLeftBranch, context, interpreterFilename);
				rightValue = traverse(t.mRightBranch, context, interpreterFilename);

				switch (t.mOperation)
				{
				case PLUS_OPERATION:
					d = leftValue.getNumericValue() + rightValue.getNumericValue();
					returnType = Argument.NUMERIC;
					break;
				case CONCAT_OPERATION:
					s = leftValue.toString() + rightValue.toString();
					returnType = Argument.STRING;
					break;
				case MINUS_OPERATION:
					d = leftValue.getNumericValue() - rightValue.getNumericValue();
					returnType = Argument.NUMERIC;
					break;
				case MULTIPLY_OPERATION:
					d = leftValue.getNumericValue() * rightValue.getNumericValue();
					returnType = Argument.NUMERIC;
					break;
				case REPEAT_OPERATION:
					/*
					 * Repeat string N times.
					 */
					StringBuffer sb = new StringBuffer();
					int repeatCount = (int)(Math.floor(rightValue.getNumericValue()));
					for (int i = 0; i < repeatCount; i++)
					{
						sb.append(leftValue.toString());
					}
					s = sb.toString();
					returnType = Argument.STRING;
					break;
				case DIVIDE_OPERATION:
					d = leftValue.getNumericValue() / rightValue.getNumericValue();
					returnType = Argument.NUMERIC;
					break;
				case MODULO_OPERATION:
					d = NumericalAnalysis.fmod(leftValue.getNumericValue(),
						rightValue.getNumericValue());
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_EQUALS_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = NumericalAnalysis.equals(l, r) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_NOT_EQUALS_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = NumericalAnalysis.equals(l, r) ? 0 : 1;
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_GREATER_THAN_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = (l > r && (!NumericalAnalysis.equals(l, r))) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_GREATER_EQUAL_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = (l > r || NumericalAnalysis.equals(l, r)) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_LESS_THAN_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = (l < r && (!NumericalAnalysis.equals(l, r))) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case NUMERIC_LESS_EQUAL_OPERATION:
					l = leftValue.getNumericValue();
					r = rightValue.getNumericValue();
					d = (l < r || NumericalAnalysis.equals(l, r)) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case AND_OPERATION:
					d = (leftValue.getNumericValue() != 0 &&
						rightValue.getNumericValue() != 0) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case OR_OPERATION:
					d = (leftValue.getNumericValue() != 0 ||
						rightValue.getNumericValue() != 0) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_EQUALS_OPERATION:
					d = leftValue.toString().equals(rightValue.toString()) ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_NOT_EQUALS_OPERATION:
					d = leftValue.toString().equals(rightValue.toString()) ? 0 : 1;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_GREATER_THAN_OPERATION:
					d = leftValue.toString().compareTo(rightValue.toString()) > 0 ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_GREATER_EQUAL_OPERATION:
					d = leftValue.toString().compareTo(rightValue.toString()) >= 0 ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_LESS_THAN_OPERATION:
					d = leftValue.toString().compareTo(rightValue.toString()) < 0 ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				case LEXICAL_LESS_EQUAL_OPERATION:
					d = leftValue.toString().compareTo(rightValue.toString()) <= 0 ? 1 : 0;
					returnType = Argument.NUMERIC;
					break;
				}

				if (returnType == Argument.NUMERIC)
				{
					/*
					 * Fail on numeric overflow and divide by zero.
					 */
					if (Double.isInfinite(d) || Double.isNaN(d))
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NUMERIC_OVERFLOW));

					if (d == 0.0)
						retval = Argument.numericZero;
					else if (d == 1.0)
						retval = Argument.numericOne;
					else
						retval = new Argument(d);
				}
				else
				{
					if (s.length() == 0)
						retval = Argument.emptyString;
					else
						retval = new Argument(Argument.STRING, s);
				}
			}
			return(retval);
		}
		
		/**
		 * String representation of an expression tree.
		 * @return expression as a string.
		 */
		public String toString()
		{
			String retval;
			StringBuffer sb;
			
			if (mIsLeaf)
			{
				retval = mLeafArg.toString();
			}
			else
			{
				String operation = "";
				switch (mOperation)
				{
					case PLUS_OPERATION:
						operation = "+";
						break;
					case CONCAT_OPERATION:
						operation = ".";
						break;
					case MINUS_OPERATION:
						operation = "-";
						break;
					case MULTIPLY_OPERATION:
						operation = "*";
						break;
					case REPEAT_OPERATION:
						operation = "x";
						break;
					case DIVIDE_OPERATION:
						operation = "/";
						break;
					case MODULO_OPERATION:
						operation = "%";
						break;
					case LEXICAL_EQUALS_OPERATION:
						operation = "eq";
						break;
					case LEXICAL_NOT_EQUALS_OPERATION:
						operation = "ne";
						break;
					case LEXICAL_GREATER_THAN_OPERATION:
						operation = "gt";
						break;
					case LEXICAL_LESS_THAN_OPERATION:
						operation = "lt";
						break;
					case LEXICAL_GREATER_EQUAL_OPERATION:
						operation = "ge";
						break;
					case LEXICAL_LESS_EQUAL_OPERATION:
						operation = "le";
						break;
					case NUMERIC_EQUALS_OPERATION:
						operation = "==";
						break;
					case NUMERIC_NOT_EQUALS_OPERATION:
						operation = "!=";
						break;
					case NUMERIC_GREATER_THAN_OPERATION:
						operation = ">";
						break;
					case NUMERIC_LESS_THAN_OPERATION:
						operation  = "<";
						break;
					case NUMERIC_GREATER_EQUAL_OPERATION:
						operation = ">=";
						break;
					case NUMERIC_LESS_EQUAL_OPERATION:
						operation = "<=";
						break;
					case CONDITIONAL_OPERATION:
						operation = "?";
						break;
					case ASSIGN_OPERATION:
						operation = "=";
						break;
					case AND_OPERATION:
						operation = "and";
						break;
				 	case OR_OPERATION:
				 		operation  = "or";
				 		break;
				 	case NOT_OPERATION:
				 		operation = "not";
				 		break;
					case HASHMAP_REFERENCE:
						operation = "[";
						break;
				}
				
				sb = new StringBuffer();
				sb.append(mLeftBranch.toString());
				sb.append(' ');
				sb.append(operation);
				sb.append(' ');

				if (mOperation != NOT_OPERATION)
					sb.append(mRightBranch.toString());
				if (mOperation == HASHMAP_REFERENCE)
					sb.append(']');
				if (mOperation == CONDITIONAL_OPERATION)
					sb.append(" : ").append(mThirdFunctionExpression.toString());
				retval = sb.toString();
			}
			return(retval);
		}
	}

	ExpressionTreeNode mExprTree;

	/*
	 * Parse expression including "or" boolean operations.
	 */
	private ExpressionTreeNode parseOrBoolean(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, b;
		int op1, op2;

		expr = parseAndBoolean(p);
		while (true)
		{
			/*
			 * If next two characters spell "or" then parse
			 * right hand side of expression.
			 */
			op1 = p.readNonSpace();
			if (op1 == 'o' || op1 == 'O')
			{
				op2 = p.read();
				if (op2 == 'r' || op2 == 'R')
				{
					b = parseAndBoolean(p);
					expr = new ExpressionTreeNode(expr, OR_OPERATION, b);
				}
				else
				{
					p.unread(op2);
					p.unread(op1);
					break;
				}
			}
			else				
			{
				p.unread(op1);
				break;
			}
		}
		return(expr);
	}

	/*
	 * Parse expression including "and" boolean operations.
	 */
	private ExpressionTreeNode parseAndBoolean(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, b;
		int op1, op2, op3;

		expr = parseNotBoolean(p);
		while (true)
		{
			/*
			 * If next three characters spell "and" then parse
			 * right hand side of expression.
			 */
			op1 = p.readNonSpace();
			if (op1 == 'a' || op1 == 'A')
			{
				op2 = p.read();
				if (op2 == 'n' || op2 == 'N')
				{
					op3 = p.read();
					if (op3 == 'd' || op3 == 'D')
					{
						b = parseNotBoolean(p);
						expr = new ExpressionTreeNode(expr, AND_OPERATION, b);
					}
					else
					{
						p.unread(op3);
						p.unread(op2);
						p.unread(op1);
						break;
					}
				}
				else
				{
					p.unread(op2);
					p.unread(op1);
					break;
				}
			}
			else				
			{
				p.unread(op1);
				break;
			}
		}
		return(expr);
	}

	/*
	 * Parse expression including "not" boolean operations.
	 */
	private ExpressionTreeNode parseNotBoolean(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode b, expr = null;
		int op1, op2, op3, op4;

		/*
		 * If next three characters spell "not" then parse
		 * expression to be negated.
		 */
		op1 = p.readNonSpace();
		if (op1 == 'n' || op1 == 'N')
		{
			op2 = p.read();
			if (op2 == 'o' || op2 == 'O')
			{
				op3 = p.read();
				if (op3 == 't' || op3 == 'T')
				{
					op4 = p.read();
					if (!Character.isLetterOrDigit((char)op4) && op4 != '_' && op4 != '.')
					{
						b = parseNotBoolean(p);
						expr = new ExpressionTreeNode(b, NOT_OPERATION, null);
					}
					else
					{
						p.unread(op4);
						p.unread(op3);
						p.unread(op2);
						p.unread(op1);
					}
				}
				else
				{
					p.unread(op3);
					p.unread(op2);
					p.unread(op1);
				}
			}
			else
			{
				p.unread(op2);
				p.unread(op1);
			}
		}
		else				
		{
			p.unread(op1);
		}

		if (expr == null)
			expr = parseAssignment(p);
		return(expr);
	}

	/*
	 * Parse expression including assignment to variables.
	 */
	private ExpressionTreeNode parseAssignment(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, value;
		int op1, op2;

		expr = parseConditional(p);
		while (true)
		{
			/*
			 * If next character is '=' then we have an assignment
			 * to a variable.
			 */
			op1 = p.readNonSpace();
			if (op1 == '=')
			{
				op2 = p.read();
				p.unread(op2);
				if (op2 != '=')
				{
					/*
					 * Check that lefthandside of an assignment is a variable name,
					 * or an element in a hashmap.
					 */
					if (expr.mIsLeaf)
					{
						if (expr.mLeafArg.getType() != Argument.VARIABLE)
						{
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
								MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
						}
					}
					else if (expr.mOperation != HASHMAP_REFERENCE)
					{
						throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
								MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
					}
					value = parseConditional(p);
					expr = new ExpressionTreeNode(expr, ASSIGN_OPERATION, value);
				}
				else
				{
					p.unread(op1);
					break;
				}
			}
			else				
			{
				p.unread(op1);
				break;
			}
		}
		return(expr);
	}

	/*
	 * Parse conditional expression, like: hour < 12 ? "AM" : "PM"
	 */
	private ExpressionTreeNode parseConditional(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, trueExpr, falseExpr;
		int op;

		expr = parseComparison(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '?')
			{
				/*
				 * Parse full expression for true and false cases so expression
				 * i == 1 ? j == 2 ? 3 : 4 : 5
				 * is parsed correctly using "right associativity".
				 */
				trueExpr = parseOrBoolean(p);
				op = p.readNonSpace();
				if (op == ':')
				{
					falseExpr = parseOrBoolean(p);
					expr = new ExpressionTreeNode(expr, CONDITIONAL_OPERATION, trueExpr, falseExpr);
				}
				else
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
						MapyrusMessages.get(MapyrusMessages.INVALID_CONDITIONAL));
				}
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
	 * Parse a comparison expression.
	 */
	private ExpressionTreeNode parseComparison(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, cond;
		int op1, op2;
		int opType;

		cond = parseExpression(p);
		while (true)
		{
			/*
			 * What type of comparison have we got?
			 */
			opType = NO_OPERATION;
			op1 = p.readNonSpace();
			if (op1 == '<' || op1 == '>' || op1 == '!' || op1 == '=')
			{
				/*
				 * First character makes it look like a numerical comparision.  Is it?
				 */
				op2 = p.read();
				if (op2 == '=')
				{
					if (op1 == '=')
						opType = NUMERIC_EQUALS_OPERATION;
					else if (op1 == '!')
						opType = NUMERIC_NOT_EQUALS_OPERATION;
					else if (op1 == '<')
						opType = NUMERIC_LESS_EQUAL_OPERATION;
					else
						opType = NUMERIC_GREATER_EQUAL_OPERATION;
				}
				else
				{
					p.unread(op2);

					if (op1 == '<')
						opType = NUMERIC_LESS_THAN_OPERATION;
					else if (op1 == '>')
						opType = NUMERIC_GREATER_THAN_OPERATION;
				}
			}
			else if (op1 == 'l' || op1 == 'e' || op1 == 'g' || op1 == 'n')
			{
				/*
				 * First character makes it look like a string comparision.
				 */
				op2 = p.read();
			
				if (op1 == 'n' && op2 == 'e')
					opType = LEXICAL_NOT_EQUALS_OPERATION;
				else if (op1 == 'e' && op2 == 'q')
					opType = LEXICAL_EQUALS_OPERATION;
				else if (op1 == 'l' && op2 == 't')
					opType = LEXICAL_LESS_THAN_OPERATION;
				else if (op1 == 'l' && op2 == 'e')
					opType = LEXICAL_LESS_EQUAL_OPERATION;
				else if (op1 == 'g' && op2 == 't')
					opType = LEXICAL_GREATER_THAN_OPERATION;
				else if (op1 == 'g' && op2 == 'e')
					opType = LEXICAL_GREATER_EQUAL_OPERATION;
				else
					p.unread(op2);
			}

			/*
			 * If we found a valid comparison then parse right-hand
			 * side of comparison.
			 */
			if (opType != NO_OPERATION)
			{
				expr = parseExpression(p);
				cond = new ExpressionTreeNode(cond, opType, expr);
			}
			else				
			{
				p.unread(op1);
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
			if (op == '+' || op == '-' || op == '.')
			{
				term = parseTerm(p);
				int opType;
				if (op == '+')
					opType = PLUS_OPERATION;
				else if (op == '-')
					opType = MINUS_OPERATION;
				else
					opType = CONCAT_OPERATION;

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

		term = parseUnary(p);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '*' || op == '/' || op == 'x' || op == '%')
			{
				int opType;

				if (op == '*')
					opType = MULTIPLY_OPERATION;
				else if (op == '/')
					opType = DIVIDE_OPERATION;
				else if (op == '%')
					opType = MODULO_OPERATION;
				else
					opType = REPEAT_OPERATION;

				factor = parseUnary(p);
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
	 * Parse expression including unary plus or minus.
	 */
	private ExpressionTreeNode parseUnary(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr;
		int op;

		op = p.readNonSpace();
		if (op != '+' && op != '-')
		{
			p.unread(op);
		}
		expr = parseHashMapReference(p);
		if (op == '-')
		{
			/*
			 * Negate value of expression by multiplying by -1.
			 */
			ExpressionTreeNode left = new ExpressionTreeNode(Argument.numericMinusOne);
			expr = new ExpressionTreeNode(left, MULTIPLY_OPERATION, expr);
		}
		return(expr);
	}

	/*
	 * Parse expression including reference to an element in a hashmap.
	 */
	private ExpressionTreeNode parseHashMapReference(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, keyExpr;
		int op1;

		expr = parseFactor(p);
		while (true)
		{
			/*
			 * If next character is '[' then parse hashmap key, then closing ']'.
			 */
			op1 = p.readNonSpace();
			if (op1 == '[')
			{
				keyExpr = parseOrBoolean(p);
				op1 = p.readNonSpace();
				if (op1 != ']')
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) + ": ']'");
				}

				/*
				 * Expression tree for the hashmap reference a["foo"] is:
				 *      []
				 *     /  \
				 *    a   "foo"
				 */
				expr = new ExpressionTreeNode(expr, HASHMAP_REFERENCE, keyExpr);
			}
			else
			{
				p.unread(op1);
				break;
			}
		}
		return(expr);
	}
	
	/*
	 * Parse a single number, string or variable name.
	 */
	private ExpressionTreeNode parseFactor(Preprocessor p) throws IOException, MapyrusException
	{
		boolean parsedDigit;
		StringBuffer buf = new StringBuffer();
		ExpressionTreeNode expr;
		int c, quote;

		boolean inOctalCode;
		boolean inEscape;
		int octalCode;
		int nOctalDigits;

		c = p.readNonSpace();
		if (c == '\'' || c == '"')
		{
			/*
			 * It's a quoted string.  Keep reading up until matching quote.
			 */
			quote = c;
			inOctalCode = inEscape = false;
			octalCode = nOctalDigits = 0;
			while ((c = p.read()) != quote || inEscape)
			{
				if (c == -1)
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
				}

				if (inOctalCode)
				{
					/*
					 * If next character is a digit then it is part of a
					 * a '\nnn' octal character code.
					 */
					if (Character.isDigit((char)c) && nOctalDigits < 3)
					{
						octalCode *= 8;
						octalCode += c - '0';
						nOctalDigits++;
					}
					else
					{
						/*
						 * Next character is not part of octal code.
						 * Add character with octal code we've built
						 * then return to regular parsing.
						 */
						buf.append((char)octalCode);
						inOctalCode = false;
					}
				}

				if (!inOctalCode)
				{
					if (inEscape)
					{
						/*
						 * '\\' compressed to single backslash,
						 * '\n' converted to a newline, '\r' is stripped -- it
						 * is not useful since we use Java's line separator internally.
						 * '\t" is converted to a tab.
						 * '\367' is converted to character code (3 * 64) + (6 * 8) + 7.
						 * Escaping of other characters is ignored.
						 */
						if (c == 'n')
						{
							buf.append(Constants.LINE_SEPARATOR);
						}
						else if (c == 't')
						{
							buf.append("\t");
						}
						else if (Character.isDigit((char)c))
						{
							inOctalCode = true;
							octalCode = c - '0';
							nOctalDigits = 1;
						}
						else if (c != 'r')
						{
							buf.append((char)c);
						}
						inEscape = false;
					}
					else if (c == '\\')
					{
						inEscape = true;
					}
					else
					{
						buf.append((char)c);
					}
				}
			}

			/*
			 * Add any character we were building as octal code when quoted string finished.
			 */
			if (inOctalCode)
				buf.append((char)octalCode);

			return(new ExpressionTreeNode(new Argument(Argument.STRING, buf.toString())));
		}

		if (Character.isDigit((char)c) || c == '.')
		{
			/*
			 * Parse a decimal number and return it as a leaf node.
			 */
			parsedDigit = false;
			while (Character.isDigit((char)c))
			{
				parsedDigit = true;
				buf.append((char)c);
				c = p.read();
			}
			
			if (c == '.')
			{
				buf.append((char)c);
				c = p.read();
			}

			while (Character.isDigit((char)c))
			{
				parsedDigit = true;
				buf.append((char)c);
				c = p.read();
			}

			if (!parsedDigit)
			{
				throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER));
			}

			if (c == 'e' || c == 'E')
			{
				buf.append((char)c);
				c = p.read();
				if (c == '+' || c == '-')
				{
					buf.append((char)c);
					c = p.read();
				}

				/*
				 * Expect at least one digit for the exponent value.
				 */
				if (!Character.isDigit((char)c))
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER));
				}

				while (Character.isDigit((char)c))
				{
					buf.append((char)c);
					c = p.read();
				}
			}

			p.unread(c);

			double d = 0.0;
			try
			{
				d = Double.parseDouble(buf.toString());
			}
			catch (NumberFormatException e)
			{
				/*
				 * We parsed the number so it will
				 * always be valid.
				 */
			}

			return(new ExpressionTreeNode(new Argument(d)));
		}

		if (c == -1)
		{
			throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}

		if (c == '(')
		{
			expr = parseOrBoolean(p);

			c = p.readNonSpace();
			if (c != ')')
			{
				throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNMATCHED_BRACKET));
			}
		}
		else if (Character.isLetter((char)c) || c == '$')
		{
			/*
			 * It does not look like a numeric expression or a string
			 * expression so maybe it is a variable name or function.
			 */
			buf.append((char)c);
			c = p.read();
			while (c != -1 && (c == '.' || c == '_' || c == ':' ||
				Character.isLetterOrDigit((char)c)))
			{
				buf.append((char)c);
				c = p.read();
			}

			/*
			 * Is this a function call like "round(3.14)"?
			 */
			Function f = FunctionTable.getFunction(buf.toString());
			if (f != null)
			{
				/*
				 * Parse opening '(', arguments for this function, then closing ')'.
				 */
				if (Character.isWhitespace((char)c))
					c = p.readNonSpace();
				if (c != '(')
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) + ": '('");
				}

				int minArgs = f.getMinArgumentCount();
				int maxArgs = f.getMaxArgumentCount();

				/*
				 * Parse expression for each function argument.
				 */
				ExpressionTreeNode functionExpressions[] = new ExpressionTreeNode[4];
				for (int i = 0; i < functionExpressions.length; i++)
					functionExpressions[i] = null;

				if (minArgs >= 0)
				{
					functionExpressions[0] = parseOrBoolean(p);
				}

				for (int i = 1; i < maxArgs; i++)
				{
					/*
					 * Parse comma before next value, or closing bracket.
					 */
					c = p.readNonSpace();
					if (i >= minArgs && c == ')')
					{
						p.unread(c);
						break;
					}
					else if (c != ',')
					{
						throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
							": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
							": " + buf.toString());
					}

					functionExpressions[i] = parseOrBoolean(p);
				}

				/*
				 * Read closing bracket.
				 */
				c = p.readNonSpace();
				if (c != ')')
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
						": " + buf.toString());
				}

				expr = new ExpressionTreeNode(f, functionExpressions[0],
					functionExpressions[1], functionExpressions[2], functionExpressions[3]);
			}
			else
			{
				p.unread(c);
				expr = new ExpressionTreeNode(new Argument(Argument.VARIABLE, buf.toString()));
			}	
		}
		else
		{
			/*
			 * It's nothing that we understand.
			 */
			throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.INVALID_EXPRESSION));
		}

		return(expr);
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
		mExprTree = parseOrBoolean(p);
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
	public Argument evaluate(ContextStack context, String interpreterFilename) throws MapyrusException
	{
		return(mExprTree.evaluate(context, interpreterFilename));
	}

	/**
	 * Returns name of variable in an expression that is only a variable name.
	 * @return variable name, or null if expression is not simply a variable name.
	 */
	public String getVariableName()
	{
		if (mExprTree.mIsLeaf && mExprTree.mLeafArg.getType() == Argument.VARIABLE)
			return(mExprTree.mLeafArg.getVariableName());
		else
			return(null);
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
			a1 = e1.evaluate(context, "test");
			a2 = e2.evaluate(context, "test");
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

