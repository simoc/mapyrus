/*
 * $Id$
 */
package au.id.chenery.mapyrus.dataset;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.ArrayList;
import au.id.chenery.mapyrus.*;

/**
 * Implements reading of geographic datasets from a delimited text file
 * with one geometry plus its attributes per line.
 * Suitable for reading comma separated file or other simplistic file formats.
 */
public class TextfileDataset implements GeographicDataset
{
	/*
	 * File we are reading from.
	 * Process handle to external process we are reading from.
	 */
	private LineNumberReader mReader;
	private String mFilename;
	private Process mProcess;
	
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
	private String mDelimiters;

	/*
	 * String that denotes comment lines in text file.  These lines
	 * are ignored.
	 */
	private String mComment;

	/*
	 * Area being queried.
	 */
	private Rectangle2D.Double mQueryExtents;

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
	 * @param delimiters are field delimiters, if null then whitespace is used.
	 */
	private StringTokenizer createStringTokenizer(String str, String delimiters)
	{
		StringTokenizer retval;
		
		if (delimiters == null)
			retval = new StringTokenizer(str);
		else
			retval = new StringTokenizer(str, delimiters);
		return(retval);
	}

	/**
	 * Open text file containing geographic data for querying.
	 * @param filename name of text file to open.
	 * @param extras options specific to text file datasets, given as var=value pairs.
	 * @param geometryFieldNames comma separated list of names of fields containing geometry.
	 */	
	public TextfileDataset(String filename, String extras, String []geometryFieldNames)
		throws FileNotFoundException, IOException, MapyrusException
	{
		String header, fieldType;
		StringTokenizer st;
		ArrayList list;
		int i, j;
		Integer fType;
		String token;
		boolean foundGeometryField;
		String fieldNames = null, fieldTypes = null;
		String fieldNameDelimiters = null, fieldTypeDelimiters = null;
		String nextLine;
		BufferedReader bufferedReader;

		/*
		 * Check if we should start a program and read its output instead
		 * of just reading from a file.
		 */
		if (filename.endsWith("|"))
		{
			String command = filename.substring(0, filename.length() - 1).trim();
			mProcess = Runtime.getRuntime().exec(command);
			bufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
		}
		else
		{
			bufferedReader = new BufferedReader(new FileReader(filename));
		}
		mReader = new LineNumberReader(bufferedReader);
		mFilename = filename;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		mDelimiters = null;
		mComment = "#";

		st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			token = st.nextToken();
			if (token.startsWith("comment="))
				mComment = token.substring(8);
			else if (token.startsWith("delimiters="))
				mDelimiters = token.substring(11);
			else if (token.startsWith("fieldnames="))
				fieldNames = token.substring(11);
			else if (token.startsWith("fieldtypes="))
				fieldTypes = token.substring(11);
		}
		
		/*
		 * If field names and types not given then read them from first two lines
		 * of file.  Then skip more lines until we get to start of data.
		 */
		if (fieldNames != null)
		{
			fieldNameDelimiters = ",";
		}
		else
		{
			fieldNames = readLine();
			fieldNameDelimiters = mDelimiters;
			if (fieldNames == null)
				throw new MapyrusException("Unexpected end of file in '" + filename + "'");
		}

		if (fieldTypes != null)
		{
			fieldTypeDelimiters = ",";
		}
		else
		{
			fieldTypes = readLine();
			fieldTypeDelimiters = mDelimiters;
			if (fieldNames == null)
				throw new MapyrusException("Unexpected end of file in '" + filename + "'");
		}

		st = createStringTokenizer(fieldNames, fieldNameDelimiters);
		list = new ArrayList();
		while (st.hasMoreTokens())
		{
			list.add((String)st.nextToken());
		}
		mFieldNames = new String[list.size()];
		for (i = 0; i < mFieldNames.length; i++)
			mFieldNames[i] = (String)list.get(i);
		
		mFieldTypes = new int[mFieldNames.length];
		st = createStringTokenizer(fieldTypes, fieldTypeDelimiters);
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
		 * Caller must give us field names containing geometry.
		 * We cannot possibly work it out for ourselves here.
		 */
		if (geometryFieldNames.length < 2)
		{
			throw new MapyrusException("Names of fields in dataset containing geometry required");
		}

