/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Interfaces to an external shared library to which we pipe coordinate values
 * and then read back transformed values.  Most useful in combination with
 * PROJ coordinate transform program for reprojecting coordinates.
 *
 * Uses JNI interface to call native methods to define transformation and then
 * to transform points between coordinate systems.
 */
public class WorldCoordinateTransform
{
	/*
	 * Native methods implementing defintion and transformation
	 * between coordinate systems and retrieving error message when something
	 * goes wrong.
	 */
	private static native int define(String description);
	private static native int transform(int t1, int t2, double []coords, int nCoords);

	static private Hashtable mDefinedCoordinateSystems;

	static
	{
		System.loadLibrary("mapyrusproj");
		mDefinedCoordinateSystems = new Hashtable();
	}

	/**
	 * Define a coordinate system.  Synchronized here because external
	 * library may not be thread-safe.
	 */
	static private synchronized int defineCoordinateSystem(String description)
		throws MapyrusException
	{
		Integer definition;
		int newProjection;
		
		definition = (Integer)mDefinedCoordinateSystems.get(description);
		if (definition == null)
		{
			newProjection = define(description);
			if (newProjection < 0)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.DEFINE_TRANSFORM) +
					": " + description);
			}
			definition = new Integer(newProjection);
			mDefinedCoordinateSystems.put(description, definition);
		}
		return(definition.intValue());
	}

	/**
 	 * Convert X, Y coordinates from one coordinate system to another.
 	 * Synchronized here because external library may not be thread-safe.
 	 * @param p1 is source coordinate system.
 	 * @param p2 is destination coordinate system.
 	 * @param coords is array of X, Y coordinates to transform.
 	 */
	private static synchronized void transform(int p1, int p2,
		double []coords) throws MapyrusException
	{
		int nTransformed = transform(p1, p2, coords, coords.length / 2);

		if (nTransformed * 2 != coords.length)
		{
			Integer id;
			String sourceName = null, destName = null;

			/*
			 * Find the names of the two coordinate systems that were used.
			 */
			Enumeration e = mDefinedCoordinateSystems.keys();
			while (e.hasMoreElements())
			{
				String s = (String)e.nextElement();
				id = (Integer)mDefinedCoordinateSystems.get(s);
				if (id.intValue() == p1)
					sourceName = s;
				if (id.intValue() == p2)
					destName = s;
			}
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TRANSFORM_ERROR) +
				": " + sourceName + " " + destName + ": " +
				coords[nTransformed * 2] + " " + coords[nTransformed * 2 + 1]);
		}
	}

	/*
	 * Ids of the two coordinate systems in this transformation.
	 */	
	private int mSourceSystem;
	private int mDestinationSystem;

	/**
	 * Define a transformation between two coordinate systems.
	 */
	public WorldCoordinateTransform(String system1, String system2)
		throws MapyrusException
	{
		mSourceSystem = defineCoordinateSystem(system1);
		mDestinationSystem = defineCoordinateSystem(system2);
	}

	/**
	 * Transform coordinates from first coordinate system to the second.
	 * @param coords is array of X, Y coordinate values to be transformed.
	 */
	public void forwardTransform(double []coords) throws MapyrusException
	{
		if (mSourceSystem != mDestinationSystem)
			transform(mSourceSystem, mDestinationSystem, coords);
	}

	/**
	 * Transform coordinates in reverse, from second coordinate system to the first.
	 * @param coords is array of X, Y coordinate values to be transformed.
	 */
	public void backwardTransform(double []coords) throws MapyrusException
	{
		if (mSourceSystem != mDestinationSystem)
			transform(mDestinationSystem, mSourceSystem, coords);
	}
}
