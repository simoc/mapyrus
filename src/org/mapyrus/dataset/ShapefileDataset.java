/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading of geographic datasets from ESRI shape files.
 */
public class ShapefileDataset implements GeographicDataset
{
	/*
	 * Magic number in file header.
	 */
	private static final int MAGIC_NUMBER = 9994;

	/*
	 * Sentinel value indicating end of header records in a DBF file.
	 * Value indicating deleted record in DBF file.
	 */
	private static final byte DBF_HEADER_SENTINEL = 0x0D;
	private static final byte DBF_DELETED_RECORD = '*';

	/*
	 * Types of data present in shape file.
	 */
	private static final int NULL_SHAPE = 0;
	private static final int POINT = 1;
	private static final int POLYLINE = 3;
	private static final int POLYGON = 5;
	private static final int MULTIPOINT = 8;
	private static final int POINT_Z = 11;
	private static final int POLYLINE_Z = 13;
	private static final int POLYGON_Z = 15;
	private static final int MULTIPOINT_Z = 18;
	private static final int POINT_M = 21;
	private static final int POLYLINE_M = 23;
	private static final int POLYGON_M = 25;
	private static final int MULTIPOINT_M = 28;
	private static final int MULTIPATCH = 31;

	/*
	 * types of fields in DBF database file.
	 */
	private static final byte DBF_CHARACTER = 'C';
	private static final byte DBF_DATE = 'D';
	private static final byte DBF_NUMBER = 'N';
	private static final byte DBF_FLOATING = 'F';
	private static final byte DBF_LOGICAL = 'L';

	/*
	 * Empty path for null shapes.
	 */
	private static final double []EMPTY_PATH = {Argument.GEOMETRY_POINT, 0};

	/*
	 * Files containing data, their lengths and type.
	 */	
	private DataInputStream mShapeStream;
	private DataInputStream mDBFStream;
	private String mFilename;
	private int mShapeFileLength, mShapeFileType, mGeometryType;
	private int mDBFRecordLength;
	private String mProjection;
	
	/*
	 * Flags indicating which fields in DBF file that user wants to fetch
	 * and the total number of fields the user wants to fetch.
	 */
	private ArrayList mDBFFieldsToFetch;
	private int mNDBFFieldsToFetch;

	/*
	 * Field names, types, and types and lengths as given in DBF file.
	 */
	private String []mFieldNames;
	private int []mFieldTypes;
	private int []mDBFFieldTypes;
	private int []mDBFFieldLengths;

	/*
	 * Extents of shape file and extents being queried.
	 */
	private Rectangle2D.Double mExtents;
	private Rectangle2D.Double mQueryExtents;
	
	/*
	 * Number of bytes already read for query.
	 * A record read from DBF file for query.
	 */
	private int mBytesRead;
	private byte []mDBFRecord;