		/*
		 * Find indexes of fields caller says are the geometry for each row.
		 */
		list.clear();
		for (i = 0; i < geometryFieldNames.length; i++)
		{
			foundGeometryField = false;
			for (j = 0; j < mFieldNames.length && foundGeometryField == false; j++)
			{
				if (geometryFieldNames[i].equalsIgnoreCase(mFieldNames[j]))
				{
					if (mFieldTypes[j] == Argument.STRING)
					{
						throw new MapyrusException("Field '" + mFieldNames[j] +
							"' is wrong type for geometry in file '" +
							filename + "'");
					}
					list.add(new Integer(j));
					foundGeometryField = true;
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
		mGeometryFieldIndexes = new int[list.size()];
		for (i = 0; i < list.size(); i++)
			mGeometryFieldIndexes[i] = ((Integer)(list.get(i))).intValue();
	}

	/**
	 * Returns projection of dataset, which is not defined for a text file.
	 * @return string "undef".
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
	 * Return names of fields in this text file.
	 * @return fieldnames.
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * Return types of fields in this text file.
	 * @return field types.
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
	 * Return extents in text file.  These are not known until the whole
	 * file is scanned.
	 * @return degree values covering the whole world
	 */
	public Rectangle2D.Double getWorlds()
	{
		return new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0);
	}

	/**
	 * Begins a query on a text file dataset.
	 * @param extents the area of interest for the query.  Geometry outside
	 * these extents will be skipped.
	 * @param resolution is hint for minimum distance between coordinate values.
	 */
	public void query(Rectangle2D.Double extents, double resolution)
		throws MapyrusException
	{
		mQueryExtents = extents;
	}

	/**
	 * Read next row from file and split it into fields.  Build fields into Row structure.
	 */
	private boolean readNextRow(Row row) throws MapyrusException
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
		row.clear();
		i = geometryFieldIndex = 0;
		st = createStringTokenizer(nextLine, mDelimiters);
		while (st.hasMoreTokens() && i < mFieldNames.length)
		{
			fieldValue = st.nextToken();

			if (mFieldTypes[i] == Argument.NUMERIC)
			{
				try
				{
					field = new Argument(Double.parseDouble(fieldValue));
				}
				catch (NumberFormatException e)
				{
					throw new MapyrusException("Invalid numeric value '" +
						fieldValue + "' in file " + mFilename + " line " +
						mReader.getLineNumber());
				}
			}
			else
			{
				field = new Argument(Argument.STRING, fieldValue);
			}
			row.add(field);
			i++;
		}
		
		/*
		 * Make sure we read the correct number of fields.
		 */
		if (i != mFieldNames.length)
		{
			throw new MapyrusException("Missing fields in file " +
				mFilename + " line " + mReader.getLineNumber());
		}
		return(true);
	}

	/**
	 * Gets next row from file that contains geometry crossing the query extents.
	 * @return next row read, or null if no row found.
	 */
	public Row fetch() throws MapyrusException
	{
		boolean retval;
		boolean rowOutside;
		int i;
		Argument x, y;
		boolean left, right, bottom, top, inX, inY;
		boolean insideExtents = false;
		int outcode;
		boolean nextRowAvailable;
		Row row = new Row();

		do
		{
			nextRowAvailable = readNextRow(row);
			if (nextRowAvailable)
			{
				/*
				 * Walk through points in geometry and see whether
				 * they cross the area we are querying.
				 */
				left = right = bottom = top = inX = inY = false;
				for (i = 0; i < mGeometryFieldIndexes.length; i += 2)
				{
					x = (Argument)(row.get(i));
					y = (Argument)(row.get(i + 1));
					outcode = mQueryExtents.outcode(x.getNumericValue(),
						y.getNumericValue());
						
					if ((outcode & Rectangle2D.OUT_LEFT) != 0)
						left = true;
					else if ((outcode & Rectangle2D.OUT_RIGHT) != 0)
						right = true;
					else
						inX = true;
						
					if ((outcode & Rectangle2D.OUT_BOTTOM) != 0)
						bottom = true;
					else if ((outcode & Rectangle2D.OUT_TOP) != 0)
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

		if (insideExtents)
			return(row);
		else
		{
			/*
			 * We've read all of external program's output, now wait for
			 * it to terminate.
			 */
			if (mProcess != null)
			{
				try
				{
					mProcess.waitFor();
				}
				catch (InterruptedException e)
				{
					throw new MapyrusException(e.getMessage());
				}
			}
			
			return(null);
		}
	}
}
