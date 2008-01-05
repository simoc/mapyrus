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
package org.mapyrus;

import java.awt.geom.Rectangle2D;

import org.mapyrus.dataset.GeographicDataset;

/**
 * Manages a dataset being read.  Methods are provided to simplify
 * fetching and checking for reaching last row.
 */
public class Dataset
{
	private GeographicDataset mDataset;
	private Row mDatasetRow;
	private int mDatasetRowCount;

	/**
	 * Setup a new dataset for reading.
	 * @param dataset is dataset to be read.
	 */
	public Dataset(GeographicDataset dataset) throws MapyrusException
	{
		mDataset = dataset;
		mDatasetRow = dataset.fetch();
		mDatasetRowCount = 0;
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getProjection().
	 */	
	public String getProjection()
	{
		return(mDataset.getProjection());
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getWorlds().
	 */
	public Rectangle2D.Double getWorlds()
	{
		return(mDataset.getWorlds());
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getFieldNames().
	 */
	public String []getFieldNames()
	{
		return(mDataset.getFieldNames());
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
		 * Return row we've already fetched, then fetch another.
		 */		
		Row retval = mDatasetRow;
		mDatasetRow = mDataset.fetch();
		mDatasetRowCount++;

		return(retval);
	}

	/**
	 * Returns the number of rows already fetched from dataset.
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
		try
		{
			if (mDataset != null)
				mDataset.close();
		}
		catch (MapyrusException e)
		{
			throw e;
		}
		finally
		{
			/*
			 * Always clear dataset after attempting to close it
			 * so that we will never try to close it again.
			 */
			mDataset = null;
			mDatasetRow = null;
			mDatasetRowCount = 0;
		}
	}
}
