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

import java.sql.Connection;

/**
 * Holds a JDBC connection and the timestamp of the last time it was used.
 */
public class TimeStampedConnection
{
	private Connection mConnection;
	private long mLastUseTimeStamp;

	/**
	 * Create new timestamped connection.
	 * @param connection JDBC connection.
	 */
	public TimeStampedConnection(Connection connection)
	{
		mConnection = connection;
		mLastUseTimeStamp = System.currentTimeMillis();
	}
	
	/**
	 * Get JDBC connection.
	 * @return JDBC connection.
	 */
	public Connection getConnection()
	{
		return(mConnection);
	}
	
	/**
	 * Get time stamp of time connection was last used.
	 * @return timestamp in milliseconds.
	 */
	public long getLastUseTimeStamp()
	{
		return(mLastUseTimeStamp);
	}
}
