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
package org.mapyrus.image;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.io.GeoImageBoundingBox;

/**
 * Reads image formats that Java does not support directly,
 * (for example GRASS raster format), using Java Reflection to access
 * Java classes outside of Mapyrus.
 */
public class MapyrusExternalImage implements GeoImageBoundingBox
{
	private BufferedImage m_image;
	private Rectangle2D m_bounds;
	private HashMap<String, Color> m_legendKeys;

	public MapyrusExternalImage(String className, String filename, String extras) throws MapyrusException
	{
		String methodName = "";
		try
		{
			/*
			 * Call constructor to create object to use to read image.
			 */
			Class clazz = Class.forName(className);
			methodName = "<init>";
			Constructor constructor = clazz.getConstructor(String.class, String.class);
			Object imageReader = constructor.newInstance(filename, extras);

			/*
			 * Find and call methods to read image.
			 */
			methodName = "read";
			Method readMethod = clazz.getMethod(methodName);
			m_image = (BufferedImage)readMethod.invoke(imageReader);

			methodName = "getBounds";
			Method getBoundsMethod = clazz.getMethod(methodName);
			m_bounds = (Rectangle2D)getBoundsMethod.invoke(imageReader);

			try
			{
				methodName = "getLegendKeys";
				Method getLegendKeysMethod = clazz.getMethod(methodName);
				m_legendKeys = (HashMap)getLegendKeysMethod.invoke(imageReader);
			}
			catch (NoSuchMethodException e)
			{
				/*
				 * Legend is optional so just continue if no method to get legend.
				 */
			}
		}
		catch (NoSuchMethodException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + "." + methodName + ": " + e.getClass().getName() + ": " + e.getMessage());
		}
		catch (ClassCastException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + "." + methodName + ": " + e.getClass().getName() + ": " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + ": " + e.getClass().getName() + ": " + e.getMessage());
		}
		catch (InstantiationException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + "." + methodName + ": " + e.getClass().getName() + ": " + e.getMessage());
		}
		catch (InvocationTargetException e)
		{
			String targetMessage = "";
			if (e.getTargetException() != null)
				targetMessage = e.getTargetException().getMessage();
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.BAD_IMAGE_READER_CLASS) +
				": " + className + "." + methodName + ": " + e.getClass().getName() + ": " + targetMessage);
		}
	}

	/**
	 * Get image for display.
	 * @return displayable image.
	 */
	public BufferedImage read()
	{
		return(m_image);
	}

	/**
	 * Get bounding rectangle for image.
	 * @return bounding rectangle.
	 */
	public Rectangle2D getBounds()
	{
		return(m_bounds);
	}

	/**
	 * Get legend keys for image, a description of each color used in the image.
	 * @return table of legend keys for this image.
	 */
	public HashMap<String, Color> getLegendKeys()
	{
		return(m_legendKeys);
	}
}