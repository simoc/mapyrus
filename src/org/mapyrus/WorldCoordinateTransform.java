/**
 * Interfaces to an external shared library to which we pipe coordinate values and then 
 * read back transformed values.  Most useful in combination with PROJ.4 coordinate
 * transform program for reprojecting coordinates.
 *
 * Uses JNI interface to call native methods to define transformation and then
 * to transform points between coordinate systems.
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.*;

public class WorldCoordinateTransform
{
	/*
	 * Native methods implementing defintion and transformation between coordinate systems.
	 */
	static native int defineCS(String description);
	static native int transform(int t1, int t2, double []coords, int nCoords);

	static private Hashtable mDefinedCoordinateSystems;
	
	static
	{
		System.loadLibrary("mapyrusproj4");
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
			newProjection = defineCS(description);
			if (newProjection < 0)
			{
				throw new MapyrusException("Failed to define coordinate system " +
					description);
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
		if (transform(p1, p2, coords, coords.length / 2) < 0)
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
			throw new MapyrusException("Failed to transform coordinates from '" +
				sourceName + "' to '" + destName + "'");
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
		if (mSourceSystem < 0)
		{
			throw new MapyrusException("Failed to define coordinate system " +
				system1);
		}
		mDestinationSystem = defineCoordinateSystem(system2);
		if (mDestinationSystem < 0)
		{
			throw new MapyrusException("Failed to define coordinate system " +
				system2);
		}
	}

	/**
	 * Transform coordinates from first coordinate system to the second.
	 * @param coords is array of X, Y coordinate values to be transformed.
	 */
	public void forwardTransform(double []coords) throws MapyrusException
	{
		transform(mSourceSystem, mDestinationSystem, coords);
	}

	/**
	 * Transform coordinates in reverse, from second coordinate system to the first.
	 * @param coords is array of X, Y coordinate values to be transformed.
	 */
	public void backwardTransform(double []coords) throws MapyrusException
	{
		transform(mSourceSystem, mDestinationSystem, coords);
	}
}
