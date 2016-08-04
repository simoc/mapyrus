/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2016 Simon Chenery.
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
package org.mapyrus.font;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.HashMap;

import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.io.ASCII85Writer;

/**
 * Methods to extract fields from an Adobe OpenType (.otf) format font file.
 * File format described at www.microsoft.com/typography/otfspec/otf.htm
 */
public class OpenTypeFont
{
	private static final long MAGIC_NUMBER_CFF = 0x4f54544f;
	private static final long MAGIC_NUMBER_TTF = 0x00010000;

	private static final int CMAP_PLATFORM_ID_UNICODE_BMP = 3;
	private static final int CMAP_ENCODING_ID_UNICODE_BMP = 1;

	private static final int NAME_PLATFORM_ID_UNICODE = 1;
	private static final int NAME_ENCODING_ID_UNICODE_1_0 = 0;

	private static final int NAME_FULL_NAME_CODE = 4;
	private static final int NAME_POSTSCRIPT_NAME_CODE = 6;

	private static final int LINE_LENGTH = 30;

	private String m_otfFilename;

	private int m_CIDFontType;
	private short m_glyphXMin;
	private short m_glyphYMin;
	private short m_glyphXMax;
	private short m_glyphYMax;
	private String m_fullFontName;
	private String m_postScriptFontName;
	private int m_italicAngle;
	private int m_flags;

	private short m_ascender;
	private short m_descender;
	private int m_numberOfHMetrics;

	private int []m_hMetrics;
	private HashMap<Integer, Integer> m_glyphIndexes;

	/**
	 * Pointer to a table in the OpenType file from the header at the start of the file.
	 */
	class TableRecord
	{
		long fileOffset;
		long length;
	}

	TableRecord m_CFFTableRecord;
	TableRecord m_glyfTableRecord;

	/**
	 * Create metrics for a character.
	 * @param filename character code in AFM file.
	 */
	public OpenTypeFont(String otfFilename) throws IOException, MapyrusException
	{
		RandomAccessFile r = null;

		m_otfFilename = otfFilename;

		try
		{
			r = new RandomAccessFile(otfFilename, "r");
			readFile(r, otfFilename);
		}
		finally
		{
			if (r != null)
				r.close();
		}
	}

