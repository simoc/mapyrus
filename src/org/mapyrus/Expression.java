/*
 * This file is part of Mapyrus.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.*;

/**
 * An expression tree.  Parser for numeric or string expression that builds
 * a binary tree holding the expression.  The expression can be later 
 * be evaluated and the tree is evaluated to a single value.
 *
 */
public class Expression
{
	/*
	 * Types of operations allowed between two numbers or strings
	 * (or two expressions).
	 */
	private static final int NO_OPERATION = 0;
	private static final int PLUS_OPERATION = 1;
	private static final int MINUS_OPERATION = 2;
	private static final int CONCAT_OPERATION = 3; /* 'qw' . 'er' . 7 = 'qwer7' */
	private static final int MULTIPLY_OPERATION = 4;	/* 'qw' * 2 = 'qwqw' */
	private static final int DIVIDE_OPERATION = 5;

	private static final int EQUALS_OPERATION = 7;
	private static final int NOT_EQUALS_OPERATION = 8;
	private static final int GREATER_THAN_OPERATION = 9;
	private static final int LESS_THAN_OPERATION = 10;
	private static final int GREATER_EQUAL_OPERATION = 11;
	private static final int LESS_EQUAL_OPERATION = 12;

	private static final int AND_OPERATION = 13;
	private static final int OR_OPERATION = 14;

	/*
	 * Names and types of functions we allow on numbers and strings.
	 */
	private static final int ROUND_FUNCTION = 1;	/* round(3.14) = 3 */
	private static final String ROUND_FUNCTION_NAME = "round";
	private static final int RANDOM_FUNCTION = 2;	/* random(3) = [0, 3) */
	private static final String RANDOM_FUNCTION_NAME = "random";
	private static final int LENGTH_FUNCTION = 3;	/* length("foo") = 3 */
	private static final String LENGTH_FUNCTION_NAME = "length";
	private static final int MATCH_FUNCTION = 4;	/* match('foobar', 'ob') = 3 */
	private static final String MATCH_FUNCTION_NAME = "match";
	private static final int SUBSTR_FUNCTION = 5;	/* substr('foobar', 2, 3) = 'oob' */
	private static final String SUBSTR_FUNCTION_NAME = "substr";

	/*
	 * Lookup tables of functions and the number of
	 * arguments that they each take.
	 */
	private static final HashMap mFunctionTypeLookup;
	private static final byte[] mFunctionArgumentCount;
	
	static
	{
		mFunctionTypeLookup = new HashMap();
		mFunctionTypeLookup.put(ROUND_FUNCTION_NAME, new Integer(ROUND_FUNCTION));
		mFunctionTypeLookup.put(RANDOM_FUNCTION_NAME, new Integer(RANDOM_FUNCTION));
		mFunctionTypeLookup.put(LENGTH_FUNCTION_NAME, new Integer(LENGTH_FUNCTION));
		mFunctionTypeLookup.put(MATCH_FUNCTION_NAME, new Integer(MATCH_FUNCTION));
		mFunctionTypeLookup.put(SUBSTR_FUNCTION_NAME, new Integer(SUBSTR_FUNCTION));
		
		mFunctionArgumentCount = new byte[SUBSTR_FUNCTION + 1];
		mFunctionArgumentCount[ROUND_FUNCTION] = 1;
		mFunctionArgumentCount[RANDOM_FUNCTION] = 1;
		mFunctionArgumentCount[LENGTH_FUNCTION] = 1;
		mFunctionArgumentCount[MATCH_FUNCTION] = 2;
		mFunctionArgumentCount[SUBSTR_FUNCTION] = 3;
	}

	/*
	 * Maximum number of compiled regular expressions we'll cache.
	 */
	private static final int MAX_COMPILED_REGEX = 100;
	
	/*
	 * Line separator replaces '\n' sequences in expressions.
	 */
	private static final String mLineSeparator = System.getProperty("line.separator");

	/*
	 * Static table of frequently used regular expressions.
	 */
	private static LRUCache mRegexCache = new LRUCache(MAX_COMPILED_REGEX);

