/*
 * $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Implements reading of geographic datasets from a delimited text file
 * with one geometry plus its attributes per line.
 * Suitable for reading comma separated file or other simplistic file formats.
 */
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
	 * Field separators.  Normally a comma or keyword 'whitespace' (meaning anything
	 * blank).
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
		mFieldTypeLookup.put("number", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("numeric", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("int", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("integer", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("real", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("double", new Integer(Argument.NUMERIC));
		mFieldTypeLookup.put("float", new Integer(Argument.NUMERIC));
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

	/**
	 * Create string tokenizer to split line using delimiters set for this file.
	 * @param str is string to be split into tokens.
	 */
	private StringTokenizer createStringTokenizer(String str)
	{
		StringTokenizer retval;
		
		if (mDelimeters == null)
			retval = new StringTokenizer(str);
		else
			retval = new StringTokenizer(str, mDelimeters);
		return(retval);
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
		mDelimeters = null;
		mComment = "#";
		startLine = null;

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

		st = createStringTokenizer(fieldNames);
		v = new Vector();
		while (st.hasMoreTokens())
		{
			v.add(st.nextToken());
		}
		mFieldNames = (String [])(v.toArray());
		
		mFieldTypes = new int[mFieldNames.length];
		st = createStringTokenizer(fieldTypes);
		i = 0;
		while (st.hasMoreTokens() && i < mFieldTypes.length)
		{
			fieldType = st.nextToken();
			fieldType = fieldType.toLowerCase();
			fType = (Integer)mFieldTypeLookup.get(fieldType);
			if (fType == null)
				mFieldTypes[i] = Argument.STRING;
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
					if (mFieldTypes[j] == Argument.STRING)
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
	 *
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

			/*
			 * Create row for returning results of this query.
			 */
			mRow = new Row();
			for (int i = 0; i < mFieldNames.length; i++)
				mRow.add(new Argument(0.0));
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
		Argument field;

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
		st = createStringTokenizer(nextLine);
		while (st.hasMoreTokens() && i < mFieldNames.length)
		{
			fieldValue = st.nextToken();

			field = (Argument)(mRow.elementAt(i));
			if (mFieldTypes[i] == Argument.NUMERIC)
			{
				try
				{
					field.setNumericValue(Double.parseDouble(fieldValue));
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
				field.setStringValue(fieldValue);
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
		boolean left, right, bottom, top, inX, inY;
		boolean insideExtents = false;
		int outcode;
		boolean nextRowAvailable;

		do
		{
			nextRowAvailable = readNextRow();
			if (nextRowAvailable)
			{
				/*
				 * Walk through points in geometry and see whether
				 * they cross the area we are querying.
				 */
				left = right = bottom = top = inX = inY = false;
				for (i = 0; i < mGeometryFieldIndexes.length; i += 2)
				{
					x = (Argument)(mRow.elementAt(i));
					y = (Argument)(mRow.elementAt(i + 1));
					outcode = mQueryExtents.outcode(x.getNumericValue(),
						y.getNumericValue());
						
					if ((outcode & Rectangle2D.OUT_LEFT) != 0)
						left = true;
					else if ((outcode & Rectangle2D.OUT_RIGHT) != 0)
						right = true;
					else
						inX = true;
						
					if ((outcode | Rectangle2D.OUT_BOTTOM) != 0)
						bottom = true;
					else if ((outcode | Rectangle2D.OUT_TOP) != 0)
						top = true;
					else
						inY = true;					
				}
				
				/*
				 * Geometry inside query extents if there is a point inside extents
				 * (or a point both sides of the extents) in both X and Y axes.
				 */
				insideExtents = (inX || (left && right)) && (inY || (bottom && top));
			}
		}
		while (nextRowAvailable && (!insideExtents));

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

		/*
		 * Fetch next row if it has not been read already.
		 */		
		if (mRowAvailable == false)
			mRowAvailable = readMatchingRow();

		if (mRowAvailable == false)
			throw new MapyrusException("No more rows to fetch from '" + mFilename + "'");

		return(mRow);
	}
}