	private void readFile(RandomAccessFile r, String otfFilename) throws IOException, MapyrusException
	{
		m_glyphXMin = 0;
		m_glyphYMin = 0;
		m_glyphXMax = 1;
		m_glyphYMax = 1;
		m_glyphIndexes = new HashMap<Integer, Integer>();
		m_fullFontName = "unknown";
		m_postScriptFontName = "unknown";
		m_flags = 0;

		HashMap<String, TableRecord> tableRecords = new HashMap<String, TableRecord>();

		long magic = readUnsignedInt(r);
        if (magic != MAGIC_NUMBER_CFF && magic != MAGIC_NUMBER_TTF)
        {
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_OTF_FILE) + ": " + otfFilename);
        }
        int numTables = r.readUnsignedShort();
        r.readUnsignedShort(); /* searchRange */
        r.readUnsignedShort(); /* entrySelector */
        r.readUnsignedShort(); /* rangeShift */

        /*
         * Read file offset of each table in OpenType file.
         */
        for (int i = 0; i < numTables; i++)
        {
            byte b1 = r.readByte();
            byte b2 = r.readByte();
            byte b3 = r.readByte();
            byte b4 = r.readByte();

            StringBuilder sb = new StringBuilder();
            sb.append((char)b1);
            sb.append((char)b2);
            sb.append((char)b3);
            sb.append((char)b4);
            String tag = sb.toString();

            TableRecord tableRecord = new TableRecord();
            readUnsignedInt(r); /* checkSum */
            tableRecord.fileOffset = readUnsignedInt(r);
            tableRecord.length = readUnsignedInt(r);

            tableRecords.put(tag, tableRecord);
        }

        String tableTag = "cmap";
        TableRecord tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readCmap(r);

        tableTag = "head";
        tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readHead(r);

        tableTag = "name";
        tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readName(r);

        tableTag = "hhea";
        tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readHhea(r);

        tableTag = "hmtx";
        tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readHmtx(r);

        tableTag = "post";
        tableRecord = tableRecords.get(tableTag);
        if (tableRecord == null)
        	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " + tableTag + ": " + otfFilename);
        r.seek(tableRecord.fileOffset);
        readPost(r);

        String tableTag0 = "CFF ";
        String tableTag2 = "glyf";
        tableRecord = tableRecords.get(tableTag0);
        if (tableRecord != null)
        {
        	m_CIDFontType = 0;
        	r.seek(tableRecord.fileOffset);
            readCFF(r, tableRecord);
        }
        else
        {
            tableRecord = tableRecords.get(tableTag2);
            if (tableRecord != null)
            {
            	m_CIDFontType = 2;
            	r.seek(tableRecord.fileOffset);
                readGlyf(r, tableRecord);
            }
            else
            {
            	throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.TABLE_NOT_FOUND) + ": " +
            		tableTag0 + "/" + tableTag2 + ": " + otfFilename);
            }
        }
	}

	/**
	 * Read 'cmap' table from OpenType file and extract fields
	 * we need for calculating character metrics.
	 * @param r file to read from.
	 */
	private void readCmap(RandomAccessFile r) throws IOException
	{
		long fileOffset = r.getFilePointer();

		r.readUnsignedShort(); /* version */
        int numCmapTables = r.readUnsignedShort();

        for (int j = 0; j < numCmapTables; j++)
        {
            int platformID = r.readUnsignedShort();
            int encodingID = r.readUnsignedShort();
            int cmapOffset = r.readInt();

            if (platformID == CMAP_PLATFORM_ID_UNICODE_BMP &&
            	encodingID == CMAP_ENCODING_ID_UNICODE_BMP)
            {
            	long fileOffset2 = r.getFilePointer();

            	r.seek(fileOffset + cmapOffset);

            	int encodingFormat = r.readUnsignedShort();
            	r.readUnsignedShort(); /*encodingLength */
            	r.readUnsignedShort(); /*encodingLanguage */

            	if (encodingFormat == 4)
            	{
            		readCmapEncodingFormat4(r);
            	}
            	else if (encodingFormat == 0)
            	{
            		readCmapEncodingFormat0(r);
            	}

            	r.seek(fileOffset2);
            }
        }
	}

	/**
	 * Read 'cmap' subtable containing format 4 Segment mapping to delta values
	 * for glyph indexes.
	 * @param r file to read from.
	 */
	private void readCmapEncodingFormat4(RandomAccessFile r) throws IOException
	{
       	int segCountX2 = r.readUnsignedShort();
    	r.readUnsignedShort(); /* searchRange */
    	r.readUnsignedShort(); /* entrySelector */
    	r.readUnsignedShort(); /* rangeShift */
    	int []endCounts = new int[segCountX2 / 2];
    	for (int k = 0; k < segCountX2 / 2; k++)
    	{
    		endCounts[k] = r.readUnsignedShort();
    	}
    	r.readUnsignedShort(); /* reservedPad */
    	int []startCounts = new int[segCountX2 / 2];
    	for (int k = 0; k < segCountX2 / 2; k++)
    	{
    		startCounts[k] = r.readUnsignedShort();
    	}
    	short []idDeltas = new short[segCountX2 / 2];
    	for (int k = 0; k < segCountX2 / 2; k++)
    	{
    		idDeltas[k] = r.readShort();
    	}
    	long []idRangeOffsets = new long[segCountX2 / 2];
    	for (int k = 0; k < segCountX2 / 2; k++)
    	{
    		long fileOffset = r.getFilePointer();
    		int idRangeOffset = r.readUnsignedShort();
    		if (idRangeOffset == 0)
    			idRangeOffsets[k] = idRangeOffset;
    		else
    			idRangeOffsets[k] = fileOffset + idRangeOffset;
    	}

    	for (int k = 0; k < segCountX2 / 2; k++)
    	{
    		if (idRangeOffsets[k] > 0)
			{
				r.seek(idRangeOffsets[k]);
			}

    		for (int c = startCounts[k]; c <= endCounts[k]; c++)
    		{
    			int glyphIndex;

    			if (idRangeOffsets[k] > 0)
    			{
    				int glyphId = r.readUnsignedShort();
    				glyphIndex = idDeltas[k] + c + glyphId;
    				glyphIndex = idDeltas[k] + glyphId;
    			}
    			else
    			{
    				glyphIndex = idDeltas[k] + c;
    			}

    			m_glyphIndexes.put(Integer.valueOf(c), Integer.valueOf(glyphIndex));
    		}
    	}
	}

	/**
	 * Read 'cmap' subtable containing format 0 Byte encoding table
	 * for glyph indexes.
	 * @param r file to read from.
	 */
	private void readCmapEncodingFormat0(RandomAccessFile r) throws IOException
	{
		for (int i = 0; i < 256; i++)
		{
			int glyphId = r.readUnsignedShort();
			m_glyphIndexes.put(Integer.valueOf(i), Integer.valueOf(glyphId));
		}
	}

	/**
	 * Read 'head' table from OpenType file and extract fields
	 * we need for calculating character metrics.
	 * @param r file to read from.
	 */
	private void readHead(RandomAccessFile r) throws IOException
	{
		readUnsignedInt(r); /* version */
		readUnsignedInt(r); /* fontRevision */
		readUnsignedInt(r); /* checkSumAdjustment */
		readUnsignedInt(r); /* magicNumber */
		r.readUnsignedShort(); /* flags */
		r.readUnsignedShort(); /* unitsPerEm */
		r.readLong(); /* creation date */
		r.readLong(); /* modification date */
		m_glyphXMin = r.readShort();
		m_glyphYMin = r.readShort();
		m_glyphXMax = r.readShort();
		m_glyphYMax = r.readShort();
	}

	/**
	 * Read 'name' table from OpenType file and extract font name fields.
	 * @param r file to read from.
	 */
	private void readName(RandomAccessFile r) throws IOException
	{
		long fileOffset = r.getFilePointer();

		r.readUnsignedShort(); /* format */
        int nameCount = r.readUnsignedShort();
        int stringOffset = r.readUnsignedShort();

        for (int j = 0; j < nameCount; j++)
        {
            int platformID = r.readUnsignedShort();
            int encodingID = r.readUnsignedShort();
            r.readUnsignedShort(); /* languageID */
            int nameID = r.readUnsignedShort();
            int nameLength = r.readUnsignedShort();
            int nameOffset = r.readUnsignedShort();

            if (platformID == NAME_PLATFORM_ID_UNICODE &&
            	encodingID == NAME_ENCODING_ID_UNICODE_1_0)
            {
            	if (nameID == NAME_FULL_NAME_CODE || nameID == NAME_POSTSCRIPT_NAME_CODE)
            	{
            		long savedFilePointer = r.getFilePointer();
            		r.seek(fileOffset + stringOffset + nameOffset);

            		StringBuilder sb = new StringBuilder();
            		for (int k = 0; k < nameLength; k++)
            		{
            			byte b = r.readByte();
            			sb.append((char)b);
            		}
            		if (nameID == NAME_FULL_NAME_CODE)
            			m_fullFontName = sb.toString();
            		else
            			m_postScriptFontName = sb.toString();

            		r.seek(savedFilePointer);
            	}
            }
        }
	}

	/**
	 * Read 'hhea' table from OpenType file and extract fields
	 * we need for calculating character metrics.
	 * @param r file to read from.
	 */
	private void readHhea(RandomAccessFile r) throws IOException
	{
		r.readInt(); /* version */
        m_ascender = r.readShort();
        m_descender = r.readShort();
        r.skipBytes(13 * 2);
        m_numberOfHMetrics = r.readUnsignedShort();
	}

	/**
	 * Read 'hmtx' table from OpenType file and extract fields
	 * we need for calculating character metrics.
	 * @param r file to read from.
	 */
	private void readHmtx(RandomAccessFile r) throws IOException
	{
		m_hMetrics = new int[m_numberOfHMetrics];
		for (int j = 0; j < m_numberOfHMetrics; j++)
        {
            int advanceWidth = r.readUnsignedShort();
            r.readShort(); /* lsb */
            m_hMetrics[j] = advanceWidth;
        }
	}

	/**
	 * Read 'post' table from OpenType file and extract fields
	 * we need for embedding font in PDF file.
	 * @param r file to read from.
	 */
	private void readPost(RandomAccessFile r) throws IOException
	{
		readUnsignedInt(r); /* version */
		m_italicAngle = r.readShort();
		r.readShort(); /* italicAngle decimal part */
		r.readShort(); /* underlinePosition */
		r.readShort(); /* underlineThickness */
		long isFixedPitch = readUnsignedInt(r);
		if (isFixedPitch != 0)
			m_flags |= 1;
	}

	/**
	 * Read 'CFF' table from OpenType file containing Compact Font Format (CFF)
	 * PostScript font program that is embedded in PDF file.
	 * @param r file to read from.
	 * @param tableRecord position in file of CFF record.
	 */
	private void readCFF(RandomAccessFile r, TableRecord tableRecord) throws IOException
	{
		/*
		 * Just note position of table in file. We will read it later.
		 */
		m_CFFTableRecord = tableRecord;
	}

	/**
	 * Read 'glyf' table from OpenType file containing TrueType font program
	 * that is embedded in PDF file.
	 * @param r file to read from.
	 * @param tableRecord position in file of glyf record.
	 */
	private void readGlyf(RandomAccessFile r, TableRecord tableRecord) throws IOException
	{
		/*
		 * We will embed whole OpenType font file, not just this table.
		 */
		m_glyfTableRecord = new TableRecord();
		m_glyfTableRecord.fileOffset = 0;
		m_glyfTableRecord.length = r.length();
	}

	/**
	 * Read four byte unsigned integer from file.
	 * @param r file to read from.
	 * @return value read from file.
	 */
	private long readUnsignedInt(RandomAccessFile r) throws IOException
	{
		long high = r.readUnsignedShort();
		long low = r.readUnsignedShort();
		return high * 65536 + low;
	}

	/**
	 * Is this a CIDFontType0 with CFF PostScript outlines, or CIDFontType2 with TrueType outlines?
	 * @return CID Font Type.
	 */
	public int getCIDFontType()
	{
		return m_CIDFontType;
	}

	/**
	 * Get PostScript name for font.
	 * @return PostScript name.
	 */
	public String getPostScriptFontName()
	{
		return m_postScriptFontName;
	}

	/**
	 * Get full font name.
	 * @return Full font name.
	 */
	public String getFullFontName()
	{
		return m_fullFontName;
	}

	/**
	 * Returns height of capital letters in font.
	 * @return cap height.
	 */
	public int getCapHeight()
	{
		return(m_glyphYMax / 2);
	}

	/**
	 * Returns bounding box of font.
	 * @return font bounding box.
	 */
	public Rectangle getFontBBox()
	{
		return new Rectangle(m_glyphXMin, m_glyphYMin, m_glyphXMax - m_glyphXMin, m_glyphYMax - m_glyphYMin);
	}

	/**
	 * Returns font type as PDF bit flags.
	 * @return font bit flags.
	 */
	public int getFlags()
	{
		return(m_flags);
	}

	/**
	 * Returns maximum height of font above baseline.
	 * @return ascender height.
	 */
	public int getAscender()
	{
		return(m_ascender);
	}

	/**
	 * Returns maximum height of font below baseline.
	 * @return descender height.
	 */
	public int getDescender()
	{
		return(m_descender);
	}

	/**
	 * Returns italic angle of font.
	 * @return italic angle.
	 */
	public int getItalicAngle()
	{
		return(m_italicAngle);
	}

	/**
	 * Return string representation of object.
	 * @return string representation.
	 */
	public String toString()
	{
		return("OpenType font " + m_fullFontName);
	}

	/**
	 * Calculate size of string displayed using this font.
	 * @param s string to calculate size for.
	 * @param pointSize point size in which string is displayed.
	 * @return size of string in points.
	 */
	StringDimension getStringDimension(String s, double pointSize)
	{
		int total = 0;
		int sLength = s.length();
		char c;
		double pointLen;
		double ascent, descent;
		StringDimension retval = new StringDimension();

		/*
		 * Add up widths of all characters in string.
		 */
		for (int i = 0; i < sLength; i++)
		{
			c = s.charAt(i);
			Integer hMetricsIndex = m_glyphIndexes.get(Integer.valueOf(c));
			if (hMetricsIndex != null && hMetricsIndex.intValue() < m_hMetrics.length)
				total += m_hMetrics[hMetricsIndex.intValue()];
			else
				total += m_glyphXMax;
		}
		pointLen = (double)total / m_glyphXMax * pointSize;
		ascent = (double)m_ascender / m_glyphYMax * pointSize;
		descent = (double)m_descender / m_glyphYMax * pointSize;

		retval.setSize(pointLen, pointSize, ascent, descent);
		return retval;
	}

	/**
	 * Return definition of font read from .otf file, suitable for inclusion
	 * in a PDF file stream object.
	 * @return font definition.
	 */
	public String getFontDefinition() throws IOException
	{
		RandomAccessFile r = null;
		StringBuffer sb = new StringBuffer();

		try
		{
			TableRecord tableRecord;

			if (m_CIDFontType == 2)
				tableRecord = m_glyfTableRecord;
			else
				tableRecord = m_CFFTableRecord;

			r = new RandomAccessFile(m_otfFilename, "r");
			r.seek(tableRecord.fileOffset);

			/*
			 * Font files can be several megabytes and do compress to a much smaller size,
			 * so better to use Flate and ASCII85 encoding.
			 */
			StringWriter ascii85sw = new StringWriter((int)tableRecord.length * 2);
			ASCII85Writer ascii85 = new ASCII85Writer(ascii85sw, true);
			for (int i = 0; i < tableRecord.length; i++)
			{
				byte byteValue = r.readByte();
				ascii85.write(byteValue);
			}
			ascii85.close();
			ascii85sw.flush();
			String eodMarker = "~>";
			int nEncodedChars = ascii85.getEncodedLength() + eodMarker.length();

			sb.ensureCapacity(nEncodedChars + 200);

			sb.append("<< /Type /FontFile ");
			if (m_CIDFontType == 0)
				sb.append("/Subtype /CIDFontType0C");
			sb.append(" /Length ").append(nEncodedChars);
			if (m_CIDFontType == 2)
				sb.append(" /Length1 ").append(tableRecord.length);
			sb.append(" /Filter [/ASCII85Decode /FlateDecode] >>");
			sb.append(Constants.LINE_SEPARATOR);
			sb.append("stream");
			sb.append(Constants.LINE_SEPARATOR);
			sb.append(ascii85sw.getBuffer());
			sb.append(eodMarker);
			sb.append(Constants.LINE_SEPARATOR);
			sb.append("endstream");
		}
		finally
		{
			if (r != null)
				r.close();
		}

		return sb.toString();
	}

	/**
	 * Return array of character widths for PDF CIDFont dictionary W entry.
	 * @return character width string.
	 */
	public String getCharWidths()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[ 0 [");
		for (int i = 0; i < m_hMetrics.length; i++)
		{
			if (i > 0)
			{
				if (i % LINE_LENGTH == 0)
					sb.append(Constants.LINE_SEPARATOR).append("% ").append(i).append(Constants.LINE_SEPARATOR);
				else
					sb.append(" ");
			}
			sb.append(m_hMetrics[i]);
		}
		sb.append("] ]");
		return sb.toString();
	}

	/**
	 * Get glyph in OpenType font that character maps to.
	 * @param c character (in Unicode).
	 * @return glyph in OpenType font.
	 */
	public char getEncodedChar(char c)
	{
		Integer glyphId = m_glyphIndexes.get(Integer.valueOf(c));
		if (glyphId == null)
			return c;
		else
			return (char)glyphId.intValue();
	}
}
