/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2007 Simon Chenery.
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
package org.mapyrus.pdf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Provides functions for parsing PDF format files.
 */
public class PDFFile
{
	private String m_filename;
	private RandomAccessFile m_pdfFile;
	private HashMap m_objects;

	public PDFFile(String filename) throws IOException, MapyrusException
	{
		/*
		 * Open file, find and read the 'xref' table at the end of the
		 * file giving the file offset of each object.
		 */
		m_filename = filename;
		m_pdfFile = new RandomAccessFile(filename, "r");
		String header = m_pdfFile.readLine();
		if (!header.startsWith("%PDF-"))
		{
			throw new MapyrusException("");
		}

		byte []xrefBuf = new byte[20];
		m_pdfFile.seek(m_pdfFile.length() - xrefBuf.length);
		m_pdfFile.readLine();	/* skip line with 'startxref' keyword */
		String line = m_pdfFile.readLine();
		long xrefOffset = Long.parseLong(line);

		m_pdfFile.seek(xrefOffset);
		m_pdfFile.readLine();	/* skip line with 'xref' keyword */
		line = m_pdfFile.readLine();
		StringTokenizer st = new StringTokenizer(line);
		if (st.countTokens() < 2)
			throw new MapyrusException("");
		int startIndex = Integer.parseInt(st.nextToken());
		int count = Integer.parseInt(st.nextToken());
		HashMap objectOffsets = new HashMap();
		for (int i = 0; i < count; i++)
		{
			line = m_pdfFile.readLine();
			st = new StringTokenizer(line);
			if (st.countTokens() < 3)
				throw new MapyrusException("");
			Long objOffset = Long.valueOf(st.nextToken());
			st.nextToken();	/* skip generation number */
			String status = st.nextToken();
			if (status.equals("n"))
				objectOffsets.put(new Integer(i + startIndex), objOffset);
		}

		/*
		 * Now read the 'trailer' dictionary.
		 */
		m_pdfFile.readLine();	/* skip line with 'trailer' keyword */
		PDFObject trailer = readObject();

		/*
		 * Read each of the objects in the PDF file. 
		 */
		m_objects = new HashMap(objectOffsets.size());
		Iterator it = objectOffsets.keySet().iterator();
		while (it.hasNext())
		{
			Integer key = (Integer)(it.next());
			Long objectOffset = (Long)objectOffsets.get(key);
			m_pdfFile.seek(objectOffset.longValue());

			int id = readObjectBegin();
			if (id != key.intValue())
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
					": " + m_filename);
			}
			PDFObject obj = readObject();
			long streamOffset = readObjectEnd();
			obj.setStreamOffset(streamOffset);

