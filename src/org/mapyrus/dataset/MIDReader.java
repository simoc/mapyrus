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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;

import org.mapyrus.Argument;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading attributes from MID file that
 * acccompanies MIF file in MapInfo Interchange Format dataset.
 */
public class MIDReader
{
	/*
	 * Types of attribute fields encountered in .mid file. 
	 */	
	public static final int NUMERIC_FIELD = 0;
	public static final int STRING_FIELD = 1;
	public static final int LOGICAL_FIELD = 2;

	private BufferedInputStream mFile;
	private String mFilename;
	private byte mFieldDelimiter;
	private StringBuffer mFieldBuffer;
	private int mRowCounter;
	
	/*
	 * Type of each field, read from .MIF file.
	 */
	private int []mFieldTypes;
	
	/*
	 * List of flags indicating which fields are to be fetched for each row.
	 */
	private boolean []mFieldsToFetch;

	public MIDReader(String filename, int []fieldTypes, boolean []fieldsToFetch,
		char delimiter) throws IOException
	{		
		mFile = new BufferedInputStream(new FileInputStream(filename));
		mFilename = filename;
		mFieldDelimiter = (byte)delimiter;
		mFieldTypes = fieldTypes;
		mFieldsToFetch = fieldsToFetch;
		mFieldBuffer = new StringBuffer();
		mRowCounter = 1;
	}

	/**
	 * Read next row from MID file.
	 * @param row row to add fields to.
	 * @return row with added fields.
	 * @throws IOException
	 * @throws MapyrusException
	 */
	public Row getRow(Row row)
		throws IOException, MapyrusException
	{
		Argument field;

		for (int i = 0; i < mFieldTypes.length; i++)
		{
			mFieldBuffer.setLength(0);

			int c = mFile.read();
			boolean openedQuotes = (c == '"');
			if (openedQuotes)
				c = mFile.read();

			/*
			 * Read each character of field value.
			 */
			boolean closedQuotes = false;
			while (c != mFieldDelimiter || (openedQuotes && !closedQuotes))
			{
				if (c == -1)
				{
					throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
						": " + mFilename);
				}

				/*
				 * End of line marks end of last field.
				 */
				if (c == '\r' || c == '\n')
				{
					if (i != mFieldTypes.length - 1)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.MISSING_FIELD) +
							": " + mFilename + ":" + mRowCounter);
					}

					/*
					 * Accept all kinds of line endings: "\n", "\r\n", "\r".
					 */
					if (c == '\r')
					{
						mFile.mark(2);
						c = mFile.read();
						if (c != '\n')
							mFile.reset();
					}
					break;
				}

				if (openedQuotes && c == '"')
				{
					closedQuotes = true;
				}
				else
				{
					mFieldBuffer.append((char)c);
				}
				c = mFile.read();
			}

			if (mFieldsToFetch[i])
			{
				/*
				 * Convert field to Mapyrus Argument object, depending on the
				 * field type.
				 * Use constants for frequently occurring field values.
				 */
				String fieldValue = mFieldBuffer.toString();
				if (mFieldTypes[i] == NUMERIC_FIELD)
				{
					if (fieldValue.equals("0"))
					{
						field = Argument.numericZero;
					}
					else
					{
						double d = Double.parseDouble(fieldValue);
						field = new Argument(d);
					}
				}
				else if (mFieldTypes[i] == LOGICAL_FIELD)
				{
					c = mFieldBuffer.charAt(0);
					field = (c == 'T' || c == 't') ? Argument.numericOne : Argument.numericZero;
				}
				else if (mFieldBuffer.length() == 0)
				{
					field = Argument.emptyString;
				}
				else
				{
					field = new Argument(Argument.STRING, fieldValue);
				}
				row.add(field);
			}
		}

		mRowCounter++;
		return(row);
	}

	/*
	 * Close MID file.
	 */
	public void close()
	{
		try
		{
			if (mFile != null)
				mFile.close();
		}
		catch (IOException e)
		{
		}
		mFile = null;
	}
}
