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
	 * Types of data present in file.
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
	 * Flag for another row being available to be fetched for query.
	 * A record read from DBF file for query.
	 */
	private Rectangle2D.Double mQueryExtents;
	private int mBytesRead;
	private boolean mRowAvailable;
	private byte []mDBFRecord;

	/*
	 * Row and geometry returned for each result fetched for query.
	 * Bounding box of a shape.
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
			shapeFilename = filename + ".SHP";
			dbfFilename = filename + ".DBF";
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
		readDBFHeader();
	}

	/**
	 * Reads 4 byte little endian integer value.
	 * @param f input stream to read from.
	 * @return int value.
	 */
	private int readLittleEndianInt(DataInputStream f) throws IOException
	{
		long n;

		n = f.read();
		n = n | (f.read() << 8);
		n = n | (f.read() << 16);
		n = n | (f.read() << 24);

	    return((int)n);
	}

	/**
	 * Reads 2 byte little endian short integer value.
	 * @param f input stream to read from.
	 * @return short value.
	 */
	private short readLittleEndianShort(DataInputStream f) throws IOException
	{
		int n;

		n = f.read();
		n = n | (f.read() << 8);

	    return((short)n);
	}

	/*
	 * Byte buffer for reading 8 bytes as little endian double value.
	 */
	byte mDoubleBuffer[] = new byte[8];
	DataInputStream mDoubleStream =
		new DataInputStream(new ByteArrayInputStream(mDoubleBuffer));
	
	/**
	 * Reads 8 byte little endian double value.
	 * @param f input stream to read from.
	 * @return double value.
	 */
	private double readLittleEndianDouble(DataInputStream f) throws IOException
	{
		byte b;
		double d;

		/*
		 * Read 8 byte value, reverse bytes, then read it again as a double
		 * value.
		 */
		f.read(mDoubleBuffer);
		for (int i = 0; i < mDoubleBuffer.length / 2; i++)
		{
			b = mDoubleBuffer[i];
			mDoubleBuffer[i] = mDoubleBuffer[mDoubleBuffer.length - i];
			mDoubleBuffer[mDoubleBuffer.length - i] = b;
		}

		mDoubleStream.mark(mDoubleBuffer.length);
		d = mDoubleStream.readDouble();
		mDoubleStream.reset();
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
		mMin = readLittleEndianDouble(mShapeStream);
		mMax = readLittleEndianDouble(mShapeStream);
		mExtents = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
	}

	/*
	 * Read header from DBF database file
	 */
	void readDBFHeader() throws IOException
	{
		int nDBFRecords, headerLength, nFields;
		int i, j;
		int fieldType;
		byte dbfField[] = new byte[32];
		StringBuffer fieldName = new StringBuffer();

		/*
		 * The header tells us how many fields there are.
		 */
		mDBFStream.skip(4);
		nDBFRecords = readLittleEndianInt(mDBFStream);
		headerLength = readLittleEndianShort(mDBFStream);
		nFields = (headerLength - 32) / 32;
		mDBFRecordLength = readLittleEndianShort(mDBFStream);
		mDBFStream.skip(20);

		/*
		 * Add one extra field to end of list for the geometry.
		 */
		mFieldNames = new String[nFields + 1];
		mFieldTypes = new int[nFields + 1];
		mShapeFieldTypes = new int[nFields + 1];
		mShapeFieldLengths = new int[nFields + 1];

		mGeometryField = new int[1];
		mGeometryField[0] = nFields;
		mFieldNames[nFields] = "GEOMETRY";
		mFieldTypes[nFields] = Argument.GEOMETRY;

		/*
		 * Read description of each field.
		 */
		for (i = 0; i < nFields; i++)
		{
			mDBFStream.read(dbfField);

			/*
			 * Extract null terminated field name.
			 */			
			fieldName.delete(0, 11);
			for (j = 0; j < 11; j++)
			{
				if (dbfField[i] == 0)
					break;
				fieldName.append((char)dbfField[i]);
			}
			mFieldNames[i] = fieldName.toString();

			mShapeFieldTypes[i] = dbfField[10];
			mShapeFieldLengths[i] = dbfField[15];

			/*
			 * Convert shape field type to our representation of field types.
			 */
			switch (mShapeFieldTypes[i])
			{
				case 'C': /* character */
				case 'D': /* date */
					mFieldTypes[i] = Argument.STRING;
					break;
				case 'L': /* logical */
				case 'N': /* number */
				case 'F': /* floating point number */
					mFieldTypes[i] = Argument.NUMERIC;
					break;
			}
		}

		/*
		 * Leave DBF file at position of first record.
		 */
		int skipBytes = headerLength - nFields * dbfField.length;
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
			for (int i = 0; i < mFieldTypes.length + 1; i++)
			{
				mRow.add(new Argument(0.0));
			}
		}
		else
		{
			/*
			 * Shape file does not overlap current extents.  Query returns nothing.
			 */
			mBytesRead = mShapeFileLength;
		}
		mRowAvailable = false;
	}

	/**
	 * Read next shape from shapefile that is inside or crossing the query extents.
	 * @return true if a row was read.
	 */
	private boolean readNextRow() throws MapyrusException
	{
		int recordNumber;
		int recordLength;
		double x, y, xMin, yMin, xMax, yMax;
		int i, type;
		boolean foundShape = false;

		try
		{
			/*
			 * Keep reading until we get a shape inside the extents or we reach
			 * the end of the file.
			 */
			while (!foundShape && mBytesRead < mShapeFileLength)
			{
				/*
				 * Read header for next shape.
				 */
				recordNumber = readLittleEndianInt(mShapeStream);
				recordLength = readLittleEndianInt(mShapeStream);

				if (mShapeFileType == POINT)
				{
					/*
					 * Read point coordinates, see if they are inside
					 * query extents.
					 */
					mPath[0] = 4;
					mPath[1] = PathIterator.SEG_MOVETO;
					type = readLittleEndianInt(mShapeStream);
					mPath[2] = readLittleEndianDouble(mShapeStream);
					mPath[3] = readLittleEndianDouble(mShapeStream);
					for (i = 16; i < recordLength; i++)
						mShapeStream.read();
					foundShape = mQueryExtents.contains(mPath[2], mPath[3]);
				}
				else if (mShapeFileType == POLYLINE)
				{
					/*
					 * Read bounding box of polyline.  Find if it intersects
					 * with query extents.
					 */
					type = readLittleEndianInt(mShapeStream);
					xMin = readLittleEndianDouble(mShapeStream);
					yMin = readLittleEndianDouble(mShapeStream);
					xMax = readLittleEndianDouble(mShapeStream);
					yMax = readLittleEndianDouble(mShapeStream);
					foundShape = mQueryExtents.intersects(xMin, yMin, xMax - xMin, yMax - yMin);
					if (foundShape)
					{
					}
					else
					{
						mShapeStream.skip(recordLength);
					}
				}
				mBytesRead += recordLength;

				/*
				 * Read attribute fields for this shape.  Don't bother
				 * unpacking them if we are skipping this shape.
				 */
				mDBFStream.read(mDBFRecord);
				if (foundShape)
				{
					XXX
					for (i = 0; i < 1; i++)
					{
					}
				}
			}

			if (foundShape)
			{
				Argument geometry = (Argument)mRow.elementAt(0);
				geometry.setGeometryValue(mPath);
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		return (foundShape);
	}

	/**
	 * Finds if current query has more data available. 
	 * @return true if query has more rows available to be fetched.
	 */
	public boolean hasMoreRows() throws MapyrusException
	{
		if (!mRowAvailable)
			mRowAvailable = readNextRow();
		return(mRowAvailable);
	}

	/**
	 * Fetch next row that is a result of current query.
	 * @return row returns next row result.
	 */
	public Row fetch() throws MapyrusException
	{
		/*
		 * Fetch next row if it has not been read already.
		 */
		if (!mRowAvailable)
			mRowAvailable = readNextRow();

		if (!mRowAvailable)
			throw new MapyrusException("No more rows to fetch from '" + mFilename + ".shp'");

		/*
		 * Return current row, this row is no longer available.
		 */
		mRowAvailable = false;
		return(mRow);
	}
}
