/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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
	String mFilename;
	long mExpiryTimestamp;
	
	/**
	 * Create new transient file.
	 * @param filename name of temporary file.
	 * @param expiry timestamp in milliseconds since 1970 at which the file expires,
	 */
	public TransientFile(String filename, long expiry)
	{
		mFilename = filename;
		mExpiryTimestamp = expiry;	

		File f = new File(filename);
		f.deleteOnExit();
	}

	/**
	 * Returns name of temporary file.
	 * @return filename.
	 */
	public String getFilename()
	{
		return(mFilename);
	}

	/**
	 * Return expiry timestamp of this temporary file.
	 * @return expiry timestamp measured in milliseconds since 1970.
	 */
	public long getExpiry()
	{
		return(mExpiryTimestamp);
	}
	
	public String toString()
	{
		return(mFilename + " expiring at " + mExpiryTimestamp);
	}
}
