/*
 * $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.util.Hashtable;

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
	 * Return list of types of fields in this dataset.
	 * @return list of field types.
	 */
	public int []getFieldTypes();

	/**
	 * Return indexes of geometry fields in list of field names.
	 * @return list of field indexes.
	 */
	public int[] getGeometryFieldIndexes();

	/**
	 * Return world coordinate extents of this dataset in projection of dataset.
	 * @return extents.
	 */
	public Rectangle2D getWorlds();
	
	/**
	 * Query the dataset.  Finds all data inside or crossing the area of interest
	 * and matching the where clause.  Makes results available to be fetched.
	 * Only one query can be made on a dataset at one time, beginning a
	 * new query closes the last one.
	 * 
	 * @param extents is area of interest for this query.
	 * @param whereClause is additional SQL where clause for query.
	 */
	public void query(Rectangle2D.Double extents, String whereClause)
		throws MapyrusException;
		
	/**
	 * Finds if current query has more data available. 
	 * @return true if query has more rows available to be fetched.
	 */
	public boolean hasMoreRows() throws MapyrusException;
	
	/**
	 * Fetch next row that is a result of current query.
	 * @param query handle to query.
	 * @param row returns next row result.
	 */
	public Row fetch() throws MapyrusException;
}
