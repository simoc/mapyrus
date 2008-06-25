/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2008 Simon Chenery.
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
package org.mapyrus.dataset;

import java.awt.geom.Point2D;
import java.sql.SQLException;
import oracle.sql.STRUCT;
import oracle.spatial.geometry.JGeometry;
import org.mapyrus.Argument;

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

	public static Argument parseGeometry(Object o) throws SQLException
	{
		STRUCT st = (oracle.sql.STRUCT)o;
		JGeometry geom = JGeometry.load(st);
		double []ordinates;
		Argument retval = Argument.emptyGeometry;

		if (geom.isPoint())
		{
			ordinates = geom.getPoint();
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
		return(retval);
	}
}
