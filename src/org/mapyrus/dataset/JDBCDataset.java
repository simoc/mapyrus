/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004 Simon Chenery.
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

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.sql.*;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading of geographic datasets from an RDBMS via JDBC interface.
 */
public class JDBCDataset implements GeographicDataset
{
	/*
	 * Single shared connection for each database we are connected to.
	 */
	private static HashMap mDbs = new HashMap();

	/*
	 * SQL query being executed and it's result.
	 */
	private Statement mStatement = null;
	private ResultSet mResultSet = null;
	private String mSql;

	/*
	 * Names and types of fields returned from SQL query.
	 */
	private String []mFieldNames;
	private int []mFieldTypes;

	/**
	 * Initialise and return single shared database connection.
	 * @param url database connection string
	 * @param username username for database connection
	 * @param password password for database connection
	 * @return database connection
	 */
	private synchronized Connection getConnection(String url, Properties properties)
		throws SQLException
	{
		Connection retval = (Connection)mDbs.get(url);
		if (retval == null)
		{
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
			mDbs.put(url, retval);
		}
		return(retval);
	}

	/**
	 * Open connection to RDBMS and make SQL query, returning geographic data.
	 * @param filename is SQL select statement to read data from.
	 * @param extras if RDBMS connection options.
	 * @throws MapyrusException
	 */
	public JDBCDataset(String filename, String extras)
		throws MapyrusException
	{
		StringTokenizer st;
		String token, key, value;
		String driver = null;
		String url = null;
		Properties properties = new Properties();
		Connection connection;

		mSql = filename;

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			int index = token.indexOf('=');
			if (index > 0)
			{
				key = token.substring(0, index);
				value = token.substring(index + 1);

				if (key.equals("driver"))
					driver = value;
				else if (key.equals("url"))
					url = value;
				else
				{
					properties.put(key, value);
				}
			}
		}

