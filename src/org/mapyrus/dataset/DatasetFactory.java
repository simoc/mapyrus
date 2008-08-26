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

import java.io.IOException;
import java.io.InputStream;

import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

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
	 */
	public static GeographicDataset open(String type, String name,
		String extras, InputStream stdin) throws MapyrusException
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
			if (type.equalsIgnoreCase("textfile"))
				retval = new TextfileDataset(name, extras, stdin);
			else if (type.equalsIgnoreCase("shapefile"))
				retval = new ShapefileDataset(name, extras);
			else if (type.equalsIgnoreCase("jdbc"))
				retval = new JDBCDataset(name, extras);
			else if (type.equalsIgnoreCase("grass"))
				retval = new GrassDataset(name, extras);
			else if (type.equalsIgnoreCase("ogrinfo"))
				retval = new OGRDataset(name, extras, stdin);
			else if (type.equalsIgnoreCase("osm"))
				retval = new OpenStreetMapDataset(name, extras, stdin);
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
