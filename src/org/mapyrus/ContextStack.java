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

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.mapyrus.dataset.DatasetFactory;
import org.mapyrus.dataset.GeographicDataset;
import org.mapyrus.font.StringDimension;
import org.mapyrus.image.Bitmap;
import org.mapyrus.image.ImageIOWrapper;

/**
 * Contexts for interpretation that are pushed and popped as procedure
 * blocks are called and return so that changes in a procedure block
 * are local to that block.
 */
public class ContextStack
{
	/*
	 * Maximum allowed stacking of contexts.
	 * Any deeper is probably infinite recursion.
	 */
	private static final int MAX_STACK_LENGTH = 30;
	
	/*
	 * Prefix for internal variables.
	 */
	private static final String INTERNAL_VARIABLE_PREFIX = Constants.PROGRAM_NAME + ".";
	
	/*
	 * Internal variable names.
	 */
	private static final String PATH_VARIABLE = "path";
	private static final String WORLDS_VARIABLE = "worlds";
	private static final String DATASET_VARIABLE = "dataset";
	private static final String PAGE_VARIABLE = "page";
	private static final String SCREEN_VARIABLE = "screen";
	private static final String IMAGEMAP_VARIABLE = "imagemap";
	
	/*
	 * Stack of contexts, with current context in last slot.
	 */
	private LinkedList mStack;

	/*
	 * List of legend keys encountered whilst interpreting statements.
	 */
	private LegendEntryList mLegendEntries;

	/*
	 * Cache of icons we've already used and are likely to use again.
	 */
	private LRUCache mIconCache;

	/*
	 * Time at which this context was allocated.
	 */
	private long mStartTime;

	/*
	 * Point clicked in HTML imagemap and passed in HTTP request we are processing.
	 */
	private Point mImagemapPoint;

	/*
	 * MIME type of content being returned to HTTP client.
	 */
	private String mMimeType;

	/**
	 * Create new stack of contexts to manage state as procedure blocks
	 * are called.
	 */
	public ContextStack()
	{
		mStack = new LinkedList();
		mStack.add(new Context());
		mStartTime = System.currentTimeMillis();
		mImagemapPoint = null;
		mLegendEntries = new LegendEntryList();
		mIconCache = new LRUCache(Constants.ICON_CACHE_SIZE);
		mMimeType = MimeTypes.get("html");
	}

	/**
	 * Get current context from top of stack.
	 * @return current context.
	 */
	private Context getCurrentContext()
	{
		return((Context)mStack.getLast());
	}

	/**
	 * Pops current context from stack.
	 * @return number of elements left in stack after pop.
	 */
	private int popContext()
		throws IOException, MapyrusException
	{
		int i = mStack.size();

		if (i > 0)
		{
			/*
			 * Finish off current context, remove it from stack.
			 */
			Context context = (Context)mStack.removeLast();
			i--;
			int attributesSet = context.closeContext();

			/*
			 * If graphics attributes were set in context then set them changed
			 * in the context that is now current so they are set again
			 * here before being used.
			 */
			if (i > 0 && attributesSet != 0)
				getCurrentContext().setAttributesChanged(attributesSet);
		}
		return(i);
	}