		try
		{
			if (driver != null)
				Class.forName(driver);
		}
		catch (ClassNotFoundException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_JDBC_CLASS) +
				": " + e.getMessage() + ": " + driver);
		}

		try
		{
			/*
			 * Connect to database.
			 */
			connection = getConnection(url, properties);
		}
		catch (SQLException e)
		{
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() + ": " +
				 e.getSQLState() + ": " + url);
		}

		try
		{
			/*
			 * Send SQL query to database so we can immediately find the
			 * field names and types it returns.
			 */
			mStatement = connection.createStatement();

			try
			{
				/*
				 * Set timeout for SQL statement execution but just continue
				 * anyway if database does not support it.
				 */
				mStatement.setQueryTimeout(Constants.DB_CONNECTION_TIMEOUT);
			}
			catch (SQLException e)
			{
			}

			mResultSet = mStatement.executeQuery(mSql);
			ResultSetMetaData resultSetMetadata = mResultSet.getMetaData();

			int columnCount = resultSetMetadata.getColumnCount();
			mFieldNames = new String[columnCount];
			mFieldTypes = new int[columnCount];
			for (int i = 0; i < columnCount; i++)
			{
				mFieldNames[i] = resultSetMetadata.getColumnName(i + 1);

				/*
				 * Check that field name is acceptable for defining as variable name.
				 */
				char c = mFieldNames[i].charAt(0);
				boolean isValidName = (Character.isLetter((char)c) || c == '$');

				if (isValidName)
				{
					int j = 1;
					while (isValidName && j < mFieldNames[i].length())
					{
						c = mFieldNames[i].charAt(j);
						isValidName = (c == '.' || c == '_' ||
							Character.isLetterOrDigit((char)c));
						j++;
					}
				}
				if (!isValidName)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FIELD_NAME) +
						": " + mFieldNames[i]);
				}

				mFieldTypes[i] = resultSetMetadata.getColumnType(i + 1);
			}
		}
		catch (SQLException e1)
		{
			/*
			 * Free any database statement created for this statement.
			 */
			try
			{
				close();
			}
			catch (MapyrusException e)
			{
			}

			throw new MapyrusException(e1.getErrorCode() + ": " + e1.getMessage() + ": " +
				e1.getSQLState() + ": " + mSql);
		}
		catch (MapyrusException e2)
		{
			/*
			 * Free any database statement created for this statement.
			 */
			try
			{
				close();
			}
			catch (MapyrusException e)
			{
			}

			throw e2;
		}
	}

	/**
	 * Projection of database data not known.
	 */
	public String getProjection()
	{
		return null;
	}

	/**
	 * Return metadata about the dataset.
	 * @return metadata as (key, value) pairs.
	 */
	public Hashtable getMetadata()
	{
		return null;
	}

	/**
	 * Return names of fields returned by the SQL query.
	 * @return fieldnames.
	 */
	public String[] getFieldNames()
	{
		return mFieldNames;
	}

	/**
	 * Return extents of query results.  We do not know this.
	 * @return degree values covering the whole world
	 */
	public Double getWorlds()
	{
		return new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0);
	}

	/**
	 * Fetch next row returned by query.
	 * @return next row returned by query, or null if no more rows available.
	 */
	public Row fetch() throws MapyrusException
	{
		Row retval;
		Argument arg;
		boolean bool;

		try
		{		
			if (mResultSet.next())
			{
				/*
				 * Convert row returned by JDBC to row expected by Mapyrus.
				 */
				retval = new Row();
				for (int i = 0; i < mFieldTypes.length; i++)
				{
					if (mFieldTypes[i] == Types.TINYINT)
						arg = new Argument(mResultSet.getByte(i + 1));
					else if (mFieldTypes[i] == Types.SMALLINT)
						arg = new Argument(mResultSet.getShort(i + 1));
					else if (mFieldTypes[i] == Types.INTEGER)
						arg = new Argument(mResultSet.getInt(i + 1));
					else if (mFieldTypes[i] == Types.BIGINT)
						arg = new Argument(mResultSet.getLong(i + 1));
					else if (mFieldTypes[i] == Types.REAL)
						arg = new Argument(mResultSet.getFloat(i + 1));
					else if (mFieldTypes[i] == Types.FLOAT ||
						mFieldTypes[i] == Types.DOUBLE)
					{
						arg = new Argument(mResultSet.getDouble(i + 1));
					}
					else if (mFieldTypes[i] == Types.DECIMAL ||
						mFieldTypes[i] == Types.NUMERIC)
					{
						arg = new Argument(mResultSet.getDouble(i + 1));
					}
					else if (mFieldTypes[i] == Types.BIT)
					{
						bool = mResultSet.getBoolean(i + 1);
						arg = (bool ? Argument.numericOne : Argument.numericZero);
					}
					else if (mFieldTypes[i] == Types.CHAR ||
						mFieldTypes[i] == Types.VARCHAR ||
						mFieldTypes[i] == Types.LONGVARCHAR ||
						mFieldTypes[i] == Types.CLOB)
					{
						String fieldValue = mResultSet.getString(i + 1);
						if (fieldValue == null)
							arg = Argument.emptyString;
						else
							arg = new Argument(Argument.STRING, fieldValue);
					}
					else if (mFieldTypes[i] == Types.BINARY ||
						mFieldTypes[i] == Types.VARBINARY ||
						mFieldTypes[i] == Types.LONGVARBINARY)
					{
						byte []b = mResultSet.getBytes(i + 1);
						double []geometry = WKBGeometryParser.parse(b);
						arg = new Argument((int)geometry[0], geometry);
					}
					else if (mFieldTypes[i] == Types.BLOB)
					{
						Blob blob = mResultSet.getBlob(i + 1);
						byte []b = blob.getBytes(0, (int)blob.length());
						double []geometry = WKBGeometryParser.parse(b);
						arg = new Argument((int)geometry[0], geometry);
					}
					else if (mFieldTypes[i] == Types.DATE ||
						mFieldTypes[i] == Types.TIME ||
						mFieldTypes[i] == Types.TIMESTAMP)
					{
						String fieldValue = mResultSet.getString(i + 1);
						if (fieldValue == null)
							arg = Argument.emptyString;
						else
							arg = new Argument(Argument.STRING, fieldValue);
					}
					else
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNKNOWN_FIELD_TYPE) +
							": " + mFieldTypes[i]);
					}
					retval.add(arg);
				}
			}
			else
			{
				retval = null;
			}
		}
		catch (SQLException e)
		{
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() + ": " +
				e.getSQLState() + ": " + mSql);
		}
		return(retval);
	}

	/**
	 * Finish query from database.
	 */
	public void close() throws MapyrusException
	{
		try
		{
			if (mStatement != null)
				mStatement.close();
		}
		catch (SQLException e)
		{
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() + ": " +
				e.getSQLState());
		}
	}
}