	/**
	 * Open ESRI shape file containing geographic data for querying.
	 * @param filename name of shape file to open, with or without shp suffix.
	 * @param extras options specific to text file datasets, given as var=value pairs.
	 */	
	public ShapefileDataset(String filename, String extras)
		throws FileNotFoundException, IOException, MapyrusException
	{
		String shapeFilename, dbfFilename, prjFilename;
		StringTokenizer st, st2;
		String token, s;
		HashSet extrasDBFFields;
		double d, xMin, yMin, xMax, yMax;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		extrasDBFFields = null;
		xMin = yMin = -Float.MAX_VALUE;
		xMax = yMax = Float.MAX_VALUE;

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.startsWith("dbffields="))
			{
				/*
				 * Parse list of comma separated field names that user wants
				 * to fetch.
				 */
				extrasDBFFields = new HashSet();
				st2 = new StringTokenizer(token.substring(10), ",");
				while (st2.hasMoreTokens())
				{
					token = st2.nextToken();
					extrasDBFFields.add(token);
				}
			}
			else if (token.startsWith("xmin=") || token.startsWith("ymin=") ||
				token.startsWith("xmax=") || token.startsWith("ymax="))
			{
				s = token.substring(5);
				try
				{
					d = Double.parseDouble(s);
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
						": " + s);
				}
				if (token.startsWith("xmin="))
					xMin = d;
				else if (token.startsWith("ymin="))
					yMin = d;
				else if (token.startsWith("xmax="))
					xMax = d;
				else
					yMax = d;
			}
		}
		mQueryExtents = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);

		/*
		 * Determine full names of .shp and .dbf files.
		 */
		if (filename.endsWith(".shp"))
		{
			shapeFilename = filename;
			mFilename = filename.substring(0, filename.length() - 4);
			dbfFilename = mFilename + ".dbf";
			prjFilename = mFilename + ".prj";
		}
		else if (filename.endsWith(".SHP"))
		{
			shapeFilename = filename;
			mFilename = filename.substring(0, filename.length() - 4);
			dbfFilename = mFilename + ".DBF";
			prjFilename = mFilename + ".PRJ";
		}
		else
		{
			mFilename = filename;
			shapeFilename = filename + ".shp";
			dbfFilename = filename + ".dbf";
			prjFilename = filename + ".prj";
		}
			
		mShapeStream = new DataInputStream(new BufferedInputStream(new FileInputStream(shapeFilename)));
		try
		{
			mDBFStream = new DataInputStream(new BufferedInputStream(new FileInputStream(dbfFilename)));
		}
		catch (FileNotFoundException e)
		{
			/*
			 * If .dbf file does not exist then just continue without it.
			 */
			mDBFStream = null;
		}

		/*
		 * If there is an accompanying .prj file with the projection then read it.
		 */
		try
		{
			BufferedReader prjReader = new BufferedReader(new FileReader(prjFilename));
			mProjection = prjReader.readLine();
		}
		catch(FileNotFoundException e)
		{
			mProjection = "";
		}

		/*
		 * Read shape header, checking magic number and reading everything with
		 * correct byte order.
		 */
		readShapeHeader();

		/*
		 * Read header from database file to get names and types of other fields.
		 */
		readDBFHeader(extrasDBFFields);

		if (mQueryExtents.intersects(mExtents))
		{
			mBytesRead = 0;
			mDBFRecord = new byte[mDBFRecordLength];
		}
		else
		{
			/*
			 * Shape file does not overlap current extents.  Fetch will return nothing.
			 */
			mBytesRead = mShapeFileLength;
		}
	}

	/**
	 * Reads 8 byte little endian long integer value.
	 * @param f input stream to read from.
	 * @return long value.
	 */
	private long readLittleEndianLong(DataInputStream f) throws IOException
	{
		long n, n1, n2, n3, n4, n5, n6, n7, n8;

		n1 = f.read();
		n2 = f.read();
		n3 = f.read();
		n4 = f.read();
		n5 = f.read();
		n6 = f.read();
		n7 = f.read();
		n8 = f.read();

		n = ((n8 <<56) + (n7 << 48) + (n6 << 40) + (n5 << 32) +
			(n4 << 24) + (n3 << 16) + (n2 << 8) + n1);
		return(n);
	}

	/**
	 * Reads 4 byte little endian integer value.
	 * @param f input stream to read from.
	 * @return int value.
	 */
	private int readLittleEndianInt(DataInputStream f) throws IOException
	{
		int n, n1, n2, n3, n4;

		n1 = f.read();
		n2 = f.read();
		n3 = f.read();
		n4 = f.read();
		n = ((n4 << 24) + (n3 << 16) + (n2 << 8) + n1);

	    return(n);
	}

	/**
	 * Reads 2 byte little endian short integer value.
	 * @param f input stream to read from.
	 * @return short value.
	 */
	private short readLittleEndianShort(DataInputStream f) throws IOException
	{
		int n1, n2;

		n1 = f.read();
		n2 = f.read();

	    return((short)((n2 << 8) + n1));
	}

	/**
	 * Reads 8 byte little endian double value.
	 * @param f input stream to read from.
	 * @return double value.
	 */
	private double readLittleEndianDouble(DataInputStream f) throws IOException
	{
		long l;
		double d;

		l = readLittleEndianLong(f);
		d = Double.longBitsToDouble(l);
		return(d);
	}

	/*
	 * Read shape file header.
	 */
	private void readShapeHeader() throws IOException, MapyrusException
	{
		int magic;
		double xMin, yMin, xMax, yMax;

		magic = mShapeStream.readInt();
		if (magic != MAGIC_NUMBER)
		{
			throw new MapyrusException(mFilename + ": " +
				MapyrusMessages.get(MapyrusMessages.NOT_SHAPE_FILE));
		}

		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeFileLength = mShapeStream.readInt() * 2 - 100;
		readLittleEndianInt(mShapeStream);	/* version */
		mShapeFileType = readLittleEndianInt(mShapeStream);
		xMin = readLittleEndianDouble(mShapeStream);
		yMin = readLittleEndianDouble(mShapeStream);
		xMax = readLittleEndianDouble(mShapeStream);
		yMax = readLittleEndianDouble(mShapeStream);
		
		readLittleEndianDouble(mShapeStream);	/* zMin */
		readLittleEndianDouble(mShapeStream);	/* zMax */
		readLittleEndianDouble(mShapeStream);	/* mMin */
		readLittleEndianDouble(mShapeStream);	/* mMax */
		mExtents = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);

		/*
		 * Convert geometry type to the type we use internally.
		 */		
		switch (mShapeFileType)
		{
			case NULL_SHAPE:
				mGeometryType = 0;
				break;
			case POINT:
			case POINT_Z:
			case POINT_M:
				mGeometryType = Argument.GEOMETRY_POINT;
				break;
			case POLYLINE:
			case POLYLINE_Z:
			case POLYLINE_M:
				mGeometryType = Argument.GEOMETRY_MULTILINESTRING;
				break;
			case POLYGON:
			case POLYGON_Z:
			case POLYGON_M:
			case MULTIPATCH:
				mGeometryType = Argument.GEOMETRY_MULTIPOLYGON;
				break;
			case MULTIPOINT:
			case MULTIPOINT_Z:
			case MULTIPOINT_M:
				mGeometryType = Argument.GEOMETRY_MULTIPOINT;
				break;
		}
	}

	/**
	 * Unpack a string from a byte buffer.  Trailing whitespace or null bytes are
	 * removed from string.
	 * @param buf is buffer to unpack string from.
	 * @param offset is offset to begin unpacking in buffer
	 * @param length is number of bytes to add
	 * @return unpacked string
	 */
	private String unpackString(byte []buf, int offset, int length)
	{
		String retval;
		int i = offset + length - 1;
		while (i >= offset && (buf[i] == 0 || Character.isWhitespace((char)buf[i])))
			i--;

		if (i < offset)
			retval = "";
		else
			retval = new String(buf, offset, i - offset + 1);
		return(retval);
	}

	/*
	 * Read header from DBF database file
	 */
	private void readDBFHeader(HashSet dbfFieldnameList)
		throws IOException
	{
		int headerLength, nTotalFields;
		String fieldName;
		int i;
		int fieldIndex;
		byte dbfField[];
		ArrayList dbfFields = new ArrayList();
		int nBytesRead;
		boolean fetchStatus;

		nTotalFields = mNDBFFieldsToFetch = headerLength = nBytesRead = 0;
		mDBFFieldsToFetch = new ArrayList();

		if (mDBFStream != null)
		{
			mDBFStream.skipBytes(4);
			readLittleEndianInt(mDBFStream);	/* number of DBF records */
			headerLength = readLittleEndianShort(mDBFStream);
			mDBFRecordLength = readLittleEndianShort(mDBFStream);
			mDBFStream.skipBytes(20);
			nBytesRead = 32;

			/*
			 * Read record describing each field.
			 */
			do
			{
				dbfField = new byte[32];
				dbfField[0] = (byte)(mDBFStream.read());
				nBytesRead++;
				if (dbfField[0] != DBF_HEADER_SENTINEL)
				{
					mDBFStream.read(dbfField, 1, dbfField.length - 1);
					fieldName = unpackString(dbfField, 0, 11);
	
					/*
					 * Build list of flags indicating which fields we'll
					 * be fetching for the user.
					 */
					fetchStatus = (dbfFieldnameList == null ||
						dbfFieldnameList.contains(fieldName));
					mDBFFieldsToFetch.add(new Boolean(fetchStatus));
					if (fetchStatus)
						mNDBFFieldsToFetch++;
	
					nBytesRead += dbfField.length - 1;
					dbfFields.add(dbfField);
					nTotalFields++;
				}
			}
			while (dbfField[0] != DBF_HEADER_SENTINEL);
		}

		/*
		 * Add one extra field to end of field list for the geometry.
		 */
		mFieldNames = new String[mNDBFFieldsToFetch + 1];
		mFieldTypes = new int[mNDBFFieldsToFetch + 1];
		mDBFFieldTypes = new int[nTotalFields];
		mDBFFieldLengths = new int[nTotalFields];

		mFieldNames[mNDBFFieldsToFetch] = "GEOMETRY";
		mFieldTypes[mNDBFFieldsToFetch] = Argument.GEOMETRY;

		/*
		 * Read description of each field.
		 */
		for (i = fieldIndex = 0; i < nTotalFields; i++)
		{
			dbfField = (byte [])(dbfFields.get(i));

			mDBFFieldTypes[i] = dbfField[11];

			/*
			 * Length is unsigned byte value.
			 */
			if (dbfField[16] > 0)
				mDBFFieldLengths[i] = dbfField[16];
			else
				mDBFFieldLengths[i] = 256 + dbfField[16];

			/*
			 * Unpack field information if we are going to be fetching this field.
			 */
			if (((Boolean)mDBFFieldsToFetch.get(i)).booleanValue())
			{
				/*
				 * Extract null terminated field name.
				 */
				mFieldNames[fieldIndex] = unpackString(dbfField, 0, 11);
	
				/*
				 * Convert shape field type to our representation of field types.
				 */
				switch (mDBFFieldTypes[i])
				{
					case DBF_CHARACTER:
					case DBF_DATE:
						mFieldTypes[fieldIndex] = Argument.STRING;
						break;
					case DBF_LOGICAL:
					case DBF_NUMBER:
					case DBF_FLOATING:
						mFieldTypes[fieldIndex] = Argument.NUMERIC;
						break;
				}
				fieldIndex++;
			}
		}

		/*
		 * Leave DBF file at position of first record.
		 */
		int skipBytes = headerLength - nBytesRead;
		if (skipBytes > 0)
			mDBFStream.skipBytes(skipBytes);
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getProjection()
	 */
	public String getProjection()
	{
		return mProjection;
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata()
	{
		return(null);
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getFieldNames()
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * @see org.mapyrus.dataset.GeographicDataset#getWorlds()
	 */
	public Rectangle2D.Double getWorlds()
	{
		return(mExtents);
	}

	/**
	 * Read next shape from shapefile that is inside or crossing the query extents.
	 * @return true if a row was read.
	 */
	public Row fetch() throws MapyrusException
	{
		int recordLength;
		double x, y, lastX, lastY, xMin, yMin, xMax, yMax;
		double fieldValue;
		int i, shapeType;
		int nBytes, nParts, nPoints, partIndex, pathIndex;
		boolean shapeInExtents = false;
		Row row;
		double path[] = null;

		try
		{
			/*
			 * Keep reading until we get a shape inside the extents or we reach
			 * the end of the file.
			 */
			row = new Row();
			while (!shapeInExtents && mBytesRead < mShapeFileLength)
			{
				/*
				 * Read header for next shape.  Convert record length to byte length.
				 */
				mShapeStream.readInt();	/* record number */
				recordLength = mShapeStream.readInt() * 2;
				
				shapeType = readLittleEndianInt(mShapeStream);
				nBytes = 4;
				
				if (shapeType == 0)
				{
					/*
					 * A null shape.
					 */
					path = EMPTY_PATH;
					shapeInExtents = true;
				}
				else if (mShapeFileType == POINT || mShapeFileType == POINT_Z ||
					mShapeFileType == POINT_M)
				{
					/*
					 * Read point coordinates, see if they are inside
					 * query extents.  Skip Z and Measure values for 3D shapes.
					 */
					path = new double[5];
					path[0] = Argument.GEOMETRY_POINT;
					path[1] = 1;
					path[2] = Argument.MOVETO;
					path[3] = readLittleEndianDouble(mShapeStream);
					path[4] = readLittleEndianDouble(mShapeStream);
					nBytes += 16;

					/*
					 * Accept points on query boundary rectangle, reject anything outside.
					 */
					shapeInExtents = (mQueryExtents.outcode(path[3], path[4]) == 0);
				}
				else if (mShapeFileType == POLYLINE || mShapeFileType == POLYGON ||
					mShapeFileType == POLYLINE_Z || mShapeFileType == POLYGON_Z ||
					mShapeFileType == POLYLINE_M || mShapeFileType == POLYGON_M ||
					mShapeFileType == MULTIPATCH)
				{
					/*
					 * Read bounding box of polyline or polygon.
					 * Find if it intersects with query extents.
					 */
					xMin = readLittleEndianDouble(mShapeStream);
					yMin = readLittleEndianDouble(mShapeStream);
					xMax = readLittleEndianDouble(mShapeStream);
					yMax = readLittleEndianDouble(mShapeStream);
					nBytes += 4 * 8;
					shapeInExtents = mQueryExtents.intersects(xMin, yMin, xMax - xMin, yMax - yMin);
					if (shapeInExtents)
					{
						/*
						 * Read polyline or polygon coordinates.
						 */
						nParts = readLittleEndianInt(mShapeStream);
						nPoints = readLittleEndianInt(mShapeStream);
						nBytes += 2 * 4;

						int []parts = new int[nParts];
						for (i = 0; i < nParts; i++)
							parts[i] = readLittleEndianInt(mShapeStream);
						nBytes += nParts * 4;

						/*
						 * Skip part type information in multi-patch files.
						 */
						if (mShapeFileType == MULTIPATCH)
						{
							mShapeStream.skipBytes(nParts * 4);
							nBytes += nParts * 4;
						}
						path = new double[2 + nParts * 2 + nPoints * 3];
						int counter = 0;
						int counterIndex = 0;
						boolean isPolyline = (mShapeFileType == POLYLINE ||
							mShapeFileType == POLYLINE_M || mShapeFileType == POLYLINE_Z);

						/*
						 * Polylines in shape file may be separate LINESTRING geometries.
						 * Always return a MULTILINESTRING for polylines (even if it is
						 * only one segment) so geometry type remains consistent.
						 */
						if (isPolyline)
						{
							path[0] = Argument.GEOMETRY_MULTILINESTRING;
							path[1] = nParts;
						}
						else
						{
							path[0] = Argument.GEOMETRY_POLYGON;
						}

						partIndex = 0;
						pathIndex = 2;
						lastX = lastY = Double.MAX_VALUE;
						for (i = 0; i < nPoints; i++)
						{
							/*
							 * Add next coordinates, as either a moveto or lineto.
							 */
							x = readLittleEndianDouble(mShapeStream);
							y = readLittleEndianDouble(mShapeStream);
							nBytes += 2 * 8;
							if (partIndex < nParts && parts[partIndex] == i)
							{
								if (isPolyline)
								{
									if (partIndex > 0)
									{
										/*
										 * Set number of points in last part, allowing for duplicate
										 * points that were skipped.
										 */
										path[counterIndex] = counter;
									}
									counter = 0;

									path[pathIndex] = Argument.GEOMETRY_LINESTRING;
									counterIndex = pathIndex + 1;
									pathIndex += 2;
								}
								path[pathIndex] = Argument.MOVETO;
								pathIndex++;
								partIndex++;
							}
							else if (x == lastX && y == lastY)
							{
								/*
								 * Skip duplicate points.
								 */
								continue;
							}
							else
							{
								path[pathIndex] = Argument.LINETO;
								pathIndex++;
							}

							path[pathIndex] = lastX = x;
							path[pathIndex + 1] = lastY = y;
							pathIndex += 2;
							counter++;
						}

						/*
						 * Finally set number of points polygon or polyline, allowing
						 * for duplicate points that were skipped.
						 */
						if (isPolyline)
							path[counterIndex] = counter;
						else
							path[1] = counter;
					}
					else
					{
						/*
						 * Shape is outside query extents, skip it.
						 */
					}
				}
				else if (mShapeFileType == MULTIPOINT || mShapeFileType == MULTIPOINT_Z ||
					mShapeFileType == MULTIPOINT_M)
				{
					/*
					 * Read bounding box of points.
					 * Find if it intersects with query extents.
					 */
					xMin = readLittleEndianDouble(mShapeStream);
					yMin = readLittleEndianDouble(mShapeStream);
					xMax = readLittleEndianDouble(mShapeStream);
					yMax = readLittleEndianDouble(mShapeStream);
					nBytes += 4 * 8;
					shapeInExtents = mQueryExtents.intersects(xMin, yMin, xMax - xMin, yMax - yMin);
					if (shapeInExtents)
					{
						nPoints = readLittleEndianInt(mShapeStream);
						nBytes += 4;

						/*
						 * Read each of the points and add them to the path.
						 */
						path = new double[nPoints * 5 + 2];
						path[0] = Argument.GEOMETRY_MULTIPOINT;
						path[1] = nPoints;

						pathIndex = 2;
						for (i = 0; i < nPoints; i++)
						{
							path[pathIndex] = Argument.GEOMETRY_POINT;
							path[pathIndex + 1] = 1;
							path[pathIndex + 2] = Argument.MOVETO;
							path[pathIndex + 3] = readLittleEndianDouble(mShapeStream);
							path[pathIndex + 4] = readLittleEndianDouble(mShapeStream);
							pathIndex += 5;
							nBytes += 16;
						}
					}
				}

				/*
				 * Skip until end of this record in shape file.
				 */		
				if (nBytes < recordLength)
					mShapeStream.skipBytes(recordLength - nBytes);

				mBytesRead += recordLength + 8;

				/*
				 * If user wants any attribute fields then read them for this shape.
				 * Don't bother unpacking them if we are skipping this shape.
				 */
				if (mNDBFFieldsToFetch > 0)
				{
					mDBFStream.read(mDBFRecord);
					while (mDBFRecord[0] == DBF_DELETED_RECORD)
					{
						/*
						 * Skip deleted records.
						 */
						mDBFStream.read(mDBFRecord);
					}
				}

				if (shapeInExtents)
				{
					if (mNDBFFieldsToFetch > 0)
					{
						int recordOffset = 1;
						for (i = 0; i < mDBFFieldTypes.length; i++)
						{
							Argument arg = null;
	
							/*
							 * Only unpack fields that user asked for.
							 */
							if (((Boolean)mDBFFieldsToFetch.get(i)).booleanValue())
							{
								if (mDBFFieldTypes[i] == DBF_CHARACTER ||
									mDBFFieldTypes[i] == DBF_DATE)
								{
									arg = new Argument(Argument.STRING,
										unpackString(mDBFRecord, recordOffset,
										mDBFFieldLengths[i]));
								}
								else if (mDBFFieldTypes[i] == DBF_NUMBER ||
									mDBFFieldTypes[i] == DBF_FLOATING)
								{
									String s = unpackString(mDBFRecord,
										recordOffset, mDBFFieldLengths[i]);
									try
									{
										fieldValue = Double.parseDouble(s);
									}
									catch (NumberFormatException e)
									{
										fieldValue = 0.0;
									}
									arg = new Argument(fieldValue);
								}
								else if (mDBFFieldTypes[i] == DBF_LOGICAL)
								{
									switch ((char)mDBFRecord[recordOffset])
									{
										case 'y':
										case 'Y':
										case 'T':
										case 't':
											arg = Argument.numericOne;
											break;
										default:
											arg = Argument.numericZero;
											break;
									}
								}
								row.add(arg);
							}
	
							recordOffset += mDBFFieldLengths[i];
						}
					}

					/*
					 * Add geometry as final field.
					 */
					row.add(new Argument(mGeometryType, path));
				}
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}

		/*
		 * Return next row, or null if we did not find one.
		 */
		if (shapeInExtents)
			return(row);
		else
			return(null);
	}

	/**
	 * Closes dataset.
	 */
	public void close() throws MapyrusException
	{
		try
		{
			mShapeStream.close();
			if (mDBFStream != null)
				mDBFStream.close();
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
	}
}
