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
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.Rectangle2D.Double;
import java.util.Hashtable;

import au.id.chenery.mapyrus.MapyrusException;
import au.id.chenery.mapyrus.Row;

/**
 * Implements reading of geographic datasets from an RDBMS via JDBC interface.
 */
public class JDBCDataset implements GeographicDataset
{
	/**
	 * Create 
	 * @param filename is SQL select statement to read data from.
	 * @param extras
	 * @param geometryFieldNames
	 * @throws MapyrusException
	 */
	public JDBCDataset(String filename, String extras, String []geometryFieldNames)
		throws MapyrusException
	{
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getProjection()
	 */
	public String getProjection() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getFieldNames()
	 */
	public String[] getFieldNames() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getGeometryFieldIndexes()
	 */
	public int[] getGeometryFieldIndexes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getWorlds()
	 */
	public Double getWorlds() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#query(java.awt.geom.Rectangle2D.Double, double)
	 */
	public void query(Double extents, double resolution)
		throws MapyrusException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#fetch()
	 */
	public Row fetch() throws MapyrusException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#close()
	 */
	public void close()
	{
		// TODO Auto-generated method stub
	}
}
