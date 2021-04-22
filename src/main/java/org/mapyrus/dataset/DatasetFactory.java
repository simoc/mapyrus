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

import java.io.IOException;
import java.io.InputStream;

import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Throttle;

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
	 * @param extras are special options for this dataset type such as
	 * database connection information, or instructions for interpreting data.
	 * @param stdin standard input stream of interpreter.
	 * @param throttle limits CPU usage whilst reading this dataset.
	 * @return open dataset.
	 * @throws MapyrusException if type is not supported or opening dataset failed.
	 */
	public static GeographicDataset open(String type, String name,
		String extras, InputStream stdin, Throttle throttle) throws MapyrusException
	{
		GeographicDataset retval = null;
		String errorMessage = null;

		/*
		 * Branch to open dataset, depending on type.
		 * Catch all kinds of dataset opening exceptions here and return
		 * them all as MapyrusExceptions to avoid exposing higher level code
		 * to lots of exception types.
		 */		
		try
		{
			if (!(type.equalsIgnoreCase("internal") || type.equalsIgnoreCase("jdbc")))
			{
				/*
				 * Check if file access is allowed.
				 */
				if (!throttle.isIOAllowed())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) +
						": " + name);
				}
			}

			if (type.equalsIgnoreCase("textfile"))
				retval = new TextfileDataset(name, extras, stdin);
			else if (type.equalsIgnoreCase("shapefile"))
				retval = new ShapefileDataset(name, extras);
			else if (type.equalsIgnoreCase("jdbc"))
				retval = new JDBCDataset(name, extras);
			else if (type.equalsIgnoreCase("osm"))
				retval = new OpenStreetMapDataset(name, extras, stdin);
			else if (type.equalsIgnoreCase("internal"))
				retval = new InternalDataset(name, extras);
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
				": " + name + Constants.LINE_SEPARATOR + errorMessage);
		}
		return(retval);
	}
}
