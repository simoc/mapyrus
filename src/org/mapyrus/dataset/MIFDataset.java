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
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Row;

/**
 * Implements reading from MapInfo Interchange Format (MIF).
 * This format stores geometry in a file with .MIF suffix
 * and attributes in a file with .MID suffix.
 * Format available in internet as 'mif_mid.txt'.
 */
public class MIFDataset implements GeographicDataset
{
	/*
	 * Attribute fields to return giving color and symbol information read
	 * from MIF file.
	 */
	private static final String[] GEOMETRY_FIELDS =
		new String[]{"GEOMETRY", "PENWIDTH", "PENPATTERN", "PENCOLOR",
			"BRUSHPATTERN", "BRUSHFORECOLOR", "BRUSHBACKCOLOR",
			"SYMBOLSHAPE", "SYMBOLCOLOR", "SYMBOLSIZE", "SYMBOLFONT", "SYMBOLSTYLE"};

	/*
	 * Default color, size  and font for geometry, if not defined in MIF file.
	 */
	private static final Argument DEFAULT_COLOR = new Argument(Argument.STRING, "0x000000");
	private static final Argument DEFAULT_SIZE = new Argument(4.0);
	private static final Argument DEFAULT_FONT = new Argument(Argument.STRING, "MapInfo Symbols");

	private LineNumberReader mMIFFile;
	private MIDReader mMIDFile;
	private String mFilename;
	
	/*
	 * Next line to read from MIF file.
	 */
	private String mMIFNextLine;

	private char mDelimiter;
	private String mProjection;
	private String []mFieldNames;
	private int []mMIDFieldTypes;

	/*
	 * Attributes to fetch from MID file.
	 */
	private boolean []mMIDFieldsToFetch;
	private int mNMIDFieldsToFetch;

	private class MIDReader
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

		public MIDReader(String filename, char delimiter) throws IOException
		{		
			mFile = new BufferedInputStream(new FileInputStream(filename));
			mFilename = filename;
			mFieldDelimiter = (byte)delimiter;
			mFieldBuffer = new StringBuffer();
			mRowCounter = 1;
		}

