/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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

import java.net.URLConnection;

/**
 * MIME types for filename extensions.
 * Provides a lookup of MIME types for filename extensions.
 */
public class MimeTypes
{
	/**
	 * Returns the MIME type for a given file extension.
	 * @param extension is filename suffix to lookup MIME type for.
	 * @return MIME type for extension, or null if it is not known.
	 */
	public static String get(String extension)
	{
		String retval;
		if (extension.equalsIgnoreCase("svg") ||
			extension.equalsIgnoreCase("svgz"))
		{
			/*
			 * Convert SVG MIME type ourselves as it does not
			 * yet appear in my MIME type database.
			 */
			retval = "image/svg+xml";
		}
		else if (extension.equalsIgnoreCase("bmp"))
		{
			/*
			 * BMP image format also not in my MIME type database.
			 */
			retval = "image/bmp";
		}
		else if (extension.equalsIgnoreCase("ico"))
		{
			/*
			 * ICO image format also not in my MIME type database.
			 */
			retval = "image/x-icon";
		}
		else if (extension.equalsIgnoreCase("tfw"))
		{
			/*
			 * Geoferencing files for images are text files.
			 */
			retval = "text/plain";
		}
		else if (extension.equalsIgnoreCase("js"))
		{
			retval = "application/x-javascript";
		}
		else if (extension.equalsIgnoreCase("css"))
		{
			retval = "text/css";
		}
		else
		{
			retval = URLConnection.guessContentTypeFromName("x." + extension);
		}
		return(retval);
	}
}