	/**
	 * Pushes copy of context at top of stack onto stack.
	 * This context is later removed with popContext().
	 * @param blockName is procedure block name containing statements to be executed.
	 */
	private void pushContext(String blockName) throws MapyrusException
	{
		if (mStack.size() == MAX_STACK_LENGTH)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.RECURSION));
		}
		mStack.add(new Context(getCurrentContext(), blockName));
	}

	/**
	 * Set point passed in HTML imagemap request.
	 * @param pt pixel position clicked in image.
	 */
	public void setImagemapPoint(Point pt)
	{
		mImagemapPoint = pt;				
	}

	/**
	 * Sets output file for drawing to.
	 * @param filename name of image file output will be saved to
	 * @param format is image format for saved output
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param extras contains extra settings for this output.
	 * @param stdoutStream standard output stream for program.
	 */
	public void setOutputFormat(String format, String filename,
		double width, double height, String extras,
		PrintStream stdoutStream)
		throws IOException, MapyrusException
	{
		getCurrentContext().setOutputFormat(format, filename,
			width, height, extras, stdoutStream);
	}

	/**
	 * Sets image for drawing to.
	 * @param image is buffered image to draw into.
	 * @param extras contains extra settings for this output.
	 */
	public void setOutputFormat(BufferedImage image, String extras)
		throws IOException, MapyrusException
	{
		getCurrentContext().setOutputFormat(image, extras);
	}

	/**
	 * Close any open output file being created.
	 */
	public void closeOutputFormat() throws IOException, MapyrusException
	{
		getCurrentContext().closeOutputFormat();
	}

	/**
	 * Sets linestyle.
	 * @param width is width for lines in millimetres.
	 * @param cap is a BasicStroke end cap value.
	 * @param join is a BasicStroke line join value.
	 * @param phase is offset at which pattern is started.
	 * @param dashes list of dash pattern lengths.
	 */
	public void setLinestyle(double width, int cap, int join,
		double phase, float []dashes)
	{
		getCurrentContext().setLinestyle(width, cap, join, phase, dashes);
	}

	/**
	 * Sets color.
	 * @param c is new color for drawing.
	 */
	public void setColor(Color c)
	{
		getCurrentContext().setColor(c);
	}

	/**
	 * Sets transparent color blend mode.
	 * @param blend is blend mode.
	 */
	public void setBlend(String blend)
	{
		getCurrentContext().setBlend(blend);
	}

	/**
	 * Sets color to contrast of current color.
	 * @param alpha alpha value for new color.
	 */
	public void contrastColor(int alpha)
	{
		Color contrast;
		Color currentColor = getCurrentContext().getColor();

		/*
		 * Calculate darkness of current color.
		 */
		int darkness = currentColor.getRed() * 3 +
			currentColor.getGreen() * 4 +
			currentColor.getBlue() * 3;

		/*
		 * If color is currently close to black, then contrasting
		 * color is white, otherwise contrasting color is black.
		 */
		if (darkness > (3 + 4 + 3) * 255 / 2)
		{
			if (alpha == 255)
				contrast = Color.BLACK;
			else
				contrast = new Color(0, 0, 0, alpha);
		}
		else
		{
			if (alpha == 255)
				contrast = Color.WHITE;
			else
				contrast = new Color(255, 255, 255, alpha);
		}

		getCurrentContext().setColor(contrast);
	}

	/**
	 * Set color brighter than the current color.
	 * @param alpha alpha value for new color.
	 */
	public void setBrighterColor(int alpha)
	{
		Color currentColor = getCurrentContext().getColor();
		int currentAlpha = currentColor.getAlpha();
		Color brighter = currentColor.brighter();
		if (alpha != currentAlpha)
		{
			brighter = new Color(brighter.getRed(), brighter.getGreen(),
				brighter.getBlue(), alpha);
		}
		getCurrentContext().setColor(brighter);
	}

	/**
	 * Set color darker than the current color.
	 * @param alpha alpha value for new color.
	 */
	public void setDarkerColor(int alpha)
	{
		Color currentColor = getCurrentContext().getColor();
		int currentAlpha = currentColor.getAlpha();
		Color darker = currentColor.darker();
		if (alpha != currentAlpha)
		{
			darker = new Color(darker.getRed(), darker.getGreen(),
				darker.getBlue(), alpha);
		}
		getCurrentContext().setColor(darker);
	}

	/**
	 * Sets font for labelling with.
	 * @param name is name of font.
	 * @param size is size for labelling in millimetres.
	 * @param outlineWidth if non-zero, gives line width to use for drawing
	 * outline of each character of labels.
	 * @param lineSpacing spacing between lines in multi-line labels, as
	 * a multiple of the font size.
	 */
	public void setFont(String name, double size, double outlineWidth,
		double lineSpacing)
	{
		getCurrentContext().setFont(name, size, outlineWidth, lineSpacing);
	}

	/**
	 * Sets horizontal and vertical justification for labelling.
	 * @param code is bit flags of Context.JUSTIFY_* values for justification.
	 */
	public void setJustify(int code)
	{
		getCurrentContext().setJustify(code);
	}

	/**
	 * Sets scaling for subsequent coordinates.
	 * @param factor is new scaling in X and Y axes.
	 */
	public void setScaling(double factor) throws MapyrusException
	{
		if (factor == 0.0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SCALING));
		else if (factor != 1.0)
			getCurrentContext().setScaling(factor);
	}

	/**
	 * Sets translation for subsequent coordinates.
	 * @param x is new point for origin on X axis.
	 * @param y is new point for origin on Y axis.
	 */
	public void setTranslation(double x, double y)
	{
		if (x != 0.0 || y != 0.0)
			getCurrentContext().setTranslation(x, y);
	}

	/**
	 * Sets rotation for subsequent coordinates.
	 * @param angle is rotation angle in radians, going anti-clockwise.
	 */
	public void setRotation(double angle)
	{
		if (angle != 0.0)
			getCurrentContext().setRotation(angle);
	}

	/**
	 * Sets transformation from real world coordinates to page coordinates.
	 * @param wx1 minimum X world coordinate.
	 * @param wy1 minimum Y world coordinate.
	 * @param wx2 maximum X world coordinate.
	 * @param wy2 maximum Y world coordinate.
	 * @param px1 millimetre position on page of wx1.
	 * @param py1 millimetre position on page of wy1.
	 * @param px2 millimetre position on page of wx2, or 0 to use whole page.
	 * @param py2 millimetre position on page of wy2, or 0 to use whole page.
	 * @param units units of world coordinates (WORLD_UNITS_METRES,WORLD_UNITS_FEET, etc.)
	 * @param allowDistortion if true then different scaling in X and Y axes allowed.
	 */
	public void setWorlds(double wx1, double wy1, double wx2, double wy2,
		double px1, double py1, double px2, double py2,
		int units, boolean allowDistortion)
		throws MapyrusException
	{
		getCurrentContext().setWorlds(wx1, wy1, wx2, wy2, px1, py1, px2, py2,
			units, allowDistortion);
	}

	/**
	 * Sets transformation between two world coordinate systems.
	 * @param sourceSystem description of coordinate system coordinates transformed form.
	 * @param destinationSystem description of coordinate system coordinates
	 * are transformed to.
	 */
	public void setTransform(String sourceSystem, String destinationSystem)
		throws MapyrusException
	{
		getCurrentContext().setReprojection(sourceSystem, destinationSystem);
	}

	/**
	 * Transform geometry from page coordinates to world coordinates.
	 * @param arg geometry.
	 * @return transformed geometry.
	 */
	public Argument transformToWorlds(Argument arg) throws MapyrusException
	{
		Argument retval = getCurrentContext().transformToWorlds(arg);
		return(retval);
	}

	/**
	 * Transform geometry from world coordinates to page coordinates.
	 * @param arg geometry.
	 * @return transformed geometry.
	 */
	public Argument transformToPage(Argument arg) throws MapyrusException
	{
		Argument retval = getCurrentContext().transformToPage(arg);
		return(retval);
	}

	/**
	 * Sets dataset to read from.
	 * @param type is format of dataset, for example, "text".
	 * @param name is name of dataset to open.
	 * @param extras are special options for this dataset type such as database
	 * connection information, or instructions for interpreting data.
	 * @param stdin standard ihput stream of interpreter.
	 */
	public void setDataset(String type, String name,
		String extras, InputStream stdin) throws MapyrusException
	{
		GeographicDataset dataset;
		dataset = DatasetFactory.open(type, name, extras, stdin);
		getCurrentContext().setDataset(dataset);
	}

	/**
	 * Sets file for writing standard output to.
	 * File will automatically be closed when this context is closed.
	 * @param stdout stream to write to.
	 */
	public void setStdout(PrintStream stdout) throws IOException
	{
		getCurrentContext().setStdout(stdout);
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void moveTo(double x, double y) throws MapyrusException
	{
		getCurrentContext().moveTo(x, y);
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void lineTo(double x, double y) throws MapyrusException
	{
		getCurrentContext().lineTo(x, y);
	}

	/**
	 * Add point to path with straight line segment relative to last point.
	 * @param x X coordinate distance to move, relative to last point.
	 * @param y Y coordinate distance to move, relative to last point.
	 */
	public void rlineTo(double x, double y) throws MapyrusException
	{
		getCurrentContext().rlineTo(x, y);
	}

	/**
	 * Add circular arc to path from last point to a new point, given centre and direction.
	 * @param direction positive for clockwise, negative for anti-clockwise.
	 * @param xCentre X coordinate of centre point of arc.
	 * @param yCentre Y coordinate of centre point of arc.
	 * @param xEnd X coordinate of end point of arc.
	 * @param yEnd Y coordinate of end point of arc.
	 */
	public void arcTo(int direction, double xCentre, double yCentre,
		double xEnd, double yEnd) throws MapyrusException
	{
		getCurrentContext().arcTo(direction, xCentre, yCentre, xEnd, yEnd);
	}

	/**
	 * Add Bezier curve to path from last point to a new point.
	 * @param xControl1 X coordinate of first Bezier control point.
	 * @param yControl1 Y coordinate of first Bezier control point.
	 * @param xControl2 X coordinate of second Bezier control point.
	 * @param yControl2 Y coordinate of second Bezier control point.
	 * @param xEnd X coordinate of end point of curve.
	 * @param yEnd Y coordinate of end point of curve.
	 */
	public void curveTo(double xControl1, double yControl1,
		double xControl2, double yControl2,
		double xEnd, double yEnd) throws MapyrusException
	{
		getCurrentContext().curveTo(xControl1, yControl1,
			xControl2, yControl2, xEnd, yEnd);
	}

	/**
	 * Adds ellipse to path.
	 * @param xMin minimum X coordinate of rectangle containing ellipse.
	 * @param yMin minimum Y coordinate of rectangle containing ellipse.
	 * @param xMax maximum X coordinate of rectangle containing ellipse.
	 * @param yMax maximum Y coordinate of rectangle containing ellipse.
	 */
	public void ellipseTo(double xMin, double yMin, double xMax, double yMax)
		throws MapyrusException
	{
		getCurrentContext().ellipseTo(xMin, yMin, xMax, yMax);
	}

	/**
	 * Resets path to empty.
	 */
	public void clearPath()
	{
		getCurrentContext().clearPath();
	}

	/**
	 * Closes path back to last moveTo point.
	 */
	public void closePath()
	{
		getCurrentContext().closePath();
	}

	/**
	 * Draws icon on page.
	 * @param filename file containing icon.
	 * @param size size for icon on page in millimetres.
	 */
	public void drawIcon(String filename, double size)
		throws IOException, MapyrusException
	{
		BufferedImage icon;
		boolean isDigits = false;
		boolean isResource = false;
		int digitsType = 0;

		/*
		 * Have we opened icon before and cached it?
		 */
		icon = (BufferedImage)mIconCache.get(filename);
		if (icon == null)
		{
			URL url;

			/*
			 * Check if icon is "inlined" as hex or binary digits.
			 */
			if (filename.length() >= 3)
			{
				char c1 = filename.charAt(0);
				char c2 = Character.toLowerCase(filename.charAt(1));
				if (c1 == '#')
				{
					isDigits = true;
					digitsType = Bitmap.HEX_DIGIT_BITMAP;
				}
				else if (c1 == '0' && c2 == 'x')
				{
					isDigits = true;
					digitsType = Bitmap.HEX_DIGIT_BITMAP;
				}
				else if ((c1 == '0' || c1 == '1') && (c2 == '0' || c2 == '1'))
				{
					isDigits = true;
					digitsType = Bitmap.BINARY_DIGIT_BITMAP;
				}
				else if (filename.startsWith("resource:"))
				{
					isResource = true;
					filename = filename.substring(9);
				}
			}

			if (isDigits)
			{
				Bitmap bitmap = new Bitmap(filename, digitsType,
					getCurrentContext().getColor());
				icon = bitmap.getBufferedImage();
			}
			else
			{
				/*
				 * Load icon from either as a resource from a JAR file,
				 * a URL, or as a plain file.
				 */
				try
				{
					if (isResource)
					{
						ClassLoader loader = this.getClass().getClassLoader();
						url = loader.getResource(filename);
					}
					else
					{
						url = new URL(filename);
					}
					icon = ImageIOWrapper.read(url, getCurrentContext().getColor());
				}
				catch (MalformedURLException e)
				{
					icon = ImageIOWrapper.read(new File(filename), getCurrentContext().getColor());
				}

				if (icon == null)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) +
						": " + filename);
				}
			}

			/*
			 * Do not cache large icons, load them each time they are needed.
			 * Do not cache an icon given as hex digits as we may want
			 * it in a different color next time.
			 */
			if ((!isDigits) && icon.getHeight() * icon.getWidth() <= 128 * 128)
				mIconCache.put(filename, icon);
		}
		getCurrentContext().drawIcon(icon, size);
	}

	/**
	 * Draws geo-referenced image on page.
	 * @param filename geo-referenced image filename.
	 * @param extras extra parameters to control display of image.
	 */
	public void drawGeoImage(String filename, String extras)
		throws IOException, MapyrusException
	{
		getCurrentContext().drawGeoImage(filename, extras);
	}

	/**
	 * Includes Encsapsulated PostScript file in page.
	 * @param EPS filename.
	 * @param size size for EPS file on page in millimetres.
	 */
	public void drawEPS(String filename, double size)
		throws IOException, MapyrusException
	{
		getCurrentContext().drawEPS(filename, size);
	}

	/**
	 * Replace path with regularly spaced points along it.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point.
	 */
	public void samplePath(double spacing, double offset) throws MapyrusException
	{
		getCurrentContext().samplePath(spacing, offset);
	}
	
	/**
	 * Replace path defining polygon with parallel stripe
	 * lines covering the polygon.
	 * @param spacing is distance between stripes.
	 * @param angle is angle of stripes, in radians, with zero horizontal.
	 */
	public void stripePath(double spacing, double angle)
	{
		getCurrentContext().stripePath(spacing, angle);
	}

	/**
	 * Shift all coordinates in path shifted by a fixed amount.
	 * @param xShift distance in millimetres to shift X coordinate values.
	 * @param yShift distance in millimetres to shift Y coordinate values.
	 */
	public void translatePath(double xShift, double yShift)
	{
		getCurrentContext().translatePath(xShift, yShift);
	}

	/**
	 * Replace path with new paths at parallel distances to original path.
	 * @param distances list of parallel distances for new paths.
	 */
	public void parallelPath(double []distances) throws MapyrusException
	{
		getCurrentContext().parallelPath(distances);
	}

	/**
	 * Replace path with selected parts of path.
	 * @param offsets offset along original path to select.
	 * @param lengths length of original path to select at each offset.
	 */
	public void selectPath(double []offsets, double []lengths)
		throws MapyrusException
	{
		getCurrentContext().selectPath(offsets, lengths);
	}

	/**
	 * Replace path defining polygon with a sinkhole point.
	 */
	public void createSinkhole()
	{
		getCurrentContext().createSinkhole();
	}

	/**
	 * Replace path with path cut to rectangle defined by (x1, y1) and (x2, y2).
	 * @param x1 lower-left corner of rectangle.
	 * @param y1 lower-left corner of rectangle.
	 * @param x2 upper-right corner of rectangle.
	 * @param y2 upper-right corner of rectangle.
	 */
	public void guillotine(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		getCurrentContext().guillotine(x1, y1, x2, y2);
	}

	/**
	 * Mark rectangular area on page (x1, y1) and (x2, y2) as protected.
	 * @param x1 lower-left corner of rectangle.
	 * @param y1 lower-left corner of rectangle.
	 * @param x2 upper-right corner of rectangle.
	 * @param y2 upper-right corner of rectangle.
	 */
	public void protect(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		getCurrentContext().setPageMask(x1, y1, x2, y2, 1);
	}

	/**
	 * Mark rectangular area on page (x1, y1) and (x2, y2) as unprotected.
	 * @param x1 lower-left corner of rectangle.
	 * @param y1 lower-left corner of rectangle.
	 * @param x2 upper-right corner of rectangle.
	 * @param y2 upper-right corner of rectangle.
	 */
	public void unprotect(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		getCurrentContext().setPageMask(x1, y1, x2, y2, 0);
	}

	/**
	 * Determine whether part of a rectangular area of page is protected.
	 * @param x1 lower-left corner of rectangle.
	 * @param y1 lower-left corner of rectangle.
	 * @param x2 upper-right corner of rectangle.
	 * @param y2 upper-right corner of rectangle.
	 * @return true if part of this rectangular region is protected.
	 */
	public boolean isProtected(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		boolean isZero = getCurrentContext().isPageMaskAllZero(x1, y1, x2, y2);
		return(!isZero);
	}

	/**
	 * Draw currently defined path.
	 */
	public void stroke() throws IOException, MapyrusException
	{
		getCurrentContext().stroke();
	}

	/**
	 * Fill currently defined path.
	 */
	public void fill() throws IOException, MapyrusException
	{
		getCurrentContext().fill();
	}

	/**
	 * Gradient fill current path.  Colors at each of the four corners
	 * of the path are defined.  Colors will gradually fade
	 * through the area covered by the path to give a smooth change
	 * of color.
	 * @param c1 color for lower left corner of path.
	 * @param c2 color for lower right corner of path.
	 * @param c3 color for upper left corner of path.
	 * @param c4 color for upper right corner of path.
	 * @param c5 color in center of path.
	 */
	public void gradientFill(Color c1, Color c2, Color c3, Color c4, Color c5)
		throws IOException, MapyrusException
	{
		getCurrentContext().gradientFill(c1, c2, c3, c4, c5);
	}

	/**
	 * Set event script for currently defined path.
	 * @param script commands to run for currently defined path.
	 */
	public void setEventScript(String script) throws IOException, MapyrusException
	{
		getCurrentContext().setEventScript(script);
	}

	/**
	 * Clip to show only area outside currently defined path,
	 * protecting what is inside path.
	 */
	public void clipOutside() throws MapyrusException
	{
		getCurrentContext().clipOutside();
	}

	/**
	 * Clip to show only area inside currently defined path.
	 */
	public void clipInside()
	{
		getCurrentContext().clipInside();
	}

	/**
	 * Draw label positioned at current point.
	 * @param label label to draw.
	 */
	public void label(String label) throws IOException, MapyrusException
	{
		getCurrentContext().label(label);
	}

	/**
	 * Draw label along currently defined path.
	 * @param spacing spacing between letters.
	 * @param offset offset along path at which to begin label.
	 * @param label label to draw.
	 */
	public void flowLabel(double spacing, double offset,
		String label) throws IOException, MapyrusException
	{
		getCurrentContext().flowLabel(spacing, offset, label);
	}

	/**
	 * Draw a table (a grid with a value in each cell) at current path position.
	 * @param extras options for table.
	 * @param list of arrays giving values in each column.
	 */
	public void drawTable(String extras, ArrayList columns) throws IOException, MapyrusException
	{
		getCurrentContext().drawTable(extras, columns);
	}

	/**
	 * Returns the number of moveTo's in path defined in current context.
	 * @return count of moveTo calls made.
	 */
	public int getMoveToCount()
	{
		int retval = getCurrentContext().getMoveToCount();
		return(retval);
	}

	/**
	 * Returns the number of lineTo's in path defined in current context.
	 * @return count of lineTo calls made for this path.
	 */
	public int getLineToCount()
	{
		int retval = getCurrentContext().getLineToCount();
		return(retval);
	}
	
	/**
	 * Returns rotation angle for each moveTo point in current path.
	 * @return list of rotation angles.
	 */	
	public ArrayList getMoveToRotations()
	{
		return(getCurrentContext().getMoveToRotations());
	}

	/**
	 * Returns coordinates for each each moveTo point in current path
	 * @return list of Point2D.Float objects.
	 */	
	public ArrayList getMoveTos() throws MapyrusException
	{
		return(getCurrentContext().getMoveTos());
	}

	/**
	 * Returns height and width of a string, drawn to current page with current font.
	 * @param s string to calculate dimensions for.
	 * @return height and width of string in millimetres.
	 */	
	public StringDimension getStringDimension(String s) throws MapyrusException
	{
		StringDimension retval;
		try
		{
			retval = getCurrentContext().getStringDimension(s, true);
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return(retval);
		
	}

	/**
	 * Get resolution of page.
	 * @return page resolution in millimetres.
	 */
	public double getResolution() throws MapyrusException
	{
		double retval = getCurrentContext().getResolution();
		return(retval);
	}

	/**
	 * Return next row from dataset.
	 * @return field values for next row.
	 */
	public Row fetchRow() throws MapyrusException
	{
		Dataset dataset = getCurrentContext().getDataset();
		if (dataset == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_DATASET));
		return(dataset.fetchRow());
	}

	/**
	 * Return names of fields in current dataset.
	 * @return names of fields.
	 */
	public String []getDatasetFieldNames() throws MapyrusException
	{
		Dataset dataset = getCurrentContext().getDataset();
		if (dataset == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_DATASET));
		String []retval = dataset.getFieldNames();
		return(retval);
	}

	/**
	 * Get stream that standard output is currently being sent to.
	 * @return standard output stream.
	 */
	public PrintStream getStdout()
	{
		return(getCurrentContext().getStdout());
	}

	/**
	 * Returns one component of a bounding box.
	 * @param part the information to be taken from the bounding box, "min.x", "width", etc.
	 * @param bounds the bounding box to be queried
	 * @return part of the information from bounding box, or null if part is unknown.
	 */
	private Argument getBoundingBoxVariable(String part, Rectangle2D bounds)
	{
		Argument retval;

		if (part.equals("min.x"))
			retval = new Argument(bounds.getMinX());
		else if (part.equals("min.y"))
			retval = new Argument(bounds.getMinY());
		else if (part.equals("max.x"))
			retval = new Argument(bounds.getMaxX());
		else if (part.equals("max.y"))
			retval = new Argument(bounds.getMaxY());
		else if (part.equals("width"))
			retval = new Argument(bounds.getWidth());
		else if (part.equals("height"))
			retval = new Argument(bounds.getHeight());
		else
			retval = null;

		return(retval);
	}

	/**
	 * Create string argument from two digit number.
	 * @param i number to create string from.
	 * @return argument containing value i.
	 */	
	private Argument setTwoDigitNumber(int i)
	{
		Argument retval;
		
		if (i >= 10)
			retval = new Argument(Argument.STRING, Integer.toString(i));
		else
			retval = new Argument(Argument.STRING, "0" + Integer.toString(i));
		return(retval);
	}

	/**
	 * Returns value of a variable.
	 * @param varName variable name to lookup.
	 * @param interpreterFilename name of file being interpreted.
	 * @return value of variable, or null if it is not defined.
	 */
	public Argument getVariableValue(String varName, String interpreterFilename)
		throws MapyrusException
	{
		Argument retval = null;
		String sub;
		char c;
		double d;
		int i;
		Rectangle2D bounds;

		if (varName.startsWith(INTERNAL_VARIABLE_PREFIX) &&
			varName.length() > INTERNAL_VARIABLE_PREFIX.length())
		{
			c = varName.charAt(INTERNAL_VARIABLE_PREFIX.length());

			/*
			 * Return internal/system variable.
			 */
			if (c == 'f' && varName.equals(INTERNAL_VARIABLE_PREFIX + "fetch.more"))
			{
				Dataset dataset = getCurrentContext().getDataset();
				if (dataset != null && dataset.hasMoreRows())
					retval = Argument.numericOne;
				else
					retval = Argument.numericZero;
			}
			else if (c == 'f' && varName.equals(INTERNAL_VARIABLE_PREFIX + "fetch.count"))
			{
				Dataset dataset = getCurrentContext().getDataset();
				if (dataset == null)
					retval = Argument.numericZero;
				else
					retval = new Argument(dataset.getFetchCount());
			}
			else if (c == 't' && varName.equals(INTERNAL_VARIABLE_PREFIX + "timer"))
			{
				/*
				 * The elapsed time in seconds since this context was created
				 * at the beginning of interpreting a file.
				 */
				retval = new Argument((System.currentTimeMillis() - mStartTime) / 1000.0);
			}
			else if (c == 't' && varName.startsWith(INTERNAL_VARIABLE_PREFIX + "time."))
			{
				sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + "time.".length());
				GregorianCalendar calendar = new GregorianCalendar();

				if (sub.equals("hour"))
					retval = setTwoDigitNumber(calendar.get(Calendar.HOUR_OF_DAY));
				else if (sub.equals("minute"))
					retval = setTwoDigitNumber(calendar.get(Calendar.MINUTE));
				else if (sub.equals("second"))
					retval = setTwoDigitNumber(calendar.get(Calendar.SECOND));
				else if (sub.equals("day"))
					retval = setTwoDigitNumber(calendar.get(Calendar.DAY_OF_MONTH));
				else if (sub.equals("day.name"))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
					retval = new Argument(Argument.STRING, sdf.format(calendar.getTime()));
				}
				else if (sub.equals("month"))
					retval = setTwoDigitNumber(calendar.get(Calendar.MONTH) + 1);
				else if (sub.equals("month.name"))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("MMMM");
					retval = new Argument(Argument.STRING, sdf.format(calendar.getTime()));
				}
				else if (sub.equals("week.of.year"))
					retval = new Argument(calendar.get(Calendar.WEEK_OF_YEAR));
				else if (sub.equals("day.of.week"))
				{
					int dayOfWeek;

					/*
					 * Convert Java Calendar values for days into values 1-7,
					 * with Monday=1 like in cron(1) tasks.
					 */
					int cd = calendar.get(Calendar.DAY_OF_WEEK);
					if (cd == Calendar.MONDAY)
						dayOfWeek = 1;
					else if (cd == Calendar.TUESDAY)
						dayOfWeek = 2;
					else if (cd == Calendar.WEDNESDAY)
						dayOfWeek = 3;
					else if (cd == Calendar.THURSDAY)
						dayOfWeek = 4;
					else if (cd == Calendar.FRIDAY)
						dayOfWeek = 5;
					else if (cd == Calendar.SATURDAY)
						dayOfWeek = 6;
					else
						dayOfWeek = 7;
					retval = new Argument(dayOfWeek);
				}
				else if (sub.equals("year"))
					retval = new Argument(calendar.get(Calendar.YEAR));
				else if (sub.equals("stamp"))	
					retval = new Argument(Argument.STRING, calendar.getTime().toString());
				else
					retval = null;
			}
			else if (c == 'v' && varName.equals(INTERNAL_VARIABLE_PREFIX + "version"))
			{
				retval = new Argument(Argument.STRING, Constants.getVersion());
			}
			else if (c == 'f' && varName.equals(INTERNAL_VARIABLE_PREFIX + "freeMemory"))
			{
				retval = new Argument(Runtime.getRuntime().freeMemory());
			}
			else if (c == 't' && varName.equals(INTERNAL_VARIABLE_PREFIX + "totalMemory"))
			{
				retval = new Argument(Runtime.getRuntime().totalMemory());
			}
			else if (c == 'f' && varName.equals(INTERNAL_VARIABLE_PREFIX + "filename"))
			{
				retval = new Argument(Argument.STRING, interpreterFilename);
			}
			else if (c == 'r' && varName.equals(INTERNAL_VARIABLE_PREFIX + "rotation"))
			{
				retval = new Argument(Math.toDegrees(getCurrentContext().getRotation()));
			}
			else if (c == 's' && varName.equals(INTERNAL_VARIABLE_PREFIX + "scale"))
			{
				retval = new Argument(getCurrentContext().getScaling());
			}
			else if (c == 'k' && varName.equals(INTERNAL_VARIABLE_PREFIX + "key.count"))
			{
				retval = new Argument(mLegendEntries.size());
			}
			else if (c == 'k' && varName.equals(INTERNAL_VARIABLE_PREFIX + "key.next"))
			{
				LegendEntry top = mLegendEntries.first();
				if (top == null)
					retval = Argument.emptyString;
				else
					retval = new Argument(Argument.STRING, top.getBlockName());
			}
			else if (varName.startsWith(INTERNAL_VARIABLE_PREFIX + PAGE_VARIABLE + "."))
			{
				sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + PAGE_VARIABLE.length() + 1);
				if (sub.equals("width"))
					retval = new Argument(getCurrentContext().getPageWidth());
				else if (sub.equals("height"))
					retval = new Argument(getCurrentContext().getPageHeight());
				else if (sub.equals("format"))
					retval = new Argument(Argument.STRING, getCurrentContext().getPageFormat());
				else if (sub.equals("resolution.mm"))
					retval = new Argument(getCurrentContext().getResolution());
				else if (sub.equals("resolution.dpi"))
				{
					retval = new Argument(Constants.MM_PER_INCH /
						getCurrentContext().getResolution());
				}
				else
					retval = null;
			}
			else if (varName.startsWith(INTERNAL_VARIABLE_PREFIX + SCREEN_VARIABLE + "."))
			{
				sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + SCREEN_VARIABLE.length() + 1);
				if (sub.equals("width"))
					retval = new Argument(Constants.getScreenWidth());
				else if (sub.equals("height"))
					retval = new Argument(Constants.getScreenHeight());
				else if (sub.equals("resolution.dpi"))
					retval = new Argument(Constants.getScreenResolution());
				else if (sub.equals("resolution.mm"))
				{
					retval = new Argument(Constants.MM_PER_INCH /
						Constants.getScreenResolution());
				}
				else
					retval = null;
			}
			else if (varName.startsWith(INTERNAL_VARIABLE_PREFIX + PATH_VARIABLE + "."))
			{
				sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + PATH_VARIABLE.length() + 1);
				if (sub.equals("length"))
					retval = new Argument(getCurrentContext().getPathLength());
				else if (sub.equals("area"))
					retval = new Argument(getCurrentContext().getPathArea());
				else if (sub.equals("centroid.x"))
					retval = new Argument(getCurrentContext().getPathCentroid().getX());
				else if (sub.equals("centroid.y"))
					retval = new Argument(getCurrentContext().getPathCentroid().getY());
				else
				{
					bounds = getCurrentContext().getBounds2D();
					retval = getBoundingBoxVariable(sub, bounds);
				}
			}
			else if (varName.startsWith(INTERNAL_VARIABLE_PREFIX + WORLDS_VARIABLE + "."))
			{
				bounds = getCurrentContext().getWorldExtents();
				sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + WORLDS_VARIABLE.length() + 1);
				if (sub.equals("scale"))
				{
					retval = new Argument(getCurrentContext().getWorldScale());
				}
				else
				{
					retval = getBoundingBoxVariable(sub, bounds);
				}
			}
			else if (varName.startsWith(INTERNAL_VARIABLE_PREFIX + DATASET_VARIABLE + "."))
			{
				Dataset dataset = getCurrentContext().getDataset();
				if (dataset == null)
				{
					/*
					 * None of these variables are meaningful if there is
					 * no dataset defined.
					 */
					retval = Argument.emptyString;
				}
				else
				{
					sub = varName.substring(INTERNAL_VARIABLE_PREFIX.length() + DATASET_VARIABLE.length() + 1);
					if (sub.equals("projection"))
					{
						String projection = dataset.getProjection();
						if (projection == null)
							retval = Argument.emptyString;
						else
							retval = new Argument(Argument.STRING, projection);
					}
					else if (sub.equals("fieldnames"))
					{
						String []fieldNames = dataset.getFieldNames();
						retval = new Argument();

						for (i = 0; i < fieldNames.length; i++)
						{
							retval.addHashMapEntry(String.valueOf(i + 1),
								new Argument(Argument.STRING, fieldNames[i]));
						}
					}
					else
					{
						Rectangle2D.Double worlds;
						worlds = dataset.getWorlds();
						retval = getBoundingBoxVariable(sub, worlds);
					}
				}
			}
			else if (varName.equals(INTERNAL_VARIABLE_PREFIX + IMAGEMAP_VARIABLE + ".x"))
			{
				if (mImagemapPoint == null)
					retval = Argument.numericMinusOne;
				else
					retval = new Argument(mImagemapPoint.x);
			}
			else if (varName.equals(INTERNAL_VARIABLE_PREFIX + IMAGEMAP_VARIABLE + ".y"))
			{
				if (mImagemapPoint == null)
					retval = Argument.numericMinusOne;
				else
					retval = new Argument(mImagemapPoint.y);
			}
		}
		else
		{
			Context context = (Context)(mStack.getLast());
			if (mStack.size() > 1 && context.hasLocalScope(varName))
			{
				/*
				 * Lookup local variable in current context.
				 */
				retval = context.getVariableValue(varName);
			}
			else	
			{
				/*
				 * Variable not defined in current context, is
				 * it set as a global in the first context instead?
				 */
				context = (Context)(mStack.getFirst());
				retval = context.getVariableValue(varName);
			
				String property = null;
				try
				{
					if (retval == null)
					{
						/*
						 * Variable not defined by user.  Is it set
						 * as a system property?
						 */
						property = System.getProperty(varName);
						if (property != null)
						{
							/*
							 * Try to convert it to a number.
							 */
							d = Double.parseDouble(property);
							retval = new Argument(d);
						}
					}
				}
				catch (SecurityException e)
				{
					/*
					 * We cannot access variable as a property so
					 * consider it to be undefined.
					 */
				}
				catch (NumberFormatException e)
				{
					/*
					 * System property was found but it is a
					 * string, not a number.
					 */
					retval = new Argument(Argument.STRING, property);
				}
			}
		}
		return(retval);
	}

	/**
	 * Indicates that a variable in the current context is to have local scope,
	 * defined in current context only and not accessible by any other context.
	 * @param varName name of variable to be treated as global
	 */
	public void setLocalScope(String varName) throws MapyrusException
	{
		getCurrentContext().setLocalScope(varName);
	}
	
	/**
	 * Define a variable in context,
	 * replacing any existing variable of the same name.
	 * @param varName name of variable to define.
	 * @param value is value for this variable
	 */
	public void defineVariable(String varName, Argument value)
	{
		Context currentContext = getCurrentContext();
		Context c;

		/*
		 * Define variable in first (global) context
		 * unless defined local.
		 */
		if (currentContext.hasLocalScope(varName))
			c = currentContext;
		else
			c = (Context)(mStack.getFirst());

		c.defineVariable(varName, value);
	}
	
	/**
	 * Define an key-value entry in a hashmap in context,
	 * replacing any existing entry with the same key.
	 * @param hashMapName name of hashmap to add entry to.
	 * @param key is key to add.
	 * @param value is value to add.
	 */
	public void defineHashMapEntry(String hashMapName, String key, Argument value)
	{
		Context currentContext = getCurrentContext();
		Context c;

		/*
		 * Define variable in first (global) context
		 * unless defined local.
		 */
		if (currentContext.hasLocalScope(hashMapName))
			c = currentContext;
		else
			c = (Context)(mStack.getFirst());

		c.defineHashMapEntry(hashMapName, key, value);
	}

	/**
	 * Add entry for a legend.
	 * @param description description text for legend entry.
	 * @param type legend type LegendEntryList.(POINT|LINE|BOX)_ENTRY.
	 * @param legendArgs arguments to procedure block when creating legend.
	 * @param legendArgIndex start index for legendArgs argument.
	 * @param nLegendArgs is number of arguments to procedure block.
	 */
	public void addLegendEntry(String description, int type,
		Argument []legendArgs, int legendArgIndex, int nLegendArgs)
	{
		String blockName = getCurrentContext().getBlockName();
		
		/*
		 * Ignore legend entries defined outside of a procedure block.
		 */
		if (blockName != null)
		{
			mLegendEntries.add(blockName, legendArgs, legendArgIndex,
				nLegendArgs, type, description);
		}
	}

	/**
	 * Return list of procedures for which legend entries are to be drawn.
	 * @return legend entry list.
	 */
	public LegendEntryList getLegendEntries()
	{
		return(mLegendEntries);
	}

	/**
	 * Set MIME type for content being returned from HTTP request.
	 * @param mimeType MIME type for content.
	 */
	public void setMimeType(String mimeType)
	{
		mMimeType = mimeType;
	}

	/**
	 * Get MIME type for content being returned from HTTP request.
	 * @param mimeType MIME type for content.
	 */
	public String getMimeType()
	{
		return(mMimeType);
	}

	/**
	 * Save current context so that it can be restored later with restoreState.
	 * @param name of procedure block that saved state will run.
	 */
	public void saveState(String blockName) throws MapyrusException
	{
		pushContext(blockName);
	}

	/**
	 * Restore context to state before saveState was called.
	 */
	public void restoreState() throws IOException, MapyrusException
	{
		popContext();
	}

	/**
	 * Pops all contexts from stack that were pushed with saveState.
	 * A ContextStack cannot be used again after this call.
	 */
	public void closeContextStack() throws IOException, MapyrusException
	{
		int nContexts = 0;

		try
		{
			do
			{
				nContexts = popContext();
			}
			while (nContexts > 0);
		}
		catch (IOException e)
		{
			/*
			 * Force all remaining contexts to be closed too.
			 */
			do
			{
				try
				{
					nContexts = popContext();
				}
				catch (IOException e1)
				{
				}
				catch (MapyrusException e2)
				{
				}
			}
			while (nContexts > 0);
			
			throw e;
		}
		catch (MapyrusException e)
		{
			/*
			 * Force all remaining contexts to be closed too.
			 */
			do
			{
				try
				{
					nContexts = popContext();
				}
				catch (IOException e1)
				{
				}
				catch (MapyrusException e2)
				{
				}
			}
			while (nContexts > 0);
			
			throw e;
		}
	}
}
