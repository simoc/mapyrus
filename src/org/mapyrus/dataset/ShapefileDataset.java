/*
 * $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import au.id.chenery.mapyrus.Argument;
import au.id.chenery.mapyrus.MapyrusException;
import au.id.chenery.mapyrus.Row;

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
	 * Files containing data, their lengths and type.
	 */	
	private DataInputStream mShapeStream;
	private DataInputStream mDBFStream;
	private String mFilename;
	private int mShapeFileLength, mShapeFileType;
	private int mDBFRecordLength;

	/*
	 * Field names, types, and types and lengths as given in shape file.
	 */
	private String []mFieldNames;
	private int []mFieldTypes;
	private int []mShapeFieldTypes;
	private int []mShapeFieldLengths;

	/*
	 * Index of field containing geometry.
	 */
	private int []mGeometryField;

	/*
	 * Extents of shape file.
	 */
	private Rectangle2D.Double mExtents;
	
	/*
	 * Extents being queried and number of records read for query.
	 * A record read from DBF file for query.
	 */
	private Rectangle2D.Double mQueryExtents;
	private int mBytesRead;
	private byte []mDBFRecord;

	/*
	 * Row and geometry returned for each result fetched for query.
	 * Path containing geometry for each row.  Used for each shape to avoid
	 * continually allocating and freeing memory.
	 */
	private Row mRow;
	private double []mPath;

	/**
	 * Open ESRI shape file containing geographic data for querying.
	 * @param filename name of shape file to open, with or without shp suffix.
	 * @param extras options specific to text file datasets, given as var=value pairs.
	 * @param geometryFieldNames comma separated list of names of fields containing geometry.
	 */	
	public ShapefileDataset(String filename, String extras, String []geometryFieldNames)
		throws FileNotFoundException, IOException, MapyrusException
	{
		String shapeFilename, dbfFilename;
		
		/*
		 * Determine full names of .shp and .dbf files.
		 */
		if (filename.endsWith(".shp"))
		{
			shapeFilename = filename;
			mFilename = filename.substring(0, filename.length() - 4);
			dbfFilename = mFilename + ".dbf";
		}
		else if (filename.endsWith(".SHP"))
		{
			shapeFilename = filename;
			mFilename = filename.substring(0, filename.length() - 4);
			dbfFilename = mFilename + ".DBF";
		}
		else
		{
			mFilename = filename;
			shapeFilename = filename + ".shp";
			dbfFilename = filename + ".dbf";
		}
			
		mShapeStream = new DataInputStream(new BufferedInputStream(new FileInputStream(shapeFilename)));
		mDBFStream = new DataInputStream(new BufferedInputStream(new FileInputStream(dbfFilename)));

		/*
		 * Read shape header, checking magic number and reading everything with
		 * correct byte order.
		 */
		readShapeHeader();
		
		/*
		 * Read header from database file to get names and types of other fields.
		 */
		readDBFHeader(geometryFieldNames);
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
		double xMin, yMin, xMax, yMax, zMin, zMax, mMin, mMax;

		magic = mShapeStream.readInt();
		if (magic != MAGIC_NUMBER)
		{
			throw new MapyrusException("File '" + mFilename +
				".shp' is not ESRI shape file format.");
		}

		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeStream.readInt();
		mShapeFileLength = (mShapeStream.readInt() * 2 - 100);
		int version = readLittleEndianInt(mShapeStream);
		mShapeFileType = readLittleEndianInt(mShapeStream);
		xMin = readLittleEndianDouble(mShapeStream);
		yMin = readLittleEndianDouble(mShapeStream);
		xMax = readLittleEndianDouble(mShapeStream);
		yMax = readLittleEndianDouble(mShapeStream);
		zMin = readLittleEndianDouble(mShapeStream);
		zMax = readLittleEndianDouble(mShapeStream);
		mMin = readLittleEndianDouble(mShapeStream);
		mMax = readLittleEndianDouble(mShapeStream);
		mExtents = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
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
		while (i > offset && (buf[i] == 0 || Character.isWhitespace((char)buf[i])))
			i--;

		if (i == offset)
			retval = new String();
		else
			retval = new String(buf, offset, i - offset + 1);
		return(retval);
	}

	/*
	 * Read header from DBF database file
	 */
	private void readDBFHeader(String []geometryFieldNames) throws IOException
	{
		int nDBFRecords, headerLength, nFields;
		int i, j;
		int fieldType;
		byte dbfField[];
		Vector dbfFields = new Vector();
		int nBytesRead;

		mDBFStream.skip(4);
		nDBFRecords = readLittleEndianInt(mDBFStream);
		headerLength = readLittleEndianShort(mDBFStream);
		mDBFRecordLength = readLittleEndianShort(mDBFStream);
		mDBFStream.skip(20);
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
				nBytesRead += dbfField.length - 1;
				dbfFields.add(dbfField);
			}
		}
		while (dbfField[0] != DBF_HEADER_SENTINEL);
		nFields = dbfFields.size();

		/*
		 * Add one extra field to end of field list for the geometry.
		 */
		mFieldNames = new String[nFields + 1];
		mFieldTypes = new int[nFields + 1];
		mShapeFieldTypes = new int[nFields];
		mShapeFieldLengths = new int[nFields];

		mGeometryField = new int[1];
		mGeometryField[0] = nFields;
		if (geometryFieldNames.length > 0)
			mFieldNames[nFields] = geometryFieldNames[0];
		else
			mFieldNames[nFields] = "GEOMETRY";
			
		mFieldTypes[nFields] = Argument.GEOMETRY;

		/*
		 * Read description of each field.
		 */
		for (i = 0; i < nFields; i++)
		{
			dbfField = (byte [])(dbfFields.elementAt(i));

			/*
			 * Extract null terminated field name.
			 */			
			mFieldNames[i] = unpackString(dbfField, 0, 11);
			mShapeFieldTypes[i] = dbfField[11];
			mShapeFieldLengths[i] = dbfField[16];

			/*
			 * Convert shape field type to our representation of field types.
			 */
			switch (mShapeFieldTypes[i])
			{
				case DBF_CHARACTER:
				case DBF_DATE:
					mFieldTypes[i] = Argument.STRING;
					break;
				case DBF_LOGICAL:
				case DBF_NUMBER:
				case DBF_FLOATING:
					mFieldTypes[i] = Argument.NUMERIC;
					break;
			}
		}

		/*
		 * Leave DBF file at position of first record.
		 */
		int skipBytes = headerLength - nBytesRead;
		if (skipBytes > 0)
			mDBFStream.skip(skipBytes);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getProjection()
	 */
	public String getProjection()
	{
		return null;
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata()
	{
		return(null);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getFieldNames()
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getFieldTypes()
	 */
	public int[] getFieldTypes()
	{
		return(mFieldTypes);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getGeometryFieldIndexes()
	 */
	public int[] getGeometryFieldIndexes() 
	{
		return(mGeometryField);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#getWorlds()
	 */
	public Rectangle2D getWorlds()
	{
		return(mExtents);
	}

	/**
	 * @see au.id.chenery.mapyrus.dataset.GeographicDataset#query(Double)
	 */
	public void query(Rectangle2D.Double extents) throws MapyrusException
	{
		if (extents.intersects(mExtents))
		{
			mBytesRead = 0;
			mQueryExtents = extents;
			mPath = new double[100];
			mDBFRecord = new byte[mDBFRecordLength];
			mRow = new Row();
		}
		else
		{
			/*
			 * Shape file does not overlap current extents.  Query will return nothing.
			 */
			mBytesRead = mShapeFileLength;
		}
	}

	/**
	 * Read next shape from shapefile that is inside or crossing the query extents.
	 * @return true if a row was read.
	 */
	public Row fetch() throws MapyrusException
	{
		int recordNumber;
		int recordLength;
		double x, y, lastX, lastY, xMin, yMin, xMax, yMax;
		double fieldValue;
		int i, shapeType;
		int nBytes, nParts, nPoints, partIndex, pathIndex;
		boolean shapeInExtents = false;

		try
		{
			/*
			 * Keep reading until we get a shape inside the extents or we reach
			 * the end of the file.
			 */
			mRow.clear();
			while (!shapeInExtents && mBytesRead < mShapeFileLength)
			{
				/*
				 * Read header for next shape.  Convert record length to byte length.
				 */
				recordNumber = mShapeStream.readInt();
				recordLength = mShapeStream.readInt() * 2;
				shapeType = readLittleEndianInt(mShapeStream);

				if (mShapeFileType == POINT)
				{
					/*
					 * Read point coordinates, see if they are inside
					 * query extents.
					 */
					mPath[0] = 4;
					mPath[1] = PathIterator.SEG_MOVETO;
					
					mPath[2] = readLittleEndianDouble(mShapeStream);
					mPath[3] = readLittleEndianDouble(mShapeStream);
					if (recordLength > 20)
						mShapeStream.skip(recordLength - 20);
					shapeInExtents = mQueryExtents.contains(mPath[2], mPath[3]);
				}
				else if (mShapeFileType == POLYLINE)
				{
					/*
					 * Read bounding box of polyline.  Find if it intersects
					 * with query extents.
					 */
					xMin = readLittleEndianDouble(mShapeStream);
					yMin = readLittleEndianDouble(mShapeStream);
					xMax = readLittleEndianDouble(mShapeStream);
					yMax = readLittleEndianDouble(mShapeStream);
					shapeInExtents = mQueryExtents.intersects(xMin, yMin, xMax - xMin, yMax - yMin);
					if (shapeInExtents)
					{
						/*
						 * Read polyline coordinates.
						 */
						nParts = readLittleEndianInt(mShapeStream);
						nPoints = readLittleEndianInt(mShapeStream);

						int []parts = new int[nParts];
						for (i = 0; i < nParts; i++)
							parts[i] = readLittleEndianInt(mShapeStream);

						/*
						 * Make path array longer if this polyline has more points
						 */
						int nEls = nPoints * 3 + 1;
						if (mPath.length < nEls)
							mPath = new double[nEls];


						partIndex = 0;
						pathIndex = 1;
						lastX = lastY = Double.MAX_VALUE;
						for (i = 0; i < nPoints; i++)
						{
							/*
							 * Add next coordinates, as either a moveto or lineto.
							 */
							x = readLittleEndianDouble(mShapeStream);
							y = readLittleEndianDouble(mShapeStream);
							if (partIndex < nParts && parts[partIndex] == i)
							{
								mPath[pathIndex] = PathIterator.SEG_MOVETO;
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
								mPath[pathIndex] = PathIterator.SEG_LINETO;
							}
								
							mPath[pathIndex + 1] = lastX = x;
							mPath[pathIndex + 2] = lastY = y;
							pathIndex += 3;
						}
						mPath[0] = pathIndex;
						
						/*
						 * Skip any remaining bytes at end of record.
						 */
						nBytes = nPoints * 16 + nParts * 4 + 4 * 8 + 2 * 4 + 4;
						if (nBytes < recordLength)
							mShapeStream.skip(recordLength - nBytes);
					}
					else
					{
						/*
						 * Line is outside query extents, skip it.
						 */
						mShapeStream.skip(recordLength - 4 * 8 - 4);
					}
				}
				mBytesRead += recordLength + 8;

				/*
				 * Read attribute fields for this shape.  Don't bother
				 * unpacking them if we are skipping this shape.
				 */
				mDBFStream.read(mDBFRecord);
				while (mDBFRecord[0] == DBF_DELETED_RECORD)
				{
					/*
					 * Skip deleted records.
					 */
					mDBFStream.read(mDBFRecord);
				}

				if (shapeInExtents)
				{
					int fieldIndex = 1;
					for (i = 0; i < mShapeFieldTypes.length; i++)
					{
						Argument arg = null;

						if (mShapeFieldTypes[i] == DBF_CHARACTER ||
							mShapeFieldTypes[i] == DBF_DATE)
						{
							arg = new Argument(Argument.STRING,
								unpackString(mDBFRecord, fieldIndex, mShapeFieldLengths[i]));
						}
						else if (mShapeFieldTypes[i] == DBF_NUMBER ||
							mShapeFieldTypes[i] == DBF_FLOATING)
						{
							String s = unpackString(mDBFRecord,
								fieldIndex, mShapeFieldLengths[i]);
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
						else if (mShapeFieldTypes[i] == DBF_LOGICAL)
						{
							switch ((char)mDBFRecord[fieldIndex])
							{
								case 'y':
								case 'Y':
								case 'T':
								case 't':
									arg = new Argument(1.0);
									break;
								default:
									arg = new Argument(0.0);
									break;
							}
						}
						mRow.add(arg);

						fieldIndex += mShapeFieldLengths[i];
					}
					
					/*
					 * Add geometry as final field.
					 */
					mRow.add(new Argument(mPath));
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
			return(mRow);
		else
			return(null);
	}
}
