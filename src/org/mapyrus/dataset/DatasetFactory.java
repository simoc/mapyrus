/*
 * $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.io.IOException;

import au.id.chenery.mapyrus.MapyrusException;
import au.id.chenery.mapyrus.MapyrusMessages;

/**
 * Factory class returning new dataset objects.  Provides single interface to
 * opening of all different dataset types.
 */
public class DatasetFactory
{
	/**
	 * Opens a dataset to read geometry from.
	 * @param type is format of dataset, for example, "text".
	 * @param name is name of dataset to open.
	 * @param extras are special options for this dataset type such as database connection
	 * information, or instructions for interpreting data.
	 * @param geometryFieldNames is list of names of fields containing geometry.
	 */
	public static GeographicDataset open(String type, String name,
		String extras, String []geometryFieldNames) throws MapyrusException
	{
		GeographicDataset retval = null;
		String errorMessage = null;

		/*
		 * Branch to open dataset, depending on type.
		 * Catch all kinds of dataset openning exceptions here and return
		 * them all as MapyrusExceptions to avoid exposing higher level code
		 * to lots of exception types.
		 */		
		try
		{
			// TODO dynamically load class for datasets for easier extensionsibility.
			if (type.equalsIgnoreCase("textfile"))
				retval = new TextfileDataset(name, extras, geometryFieldNames);
			else if (type.equalsIgnoreCase("shapefile"))
				retval = new ShapefileDataset(name, extras, geometryFieldNames);
			else
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_DATASET_TYPE) +
					": " + type);
			}
		}
		catch (IOException e)
		{
			errorMessage = e.getMessage();
		}

		if (retval == null)				
		{
			if (errorMessage == null)
				errorMessage = "";

			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.OPEN_DATASET_ERROR) +
				": " + name + System.getProperty("line.separator") + errorMessage);
		}
		return(retval);
	}
}
