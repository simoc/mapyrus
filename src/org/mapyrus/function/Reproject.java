/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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

package org.mapyrus.function;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;
import com.jhlabs.map.proj.ProjectionException;

/**
 * Reprojects a point from one projection to another.
 * reproject("epsg:4326", "epsg:31464", 11, 48) -> POINT(4425384, 5318396).
 */
public class Reproject implements Function
{
	private Hashtable<String, Projection> m_projectionCache;

	public Reproject()
	{
		m_projectionCache = new Hashtable<String, Projection>();

		/*
		 * Force a projection to be loaded so we get a class
		 * not found exception at startup, not halfway through
		 * running some commands.
		 */
		try
		{
			String []parameters = new String[]{"+proj=latlong"};
			ProjectionFactory.fromPROJ4Specification(parameters);
		}
		catch (ProjectionException e)
		{
		}
	}

	/**
	 * @see org.mapyrus.function.Function#evaluate(org.mapyrus.ContextStack, ArrayList)
	 */
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument retval = null;
		String srcProjName = args.get(0).getStringValue();
		String destProjName = args.get(1).getStringValue();

		Projection srcProj = getProjection(srcProjName);
		Projection destProj = getProjection(destProjName);

		if (args.size() == 4)
		{
			/*
			 * Reproject single point.
			 */
			double x = args.get(2).getNumericValue();
			double y = args.get(3).getNumericValue();
			Point2D.Double pt = new Point2D.Double(x, y);

			reproject(srcProj, destProj, pt);

			int nCoords = (Double.isNaN(pt.x) || Double.isNaN(pt.y)) ? 0 : 1;
			double []coords = new double[5];
			coords[0] = Argument.GEOMETRY_POINT;
			coords[1] = nCoords;
			coords[2] = Argument.MOVETO;
			coords[3] = pt.x;
			coords[4] = pt.y;

			retval = new Argument(Argument.GEOMETRY_POINT, coords);
		}
		else
		{
			/*
			 * Reproject geometry argument, then reate new geometry
			 * argument with reprojected coordinates. 
			 */
			Argument geometry = args.get(2);
			double []coords = geometry.getGeometryValue();
			double []newCoords = new double[coords.length];
			System.arraycopy(coords, 0, newCoords, 0, coords.length);
			reproject(srcProj, destProj, newCoords, 0);
			retval = new Argument((int)coords[0], newCoords);
		}
		return(retval);
	}

	/**
	 * Reproject geometry.
	 * @param srcProj source projection.
	 * @param destProj destination projection.
	 * @param coords coordinates of geometry to reproject.
	 * @param coordIndex index to start in coords array.
	 * @return index of next 
	 */
	private int reproject(Projection srcProj, Projection destProj,
		double []coords, int coordIndex) throws MapyrusException
	{
		int geometryType = (int)coords[coordIndex++];
		int nCoords, nCoordsIndex;
		Point2D.Double pt = new Point2D.Double();

		switch (geometryType)
		{
			case Argument.GEOMETRY_POINT:
			case Argument.GEOMETRY_LINESTRING:
			case Argument.GEOMETRY_POLYGON:
				nCoordsIndex = coordIndex++;
				nCoords = (int)coords[nCoordsIndex];
				for (int i = 0; i < nCoords; i++)
				{
					pt.x = coords[coordIndex + 1];
					pt.y = coords[coordIndex + 2];
					reproject(srcProj, destProj, pt);
					if (Double.isNaN(pt.x) || Double.isNaN(pt.y))
					{
						/*
						 * This pair of coordinates cannot be reprojected.
						 * Remove the pair of coordinates from the list,
						 * shuffling later coordinates left to fill their place.
						 */
						coords[nCoordsIndex]--;

						int op = (int)coords[coordIndex];
						System.arraycopy(coords, coordIndex + 3, coords, coordIndex, coords.length - coordIndex - 3);

						/*
						 * Ensure we do not lose any MOVE at the start of the geometry.
						 */
						if (op == Argument.MOVETO && coords[nCoordsIndex] > 0)
							coords[coordIndex] = op;
					}
					else
					{
						coords[coordIndex + 1] = pt.x;
						coords[coordIndex + 2] = pt.y;
						coordIndex += 3;
					}
				}
				break;
			case Argument.GEOMETRY_MULTIPOINT:
			case Argument.GEOMETRY_MULTILINESTRING:
			case Argument.GEOMETRY_MULTIPOLYGON:
			case Argument.GEOMETRY_COLLECTION:
				int nGeometries = (int)coords[coordIndex++];
				for (int i = 0; i < nGeometries; i++)
				{
					/*
					 * Recursively reproject each geometry.
					 */
					coordIndex = reproject(srcProj, destProj, coords, coordIndex);
				}
				break;
		}
		return(coordIndex);
	}

	/**
	 * Reproject a single point.
	 * @srcProj source projection.
	 * @destProj destination projection.
	 * @pt point to reproject.
	 */
	private void reproject(Projection srcProj, Projection destProj,
		Point2D.Double pt) throws MapyrusException
	{
		if (!srcProj.isRectilinear())
		{
			/*
			 * Reproject point back to latitude/longitude.
			 */
			srcProj.inverseTransform(pt, pt);
		}

		//TODO add datum shift here when available in PROJ.4 Java library.

		if (!destProj.isRectilinear())
		{
			/*
			 * Reproject point to target projection.
			 */
			destProj.transform(pt, pt);
		}
	}

	private Projection getProjection(String projectionName) throws MapyrusException
	{
		Projection retval = m_projectionCache.get(projectionName);

		if (retval == null)
		{
			/*
			 * Is projection a name or list of
			 * projection parameters?
			 */
			String []parameters = projectionName.trim().split("\\s+");
			try
			{
				if (parameters.length > 0 && parameters[0].startsWith("+"))
					retval = ProjectionFactory.fromPROJ4Specification(parameters);
				else
					retval = ProjectionFactory.getNamedPROJ4CoordinateSystem(projectionName);
			}
			catch (ProjectionException e)
			{
				throw new MapyrusException(e.getMessage() + ": " + projectionName);
			}
			if (retval != null)
				m_projectionCache.put(projectionName, retval);
		}
		if (retval == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNKNOWN_PROJECTION) + ": " + projectionName);
		return(retval);
	}

	/**
	 * @see org.mapyrus.function.Function#getMaxArgumentCount()
	 */
	public int getMaxArgumentCount()
	{
		return(4);
	}

	/**
	 * @see org.mapyrus.function.Function#getMinArgumentCount()
	 */
	public int getMinArgumentCount()
	{
		return(3);
	}

	/**
	 * @see org.mapyrus.function.Function#getName()
	 */
	public String getName()
	{
		return("reproject");
	}
}
