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

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

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
	 * SQL query being executed and it's result.
	 */
	private Connection m_connection = null;
	private Statement m_statement = null;
	private ResultSet m_resultSet = null;
	private String m_sql;

	private String m_url;
	private String m_jndiName = null;

	/*
	 * Names and types of fields returned from SQL query.
	 */
	private String []m_fieldNames;
	private int []m_fieldTypes;

	/**
	 * Open connection to RDBMS and make SQL query, returning geographic data.
	 * @param filename is SQL select statement to read data from.
	 * @param extras if RDBMS connection options.
	 * @throws MapyrusException if connection to RDBMS fails.
	 */
	public JDBCDataset(String filename, String extras)
		throws MapyrusException
	{
		StringTokenizer st;
		String token, key, value;
		String driver = null;
		Properties properties = new Properties();

		m_sql = filename;

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
					m_url = value;
				else if (key.equals("jndiname"))
					m_jndiName = value;
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
			if (m_jndiName != null)
			{
				/*
			 	 * Connect to database using JNDI.
				 * Obtain our environment naming context.
				 */
				Context initCtx = new InitialContext();
				Context envCtx = (Context) initCtx.lookup("java:comp/env");

				/*
				 * Look up our data source.
				 */
				DataSource ds = (DataSource)envCtx.lookup(m_jndiName);

				/*
				 * Allocate and use a connection from the pool.
				 */
				m_connection = ds.getConnection();
			}
			else
			{
				m_connection = ConnectionPool.get(m_url, properties);
			}
		}
		catch (SQLException e)
		{
			String state = e.getSQLState();
			if (state != null)
				state = ": " + state;
			else
				state = "";
			if (m_url == null)
				m_url = "";
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() +
				 state + ": " + m_url);
		}
		catch (NamingException e)
		{
			throw new MapyrusException(e.getMessage() + ": " + m_jndiName);
		}

		try
		{
			/*
			 * Send SQL query to database so we can immediately find the
			 * field names and types it returns.
			 */
			m_statement = m_connection.createStatement();

			try
			{
				/*
				 * Set timeout for SQL statement execution but just continue
				 * anyway if database does not support it.
				 */
				m_statement.setQueryTimeout(Constants.DB_CONNECTION_TIMEOUT);
			}
			catch (SQLException e)
			{
			}

			m_resultSet = m_statement.executeQuery(m_sql);
			ResultSetMetaData resultSetMetadata = m_resultSet.getMetaData();

			int columnCount = resultSetMetadata.getColumnCount();
			m_fieldNames = new String[columnCount];
			m_fieldTypes = new int[columnCount];
			for (int i = 0; i < columnCount; i++)
			{
				m_fieldNames[i] = resultSetMetadata.getColumnName(i + 1);

				/*
				 * Check that field name is acceptable for defining as variable name.
				 */
				char c = m_fieldNames[i].charAt(0);
				boolean isValidName = (Character.isLetter(c) || c == '$');

				if (isValidName)
				{
					int j = 1;
					while (isValidName && j < m_fieldNames[i].length())
					{
						c = m_fieldNames[i].charAt(j);
						isValidName = (c == '.' || c == '_' ||
							Character.isLetterOrDigit(c));
						j++;
					}
				}
				if (!isValidName)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_FIELD_NAME) +
						": " + m_fieldNames[i]);
				}

				m_fieldTypes[i] = resultSetMetadata.getColumnType(i + 1);
			}
		}
		catch (SQLException e1)
		{
			/*
			 * Free any database statement created for this statement.
			 */
			try
			{
				close(false);
			}
			catch (MapyrusException e)
			{
			}

			String state = e1.getSQLState();
			if (state != null)
				state = ": " + state;
			else
				state = "";
			throw new MapyrusException(e1.getErrorCode() + ": " + e1.getMessage() +
				state + ": " + m_sql);
		}
		catch (MapyrusException e2)
		{
			/*
			 * Free any database statement created for this statement.
			 */
			try
			{
				close(false);
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
	@Override
	public String getProjection()
	{
		return null;
	}

	/**
	 * Return metadata about the dataset.
	 * @return metadata as (key, value) pairs.
	 */
	@Override
	public Hashtable<String, String> getMetadata()
	{
		return null;
	}

	/**
	 * Return names of fields returned by the SQL query.
	 * @return fieldnames.
	 */
	@Override
	public String[] getFieldNames()
	{
		return m_fieldNames;
	}

	/**
	 * Return extents of query results.  We do not know this.
	 * @return degree values covering the whole world
	 */
	@Override
	public Double getWorlds()
	{
		return new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0);
	}

	/**
	 * Fetch next row returned by query.
	 * @return next row returned by query, or null if no more rows available.
	 */
	@Override
	public Row fetch() throws MapyrusException
	{
		Row retval;
		Argument arg;
		boolean bool;

		try
		{		
			if (m_resultSet.next())
			{
				/*
				 * Convert row returned by JDBC to row expected by Mapyrus.
				 */
				retval = new Row();
				for (int i = 0; i < m_fieldTypes.length; i++)
				{
					if (m_fieldTypes[i] == Types.TINYINT)
						arg = new Argument(m_resultSet.getByte(i + 1));
					else if (m_fieldTypes[i] == Types.SMALLINT)
						arg = new Argument(m_resultSet.getShort(i + 1));
					else if (m_fieldTypes[i] == Types.INTEGER)
						arg = new Argument(m_resultSet.getInt(i + 1));
					else if (m_fieldTypes[i] == Types.BIGINT)
						arg = new Argument(m_resultSet.getLong(i + 1));
					else if (m_fieldTypes[i] == Types.REAL)
						arg = new Argument(m_resultSet.getFloat(i + 1));
					else if (m_fieldTypes[i] == Types.FLOAT ||
						m_fieldTypes[i] == Types.DOUBLE)
					{
						arg = new Argument(m_resultSet.getDouble(i + 1));
					}
					else if (m_fieldTypes[i] == Types.DECIMAL ||
						m_fieldTypes[i] == Types.NUMERIC)
					{
						arg = new Argument(m_resultSet.getDouble(i + 1));
					}
					else if (m_fieldTypes[i] == Types.BIT)
					{
						bool = m_resultSet.getBoolean(i + 1);
						arg = (bool ? Argument.numericOne : Argument.numericZero);
					}
					else if (m_fieldTypes[i] == Types.CHAR ||
						m_fieldTypes[i] == Types.VARCHAR ||
						m_fieldTypes[i] == Types.LONGVARCHAR ||
						m_fieldTypes[i] == Types.CLOB)
					{
						String fieldValue = m_resultSet.getString(i + 1);
						if (fieldValue == null)
							arg = Argument.emptyString;
						else
							arg = new Argument(Argument.STRING, fieldValue);
					}
					else if (m_fieldTypes[i] == Types.BINARY ||
						m_fieldTypes[i] == Types.VARBINARY ||
						m_fieldTypes[i] == Types.LONGVARBINARY ||
						m_fieldTypes[i] == Types.BLOB)
					{
						byte []b = m_resultSet.getBytes(i + 1);
						if (b == null)
						{
							arg = Argument.emptyGeometry;
						}
						else
						{
							double []geometry = WKBGeometryParser.parse(b);
							arg = new Argument((int)geometry[0], geometry);
						}
					}
					else if (m_fieldTypes[i] == Types.DATE ||
						m_fieldTypes[i] == Types.TIME ||
						m_fieldTypes[i] == Types.TIMESTAMP)
					{
						String fieldValue = m_resultSet.getString(i + 1);
						if (fieldValue == null)
							arg = Argument.emptyString;
						else
							arg = new Argument(Argument.STRING, fieldValue);
					}
					else if (m_fieldTypes[i] == Types.OTHER)
					{
						byte b[] = m_resultSet.getBytes(i + 1);
						if (b == null || b.length == 0)
						{
							arg = Argument.emptyString;
						}
						else
						{
							try
							{
								/*
								 * PostGIS returns geometry types as a hex digit string.
								 * If we can parse this then attempt to set it as a geometry
								 * string, falling back to a plain text string if we fail.
								 */
								byte []rawBytes = parseHexDigits(b);
								if (rawBytes != null)
								{
									double []geometry = WKBGeometryParser.parse(rawBytes);
									arg = new Argument((int)geometry[0], geometry);
								}
								else
								{
									arg = new Argument(Argument.STRING, new String(b));
								}
							}
							catch (MapyrusException e)
							{
								arg = new Argument(Argument.STRING, new String(b));
							}
						}
					}
					else if (m_fieldTypes[i] == 2002)
					{
						Object obj = m_resultSet.getObject(i + 1);
						if (obj == null)
						{
							arg = Argument.emptyString;
						}
						else
						{
							arg = OracleGeometry.parseGeometry(obj);
						}
					}
					else
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNKNOWN_FIELD_TYPE) +
							": " + m_fieldTypes[i]);
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
			String state = e.getSQLState();
			if (state != null)
				state = ": " + state;
			else
				state = "";
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() +
				state + ": " + m_sql);
		}
		return(retval);
	}

	/**
	 * Parse ASCII hex digits into byte array.
	 * @param b array containing hex digits, with one character per byte.
	 * @return array of parsed hex digits, or null if array does not contain hex digits.
	 */
	private byte []parseHexDigits(byte []b)
	{
		byte retval[] = new byte[b.length / 2];
		int lastByte = 0;
		for (int i = 0; i < b.length; i++)
		{
			int n = b[i];
			if (n >= '0' && n <= '9')
				n = n - '0';
			else if (n >= 'A' && n <= 'F')
				n = n - 'A' + 10;
			else if (n >= 'a' && n <= 'f')
				n = n - 'a' + 10;
			else
			{
				/*
				 * Encountered something that is not a hex digit.
				 * Return failure.
				 */
				return(null);
			}

			/*
			 * Pack each two parse hex digits into a single byte.
			 */
			if (i % 2 != 0)
			{
				retval[i / 2] = (byte)((lastByte << 4) | n);
			}
			lastByte = n;
		}
		return(retval);
	}

	private void close(boolean succeeded) throws MapyrusException
	{
		try
		{
			if (m_statement != null)
				m_statement.close();
		}
		catch (SQLException e)
		{
			String state = e.getSQLState();
			if (state != null)
				state = ": " + state;
			else
				state = "";
			throw new MapyrusException(e.getErrorCode() + ": " + e.getMessage() +
				state);
		}
		finally
		{
			if (m_connection != null)
			{
				if (m_jndiName != null)
				{
					try
					{
						/*
						 * Return JDBC connection that we have finished with.
						 */
						m_connection.close();
					}
					catch (SQLException e)
					{
					}
				}
				else
				{
					ConnectionPool.put(m_url, m_connection, succeeded);
				}
			}
			m_connection = null;
			m_statement = null;
		}
	}

	/**
	 * Finish query from database.
	 */
	@Override
	public void close() throws MapyrusException
	{
		close(true);
	}
}
