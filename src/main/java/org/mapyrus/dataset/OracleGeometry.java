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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.dataset;

import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.util.ArrayList;
import oracle.sql.STRUCT;
import oracle.spatial.geometry.JGeometry;
import org.mapyrus.Argument;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Convert Oracle Spatial geometry values to Mapyrus geometry.
 * See Oracle Spatial User's Guide and Reference,
 * Oracle Spatial Java API Reference.
 */
public class OracleGeometry
{
	/*
	 * SDO_INTERPRETATION types in SDO_ELEM_INFO array.
	 */
	private static final int INTERPRETATION_LINES = 1;
	private static final int INTERPRETATION_ARCS = 2;
	private static final int INTERPRETATION_RECTANGLE = 3;
	private static final int INTERPRETATION_CIRCLE = 4;

	/**
	 * Parse Oracle JGeometry data type into Mapyrus geometry.
	 * @param o Oracle geometry
	 * @return Mapyrus geometry
	 */
	public static Argument parseGeometry(Object o)
		throws SQLException, MapyrusException
	{
		STRUCT st = (oracle.sql.STRUCT)o;
		JGeometry geom = JGeometry.load(st);
		Argument retval = Argument.emptyGeometry;

		if (geom.isPoint())
		{
			/*
			 * Geometry is a single point.
			 */
			double []ordinates = geom.getPoint();
			if (ordinates != null)
			{
				double []coords = new double[]
				{
					Argument.GEOMETRY_POINT,
					1,
					Argument.MOVETO,
					ordinates[0],
					ordinates[1]
				};
				retval = new Argument(Argument.GEOMETRY_POINT, coords);
			}
		}
		else if (geom.isMultiPoint())
		{
			/*
			 * Geometry is multiple points.
			 */
			Point2D []pts = geom.getJavaPoints();
			if (pts != null)
			{
				double []coords = new double[pts.length * 5 + 2];
				coords[0] = Argument.GEOMETRY_MULTIPOINT;
				coords[1] = pts.length;
				int index = 2;
				for (int i = 0; i < pts.length; i++)
				{
					coords[index++] = Argument.GEOMETRY_POINT;
					coords[index++] = 1;
					coords[index++] = Argument.MOVETO;
					coords[index++] = pts[i].getX();
					coords[index++] = pts[i].getY();
				}
				retval = new Argument(Argument.GEOMETRY_MULTIPOINT, coords);
			}
		}
		else
		{
			/*
			 * Geometry is one or more lines or polygons.
			 */
			int []elemInfo = geom.getElemInfo();
			double []ordinatesArray = geom.getOrdinatesArray();
			retval = createGeometryArgument(elemInfo, ordinatesArray);
		}
		return(retval);
	}