	/**
	 * Compile a regular expression string into a Pattern that can be used for matching.
	 * Patterns are cached to avoid recomputing them again and again.
	 * Synchronized because LRUCache is not thread-safe.
	 * @param regex is regular expression to compile.
	 * @return compiled pattern
	 */
	private static synchronized Pattern compileRegex(String regex) throws MapyrusException
	{
		Pattern retval = (Pattern)(mRegexCache.get(regex));

		if (retval == null)
		{
			try
			{
				retval = Pattern.compile(regex);
			}
			catch (PatternSyntaxException e)
			{
				throw new MapyrusException(e.getMessage());
			}

			/*
			 * Cache newly compiled regular expression.
			 */
			mRegexCache.put(regex, retval);
		}
		return(retval);
	}

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
		int mFunction;
		ExpressionTreeNode mThirdFunctionExpression;

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
		 * Create a node containing a call to a function.
		 * @param functionType is identifier of function being called.
		 * @param arg1 is first argument to function.
		 * @param arg2 is second argument to function (or null for single
		 * argument functions).
		 * @param arg3 is third argument to function (or null for functions
		 * with less than 3 arguments)
		 */
		public ExpressionTreeNode(int functionType, ExpressionTreeNode arg1,
			ExpressionTreeNode arg2, ExpressionTreeNode arg3)
		{
			mIsLeaf = false;
			mIsFunction = true;
			mFunction = functionType;
			mLeftBranch = arg1;
			mRightBranch = arg2;
			mThirdFunctionExpression = arg3;
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
			Argument leftValue, rightValue, thirdValue;
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
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_UNDEFINED) +
							": " + t.mLeafArg.getVariableName());
					}
				}
				else
				{
					retval = t.mLeafArg;
				}
			}
			else if (t.mIsFunction)
			{
				/*
				 * Evaluate function.
				 */
				if (t.mFunction == ROUND_FUNCTION || t.mFunction == RANDOM_FUNCTION)
				{
					leftValue = traverse(t.mLeftBranch, context);
					if (leftValue.getType() != Argument.NUMERIC)
					{
						String message = MapyrusMessages.get(MapyrusMessages.NUMERIC_FUNCTION) + ": ";
						if (t.mFunction == ROUND_FUNCTION)
							throw new MapyrusException(message + ROUND_FUNCTION_NAME);
						else
							throw new MapyrusException(message + RANDOM_FUNCTION_NAME);
					}
					
					if (t.mFunction == ROUND_FUNCTION)
						d = Math.round(leftValue.getNumericValue());
					else
						d = Math.random() * leftValue.getNumericValue();
					retval = new Argument(d);
				}
				else if (t.mFunction == LENGTH_FUNCTION)
				{
					leftValue = traverse(t.mLeftBranch, context);
					retval = new Argument(leftValue.toString().length());
				}
				else if (t.mFunction == MATCH_FUNCTION)
				{
					leftValue = traverse(t.mLeftBranch, context);
					rightValue = traverse(t.mRightBranch, context);

					/*
					 * Find index of start of regular expression in string.
					 */
					Pattern pattern = compileRegex(rightValue.toString());
					Matcher matcher = pattern.matcher(leftValue.toString());
					if (matcher.find())
						retval = new Argument(matcher.start() + 1);
					else
						retval = Argument.numericZero;
				}
				else /* SUBSTR_FUNCTION */
				{
					String s;
					int startIndex, extractLen, len;

					leftValue = traverse(t.mLeftBranch, context);
					s = leftValue.toString();
					rightValue = traverse(t.mRightBranch, context);
					thirdValue = traverse(t.mThirdFunctionExpression, context);
					if (rightValue.getType() != Argument.NUMERIC ||
						thirdValue.getType() != Argument.NUMERIC)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NUMERIC_FUNCTION) +
							": " + SUBSTR_FUNCTION_NAME);
					}
					
					/*
					 * Convert to zero-based indexing used by java.
					 */
					startIndex = (int)(Math.floor(rightValue.getNumericValue()));
					startIndex--;
					if (startIndex < 0)
						startIndex = 0;
					extractLen = (int)(Math.floor(thirdValue.getNumericValue()));
					
					len = s.length();
					if (extractLen < 1 || startIndex >= len)
					{
						/*
						 * Substring is totally to the left or right of
						 * the string.  So substring is empty. 
						 */
						retval = Argument.emptyString;
					}
					else
					{
						if (startIndex + extractLen > len)
							extractLen = len - startIndex;

						retval = new Argument(Argument.STRING,
							s.substring(startIndex, startIndex + extractLen));
					}
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
					Argument leftVarValue =
						context.getVariableValue(leftValue.getVariableName());
					if (leftVarValue == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_UNDEFINED) +
							": " + leftValue.getVariableName());
					}
					leftValue = leftVarValue;
				}
				if (rightValue.getType() == Argument.VARIABLE)
				{
					Argument rightVarValue =
						context.getVariableValue(rightValue.getVariableName());
					if (rightVarValue == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_UNDEFINED) +
							": " + rightValue.getVariableName());
					}
					rightValue = rightVarValue;
				}

				/*
				 * Check types for operation.  Concatenating a string and numbers is OK
				 * and so is multiplying a string.  But everything else requires matching
				 * types.
				 */
				op = t.mOperation;
				if (op == CONCAT_OPERATION)
				{
					/*
					 * Different types can be concatenated.
					 */
				}
				else if (op == MULTIPLY_OPERATION && leftValue.getType() == Argument.STRING)
				{
					if (rightValue.getType() != Argument.NUMERIC)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.WRONG_TYPES));
					}
				}
				else if (leftValue.getType() != rightValue.getType())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.WRONG_TYPES));
				}
				
				/*
				 * Do string and numeric operations separately.
				 */
				if (leftValue.getType() == Argument.NUMERIC)
				{
					if (op == CONCAT_OPERATION)
					{
						retval = new Argument(Argument.STRING,
							leftValue.toString() + rightValue.toString());
					}
					else
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
						else if (op == EQUALS_OPERATION)
							d = (l == r) ? 1 : 0;
						else if (op == NOT_EQUALS_OPERATION)
							d = (l != r) ? 1 : 0;
						else if (op == GREATER_THAN_OPERATION)
							d = (l > r) ? 1 : 0;
						else if (op == GREATER_EQUAL_OPERATION)
							d = (l >= r) ? 1 : 0;
						else if (op == LESS_THAN_OPERATION)
							d = (l < r) ? 1 : 0;
						else if (op == LESS_EQUAL_OPERATION)
							d = (l <= r) ? 1 : 0;
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
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_NUMERIC_OPERATION));
						}
	
						/*
						 * Fail on numeric overflow and divide by zero.
						 */
						if (d == Double.NEGATIVE_INFINITY || d == Double.POSITIVE_INFINITY || d == Double.NaN)
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NUMERIC_OVERFLOW));
	
						if (d == 0.0)
							retval = Argument.numericZero;
						else if (d == 1.0)
							retval = Argument.numericOne;
						else
							retval = new Argument(d);
					}
				}
				else
				{
					StringBuffer s;
					String l = leftValue.getStringValue();
					if (op == MULTIPLY_OPERATION)
					{
						/*
						 * Repeat string N times.
						 */
						s = new StringBuffer();
						for (int i = 0; i < rightValue.getNumericValue(); i++)
						{
							s.append(l);
						}
						retval = new Argument(Argument.STRING, s.toString());
					}
					else if (op == CONCAT_OPERATION)
					{
						/*
						 * Add whatever is on right-hand side to string.
						 */
						String r = rightValue.toString();
						retval = new Argument(Argument.STRING, l + r);
					}
					else
					{
						String r = rightValue.getStringValue();
						if (op == EQUALS_OPERATION)
							d = l.equals(r) ? 1 : 0;
						else if (op == NOT_EQUALS_OPERATION)
							d = l.equals(r) ? 0 : 1;
						else if (op == GREATER_THAN_OPERATION)
							d = (l.compareTo(r) > 0) ? 1 : 0;
						else if (op == GREATER_EQUAL_OPERATION)
							d = (l.compareTo(r) >= 0) ? 1 : 0;
						else if (op == LESS_THAN_OPERATION)
							d = (l.compareTo(r) < 0) ? 1 : 0;
						else if (op == LESS_EQUAL_OPERATION)
							d = (l.compareTo(r) <= 0) ? 1 : 0;
						else
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_STRING_OPERATION));
						}
						
						if (d == 0.0)
							retval = Argument.numericZero;
						else if (d == 1.0)
							retval = Argument.numericOne;
						else
							retval = new Argument(d);
					}
				}
			}
			return(retval);
		}
	}

	ExpressionTreeNode mExprTree;

	/*
	 * Parse expression including "and" boolean operations.
	 */
	private ExpressionTreeNode parseAndBoolean(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, b;
		int op1, op2, op3;

		expr = parseOrBoolean(p);
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
						b = parseOrBoolean(p);
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
	 * Parse expression including "or" boolean operations.
	 */
	private ExpressionTreeNode parseOrBoolean(Preprocessor p)
		throws IOException, MapyrusException
	{
		ExpressionTreeNode expr, comp;
		int op1, op2;

		expr = parseComparison(p);
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
					comp = parseComparison(p);
					expr = new ExpressionTreeNode(expr, OR_OPERATION, comp);
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
			if (op1 == '<' || op1 == '>' || op1 == '!')
			{
				op2 = p.read();
				if (op2 == '=')
				{
					if (op1 == '!')
						opType = NOT_EQUALS_OPERATION;
					else if (op1 == '<')
						opType = LESS_EQUAL_OPERATION;
					else
						opType = GREATER_EQUAL_OPERATION;
				}
				else
				{
					p.unread(op2);

					if (op1 == '<')
						opType = LESS_THAN_OPERATION;
					else if (op1 == '>')
						opType = GREATER_THAN_OPERATION;
				}
			}
			else if (op1 == '=')
			{
				opType = EQUALS_OPERATION;
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
		boolean parsedDigit;
		StringBuffer buf = new StringBuffer();
		ExpressionTreeNode expr;
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
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
				}

				if (lastC == '\\')
				{
					/*
					 * '\\' compressed to single backslash,
					 * '\n' converted to a newline.  Escaping
					 * of other characters is ignored.
					 */
					if (c == 'n')
						buf.append(mLineSeparator);
					else
						buf.append((char)c);
				}
				else if (c != '\\')
				{
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

			if (hasUnaryMinus)
				d = -d;

			return(new ExpressionTreeNode(new Argument(d)));
		}

		if (c == -1)
		{
			throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}

		if (c == '(')
		{
			expr = parseAndBoolean(p);

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
			while (c != -1 && (c == '.' || c == '_' ||
				Character.isLetterOrDigit((char)c)))
			{
				buf.append((char)c);
				c = p.read();
			}

			/*
			 * Is this a function call like "round(3.14)"?
			 */
			Integer functionType = (Integer)(mFunctionTypeLookup.get(buf.toString()));
			if (functionType != null)
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

				int nArgs = mFunctionArgumentCount[functionType.intValue()];
				ExpressionTreeNode functionExpressions[] = new ExpressionTreeNode[Math.max(nArgs, 3)];

				/*
				 * Parse expression for each function argument.
				 */
				for (int i = 0; i < functionExpressions.length; i++)
					functionExpressions[i] = null;
				for (int i = 0; i < nArgs; i++)
				{
					if (i > 0)
					{
						/*
						 * Parse comma before next value.
						 */
						c = p.readNonSpace();
						if (c != ',')
						{
							throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
								": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
								": " + buf.toString());
						}
					}
					functionExpressions[i] = parseAndBoolean(p);
				}

				c = p.readNonSpace();
				if (c != ')')
				{
					throw new MapyrusException(p.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.WRONG_FUNCTION_VALUES) +
						": " + buf.toString());
				}

				expr = new ExpressionTreeNode(functionType.intValue(),
					functionExpressions[0], functionExpressions[1], functionExpressions[2]);
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

		if (hasUnaryMinus)
		{
			/*
			 * Expand expression to negate value.
			 */
			ExpressionTreeNode left = new ExpressionTreeNode(new Argument(-1.0));
			expr = new ExpressionTreeNode(left, MULTIPLY_OPERATION, expr);
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
		mExprTree = parseAndBoolean(p);
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

