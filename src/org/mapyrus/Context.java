/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.mapyrus.dataset.GeographicDataset;
import org.mapyrus.font.StringDimension;
import org.mapyrus.geom.Sinkhole;
import org.mapyrus.geom.SutherlandHodgman;
import org.mapyrus.image.GradientFillFactory;
import org.mapyrus.image.ImageFilter;
import org.mapyrus.image.ImageIOWrapper;
import org.mapyrus.image.MapyrusExternalImage;
import org.mapyrus.io.GeoImageBoundingBox;
import org.mapyrus.io.ImageClippingFile;
import org.mapyrus.io.TFWFile;
import org.mapyrus.io.WMSRequestBoundingBox;

/**
 * Maintains state information during interpretation inside a single procedure block.
 * Holds the graphics attributes (color, line styles, transformations, etc.), the
 * variables set by the user and connections to external data sources.
 */
public class Context
{
	/*
	 * Units of world coordinate system.
	 */
	public static final int WORLD_UNITS_METRES = 1;
	public static final int WORLD_UNITS_FEET = 2;
	public static final int WORLD_UNITS_DEGREES = 3;

	/*
	 * Bit flags of graphical attributes.  OR-ed together
	 * to control which attributes have been set, or not set.
	 */
	private static final int ATTRIBUTE_FONT = 1;
	private static final int ATTRIBUTE_JUSTIFY = 2;
	private static final int ATTRIBUTE_COLOR = 4;
	private static final int ATTRIBUTE_BLEND = 8;
	private static final int ATTRIBUTE_LINESTYLE = 16;
	private static final int ATTRIBUTE_CLIP = 32;

	/*
	 * Page resolution to use when no output page defined.
	 */
	private static final int DEFAULT_RESOLUTION = 96;

	/*
	 * Fixed miter limit for line joins.
	 */
	private static final float MITER_LIMIT = 10.0f;

	/*
	 * Number of straight line segments in each sine wave curve.
	 */
	private static final int N_SINE_WAVE_STEPS = 20;

	/*
	 * Graphical attributes
	 */	
	private Color m_color;
	private String m_blend;
	private BasicStroke m_linestyle;
	private int m_justify;
	private String m_fontName;
	private double m_fontSize;
	private double m_fontRotation;
	private double m_fontOutlineWidth;
	private double m_fontLineSpacing;

	/*
	 * Bit flags of graphical attributes that have been changed by caller
	 * but not set yet.
	 */
	private int m_attributesPending;

	/*
	 * Bit flags of graphical attributes that have been changed in this context.
	 */
	private int m_attributesChanged;
	
	/*
	 * Transformation matrix and cumulative scaling factors and rotation.
	 */
	private AffineTransform m_ctm;
	private double m_scaling;
	private double m_rotation;

	/*
	 * Transformation matrix from world coordinates to page coordinates
	 * and the units of world coordinates.
	 */
	private AffineTransform m_worldCtm;
	private Rectangle2D.Double m_worldExtents;
	private Rectangle2D.Double m_pageWorldExtents;
	private int m_worldUnits;

	/*
	 * Coordinates making up path.
	 */
	private GeometricPath m_path;

	/*
	 * Path in context from which this context was created.
	 * Used when path is not modified in this context to avoid
	 * needlessly copying paths from one context to another.
	 */
	private GeometricPath m_existingPath;

	/*
	 * List of clip polygons making up the complete clipping path.
	 * We need a list of clip polygons instead of a single clip
	 * polygon to preserve the inside/outside of each individual
	 * clip polygon.
	 */
	private ArrayList<GeometricPath> m_clippingPaths;
	
	/*
	 * Currently defined variables and variables that are local
	 * to this context.
	 */
	private HashMap<String, Argument> m_vars;
	private HashSet<String> m_localVars;

	/*
	 * Output device we are drawing to.
	 */
	private OutputFormat m_outputFormat;
	
	/*
	 * Flag true if output and dataset defined in this context.  In this case
	 * we must close the output file and dataset when this context is finished.
	 */
	private boolean m_outputDefined;
	private boolean m_datasetDefined;

	/*
	 * Dataset currently being read from, the next row to provide to caller
	 * and the number of rows already fetched from it.
	 */
	private Dataset m_dataset;

	/*
	 * Stream that standard output is writing to.
	 */
	private PrintStream m_stdoutStream;
	private boolean m_stdoutStreamDefined;
	
	/*
	 * Name of procedure block that this context is executing in.
	 */
	private String m_blockName;

	/*
	 * Cache of image extents we have read previously from .tfw files and.
	 * OGC WMS requests.  Avoid re-reading images which will be outside
	 * page area and not visible.
	 */
	private static Hashtable<String, GeoImageBoundingBox> m_imageBoundsCache =
		new Hashtable<String, GeoImageBoundingBox>();

	/**
	 * Clear graphics context to empty state.
	 * @param c context to clear.
	 */
	private void initialiseContext(Context c)
	{
		m_color = Color.BLACK;
		m_blend = "Normal";
		m_linestyle = new BasicStroke(0.1f);
		m_justify = OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_BOTTOM;
		m_fontName = "SansSerif";
		m_fontSize = 5;
		m_fontRotation = 0;
		m_fontOutlineWidth = 0;
		m_fontLineSpacing = 1;

		m_path = m_existingPath = null;
		m_clippingPaths = null;
		m_ctm = new AffineTransform();
		m_scaling =  1.0;
		m_rotation = 0.0;

		m_attributesPending = (ATTRIBUTE_CLIP | ATTRIBUTE_COLOR |
			ATTRIBUTE_BLEND |
			ATTRIBUTE_FONT | ATTRIBUTE_JUSTIFY | ATTRIBUTE_LINESTYLE);
		m_attributesChanged = 0;

		m_worldCtm = null;
		m_pageWorldExtents = null;
	}

	/**
	 * Create a new context with reasonable default values.
	 */		
	public Context()
	{
		m_vars = null;
		m_localVars = null;

		m_outputFormat = null;
		m_outputDefined = false;
		m_datasetDefined = false;
		m_dataset = null;
		m_stdoutStream = null;
		m_stdoutStreamDefined = false;

		/*
		 * First context is outside of any procedure block.
		 */
		m_blockName = null;

		initialiseContext(this);
	}

	/**
	 * Create a new context, making a copy from an existing context.
	 * @param existing is context to copy from.
	 * @param blockName is name of procedure block which context will execute.
	 */
	public Context(Context existing, String blockName)
	{
		m_color = existing.m_color;
		m_blend = existing.m_blend;
		m_linestyle = existing.m_linestyle;
		m_justify = existing.m_justify;
		m_fontName = existing.m_fontName;
		m_fontSize = existing.m_fontSize;
		m_fontRotation = existing.m_fontRotation;
		m_fontOutlineWidth = existing.m_fontOutlineWidth;
		m_fontLineSpacing = existing.m_fontLineSpacing;

		m_ctm = new AffineTransform(existing.m_ctm);
		m_worldCtm = null;
		m_scaling = existing.m_scaling;
		m_rotation = existing.m_rotation;
		m_dataset = existing.m_dataset;

		/*
		 * Only create variable lookup tables when some values are
		 * defined locally.
		 */
		m_vars = null;
		m_localVars = null;

		/*
		 * Don't copy path -- it can be large.
		 * Just keep reference to existing path.
		 *
		 * Create path locally when needed.  If path is referenced without
		 * being created then we can reuse path from existing context instead.
		 *
		 * This saves unnecessary copying of paths when contexts are created.
		 */
		m_path = null;
		if (existing.m_path != null)
			m_existingPath = existing.m_path;
		else
			m_existingPath = existing.m_existingPath;

		/*
		 * Copy list of paths we must clip against.
		 */
		if (existing.m_clippingPaths != null)			
			m_clippingPaths = new ArrayList<GeometricPath>(existing.m_clippingPaths);
		else
			m_clippingPaths = null;

		m_outputFormat = existing.m_outputFormat;
		m_outputDefined = false;

		/*
		 * Save state in parent context so it won't be disturbed by anything
		 * that gets changed in this new context.
		 */		
		if (m_outputFormat != null)
		{
			m_outputFormat.saveState();
		}
		m_datasetDefined = false;

		m_stdoutStream = existing.m_stdoutStream;
		m_stdoutStreamDefined = false;

		m_attributesPending = existing.m_attributesPending;
		m_attributesChanged = 0;
		
		m_blockName = blockName;
	}

	private GeometricPath getDefinedPath()
	{
		GeometricPath retval;
		
		/*
		 * Return path defined in this context, or one defined
		 * in previous context if nothing set here.
		 */
		if (m_path != null)
			retval = m_path;
		else
			retval = m_existingPath;
		return(retval);
	}

	/**
	 * Return page width for output we are currently writing.
	 * @return width in millimetres.
	 */
	public double getPageWidth()
	{
		double retval;
		
		if (m_outputFormat == null)
			retval = 0.0;
		else
			retval = m_outputFormat.getPageWidth();
		
		return(retval);
	}

	/**
	 * Return page height for output we are currently writing.
	 * @return height in millimetres.
	 */
	public double getPageHeight()
	{
		double retval;
		
		if (m_outputFormat == null)
			retval = 0.0;
		else
			retval = m_outputFormat.getPageHeight();
		
		return(retval);
	}
	
	/**
	 * Return file format for output we are currently writing.
	 * @return file format.
	 */
	public String getPageFormat()
	{
		String retval;
		
		if (m_outputFormat == null)
			retval = "";
		else
			retval = m_outputFormat.getPageFormat();
		
		return(retval);
	}

	/**
	 * Return resolution of page we are writing to as a distance measurement.
	 * @return distance in millimetres between centres of adjacent pixels.
	 */
	public double getResolution() throws MapyrusException
	{
		double retval;
		
		if (m_outputFormat == null)
			retval = Constants.MM_PER_INCH / DEFAULT_RESOLUTION;
		else
			retval = m_outputFormat.getResolution();

		return(retval);
	}

	/**
	 * Set graphics attributes (color, line width, etc.) if they
	 * have changed since the last time we drew something.
	 * @param attributeMask bit mask of attributes to set: ATTRIBUTE_*.
	 */
	private void setGraphicsAttributes(int attributeMask)
		throws IOException, MapyrusException
	{
		int maskComplement = (~attributeMask);

		if ((m_attributesPending & ATTRIBUTE_FONT & attributeMask) != 0)
			m_outputFormat.setFontAttribute(m_fontName, m_fontSize, m_fontRotation, m_fontOutlineWidth, m_fontLineSpacing);
		if ((m_attributesPending & ATTRIBUTE_JUSTIFY & attributeMask) != 0)
			m_outputFormat.setJustifyAttribute(m_justify);
		if ((m_attributesPending & ATTRIBUTE_COLOR & attributeMask) != 0)
			m_outputFormat.setColorAttribute(m_color);
		if ((m_attributesPending & ATTRIBUTE_BLEND & attributeMask) != 0)
			m_outputFormat.setBlendAttribute(m_blend);
		if ((m_attributesPending & ATTRIBUTE_LINESTYLE & attributeMask) != 0)
			m_outputFormat.setLinestyleAttribute(m_linestyle);
		if ((m_attributesPending & ATTRIBUTE_CLIP & attributeMask) != 0)
			m_outputFormat.setClipAttribute(m_clippingPaths);

		/*
		 * Clear attributes we've just set -- they are no longer pending.
		 */
		m_attributesPending = (m_attributesPending & maskComplement);
	}