	private static Argument createGeometryArgument(int []elemInfo,
		double []ordinatesArray) throws MapyrusException
	{
		Argument retval = Argument.emptyGeometry;
		ArrayList<double []> coordLists = new ArrayList<double []>();
		ArrayList<Integer> eTypes = new ArrayList<Integer>();
		ArrayList<Argument> geometryList = new ArrayList<Argument>();

		/*
		 * Get (X, Y) coordinate list for each element in geometry.
		 */
		int elemIndex = 0;
		while (elemIndex < elemInfo.length)
		{
			/*
			 * Index of first element in ordinates array is 1. 
			 */
			int ordinatesStartIndex = elemInfo[elemIndex] - 1;
			int ordinatesEndIndex;
			if (elemIndex + 3 < elemInfo.length)
				ordinatesEndIndex = elemInfo[elemIndex + 3] - 1;
			else
				ordinatesEndIndex = ordinatesArray.length;

			int eType = elemInfo[elemIndex + 1];
			eTypes.add(Integer.valueOf(eType));
			eType = (eType % 1000);
			int interpretation = elemInfo[elemIndex + 2];
			double []coords = null;

			if (eType == 3 && interpretation == INTERPRETATION_RECTANGLE)
			{
				/*
				 * Two (X, Y) coordinates defining a rectangle.
				 */
	 			double x1 = ordinatesArray[ordinatesStartIndex];
	 			double y1 = ordinatesArray[ordinatesStartIndex + 1];
	 			double x2 = ordinatesArray[ordinatesStartIndex + 2];
	 			double y2 = ordinatesArray[ordinatesStartIndex + 3];

				coords = new double[5 * 2];
				coords[0] = x1;
				coords[1] = y1;

				coords[2] = x2;
				coords[3] = y1;

				coords[4] = x2;
				coords[5] = y2;

				coords[6] = x1;
				coords[7] = y2;

				coords[8] = x1;
				coords[9] = y1;
			}
			else if (eType == 3 && interpretation == INTERPRETATION_CIRCLE)
			{
				/*
				 * A complete circle defined by three points on circumference.
				 */
	 			double x1 = ordinatesArray[ordinatesStartIndex];
	 			double y1 = ordinatesArray[ordinatesStartIndex + 1];
	 			double x2 = ordinatesArray[ordinatesStartIndex + 2];
	 			double y2 = ordinatesArray[ordinatesStartIndex + 3];
	 			double x3 = ordinatesArray[ordinatesStartIndex + 4];
	 			double y3 = ordinatesArray[ordinatesStartIndex + 5];
	 			
	 			/*
	 			 * Expand circle into a series of straight line segments.
	 			 */
	 			double []arcPts = JGeometry.computeArc(x1, y1, x2, y2, x3, y3);
	 			coords = new double[(360 + 1) * 2];
	 			for (int i = 0; i <= 360; i++)
	 			{
	 				double radians = Math.toRadians(i);
	 				coords[i * 2] = arcPts[0] + Math.cos(radians) * arcPts[2];
	 				coords[i * 2 + 1] = arcPts[1] + Math.sin(radians) * arcPts[2];
	 			}
			}
			else if ((eType == 2 || eType == 3) &&
				interpretation == INTERPRETATION_LINES)
			{
				/*
				 * A single line or polygon.
				 */
				coords = new double[ordinatesEndIndex - ordinatesStartIndex];
				for (int i = 0; i < coords.length; i++)
				{
					coords[i] = ordinatesArray[ordinatesStartIndex + i];
				}
			}
			else
			{
				/*
				 * We cannot read all Oracle geometry types.
				 * Include the SDO_ELEM_INFO numbers in error message so I know
				 * which geometry type is missing.
				 */
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNSUPPORTED_ORACLE) + ": " +
					elemInfo[elemIndex] + " " +
					elemInfo[elemIndex + 1] + " " +
					elemInfo[elemIndex + 2]);
			}
			coordLists.add(coords);
			elemIndex += 3;
		}

		/*
		 * Now build Mapyrus geometry from coordinates.
		 */
		int coordIndex = 0;
		while (coordIndex < coordLists.size())
		{
			Integer eType = eTypes.get(coordIndex);
			double []coords = coordLists.get(coordIndex);
			int nCoords = coords.length / 2;
			if (eType.intValue() == 2)
			{
				/*
				 * A simple line.
				 */
				double []argCoords = new double[2 + nCoords * 3];
	 			argCoords[0] = Argument.GEOMETRY_LINESTRING;
				argCoords[1] = nCoords;
				int argCoordsIndex = 2;
				for (int i = 0; i < nCoords; i++)
				{
					if (i == 0)
						argCoords[argCoordsIndex] = Argument.MOVETO;
					else
						argCoords[argCoordsIndex] = Argument.LINETO;
					argCoords[argCoordsIndex + 1] = coords[i * 2];
					argCoords[argCoordsIndex + 2] = coords[i * 2 + 1];
					argCoordsIndex += 3;
				}
				geometryList.add(new Argument(Argument.GEOMETRY_LINESTRING, argCoords));
				coordIndex++;
			}
			else if (eType.intValue() == 1003)
			{
				/*
				 * Next element is a polygon exterior.
				 */
				int interiorIndex = coordIndex + 1;
				while (interiorIndex < coordLists.size() &&
					eTypes.get(interiorIndex).intValue() == 2003)
				{
					/*
					 * Count any polygon interiors (islands) too.
					 */
					double []interiorCoords = coordLists.get(interiorIndex);
					nCoords += (interiorCoords.length / 2);
					interiorIndex++;
				}
				double []argCoords = new double[2 + nCoords * 3];
	 			argCoords[0] = Argument.GEOMETRY_POLYGON;
				argCoords[1] = nCoords;
				int argCoordsIndex = addPolygonCoords(argCoords, 2, coords);

				coordIndex++;
				while (coordIndex < coordLists.size() &&
					eTypes.get(coordIndex).intValue() == 2003)
				{
					/*
					 * Add any polygon interiors (islands) too.
					 */
					double []interiorCoords = coordLists.get(coordIndex);
					argCoordsIndex = addPolygonCoords(argCoords,
						argCoordsIndex, interiorCoords);
					coordIndex++;
				}
				geometryList.add(new Argument(Argument.GEOMETRY_POLYGON, argCoords));
			}
		}

		/*
		 * Return a multiple geometry type if there is more than one geometry.
		 */
		if (geometryList.size() > 1)
		{
			int nValues = 0;
			boolean isAllLineStrings = true;
			boolean isAllPolygons = true;
			for (int i = 0; i < geometryList.size(); i++)
			{
				Argument arg = geometryList.get(i);
				nValues += arg.getGeometryValue().length;
				double geomType = arg.getGeometryValue()[0];
				if (geomType != Argument.GEOMETRY_LINESTRING)
					isAllLineStrings = false;
				if (geomType != Argument.GEOMETRY_POLYGON)
					isAllPolygons = false;
			}

			/*
			 * Combine all geometries together into a multiple geometry or collection.
			 */
			double []argCoords = new double[2 + nValues];
			if (isAllLineStrings)
				argCoords[0] = Argument.GEOMETRY_MULTILINESTRING;
			else if (isAllPolygons)
				argCoords[0] = Argument.GEOMETRY_MULTIPOLYGON;
			else
				argCoords[0] = Argument.GEOMETRY_COLLECTION;
			argCoords[1] = geometryList.size();
			int argCoordsIndex = 2;
			for (int i = 0; i < geometryList.size(); i++)
			{
				/*
				 * Copy all geometries into geometry collection.
				 */
				Argument arg = geometryList.get(i);
				double []coords = arg.getGeometryValue();
				for (int j = 0; j < coords.length; j++)
				{
					argCoords[argCoordsIndex++] = coords[j];
				}
			}
			retval = new Argument(Argument.GEOMETRY_COLLECTION, argCoords);
		}
		else
		{
			/*
			 * Return the single geometry that we have already built.
			 */
			retval = geometryList.get(0);
		}
		return(retval);
	}

	/**
	 * Copy coordinates from (X, Y) list into Mapyrus geometry array.
	 * @param polygonCoords array to copy coordinates into.
	 * @param polygonCoordsIndex index to start copying into in polygonCoords.
	 * @param coords (X, Y) coordinates to be copied.
	 * @return next free index in polygonCoords after copy.
	 */
	private static int addPolygonCoords(double []polygonCoords,
		int polygonCoordsIndex, double []coords)
	{
		for (int i = 0; i < coords.length; i += 2)
		{
			if (i == 0)
				polygonCoords[polygonCoordsIndex] = Argument.MOVETO;
			else
				polygonCoords[polygonCoordsIndex] = Argument.LINETO;
			polygonCoordsIndex++;
			polygonCoords[polygonCoordsIndex++] = coords[i];
			polygonCoords[polygonCoordsIndex++] = coords[i + 1];
		}
		return(polygonCoordsIndex);
	}
}
