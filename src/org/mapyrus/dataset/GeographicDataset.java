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
package org.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import org.mapyrus.*;

/**
 * Interface to a vector geographic format.  Provides methods to open and read
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
	 * Return world coordinate extents of this dataset in projection of dataset.
	 * @return extents.
	 */
	public Rectangle2D.Double getWorlds();

	/**
	 * Fetch next row of data from dataset.
	 * @return row returns next row of data, or null if there is no row available.
	 */
	public Row fetch() throws MapyrusException;
	
	/**
	 * Close dataset, closing any files, database connections and other resources.
	 */
	public void close() throws MapyrusException;
}