	/**
	 * Flag that graphics attributes have been changed by a call to a procedure.
	 * @param attributes bit flags of attributes that are changed.
	 */
	public void setAttributesChanged(int attributes)
	{
		m_attributesPending |= attributes;
		m_attributesChanged |= attributes;
	}

	/**
	 * Sets output file for drawing to.
	 * @param filename name of image file output will be saved to.
	 * @param format is image format for saving output.
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
		closeOutputFormat();

		/*
		 * Clear graphics context before beginning new page.
		 */
		initialiseContext(this);

		m_outputFormat = new OutputFormat(filename, format,
			width, height, extras, stdoutStream);
		m_outputDefined = true;
	}

    /**
     * Sets image for drawing to.
     * @param image is buffered image to draw into.
     * @param imageMapWriter is HTML image map to write to.
     * @param extras contains extra settings for this output.
     */
    public void setOutputFormat(BufferedImage image,
		PrintWriter imageMapWriter, String extras)
		throws IOException, MapyrusException
    {
		setOutputFormat(image, extras);
		if (imageMapWriter != null)
			m_outputFormat.setImageMapWriter(imageMapWriter);
    }

	/**
	 * Sets image for drawing to.
	 * @param image is buffered image to draw into.
	 * @param extras contains extra settings for this output.
	 */
	public void setOutputFormat(BufferedImage image, String extras)
		throws IOException, MapyrusException
	{
		closeOutputFormat();

		/*
		 * Clear graphics context before beginning new page.
		 */
		initialiseContext(this);

		m_outputFormat = new OutputFormat(image, extras);
		m_outputDefined = true;
	}

	/**
	 * Close any open output file being created.
	 */
	public void closeOutputFormat() throws IOException, MapyrusException
	{
		if (m_outputFormat != null && !m_outputDefined)
		{
			/*
			 * If state could be restored then no need for caller to set
			 * graphical attributes back to their old values again.
			 */
			boolean restoredState = m_outputFormat.restoreState();
			if (restoredState)
				m_attributesChanged = 0;
		}

		if (m_outputDefined)
		{
			m_outputFormat.closeOutputFormat();
			m_outputFormat = null;
			m_outputDefined = false;
		}		
	}

	/**
	 * Closes a context.  Any output started in this context is completed,
	 * memory used for context is released.
	 * A context cannot be used again after this call.
	 * @return bit flag of graphical attributes that were changed in this context
	 * and cannot be restored.
	 */
	public int closeContext() throws IOException, MapyrusException
	{
		closeOutputFormat();

		/*
		 * Close any dataset we opened in this context.
		 */
		if (m_datasetDefined)
		{
			m_dataset.close();
		}

		m_dataset = null;

		/*
		 * Close any file we opened for standard output in this context.
		 */
		if (m_stdoutStreamDefined)
		{
			if (m_stdoutStream == System.out)
				m_stdoutStream.flush();
			else
				m_stdoutStream.close();
			m_stdoutStreamDefined = false;
			m_stdoutStream = null;
		}
		m_path = m_existingPath = null;
		m_clippingPaths = null;
		m_vars = null;
		m_localVars = null;
		return(m_attributesChanged);
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
		/*
		 * Adjust width and dashes by current scaling factor.
		 */
		if (dashes == null)
		{
			m_linestyle = new BasicStroke((float)(width * m_scaling),
				cap, join, MITER_LIMIT);
		}
		else
		{
			for (int i = 0; i < dashes.length; i++)
				dashes[i] *= m_scaling;

			m_linestyle = new BasicStroke((float)(width * m_scaling), cap, join,
				MITER_LIMIT, dashes, (float)phase);
		}
		m_attributesPending |= ATTRIBUTE_LINESTYLE;
		m_attributesChanged |= ATTRIBUTE_LINESTYLE;
	}

	/**
	 * Gets current color.
	 * @return current color.
	 */
	public Color getColor()
	{
		return(m_color);
	}

	/**
	 * Sets color.
	 * @param c is new color for drawing.
	 */
	public void setColor(Color color)
	{
		m_color = color;
		m_attributesPending |= ATTRIBUTE_COLOR;
		m_attributesChanged |= ATTRIBUTE_COLOR;
	}

	/**
	 * Sets transparent color blend mode.
	 * @param blend is blend mode.
	 */
	public void setBlend(String blend)
	{
		m_blend = blend;
		m_attributesPending |= ATTRIBUTE_BLEND;
		m_attributesChanged |= ATTRIBUTE_BLEND;
	}

	/**
	 * Sets font for labelling with.
	 * @param fontName is name of font as defined in java.awt.Font class.
	 * @param fontSize is size for labelling in millimetres.
	 * @param fontOutlineWidth if non-zero, gives line width to use for drawing
	 * outline of each character of labels.
	 * @param fontLineSpacing line spacing for multi-line labels, as a
	 * multiple of the font size.
	 */
	public void setFont(String fontName, double fontSize,
		double fontOutlineWidth, double fontLineSpacing)
	{
		m_fontName = fontName;
		m_fontSize = fontSize * m_scaling;
		m_fontRotation = m_rotation;
		m_fontOutlineWidth = fontOutlineWidth * m_scaling;
		m_fontLineSpacing = fontLineSpacing;
		m_attributesChanged |= ATTRIBUTE_FONT;
		m_attributesPending |= ATTRIBUTE_FONT;
	}

	/**
	 * Adds rotation for font.
	 * @param rotation is angle in degrees.
	 * @return previous rotation value.
	 */
	private double addFontRotation(double rotation)
	{
		double retval = m_fontRotation;
		m_fontRotation += rotation;
		m_attributesChanged |= ATTRIBUTE_FONT;
		m_attributesPending |= ATTRIBUTE_FONT;
		return(retval);
	}

	/**
	 * Sets horizontal and vertical justification for labelling.
	 * @param code is bit flags of JUSTIFY_* constant values for justification.
	 * @return previous justification value.
	 */
	public int setJustify(int code)
	{
		int previous = m_justify;
		m_justify = code;
		m_attributesChanged |= ATTRIBUTE_JUSTIFY;
		m_attributesPending |= ATTRIBUTE_JUSTIFY;
		return(previous);
	}

	/**
	 * Sets scaling for subsequent coordinates.
	 * @param factor is new scaling in X and Y axes.
	 */
	public void setScaling(double factor)
	{
		m_ctm.scale(factor, factor);
		m_scaling *= factor;
	}
	
	/**
	 * Sets translation for subsequent coordinates.
	 * @param x is new point for origin on X axis.
	 * @param y is new point for origin on Y axis.
	 */
	public void setTranslation(double x, double y)
	{
		m_ctm.translate(x, y);
	}
	
	/**
	 * Sets rotation for subsequent coordinates.
	 * @param angle is rotation angle in radians, measured counter-clockwise.
	 */
	public void setRotation(double angle)
	{
		m_ctm.rotate(angle);
		m_rotation += angle;
		m_rotation = Math.IEEEremainder(m_rotation, Math.PI * 2);
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
		if (m_outputFormat == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_OUTPUT));		

		double wxDiff = wx2 - wx1;
		double wyDiff = wy2 - wy1;
		double wxMid, wyMid;

		if (px2 == 0 && py2 == 0)
		{
			/*
			 * Use whole page.
			 */
			px2 = m_outputFormat.getPageWidth();
			py2 = m_outputFormat.getPageHeight();
		}

		double pxDiff = px2 - px1;
		double pyDiff = py2 - py1;

		if (pxDiff <= 0.0 || pyDiff <= 0.0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_RANGE));

		if (wxDiff == 0.0 || wyDiff == 0.0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.ZERO_WORLD_RANGE));

		double worldAspectRatio = Math.abs(wyDiff / wxDiff);
		double pageAspectRatio = pyDiff / pxDiff;

		if (!allowDistortion)
		{
			/*
			 * Expand world coordinate range in either X or Y axis so
			 * it has same aspect ratio as area on page.
			 */
			if (worldAspectRatio > pageAspectRatio)
			{
				/*
				 * World coordinate range is taller than page coordinate
				 * system.  Expand X axis range to compensate:
				 *
				 *  PAGE    WORLDS    EXPANDED WORLDS
				 *  +---+   +---+     +-+---+-+
				 *  |   |   |   |     |<|   |>|
				 *  |___|   |   |  => |<|   |>|
				 *          |   |     |<|   |>|
				 *          +---+     +-+---+-+
				 */
				wxMid = (wx1 + wx2) / 2.0;
				wx1 = wxMid - (wxDiff / 2.0) * (worldAspectRatio / pageAspectRatio);
				wx2 = wxMid + (wxDiff / 2.0) * (worldAspectRatio / pageAspectRatio);
			}
			else if (worldAspectRatio < pageAspectRatio)
			{
				/*
				 * World coordinate range is wider than page coordinate system.
				 * Expand Y axis range.
				 */
				wyMid = (wy1 + wy2) / 2.0;
				wy1 = wyMid - (wyDiff / 2.0) * (pageAspectRatio / worldAspectRatio);
				wy2 = wyMid + (wyDiff / 2.0) * (pageAspectRatio / worldAspectRatio);
			}
		}

		m_worldExtents = new Rectangle2D.Double(Math.min(wx1, wx2),
				Math.min(wy1, wy2),
				Math.abs(wx2 - wx1), Math.abs(wy2 - wy1));

		/*
		 * Expand world coordinate range so that it fills whole page.
		 */
		wxDiff = wx2 - wx1;
		wyDiff = wy2 - wy1;
		wx1 -= wxDiff / pxDiff * px1;
		wx2 += wxDiff / pxDiff * (m_outputFormat.getPageWidth() - px2);
		wy1 -= wyDiff / pyDiff * py1;
		wy2 += wyDiff / pyDiff * (m_outputFormat.getPageHeight() - py2);

		/*
		 * Setup CTM from world coordinates to page coordinates.
		 */
		m_worldCtm = new AffineTransform();
		m_worldCtm.scale(m_outputFormat.getPageWidth() / (wx2 - wx1),
			m_outputFormat.getPageHeight() / (wy2 - wy1));
		m_worldCtm.translate(-wx1, -wy1);
		m_pageWorldExtents = new Rectangle2D.Double(Math.min(wx1, wx2),
			Math.min(wy1, wy2),
			Math.abs(wx2 - wx1), Math.abs(wy2 - wy1));
		m_worldUnits = units;
	}

	/**
	 * Transform geometry from page coordinates to world coordinates.
	 * @param arg geometry.
	 * @return transformed geometry.
	 */
	public Argument transformToWorlds(Argument arg) throws MapyrusException
	{
		AffineTransform affine = null;
		Argument retval = arg;

		try
		{
			if (m_worldCtm != null)
			{
				affine = m_worldCtm.createInverse();
				retval = arg.transformGeometry(affine);
			}
		}
		catch (NoninvertibleTransformException e)
		{
		}
		return(retval);
	}

	/**
	 * Transform geometry from world coordinates to page coordinates.
	 * @param arg geometry.
	 * @return transformed geometry.
	 */
	public Argument transformToPage(Argument arg) throws MapyrusException
	{
		Argument retval = arg;
		if (m_worldCtm != null)
			retval = arg.transformGeometry(m_worldCtm);
		return(retval);
	}

	/**
	 * Set current dataset that can be queried and fetched from.
	 * @param dataset opened dataset for subsequent queries.
	 */
	public void setDataset(GeographicDataset dataset) throws MapyrusException
	{
		/*
		 * Clear any previous dataset defined in this context.
		 */
		if (m_dataset != null && m_datasetDefined)
			m_dataset.close();
		m_dataset = new Dataset(dataset);
		m_datasetDefined = true;
	}

	/**
	 * Sets file for writing standard output to.
	 * File will automatically be closed when this context is closed.
	 * @param stdout stream to write to.
	 */
	public void setStdout(PrintStream stdout) throws IOException
	{
		/*
		 * Close any existing standard output opened in this context.
		 */
		if (m_stdoutStream != null && m_stdoutStreamDefined)
		{
			if (m_stdoutStream == System.out)
				m_stdoutStream.flush();
			else
				m_stdoutStream.close();
		}
		m_stdoutStream = stdout;
		m_stdoutStreamDefined = true;
	}

	/**
	 * Returns scaling factor in current transformation.
	 * @return scale value.
	 */
	public double getScaling()
	{
		return(m_scaling);
	}

	/**
	 * Returns rotation angle in current transformation.
	 * @return rotation in radians.
	 */
	public double getRotation()
	{
		return(m_rotation);
	}

	/**
	 * Returns world coordinate extents being shown on page.
	 * @return rectangular area covered by extents.
	 */
	public Rectangle2D.Double getWorldExtents() throws MapyrusException
	{
		Rectangle2D.Double retval;

		if (m_worldExtents != null)
		{
			retval = m_worldExtents;
		}
		else if (m_outputFormat != null)
		{
			retval = new Rectangle2D.Double(0, 0,
				m_outputFormat.getPageWidth(), m_outputFormat.getPageHeight());
				
		}
		else
		{
			retval = new Rectangle2D.Double();
		}
		return(retval);
	}

	/**
	 * Return scale of world coordinates.  The world coordinate range divided
	 * by the page size.
	 * @return scale, (1:2000) is returned as value 2000.
	 */
	public double getWorldScale()
	{
		double scale;
		double worldWidthInMM;

		if (m_outputFormat != null && m_worldCtm != null)
		{
			worldWidthInMM = m_pageWorldExtents.width;
			if (m_worldUnits == WORLD_UNITS_METRES)
				worldWidthInMM *= 1000.0;
			else if (m_worldUnits == WORLD_UNITS_FEET)
				worldWidthInMM *= (1000.0 * 0.3048);
			else
				worldWidthInMM *= (110000 * 1000.0);

			scale = worldWidthInMM / m_outputFormat.getPageWidth();
		}
		else
		{
			scale = 1.0;
		}
		return(scale);
	}
						
	/**
	 * Get dataset currently being queried.
	 * @return dataset being queried, or null if not dataset is being queried.
	 */
	public Dataset getDataset()
	{
		return(m_dataset);
	}

	/**
	 * Get stream that standard output is currently being sent to.
	 * @return standard output stream.
	 */
	public PrintStream getStdout()
	{
		return(m_stdoutStream);
	}

	/**
	 * Get name of procedure block containing statements currently being executed.
	 * @return procedure block name, or null if outside of any procedure block.
	 */
	public String getBlockName()
	{
		return(m_blockName);
	}

	/**
	 * Add point to path.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void moveTo(double x, double y) throws MapyrusException
	{
		double srcPts[] = new double[2];
		float dstPts[] = new float[2];

		srcPts[0] = x;
		srcPts[1] = y;
		
		/*
		 * Transform point from world coordinates
		 * to millimetre position on page.
		 */		
		if (m_worldCtm != null)
			m_worldCtm.transform(srcPts, 0, srcPts, 0, 1);
		m_ctm.transform(srcPts, 0, dstPts, 0, 1);
		if (m_path == null)
			m_path = new GeometricPath();

		/*
		 * Set no rotation for point.
		 */
		m_path.moveTo(dstPts[0], dstPts[1], 0.0f);
	}

	/**
	 * Add point to path with straight line segment from last point.
	 * @param x X coordinate to add to path.
	 * @param y Y coordinate to add to path.
	 */
	public void lineTo(double x, double y) throws MapyrusException
	{
		double srcPts[] = new double[2];
		float dstPts[] = new float[2];

		srcPts[0] = x;
		srcPts[1] = y;

		/*
		 * Make sure that a start point for path was defined.
		 */
		if (m_path == null || m_path.getMoveToCount() == 0)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_MOVETO));

		/*
		 * Transform point from world coordinates
		 * to millimetre position on page.
		 */
		if (m_worldCtm != null)
			m_worldCtm.transform(srcPts, 0, srcPts, 0, 1);
		m_ctm.transform(srcPts, 0, dstPts, 0, 1);
		if (m_path == null)
			m_path = new GeometricPath();
		m_path.lineTo(dstPts[0], dstPts[1]);
	}

	/**
	 * Add point to path with straight line segment relative to last point.
	 * @param x X coordinate distance to move, relative to last point.
	 * @param y Y coordinate distance to move, relative to last point.
	 */
	public void rlineTo(double x, double y) throws MapyrusException
	{
		/*
		 * Make sure that a previous point in path is defined.
		 */
		if (m_path == null || m_path.getMoveToCount() == 0)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_MOVETO));

		try
		{
			/*
			 * Calculate position of last point in path in current
			 * coordinate system.
			 */
			Point2D currentPoint = m_path.getShape().getCurrentPoint();

			/*
			 * Transform back to current world coordinate system.
			 */
// TODO should transform through mProjection too if it is not null.
			if (m_worldCtm != null)
				m_worldCtm.inverseTransform(currentPoint, currentPoint);
			m_ctm.inverseTransform(currentPoint, currentPoint);

			/*
			 * Work out absolute position, based on last point.  Then draw
			 * a line segment to that point.
			 */
			x += currentPoint.getX();
			y += currentPoint.getY();
			lineTo(x, y);
		}
		catch (NoninvertibleTransformException e)
		{
			/*
			 * The matrix should always be invertible because we prevent
			 * scaling by 0 -- the only way an AffineTransform can become singular.
			 */
			throw new MapyrusException(e.getMessage());
		}
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
		double centrePts[] = new double[2];
		double endPts[] = new double[2];
		float dstPts[] = new float[4];

		centrePts[0] = xCentre;
		centrePts[1] = yCentre;
		endPts[0] = xEnd;
		endPts[1] = yEnd;

		/*
		 * Make sure that a start point for arc was defined.
		 */
		if (m_path == null || m_path.getMoveToCount() == 0)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_ARC_START));

		/*
		 * Transform points from world coordinates
		 * to millimetre position on page.
		 */
		if (m_worldCtm != null)
		{
			m_worldCtm.transform(centrePts, 0, centrePts, 0, 1);
			m_worldCtm.transform(endPts, 0, endPts, 0, 1);
		}
		m_ctm.transform(centrePts, 0, dstPts, 0, 1);
		m_ctm.transform(endPts, 0, dstPts, 2, 1);

		m_path.arcTo(direction, dstPts[0], dstPts[1], dstPts[2], dstPts[3]);
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
		double pts[] = new double[6];
		float dstPts[] = new float[6];

		pts[0] = xControl1;
		pts[1] = yControl1;
		pts[2] = xControl2;
		pts[3] = yControl2;
		pts[4] = xEnd;
		pts[5] = yEnd;

		/*
		 * Make sure that a start point for curve was defined.
		 */
		if (m_path == null || m_path.getMoveToCount() == 0)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_BEZIER_START));

		/*
		 * Transform points from world coordinates
		 * to millimetre position on page.
		 */
		if (m_worldCtm != null)
		{
			m_worldCtm.transform(pts, 0, pts, 0, pts.length / 2);
		}
		m_ctm.transform(pts, 0, dstPts, 0, pts.length / 2);

		m_path.curveTo(dstPts[0], dstPts[1], dstPts[2], dstPts[3], dstPts[4], dstPts[5]);
	}

	/**
	 * Add Sine wave curve to path from last point to a new point.
	 * @param x X coordinate of end of path.
	 * @param y Y coordinate of end of path.
	 * @param nRepeats number of repeats of sine wave pattern.
	 * @param amplitude scaling factor for height of sine wave.
	 */
	public void sineWaveTo(double x, double y,
		double nRepeats, double amplitude) throws MapyrusException
	{
		/*
		 * Make sure that a start point for sine wave was defined.
		 */
		if (m_path == null || m_path.getMoveToCount() == 0)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_SINE_WAVE_START));

		try
		{
			/*
			 * Calculate position of last point in path in current
			 * coordinate system.
			 */
			Point2D currentPoint = m_path.getShape().getCurrentPoint();

			/*
			 * Transform back to current world coordinate system.
			 */
//TODO should transform through mProjection too if it is not null.
			if (m_worldCtm != null)
				m_worldCtm.inverseTransform(currentPoint, currentPoint);
			m_ctm.inverseTransform(currentPoint, currentPoint);

			double xDiff = x - currentPoint.getX();
			double yDiff = y - currentPoint.getY();
			double length = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

			if (length > 0 && nRepeats > 0)
			{
				double angle = Math.atan2(yDiff, xDiff);
				double cosOffset = Math.cos(angle + Math.PI / 2);
				double sinOffset = Math.sin(angle + Math.PI / 2);
				for (int i = 0; i < nRepeats; i++)
				{
					/*
					 * Draw each repeat of sine wave pattern.
					 */
					double xStart = currentPoint.getX() + (i / nRepeats) * xDiff;
					double yStart = currentPoint.getY() + (i / nRepeats) * yDiff;
					double thisRepeatLength = nRepeats - i;
					if (thisRepeatLength > 1)
						thisRepeatLength = 1;
					for (int j = 0; j <= N_SINE_WAVE_STEPS; j++)
					{
						/*
						 * Draw each sine wave pattern as many straight line segments.
						 */
						double x1 = xStart + ((double)j / N_SINE_WAVE_STEPS) * (xDiff * thisRepeatLength / nRepeats);
						double y1 = yStart + ((double)j / N_SINE_WAVE_STEPS) * (yDiff * thisRepeatLength / nRepeats);
						double offset = Math.sin((double)j / N_SINE_WAVE_STEPS * (Math.PI * 2) * thisRepeatLength) * amplitude;
						x1 += cosOffset * offset;
						y1 += sinOffset * offset;
						lineTo(x1, y1);
					}
				}
			}
		}
		catch (NoninvertibleTransformException e)
		{
			/*
			 * The matrix should always be invertible because we prevent
			 * scaling by 0 -- the only way an AffineTransform can become singular.
			 */
			throw new MapyrusException(e.getMessage());
		}
	}

	/**
	 * Add ellipse to path.
	 * @param xMin minimum X coordinate of rectangle containing ellipse.
	 * @param yMin minimum Y coordinate of rectangle containing ellipse.
	 * @param xMax maximum X coordinate of rectangle containing ellipse.
	 * @param yMax maximum Y coordinate of rectangle containing ellipse.
	 */
	public void ellipseTo(double xMin, double yMin, double xMax, double yMax)
		throws MapyrusException
	{
		double cornerPts[] = new double[4];
		float dstPts[] = new float[4];

		cornerPts[0] = xMin;
		cornerPts[1] = yMin;
		cornerPts[2] = xMax;
		cornerPts[3] = yMax;

		/*
		 * Transform points from world coordinates
		 * to millimetre position on page.
		 */
		if (m_worldCtm != null)
		{
			m_worldCtm.transform(cornerPts, 0, cornerPts, 0, 2);
		}
		m_ctm.transform(cornerPts, 0, dstPts, 0, 2);

		if (m_path == null)
			m_path = new GeometricPath();
		m_path.ellipseTo((dstPts[0] + dstPts[2]) / 2, (dstPts[1] + dstPts[3]) / 2,
			(cornerPts[2] - cornerPts[0]) * m_scaling,
			(cornerPts[3] - cornerPts[1]) * m_scaling, m_rotation);
	}

	/**
	 * Clears currently defined path.
	 */
	public void clearPath()
	{
		/*
		 * If a path was defined then clear it.
		 * If no path then clear any path we are using from another
		 * context too.
		 */
		if (m_path != null)
			m_path.reset();
		else
			m_existingPath = null;
	}

	/**
	 * Closes path back to last moveTo point.
	 */
	public void closePath()
	{
		if (m_path != null)
		{
			m_path.closePath();
		}
		else if (m_existingPath != null)
		{
			m_path = new GeometricPath(m_existingPath);
			m_path.closePath();
		}
	}

	/**
	 * Replace path with regularly spaced points along it.
	 * @param spacing is distance between points.
	 * @param offset is starting offset of first point.
	 */
	public void samplePath(double spacing, double offset) throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();

		if (path != null)
		{
			m_path = path.samplePath(spacing * m_scaling,
				offset * m_scaling, resolution);
		}
	}

	/**
	 * Replace path defining polygon with parallel stripe
	 * lines covering the polygon.
	 * @param spacing is distance between stripes.
	 * @param angle is angle of stripes, in radians, with zero horizontal.
	 */
	public void stripePath(double spacing, double angle)
	{
		GeometricPath path = getDefinedPath();

		if (path != null)
		{
			m_path = path.stripePath(spacing * m_scaling,
				angle + m_rotation);
		}
	}

	/**
	 * Replace path defining polygon with a sinkhole point.
	 */
	public void createSinkhole()
	{
		GeometricPath path = getDefinedPath();

		if (path != null)
		{
			Point2D pt = Sinkhole.calculate(path.getShape());
			m_path = new GeometricPath();
			m_path.moveTo((float)pt.getX(), (float)pt.getY(), 0);
		}
	}

	/**
	 * Replace path with path clipped against a rectangle.
	 */
	public void guillotine(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null)
		{
			double srcPts[] = new double[4];
			float dstPts[] = new float[6];

			srcPts[0] = x1;
			srcPts[1] = y1;

			srcPts[2] = x2;
			srcPts[3] = y2;

			/*
			 * Transform rectangle from world coordinates
			 * to millimetre position on page.
			 */
			if (m_worldCtm != null)
				m_worldCtm.transform(srcPts, 0, srcPts, 0, 2);
			m_ctm.transform(srcPts, 0, dstPts, 0, 2);

			double resolution = getResolution();
			x1 = Math.min(dstPts[0], dstPts[2]);
			y1 = Math.min(dstPts[1], dstPts[3]);
			x2 = Math.max(dstPts[0], dstPts[2]);
			y2 = Math.max(dstPts[1], dstPts[3]);
			Rectangle2D.Double rect = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);

			/*
			 * If path is made up of only move points then keep those
			 * that fall inside rectangle. 
			 */
			if (path.getLineToCount() == 0 && path.getMoveToCount() > 0)
			{
				ArrayList moveTos = path.getMoveTos();
				ArrayList<Double> moveToRotations = path.getMoveToRotations();
				m_path = new GeometricPath();
				for (int i = 0; i < moveTos.size(); i++)
				{
					Point2D.Float pt = (Point2D.Float)(moveTos.get(i));
					if (rect.outcode(pt) == 0)
					{
						Double rotation = moveToRotations.get(i);
						m_path.moveTo(pt.x, pt.y, rotation.doubleValue());
					}
				}
				return;
			}

			/*
			 * Return immediately if shape is completely inside or outside
			 * clip rectangle.
			 */
			GeneralPath s = path.getShape();
			Rectangle2D bounds = s.getBounds2D();
			if (rect.contains(bounds))
				return;
			if (!rect.intersects(bounds))
			{
				m_path = new GeometricPath();
				return;
			}

			GeneralPath p = SutherlandHodgman.clip(s, rect, resolution);

			/*
			 * Copy clipped path back to current path.
			 */
			if (m_path == path)
				m_path.reset();
			else
				m_path = new GeometricPath();
			float []coords = dstPts;
			PathIterator pi = p.getPathIterator(Constants.IDENTITY_MATRIX);
			while (!pi.isDone())
			{
				int segmentType = pi.currentSegment(coords);
				if (segmentType == PathIterator.SEG_MOVETO)
					m_path.moveTo(coords[0], coords[1], 0);
				else if (segmentType == PathIterator.SEG_LINETO)
					m_path.lineTo(coords[0], coords[1]);
				else if (segmentType == PathIterator.SEG_CLOSE)
					m_path.closePath();
				pi.next();
			}
		}
	}

	/**
	 * Set rectangular area in page mask to a value.
	 * @param x1 lower-left corner of rectangle.
	 * @param y1 lower-left corner of rectangle.
	 * @param x2 upper-right corner of rectangle.
	 * @param y2 upper-right corner of rectangle.
	 * @param maskValue value to set in mask for rectangular area.
	 */
	public void setPageMask(double x1, double y1, double x2, double y2, int maskValue)
		throws MapyrusException
	{
		double srcPts[] = new double[4];
		float dstPts[] = new float[6];

		srcPts[0] = x1;
		srcPts[1] = y1;

		srcPts[2] = x2;
		srcPts[3] = y2;

		/*
		 * Transform rectangle from world coordinates
		 * to millimetre position on page.
		 */
		if (m_worldCtm != null)
			m_worldCtm.transform(srcPts, 0, srcPts, 0, 2);
		m_ctm.transform(srcPts, 0, dstPts, 0, 2);

		if (m_outputFormat != null)
		{
			/*
			 * Get mask for this page and mark area as protected/unprotected.
			 */
			PageMask pageMask = m_outputFormat.getPageMask();

			pageMask.setValue(Math.round(dstPts[0]), Math.round(dstPts[1]),
				Math.round(dstPts[2]), Math.round(dstPts[3]), maskValue);
		}
	}

	/**
	 * Set page mask for region of page.
	 * @param geometry area of page to set mask for.
	 * @param maskValue value to set in mask.
	 * @throws MapyrusException
	 */
	public void setPageMask(Argument geometry, int maskValue)
		throws MapyrusException
	{
		if (m_outputFormat != null)
		{
			/*
			 * Get mask for this page and mark area as protected/unprotected.
			 */
			PageMask pageMask = m_outputFormat.getPageMask();
			GeometricPath path = new GeometricPath();
			double []coords = geometry.getGeometryValue();
			addGeometryToPath(coords, 0, path);
			pageMask.setValue(path.getShape(), maskValue);
		}
	}

	/**
	 * Set page mask for region of page covered by current path.
	 * @param maskValue value to set in mask.
	 */
	public void setPageMask(int maskValue) throws MapyrusException
	{
		if (m_outputFormat != null)
		{
			GeometricPath path = getDefinedPath();
			if (path != null)
			{
				PageMask pageMask = m_outputFormat.getPageMask();
				pageMask.setValue(path.getShape(), maskValue);
			}
		}
	}

	public boolean isPageMaskAllZero(double x1, double y1, double x2, double y2)
		throws MapyrusException
	{
		boolean retval = true;

		if (m_outputFormat != null)
		{
			double srcPts[] = new double[4];
			float dstPts[] = new float[4];

			srcPts[0] = x1;
			srcPts[1] = y1;

			srcPts[2] = x2;
			srcPts[3] = y2;

			/*
			 * Transform rectangle from world coordinates
			 * to millimetre position on page.
			 */
			if (m_worldCtm != null)
				m_worldCtm.transform(srcPts, 0, srcPts, 0, 2);
			m_ctm.transform(srcPts, 0, dstPts, 0, 2);

			/*
			 * Get mask for this page and check whether area is protected.
			 */
			PageMask pageMask = m_outputFormat.getPageMask();

			retval = pageMask.isAllZero(Math.round(dstPts[0]), Math.round(dstPts[1]),
				Math.round(dstPts[2]), Math.round(dstPts[3]));
		}
		return(retval);
	}

	private int addGeometryToPath(double []coords, int index, GeometricPath path)
		throws MapyrusException
	{
		double srcPts[] = new double[2];
		float dstPts[] = new float[2];
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
					srcPts[0] = coords[index + 1];
					srcPts[1] = coords[index + 2];

					/*
					 * Transform rectangle from world coordinates
					 * to millimetre position on page.
					 */
					if (m_worldCtm != null)
						m_worldCtm.transform(srcPts, 0, srcPts, 0, 1);
					m_ctm.transform(srcPts, 0, dstPts, 0, 1);

					if (coords[index] == Argument.MOVETO)
						path.moveTo(dstPts[0], dstPts[1], 0);
					else
						path.lineTo(dstPts[0], dstPts[1]);
					index += 3;
				}
				break;
			case Argument.GEOMETRY_MULTIPOINT:
			case Argument.GEOMETRY_MULTILINESTRING:
			case Argument.GEOMETRY_MULTIPOLYGON:
			case Argument.GEOMETRY_COLLECTION:
				for (i = 0; i < nCoords; i++)
				{
					index = addGeometryToPath(coords, index, path);
				}
				break;
		}
		return(index);
	}

	/**
	 * Determine whether a part of the page is protected.
	 * @param geometry area to check.
	 * @return true if any part of this region is protected.
	 */
	public boolean isPageMaskAllZero(Argument geometry) throws MapyrusException
	{
		boolean retval = true;

		if (m_outputFormat != null)
		{
			double coords[] = geometry.getGeometryValue();
			GeometricPath path = new GeometricPath();
			addGeometryToPath(coords, 0, path);

			/*
			 * Get mask for this page and check whether area is protected.
			 */
			PageMask pageMask = m_outputFormat.getPageMask();
			retval = pageMask.isAllZero(path.getShape());
		}
		return(retval);
	}

	/**
	 * Determine whether a part of the page covered by current path is protected.
	 * @return true if any part of path is protected.
	 */
	public boolean isPageMaskAllZero() throws MapyrusException
	{
		boolean retval = true;
		GeometricPath path = getDefinedPath();
		if (path != null)
		{
			PageMask pageMask = m_outputFormat.getPageMask();
			retval = pageMask.isAllZero(path.getShape());
		}
		return(retval);
	}

	/**
	 * Shift all coordinates in path shifted by a fixed amount.
	 * @param xShift distance in millimetres to shift X coordinate values.
	 * @param yShift distance in millimetres to shift Y coordinate values.
	 */
	public void translatePath(double xShift, double yShift)
	{
		GeometricPath path = getDefinedPath();
		double coords[] = new double[2];
		coords[0] = xShift;
		coords[1] = yShift;

		/*
		 * Scale and rotate shift to current transformation matrix.
		 */
		if (!m_ctm.isIdentity())
		{
			AffineTransform at = AffineTransform.getRotateInstance(m_rotation);
			at.scale(m_scaling, m_scaling);
		
			at.transform(coords, 0, coords, 0, 1);
		}

		if (path != null)
			m_path = path.translatePath(coords[0], coords[1]);
	}

	/**
	 * Replace path with new paths at parallel distances to original path.
	 * @param distances list of parallel distances for new paths.
	 */
	public void parallelPath(double []distances) throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();

		for (int i = 0; i < distances.length; i++)
			distances[i] *= m_scaling;

		if (path != null)
			m_path = path.parallelPath(distances, resolution);
	}

	/**
	 * Replace path with selected parts of path.
	 * @param offsets offsets along existing path to select.
	 * @param lengths length of existing path to select at each offset.
	 */
	public void selectPath(double []offsets, double []lengths)
		throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();

		if (path != null)
		{
			for (int i = 0; i < offsets.length; i++)
			{
				offsets[i] *= m_scaling;
				lengths[i] *= m_scaling;
			}

			m_path = path.selectPath(offsets, lengths, resolution);
		}
	}

	/**
	 * Reverse direction of path.
	 */
	public void reversePath() throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();

		if (path != null)
			m_path = path.reversePath(resolution);
	}

	/**
	 * Draw image icon at current point on path.
	 * @param icon icon to draw.
	 * @param size size for icon in millimetres.
	 */
	public void drawIcon(BufferedImage icon, double size)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			setGraphicsAttributes(ATTRIBUTE_CLIP);
			m_outputFormat.drawIcon(path.getMoveTos(), icon, size, m_rotation, m_scaling);
		}
	}

	/**
	 * Draws geo-referenced image on page.
	 * @param filename geo-referenced image filename.
	 * @param extras extra parameters to control display of image.
	 */
	public void drawGeoImage(String filename, String extras)
		throws IOException, MapyrusException
	{
		BufferedImage image;
		Rectangle2D.Double worldExtents = m_pageWorldExtents;
		if (worldExtents == null)
			worldExtents = getWorldExtents();
		GeoImageBoundingBox imageBounds;
		boolean isWMSRequest = false;
		URL url = null;

		/*
		 * Parse options for the image.
		 */
		GeometricPath clipPolygon = null;
		float hue = 1;
		float saturation = 1;
		float brightness = 1;
		StringTokenizer st = new StringTokenizer(extras);
		MapyrusExternalImage readerClass = null;
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("clipfile="))
			{
				String clipFilename = token.substring(9);
				ImageClippingFile clipFile = new ImageClippingFile(clipFilename, m_worldCtm);
				clipPolygon = clipFile.getClippingPolygon();
			}
			else if (token.startsWith("hue="))
			{
				String s = token.substring(4);
				try
				{
					hue = Float.parseFloat(s);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + s);
				}
			}
			else if (token.startsWith("saturation="))
			{
				String s = token.substring(11);
				try
				{
					saturation = Float.parseFloat(s);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + s);
				}
			}
			else if (token.startsWith("brightness="))
			{
				String s = token.substring(11);
				try
				{
					brightness = Float.parseFloat(s);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + s);
				}
			}
			else if (token.startsWith("readerclass="))
			{
				String className = token.substring(12);
				readerClass = new MapyrusExternalImage(className, filename, extras);
			}
		}

		if (readerClass == null)
		{
			try
			{
				url = new URL(filename);
				String urlQuery = url.getQuery();
				if (urlQuery != null)
					isWMSRequest = (urlQuery.toUpperCase().indexOf("REQUEST=GETMAP") >= 0);
			}
			catch (MalformedURLException e)
			{
			}
		}

		/*
		 * Reading large images takes a lot of time.
		 * Do not open it for display if it is not visible
		 * on the page.
		 */
		imageBounds = (GeoImageBoundingBox)m_imageBoundsCache.get(filename);
		if (imageBounds != null &&
			(!imageBounds.getBounds().intersects(worldExtents)))
		{
			return;
		}

		/*
		 * Load image.
		 */
		if (url != null)
		{
			try
			{
				image = ImageIOWrapper.read(url, getColor());
			}
			catch (IOException e)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CANNOT_OPEN_URL) +
					": " + url + Constants.LINE_SEPARATOR + e.getMessage());
			}
		}
		else if (readerClass != null)
		{
			image = readerClass.read();
		}
		else
		{
			image = ImageIOWrapper.read(new File(filename), getColor());
		}

		if (image == null)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FORMAT) +
				": " + filename);
		}

		if (imageBounds == null)
		{
			if (isWMSRequest)
				imageBounds = new WMSRequestBoundingBox(url);
			else if (readerClass != null)
				imageBounds = readerClass;
			else
				imageBounds = new TFWFile(filename, image);

			/*
			 * Do not put WMS requests in cache because it is unlikely
			 * that exactly the same request will be used in the future.
			 * 
			 * Do not put images from external classes in cache because
			 * we have no idea how they operate.
			 */
			if (!(isWMSRequest || readerClass != null))
				m_imageBoundsCache.put(filename, imageBounds);
		}

		/*
		 * Convert world coordinate bounding box of image to millimetre
		 * positions on the page.
		 */
		Rectangle2D bounds = imageBounds.getBounds();
		double []cornerPts = new double[4];

		/*
		 * Save original path and clipping path.
		 */
		GeometricPath pathCopy = m_path;
		ArrayList<GeometricPath> clippingPathCopy;
		if (m_clippingPaths != null)
			clippingPathCopy = new ArrayList<GeometricPath>(m_clippingPaths);
		else
			clippingPathCopy = null;

		if (clipPolygon != null)
		{
			/*
			 * Temporarily set path to clip polygon read from file
			 * and clip to it.
			 */
			m_outputFormat.saveState();
			m_path = clipPolygon;
			clipInside();
		}

		setGraphicsAttributes(ATTRIBUTE_CLIP);

		if (worldExtents.getBounds().contains(bounds))
		{
			cornerPts[0] = bounds.getMinX();
			cornerPts[1] = bounds.getMinY();
			cornerPts[2] = bounds.getMaxX();
			cornerPts[3] = bounds.getMaxY();

			if (m_worldCtm != null)
				m_worldCtm.transform(cornerPts, 0, cornerPts, 0, 2);

			if (hue != 1 || saturation != 1 || brightness != 1)
			{
				ImageFilter.filter(image, hue, saturation, brightness);
			}

			/*
			 * Entire image is on page.  Draw it all.
			 */
			m_outputFormat.drawGeoImage(image, cornerPts[0], cornerPts[1],
					cornerPts[2] - cornerPts[0],
					cornerPts[3] - cornerPts[1]);
		}
		else if (worldExtents.intersects(bounds))
		{
			/*
			 * Image is partially on page.  Only draw the part of the
			 * image that is visible.
			 */
			double x1factor = (worldExtents.getMinX() - bounds.getMinX()) /
				bounds.getWidth();
			double y1factor = (worldExtents.getMinY() - bounds.getMinY()) /
				bounds.getHeight();
			double x2factor = (worldExtents.getMaxX() - bounds.getMinX()) /
				bounds.getWidth();
			double y2factor = (worldExtents.getMaxY() - bounds.getMinY()) /
				bounds.getHeight();

			if (x1factor > 1)
				x1factor = 1;
			else if (x1factor < 0)
				x1factor = 0;

			if (y1factor > 1)
				y1factor = 1;
			else if (y1factor < 0)
				y1factor = 0;

			if (x2factor > 1)
				x2factor = 1;
			else if (x2factor < 0)
				x2factor = 0;
			
			if (y2factor > 1)
				y2factor = 1;
			else if (y2factor < 0)
				y2factor = 0;

			double wx1 = bounds.getMinX() + bounds.getWidth() * x1factor;
			double ix1 = image.getWidth() * x1factor;
			double wy1 = bounds.getMinY() + bounds.getHeight() * y1factor;
			double iy1 = image.getHeight() * y1factor;
			double wx2 = bounds.getMinX() + bounds.getWidth() * x2factor;
			double ix2 = image.getWidth() * x2factor;
			double wy2 = bounds.getMinY() + bounds.getHeight() * y2factor;
			double iy2 = image.getHeight() * y2factor;

			cornerPts[0] = wx1;
			cornerPts[1] = wy1;
			cornerPts[2] = wx2;
			cornerPts[3] = wy2;

			if (m_worldCtm != null)
				m_worldCtm.transform(cornerPts, 0, cornerPts, 0, 2);

			int iWidth = (int)Math.round(ix2 - ix1);
			int iHeight = (int)Math.round(iy2 - iy1);
			double iy = image.getHeight() - iy2;

			/*
			 * Protect against round-off errors calculating pixel positions outside image.
			 */
			int x1 = (int)Math.round(ix1);
			int y1 = (int)Math.round(iy);
			int w = Math.max(iWidth, 1);
			int h = Math.max(1, iHeight);
			if (x1 + w > image.getWidth())
				x1--;
			if (x1 < 0)
				x1 = 0;
			if (y1 + h > image.getHeight())
				y1--;
			if (y1 < 0)
				y1 = 0;
			image = image.getSubimage(x1, y1, w, h);

			if (hue != 1 || saturation != 1 || brightness != 1)
			{
				ImageFilter.filter(image, hue, saturation, brightness);
			}

			m_outputFormat.drawGeoImage(image, cornerPts[0], cornerPts[1],
				cornerPts[2] - cornerPts[0], cornerPts[3] - cornerPts[1]);
		}

		/*
		 * Remove any clipping polygon we set for the image.
		 */
		if (clipPolygon != null)
		{
			m_path = pathCopy;
			m_clippingPaths = clippingPathCopy;
			if (!m_outputFormat.restoreState())
			{
				m_attributesChanged |= ATTRIBUTE_CLIP;
				m_attributesPending |= ATTRIBUTE_CLIP;
			}
		}
	}

	/**
	 * Includes Encsapsulated PostScript file in page.
	 * @param EPS filename.
	 * @param size size for EPS file on page in millimetres.
	 */
	public void drawEPS(String filename, double size)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			setGraphicsAttributes(ATTRIBUTE_CLIP);
			m_outputFormat.drawEPS(path.getMoveTos(), filename,
				size, m_rotation, m_scaling);
		}
	}

	/**
	 * Includes Scalable Vector Graphics file in page.
	 * @param SVG filename.
	 * @param size size for SVG file on page in millimetres.
	 */
	public void drawSVG(String filename, double size)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			setGraphicsAttributes(ATTRIBUTE_CLIP);
			m_outputFormat.drawSVG(path.getMoveTos(), filename,
				size, m_rotation, m_scaling);
		}
	}

	/**
	 * Add Scalable Vector Graphics code to page.
	 * @param xml XML elements to add to SVG file.
	 */
	public void addSVGCode(String xml)
		throws IOException, MapyrusException
	{
		m_outputFormat.addSVGCode(xml);
	}

	/**
	 * Includes PDF file in page.
	 * @param SVG filename.
	 * @param page page number in PDF file to display.
	 * @param size size for SVG file on page in millimetres.
	 */
	public void drawPDF(String filename, int page, double size)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			setGraphicsAttributes(ATTRIBUTE_CLIP);
			m_outputFormat.drawPDF(path.getMoveTos(), filename, page,
				size, m_rotation, m_scaling);
		}
	}

	/**
	 * Draw currently defined path.
	 * @param xmlAttributes XML attributes to add for SVG output.
	 */
	public void stroke(String xmlAttributes) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			setGraphicsAttributes(ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_LINESTYLE|ATTRIBUTE_CLIP);
			m_outputFormat.stroke(path.getShape(), xmlAttributes);
		}
	}

	/**
	 * Fill currently defined path.
	 * @param xmlAttributes XML attributes to add for SVG output.
	 */
	public void fill(String xmlAttributes) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		
		if (path != null && m_outputFormat != null)
		{	
			setGraphicsAttributes(ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_CLIP);
			m_outputFormat.fill(path.getShape(), xmlAttributes);
		}
	}

	/**
	 * Fill currently defined path with gradient fill pattern.
	 * @param c1 color for lower-left corner of image.
	 * @param c2 color for lower-right corner of image.
	 * @param c3 color for top-left corner of image.
	 * @param c4 color for top-right corner of image.
	 * @param c5 color for center of image, if null then not used.
	 */
	public void gradientFill(Color c1, Color c2, Color c3, Color c4, Color c5)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();

		if (path != null && m_outputFormat != null)
		{
			if (m_outputFormat.getPageFormat().equals("svg"))
			{
				boolean isVertical = c1.equals(c2); 
				if (isVertical)
					m_outputFormat.gradientFill(path.getShape(), isVertical, c1, c3);
				else
					m_outputFormat.gradientFill(path.getShape(), isVertical, c1, c2);
			}
			else
			{

				Rectangle2D bounds = path.getBounds2D();
	
				/*
				 * Temporarily set clipping path to be inside the current path.
				 */
				m_outputFormat.saveState();
				if (m_clippingPaths == null)
					m_clippingPaths = new ArrayList<GeometricPath>();
				ArrayList<GeometricPath> copy = new ArrayList<GeometricPath>(m_clippingPaths);
				clipInside();
				setGraphicsAttributes(ATTRIBUTE_CLIP);

				/*
				 * Draw gradiated image pattern covering complete current path.
				 */
				BufferedImage image = GradientFillFactory.getImage(c1, c2, c3, c4, c5);
				ArrayList<Point2D> coords = new ArrayList<Point2D>();
				coords.add(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
				m_outputFormat.drawIcon(coords, image,
					Math.max(bounds.getWidth(), bounds.getHeight()), 0.0, 1.0);
	
				/*
				 * Restore original clipping path.
				 */
				m_clippingPaths = copy;
				m_outputFormat.setClipAttribute(m_clippingPaths);
				m_outputFormat.restoreState();
			}
		}
	}

	/**
	 * Set event script for currently defined path.
	 * @param script commands to run for currently defined path.
	 */
	public void setEventScript(String script) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		
		if (path != null && m_outputFormat != null)
		{	
			m_outputFormat.setEventScript(path.getShape(), script);
		}
	}

	/**
	 * Clip to show only area outside currently defined path,
	 * protecting what is inside path.
	 */
	public void clipOutside() throws MapyrusException
	{
		GeometricPath path = getDefinedPath();
		GeometricPath protectedPath;

		if (path != null && m_outputFormat != null)
		{
			/*
			 * Add a rectangle around the edge of the page as the new polygon
			 * perimeter.  The path becomes an island in the polygon (with
			 * opposite direction so winding rule works) and only
			 * the area outside the path is then visible.
			 */
			float width = (float)(m_outputFormat.getPageWidth());
			float height = (float)(m_outputFormat.getPageHeight());

			protectedPath = new GeometricPath();
			protectedPath.moveTo(0.0f, 0.0f, 0.0);
			if (path.isClockwise(getResolution()))
			{
				/*
				 * Outer rectange should be anti-clockwise.
				 */
				protectedPath.lineTo(width, 0.0f);
				protectedPath.lineTo(width, height);
 				protectedPath.lineTo(0.0f, height);
			}
			else
			{
				/*
				 * Outer rectangle should be clockwise.
				 */
				protectedPath.lineTo(0.0f, height);
				protectedPath.lineTo(width, height);
				protectedPath.lineTo(width, 0.0f);
			}
			protectedPath.closePath();
			protectedPath.append(path, false);

			m_attributesPending |= ATTRIBUTE_CLIP;
			m_attributesChanged |= ATTRIBUTE_CLIP;
			m_outputFormat.clip(protectedPath.getShape());

			/*
			 * Add this polygon to list of paths we are clipping against.
			 */
			if (m_clippingPaths == null)
				m_clippingPaths = new ArrayList<GeometricPath>();
			m_clippingPaths.add(protectedPath);
		}
	}

	/**
	 * Clip to show only inside of currently defined path.
	 */
	public void clipInside()
	{
		GeometricPath path = getDefinedPath();
		GeometricPath clipPath;

		if (path != null && m_outputFormat != null)
		{
			clipPath = new GeometricPath(path);
			if (m_clippingPaths == null)
				m_clippingPaths = new ArrayList<GeometricPath>();
			m_clippingPaths.add(clipPath);
			m_attributesPending |= ATTRIBUTE_CLIP;
			m_attributesChanged |= ATTRIBUTE_CLIP;
			if (m_outputFormat != null)
			{
				m_outputFormat.clip(clipPath.getShape());
			}
		}
	}

	/**
	 * Draw label positioned at (or along) currently defined path.
	 * @param label is string to draw on page.
	 */
	public void label(String label) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		
		if (path != null && m_outputFormat != null)
		{	
			setGraphicsAttributes(ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_FONT|ATTRIBUTE_JUSTIFY|ATTRIBUTE_CLIP);
			m_outputFormat.label(path.getMoveTos(), label);
		}
	}



	/**
	 * Draw label along currently defined path.
	 * @param spacing spacing between letters.
	 * @param offset offset along path at which to begin label.
	 * @param label label to draw.
	 */
	public void flowLabel(double spacing, double offset, String label)
		throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		Point2D.Double startPt, endPt;

		if (path != null && path.getMoveToCount() > 0 && m_outputFormat != null)
		{
			spacing *= m_scaling;
			offset *= m_scaling;
			int nLetters = label.length();

			/*
			 * Find length of each letter in string, and total string
			 * length including spaces between characters.
			 */
			String []letters = new String[nLetters];
			double []stringWidths = new double[nLetters];
			double totalStringWidth = 0.0;
			for (int i = 0; i < nLetters; i++)
			{
				if (i > 0)
					totalStringWidth += spacing;

				letters[i] = label.substring(i, i + 1);
				stringWidths[i] = getStringDimension(letters[i], false).getWidth();
				totalStringWidth += stringWidths[i];
			}

			if ((m_justify & OutputFormat.JUSTIFY_RIGHT) != 0)
				offset -= totalStringWidth;
			if ((m_justify & OutputFormat.JUSTIFY_CENTER) != 0)
				offset -= totalStringWidth / 2;

			/*
			 * Make list of points of the line segments along which label 
			 * will be drawn.
			 */
			ArrayList<Point2D> pathPoints = path.getSubPathPoints(offset, totalStringWidth);
			
			/*
			 * If path could not be calculated then just use horizontal line from
			 * first point of path.
			 */
			if (pathPoints.isEmpty())
			{
				Point2D.Float pt = (Point2D.Float)path.getMoveTos().get(0);
				startPt = new Point2D.Double(pt.getX(), pt.getY());
				endPt = new Point2D.Double(startPt.x + Math.cos(m_rotation) * totalStringWidth,
					startPt.y + Math.sin(m_rotation) * totalStringWidth);
				pathPoints.add(startPt);
				pathPoints.add(endPt);
			}

			/*
			 * Find direction of path.  If it goes left or down then walk through it
			 * in reverse direction so that label appears the right way up.
			 */
			startPt = (Point2D.Double)pathPoints.get(0);
			endPt = (Point2D.Double)pathPoints.get(pathPoints.size() - 1);
			double angle = Math.atan2(endPt.y - startPt.y, endPt.x - startPt.x);
			int startIndex, endIndex;
			int step;
			int justify;

			if (startPt.distanceSq(endPt) > 0 && Math.abs(angle + m_rotation) > Math.PI / 2)
			{
				startIndex = pathPoints.size() - 1;
				endIndex = 0;
				step = -1;
				justify = OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_TOP;
			}
			else
			{
				startIndex = 0;
				endIndex = pathPoints.size() - 1;
				step = 1;
				justify = OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_BOTTOM;
			}

			/*
			 * Draw letters along each line segment.
			 */
			int letterIndex = 0;
			ArrayList<Point2D> pointPath = new ArrayList<Point2D>();

			offset = 0;
			while (letterIndex < nLetters && startIndex != endIndex)
			{
				startPt = (Point2D.Double)pathPoints.get(startIndex);
				endPt = (Point2D.Double)pathPoints.get(startIndex + step);

				double xDiff = endPt.x - startPt.x;
				double yDiff = endPt.y - startPt.y;
				double dist = Math.sqrt(xDiff * xDiff + yDiff * yDiff); 
				angle = Math.atan2(yDiff, xDiff);
				double cosAngle = Math.cos(angle);
				double sinAngle = Math.sin(angle);

				/*
				 * Set rotation of font to follow current line segment.
				 */
				addFontRotation(angle);
				int previousJustify = setJustify(justify);
				setGraphicsAttributes(ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_FONT|ATTRIBUTE_JUSTIFY|ATTRIBUTE_CLIP);

				/*
				 * Draw each of the letters that will fit along
				 * this line segment.
				 *
				 * If path is not long enough for all letters, then
				 * last line segment is extended to fit in all letters.
				 */
				boolean isLastSegment = (startIndex + step == endIndex);
				while (letterIndex < nLetters &&
					(offset + stringWidths[letterIndex] / 2 < dist || isLastSegment))
				{
					double x = startPt.x + offset * cosAngle;
					double y = startPt.y + offset * sinAngle;
					pointPath.clear();
					pointPath.add(new Point2D.Double(x, y));

					m_outputFormat.label(pointPath, letters[letterIndex]);
					offset += stringWidths[letterIndex] + spacing;
					letterIndex++;
				}

				/*
				 * Continue remaining letters on next line segment.
				 */
				offset -= dist;
				startIndex += step;

				addFontRotation(-angle);
				setJustify(previousJustify);
			}
		}
	}

	/**
	 * Draw a table (a grid with a value in each cell) at current path position.
	 * @param extras options for table.
	 * @param list of arrays giving values in each column.
	 */
	public void drawTable(String extras, ArrayList columns) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		if (path == null || m_outputFormat == null)
			return;

		double columnWidths[];
		double rowHeights[] = null;
		double minRowHeight = getStringDimension("X", false).getHeight();
		double yPadding = minRowHeight / 4;
		double xPadding;
		Object primaryKeys[] = null;

		ArrayList<Color> bgColors = new ArrayList<Color>();
		boolean drawBorders = true;
		double []justify = null;
		int sortColumn = -1;
		int sortOrder = 1;

		/*
		 * Override default settings for table with options given by user.
		 */
		StringTokenizer st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("background="))
			{
				String colorNames = token.substring(11);
				StringTokenizer st2 = new StringTokenizer(colorNames, ",");
				while (st2.hasMoreTokens())
				{
					String colorName = st2.nextToken();
					Color c = ColorDatabase.getColor(colorName, 255, getColor());
					if (c == null)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.COLOR_NOT_FOUND) +
							": " + colorName);
					}
					bgColors.add(c);
				}
			}
			else if (token.startsWith("borders="))
			{
				String flag = token.substring(8);
				drawBorders = flag.equalsIgnoreCase("true");
			}
			else if (token.startsWith("justify="))
			{
				String justifyValues = token.substring(8);
				StringTokenizer st2 = new StringTokenizer(justifyValues, ",");
				justify = new double[columns.size()];
				int counter = 0;
				while (st2.hasMoreTokens() && counter < columns.size())
				{
					String s = st2.nextToken().toLowerCase();
					double offset = 0;
					if (s.equals("right"))
						offset = 1;
					else if (s.equals("center") || s.equals("centre"))
						offset = 0.5;
					justify[counter] = offset;
					counter++;
				}
			}
			else if (token.startsWith("sortcolumn="))
			{
				String column = token.substring(11);
				try
				{
					sortColumn = Integer.parseInt(column);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLUMN) +
						": " + column);
				}
				if (sortColumn < 1 || sortColumn > columns.size())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLUMN) +
						": " + column);
				}

			}
			else if (token.startsWith("sortorder="))
			{
				token = token.substring(10);
				if (token.startsWith("desc"))
					sortOrder = -1;
			}
		}

		if (sortColumn < 0)
		{
			primaryKeys = ((Argument)columns.get(0)).getHashMapKeys();
		}
		else
		{
			Argument arg = (Argument)columns.get(sortColumn - 1);
			primaryKeys = arg.getHashMapKeysSortedByValue();
			if (sortOrder < 0)
			{
				/*
				 * Reverse array order.
				 */
				Object reversed[] = new Object[primaryKeys.length];
				for (int i = 0; i < primaryKeys.length; i++)
				{
					reversed[i] = primaryKeys[primaryKeys.length - i - 1];
				}
				primaryKeys = reversed;
			}
		}
		rowHeights = new double[primaryKeys.length];
		Arrays.fill(rowHeights, minRowHeight);
		columnWidths = new double[columns.size()];

		/*
		 * Calculate width needed for each column and height needed for each row.
		 */
		for (int i = 0; i < columnWidths.length; i++)
		{
			columnWidths[i] = 0;
			Argument arg = (Argument)columns.get(i);

			for (int j = 0; j < primaryKeys.length; j++)
			{
				String s = arg.getHashMapEntry(primaryKeys[j].toString()).toString();
				StringDimension dim = getStringDimension(s, false);
				if (dim.getWidth() > columnWidths[i])
					columnWidths[i] = dim.getWidth();
				if (dim.getHeight() > rowHeights[j])
					rowHeights[j] = dim.getHeight();
			}
		}

		/*
		 * Save state so we can temporarily change label justification.
		 */
		m_outputFormat.saveState();
		int oldJustify = setJustify(OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_TOP);
		int attributeMask = ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_FONT|
			ATTRIBUTE_JUSTIFY|ATTRIBUTE_CLIP|ATTRIBUTE_LINESTYLE;
		setGraphicsAttributes(attributeMask);

		ArrayList moveTos = path.getMoveTos();
		for (int i = 0; i < moveTos.size(); i++)
		{
			Point2D.Float pt = (Point2D.Float)moveTos.get(i);
			Point2D.Float ptCopy = (Point2D.Float)pt.clone();

			for (int j = 0; j < columns.size(); j++)
			{
				ptCopy.y = pt.y;

				xPadding = columnWidths[j] / 20;
				if (xPadding < 1)
					xPadding = 1;
				if (xPadding > 20)
					xPadding = 20;

				if (justify != null)
				{
					/*
					 * Set justification for labels in this column.
					 */
					if (justify[j] == 1)
						setJustify(OutputFormat.JUSTIFY_RIGHT | OutputFormat.JUSTIFY_TOP);
					else if (justify[j] == 0)
						setJustify(OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_TOP);
					else
						setJustify(OutputFormat.JUSTIFY_CENTER | OutputFormat.JUSTIFY_TOP);
					setGraphicsAttributes(ATTRIBUTE_JUSTIFY);
				}

				Argument arg = (Argument)columns.get(j);
				for (int k = 0; k < primaryKeys.length; k++)
				{
					/*
					 * Draw box around each entry in the table.
					 */
					GeometricPath box = new GeometricPath();
					float x1 = (float)(ptCopy.x);
					float y1 = (float)(ptCopy.y - yPadding - rowHeights[k] - yPadding);
					float x2 = (float)(x1 + xPadding + columnWidths[j] + xPadding);
					float y2 = (float)(ptCopy.y); 
					box.moveTo(x1, y1, 0);
					box.lineTo(x1, y2);
					box.lineTo(x2, y2);
					box.lineTo(x2, y1);
					box.closePath();

					if (!bgColors.isEmpty())
					{
						m_outputFormat.saveState();
						int slotIndex = k * columns.size() + j;
						Color c = (Color)bgColors.get(slotIndex % bgColors.size());
						m_outputFormat.setColorAttribute(c);
						m_outputFormat.fill(box.getShape(), null);
						m_outputFormat.restoreState();
						m_attributesChanged |= ATTRIBUTE_COLOR;
						m_attributesPending |= ATTRIBUTE_COLOR;
						setGraphicsAttributes(ATTRIBUTE_COLOR);
					}
					if (drawBorders)
						m_outputFormat.stroke(box.getShape(), null);

					String s = arg.getHashMapEntry(primaryKeys[k].toString()).toString();
					Point2D.Float labelPt = new Point2D.Float();
					labelPt.x = (float)(ptCopy.x + xPadding);
					labelPt.y = (float)(ptCopy.y - yPadding);

					/*
					 * Justify the label too. 
					 */
					if (justify != null)
						labelPt.x += justify[j] * columnWidths[j];

					ArrayList<Point2D> ptList = new ArrayList<Point2D>();
					ptList.add(labelPt);
					m_outputFormat.label(ptList, s);

					ptCopy.y -= (yPadding + rowHeights[k] + yPadding); 
				}
				ptCopy.x += (xPadding + columnWidths[j] + xPadding);
			}
		}

		m_outputFormat.restoreState();
		setJustify(oldJustify);
		m_attributesChanged |= attributeMask;
		m_attributesPending |= attributeMask;
	}

	/**
	 * Draw a tree of labels at current path position.
	 * @param extras options for tree.
	 * @param tree array containing labels for tree.
	 */
	public void drawTree(String extras, Argument tree) throws IOException, MapyrusException
	{
		GeometricPath path = getDefinedPath();
		if (path == null || m_outputFormat == null)
			return;

		StringDimension dim = getStringDimension("X", false);
		String delimiter = null;

		/*
		 * Override default settings for tree with options given by user.
		 */
		StringTokenizer st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("delimiter="))
			{
				delimiter = token.substring(10);
			}
		}

		Object keys[] = tree.getHashMapKeys();

		/*
		 * Save state so we can temporarily change label justification.
		 */
		m_outputFormat.saveState();
		int oldJustify = setJustify(OutputFormat.JUSTIFY_LEFT | OutputFormat.JUSTIFY_TOP);
		int attributeMask = ATTRIBUTE_COLOR|ATTRIBUTE_BLEND|ATTRIBUTE_FONT|
			ATTRIBUTE_JUSTIFY|ATTRIBUTE_CLIP|ATTRIBUTE_LINESTYLE;
		setGraphicsAttributes(attributeMask);

		ArrayList moveTos = path.getMoveTos();
		for (int i = 0; i < moveTos.size(); i++)
		{
			Point2D.Float pt = (Point2D.Float)moveTos.get(i);
			Point2D.Float ptCopy = (Point2D.Float)pt.clone();
			LinkedList<String> lastEntry = new LinkedList<String>();
			LinkedList<Float> lastY2 = new LinkedList<Float>();

			for (int j = 0; j < keys.length; j++)
			{
				String val = tree.getHashMapEntry(keys[j].toString()).getStringValue();
				if (delimiter == null)
					st = new StringTokenizer(val);
				else
					st = new StringTokenizer(val, delimiter);

				LinkedList<String> entry = new LinkedList<String>();
				while (st.hasMoreTokens())
					entry.add(st.nextToken());

				int indent = 0;
				ListIterator it = entry.listIterator();
				ListIterator lastIt = lastEntry.listIterator();
				while (it.hasNext() && lastIt.hasNext())
				{
					String token = (String)it.next();
					String lastToken = (String)lastIt.next();
					if (token.equals(lastToken))
						indent++;
					else
						break;
				}

				/*
				 * Draw 'L' line dropping from element above and the
				 * label to the right of it.
				 */
				for (int k = indent; k < entry.size(); k++)
				{
					String s = (String)entry.get(k);

					GeometricPath box = new GeometricPath();
					float x1 = (float)(ptCopy.x + dim.getWidth() * k - dim.getWidth() / 2);
					float y1 = (float)(ptCopy.y - dim.getHeight() / 5);
					float x2 = (float)(x1 + dim.getWidth() / 2);
					float y2 = (float)(y1 - dim.getHeight() / 2);
					if (k < lastY2.size())
					{
						/*
						 * Draw line down from last entry at this level of indentation.
						 */
						y1 = ((Float)(lastY2.get(k))).floatValue();
						int l = lastY2.size() - k;
						while (l-- > 0)
							lastY2.removeLast();
					}
					box.moveTo(x1, y1, 0);
					box.lineTo(x1, y2);
					box.lineTo(x2, y2);
					if (k > 0)
						m_outputFormat.stroke(box.getShape(), null);

					Point2D.Float labelPt = new Point2D.Float();
					labelPt.x = (float)(ptCopy.x + dim.getWidth() * k);
					if (k > 0)
						labelPt.x += dim.getWidth() / 5; 
					labelPt.y = (float)ptCopy.y;

					ArrayList<Point2D> ptList = new ArrayList<Point2D>();
					ptList.add(labelPt);
					m_outputFormat.label(ptList, s);
					StringDimension sDim = getStringDimension(s, false);

					ptCopy.y -= Math.max(sDim.getHeight(), dim.getHeight());
					lastY2.add(k, new Float(y2));
				}
				lastEntry = entry;
			}
		}

		m_outputFormat.restoreState();
		setJustify(oldJustify);
		m_attributesChanged |= attributeMask;
		m_attributesPending |= attributeMask;
	}

	/**
	 * Returns the number of moveTo's in path defined in this context.
	 * @return count of moveTo calls made.
	 */
	public int getMoveToCount()
	{
		int retval;
		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = 0;
		else
			retval = path.getMoveToCount();
		return(retval);
	}

	/**
	 * Returns the number of lineTo's in path defined in this context.
	 * @return count of lineTo calls made for this path.
	 */
	public int getLineToCount()
	{
		int retval;
		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = 0;
		else
			retval = path.getLineToCount();
		return(retval);
	}

	/**
	 * Returns geometric length of current path.
	 * @return length of current path.
	 */
	public double getPathLength() throws MapyrusException
	{
		double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = 0.0;
		else
			retval = path.getLength(resolution);
		return(retval);
	}

	/**
	 * Returns geometric area of current path.
	 * @return area of current path.
	 */
	public double getPathArea() throws MapyrusException
	{
		double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = 0.0;
		else
			retval = path.getArea(resolution);
		return(retval);
	}

	/**
	 * Returns geometric centroid of current path.
	 * @return centroid of current path.
	 */
	public Point2D.Double getPathCentroid() throws MapyrusException
	{
		Point2D.Double retval;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path == null)
			retval = new Point2D.Double();
		else
			retval = path.getCentroid(resolution);
		return(retval);
	}

	/**
	 * Returns angle of first line segment in path.
	 * @return angle in radians.
	 */
	public double getPathStartAngle() throws MapyrusException
	{
		double retval = 0;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path != null)
			retval = path.getStartAngle(resolution);
		return(retval);
	}

	/**
	 * Returns angle of last line segment in path.
	 * @return angle in radians.
	 */
	public double getPathEndAngle() throws MapyrusException
	{
		double retval = 0;
		GeometricPath path = getDefinedPath();
		double resolution = getResolution();
		
		if (path != null)
			retval = path.getEndAngle(resolution);
		return(retval);
	}

	/**
	 * Returns first point in path.
	 * @return first point or (0, 0) if path is empty.
	 */
	public Point2D getPathStartPoint() throws MapyrusException
	{
		Point2D retval = null;
		GeometricPath path = getDefinedPath();

		if (path != null)
			retval = path.getStartPoint();
		if (retval == null)
			retval = new Point2D.Double();
		return(retval);
	}

	/**
	 * Returns last point in path.
	 * @return last point or (0, 0) if path is empty.
	 */
	public Point2D getPathEndPoint() throws MapyrusException
	{
		Point2D retval = null;
		GeometricPath path = getDefinedPath();

		if (path != null)
			retval = path.getEndPoint();
		if (retval == null)
			retval = new Point2D.Double();
		return(retval);
	}

	/**
	 * Returns rotation angle for each each moveTo point in current path
	 * @return array of rotation angles relative to rotation in current
	 * transformation matrix.
	 */
	public ArrayList getMoveToRotations()
	{
		ArrayList retval;

		GeometricPath path = getDefinedPath();

		if (path == null)
			retval = null;
		else
			retval = path.getMoveToRotations();
		return(retval);
	}

	/**
	 * Returns coordinate for each each moveTo point in current path.
	 * @return array of Point2D.Float objects relative to current transformation matrix.
	 */
	public ArrayList<Point2D> getMoveTos() throws MapyrusException
	{
		ArrayList<Point2D> retval = null;
		GeometricPath path = getDefinedPath();
		AffineTransform inverse;
		ArrayList moveTos;

		try
		{
			if (path != null)
			{
				/*
				 * If there is no transformation matrix then we can return original
				 * coordinates, otherwise we must convert all coordinates to be relative
				 * to the current transformation matrix and build a new list.
				 */
				if (m_ctm.isIdentity())
				{
					retval = path.getMoveTos();
				}
				else
				{
					inverse = m_ctm.createInverse();
					moveTos = path.getMoveTos();
					retval = new ArrayList<Point2D>(moveTos.size());

					for (int i = 0; i < moveTos.size(); i++)
					{
						Point2D.Float pt = (Point2D.Float)(moveTos.get(i));
						retval.add(inverse.transform(pt, null));
					}
				}
			}
		}
		catch (NoninvertibleTransformException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return(retval);
	}

	/**
	 * Returns bounding box of this geometry.
	 * @return bounding box, or null if no path is defined.
	 */
	public Rectangle2D getBounds2D()
	{
		Rectangle2D bounds;
		GeometricPath path = getDefinedPath();

		if (path == null)
			bounds = null;
		else
			bounds = path.getBounds2D();

		return(bounds);
	}

	/**
	 * Get current path as a geometry argument.
	 * @return current path.
	 */
	public Argument getPathArgument() throws MapyrusException
	{
		Argument retval = Argument.emptyGeometry;
		GeometricPath path = getDefinedPath();
		if (path != null)
			retval = path.toArgument(getResolution());
		return(retval);
	}

	/**
	 * Returns value of a variable.
	 * @param variable name to lookup.
	 * @return value of variable, or null if it is not defined.
	 */
	public Argument getVariableValue(String varName)
	{
		Argument retval;
		
		/*
		 * Variable is not set if no lookup table is defined.
		 */
		if (m_vars == null)
			retval = null;
		else		
			retval = (Argument)m_vars.get(varName);
			
		return(retval);
	}

	/**
	 * Indicates that a variable is to be stored locally in this context
	 * and not be made available to other contexts.
	 * @param varName name of variable to be treated as local.
	 */
	public void setLocalScope(String varName)
	{
		/*
		 * Record that variable is local.
		 */
		if (m_localVars == null)
			m_localVars = new HashSet<String>();
		m_localVars.add(varName);
	}

	/**
	 * Returns true if variable has been defined local in this context
	 * with @see setLocalScope().
	 * @param varName is name of variable to check.
	 * @return true if variable defined local.
	 */
	public boolean hasLocalScope(String varName)
	{
		return(m_localVars != null && m_localVars.contains(varName));
	}

	/**
	 * Define variable in current context, replacing any existing
	 * variable with the same name.
	 * @param varName name of variable to define.
	 * @param value is value for this variable
	 */
	public void defineVariable(String varName, Argument value)
	{
		/*
		 * Create new variable.
		 */
		if (m_vars == null)
			m_vars = new HashMap<String, Argument>();

		/*
		 * Clone hashmap variables to avoid changes to entries
		 * in one variable being visible to others.
		 */
		if (value.getType() == Argument.HASHMAP)
			value = (Argument)value.clone();
		m_vars.put(varName, value);
	}

	/**
	 * Define an key-value entry in a hashmap in current context,
	 * replacing any existing entry with the same key.
	 * @param hashMapName name of hashmap to add entry to.
	 * @param key is key to add.
	 * @param value is value to add.
	 */
	public void defineHashMapEntry(String hashMapName, String key, Argument value)
	{
		if (m_vars == null)
			m_vars = new HashMap<String, Argument>();

		/*
		 * Create new entry in a hash map.
		 */
		Argument arg = (Argument)m_vars.get(hashMapName);
		if (arg == null || arg.getType() != Argument.HASHMAP)
		{
			/*
			 * No hash map with this name used before,
			 * create new one.
			 */
			arg = new Argument();
			m_vars.put(hashMapName, arg);
			
		}
		arg.addHashMapEntry(key, value);
	}

	/**
	 * Returns dimensions of a string, drawn to current page with current font.
	 * @param s string to calculate height and width for.
	 * @param scaleToWorlds scale dimension to world coordinates, if true.
	 * @return height and width of string in millimetres.
	 */	
	public StringDimension getStringDimension(String s, boolean scaleToWorlds)
		throws IOException, MapyrusException
	{
		StringDimension retval;

		if (m_outputFormat != null)
		{
			/*
			 * Make sure current font is set *and* pass current font
			 * to stringWidth calculation as some output formats set
			 * the font and then forget it.
			 */
			setGraphicsAttributes(ATTRIBUTE_FONT);
			retval = m_outputFormat.getStringDimension(s, m_fontName, m_fontSize, m_fontLineSpacing);
			double w = retval.getWidth();
			double h = retval.getHeight();

			if (m_pageWorldExtents != null && scaleToWorlds)
			{
				w = w / m_outputFormat.getPageWidth() * m_pageWorldExtents.getWidth();
				h = h / m_outputFormat.getPageHeight() * m_pageWorldExtents.getHeight();
			}
			w = w / m_scaling;
			h = h / m_scaling;
			retval.setSize(w, h);
		}
		else
		{
			/*
			 * Not possible to accurately calculate width if no page defined.
			 */
			retval = new StringDimension();
		}
		return(retval);
	}	
}
