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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date;
import java.awt.geom.Rectangle2D;

import au.id.chenery.mapyrus.dataset.DatasetFactory;
import au.id.chenery.mapyrus.dataset.GeographicDataset;

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
	 * Variable names for geometry of currently defined path,
	 * world coordinate system, coordinate system we are projecting from
	 * and dataset we are reading from.
	 */
	private static final String GEOMETRY_VARIABLE = "geometry";
	private static final String WORLDS_VARIABLE = "worlds";
	private static final String UNPROJECTED_VARIABLE = "project";
	private static final String DATASET_VARIABLE = "dataset";
	
	/*
	 * Stack of contexts, with current context in last slot.
	 */
	private LinkedList mStack;

	/*
	 * Time at which this context was allocated.
	 */
	private long mStartTime;

	/**
	 * Create new stack of contexts to manage state as procedure blocks
	 * are called.
	 */
	public ContextStack()
	{
		mStack = new LinkedList();
		mStack.add(new Context());
		mStartTime = System.currentTimeMillis();
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
			boolean attributesSet = getCurrentContext().closeContext();
			mStack.removeLast();
			i--;
			
			/*
			 * If graphics attributes were set in context then set them changed
			 * in the context that is now current so they are set again
			 * here before being used.
			 */
			if (i > 0 && attributesSet)
				getCurrentContext().setAttributesChanged();
		}
		return(i);
	}

	/**
	 * Pushes copy of context at top of stack onto stack.
	 * This context is later removed with popContext().
	 */
	private void pushContext() throws MapyrusException
	{
		if (mStack.size() == MAX_STACK_LENGTH)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.RECURSION));
		}
		mStack.add(new Context(getCurrentContext()));
	}
				
	/**
	 * Sets output file for drawing to.
	 * @param filename name of image file output will be saved to
	 * @param format is image format for saved output
	 * @param width is the page width (in mm).
	 * @param height is the page height (in mm).
	 * @param resolution is resolution for output in dots per inch (DPI)
	 * @param extras contains extra settings for this output.
	 */
	public void setOutputFormat(String format, String filename,
		int width, int height, int resolution, String extras)
		throws IOException, MapyrusException
	{
		getCurrentContext().setOutputFormat(format, filename,
			width, height, resolution, extras);
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
	 * Sets font for labelling with.
	 * @param name is name of font.
	 * @param style is a style as defined in java.awt.Font class.
	 * @param size is size for labelling in millimetres.
	 */
	public void setFont(String name, int style, double size)
	{
		getCurrentContext().setFont(name, style, size);
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
	 * @param x is new scaling in X axis.
	 * @param y is new scaling in Y axis.
	 */
	public void setScaling(double x, double y) throws MapyrusException
	{
		if (x == 0.0 || y == 0.0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_SCALING));
		else if (x != 1.0 || y != 1.0)
			getCurrentContext().setScaling(x, y);
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
	 * @param x1 minimum X world coordinate.
	 * @param y1 minimum Y world coordinate.
	 * @param x2 maximum X world coordinate.
	 * @param y2 maximum Y world coordinate.
	 * @param units of world coordinates (WORLD_UNITS_METRES, WORLD_UNITS_FEET, etc)
	 */
	public void setWorlds(double x1, double y1, double x2, double y2, int units)
	{
		getCurrentContext().setWorlds(x1, y1, x2, y2, units);
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
	 * Sets dataset to read geometry from.
	 * @param type is format of dataset, for example, "text".
	 * @param name is name of dataset to open.
	 * @param extras are special options for this dataset type such as database connection
	 * information, or instructions for interpreting data.
	 * @param geometryFieldNames is list of names of fields containing geometry.
	 */
	public void setDataset(String type, String name,
		String extras, String []geometryFieldNames) throws MapyrusException
	{
		GeographicDataset dataset;
		dataset = DatasetFactory.open(type, name, extras, geometryFieldNames);
		getCurrentContext().setDataset(dataset);
	}

	/**
	 * Begin query on current dataset.  All geometry inside or crossing
	 */
	public void queryDataset() throws MapyrusException
	{
		getCurrentContext().queryDataset();
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
	 * Resets path to empty.
	 */
	public void clearPath()
	{
		getCurrentContext().clearPath();
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
	 * Draw currently defined path.
	 */
	public void stroke()
	{
		getCurrentContext().stroke();
	}

	/**
	 * Fill currently defined path.
	 */
	public void fill()
	{
		getCurrentContext().fill();
	}
		
	/**
	 * Clip to show only area inside currently defined path.
	 */
	public void clip()
	{
		getCurrentContext().clip();
	}

	/**
	 * Draw label positioned at (or along) currently defined path.
	 */
	public void label(String label)
	{
		getCurrentContext().label(label);
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
	 * Return next row from dataset.
	 * @return field values for next row.
	 */
	public Row fetchRow() throws MapyrusException
	{
		return(getCurrentContext().fetchDatasetRow());
	}

	/**
	 * Return indexes of fields in a row in current dataset that contain geometry.
	 * @return indexes of geometry fields.
	 */
	public int []getDatasetGeometryFieldIndexes() throws MapyrusException
	{
		return(getCurrentContext().getDataset().getGeometryFieldIndexes());
	}

	/**
	 * Return names of fields in current dataset.
	 * @return names of fields.
	 */
	public String []getDatasetFieldNames() throws MapyrusException
	{
		return(getCurrentContext().getDataset().getFieldNames());
	}

	/**
	 * Returns one component of a bounding box.
	 * @param part the information to be taken from the bounding box, "min.x", "width", etc.
	 * @param bounds the bounding box to be queried
	 * @return part of the information from bounding box, or "undef" if part is unknown.
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
			retval = new Argument(Argument.STRING, "undef");

		return(retval);
	}
	
	/**
	 * Returns value of a variable.
	 * @param variable name to lookup.
	 * @return value of variable, or null if it is not defined.
	 */
	public Argument getVariableValue(String varName) throws MapyrusException
	{
		Argument retval = null;
		String sub;
		double d;
		int i;
		Rectangle2D bounds;

		if (varName.startsWith(Mapyrus.PROGRAM_NAME + "."))
		{
			sub = varName.substring(Mapyrus.PROGRAM_NAME.length() + 1);
		
			/*
			 * Return internal/system variable.
			 */
			if (sub.equals("random"))
			{
				retval = new Argument(Math.random());
			}
			else if (sub.equals("timestamp"))
			{
				Date now = new Date();
				retval = new Argument(Argument.STRING, now.toString());
			}
			else if (sub.equals("elapsedtime"))
			{
				/*
				 * The elapsed time in seconds since this context was created
				 * at the beginning of interpreting a file.
				 */
				retval = new Argument((System.currentTimeMillis() - mStartTime) / 1000.0);
			}
			else if (sub.equals("version"))
			{
				retval = new Argument(Argument.STRING, Mapyrus.getVersion());
			}
			else if (sub.equals("rotation"))
			{
				retval = new Argument(Math.toDegrees(getCurrentContext().getRotation()));
			}
			else if (sub.equals("scale.x"))
			{
				retval = new Argument(getCurrentContext().getScalingX());
			}
			else if (sub.equals("scale.y"))
			{
				retval = new Argument(getCurrentContext().getScalingY());
			}
			else if (sub.equals("page.width"))
			{
				retval = new Argument(getCurrentContext().getPageWidth());
			}
			else if (sub.equals("page.height"))
			{
				retval = new Argument(getCurrentContext().getPageHeight());
			}
			else if (sub.equals("page.resolution"))
			{
				retval = new Argument(Constants.MM_PER_INCH /
					getCurrentContext().getResolution());
			}
			else if (sub.equals("import.moreRows"))
			{
				if (getCurrentContext().datasetHasMoreRows())
					retval = Argument.numericOne;
				else
					retval = Argument.numericZero;
			}
			else if (sub.equals("import.count"))
			{
				retval = new Argument(getCurrentContext().getDatasetQueryCount());
			}
			else if (sub.startsWith(GEOMETRY_VARIABLE + "."))
			{
				sub = sub.substring(GEOMETRY_VARIABLE.length() + 1);
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
			else if (sub.startsWith(WORLDS_VARIABLE + "."))
			{
				bounds = getCurrentContext().getWorldExtents();
				sub = sub.substring(WORLDS_VARIABLE.length() + 1);
				if (sub.equals("scale"))
				{
					retval = new Argument(getCurrentContext().getWorldScale());
				}
				else
				{
					retval = getBoundingBoxVariable(sub, bounds);
				}
			}
			else if (sub.startsWith(UNPROJECTED_VARIABLE + "."))
			{
				bounds = getCurrentContext().getUnprojectedExtents();
				sub = sub.substring(UNPROJECTED_VARIABLE.length() + 1);
				retval = getBoundingBoxVariable(sub, bounds);
			}
			else if (sub.startsWith(DATASET_VARIABLE + "."))
			{
				GeographicDataset dataset = getCurrentContext().getDataset();
				if (dataset == null)
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_DATASET));

				sub = sub.substring(DATASET_VARIABLE.length() + 1);
				if (sub.equals("projection"))
					retval = new Argument(Argument.STRING, dataset.getProjection());
				else if (sub.equals("fieldnames"))
				{
					String []fieldNames = dataset.getFieldNames();
					StringBuffer s = new StringBuffer();
					
					for (i = 0; i < fieldNames.length; i++)
					{
						if (i > 0)
							s.append(" ");
						s.append(fieldNames[i]);
					}
					retval = new Argument(Argument.STRING, s.toString());
				}
				else
				{
					retval = getBoundingBoxVariable(sub, dataset.getWorlds());
				}
			}
			else
			{
				/*
				 * Get value from system properties.
				 */
				try
				{
					String property = System.getProperty(sub);
					if (property != null)
						retval = new Argument(Argument.STRING, property);
					else
						retval = new Argument(Argument.STRING, "undef");
				}
				catch (SecurityException e)
				{
					retval = new Argument(Argument.STRING, "undef");
				}
			}
		}
		else
		{
			/*
			 * Search back through stack for a context defining
			 * this variable.
			 */
			i = mStack.size() - 1;
			Context context;
			
			while (i >= 0 && retval == null)
			{
				context = (Context)mStack.get(i);
				i--;
				retval = context.getVariableValue(varName);
			}
		}
		return(retval);
	}
	
	/**
	 * Define a variable in current context, replacing any existing
	 * variable of the same name.
	 * @param varName name of variable to define.
	 * @param value is value for this variable
	 */
	public void defineVariable(String varName, Argument value)
	{
		getCurrentContext().defineVariable(varName, value);
	}

	/**
	 * Save current context so that it can be restored later with restoreState.
	 */
	public void saveState() throws MapyrusException
	{
		pushContext();
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
		while (popContext() > 0)
			;
	}
}
