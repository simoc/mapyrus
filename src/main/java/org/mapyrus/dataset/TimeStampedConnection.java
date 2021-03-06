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

import java.sql.Connection;

/**
 * Holds a JDBC connection and the timestamp of the last time it was used.
 */
public class TimeStampedConnection
{
	private Connection m_connection;
	private long m_lastUseTimeStamp;

	/**
	 * Create new timestamped connection.
	 * @param connection JDBC connection.
	 */
	public TimeStampedConnection(Connection connection)
	{
		m_connection = connection;
		m_lastUseTimeStamp = System.currentTimeMillis();
	}
	
	/**
	 * Get JDBC connection.
	 * @return JDBC connection.
	 */
	public Connection getConnection()
	{
		return(m_connection);
	}
	
	/**
	 * Get time stamp of time connection was last used.
	 * @return timestamp in milliseconds.
	 */
	public long getLastUseTimeStamp()
	{
		return(m_lastUseTimeStamp);
	}
}
