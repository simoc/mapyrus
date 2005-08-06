/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.StringReader;
import java.util.StringTokenizer;

/**
 * Language interpreter.  Parse and executes commands read from file, or
 * typed by user.
 *
 * May be called repeatedly to interpret several files in the same context.
 */
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
	private static final String ELIF_KEYWORD = "elif";
	private static final String ENDIF_KEYWORD = "endif";

	/*
	 * Keywords for while ... do ... done and
	 * repeat ... do ... doneblocks.
	 */
	private static final String WHILE_KEYWORD = "while";
	private static final String REPEAT_KEYWORD = "repeat";
	private static final String DO_KEYWORD = "do";
	private static final String DONE_KEYWORD = "done";

	/*
	 * Keywords for for ... in ... do ... done block.
	 */
	private static final String FOR_KEYWORD = "for";
	private static final String IN_KEYWORD = "in";

	/*
	 * Keyword to return from procedure block.
	 */
	private static final String RETURN_KEYWORD = "return";

	/*
	 * States during parsing statements.
	 */
	private static final int AT_ARGUMENT = 1;		/* at start of arguments to a statement */
	private static final int AT_SEPARATOR = 2;	/* at separator between arguments */

	private static final int AT_PARAM = 3;	/* at parameter to a procedure block */
	private static final int AT_PARAM_SEPARATOR = 4;	/* at separator between parameters */

	/*
	 * Literals for linestyles.
	 */
	public static final String CAP_BUTT_STRING = "butt";
	public static final String CAP_ROUND_STRING = "round";
	public static final String CAP_SQUARE_STRING = "square";
	public static final String JOIN_BEVEL_STRING = "bevel";
	public static final String JOIN_MITER_STRING = "miter";
	public static final String JOIN_ROUND_STRING = "round";

	private ContextStack mContext;
	private PrintStream mStdoutStream;
	private InputStream mStdinStream;

	/*
	 * Evaluted arguments for statement currently being executed.
	 * A large number of statements will be executed (but only one at a
	 * time) so reusing buffer saves continually allocating a new buffers.
	 */
	Argument []mExecuteArgs;

	/*
	 * Blocks of statements for each procedure defined in
	 * this interpreter.
	 */
	private HashMap mStatementBlocks;
	
	/*
	 * Static world coordinate system units lookup table.
	 */
	private static HashMap mWorldUnitsLookup;

	static
	{
		mWorldUnitsLookup = new HashMap();
		mWorldUnitsLookup.put("m", new Integer(Context.WORLD_UNITS_METRES));
		mWorldUnitsLookup.put("metres", new Integer(Context.WORLD_UNITS_METRES));
		mWorldUnitsLookup.put("meters", new Integer(Context.WORLD_UNITS_METRES));
		mWorldUnitsLookup.put("feet", new Integer(Context.WORLD_UNITS_FEET));
		mWorldUnitsLookup.put("foot", new Integer(Context.WORLD_UNITS_FEET));
		mWorldUnitsLookup.put("ft", new Integer(Context.WORLD_UNITS_FEET));
		mWorldUnitsLookup.put("degrees", new Integer(Context.WORLD_UNITS_DEGREES));
		mWorldUnitsLookup.put("degree", new Integer(Context.WORLD_UNITS_DEGREES));
		mWorldUnitsLookup.put("deg", new Integer(Context.WORLD_UNITS_DEGREES));
	}

	/**
	 * Parses all combinations of color setting.  Sets values passed
	 * by user in graphics context.
	 * @param context graphics context to set linestyle into.
	 * @param arguments to color statement.
	 * @param nArgs number of arguments to color statement.
	 */	
	private void setColor(ContextStack context, Argument []args, int nArgs)
		throws MapyrusException
	{
		int alpha = 255;
		float decimalAlpha = 1.0f;

		if (nArgs == 1 || nArgs == 2)
		{
			String color = args[0].getStringValue();
			Color c;

			if (nArgs == 2)
			{
				/*
				 * Parse transparency value.
				 */
				decimalAlpha = (float)args[1].getNumericValue();
				if (decimalAlpha < 0.0f)
					decimalAlpha = 0.0f;
				else if (decimalAlpha > 1.0f)
					decimalAlpha = 1.0f;

				alpha = (int)Math.round(decimalAlpha * 255.0);
			}

			if (color.equals("contrast"))
			{
				context.contrastColor(alpha);
				return;
			}
			else if (color.equals("brighter"))
			{
				context.setBrighterColor(alpha);
				return;
			}
			else if (color.equals("darker"))
			{
				context.setDarkerColor(alpha);
				return;
			}

			/*
			 * Find named color of hex value in color database.
			 */
			c = ColorDatabase.getColor(color, alpha);
			if (c == null)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.COLOR_NOT_FOUND) +
					": " + color);
			}

			context.setColor(c);
		}
		else if (nArgs == 4 || nArgs == 5)
		{
			String colorType = args[0].getStringValue();
			float c1 = (float)args[1].getNumericValue();
			float c2 = (float)args[2].getNumericValue();
			float c3 = (float)args[3].getNumericValue();

			if (nArgs == 5)
			{
				/*
				 * Parse transparency value.
				 */
				decimalAlpha = (float)args[4].getNumericValue();
				if (decimalAlpha < 0.0f)
					decimalAlpha = 0.0f;
				else if (decimalAlpha > 1.0f)
					decimalAlpha = 1.0f;

				alpha = (int)Math.round(decimalAlpha * 255.0);
			}

			/*
			 * Constrain color to valid range.
			 */
			if (c2 < 0.0f)
				c2 = 0.0f;
			else if (c2 > 1.0f)
				c2 = 1.0f;

			if (c3 < 0.0f)
				c3 = 0.0f;
			else if (c3 > 1.0f)
				c3 = 1.0f;

			if (colorType.equalsIgnoreCase("hsb"))
			{
				/*
				 * Set HSB color.
				 */
				int rgb = Color.HSBtoRGB(c1, c2, c3);
				rgb = (rgb & 0xffffff);
				context.setColor(new Color(rgb | (alpha << 24), true));
			}
			else if (colorType.equalsIgnoreCase("rgb"))
			{
				if (c1 < 0.0f)
					c1 = 0.0f;
				else if (c1 > 1.0f)
					c1 = 1.0f;

				/*
				 * Set RGB color.
				 */
				context.setColor(new Color(c1, c2, c3, decimalAlpha));
			}
			else
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLOR_TYPE) +
					": " + colorType);
			}
		}
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLOR));
		}
	}

	/**
	 * Parses all combinations of linestyle setting.  Sets values passed
	 * by user, with defaults for the values they did not give.
	 * @param context graphics context to set linestyle into.
	 * @param arguments to linestyle statement.
	 * @param nArgs number of arguments to linestyle statement.
	 */	
	private void setLinestyle(ContextStack context, Argument []args, int nArgs)
		throws MapyrusException
	{
		double width = 0.1, dashPhase = 0.0;
		float dashes[] = null;
		int cap = BasicStroke.CAP_SQUARE;
		int join = BasicStroke.JOIN_MITER;

		if (nArgs == 0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINESTYLE));

		width = args[0].getNumericValue();
		if (width < 0)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINE_WIDTH) +
				": " + width);
		}

		if (nArgs >= 2)
		{
			String s = args[1].getStringValue().toLowerCase();
			if (s.equals(CAP_BUTT_STRING))
				cap = BasicStroke.CAP_BUTT;
			else if (s.equals(CAP_ROUND_STRING))
				cap = BasicStroke.CAP_ROUND;
			else if (s.equals(CAP_SQUARE_STRING))
				cap = BasicStroke.CAP_SQUARE;
			else
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_END_CAP) + ": " + s);
		}

		if (nArgs >= 3)
		{
			String s = args[2].getStringValue().toLowerCase();
			if (s.equals(JOIN_BEVEL_STRING))
				join = BasicStroke.JOIN_BEVEL;
			else if (s.equals(JOIN_MITER_STRING))
				join = BasicStroke.JOIN_MITER;
			else if (s.equals(JOIN_ROUND_STRING))
				join = BasicStroke.JOIN_ROUND;
			else
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINE_JOIN) + ": " + s);
				
		}

		if (nArgs >= 4)
		{
			dashPhase = args[3].getNumericValue();
			if (dashPhase < 0)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_DASH_PHASE) +
					": " + dashPhase);
			}
		}

		if (nArgs >= 5)
		{
			/*
			 * Build list of dash pattern values.
			 */
			dashes = new float[nArgs - 4];
			for (int i = 4; i < nArgs; i++)
			{
				dashes[i - 4] = (float)(args[i].getNumericValue());
				if (dashes[i - 4] <= 0.0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_DASH_PATTERN) +
						": " + dashes[i - 4]);
				}
			}
		}

		context.setLinestyle(width, cap, join, dashPhase, dashes);	
	}
	
	/**
	 * Parse justification for labels and then set it.
	 * @param context graphics context to set justification value into.
	 * @param justify value given in statement.
	 */
	private void setJustify(ContextStack context, String justify)
	{
		int justifyCode;
		justify = justify.toLowerCase();

		if (justify.indexOf("center") >= 0 ||
			justify.indexOf("centre") >= 0)
		{
			justifyCode = OutputFormat.JUSTIFY_CENTER;
		}
		else if (justify.indexOf("right") >= 0)
		{
			justifyCode = OutputFormat.JUSTIFY_RIGHT;
		}
		else
		{
			justifyCode = OutputFormat.JUSTIFY_LEFT;
		}
	
		if (justify.indexOf("top") >= 0)
		{
			justifyCode |= OutputFormat.JUSTIFY_TOP;
		}
		else if (justify.indexOf("middle") >= 0)
		{
			justifyCode |= OutputFormat.JUSTIFY_MIDDLE;
		}
		else
		{
			justifyCode |= OutputFormat.JUSTIFY_BOTTOM;
		}
		context.setJustify(justifyCode);
	}

	/**
	 * Parse font for labels and then set it.
	 * @param context graphics context to set font into.
	 * @param args arguments to font statement.
	 * @param nArgs number of arguments to font statement.
	 */
	private void setFont(ContextStack context, Argument []args, int nArgs)
		throws MapyrusException
	{
		double size, outlineWidth = 0.0;

		if (nArgs == 2 || nArgs == 3)
		{
			size = args[1].getNumericValue();
			if (size <= 0.0)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FONT_SIZE) +
					": " + size);
			}

			if (nArgs == 3)
			{
				String extras = args[2].getStringValue();
				StringTokenizer st = new StringTokenizer(extras);
				while (st.hasMoreTokens())
				{
					String token = st.nextToken();
					if (token.startsWith("outlinewidth="))
					{
						/*
						 * Parse line width to use for drawing outline of each
						 * letter in a label.
						 */
						String width = token.substring(13);
						try
						{
							outlineWidth = Double.parseDouble(width);
						}
						catch (NumberFormatException e)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINE_WIDTH) +
								": " + width);
						}
						if (outlineWidth < 0)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LINE_WIDTH) +
								": " + outlineWidth);
						}
					}
				}
			}

			context.setFont(args[0].getStringValue(), size, outlineWidth);
		}
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FONT));
		}
	}

	/**
	 * Draw legend for procedures used at moveto points in current path.
	 * @param st legend statement being executed.
	 * @param context contains path and output page.
	 * @param legendEntrySize size of entry in legend.
	 */
	private void displayLegend(Statement st, ContextStack context, double legendEntrySize)
		throws MapyrusException, IOException
	{
		LegendEntryList legendList = context.getLegendEntries();
		ArrayList moveTos = context.getMoveTos();

		/*
		 * Drawing legend will itself generate new legend entries.
		 * Ignore any new legend entries while the legend is being drawn.
		 */
		legendList.ignoreAdditions();

		/*
		 * Draw only as many legend entries as there are moveto points.
		 */
		long nEntries = Math.min(legendList.size(), moveTos.size());
		for (int i = 0; i < nEntries; i++)
		{
			LegendEntry entry = legendList.pop();
			String blockName = entry.getBlockName();

			Statement block = (Statement)mStatementBlocks.get(blockName);
			if (block == null)
			{
				throw new MapyrusException(st.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNDEFINED_PROC) +
					": " + blockName);
			}

			/*
			 * Check that correct number of parameters are being passed.
			 */
			ArrayList formalParameters = block.getBlockParameters();
			if (entry.getBlockArgs().length != formalParameters.size())
			{
				throw new MapyrusException(st.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.WRONG_PARAMETERS));
			}

			context.saveState(blockName);
			Point2D.Float pt = (Point2D.Float)(moveTos.get(i));
			context.setTranslation(pt.x, pt.y);

			/*
			 * Draw description label for legend entry just to the right of
			 * the symbol, line or box.
			 */
			context.clearPath();
			context.moveTo(legendEntrySize * 1.1 + 2, legendEntrySize / 2);
			String description = entry.getDescription();
			int index;
			while ((index = description.indexOf("(#)")) >= 0)
			{
				/*
				 * Replace part of legend description with number of times this
				 * entry was used.
				 */
				description = description.substring(0, index + 1) +
					entry.getReferenceCount() + description.substring(index + 2);
			}
			context.label(description);

			/*
			 * Set path to a point, line or box and then call procedure block
			 * to draw the symbol for the legend.
			 */
			context.clearPath();
			if (entry.getType() == LegendEntry.POINT_ENTRY)
			{
				/*
				 * Set path to a single point.
				 */
				context.setTranslation(legendEntrySize / 2, legendEntrySize / 2);
				context.moveTo(0.0, 0.0);
			}
			else if (entry.getType() == LegendEntry.LINE_ENTRY)
			{
				/*
				 * Set path to a horizontal line.
				 */
				context.moveTo(0.0, legendEntrySize / 2);
				context.lineTo(legendEntrySize, legendEntrySize / 2);
			}
			else if (entry.getType() == LegendEntry.ZIGZAG_ENTRY)
			{
				/*
				 * Set path to a zigzag line /\/\.
				 */
				context.moveTo(0.0, legendEntrySize / 2);
				context.lineTo(legendEntrySize / 3, legendEntrySize);
				context.lineTo(legendEntrySize * 2 / 3, 0.0);
				context.lineTo(legendEntrySize, legendEntrySize / 2);
			}
			else if (entry.getType() == LegendEntry.BOX_ENTRY)
			{
				/*
				 * Set path to a square.
				 */
				context.moveTo(0.0, 0.0);
				context.lineTo(0.0, legendEntrySize);
				context.lineTo(legendEntrySize, legendEntrySize);
				context.lineTo(legendEntrySize, 0.0);
				context.lineTo(0.0, 0.0);
			}

			/*
			 * Save additional state for boxes so that any clip region set
			 * by the procedure block is cleared before drawing outline box.
			 */
			if (entry.getType() == LegendEntry.BOX_ENTRY)
				context.saveState(blockName);
				
			makeCall(block, formalParameters, entry.getBlockArgs());

			if (entry.getType() == LegendEntry.BOX_ENTRY)
			{
				/*
				 * Draw black outline around box.
				 */
				context.restoreState();
				context.setColor(Color.BLACK);
				context.setLinestyle(0.1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0, null);
				context.stroke();
			}
			context.restoreState();
		}
		legendList.acceptAdditions();
	}

	/**
	 * Walk through a geometry, adding it to current path.
	 * @param context context containing path to add geometry to.
	 * @param coords geometry array to add to path.
	 * @param index index in geometry array at which to start walking.
	 * @return index one greater than the last element in the geometry array. 
	 */
	private int addGeometryToPath(ContextStack context, double []coords, int index)
		throws MapyrusException
	{
		int i;
		int geometryType = (int)(coords[index]);
		int nCoords = (int)(coords[index + 1]);
		index += 2;

		/*
		 * Add geometry to path.  Complex, nested geometries must be
		 * added recursively.
		 */
		switch (geometryType)
		{
			case Argument.GEOMETRY_POINT:
			case Argument.GEOMETRY_LINESTRING:
			case Argument.GEOMETRY_POLYGON:
				for (i = 0; i < nCoords; i++)
				{
					double x = coords[index + 1];
					double y = coords[index + 2];
					if (coords[index] == Argument.MOVETO)
						context.moveTo(x, y);
					else
						context.lineTo(x, y);
					index += 3;
				}
				break;
			case Argument.GEOMETRY_MULTIPOINT:
			case Argument.GEOMETRY_MULTILINESTRING:
			case Argument.GEOMETRY_MULTIPOLYGON:
			case Argument.GEOMETRY_COLLECTION:
				for (i = 0; i < nCoords; i++)
				{
					index = addGeometryToPath(context, coords, index);
				}
				break;
		}
		return(index);
	}

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
		double degrees, radius = 0.0;
		double x1, y1, x2, y2;
		double px1, py1, px2, py2;
		boolean allowDistortion;
		int units;
		double legendSize;
		String extras;

		/*
		 * If this thread was interrupted by another thread then
		 * stop execution.
		 */
		if (Thread.interrupted())
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INTERRUPTED));

		expr = st.getExpressions();
		nExpressions = expr.length;

		/*
		 * Do not evaluate variables for local statement -- we want the
		 * original list of variable names instead.
		 */
		type = st.getType();
		if (type == Statement.LOCAL)
		{
			for (int i = 0; i < nExpressions; i++)
			{
				String varName = expr[i].getVariableName();
				if (varName == null)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
				context.setLocalScope(varName);
			}
		}
		else
		{
			/*
			 * Make sure buffer we're keeping for command arguments is big
			 * enough for this command.
			 */
			if (mExecuteArgs == null || nExpressions > mExecuteArgs.length)
				mExecuteArgs = new Argument[nExpressions];

			/*
			 * Evaluate each of the expressions for this statement.
			 */
			String interpreterFilename = st.getFilename();
			for (int i = 0; i < nExpressions; i++)
			{
				mExecuteArgs[i] = expr[i].evaluate(context, interpreterFilename);
			}
		}

		switch (type)
		{
			case Statement.COLOR:
				setColor(context, mExecuteArgs, nExpressions);
				break;

			case Statement.LINESTYLE:
				setLinestyle(context, mExecuteArgs, nExpressions);
				break;

			case Statement.FONT:
				setFont(context, mExecuteArgs, nExpressions);
				break;

			case Statement.JUSTIFY:
				if (nExpressions == 1)
					setJustify(context, mExecuteArgs[0].getStringValue());
				else
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_JUSTIFY));
				break;
		
			case Statement.MOVE:
			case Statement.DRAW:
			case Statement.RDRAW:
				if (nExpressions > 0 && nExpressions % 2 == 0)
				{
					for (int i = 0; i < nExpressions; i += 2)
					{
						/*
						 * Add point to path.
						 */
						if (type == Statement.MOVE)
						{
							context.moveTo(mExecuteArgs[i].getNumericValue(),
								mExecuteArgs[i + 1].getNumericValue());
						}
						else if (type == Statement.DRAW)
						{
							context.lineTo(mExecuteArgs[i].getNumericValue(),
								mExecuteArgs[i + 1].getNumericValue());
						}
						else /* RDRAW */
						{
							context.rlineTo(mExecuteArgs[i].getNumericValue(),
								mExecuteArgs[i + 1].getNumericValue());
						}
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.WRONG_COORDINATE));
				}
				break;

			case Statement.ARC:
				if (nExpressions == 5)
				{
					int direction = (mExecuteArgs[0].getNumericValue() > 0 ? 1 : -1);

					context.arcTo(direction,
						mExecuteArgs[1].getNumericValue(),
						mExecuteArgs[2].getNumericValue(),
						mExecuteArgs[3].getNumericValue(),
						mExecuteArgs[4].getNumericValue());
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_ARC));
				}
				break;

			case Statement.CIRCLE:
				if (nExpressions == 3)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					if (radius > 0)
					{
							/*
							 * Add 360 degree arc making a full circle.
							 */
							context.moveTo(x1 - radius, y1);
							context.arcTo(1, x1, y1, x1 - radius, y1);
							context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_CIRCLE));
				}
				break;

			case Statement.ELLIPSE:
				if (nExpressions == 4)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					double xRadius = mExecuteArgs[2].getNumericValue();
					double yRadius = mExecuteArgs[3].getNumericValue();
					if (xRadius > 0 && yRadius > 0)
					{
						/*
						 * Add ellipse to path.
						 */
						context.ellipseTo(x1 - xRadius, y1 - yRadius,
							x1 + xRadius, y1 + yRadius);
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_ELLIPSE));
				}
				break;

			case Statement.CYLINDER:
				if (nExpressions == 4)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double height = mExecuteArgs[3].getNumericValue();

					if (radius > 0 && height > 0)
					{
						double xc = radius * 0.552285;
						double yc = xc / 2;

						context.moveTo(x1 - radius, y1);
						context.curveTo(x1 - radius, y1 - yc,
							x1 - xc, y1 - radius / 2,
							x1, y1 - radius / 2);
						context.curveTo(x1 + xc, y1 - radius / 2,
							x1 + radius, y1 - yc,
							x1 + radius, y1);
						context.lineTo(x1 + radius, y1 + height);
						context.curveTo(x1 + radius, y1 + height - yc,
							x1 + xc, y1 + height - radius / 2,
							x1, y1 + height - radius / 2);
						context.curveTo(x1 - xc, y1 + height - radius / 2,
							x1 - radius, y1 + height - yc,
							x1 - radius, y1 + height);
						context.closePath();

						context.ellipseTo(x1 - radius, y1 + height - radius / 2,
							x1 + radius, y1 + height + radius / 2);
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_CYLINDER));
				}
				break;

			case Statement.RAINDROP:
				if (nExpressions == 3)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					if (radius > 0)
					{
						double dist = radius;
						context.moveTo(x1 - radius, y1);
						context.arcTo(-1, x1, y1, x1 + radius, y1);
						context.curveTo(x1 + radius, y1 + dist,
								x1, y1 + radius * 3 - dist,
								x1, y1 + radius * 3);
						context.curveTo(x1, y1 + radius * 3 - dist,
								x1 - radius, y1 + dist,
								x1 - radius, y1);
						context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_RAINDROP));
				}
				break;

			case Statement.BEZIER:
				if (nExpressions == 6)
				{
					double xControl1 = mExecuteArgs[0].getNumericValue();
					double yControl1 = mExecuteArgs[1].getNumericValue();
					double xControl2 = mExecuteArgs[2].getNumericValue();
					double yControl2 = mExecuteArgs[3].getNumericValue();
					double xEnd = mExecuteArgs[4].getNumericValue();
					double yEnd = mExecuteArgs[5].getNumericValue();

					context.curveTo(xControl1, yControl1, xControl2, yControl2, xEnd, yEnd);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_BEZIER));
				}
				break;

			case Statement.WEDGE:
				if (nExpressions == 5)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double startAngle = mExecuteArgs[3].getNumericValue();
					double sweep = mExecuteArgs[4].getNumericValue();
					double endAngle = startAngle + sweep;
					int sign = sweep > 0 ? -1 : 1;
					startAngle = Math.toRadians(startAngle);
					endAngle = Math.toRadians(endAngle);
					if (radius > 0 && sweep != 0)
					{
							/*
							 * Add straight line segments and arc defining
							 * wedge (piece slice) shape.
							 */
							context.moveTo(x1, y1);
							context.lineTo(x1 + Math.cos(startAngle) * radius,
								y1 + Math.sin(startAngle) * radius);
							context.arcTo(sign, x1, y1,
								x1 + Math.cos(endAngle) * radius,
								y1 + Math.sin(endAngle) * radius);
							context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WEDGE));
				}
				break;

			case Statement.SPIRAL:
				if (nExpressions == 5)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double revs = mExecuteArgs[3].getNumericValue();
					double angle = mExecuteArgs[4].getNumericValue();
					angle = Math.toRadians(angle);
					if (radius > 0 && revs != 0)
					{
						double resolution = context.getResolution();
						double angleStep = Math.acos((radius - resolution) / radius);
						double tStep = angleStep / (Math.PI * 2);
						tStep /= Math.abs(revs);
	
						double t = 1;
						int i = 0;
						while (t > 0)
						{
							x2 = radius * t * Math.cos(Math.PI * 2 * revs * t + angle) + x1;
							y2 = radius * t * Math.sin(Math.PI * 2 * revs * t + angle) + y1;
	
							if (i == 0)
								context.moveTo(x2, y2);
							else
								context.lineTo(x2, y2);
	
							i++;
							t = 1 - (i * tStep);
						}
						context.lineTo(x1, y1);
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SPIRAL));
				}
				break;

			case Statement.HEXAGON:
				if (nExpressions == 3)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double sin60radius = 0.8660254 * radius;
					double cos60radius = 0.5 * radius;
					if (radius > 0)
					{
							/*
							 * Add six points defining hexagon.
							 */
							context.moveTo(x1 - cos60radius, y1 - sin60radius);
							context.lineTo(x1 + cos60radius, y1 - sin60radius);
							context.lineTo(x1 + radius, y1);
							context.lineTo(x1 + cos60radius, y1 + sin60radius);
							context.lineTo(x1 - cos60radius, y1 + sin60radius);
							context.lineTo(x1 - radius, y1);
							context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_HEXAGON));
				}
				break;

			case Statement.PENTAGON:
				if (nExpressions == 3)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double sin54radius = 0.809017 * radius;
					double cos54radius = 0.59778525 * radius;
					double sin18radius = 0.309017 * radius;
					double cos18radius = 0.95105652 * radius;
					if (radius > 0)
					{
						/*
						 * Add five points defining pentagon.
						 */
						context.moveTo(x1 - cos54radius, y1 - sin54radius);
						context.lineTo(x1 + cos54radius, y1 - sin54radius);
						context.lineTo(x1 + cos18radius, y1 + sin18radius);
						context.lineTo(x1, y1 + radius);
						context.lineTo(x1 - cos18radius, y1 + sin18radius);
						context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PENTAGON));
				}
				break;

			case Statement.TRIANGLE:
				if (nExpressions == 4)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					double rotation = mExecuteArgs[3].getNumericValue();
					rotation = Math.toRadians(rotation);
					
					/*
					 * Precalculate triangle corner points.
					 */
					double sin30radius = 0.5 * radius;
					double cos30radius = 0.8660254 * radius;

					AffineTransform affine = AffineTransform.getTranslateInstance(x1, y1);
					affine.rotate(rotation);

					/*
					 * Add coordinates for equilateral triangle to path,
					 * rotated to desired angle.
					 */
					Point2D.Double pt = new Point2D.Double(0, radius);
					affine.transform(pt, pt);
					context.moveTo(pt.x, pt.y);

					pt.x = cos30radius;
					pt.y = -sin30radius;
					affine.transform(pt, pt);
					context.lineTo(pt.x, pt.y);

					pt.x = -cos30radius;
					pt.y = -sin30radius;
					affine.transform(pt, pt);
					context.lineTo(pt.x, pt.y);

					context.closePath();
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_TRIANGLE));
				}
				break;

			case Statement.STAR:
				if (nExpressions == 4)
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					radius = mExecuteArgs[2].getNumericValue();
					int nPoints = (int)mExecuteArgs[3].getNumericValue();

					if (radius > 0 && nPoints > 0)
					{
						double angleBetweenPoints = (Math.PI * 2) / nPoints;
						double angle = Math.PI / 2;

						/*
						 * Angle of each point of the star.
						 */
						double starPointAngle = angleBetweenPoints / 3;

						/*
						 * Calculate distance from center of star to inner
						 * coordinates of star using rule:
						 * a / sin(A) = b / sin(B).
						 */
						double dist = radius * Math.sin(starPointAngle / 2) /
							Math.sin(Math.PI - starPointAngle / 2 - angleBetweenPoints / 2);

						/*
						 * Add each point of the star, starting at the top
						 * and proceeding clockwise.
						 */
						for (int i = 0; i < nPoints; i++)
						{
							x2 = x1 + Math.cos(angle) * radius;
							y2 = y1 + Math.sin(angle) * radius;

							if (i == 0)
								context.moveTo(x2, y2);
							else
								context.lineTo(x2, y2);

							x2 = x1 + Math.cos(angle - angleBetweenPoints / 2) * dist;
							y2 = y1 + Math.sin(angle - angleBetweenPoints / 2) * dist;
							context.lineTo(x2, y2);

							angle -= angleBetweenPoints;
						}
						context.closePath();
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_STAR));
				}
				break;

			case Statement.BOX:
			case Statement.ROUNDEDBOX:
			case Statement.BOX3D:
			case Statement.GUILLOTINE:
			case Statement.PROTECT:
			case Statement.UNPROTECT:
				if (nExpressions == 4 || ((type == Statement.ROUNDEDBOX ||
					type == Statement.BOX3D) && nExpressions == 5))
				{
					x1 = mExecuteArgs[0].getNumericValue();
					y1 = mExecuteArgs[1].getNumericValue();
					x2 = mExecuteArgs[2].getNumericValue();
					y2 = mExecuteArgs[3].getNumericValue();
					
					double xMin, xMax, yMin, yMax, depth = 0;
 
					if (x1 < x2)
					{
						xMin = x1;
						xMax = x2;
					}
					else
					{
						xMin = x2;
						xMax = x1;
					}
					if (y1 < y2)
					{
						yMin = y1;
						yMax = y2;
					}
					else
					{
						yMin = y2;
						yMax = y1;
					}

					if (type == Statement.ROUNDEDBOX)
					{
						double yRange = yMax - yMin;
						double xRange = xMax - xMin;

						if (nExpressions == 5)
						{
							radius = mExecuteArgs[4].getNumericValue();
							if (radius > xRange / 2)
								radius = xRange / 2;
							if (radius > yRange / 2)
								radius = yRange / 2;
						}
						else
						{
							radius = Math.min(xRange / 10, yRange / 10);
						}

						/*
						 * If there is no radius then draw a plain box without
						 * rounded corners instead.
						 */
						if (radius <= 0)
							type = Statement.BOX;
					}
					else if (type == Statement.BOX3D)
					{
						depth = Math.min(xMax - xMin, yMax - yMin);
						if (nExpressions == 5)
						{
							depth = mExecuteArgs[4].getNumericValue();
						}
					}

					if (type == Statement.BOX || type == Statement.BOX3D)
					{
						context.moveTo(xMin, yMin);
						context.lineTo(xMin, yMax);
						context.lineTo(xMax, yMax);
						context.lineTo(xMax, yMin);
						context.closePath();

						if (type == Statement.BOX3D)
						{

							double cos30 = Math.cos(Math.toRadians(30));
							double sin30 = Math.sin(Math.toRadians(30));

							/*
							 * Draw slanted right side of 3D box.
							 */
							x1 = xMax + depth * cos30;
							y1 = yMax + depth * sin30;
							context.moveTo(xMax, yMax);
							context.lineTo(x1, y1);
							context.lineTo(x1, y1 - (yMax - yMin));
							context.lineTo(xMax, yMin);
							context.closePath();

							/*
							 * Draw slanted top side of 3D box.
							 */
							x1 = xMin + depth * cos30;
							y1 = yMax + depth * sin30;
							context.moveTo(xMin, yMax);
							context.lineTo(x1, y1);
							context.lineTo(x1 + (xMax - xMin), y1);
							context.lineTo(xMax, yMax);
							context.closePath();
						}
					}
					else if (type == Statement.ROUNDEDBOX)
					{
						context.moveTo(xMin, yMax - radius);
						context.arcTo(1, xMin + radius, yMax - radius, xMin + radius, yMax);
						context.lineTo(xMax - radius, yMax);
						context.arcTo(1, xMax - radius, yMax - radius, xMax, yMax - radius);
						context.lineTo(xMax, yMin + radius);
						context.arcTo(1, xMax - radius, yMin + radius, xMax - radius, yMin);
						context.lineTo(xMin + radius, yMin);
						context.arcTo(1, xMin + radius, yMin + radius, xMin, yMin + radius);
						context.closePath();
					}
					else if (type == Statement.GUILLOTINE)
					{
						context.guillotine(xMin, yMin, xMax, yMax);
					}
					else if (type == Statement.PROTECT)
					{
						context.protect(xMin, yMin, xMax, yMax);
					}
					else /* UNPROTECT */
					{
						context.unprotect(xMin, yMin, xMax, yMax);
					}
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_BOX));
				}
				break;

			case Statement.ADDPATH:
				for (int i = 0; i < nExpressions; i++)
				{
					double coords[] = mExecuteArgs[i].getGeometryValue();
					addGeometryToPath(context, coords, 0);
				}
				break;
				
			case Statement.CLEARPATH:
				if (nExpressions > 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				context.clearPath();
				break;

			case Statement.CLOSEPATH:
				if (nExpressions > 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				context.closePath();
				break;

			case Statement.SAMPLEPATH:
				if (nExpressions == 2)
				{
					context.samplePath(mExecuteArgs[0].getNumericValue(), mExecuteArgs[1].getNumericValue());
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PATH_SAMPLE));
				}
				break;
				
			case Statement.STRIPEPATH:
				if (nExpressions == 2)
				{
					degrees = mExecuteArgs[1].getNumericValue();
					context.stripePath(mExecuteArgs[0].getNumericValue(), Math.toRadians(degrees));
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PATH_STRIPE));
				}
				break;

			case Statement.SHIFTPATH:
				if (nExpressions == 2)
				{
					context.translatePath(mExecuteArgs[0].getNumericValue(),
						mExecuteArgs[1].getNumericValue());
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PATH_SHIFT));
				}
				break;

			case Statement.PARALLELPATH:
				if (nExpressions > 0)
				{
					double []distances = new double[nExpressions];
					for (int i = 0; i < nExpressions; i++)
						distances[i] = mExecuteArgs[i].getNumericValue();
					context.parallelPath(distances);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_DISTANCE));
				}
				break;

			case Statement.SELECTPATH:
				if (nExpressions > 0 && nExpressions % 2 == 0)
				{
					double []offsets = new double[nExpressions / 2];
					double []lengths = new double[nExpressions / 2];
					for (int i = 0; i < nExpressions / 2; i++)
					{
						offsets[i] = mExecuteArgs[i * 2].getNumericValue();
						lengths[i] = mExecuteArgs[i * 2 + 1].getNumericValue();
					}
					context.selectPath(offsets, lengths);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				break;

			case Statement.SINKHOLE:
				if (nExpressions > 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				context.createSinkhole();
				break;

			case Statement.STROKE:
				if (nExpressions > 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				context.stroke();
				break;
				
			case Statement.FILL:
				if (nExpressions > 0)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES));
				}
				context.fill();
				break;

			case Statement.GRADIENTFILL:
				if (nExpressions == 4 || nExpressions == 5)
				{
					Color c1 = ColorDatabase.getColor(mExecuteArgs[0].toString(), 255);
					Color c2 = ColorDatabase.getColor(mExecuteArgs[1].toString(), 255);
					Color c3 = ColorDatabase.getColor(mExecuteArgs[2].toString(), 255);
					Color c4 = ColorDatabase.getColor(mExecuteArgs[3].toString(), 255);
					Color c5 = null;
					if (nExpressions == 5)
						c5 = ColorDatabase.getColor(mExecuteArgs[4].toString(), 255);
					context.gradientFill(c1, c2, c3, c4, c5);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GRADIENT_FILL));
				}
				break;

			case Statement.EVENTSCRIPT:
				if (nExpressions == 1)
				{
					String script = mExecuteArgs[0].toString();
					context.setEventScript(script);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SCRIPT));
				}
				break;

			case Statement.CLIP:
				if (nExpressions != 1)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_CLIP_SIDE));
				}
				String side = mExecuteArgs[0].toString();
				if (side.startsWith("in") || side.startsWith("IN"))
					context.clipInside();
				else
					context.clipOutside();
				break;	

			case Statement.LABEL:
			case Statement.PRINT:
			case Statement.FLOWLABEL:
				String label;
				int nChars = 0;
				int labelIndex;
				double offset = 0.0;
				double spacing = 0.0;

				if (type == Statement.FLOWLABEL)
				{
					if (nExpressions < 2)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PATH_OFFSET));
					}
					spacing = mExecuteArgs[0].getNumericValue();
					offset = mExecuteArgs[1].getNumericValue();
					labelIndex = 2;
				}
				else
				{
					labelIndex = 0;
				}

				/*
				 * Label/print a single argument, or several separated by spaces.
				 */
				if (nExpressions == labelIndex + 1)
				{
					label = mExecuteArgs[labelIndex].toString();
					nChars += label.length();
				}
				else
				{
					StringBuffer sb = new StringBuffer();
					for (int i = labelIndex; i < nExpressions; i++)
					{
						if (i > labelIndex)
							sb.append(' ');

						String nextLine = mExecuteArgs[i].toString();
						sb.append(nextLine);
						nChars += nextLine.length();
					}
					label = sb.toString();
				}

				if (type == Statement.PRINT)
				{
					mStdoutStream.println(label);
				}
				else if (nChars > 0)
				{
					if (type == Statement.FLOWLABEL)
						context.flowLabel(spacing, offset, label);
					else
						context.label(label);
				}
				break;

			case Statement.TABLE:
				if (nExpressions >= 2)
				{
					extras = mExecuteArgs[0].getStringValue();
					ArrayList columns = new ArrayList(nExpressions - 1);
					for (int i = 1; i < nExpressions; i++)
					{
						Argument arg;
						if (mExecuteArgs[i].getType() == Argument.HASHMAP)
						{
							arg = mExecuteArgs[i];
						}
						else
						{
							arg = new Argument();
							arg.addHashMapEntry("1", mExecuteArgs[i]);
						}
						columns.add(arg);
					}
					context.drawTable(extras, columns);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_TABLE));
				}
				break;

			case Statement.ICON:
				if (nExpressions == 1 || nExpressions == 2)
				{
					double size;
					if (nExpressions == 2)
						size = mExecuteArgs[1].getNumericValue();
					else
						size = 0;

					context.drawIcon(mExecuteArgs[0].getStringValue(), size);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_ICON));
				}
				break;

			case Statement.GEOIMAGE:
				if (nExpressions == 1 || nExpressions == 2)
				{
					String filename = mExecuteArgs[0].getStringValue();
					if (nExpressions == 2)
						extras = mExecuteArgs[1].getStringValue();
					else
						extras = "";
					context.drawGeoImage(filename, extras);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_GEOIMAGE));
				}
				break;

			case Statement.EPS:
				if (nExpressions == 1 || nExpressions == 2)
				{
					double size;
					if (nExpressions == 2)
					{
						size = mExecuteArgs[1].getNumericValue();
					}
					else
					{
						size = 0;
					}

					context.drawEPS(mExecuteArgs[0].getStringValue(), size);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_EPS));
				}
				break;
		
			case Statement.SCALE:
				if (nExpressions == 1)
				{
					double s = mExecuteArgs[0].getNumericValue();
					context.setScaling(s);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SCALING));
				}
				break;

			case Statement.ROTATE:
				if (nExpressions == 1)
				{
					degrees = mExecuteArgs[0].getNumericValue();
					context.setRotation(Math.toRadians(degrees));
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_ROTATION));
				}
				break;

			case Statement.WORLDS:

				if (nExpressions == 4 || nExpressions == 5)
				{
					/*
					 * Add world coordinate over whole page.
					 */
					px1 = py1 = px2 = py2 = 0;
				}
				else if (nExpressions == 8 || nExpressions == 9)
				{
					/*
					 * Set world coordinates over part of the page.
					 */
					px1 = mExecuteArgs[4].getNumericValue();
					py1 = mExecuteArgs[5].getNumericValue();
					px2 = mExecuteArgs[6].getNumericValue();
					py2 = mExecuteArgs[7].getNumericValue();
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WORLDS));
				}

				x1 = mExecuteArgs[0].getNumericValue();
				y1 = mExecuteArgs[1].getNumericValue();
				x2 = mExecuteArgs[2].getNumericValue();
				y2 = mExecuteArgs[3].getNumericValue();

				units = Context.WORLD_UNITS_METRES;
				allowDistortion = false;

				if (nExpressions == 5)
					extras = mExecuteArgs[4].getStringValue();
				else if (nExpressions == 9)
					extras = mExecuteArgs[8].getStringValue();
				else
					extras = "";

				/*
				 * Parse additional options.
				 */
				StringTokenizer st2 = new StringTokenizer(extras);
				while (st2.hasMoreTokens())
				{
					String token = st2.nextToken();
					if (token.startsWith("units="))
					{
						String s = token.substring(6);
						Integer u = (Integer)mWorldUnitsLookup.get(s);
						if (u == null)
						{
							throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_WORLD_UNITS) +
								": " + s);
						}
						units = u.intValue();
					}
					else if (token.startsWith("distortion="))
					{
						String flag = token.substring(11);
						allowDistortion = flag.equalsIgnoreCase("true");
					}
				}

				context.setWorlds(x1, y1, x2, y2, px1, py1, px2, py2,
					units, allowDistortion);
				break;

			case Statement.PROJECT:
				if (nExpressions == 2)
				{
						context.setTransform(mExecuteArgs[0].getStringValue(),
							mExecuteArgs[1].getStringValue());
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_TRANSFORM));
				}
				break;
	
			case Statement.DATASET:
				// TODO accept two arguments, with extras string then empty.
				if (nExpressions == 3)
				{
					context.setDataset(mExecuteArgs[0].getStringValue(),
						mExecuteArgs[1].getStringValue(),
						mExecuteArgs[2].getStringValue(),
						mStdinStream);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_DATASET));
				}
				break;

			case Statement.FETCH:
				/*
				 * Fetch next row from dataset.
				 */
				Row row = context.fetchRow();

				String []fieldNames = context.getDatasetFieldNames();
				String fieldName;

				for (int i = 0; i < row.size(); i++)
				{
						/*
						 * Define all fields as variables.
						 */
						if (fieldNames != null)
							fieldName = fieldNames[i];
						else
							fieldName = DefaultFieldNames.get(i);
						context.defineVariable(fieldName, (Argument)(row.get(i)));
				}
				break;

			case Statement.NEWPAGE:
				if (nExpressions >= 3 && nExpressions <= 5)
				{
					String format = mExecuteArgs[0].getStringValue();
					String filename = mExecuteArgs[1].getStringValue();
					double width, height;
					int extrasIndex;

					if (nExpressions == 3 || mExecuteArgs[2].getType() == Argument.STRING)
					{
						String paperName = mExecuteArgs[2].getStringValue();
						PageSize p = new PageSize(paperName);
						width = p.getDimension().getX();
						height = p.getDimension().getY();
						extrasIndex = 3;
					}
					else
					{
						width = mExecuteArgs[2].getNumericValue();
						height = mExecuteArgs[3].getNumericValue();
						extrasIndex = 4;
					}

					if (extrasIndex < nExpressions)
						extras = mExecuteArgs[extrasIndex].getStringValue();
					else
						extras = "";

					if (width <= 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_SIZE) +
							": " + width);
					}
					if (height <= 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_SIZE) +
							": " + height);
					}

					context.setOutputFormat(format, filename, width, height,
						extras, mStdoutStream);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE));
				}
				break;	

			case Statement.ENDPAGE:
				context.closeOutputFormat();
				break;

			case Statement.LOCAL:
				break;

			case Statement.EVAL:
				if (nExpressions == 1)
				{
					/*
					 * Run evaluated argument as a command.  This allows
					 * commands to be built and executed on-the-fly.
					 */
					String command = mExecuteArgs[0].getStringValue();
					StringReader stringReader = new StringReader(command);
					String filename = "(in eval)";
					FileOrURL f = new FileOrURL(stringReader, filename);
					byte []emptyBuffer = new byte[0];
					interpret(context, f, new ByteArrayInputStream(emptyBuffer),
						mStdoutStream);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_EVAL));
				}
				break;

			case Statement.LET:
				/*
				 * Nothing to do -- any variables were assigned during expression
				 * evaluation above.
				 */
				break;

			case Statement.KEY:
				if (nExpressions >= 2)
				{
					String entryType = mExecuteArgs[0].getStringValue();
					String description = mExecuteArgs[1].getStringValue();
					int eType = LegendEntry.parseTypeString(entryType);
					if (eType < 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LEGEND_TYPE) +
							": " + entryType);
					}
					mContext.addLegendEntry(description, eType, mExecuteArgs, 2, nExpressions - 2);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_LEGEND_ENTRY));
				}
				break;

			case Statement.LEGEND:
				if (nExpressions == 1)
				{
					legendSize = mExecuteArgs[0].getNumericValue();
					displayLegend(st, context, legendSize);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_LEGEND_SIZE));
				}
				break;
			case Statement.MIMETYPE:
				if (nExpressions == 1)
				{
					String mimeType = mExecuteArgs[0].getStringValue();
					context.setMimeType(mimeType);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_MIME_TYPE));
				}
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
		if (!(Character.isLetter((char)c) || c == '$'))
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.INVALID_KEYWORD));
		}
		
		/*
		 * Read in whole word.
		 */
		do
		{
			word.append((char)c);
			c = preprocessor.read();
		}
		while (Character.isLetterOrDigit((char)c) || c == '.' || c == '_' || c == ':');

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
		ArrayList expressions = new ArrayList();
		Expression expr;
		Statement retval = null;
		boolean finishedStatement = false;
		int c;

		state = AT_ARGUMENT;
		c = readSkipComments(preprocessor);

		/*
		 * Keep parsing statement until we get to the end of the
		 * line or end of file.
		 */
		while (!finishedStatement)
		{
			if (c == -1 || c == '\n')
			{
				finishedStatement = true;
			}
			else if (Character.isWhitespace((char)c))
			{
				/*
				 * Ignore any whitespace.
				 */
				c = readSkipComments(preprocessor);
			}
			else if (state == AT_SEPARATOR)
			{
				/*
				 * Expect a ',' between arguments to a
				 * statement or procedure block.
				 */
				if (c != ARGUMENT_SEPARATOR)
				{
					throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
						": '" + ARGUMENT_SEPARATOR + "'");
				}
				c = readSkipComments(preprocessor);
				state = AT_ARGUMENT;
			}
			else
			{
				/*
				 * Parse an expression.
				 */
				preprocessor.unread(c);
				expr = new Expression(preprocessor);
				expressions.add(expr);

				c = readSkipComments(preprocessor);
				state = AT_SEPARATOR;
			}
		}

		/*
		 * Build a statement structure for what we just parsed.
		 */
		if (c == -1 && expressions.size() == 0)
		{
			/*
			 * Could not parse anything before we got EOF.
			 */
			retval = null;
		}
		else
		{
			Expression []a = new Expression[expressions.size()];

			for (int i = 0; i < a.length; i++)
			{
				a[i] = (Expression)expressions.get(i);
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
	private ArrayList parseParameters(Preprocessor preprocessor)
		throws IOException, MapyrusException
	{
		int c;
		ArrayList parameters = new ArrayList();
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
					throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
						": '" + PARAM_SEPARATOR + "'");
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
		ArrayList parameters;
		ArrayList procedureStatements = new ArrayList();
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
					": " + END_KEYWORD);
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
	 * Reads and parses repeat or while loop statement.
	 * Parses test expression, "do" keyword, some
	 * statements, and then "done" keyword.
	 * @param preprocessor is source to read from.
	 * @param isWhileLoop true if parsing a while loop, false for a repeat loop.
	 * @param inProcedureDefn true if already parsing inside a procedure defn.
	 * @return parsed loop as single statement.
	 */
	private ParsedStatement parseLoopStatement(Preprocessor preprocessor,
		boolean isWhileLoop, boolean inProcedureDefn)
		throws IOException, MapyrusException
	{
		ParsedStatement st;
		Expression test;
		ArrayList loopStatements = new ArrayList();
		Statement statement;
		String currentFilename = preprocessor.getCurrentFilename();
		int currentLineNumber = preprocessor.getCurrentLineNumber();
		
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_DO)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + DO_KEYWORD);
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
			}
		}
		
		/*
		 * Expect "done" after statements.
		 */
		if (st.getKeywordType() != ParsedStatement.PARSED_DONE)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + DONE_KEYWORD);
		}

		statement = new Statement(test, loopStatements, isWhileLoop);
		statement.setFilenameAndLineNumber(currentFilename, currentLineNumber);
		return(new ParsedStatement(statement));		
	}

	/**
	 * Reads and parses "for" loop statement.
	 * Parses variable name, "in" keyword, arrayname, "do" keyword,
	 * some statements, and then "done" keyword.
	 * @param preprocessor is source to read from.
	 * @return parsed loop as single statement.
	 */
	private ParsedStatement parseForStatement(Preprocessor preprocessor,
		boolean inProcedureDefn)
		throws IOException, MapyrusException
	{
		ParsedStatement st;
		Expression var, arrayExpr;
		ArrayList loopStatements = new ArrayList();
		Statement statement;
		String currentFilename = preprocessor.getCurrentFilename();
		int currentLineNumber = preprocessor.getCurrentLineNumber();

		var = new Expression(preprocessor);

		/*
		 * Expect to parse "in" keyword.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside for loop.
			 */
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_IN)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + IN_KEYWORD);
		}

		arrayExpr = new Expression(preprocessor);

		/*
		 * Expect to parse "do" keyword.
		 */
		st = parseStatementOrKeyword(preprocessor, inProcedureDefn);
		if (st == null)
		{
			/*
			 * Should not reach end of file inside while loop.
			 */
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_DO)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + DO_KEYWORD);
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
			}
		}
		
		/*
		 * Expect "done" after statements.
		 */
		if (st.getKeywordType() != ParsedStatement.PARSED_DONE)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + DONE_KEYWORD);
		}

		statement = new Statement(var, arrayExpr, loopStatements);
		statement.setFilenameAndLineNumber(currentFilename, currentLineNumber);
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
		String currentFilename = preprocessor.getCurrentFilename();
		int currentLineNumber = preprocessor.getCurrentLineNumber();
		Expression test;
		ArrayList thenStatements = new ArrayList();
		ArrayList elseStatements = new ArrayList();
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
		}
		else if (st.isStatement() ||
			st.getKeywordType() != ParsedStatement.PARSED_THEN)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + THEN_KEYWORD);
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
					throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
						": " + MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF));
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
				throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
					": " + ENDIF_KEYWORD);
			}
			elseStatements.add(st.getStatement());
			checkForEndif = false;
		}

		/*
		 * Expect "endif" after statements.
		 */
		if (checkForEndif && st.getKeywordType() != ParsedStatement.PARSED_ENDIF)
		{
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.EXPECTED) +
				": " + ENDIF_KEYWORD);
		}

		statement = new Statement(test, thenStatements, elseStatements);
		statement.setFilenameAndLineNumber(currentFilename, currentLineNumber);
		return(new ParsedStatement(statement));
	}

	/*
	 * Static keyword lookup table for fast keyword lookup.
	 */
	private static HashMap mKeywordLookup;

	static
	{
		mKeywordLookup = new HashMap();
		mKeywordLookup.put(END_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_END));
		mKeywordLookup.put(THEN_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_THEN));
		mKeywordLookup.put(ELSE_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ELSE));
		mKeywordLookup.put(ELIF_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ELSIF));
		mKeywordLookup.put(ENDIF_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_ENDIF));
		mKeywordLookup.put(DO_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_DO));
		mKeywordLookup.put(DONE_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_DONE));
		mKeywordLookup.put(IN_KEYWORD,
			new ParsedStatement(ParsedStatement.PARSED_IN));
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
		boolean finishedStatement = false;

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
						throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
							": " + MapyrusMessages.get(MapyrusMessages.NESTED_PROC));
					}
					retval = parseProcedureBlock(preprocessor);
				}
				else if (lower.equals(IF_KEYWORD))
				{
					retval = parseIfStatement(preprocessor, inProcedureDefn);
				}
				else if (lower.equals(WHILE_KEYWORD))
				{
					retval = parseLoopStatement(preprocessor, true, inProcedureDefn);
				}
				else if (lower.equals(REPEAT_KEYWORD))
				{
					retval = parseLoopStatement(preprocessor, false, inProcedureDefn);
				}
				else if (lower.equals(FOR_KEYWORD))
				{
					retval = parseForStatement(preprocessor, inProcedureDefn);
				}
				else if (lower.equals(RETURN_KEYWORD))
				{
					retval = new ParsedStatement(Statement.RETURN_STATEMENT);
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

	/**
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
			throw new MapyrusException(preprocessor.getCurrentFilenameAndLineNumber() +
				": " + MapyrusMessages.get(MapyrusMessages.INVALID_KEYWORD));
		}
		return(st.getStatement());
	}

	/**
	 * Reads and parses commands from file and executes them.
	 * @param context is the context to use during interpretation.
	 * @param f is open file or URL to read from.
	 * @param stdin is stream to use for standard input by this intepreter.
	 * @param stdout is stream to use for standard output by this intepreter.
	 * File f is closed by this method when reading is completed.
	 */
	public void interpret(ContextStack context, FileOrURL f,
		InputStream stdin, PrintStream stdout)
		throws IOException, MapyrusException
	{
		Statement st;
		Preprocessor preprocessor = new Preprocessor(f);
		mInComment = false;
		mStdinStream = stdin;
		mStdoutStream = stdout;
		mContext = context;

		try
		{
			/*
			 * Keep parsing until we get EOF or reach a 'RETURN' command.
			 */
			while ((st = parseStatement(preprocessor)) != null)
			{
				int type = executeStatement(st);
				if (type == Statement.RETURN)
					break;
			}
		}
		finally
		{
			/*
			 * Ensure that all files the preprocessor opened are always closed.
			 */
			preprocessor.close();
		}
	}

	private void makeCall(Statement block, ArrayList parameters, Argument []args)
		throws IOException, MapyrusException
	{
		Statement statement;
		String parameterName;

		for (int i = 0; i < args.length; i++)
		{
			parameterName = (String)parameters.get(i);
			mContext.setLocalScope(parameterName);
			mContext.defineVariable(parameterName, args[i]);
		}

		/*
		 * Execute each of the statements in the procedure block.
		 */
		ArrayList v = block.getStatementBlock();
		for (int i = 0; i < v.size(); i++)
		{
			statement = (Statement)v.get(i);
			int type = executeStatement(statement);
			if (type == Statement.RETURN)
				break;
		}
	}

	/**
	 * Recursive function for executing single statements and
	 * blocks of statements.
	 * @param statement is statement to execute.
	 * @return type of last statement executed.
	 */
	private int executeStatement(Statement statement)
		throws IOException, MapyrusException
	{
		Argument []args;
		int statementType = statement.getType();

		/*
		 * Store procedure blocks away for later execution,
		 * execute any other statements immediately.
		 */
		if (statementType == Statement.BLOCK)
		{
			mStatementBlocks.put(statement.getBlockName(), statement);
		}
		else if (statementType == Statement.RETURN)
		{
			/*
			 * Nothing to do, just return.
			 */
		}
		else if (statementType == Statement.CONDITIONAL)
		{
			/*
			 * Execute correct part of if statement depending on value of expression.
			 */
			Expression []expr = statement.getExpressions();
			ArrayList v;
			Argument test;
			
			try
			{
				test = expr[0].evaluate(mContext, statement.getFilename());
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
			}
			
			if (test.getType() != Argument.NUMERIC)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.INVALID_EXPRESSION));
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
					statement = (Statement)v.get(i);
					statementType = executeStatement(statement);
					if (statementType == Statement.RETURN)
						break;
				}
			}
		}
		else if (statementType == Statement.WHILE_LOOP ||
			statementType == Statement.REPEAT_LOOP)
		{
			/*
			 * Find expression to test and loop statements to execute.
			 */
			Expression []expr = statement.getExpressions();
			
			ArrayList v = statement.getLoopStatements();
			Argument test;
	
			try
			{
				test = expr[0].evaluate(mContext, statement.getFilename());
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
			}

			if (test.getType() != Argument.NUMERIC)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.INVALID_EXPRESSION));
			}

			int nIterations = 0;
			if (statementType == Statement.REPEAT_LOOP)
			{
				/*
				 * Protect against round-off.
				 */
				double d = test.getNumericValue();
				if (NumericalAnalysis.equals(d, (int)d))
					nIterations = (int)Math.round(d);
				else
					nIterations = (int)test.getNumericValue();
			}

			/*
			 * Execute loop while expression remains true (non-zero).
			 */
			boolean gotReturn = false;
			int iter = 0;
			int loopStatementType = statementType;
			while
			(
				(!gotReturn) &&
			 	((loopStatementType == Statement.WHILE_LOOP &&
					test.getNumericValue() != 0.0) ||
			 	(loopStatementType == Statement.REPEAT_LOOP &&
				 	iter < nIterations))
			)
			{
				/*
				 * Execute each of the statements.
				 */	
				for (int i = 0; i < v.size(); i++)
				{
					Statement st = (Statement)v.get(i);
					statementType = executeStatement(st);
					if (statementType == Statement.RETURN)
					{
						gotReturn = true;
						break;
					}
				}

				if (loopStatementType == Statement.WHILE_LOOP && (!gotReturn))
				{
					test = expr[0].evaluate(mContext, statement.getFilename());
					if (test.getType() != Argument.NUMERIC)
					{
						throw new MapyrusException(statement.getFilenameAndLineNumber() +
							": " + MapyrusMessages.get(MapyrusMessages.INVALID_EXPRESSION));
					}
				}
				iter++;
			}
		}
		else if (statementType == Statement.FOR_LOOP)
		{
			/*
			 * Find hashmap to loop through and the variable to assign
			 * each hashmap key into.
			 */
			Expression []varExpr = statement.getExpressions();
			Expression hashMapExpr = statement.getForHashMap();

			ArrayList v = statement.getLoopStatements();
			Argument hashMapVar;
			String varName = varExpr[0].getVariableName();

			try
			{
				hashMapVar = hashMapExpr.evaluate(mContext, statement.getFilename());
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
			}

			if (varName == null)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.VARIABLE_EXPECTED));
			}
			if (hashMapVar.getType() == Argument.HASHMAP)
			{
				/*
				 * Take a copy of current keys in hashmap so that changes to the hashmap
				 * during the loop have no effect.
				 */
				Object []keys = hashMapVar.getHashMapKeys();
				boolean gotReturn = false;
				for (int i = 0; i < keys.length && !gotReturn; i++)
				{
					String currentKey = (String)keys[i];
					mContext.defineVariable(varName,
						new Argument(Argument.STRING, currentKey));
	
					/*
					 * Execute each of the statements.
					 */	
					for (int j = 0; j < v.size(); j++)
					{
						Statement st = (Statement)v.get(j);
						statementType = executeStatement(st);
						if (statementType == Statement.RETURN)
						{
							gotReturn = true;
							break;
						}
					}
				}
			}
		}
		else if (statementType == Statement.CALL)
		{
			/*
			 * Find the statements for the procedure block we are calling.
			 */
			String blockName = statement.getBlockName();
			Statement block = (Statement)mStatementBlocks.get(blockName);
			if (block == null)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.UNDEFINED_PROC) +
					": " + blockName);
			}

			/*
			 * Check that correct number of parameters are being passed.
			 */
			ArrayList formalParameters = block.getBlockParameters();
			Expression []actualParameters = statement.getExpressions();
			if (actualParameters.length != formalParameters.size())
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.WRONG_PARAMETERS));
			}

			try
			{
				/*
				 * Save state and set parameters passed to the procedure.
				 */
				args = new Argument[actualParameters.length];
				for (int i = 0; i < args.length; i++)
				{
					args[i] = actualParameters[i].evaluate(mContext, statement.getFilename());
				}
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
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
				ArrayList moveTos = mContext.getMoveTos();
				ArrayList rotations = mContext.getMoveToRotations();

				for (int i = 0; i < moveToCount; i++)
				{
					mContext.saveState(blockName);
					Point2D.Float pt = (Point2D.Float)(moveTos.get(i));
					mContext.setTranslation(pt.x, pt.y);
					mContext.clearPath();

					double rotation = ((Double)rotations.get(i)).doubleValue();
					mContext.setRotation(rotation);
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
				mContext.saveState(blockName);
				makeCall(block, formalParameters, args);
				mContext.restoreState();
			}
		}
		else
		{
			/*
			 * Execute single statement.  If error occurs then add filename and
			 * line number to message so user knows exactly where to look.
			 */
			try
			{
				execute(statement, mContext);
			}
			catch (MapyrusException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
			}
			catch (IOException e)
			{
				throw new MapyrusException(statement.getFilenameAndLineNumber() +
					": " + e.getMessage());
			}
		}
		return(statementType);
	}

	/**
	 * Create new language interpreter.
	 */
	public Interpreter()
	{
		mStatementBlocks = new HashMap();
		mExecuteArgs = null;
	}
	
	/**
	 * Return a clone of this interpreter.
	 * @return cloned interpreter.
	 */
	public Object clone()
	{
		Interpreter retval = new Interpreter();
		retval.mExecuteArgs = null;
		retval.mContext = null;
		retval.mInComment = false;
		retval.mStatementBlocks = (HashMap)(this.mStatementBlocks.clone());
		retval.mStdoutStream = null;
		retval.mStdinStream = null;
		return((Object)retval);
	}
}
