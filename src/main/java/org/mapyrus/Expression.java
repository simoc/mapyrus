/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

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
	private static final int PRE_INCREMENT_OPERATION = 401;	/* ++a */
	private static final int PRE_DECREMENT_OPERATION = 402;	/* --a */
	private static final int POST_INCREMENT_OPERATION = 403;	/* a++ */
	private static final int POST_DECREMENT_OPERATION = 404;	/* a-- */

	private static final int AND_OPERATION = 500;
	private static final int OR_OPERATION = 501;
	private static final int NOT_OPERATION = 502;
	
	private static final int HASHMAP_REFERENCE = 600;		/* a[77] */

	/*
	 * Nodes in binary tree describing an arithmetic expression.
	 */
	private class ExpressionTreeNode
	{
		boolean m_isLeaf;
		Argument m_leafArg;

		int m_operation;
		ArrayList<ExpressionTreeNode> m_branches;

		boolean m_isFunction;
		Function m_function;

		/**
		 * Create a leaf value containing either a number,
		 * string or variable name.
		 * @param arg is leaf argument value.
		 */
		public ExpressionTreeNode(Argument arg)
		{
			m_isLeaf = true;
			m_isFunction = false;
			m_leafArg = arg;
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
			m_isLeaf = m_isFunction = false;			
			m_branches = new ArrayList<ExpressionTreeNode>(2);
			m_branches.add(left);
			m_branches.add(right);
			m_operation = operation;
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
			m_isLeaf = m_isFunction = false;
			m_branches = new ArrayList<ExpressionTreeNode>(3);
			m_branches.add(left);
			m_branches.add(right);
			m_branches.add(third);
			m_operation = operation;
		}

		/**
		 * Create a node containing a hash map.
		 * @param keyValuePairs is list of key, value pairs in hash map.
		 */
		public ExpressionTreeNode(ArrayList<ExpressionTreeNode> keyValuePairs)
		{
			m_isFunction = false;
			m_isLeaf = true;
			m_leafArg = null;
			m_branches = keyValuePairs;
		}

		/**
		 * Create a node containing a call to a function.
		 * @param func is function being called.
		 * @param functionType is identifier of function being called.
		 * @param args list of ExpressionTreeNode arguments to function.
		 */
		public ExpressionTreeNode(Function func, ArrayList<ExpressionTreeNode> args)
		{
			m_isLeaf = false;
			m_isFunction = true;
			m_function = func;
			m_branches = args;
		}

		/**
		 * Evaluate binary tree expression..
		 * @param context variable definitions and other context information.
		 * @param interpreterFilename name of file being interpreted.
		 * @return numeric or string value of the expression.
		 * @throws MapyrusException if expression cannot be evaluated.
		 * @throws InterruptedException if evaluation is interrupted.
		 */
		public Argument evaluate(ContextStack context, String interpreterFilename)
			throws MapyrusException, InterruptedException
		{
			return(traverse(this, context, interpreterFilename));
		}

		/**
		 * Evaluate internal function.
		 * @param context is context containing variable values
		 * @param interpreterFilename name of file being interpreted.
		 * @return result of function
		 */
		private Argument evaluateFunction(ContextStack context,
			String interpreterFilename)
			throws MapyrusException, InterruptedException
		{
			int nArgs = m_branches.size();
			ArrayList<Argument> values = new ArrayList<Argument>(nArgs);
			Argument retval;

			/*
			 * Evaluate each of the arguments being passed to the function.
			 */
			for (int i = 0; i < nArgs; i++)
			{
				ExpressionTreeNode branch = (ExpressionTreeNode)m_branches.get(i);
				values.add(traverse(branch, context, interpreterFilename));
			}

			/*
			 * Evaluate function.
			 */
			try
			{
				retval = m_function.evaluate(context, values);
			}
			catch (MapyrusException e)
			{
				/*
				 * Prepend function name to error message.
				 */
				throw new MapyrusException(m_function.getName() + ": " + e.getMessage());
			}

			return(retval);
		}

		/*
		 * Recursively traverse binary expression tree to
		 * determine its value.
		 */
		private Argument traverse(ExpressionTreeNode t, ContextStack context,
			String interpreterFilename) throws MapyrusException, InterruptedException
		{
			Argument retval;
			Argument leftValue, rightValue;
			int returnType = Argument.NUMERIC;
			double l, r, d = 0.0;
			String s = null;

			if (t.m_isLeaf)
			{
				if (t.m_leafArg == null)
				{
					retval = traverseArray(t.m_branches, context, interpreterFilename);
				}
				else if (t.m_leafArg.getType() == Argument.VARIABLE)
				{
					/*
					 * Evaluate variable name.  Variables that are not assigned
					 * are given the value of an empty string (which converts to the
					 * numeric value 0), like in awk(1) and Perl.
					 */
					retval = context.getVariableValue(t.m_leafArg.getVariableName(), interpreterFilename);
					if (retval == null)
						retval = Argument.emptyString;
				}
				else
				{
					retval = t.m_leafArg;
				}
			}
			else if (t.m_isFunction)
			{
				retval = t.evaluateFunction(context, interpreterFilename);
			}
			else if (t.m_operation == NOT_OPERATION)
			{
				/*
				 * Negation operates on a single expression.
				 */
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)t.m_branches.get(0);
				leftValue = traverse(leftBranch, context, interpreterFilename);
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
			else if (t.m_operation == ASSIGN_OPERATION || t.m_operation == PRE_INCREMENT_OPERATION ||
				t.m_operation == PRE_DECREMENT_OPERATION || t.m_operation == POST_INCREMENT_OPERATION ||
				t.m_operation == POST_DECREMENT_OPERATION)
			{
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)t.m_branches.get(0);
				Argument varValue = null;

				if (t.m_operation == PRE_INCREMENT_OPERATION)
				{
					rightValue = traverse(leftBranch, context, interpreterFilename);
					rightValue = new Argument(rightValue.getNumericValue() + 1);
				}
				else if (t.m_operation == PRE_DECREMENT_OPERATION)
				{
					rightValue = traverse(leftBranch, context, interpreterFilename);
					rightValue = new Argument(rightValue.getNumericValue() - 1);
				}
				else if (t.m_operation == POST_INCREMENT_OPERATION)
				{
					varValue = traverse(leftBranch, context, interpreterFilename);
					if (varValue == Argument.emptyString)
						varValue = Argument.numericZero;
					rightValue = new Argument(varValue.getNumericValue() + 1);
				}
				else if (t.m_operation == POST_DECREMENT_OPERATION)
				{
					varValue = traverse(leftBranch, context, interpreterFilename);
					if (varValue == Argument.emptyString)
						varValue = Argument.numericZero;
					rightValue = new Argument(varValue.getNumericValue() - 1);
				}
				else
				{
					ExpressionTreeNode rightBranch = (ExpressionTreeNode)t.m_branches.get(1);
					rightValue = traverse(rightBranch, context, interpreterFilename);
				}
				if (leftBranch.m_isLeaf)
				{
					/*
					 * Simple assignment: a = b.
					 */
					String varName = leftBranch.m_leafArg.getVariableName();
					context.defineVariable(varName, rightValue);
				}
				else if (leftBranch.m_operation == HASHMAP_REFERENCE &&
					((ExpressionTreeNode)leftBranch.m_branches.get(0)).m_isLeaf)
				{
					/*
					 * Assign value as entry in a hashmap: a[55] = "foo".
					 */
					ExpressionTreeNode leftBranchVar = leftBranch.m_branches.get(0);
					String hashMapName = null;
					if (leftBranchVar != null)
						hashMapName = leftBranchVar.m_leafArg.getVariableName();
					if (hashMapName == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_VARIABLE));
					}
					Argument key = traverse(((ExpressionTreeNode)leftBranch.m_branches.get(1)), context,
						interpreterFilename);
					if (key.getType() != Argument.NUMERIC && key.getType() != Argument.STRING)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HASHMAP_KEY));
					}
					if (rightValue.getType() == Argument.HASHMAP)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NESTED_HASHMAP));
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
				if (t.m_operation == POST_INCREMENT_OPERATION || t.m_operation == POST_DECREMENT_OPERATION)
					retval = varValue;
				else
					retval = rightValue;
			}
			else if (t.m_operation == HASHMAP_REFERENCE)
			{
				Argument hashMapVar;

				/*
				 * Lookup an individual entry in a hash map from a hash map
				 * variable name and key.
				 */
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)t.m_branches.get(0);
				ExpressionTreeNode rightBranch = (ExpressionTreeNode)t.m_branches.get(1);
				if (leftBranch.m_isLeaf && leftBranch.m_leafArg != null &&
					leftBranch.m_leafArg.getType() == Argument.VARIABLE)
				{
					String varName = leftBranch.m_leafArg.getVariableName();
					hashMapVar = context.getVariableValue(varName, interpreterFilename);
				}
				else if (leftBranch.m_isLeaf && leftBranch.m_leafArg == null)
				{
					hashMapVar = traverseArray(leftBranch.m_branches, context, interpreterFilename);
				}
				else if (leftBranch.m_isFunction)
				{
					hashMapVar = traverse(leftBranch, context, interpreterFilename);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
				}

				Argument key = traverse(rightBranch, context, interpreterFilename);
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
			else if (t.m_operation == CONDITIONAL_OPERATION)
			{
				/*
				 * Test condition and return value for true, or value for false.
				 */
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)t.m_branches.get(0);
				
				leftValue = traverse(leftBranch, context, interpreterFilename);
				if (leftValue.getNumericValue() != 0)
				{
					ExpressionTreeNode rightBranch = (ExpressionTreeNode)t.m_branches.get(1);
					retval = traverse(rightBranch, context, interpreterFilename);
				}
				else
				{
					ExpressionTreeNode thirdBranch = (ExpressionTreeNode)t.m_branches.get(2);
					retval = traverse(thirdBranch, context, interpreterFilename);
				}
			}
			else
			{
				/*
				 * Either expression can be a number or a string.
				 */
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)t.m_branches.get(0);
				ExpressionTreeNode rightBranch = (ExpressionTreeNode)t.m_branches.get(1);
				leftValue = traverse(leftBranch, context, interpreterFilename);
				rightValue = traverse(rightBranch, context, interpreterFilename);

				switch (t.m_operation)
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
					StringBuilder sb = new StringBuilder();
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
		
		private Argument traverseArray(ArrayList<ExpressionTreeNode> expressions,
				ContextStack context,
				String interpreterFilename) throws MapyrusException, InterruptedException
		{
			Argument retval = new Argument();
			int i = 0;
			while (i < expressions.size())
			{
				/*
				 * Evaluate all key and value pairs in array.
				 */
				ExpressionTreeNode expr =  expressions.get(i);
				Argument key = traverse(expr, context, interpreterFilename);
				expr = expressions.get(i + 1);
				Argument value = traverse(expr, context, interpreterFilename);
				if (value.getType() == Argument.HASHMAP)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NESTED_HASHMAP));
				retval.addHashMapEntry(key.getStringValue(), value);

				i += 2;
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
			StringBuilder sb;
			
			if (m_isLeaf)
			{
				if (m_leafArg != null)
				{
					retval = m_leafArg.toString();
				}
				else
				{
					sb = new StringBuilder("[");
					String delimiter = "";
					boolean isKey = true;
					for (ExpressionTreeNode node : m_branches)
					{
						if (isKey)
						{
							sb.append(delimiter);
							delimiter = ",";
						}
						else
						{
							sb.append(":");
						}
						sb.append("'");
						sb.append(node.toString());
						sb.append("'");
						isKey = !isKey;
					}
					sb.append("]");
					return(sb.toString());
				}
			}
			else
			{
				String operation = "";
				switch (m_operation)
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
					case PRE_INCREMENT_OPERATION:
					case POST_INCREMENT_OPERATION:
						operation = "++";
						break;
					case PRE_DECREMENT_OPERATION:
					case POST_DECREMENT_OPERATION:
						operation = "--";
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
				
				sb = new StringBuilder();
				ExpressionTreeNode leftBranch = (ExpressionTreeNode)m_branches.get(0);
				if (m_operation == PRE_INCREMENT_OPERATION || m_operation == PRE_DECREMENT_OPERATION)
				{
					sb.append(operation);
					operation = "";
				}
				sb.append(leftBranch.toString());
				sb.append(' ');
				sb.append(operation);
				sb.append(' ');

				if (!(m_operation == NOT_OPERATION || m_operation == PRE_INCREMENT_OPERATION || m_operation == PRE_DECREMENT_OPERATION))
				{
					ExpressionTreeNode rightBranch = (ExpressionTreeNode)m_branches.get(1);
					sb.append(rightBranch.toString());
				}
				if (m_operation == HASHMAP_REFERENCE)
					sb.append(']');
				if (m_operation == CONDITIONAL_OPERATION)
				{
					ExpressionTreeNode thirdBranch = (ExpressionTreeNode)m_branches.get(2);
					sb.append(" : ").append(thirdBranch.toString());
				}
				retval = sb.toString();
			}
			return(retval);
		}
	}

	private ExpressionTreeNode m_exprTree;

	/*
	 * Parse expression including assignment to variables.
	 */
	private ExpressionTreeNode parseAssignment(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, value;
		int op1, op2;

		expr = parseOrBoolean(p, userFunctions);
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
					if (expr.m_isLeaf)
					{
						if (expr.m_leafArg == null ||
							expr.m_leafArg.getType() != Argument.VARIABLE)
						{
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
								MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
						}
					}
					else if (expr.m_operation != HASHMAP_REFERENCE)
					{
						throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
								MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
					}
					value = parseOrBoolean(p, userFunctions);
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
	 * Parse expression including "or" boolean operations.
	 */
	private ExpressionTreeNode parseOrBoolean(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, b;
		int op1, op2;

		expr = parseAndBoolean(p, userFunctions);
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
					b = parseAndBoolean(p, userFunctions);
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
	private ExpressionTreeNode parseAndBoolean(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, b;
		int op1, op2, op3;

		expr = parseNotBoolean(p, userFunctions);
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
						b = parseNotBoolean(p, userFunctions);
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
	private ExpressionTreeNode parseNotBoolean(Preprocessor p, HashMap<String, UserFunction> userFunctions)
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
						b = parseNotBoolean(p, userFunctions);
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
			expr = parseConditional(p, userFunctions);
		return(expr);
	}

	/*
	 * Parse conditional expression, like: hour < 12 ? "AM" : "PM"
	 */
	private ExpressionTreeNode parseConditional(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, trueExpr, falseExpr;
		int op;

		expr = parseComparison(p, userFunctions);
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
				trueExpr = parseAssignment(p, userFunctions);
				op = p.readNonSpace();
				if (op == ':')
				{
					falseExpr = parseAssignment(p, userFunctions);
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
	private ExpressionTreeNode parseComparison(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, cond;
		int op1, op2;
		int opType;

		cond = parseExpression(p, userFunctions);
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
				expr = parseExpression(p, userFunctions);
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
	private ExpressionTreeNode parseExpression(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, term;
		int op;

		expr = parseTerm(p, userFunctions);
		while (true)
		{
			op = p.readNonSpace();
			if (op == '+' || op == '-' || op == '.')
			{
				term = parseTerm(p, userFunctions);
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
	private ExpressionTreeNode parseTerm(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode term, factor;
		int op;

		term = parseUnary(p, userFunctions);
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

				factor = parseUnary(p, userFunctions);
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
	private ExpressionTreeNode parseUnary(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr;
		int op1, op2;

		op1 = p.readNonSpace();
		if (op1 != '+' && op1 != '-')
		{
			p.unread(op1);
		}
		else
		{
			/*
			 * Ignore if it is '++n' or '--n' instead of a unary.
			 */
			op2 = p.read();
			p.unread(op2);
			if (op2 == op1)
			{
				p.unread(op1);
				op1 = ' ';
			}
		}
		expr = parsePlusPlus(p, userFunctions);
		if (op1 == '-')
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
	 * Parse expression including '++' increment or '--' decrement.
	 */
	private ExpressionTreeNode parsePlusPlus(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr;
		int op1, op2, type = NO_OPERATION;

		/*
		 * Check for '++' or '--' before variable.
		 */
		op1 = p.readNonSpace();
		if (op1 == '+' || op1 == '-')
		{
			op2 = p.read();
			if (op2 == op1)
			{
				type = (op1 == '+' ? PRE_INCREMENT_OPERATION : PRE_DECREMENT_OPERATION);
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
		expr = parseHashMapReference(p, userFunctions);

		/*
		 * Check for '++' or '--' after variable.
		 */
		op1 = p.readNonSpace();
		if (op1 == '+' || op1 == '-')
		{
			op2 = p.read();
			if (op2 == op1)
			{
				if (type != NO_OPERATION)
				{
					/*
					 * '++a++' is not allowed.
					 */
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
						MapyrusMessages.get(MapyrusMessages.INVALID_EXPRESSION));
				}
				type = (op1 == '+' ? POST_INCREMENT_OPERATION : POST_DECREMENT_OPERATION);
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
		if (type != NO_OPERATION)
		{
			/*
			 * Make sure expression is a variable.  Things like '++5' are not allowed.
			 */
			if (!((expr.m_isLeaf && expr.m_leafArg.getType() == Argument.VARIABLE) ||
				expr.m_operation == HASHMAP_REFERENCE))
			{
				throw new MapyrusException(p.getCurrentFilenameAndLineNumber() + ": " +
					MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
			}
			expr = new ExpressionTreeNode(expr, type, null);
		}
		return(expr);
	}

	/*
	 * Parse expression including reference to an element in a hashmap.
	 */
	private ExpressionTreeNode parseHashMapReference(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, keyExpr;
		int op1;

		expr = parseArray(p, userFunctions);
		while (true)
		{
			/*
			 * If next character is '[' then parse hashmap key, then closing ']'.
			 */
			op1 = p.readNonSpace();
			if (op1 == '[')
			{
				keyExpr = parseAssignment(p, userFunctions);
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
	 * Parse array expression: [11, 22, "hello"] or {'name':'fred', 'age': 22}.
	 */
	private ExpressionTreeNode parseArray(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr;
		int op1;

		/*
		 * Check for '[' or '{' marking start of array.
		 */
		op1 = p.readNonSpace();
		if (op1 == '[' || op1 == '{')
		{
			boolean requiresKeys = (op1 == '{');

			/*
			 * Read each entry in array.
			 */
			ArrayList<ExpressionTreeNode> keyValuePairs = new ArrayList<ExpressionTreeNode>();
			op1 = p.readNonSpace();
			if (((!requiresKeys) && op1 == ']') || (requiresKeys && op1 == '}'))
			{
				/*
				 * Empty array.
				 */
			}
			else
			{
				p.unread(op1);
				int i = 1;
				//boolean hasKeys = true;
				do
				{
					expr = parseAssignment(p, userFunctions);
					op1 = p.readNonSpace();
					if (requiresKeys && op1 == ':')
					{
						if (expr.m_isLeaf && expr.m_leafArg == null)
						{
							/*
							 * Key must be a simple value, not an array.
							 */
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
									": " + MapyrusMessages.get(MapyrusMessages.NESTED_HASHMAP));
						}
						keyValuePairs.add(expr);

						expr = parseAssignment(p, userFunctions);						
						if (expr.m_isLeaf && expr.m_leafArg == null)
						{
							/*
							 * Do not allow nested arrays.
							 */
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
									": " + MapyrusMessages.get(MapyrusMessages.NESTED_HASHMAP));
						}
						keyValuePairs.add(expr);
					}
					else if (!requiresKeys)
					{
						if (expr.m_isLeaf && expr.m_leafArg == null)
						{
							/*
							 * Do not allow nested arrays.
							 */
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
									": " + MapyrusMessages.get(MapyrusMessages.NESTED_HASHMAP));
						}

						p.unread(op1);
						String key = Integer.toString(i);
						keyValuePairs.add(new ExpressionTreeNode(new Argument(Argument.STRING, key)));
						keyValuePairs.add(expr);
					}
					else
					{
						throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
							": " + MapyrusMessages.get(MapyrusMessages.INVALID_ARRAY));
					}

					i++;
					op1 = p.readNonSpace();
				}
				while (op1 == ',');

				/*
				 * Check that array ends with matching ']' or '}'.
				 */
				if (((!requiresKeys) && op1 != ']') || (requiresKeys && op1 != '}'))
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.INVALID_ARRAY));
				}
			}

			expr = new ExpressionTreeNode(keyValuePairs);
		}
		else
		{
			p.unread(op1);
			expr = parseFactor(p, userFunctions);
		}
		return(expr);
	}

	/*
	 * Parse a single number, string or variable name.
	 */
	private ExpressionTreeNode parseFactor(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		boolean parsedDigit;
		StringBuilder buf = new StringBuilder();
		ExpressionTreeNode expr;
		int c, quote;

		boolean inOctalCode;
		boolean inEscape;
		boolean inUnicode;
		int octalCode;
		int unicode;
		int nOctalDigits;
		int nUnicodeDigits;

		c = p.readNonSpace();
		if (c == '\'' || c == '"' || c == '\u2019')
		{
			/*
			 * It's a quoted string.  Keep reading up until matching quote.
			 */
			quote = c;
			inOctalCode = inEscape = inUnicode = false;
			octalCode = nOctalDigits = 0;
			unicode = nUnicodeDigits = 0;
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
					if (Character.isDigit((char)c) && c < '8' && nOctalDigits < 3)
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
				else if (inUnicode)
				{
					/*
					 * Reading a 4 digit hexadecimal Unicode character code.
					 */
					if (nUnicodeDigits < 4)
					{
						int hex;
						if (c >= '0' && c <= '9')
							hex = c - '0';
						else if (c >= 'A' && c <= 'F')
							hex = c - 'A' + 10;
						else if (c >= 'a' && c <= 'f')
							hex = c - 'a' + 10;
						else
						{
							String s = "";
							if (unicode != 0)
								s = Integer.toHexString(unicode);
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_UNICODE) +
								": " + s + (char)c);
						}

						unicode <<= 4;
						unicode += hex;
						nUnicodeDigits++;
					}
					else
					{
						buf.append((char)unicode);
						inUnicode = false;
					}
				}

				if (!(inOctalCode || inUnicode))
				{
					if (inEscape)
					{
						/*
						 * '\\' compressed to single backslash,
						 * '\n' converted to a newline, '\r' is stripped -- it
						 * is not useful since we use Java's line separator internally.
						 * '\t" is converted to a tab.
						 * '\367' is converted to character code (3 * 64) + (6 * 8) + 7.
						 * '\u20AC' is converted to a Unicode character.
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
							if (c >= '8')
							{
								throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_OCTAL) +
									": \\" + (char)c);
							}
							inOctalCode = true;
							octalCode = c - '0';
							nOctalDigits = 1;
						}
						else if (c == 'u')
						{
							inUnicode = true;
							nUnicodeDigits = 0;
							unicode = 0;
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
			 * Add any character we were building as octal or unicode when quoted string finished.
			 */
			if (inOctalCode)
				buf.append((char)octalCode);

			if (inUnicode)
			{
				if (nUnicodeDigits == 4)
					buf.append((char)unicode);
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_UNICODE) +
						": " + Integer.toHexString(unicode));
				}
			}

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
			expr = parseAssignment(p, userFunctions);

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
			 * Is this a function call like "round(3.14)"
			 * or a function defined by the user?
			 */
			Function f = FunctionTable.getFunction(buf.toString());
			if (f == null)
				f = (Function)userFunctions.get(buf.toString());
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
				ArrayList<ExpressionTreeNode> functionExpressions = new ArrayList<ExpressionTreeNode>(4);

				for (int i = 0; i < maxArgs; i++)
				{
					/*
					 * Parse comma before next value, or closing bracket.
					 */
					c = p.readNonSpace();
					if (c == ')')
					{
						if (i < minArgs)
						{
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
								": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
								": " + buf.toString());
						}
						p.unread(c);
						break;
					}
					else if (i == 0)
					{
						p.unread(c);
					}
					else if (c != ',')
					{
						throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
							": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
							": " + buf.toString());
					}

					ExpressionTreeNode funcExpr = parseAssignment(p, userFunctions);
					functionExpressions.add(funcExpr);
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

				expr = new ExpressionTreeNode(f, functionExpressions);
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
	 * @param userFunctions user-defined functions.
	 * @throws IOException if reading from preprocessor fails.
	 * @throws MapyrusException if expression is not valid.
	 */
	public Expression(Preprocessor p, HashMap<String, UserFunction> userFunctions)
		throws IOException, MapyrusException
	{
		m_exprTree = parseAssignment(p, userFunctions);
	}

	/**
	 * Evaluate an expression.
	 * @param context all currently defined variables and their values.
	 * @param interpreterFilename name of file being intrepreted (for error messages).
	 * @return the evaluated expression, either a string or a number.
	 * @throws MapyrusException if expression cannot be evaluated.
	 * @throws InterruptedException if evaluation is interrupted.
	 */
	public Argument evaluate(ContextStack context, String interpreterFilename)
		throws MapyrusException, InterruptedException
	{
		return(m_exprTree.evaluate(context, interpreterFilename));
	}

	/**
	 * Returns name of variable in an expression that is only a variable name.
	 * @return variable name, or null if expression is not simply a variable name.
	 */
	public String getVariableName()
	{
		if (m_exprTree.m_isLeaf && m_exprTree.m_leafArg.getType() == Argument.VARIABLE)
			return(m_exprTree.m_leafArg.getVariableName());
		else
			return(null);
	}

	public String toString()
	{
		return m_exprTree.toString();
	}

	public static void main(String []args)
	{
		try
		{
			Preprocessor p;
			Expression e1, e2;
			ContextStack context = new ContextStack();
			HashMap<String, UserFunction> userFunctions = new HashMap<String, UserFunction>();
			Argument a1, a2;

			context.defineVariable("pi", new Argument(3.1415));
			
			/*
			 * Read two expressions separated by a comma or newline.
			 */
			p = new Preprocessor(args[0]);
			e1 = new Expression(p, userFunctions);
			p.read();
			e2 = new Expression(p, userFunctions);
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
		catch (InterruptedException e)
		{
		}
		catch (MapyrusException e)
		{
		}
	}
}

