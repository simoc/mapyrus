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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.mapyrus.font;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Constants;

/**
 * A PostScript Type 1 font, read from a .pfa or .pfb font definition file.
 * Provides methods to parse the font file and include it in a
 * PostScript or PDF file.
 */
public class PostScriptFont
{
	private static final int LINE_LENGTH = 30;
	
	/*
	 * Name of font given in header of font file.
	 */
	private String m_fontName;

	/*
	 * Contents of font file.
	 */
	private StringBuilder m_fileContents;

	/**
	 * Create PostScript Type 1 font from a .pfa or .pfb file.
	 * @param filename name of .pfa or .pfb file.
	 * @param isBinary true if file is to be parsed as binary file.
	 */
	public PostScriptFont(String filename, boolean isBinary)
		throws IOException, MapyrusException
	{
		/*
		 * Only accept filenames with correct suffix.
		 */
		if (isBinary)
		{
			if (!filename.toLowerCase().endsWith(".pfb"))
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFB_FILE) +
				": " + filename);
		}
		else
		{
			if (!filename.toLowerCase().endsWith(".pfa"))
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
					": " + filename);
		}

		if (isBinary)
			readPfbFile(filename);
		else
			readPfaFile(filename);
	}

	/**
	 * Parse binary .pfb file.
	 * @param pfbFilename name of .pfb file to read.
	 */
	private void readPfbFile(String pfbFilename)
		throws IOException, MapyrusException
	{
		BufferedInputStream stream = null;

		try
		{			
			stream = new BufferedInputStream(new FileInputStream(pfbFilename));
			byte magic[] = new byte[2];
			byte header[] = new byte[4];
			ArrayList<byte []> segments = new ArrayList<byte []>();
			int totalLength = 0;

			/*
			 * Read each segment from .pfb file, as described in Adobe technical note
			 * 5040 "Supporting Downloadable PostScript Language Fonts".
			 */
			if (stream.read(magic) != magic.length)
			{
				throw new IOException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + pfbFilename);
			}
			while (magic[1] != 3)
			{
				if (magic[0] != -128)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFB_FILE) +
						": " + pfbFilename);
				}
				if (stream.read(header) != header.length)
				{
					throw new IOException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
						": " + pfbFilename);
				}
				int segmentLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();
				totalLength += segmentLength;
				byte []buf = new byte[segmentLength];
				if (stream.read(buf) != buf.length)
				{
					throw new IOException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
						": " + pfbFilename);
				}
				segments.add(buf);
				
				if (stream.read(magic) != magic.length)
				{
					throw new IOException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
						": " + pfbFilename);
				}
			}
			if (segments.size() < 3)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFB_FILE) +
					": " + pfbFilename);
			}

			/*
			 * Create embedded Type 1 font object as described in section 5.8
			 * of Adobe PDF Reference Manual. 
			 */
			m_fileContents = new StringBuilder(128 * 1024);
			m_fileContents.append("<< /Type /FontFile /Length ");
			long hexEncodedLength = totalLength * 2 + Constants.LINE_SEPARATOR.length();
			hexEncodedLength += totalLength / LINE_LENGTH * Constants.LINE_SEPARATOR.length();
			m_fileContents.append(hexEncodedLength);
			m_fileContents.append(Constants.LINE_SEPARATOR);
			m_fileContents.append("/Length1 ");
			m_fileContents.append(((byte [])segments.get(0)).length);
			m_fileContents.append(" /Length2 ");
			m_fileContents.append(((byte [])segments.get(1)).length);
			m_fileContents.append(" /Length3 ");
			m_fileContents.append(((byte [])segments.get(2)).length);
			m_fileContents.append(" /Filter /ASCIIHexDecode >>");
			m_fileContents.append(Constants.LINE_SEPARATOR);
			m_fileContents.append("stream");
			m_fileContents.append(Constants.LINE_SEPARATOR);

			/*
			 * Add all segments to PDF object as a hex encoded stream.
			 */
			StringBuilder segBuf = new StringBuilder();
			int nBytesAdded = 0;
			for (int i = 0; i < segments.size(); i++)
			{
				byte []buf = (byte [])segments.get(i);
				for (int j = 0; j < buf.length; j++)
				{
					if (i == 0 && (buf[j] == '\r' || buf[j] == '\n' || buf[j] == '\t' || buf[j] >= 32))
					{
						segBuf.append((char)buf[j]);
					}
					String s = Integer.toHexString(buf[j] & 0xff);
					if (s.length() < 2)
						m_fileContents.append('0');
					m_fileContents.append(s);

					/*
					 * Add regular line breaks.
					 */
					if ((++nBytesAdded) % LINE_LENGTH == 0)
						m_fileContents.append(Constants.LINE_SEPARATOR);
				}
				if (i == 0)
				{
					int index = segBuf.indexOf("\n/FontName");
					if (index < 0)
						index = segBuf.indexOf("\r/FontName");
					if (index < 0)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
							": " + pfbFilename);
					}
					String remainder = segBuf.substring(index + 10);
					StringTokenizer st = new StringTokenizer(remainder);
					if (!st.hasMoreTokens())
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
							": " + pfbFilename);
					}
					m_fontName = st.nextToken();
					if (m_fontName.startsWith("/"))
						m_fontName = m_fontName.substring(1);
				}
			}
			m_fileContents.append(Constants.LINE_SEPARATOR);
			m_fileContents.append("endstream");
		}
		finally
		{
			try
			{
				if (stream != null)
					stream.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Parse ASCII .pfa file.
	 * @param pfaFilename name of .pfa file to read.
	 */
	private void readPfaFile(String pfaFilename)
		throws IOException, MapyrusException
	{
		BufferedReader bufferedReader = null;

		try
		{
			bufferedReader = new BufferedReader(new FileReader(pfaFilename));
	
			/*
			 * First line of file contains PostScript keyword, then font name.  For example,
			 * %!PS-AdobeFont-1.0: LuxiSerif 1.1000
			 */
			String firstLine = bufferedReader.readLine();
			if (firstLine == null)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
					": " + pfaFilename);

			/*
			 * Read entire .pfa file into memory, most files are about 100kb in size.
			 */
			m_fileContents = new StringBuilder(128 * 1024);
			m_fileContents.append(firstLine);
			m_fileContents.append(Constants.LINE_SEPARATOR);
			
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				m_fileContents.append(line);
				m_fileContents.append(Constants.LINE_SEPARATOR);
				if (line.startsWith("/FontName"))
				{
					StringTokenizer st = new StringTokenizer(line);
					if (st.countTokens() >= 2)
					{
						st.nextToken();
						m_fontName = st.nextToken();
						if (m_fontName.startsWith("/"))
							m_fontName = m_fontName.substring(1);
					}
				}
			}

			if (m_fontName == null)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
					": " + pfaFilename);
			}
		}
		finally
		{
			if (bufferedReader != null)
				bufferedReader.close();
		}		
	}

	/**
	 * Return name of font, parsed from .pfa file.
	 * @return font name.
	 */
	public String getName()
	{
		return(m_fontName);
	}

	/**
	 * String representation of PostScript font.
	 * @return font name.
	 */
	public String toString()
	{
		return("PostScript Font " + m_fontName);
	}

	/**
	 * Return definition of font read from .pfa file, suitable for inclusion
	 * in a PostScript file.
	 * @return font definition.
	 */	
	public String getFontDefinition()
	{
		return(m_fileContents.toString());
	}
}