			m_objects.put(key, obj);
		}

		/*
		 * Find root object containing reference to pages.
		 */
		HashMap dictionary = trailer.getDictionary();
		PDFObject rootReference = (PDFObject)dictionary.get("/Root");
		int id = rootReference.getReference();
	}

	/**
	 * Read next character from PDF file, skipping over comments.
	 * @param skipComments true if comments are to be skipped.
	 * @return next char, ignoring comments.
	 */
	private int readChar(boolean skipComments) throws IOException, MapyrusException
	{
		int c = m_pdfFile.read();
		if (c == -1)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
				": " + m_filename);
		}
		else if (skipComments && c == '%')
		{
			c = readChar(skipComments);
			while (c != '\r' && c != '\n')
				c = readChar(skipComments);
		}
		return(c);
	}

	/**
	 * Peek next character from PDF file without reading it, skipping over comments.
	 * @param skipComments true if comments are to be skipped.
	 * @return next char, ignoring comments.
	 */
	private int peekChar(boolean skipComments) throws IOException, MapyrusException
	{
		long offset = m_pdfFile.getFilePointer();
		int c = readChar(skipComments);
		m_pdfFile.seek(offset);
		return(c);
	}

	/**
	 * Read object from current position in PDF file.
	 * @return a String or HashMap of key, value dictionary pairs.
	 */
	private PDFObject readObject() throws IOException, MapyrusException
	{
		PDFObject retval = null;
		int c, lastC = -1;
		StringBuffer sb = new StringBuffer();

		/*
		 * Skip whitespace.
		 */
		c = readChar(true);
		while (Character.isWhitespace((char)c))
			c = readChar(true);

		if (c == '(')
		{
			/*
			 * Parse simple string.
			 */
			c = readChar(false);
			while (!(c == ')' && lastC != '\\'))
			{
				sb.append((char)c);
				lastC = c;
				c = readChar(false);
			}
			retval = new PDFObject(sb.toString());
		}
		else if (c == '<')
		{
			c = peekChar(true);
			if (c != '<')
			{
				/*
				 * Parse simple hex string.
				 */
				c = readChar(false);
				while (!(c == '>' && lastC != '\\'))
				{
					sb.append((char)c);
					lastC = c;
					c = readChar(false);
				}
				retval = new PDFObject(sb.toString());
			}
			else
			{
				readChar(true);

				/*
				 * Parse dictionary.
				 */
				HashMap dictionary = new HashMap();

				/*
				 * Skip whitespace.
				 */
				c = readChar(true);
				while (Character.isWhitespace((char)c))
					c = readChar(true);
				while (c == '/')
				{
					sb = new StringBuffer();
					sb.append((char)c);
					c = peekChar(false);
					while ((!Character.isWhitespace((char)c)) &&
						c != '[' && c != '/' && c != '(' && c != '<')
					{
						readChar(false);
						sb.append((char)c);
						c = peekChar(false);
					}
					PDFObject value = readObject();
					dictionary.put(sb.toString(), value);

					/*
					 * Skip whitespace.
					 */
					c = readChar(true);
					while (Character.isWhitespace((char)c))
						c = readChar(true);
				}
				c = readChar(true);	/* skip over second '>' in name */
				retval = new PDFObject(dictionary);
			}
		}
		else if (c == '[')
		{
			/*
			 * Parse array of objects.
			 */
			ArrayList list = new ArrayList();

			PDFObject obj = readObject();
			while (obj != null)
			{
				list.add(obj);
				obj = readObject();
			}
			PDFObject[] array = new PDFObject[list.size()];
			list.toArray(array);
			retval = new PDFObject(array);
		}
		else if (c == ']')
		{
			/*
			 * End of array.
			 */
			retval = null;
		}
		else
		{
			/*
			 * Parse number or identifier or reference.
			 */
			sb = new StringBuffer();
			sb.append((char)c);
			c = peekChar(false);
			while ((!Character.isWhitespace((char)c)) &&
				c != '/' && c != ']' && c != '>')
			{
				readChar(false);
				sb.append((char)c);
				c = peekChar(false);
			}
			long offset = m_pdfFile.getFilePointer();
			
			/*
			 * Check if this is a reference of the form '12 0 R'.
			 * Check for any whitespace, then a '0', more whitespace, then a 'R'.
			 */
			c = readChar(true);
			while (Character.isWhitespace((char)c))
				c = readChar(true);
			if (c == '0')
			{
				c = readChar(true);
				while (Character.isWhitespace((char)c))
					c = readChar(true);
				if (c == 'R')
				{
					int ref = Integer.parseInt(sb.toString());
					retval = new PDFObject(ref);
				}
				else
				{
					retval = new PDFObject(sb.toString());
					m_pdfFile.seek(offset);
				}
			}
			else
			{
				retval = new PDFObject(sb.toString());
				m_pdfFile.seek(offset);
			}
		}
		return(retval);
	}

	/**
	 * Read object header from current position in PDF file.
	 * @return object number.
	 */
	private int readObjectBegin() throws IOException, MapyrusException
	{
		/*
		 * Read object header with format '17 0 obj' from PDF file.
		 */
		int id;
		StringBuffer sb = new StringBuffer();
		int c = readChar(true);
		while (!Character.isWhitespace((char)c))
		{
			sb.append((char)c);
			c = readChar(true);
		}
		try
		{
			id = Integer.parseInt(sb.toString());
		}
		catch (NumberFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
				": " + m_filename + ": " + sb.toString());
		}

		for (int i = 0; i < 2; i++)
		{
			while (Character.isWhitespace((char)c))
				c = readChar(true);
			while (!Character.isWhitespace((char)c))
				c = readChar(true);
		}
		return(id);
	}

	/**
	 * Read keywords marking the stream for object.
	 * @return file offset of stream for this object.
	 */
	public long readObjectEnd() throws IOException, MapyrusException
	{
		StringBuffer sb = new StringBuffer();
		
		/*
		 * Skip whitespace.
		 */
		int c = readChar(true);
		while (Character.isWhitespace((char)c))
			c = readChar(true);

		/*
		 * Read 'stream' or 'endobj' keyword.
		 */
		while (!Character.isWhitespace((char)c))
		{
			sb.append((char)c);
			c = readChar(true);
		}
		if (c == '\r')
			readChar(false);
		long retval = -1;
		if (sb.toString().equals("stream"))
			retval = m_pdfFile.getFilePointer();
		return(retval);
	}

	public String getXObjects()
	{
		return("");
	}
	
	public static void main(String []args)
	{
		try
		{
			PDFFile pdf = new PDFFile("/home/schenery/text1.pdf");
			pdf.getXObjects();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
