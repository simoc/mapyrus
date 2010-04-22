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

import java.io.File;

/**
 * A temporary file that is automatically deleted once it expires,
 * or when the JVM terminates.
 */
public class TransientFile
{
	String m_filename;
	long m_expiryTimestamp;
	
	/**
	 * Create new transient file.
	 * @param filename name of temporary file.
	 * @param expiry timestamp in milliseconds since 1970 at which the file expires,
	 */
	public TransientFile(String filename, long expiry)
	{
		m_filename = filename;
		m_expiryTimestamp = expiry;	

		File f = new File(filename);
		try
		{
			f.deleteOnExit();
		}
		catch (SecurityException e)
		{
			/*
			 * Ignore this.  If we don't have permission to
			 * delete the file then we won't have permission
			 * to create the file either.
			 */
		}
	}

	/**
	 * Returns name of temporary file.
	 * @return filename.
	 */
	public String getFilename()
	{
		return(m_filename);
	}

	/**
	 * Return expiry timestamp of this temporary file.
	 * @return expiry timestamp measured in milliseconds since 1970.
	 */
	public long getExpiry()
	{
		return(m_expiryTimestamp);
	}
	
	public String toString()
	{
		return(m_filename + " expiring at " + m_expiryTimestamp);
	}
}
