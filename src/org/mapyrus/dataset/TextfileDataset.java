/**
 * Implements reading of geographic datasets from a delimited text file
 * with one geometry plus its attributes per line.
 * Suitable for reading comma separated file or other simplistic file formats.
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

public class TextfileDataset implements GeographicDataset
{
	/*
	 * File we are reading from.
	 */
	private LineNumberReader mReader;
	private String mFilename;
	
	/*
	 * Names of fields and their types, read from header at start of file.
	 */
	private String mFieldNames[];
	private int mFieldTypes[];
	
	/*
	 * Indices of fields making up geometry.  For example, Lon, Lat values
	 * or X1, Y1, X2, Y2 values.
	 */
	private int mGeometryFieldIndexes[];

	/*
	 * Field separators.  Normally a comma or tab.
	 */
	private String mDelimeters;

	/*
	 * String that denotes comment lines in text file.  These lines
	 * are ignored.
	 */
	private String mComment;

	/*
	 * Number of lines to skip at start of file.
	 */
	private int mSkipLines;

	/*
	 * Area being queried.
	 */
	private Rectangle2D.Double mQueryExtents;

	/*
	 * Flag true when a row has already been read and is available to be fetched.
	 */
	private boolean mRowAvailable;

	/*
	 * Row of fields returned from each fetch.
	 */
	private Row mRow;

	/*
	 * Static field type lookup table for easy lookup.
	 */
	private static Hashtable mFieldTypeLookup;
	
	static
	{
		mFieldTypeLookup = new Hashtable();
		mFieldTypeLookup.put("number", new Integer(Row.NUMBER));
		mFieldTypeLookup.put("numeric", new Integer(Row.NUMBER));
	}

	/*
	 * Read next line from file, skipping comment lines.
	 */
	private String readLine() throws IOException
	{
		String s;

		do
		{
			s = mReader.readLine();
		}
		while (s != null && s.startsWith(mComment));

		return(s);
	}

	public TextfileDataset(String filename, String extras, String []geometryFieldNames)
		throws FileNotFoundException, IOException, MapyrusException
	{
		String header, fieldType;
		StringTokenizer st;
		Vector v;
		int i, j;
		Integer fType;
		String token;
		boolean foundGeometryField;
		String fieldNames = null, fieldTypes = null;
		String startLine;
		String nextLine;

		/*
		 * Check if we should start a program and read its output instead
		 * of just reading from a file.
		 */
		if (filename.endsWith("|"))
		{
		}
		mReader = new LineNumberReader(new BufferedReader(new FileReader(filename)));
		mFilename = filename;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		mDelimeters = new String(",");
		mComment = "#";
		startLine = null;
		mRow = new Row();

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.startsWith("comment="))
				mComment = token.substring(8);
			else if (token.startsWith("delimiters="))
				mDelimeters = token.substring(11);
			else if (token.startsWith("fieldnames="))
				fieldNames = token;
			else if (token.startsWith("fieldtypes="))
				fieldTypes = token;
			else if (token.startsWith("startline="))
				startLine = token;
		}
		
		/*
		 * If field names and types not given then read them from first two lines
		 * of file.  Then skip more lines until we get to start of data.
		 */
		mSkipLines = 0;
		if (fieldNames == null)
		{
			fieldNames = readLine();
			if (fieldNames == null)
				throw new MapyrusException("Unexpected end of file in '" + filename + "'");
			mSkipLines++;
		}
		if (fieldTypes == null)
		{
			fieldTypes = readLine();
			if (fieldNames == null)
				throw new MapyrusException("Unexpected end of file in '" + filename + "'");
			mSkipLines++;
		}

		/*
		 * Skip lines until we find the place where the real data starts.
		 */
		if (startLine != null)
		{
			do
			{
				nextLine = readLine();
				mSkipLines++;
			}
			while (nextLine != null && (!nextLine.startsWith(startLine)));
		}

		st = new StringTokenizer(fieldNames, mDelimeters);
		v = new Vector();
		while (st.hasMoreTokens())
		{
			v.add(st.nextToken());
		}
		mFieldNames = (String [])(v.toArray());
		
		mFieldTypes = new int[mFieldNames.length];
		st = new StringTokenizer(fieldNames, mDelimeters);
		i = 0;
		while (st.hasMoreTokens() && i < mFieldTypes.length)
		{
			fieldType = st.nextToken();
			fieldType = fieldType.toLowerCase();
			fType = (Integer)mFieldTypeLookup.get(fieldType);
			if (fType == null)
				mFieldTypes[i] = Row.STRING;
			else
				mFieldTypes[i] = fType.intValue();

			i++;
		}

		/*
		 * Make sure name and type given for each field.
		 */		
		if (i != mFieldTypes.length || st.hasMoreTokens())
		{
			throw new MapyrusException("Different number of field " +
				"names and field types in '" + filename + "'");
		}

		/*
		 * Find indexes of fields caller says are the geometry for each row.
		 */
		v.clear();
		for (i = 0; i < geometryFieldNames.length; i++)
		{
			foundGeometryField = false;
			for (j = 0; j < mFieldNames.length; j++)
			{
				if (geometryFieldNames[i].equalsIgnoreCase(mFieldNames[j]))
				{
					if (mFieldTypes[j] == Row.STRING)
					{
						throw new MapyrusException("Field '" + mFieldNames[j] +
							"' is wrong type for geometry in file '" +
							filename + "'");
					}
					v.add(new Integer(j));
				}
			}
			
			if (foundGeometryField == false)
			{
				throw new MapyrusException("Geometry field '" +
					mFieldNames[j] + "' not found in file '" + filename + "'");
			}
		}

		/*
		 * Save array of fields combined to make geometry for each row.
		 */
		mGeometryFieldIndexes = new int[v.size()];
		for (i = 0; i < v.size(); i++)
			mGeometryFieldIndexes[i] = ((Integer)(v.elementAt(i))).intValue();
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#getProjection()
	 */
	public String getProjection()
	{
		return("undef");
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#getMetadata()
	 */
	public Hashtable getMetadata()
	{
		return(new Hashtable());
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#getFieldNames()
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#getFieldTypes()
	 */
	public int[] getFieldTypes()
	{
		return(mFieldTypes);
	}

	/**
	 * Return indexes of geometry fields in list of field names.
	 * @return list of field indexes.
	 */
	public int[] getGeometryFieldIndexes()
	{
		return(mGeometryFieldIndexes);
	}
	
	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#getWorlds()
	 */
	public Rectangle2D getWorlds()
	{
		return null;
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#query(String[], Double, String)
	 */
	public void query(Rectangle2D.Double extents, String whereClause)
		throws MapyrusException
	{
		mQueryExtents = extents;

		/*
		 * Rewind file for new query.
		 */		
		try
		{
			mReader.reset();
			for (int i = 0; i < mSkipLines; i++)
				readLine();
			mRowAvailable = false;
		}
		catch (IOException e)
		{
			throw new MapyrusException("Query of from file '" + mFilename + "' failed: " +
				e.getMessage());
		}
	}

	/**
	 * Read next row from file and split it into fields.  Build fields into Row structure.
	 */
	private boolean readNextRow() throws MapyrusException
	{
		int i, geometryFieldIndex;
		StringTokenizer st;
		String message = null;
		String fieldValue, nextLine;

		/*
		 * Need next line from file.
		 */
		try
		{
			nextLine = readLine();
		}
		catch (IOException e)
		{
			throw new MapyrusException("Error reading file '" + mFilename +
				"': " +e.getMessage());
		}
		
		/*
		 * Return EOF status if no more lines available in file.
		 */
		if (nextLine == null)
			return(false);

		/*
		 * Split line into fields and build a row to be returned.
		 */
		i = geometryFieldIndex = 0;
		mRow.clear();
		st = new StringTokenizer(nextLine, mDelimeters);
		while (st.hasMoreTokens() && i < mFieldNames.length)
		{
			fieldValue = st.nextToken();

			if (mFieldTypes[i] == Row.NUMBER)
			{
				try
				{
					mRow.add(new Argument(Double.parseDouble(fieldValue)));
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException("Invalid numeric value '" +
						fieldValue + "' at line " +
						mReader.getLineNumber() + " in file '" +
						mFilename + "'");
				}
			}
			else
			{
				mRow.add(new Argument(Argument.STRING, fieldValue));
			}
			i++;
		}
		
		return(true);
	}

	/**
	 * Gets next row from file that contains geometry crossing the query extents.
	 * @return true if a row was found that crosses the area of interest.
	 */
	private boolean readMatchingRow() throws MapyrusException
	{
		boolean retval;
		boolean rowOutside;
		int i;
		Argument x, y;
		boolean insideExtents;
		int outcode, allOutcodes;

		do
		{
			if (mRowAvailable == false)
				mRowAvailable = readMatchingRow();
			if (mRowAvailable)
			{
				/*
				 * Does geometry crosses query extents?
				 */
				insideExtents = false;
				allOutcodes = 0;
				for (i = 0; i < mGeometryFieldIndexes.length && (!insideExtents); i += 2)
				{
					x = (Argument)(mRow.elementAt(i));
					y = (Argument)(mRow.elementAt(i + 1));
					outcode = mQueryExtents.outcode(x.getNumericValue(),
						y.getNumericValue());
					if (outcode == 0)
						insideExtents = true;
					else
						allOutcodes |=  outcode;
				}
				
				/*
				 * If points in geometry lie both above and below
				 */
				if (allOutcodes & (Rectangle2D.OUT_BOTTOM & Rectangle2D.OUT_TOP)
					insideExtents = true; XXX
			}
		}
		while (mRowAvailable && (!insideExtents));
		
		return(insideExtents);
	}
	
	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#hasMoreRows(Object)
	 */
	public boolean hasMoreRows() throws MapyrusException
	{
		if (mRowAvailable == false)
			mRowAvailable = readMatchingRow();
		return(mRowAvailable);
	}

	/**
	 * @see net.sourceforge.mapyrus.GeographicDataset#fetch(Object, Row)
	 */
	public Row fetch() throws MapyrusException
	{
		boolean status;
		
		if (mRowAvailable == false)
			mRowAvailable = readMatchingRow();

		if (mRowAvailable == false)
			throw new MapyrusException("No more rows to fetch from '" + mFilename + "'");

		return(mRow);
	}
}
