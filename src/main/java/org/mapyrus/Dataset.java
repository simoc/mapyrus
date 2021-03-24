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

package org.mapyrus;

import java.awt.geom.Rectangle2D;

import org.mapyrus.dataset.GeographicDataset;

/**
 * Manages a dataset being read.  Methods are provided to simplify
 * fetching and checking for reaching last row.
 */
public class Dataset
{
	private GeographicDataset m_dataset;
	private Row m_datasetRow;
	private int m_datasetRowCount;

	/**
	 * Setup a new dataset for reading.
	 * @param dataset is dataset to be read.
	 */
	public Dataset(GeographicDataset dataset) throws MapyrusException
	{
		m_dataset = dataset;
		m_datasetRow = dataset.fetch();
		m_datasetRowCount = 0;
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getProjection().
	 */	
	public String getProjection()
	{
		return(m_dataset.getProjection());
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getWorlds().
	 */
	public Rectangle2D.Double getWorlds()
	{
		return(m_dataset.getWorlds());
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getFieldNames().
	 */
	public String []getFieldNames()
	{
		return(m_dataset.getFieldNames());
	}

	/**
	 * Indicates whether another row can be fetched for query with fetchRow().
	 * @return true if another row is available.
	 */	
	public boolean hasMoreRows()
	{
		return(m_datasetRow != null);
	}

	/**
	 * Fetches next row from query.
	 * @return next row.
	 */
	public Row fetchRow() throws MapyrusException
	{
		if (m_datasetRow == null)
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_ROWS));

		/*
		 * Return row we've already fetched, then fetch another.
		 */		
		Row retval = m_datasetRow;
		m_datasetRow = m_dataset.fetch();
		m_datasetRowCount++;

		return(retval);
	}

	/**
	 * Returns the number of rows already fetched from dataset.
	 * @return row count.
	 */
	public int getFetchCount()
	{
		return(m_datasetRowCount);
	}
	
	/**
	 * Closes dataset.
	 */
	public void close() throws MapyrusException
	{
		try
		{
			if (m_dataset != null)
				m_dataset.close();
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
			m_dataset = null;
			m_datasetRow = null;
			m_datasetRowCount = 0;
		}
	}
}