		/**
		 * Read next row from MID file.
		 * @param row row to add fields to.
		 * @return row of fields.
		 * @throws IOException
		 * @throws MapyrusException
		 */
		public Row getRow(Row row)
			throws IOException, MapyrusException
		{
			Argument field;

			for (int i = 0; i < mMIDFieldTypes.length; i++)
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
						if (i != mMIDFieldTypes.length - 1)
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

				if (mMIDFieldsToFetch[i])
				{
					/*
					 * Convert field to Mapyrus Argument object, depending on the
					 * field type.
					 * Use constants for frequently occurring field values.
					 */
					String fieldValue = mFieldBuffer.toString();
					if (mMIDFieldTypes[i] == NUMERIC_FIELD)
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
					else if (mMIDFieldTypes[i] == LOGICAL_FIELD)
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

	/**
	 * Parse MIF file header.
	 */
	private void parseMIFHeader(HashSet midFieldsToFetch) throws IOException
	{
		String line, lowercaseLine;
		boolean finishedHeader = false;
		int nMIDColumns = 0;
		int counter = 0;
		boolean inColumnDefinition = false;
		ArrayList fieldNameList = new ArrayList();

		mFieldNames = GEOMETRY_FIELDS;
		mNMIDFieldsToFetch = 0;
		mDelimiter = '\t';

		/*
		 * First line of MIF must contain keyword 'version'. 
		 */
		line = mMIFFile.readLine();
		if (line == null || (!line.toLowerCase().startsWith("version")))
		{
			throw new EOFException(MapyrusMessages.get(MapyrusMessages.NOT_MIF_FILE) +
				": " + mFilename);
		}

		/*
		 * Parse useful information about attribute fields from the MIF file header.
		 */
		while (line != null && !finishedHeader)
		{
			lowercaseLine = line.toLowerCase();
			if (inColumnDefinition)
			{
				StringTokenizer st = new StringTokenizer(line, " (");
				if (st.countTokens() >= 2)
				{
					String fieldName = st.nextToken();

					if (midFieldsToFetch == null)
					{
						/*
						 * No fields set for fetching, so fetch everything.
						 */
						mMIDFieldsToFetch[counter] = true;
					}
					else
					{
						/*
						 * Fetch only fields user asked for.
						 */
						mMIDFieldsToFetch[counter] = midFieldsToFetch.contains(fieldName) ||
							midFieldsToFetch.contains(fieldName.toLowerCase()) ||
							midFieldsToFetch.contains(fieldName.toUpperCase());
					}

					if (mMIDFieldsToFetch[counter])
					{
						fieldNameList.add(fieldName);
						mNMIDFieldsToFetch++;
					}

					/*
					 * Store data type of each field.
					 */
					String fieldType = st.nextToken().toLowerCase();
					if (fieldType.equals("integer") || fieldType.equals("decimal") ||
						fieldType.equals("smallint") || fieldType.equals("float"))
					{
						mMIDFieldTypes[counter] = MIDReader.NUMERIC_FIELD;
					}
					else if (fieldType.equals("logical"))
					{
						mMIDFieldTypes[counter] = MIDReader.LOGICAL_FIELD;
					}
					else
					{
						mMIDFieldTypes[counter] = MIDReader.STRING_FIELD;
					}
				}

				counter++;
				if (counter == nMIDColumns)
					inColumnDefinition = false;
			}
			else if (lowercaseLine.startsWith("delimiter"))
			{
				int quoteIndex = lowercaseLine.indexOf('"');
				if (quoteIndex > 0)
					mDelimiter = line.charAt(quoteIndex + 1);
			}
			else if (lowercaseLine.startsWith("coordsys"))
			{
				mProjection = line.substring(8).trim();
			}
			else if (lowercaseLine.startsWith("columns"))
			{
				nMIDColumns = Integer.parseInt(line.substring(7).trim());
				mMIDFieldTypes  = new int[nMIDColumns];
				mMIDFieldsToFetch = new boolean[nMIDColumns];

				if (nMIDColumns > 0)
					inColumnDefinition = true;
			}


			if (lowercaseLine.equals("data"))
				finishedHeader = true;
			else
				line = mMIFFile.readLine();
		}

		/*
		 * Build list of fields returned by this dataset.
		 */
		mFieldNames = new String[GEOMETRY_FIELDS.length + fieldNameList.size()];
		System.arraycopy(GEOMETRY_FIELDS, 0, mFieldNames, 0, GEOMETRY_FIELDS.length);
		for (int i = 0; i < fieldNameList.size(); i++)
			mFieldNames[GEOMETRY_FIELDS.length + i] = (String)fieldNameList.get(i);

		/*
		 * Check that header was read correctly.
		 */
		if ((!finishedHeader) || inColumnDefinition)
		{
			throw new EOFException(MapyrusMessages.get(MapyrusMessages.NOT_MIF_FILE) +
				": " + mFilename);
		}

		mMIFNextLine = mMIFFile.readLine();
	}

	/**
	 * Open MapInfo import files containing geographic data for querying.
	 * @param filename name of MapInfo file to open, with or without .mif suffix.
	 * @param extras options specific to MapInfo datasets, given as var=value pairs.
	 */	
	public MIFDataset(String filename, String extras)
		throws FileNotFoundException, IOException, MapyrusException
	{
		HashSet extrasMIDFields;

		/*
		 * Set default options.  Then see if user wants to override any of them.
		 */
		extrasMIDFields = null;

		StringTokenizer st = new StringTokenizer(extras);
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (token.startsWith("midfields="))
			{
				extrasMIDFields = new HashSet();
				StringTokenizer st2 = new StringTokenizer(token.substring(10), ",");
				while (st2.hasMoreTokens())
				{
					token = st2.nextToken();
					extrasMIDFields.add(token);
				}
			}
		}

		mFilename = filename;
		mMIFFile = new LineNumberReader(new FileReader(filename));
		parseMIFHeader(extrasMIDFields);

		/*
		 * If file with .mid extension exists then open that file too.
		 */
		String midFilename;
		if (filename.endsWith(".mif"))
			midFilename = filename.substring(0, filename.length() - 4) + ".mid";
		else if (filename.endsWith(".MIF"))
			midFilename = filename.substring(0, filename.length() - 4) + ".MID";
		else
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_MIF_FILE) +
				": " + filename);
		}

		/*
		 * If .MID file exists and we need to read some attribute
		 * columns then open it.
		 */
		File f = new File(midFilename);
		if (f.canRead() && mNMIDFieldsToFetch > 0)
		{
			mMIDFile = new MIDReader(midFilename, mDelimiter);
		}
		else
		{
			/*
			 * No attributes available, only geometry.
			 */
			mMIDFile = null;
			mFieldNames = GEOMETRY_FIELDS;
		}
	}

	/**
	 * Return projection of MIF file.
	 * @return projection string.
	 */
	public String getProjection()
	{
		return(mProjection);
	}

	/**
	 * Get metadata about MIF file.
	 * @return metadata.
	 */
	public Hashtable getMetadata()
	{
		return(new Hashtable());
	}

	/**
	 * Return list of attribute field names.
	 * @return field name list.
	 */
	public String[] getFieldNames()
	{
		return(mFieldNames);
	}

	/**
	 * Return extents of text file.  We do not know this.
	 * @return degree values covering the whole world
	 */
	public Rectangle2D.Double getWorlds()
	{
		return new Rectangle2D.Double(-180.0, -90.0, 180.0, 90.0);
	}

	/**
	 * Convert integer value to color hex string.
	 * For example, 255 is converted to 0x0000ff.
	 * @param str color as integer value.
	 * @return color as hex string.
	 */
	private String intToColor(String str)
	{
		String hex;
		StringBuffer retval = new StringBuffer("0x");
		int color = Integer.parseInt(str);
		int r = ((color >> 16) & 0xff);
		int g = ((color >> 8) & 0xff);
		int b = (color & 0xff);

		hex = Integer.toHexString(r);
		if (r < 10)
			retval.append('0');
		retval.append(hex);

		hex = Integer.toHexString(g);
		if (g < 10)
			retval.append('0');
		retval.append(hex);

		hex = Integer.toHexString(b);
		if (b < 10)
			retval.append('0');
		retval.append(hex);

		return(retval.toString());
	}

	/**
	 * Parse (X, Y) coordinate value from string.
	 * @param line string to parse from.
	 * @param geometry geometry array to add coordinate to.
	 * @param offset offset in geometry array to add X and Y values to.
	 */
	private void parseXYCoordinate(String line, double []geometry, int offset)
		throws MapyrusException
	{
		int spaceIndex = line.indexOf(' ');
		if (spaceIndex < 0)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COORDINATE) +
				": " + mFilename + ":" + mMIFFile.getLineNumber());
		}

		/*
		 * Parse (X, Y) coordinate value and add it to geometry.
		 */
		double x = Double.parseDouble(line.substring(0, spaceIndex));
		do
		{
			spaceIndex++;
		}
		while (line.charAt(spaceIndex) == ' ');

		double y = Double.parseDouble(line.substring(spaceIndex));

		geometry[offset] = x;
		geometry[offset + 1] = y;
	}

	/**
	 * Parse list of (X, Y) coordinates from MIF file,
	 * with one pair of coordinates on each line.
	 * @param nPoints number of points to parse.
	 * @param geometryType type of geometry being parsed.
	 * @return geometry in Mapyrus geometry format.
	 */
	private double []parseCoordinates(int nPoints, int geometryType)
		throws IOException, MapyrusException
	{
		double []geometry;

		geometry = new double[nPoints * 3 + 2];
		geometry[0] = geometryType;
		geometry[1] = nPoints;
		int geometryIndex = 2;
		int op = Argument.MOVETO;

		/*
		 * Read an (X, Y) coordinate pair from each line.
		 */
		for (int i = 0; i < nPoints; i++)
		{
			String line = mMIFFile.readLine();
			if (line == null)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + mFilename);
			}

			geometry[geometryIndex] = op;
			parseXYCoordinate(line, geometry, geometryIndex + 1);
			geometryIndex += 3;
			op = Argument.LINETO;
		}
		return(geometry);
	}

	/**
	 * Parse simple or multi-geometry from MIF file.
	 * @param nSections number of geometries to read.
	 * @param geometryType type of each geometry being read.
	 */
	private double []parseCoordinateList(int nSections, int geometryType)
		throws IOException, MapyrusException
	{
		ArrayList sections = new ArrayList(nSections);
		int totalGeometryLength = 2;
		double []geometry = null;

		/*
		 * Parse coordinates for each sub-geometry.
		 */
		for (int i = 0; i < nSections; i++)
		{
			String line = mMIFFile.readLine();
			if (line == null)
			{
				throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + mFilename);
			}
			int nPoints = Integer.parseInt(line.trim());
			geometry = parseCoordinates(nPoints, geometryType);
			sections.add(geometry);
			totalGeometryLength += geometry.length;
		}

		double []retval;
		if (nSections == 1)
		{
			/*
			 * A single geometry can be returned directly.
			 */
			retval = geometry;
		}
		else
		{
			/*
			 * Join all geometries together into a MULTILINESTRING
			 * or MULTIPOLYGON.
			 */
			retval = new double[totalGeometryLength];
			if (geometryType == Argument.GEOMETRY_LINESTRING)
				retval[0] = Argument.GEOMETRY_MULTILINESTRING;
			else
				retval[0] = Argument.GEOMETRY_MULTIPOLYGON;

			retval[1] = nSections;
			int index = 2;
			Iterator it = sections.iterator();
			while (it.hasNext())
			{
				geometry = (double [])(it.next());
				System.arraycopy(geometry, 0, retval, index, geometry.length);
				index += geometry.length;
			}
		}
		return(retval);
	}

	/**
	 * Read next geometry from MIF file.
	 * @param row row to add geometry to.
	 * @return row with added geometry, or null if EOF reached.
	 * @throws IOException
	 * @throws MapyrusException
	 */
	private Row parseGeometry(Row row) throws IOException, MapyrusException
	{
		double []geometry = null;
		String penWidth, penPattern, penColor;
		String brushPattern, brushForeColor, brushBackColor;
		String symbolShape, symbolColor, symbolSize, symbolFont, symbolStyle;

		penWidth = penPattern = penColor = null;
		brushPattern = brushForeColor = brushBackColor = null;
		symbolShape = symbolColor = symbolSize = symbolFont = symbolStyle = null;

		/*
		 * Skip any blank lines in file.
		 * They should not be there but some exporters seem to
		 * add them.
		 */
		while (mMIFNextLine != null && mMIFNextLine.length() == 0)
		{
			mMIFNextLine = mMIFFile.readLine();
		}

		if (mMIFNextLine != null)
		{
			StringTokenizer st = new StringTokenizer(mMIFNextLine);
			if (st.hasMoreTokens())
			{
				String geometryType = st.nextToken().toLowerCase();
				if (geometryType.equals("point") && st.countTokens() == 2)
				{
					double x = Double.parseDouble(st.nextToken());
					double y = Double.parseDouble(st.nextToken());
					geometry = new double[5];
					geometry[0] = Argument.GEOMETRY_POINT;
					geometry[1] = 1;
					geometry[2] = Argument.MOVETO;
					geometry[3] = x;
					geometry[4] = y;
				}
				else if (geometryType.equals("line") && st.countTokens() == 4)
				{
					double x1 = Double.parseDouble(st.nextToken());
					double y1 = Double.parseDouble(st.nextToken());
					double x2 = Double.parseDouble(st.nextToken());
					double y2 = Double.parseDouble(st.nextToken());
					geometry = new double[8];
					geometry[0] = Argument.GEOMETRY_LINESTRING;
					geometry[1] = 2;
					geometry[2] = Argument.MOVETO;
					geometry[3] = x1;
					geometry[4] = y1;
					geometry[5] = Argument.LINETO;
					geometry[6] = x2;
					geometry[7] = y2;
				}
				else if (geometryType.equals("pline") && st.countTokens() >= 1)
				{
					String token = st.nextToken();
					if (token.equalsIgnoreCase("multiple"))
					{
						token = st.nextToken();
						int nSections = Integer.parseInt(token);
						geometry = parseCoordinateList(nSections, Argument.GEOMETRY_LINESTRING);
					}
					else
					{
						int nPoints = Integer.parseInt(token);
						geometry = parseCoordinates(nPoints, Argument.GEOMETRY_LINESTRING);
					}
				}
				else if (geometryType.equals("region") && st.countTokens() == 1)
				{
					String token = st.nextToken();
					int nRegions = Integer.parseInt(token);
					geometry = parseCoordinateList(nRegions, Argument.GEOMETRY_POLYGON);
				}
				else if ((geometryType.equals("arc") || geometryType.equals("rect") ||
					geometryType.equals("roundrect") || geometryType.equals("ellipse")) &&
					st.countTokens() == 4)
				{
					double x1 = Double.parseDouble(st.nextToken());
					double y1 = Double.parseDouble(st.nextToken());
					double x2 = Double.parseDouble(st.nextToken());
					double y2 = Double.parseDouble(st.nextToken());
					
					if (geometryType.equals("roundrect") || geometryType.equals("arc"))
					{
						String line = mMIFFile.readLine();
						if (line == null)
						{
							throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
								": " + mFilename);
						}
					}

					geometry = new double[17];
					geometry[0] = Argument.GEOMETRY_POLYGON;
					geometry[1] = 5;
					geometry[2] = Argument.MOVETO;
					geometry[3] = x1;
					geometry[4] = y1;
					geometry[5] = Argument.LINETO;
					geometry[6] = x1;
					geometry[7] = y2;
					geometry[8] = Argument.LINETO;
					geometry[9] = x2;
					geometry[10] = y2;
					geometry[11] = Argument.LINETO;
					geometry[12] = x2;
					geometry[13] = y1;
					geometry[14] = Argument.LINETO;
					geometry[15] = x1;
					geometry[16] = y1;
				}
				else
				{
					/*
					 * We've reached a geometry type we don't understand, or
					 * the MIF file is screwed up.
					 */
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_VALUES) +
						": " + mFilename + ":" + mMIFFile.getLineNumber());
				}

				/*
				 * Read any color and symbol information for this geometry
				 * and skip 'SMOOTH', 'CENTER' and lines that we are not
				 * interested in. 
				 */
				mMIFNextLine = mMIFFile.readLine();
				boolean finishedGeometry = false;
				while (mMIFNextLine != null && (!finishedGeometry))
				{
					st = new StringTokenizer(mMIFNextLine, " (,)");
					if (st.hasMoreTokens())
					{
						String token = st.nextToken().toLowerCase();
						if (token.equals("pen"))
						{
							if (st.countTokens() >= 3)
							{
								penWidth = st.nextToken();
								penPattern = st.nextToken();
								penColor = st.nextToken();
							}
						}
						else if (token.equals("brush"))
						{
							if (st.countTokens() >= 3)
							{
								brushPattern = st.nextToken();
								brushForeColor = st.nextToken();
								brushBackColor = st.nextToken();
							}
						}
						else if (token.equals("symbol"))
						{
							boolean isIcon = false;
							if (st.countTokens() >= 3)
							{
								symbolShape = st.nextToken();
								if (symbolShape.startsWith("\""))
								{
									symbolShape = symbolShape.substring(1, symbolShape.length() - 1);
									isIcon = true;
								}
								symbolColor = st.nextToken();
								symbolSize = st.nextToken();
							}
							if (st.hasMoreTokens() && (!isIcon))
							{
								/*
								 * Font name included too.
								 * Strip any surrounding quotes.
								 */
								symbolFont = st.nextToken();
								if (symbolFont.startsWith("\""))
								{
									/*
									 * Font name may be multiple words.
									 * Join strings until font name ends
									 * with matching quote.
									 */
									while (!symbolFont.endsWith("\""))
									{
										symbolFont = symbolFont + " " + st.nextToken();
									}
									symbolFont = symbolFont.substring(1, symbolFont.length() - 1);
								}
							}
							if (st.hasMoreTokens())
							{
								symbolStyle = st.nextToken();
							}
						}
						else if (token.equals("smooth"))
						{
						}
						else if (token.equals("center"))
						{
						}
						else
						{
							/*
							 * This line is not part of the preceding geometry,
							 * it must be start of next geometry. 
							 */
							finishedGeometry = true;
						}

						if (!finishedGeometry)
							mMIFNextLine = mMIFFile.readLine();
					}
				}
			}

			/*
			 * Build geometry argument to return.
			 */
			Argument arg = new Argument((int)geometry[0], geometry);
			row.add(arg);
			
			/*
			 * Add symbol and color information for this geometry.
			 */
			if (penWidth == null)
				row.add(Argument.numericOne);
			else
			{
				/*
				 * Convert pixel width to millimetres.
				 */
				double w = (Integer.parseInt(penWidth) & 7);
				w = w / Constants.getScreenResolution() * Constants.MM_PER_INCH;
				row.add(new Argument(w));
			}

			if (penPattern == null)
				row.add(Argument.numericZero);
			else
				row.add(new Argument(Double.parseDouble(penPattern)));
				
			if (penColor == null)
				row.add(DEFAULT_COLOR);
			else
				row.add(new Argument(Argument.STRING, intToColor(penColor)));
				
			if (brushPattern == null)
				row.add(Argument.numericZero);
			else
				row.add(new Argument(Double.parseDouble(brushPattern)));

			if (brushForeColor == null)
				row.add(DEFAULT_COLOR);
			else
				row.add(new Argument(Argument.STRING, intToColor(brushForeColor)));

			if (brushBackColor == null)
				row.add(DEFAULT_COLOR);
			else
				row.add(new Argument(Argument.STRING, intToColor(brushBackColor)));

			/*
			 * Shape may be either an ASCII character code or an image filename.
			 */
			if (symbolShape == null)
				row.add(Argument.emptyString);
			else
			{
				try
				{
					char c = (char)Short.parseShort(symbolShape);
					if (c > 31)
						symbolShape = Character.toString(c);
					else
						symbolShape = "";
				}
				catch (NumberFormatException e)
				{
				}
				row.add(new Argument(Argument.STRING, symbolShape));
			}

			if (symbolColor == null)
				row.add(DEFAULT_COLOR);
			else
				row.add(new Argument(Argument.STRING, intToColor(symbolColor)));

			if (symbolSize == null)
			{
				row.add(DEFAULT_SIZE);
			}
			else
			{
				/*
				 * Convert symbol size in pixels to millimetres.
				 */
				double s = Double.parseDouble(symbolSize);
				s = s / Constants.getScreenResolution() * Constants.MM_PER_INCH;
				row.add(new Argument(s));
			}

			if (symbolFont == null)
				row.add(DEFAULT_FONT);
			else
				row.add(new Argument(Argument.STRING, symbolFont));
				
			if (symbolStyle == null)
				row.add(Argument.numericZero);
			else
				row.add(new Argument(Double.parseDouble(symbolStyle)));
		}
		else
		{
			row = null;
		}

		return(row);
	}

	/**
	 * Fetch next row from MIF file.
	 * @return row, or null if reached end of file.
	 */
	public Row fetch() throws MapyrusException
	{
		Row row = new Row();

		try
		{
			row = parseGeometry(row);
			if (row != null)
			{
				if (mMIDFile != null)
					mMIDFile.getRow(row);
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		catch (NumberFormatException e)
		{
			/*
			 * Catch any error parsing geometry coordinates from MIF file.
			 */
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_NUMBER) +
				": " + mFilename + ": " + e.getMessage());
		}
		return(row);
	}

	/**
	 * Close MapInfo files.
	 */
	public void close() throws MapyrusException
	{
		if (mMIFFile != null)
		{
			try
			{
				mMIFFile.close();
			}
			catch (IOException e)
			{
			}
		}
		if (mMIDFile != null)
		{
				mMIDFile.close();
		}

		mMIFFile = null;
		mMIDFile = null;
	}
}
