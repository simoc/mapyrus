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

import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import au.id.chenery.mapyrus.*;

/**
 * Interface to a vector geographic format.  Provides methods to open and query
 * data from a vector geographic format.
 */
public interface GeographicDataset
{
	/**
	 * Return projection of this dataset.  Methods of specifying a projection differ
	 * wildly between formats so caller will have to try and make sense of it.
	 * @return projection
	 */
	public String getProjection();
	
	/**
	 * Return metadata of a dataset in a key-value lookup table.
	 * @return metadata table.
	 */
	public Hashtable getMetadata();
	
	/**
	 * Return list of names of fields in this dataset.
	 * @return array of names.
	 */
	public String []getFieldNames();
	
	/**
	 * Return indexes of geometry fields in list of field names.
	 * @return list of field indexes.
	 */
	public int[] getGeometryFieldIndexes();

	/**
	 * Return world coordinate extents of this dataset in projection of dataset.
	 * @return extents.
	 */
	public Rectangle2D.Double getWorlds();
	
	/**
	 * Query the dataset.  Finds all data inside or crossing the area of interest
	 * and matching the where clause.  Makes results available to be fetched.
	 * Resolution value is a hint to allow an appropriate level of detail to be
	 * returned in fetched geometry data.  
	 * Only one query can be made on a dataset at one time, beginning a
	 * new query closes the last one.
	 * 
	 * @param extents is area of interest for this query.
	 * @param resolution is minimum distance between coordinate values.
	 */
	public void query(Rectangle2D.Double extents, double resolution)
		throws MapyrusException;
		
	/**
	 * Fetch next row that is a result of current query.
	 * @return row returns next row result, or null if there is no row available.
	 */
	public Row fetch() throws MapyrusException;
}
