/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2007 Simon Chenery.
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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import org.mapyrus.Constants;

/**
 * Implements pool of connections to databases that can be used
 * by one thread and then returned for reuse by another thread.
 * Size of pool is not limited.  A new connection will be made
 * if there are no connections available for reuse.
 */
public class ConnectionPool
{
	private static HashMap<String, LinkedList<TimeStampedConnection>> mPool =
		new HashMap<String, LinkedList<TimeStampedConnection>>();

	/**
	 * Get an unused or new database connection.
	 * @param url database connection string
	 * @param properties username, password for database connection
	 * @return database connection
	 */
	public static synchronized Connection get(String url, Properties properties)
		throws SQLException
	{
		LinkedList<TimeStampedConnection> connections = mPool.get(url);
		if (connections != null)
		{
			/*
			 * Close any database connections that have not
			 * been used for a long time.
			 * This avoids any socket timeout on the database connection
			 * and holding idle connections open.
			 */
			long now = System.currentTimeMillis();
			ListIterator it = connections.listIterator();
			while (it.hasNext())
			{
				TimeStampedConnection tc = (TimeStampedConnection)it.next();
				long age = now - tc.getLastUseTimeStamp();
				if (age > Constants.DB_IDLE_TIMEOUT * 1000)
				{
					try
					{
						tc.getConnection().close();
					}
					catch (SQLException ignore)
					{
					}
					it.remove();
				}
			}
		}

		if (connections == null)
		{
			connections = new LinkedList<TimeStampedConnection>();
			mPool.put(url, connections);
		}

		Connection retval;
		if (connections.isEmpty())
		{
			/*
			 * Create a new connection to database.
			 */
			DriverManager.setLoginTimeout(Constants.DB_CONNECTION_TIMEOUT);
			retval = DriverManager.getConnection(url, properties);

			/*
			 * Some operations can be optimised if database
			 * knows that this connection is read-only.
			 */
			try
			{
				retval.setReadOnly(true);
			}
			catch (UnsupportedOperationException e)
			{
				/*
				 * No problem if database does not support read-only operation.
				 */
			}
		}
		else
		{
			/*
			 * Reuse an existing connection.
			 */
			retval = ((TimeStampedConnection)connections.removeFirst()).getConnection();
		}
		return(retval);
	}

	/**
	 * Return database connection to pool after use.
	 * @param url database connection string
	 * @param connection connection to return
	 * @param isGoodConnection true if connection used successfully.
	 */
	public static synchronized void put(String url, Connection connection,
		boolean isGoodConnection)
	{
		if (isGoodConnection)
		{
			/*
			 * Place connection back in the pool for reuse.
			 */
			LinkedList<TimeStampedConnection> connections = mPool.get(url);
			connections.add(new TimeStampedConnection(connection));
		}
		else
		{
			/*
			 * Close bad connection.  We will reconnect to
			 * database next time.
			 */
			try
			{
				connection.close();
			}
			catch (SQLException ignore)
			{
			}
		}
	}
}
