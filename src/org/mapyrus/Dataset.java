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

import java.awt.geom.Rectangle2D;

import au.id.chenery.mapyrus.dataset.GeographicDataset;

/**
 * Manages a dataset being queried.  Methods are provided to simplify
 * fetching and checking for reaching last row.
 */
public class Dataset
{
	private GeographicDataset mDataset;
	private Row mDatasetRow;
	private int mDatasetRowCount;

	/**
	 * Setup a new dataset for query.
	 * @param dataset is dataset to be queried.
	 */
	public Dataset(GeographicDataset dataset)
	{
		mDataset = dataset;
		mDatasetRow = null;
		mDatasetRowCount = 0;
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getProjection().
	 */	
	public String getProjection()
	{
		return(mDataset.getProjection());
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getWorlds().
	 */
	public Rectangle2D.Double getWorlds()
	{
		return(mDataset.getWorlds());
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getFieldNames().
	 */
	public String []getFieldNames()
	{
		return(mDataset.getFieldNames());
	}

	/**
	 * Begins new query on dataset, closing any previous query.
	 * @param extents is area of interest for this query.
	 * @param resolution is minimum distance between coordinate values.
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#query(Rectangle2D.Double, double). 
	 */	
	public void query(Rectangle2D.Double extents, double resolution)
		throws MapyrusException
	{
		mDataset.query(extents, resolution);

		/*
		 * Fetch first row so we know if there are any more rows available.
		 */
		mDatasetRow = mDataset.fetch();
		mDatasetRowCount = 0;
	}

	/**
	 * Indicates whether another row can be fetched for query with fetchRow().
	 * @return true if another row is available.
	 */	
	public boolean hasMoreRows()
	{
		return(mDatasetRow != null);
	}

	/**
	 * Fetches next row from query.
	 * @return next row.
	 */
	public Row fetchRow() throws MapyrusException
	{
		if (mDatasetRow == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_ROWS));

		/*
		 * Return row we've already fetched.
		 */		
		Row retval = mDatasetRow;
		mDatasetRow = mDataset.fetch();
		mDatasetRowCount++;

		return(retval);
	}

	/**
	 * Returns the number of rows already fetched fro query with fetchRow() method.
	 * @return row count.
	 */
	public int getFetchCount()
	{
		return(mDatasetRowCount);
	}
	
	/**
	 * Closes dataset.
	 */
	public void close() throws MapyrusException
	{
		mDataset.close();
		mDataset = null;
		mDatasetRow = null;
		mDatasetRowCount = 0;
	}
}
