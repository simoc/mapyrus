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
		String retval = URLConnection.guessContentTypeFromName("x." + extension);
		return(retval);
	}
}
