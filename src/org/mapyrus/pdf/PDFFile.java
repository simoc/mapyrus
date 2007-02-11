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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Provides functions for parsing PDF format files.
 */
public class PDFFile
{
	private String mFilename;
	private RandomAccessFile mPdfFile;
	private HashMap mObjects;
	private ArrayList mPageObjects;

	public PDFFile(String filename) throws IOException, MapyrusException
	{
		try
		{
			/*
			 * Open file, find and read the 'xref' table at the end of the
			 * file giving the file offset of each object.
			 */
			mFilename = filename;
			mPdfFile = new RandomAccessFile(filename, "r");
			String header = mPdfFile.readLine();
			if (!header.startsWith("%PDF-"))
			{
				throw new MapyrusException("");
			}
	
			byte []xrefBuf = new byte[20];
			mPdfFile.seek(mPdfFile.length() - xrefBuf.length);
			mPdfFile.readLine();	/* skip line with 'startxref' keyword */
			String line = mPdfFile.readLine();
			long xrefOffset = Long.parseLong(line);

			HashMap objectOffsets = new HashMap();
			mPdfFile.seek(xrefOffset);
			PDFObject trailer = readXrefSection(objectOffsets);

			/*
			 * Read each of the objects in the PDF file. 
			 */
			mObjects = new HashMap(objectOffsets.size());
			Iterator it = objectOffsets.keySet().iterator();
			while (it.hasNext())
			{
				Integer key = (Integer)(it.next());
				Long objectOffset = (Long)objectOffsets.get(key);
				mPdfFile.seek(objectOffset.longValue());

				int id = readObjectBegin();
				if (id != key.intValue())
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
						": " + mFilename);
				}
				PDFObject obj = readObject();
				long streamOffset = readObjectEnd();
				obj.setStreamOffset(streamOffset);
	
				mObjects.put(key, obj);
			}

			/*
			 * Find root object containing reference to pages.
			 */
			PDFObject rootObject = getDictionaryValue(trailer, "/Root");
			PDFObject pagesObject = getDictionaryValue(rootObject, "/Pages");

			/*
			 * Make list of objects for each page.
			 */
			mPageObjects = buildPageObjectList(pagesObject);
		}
		catch(IOException e)
		{
			/*
			 * Ensure file is closed on error.
			 */
			close();
			throw(e);
		}
		catch (MapyrusException e)
		{
			close();
			throw(e);
		}
	}

	/**
	 * Build list of pages from object defining page layout.
	 * @param pageObject object for page or pages.
	 * @return list of objects, one for each page.
	 */
	private ArrayList buildPageObjectList(PDFObject pagesObject)
		throws MapyrusException
	{
		ArrayList retval = new ArrayList();
		PDFObject kidsObject = getDictionaryValue(pagesObject, "/Kids");
		PDFObject[] kidsArray = kidsObject.getArray();
		for (int i = 0; i < kidsArray.length; i++)
		{
			PDFObject kidObject = kidsArray[i];
			if (kidObject.isReference())
				kidObject = (PDFObject)mObjects.get(new Integer(kidObject.getReference()));
			PDFObject objectType = getDictionaryValue(kidObject, "/Type");
			if (objectType.getValue().equals("/Page"))
			{
				retval.add(kidObject);
			}
			else
			{
				retval.addAll(buildPageObjectList(kidObject));
			}
		}
		return(retval);
	}

	/**
	 * Read xref sections from PDF file.
	 * @param objectOffsets table to save offset of each object into.
	 */
	private PDFObject readXrefSection(HashMap objectOffsets)
		throws IOException, MapyrusException
	{
		String line = mPdfFile.readLine();	/* skip line with 'xref' keyword */
		line = mPdfFile.readLine();

		while (!line.equals("trailer"))
		{
			StringTokenizer st = new StringTokenizer(line);

			if (st.countTokens() < 2)
				throw new MapyrusException("");
			int startIndex = Integer.parseInt(st.nextToken());
			int count = Integer.parseInt(st.nextToken());
			
			for (int i = 0; i < count; i++)
			{
				line = mPdfFile.readLine();
				st = new StringTokenizer(line);
				if (st.countTokens() < 3)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
						": " + mFilename);
				}
				Long objOffset = Long.valueOf(st.nextToken());
				st.nextToken();	/* skip generation number */
				String status = st.nextToken();
				if (status.equals("n"))
				{
					/*
					 * Newest offsets were added first.
					 * Do not overwrite them with older values from
					 * previous 'xref' sections.
					 */
					Integer key = new Integer(i + startIndex);
					if (!objectOffsets.containsKey(key))
						objectOffsets.put(key, objOffset);
				}
			}
			line = mPdfFile.readLine();
		}

		/*
		 * Now read the trailer dictionary.
		 */
		PDFObject trailer = readObject();
		PDFObject prevObject = getDictionaryValue(trailer, "/Prev");
		if (prevObject != null)
		{
			/*
			 * Read any previous xref section in the PDF file too.
			 */
			String s = prevObject.getValue();
			long offset = Long.parseLong(s);
			mPdfFile.seek(offset);
			readXrefSection(objectOffsets);
		}
		return(trailer);
	}

	/**
	 * Close the PDF file.
	 */
	public void close()
	{
		if (mPdfFile != null)
		{
			try
			{
				mPdfFile.close();
			}
			catch (IOException e)
			{
			}
			mPdfFile = null;
		}
	}

	/**
	 * Get number of pages in PDF file.
	 * @return page count.
	 */
	public int getPageCount()
	{
		return(mPageObjects.size());
	}

	/**
	 * Get resources used by a page.
	 * @param page page number.
	 * @return resources used by page. 
	 */
	private PDFObject getResources(int page) throws MapyrusException
	{
		PDFObject pageObject = (PDFObject)mPageObjects.get(page - 1);
		PDFObject resourcesObject = getDictionaryValue(pageObject, "/Resources");
		return(resourcesObject);
	}

	/**
	 * Get dictionary containing external graphics states for page.
	 * @param page page number.
	 * @return dictionary containing graphics states.
	 */
	public PDFObject getExtGState(int page) throws IOException, MapyrusException
	{
		PDFObject resourcesObject = getResources(page);
		PDFObject extGStatesObject = getDictionaryValue(resourcesObject, "/ExtGState");
		resolveAllReferences(extGStatesObject);
		return(extGStatesObject);
	}

	/**
	 * Get dictionary containing external objects for page.
	 * @param page page number.
	 * @return dictionary containing objects.
	 */
	public PDFObject getXObject(int page) throws IOException, MapyrusException
	{
		PDFObject resourcesObject = getResources(page);
		PDFObject externalObject = getDictionaryValue(resourcesObject, "/XObject");
		return(externalObject);
	}

	/**
	 * Get media box for page.
	 * @param page page number.
	 * @return (x1, y1) and (x2, y2) coordinates of page in points.
	 */
	public int[] getMediaBox(int page) throws MapyrusException
	{
		PDFObject pageObject = (PDFObject)mPageObjects.get(page - 1);
		PDFObject boxObject = getDictionaryValue(pageObject, "/MediaBox");
		PDFObject[] boxArray = boxObject.getArray();
		int retval[] = new int[4];
		for (int i = 0; i < retval.length; i++)
		{
			PDFObject obj = boxArray[i];
			if (obj.isReference())
				obj = (PDFObject)mObjects.get(new Integer(obj.getReference()));
			String s = obj.getValue();
			retval[i] = Integer.parseInt(s);
		}
		return(retval);
	}

	/**
	 * Get contents of page.
	 * @param page page number.
	 * @return page contents.
	 */
	public byte []getContents(int page) throws IOException, MapyrusException
	{
		ArrayList byteBuffers = new ArrayList();
		PDFObject pageObject = (PDFObject)mPageObjects.get(page - 1);
		PDFObject contentsObject = getDictionaryValue(pageObject, "/Contents");

		if (contentsObject == null)
		{
			/*
			 * No contents.  Return nothing.
			 */
		}
		else if (contentsObject.isArray())
		{
			/*
			 * Contents are an array of objects.
			 */
			PDFObject[] contentsArray = contentsObject.getArray();
			for (int i = 0; i < contentsArray.length; i++)
			{
				byteBuffers.add(decodeStream(contentsArray[i]));
			}
		}
		else
		{
			byteBuffers.add(decodeStream(contentsObject));
		}

		/*
		 * Find total length of all contents for this page.
		 */
		int totalLength = 0;
		for (int i = 0; i < byteBuffers.size(); i++)
			totalLength += ((byte [])byteBuffers.get(i)).length;

		byte[] retval = null;
		if (totalLength > 0)
		{
			/*
			 * Join all the byte buffers together.
			 */
			retval = new byte[totalLength];
			int offset = 0;
			for (int i = 0; i < byteBuffers.size(); i++)
			{
				byte[] buf = (byte [])byteBuffers.get(i);
				System.arraycopy(buf, 0, retval, offset, buf.length);
				offset += buf.length;
			}
		}
		return(retval);
	}

	/**
	 * Decode content stream of page contents object.
	 * @param contentsObject page contents object.
	 * @return decoded stream.
	 */
	private byte[] decodeStream(PDFObject contentsObject)
		throws IOException, MapyrusException
	{
		PDFObject filterObject = getDictionaryValue(contentsObject, "/Filter");
		PDFObject contentsLengthObject = getDictionaryValue(contentsObject, "/Length");
		String s = contentsLengthObject.getValue();
		int streamLength = Integer.parseInt(s);
		byte []buf = new byte[streamLength];
		mPdfFile.seek(contentsObject.getStreamOffset());
		mPdfFile.readFully(buf);
	
		if (filterObject != null)
		{
			/*
			 * Build list of filters to use to decode stream.
			 */
			String[] filterNames;
			if (!filterObject.isArray())
			{
				filterNames = new String[]{filterObject.getValue()};
			}
			else
			{
				PDFObject[] filterArray = filterObject.getArray();
				filterNames = new String[filterArray.length];
				for (int i = 0; i < filterArray.length; i++)
				{
					filterObject = filterArray[i];
					if (filterObject.isReference())
						filterObject = (PDFObject)mObjects.get(new Integer(filterObject.getReference()));
					filterNames[i] = filterObject.getValue();
				}
			}
			
			/*
			 * Decode stream using each filter in turn.
			 */
			for (int i = 0; i < filterNames.length; i++)
			{
				if (filterNames[i].equals("/FlateDecode"))
				{
					buf = decodeDeflatedBytes(buf);
				}
				else if (filterNames[i].equals("/ASCII85Decode"))
				{
					buf = decodeASCII85Bytes(buf);
				}
				else if (filterNames[i].equals("/ASCIIHexDecode"))
				{
					buf = decodeHexBytes(buf);
				}
			}
		}
		return(buf);
	}
	
	/**
	 * Decode deflated bytes.
	 * @param buf bytes to uncompress.
	 * @return uncompressed bytes.
	 */
	private byte[] decodeDeflatedBytes(byte []buf) throws MapyrusException
	{
		Inflater inflater = new Inflater();
		inflater.setInput(buf);
		int count = 0;
		buf = new byte[buf.length * 5];
		try
		{
			while(!inflater.finished())
			{
				int nBytes = inflater.inflate(buf, count, 1 /*buf.length - count*/);
				count += nBytes;
				if (count == buf.length)
				{
					/*
					 * Array not long enough for uncompressed data.
					 * Make it bigger.
					 */
					byte[] newBuf = new byte[buf.length * 2];
					System.arraycopy(buf, 0, newBuf, 0, buf.length);
					buf = newBuf;
				}
			}
	
			/*
			 * Return array that is exactly the size of the decompressed data.
			 */
			byte[] newBuf = new byte[count];
			System.arraycopy(buf, 0, newBuf, 0, count);
			buf = newBuf;
		}
		catch (DataFormatException e)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
				": " + mFilename + ": " + e.getMessage());
		}
		return(buf);
	}
	
	/**
	 * Decode ASCII85 encoded bytes from PDF content stream.
	 * @param buf ASCII85 bytes to decode.
	 * @return decoded bytes.
	 */
	private byte[] decodeASCII85Bytes(byte []buf)
	{
		StringBuffer ascii85 = new StringBuffer();
		byte[] newBuf = new byte[buf.length];
		int j = 0;
		int nBytes = 0;
		while (j < buf.length && buf[j] != '~')
		{
			if (!Character.isWhitespace((char)buf[j]))
			{
				ascii85.append((char)buf[j]);
				if (ascii85.length() == 5)
				{
					byte[] decoded = decodeASCII85(ascii85.toString(), ascii85.length());
					System.arraycopy(decoded, 0, newBuf, nBytes, decoded.length);
					nBytes += decoded.length;
					ascii85.setLength(0);
				}
				else if (ascii85.charAt(0) == 'z')
				{
					for (int k = 0; k < 5; k++)
						newBuf[nBytes++] = 0;
					ascii85.setLength(0);
				}
			}
			j++;
		}
		if (ascii85.length() > 0)
		{
			/*
			 * Decompress final group of 1, 2, 3 or 4 characters.
			 */
			int finalLength = ascii85.length();
			while (ascii85.length() < 5)
				ascii85.append('!');
			byte[] decoded = decodeASCII85(ascii85.toString(), finalLength);
			System.arraycopy(decoded, 0, newBuf, nBytes, decoded.length);
			nBytes += decoded.length;
		}
		
		/*
		 * Return array that is exactly the size of the decompressed data.
		 */
		buf = new byte[nBytes];
		System.arraycopy(newBuf, 0, buf, 0, nBytes);
		return(buf);
	}
	
	/**
	 * Decode five ASCII85 encoded characters into 4 bytes.
	 * @param ascii85 five character string to decode.
	 * @return four decoded bytes.
	 */
	private byte[] decodeASCII85(String ascii85, int nChars)
	{
		/*
		 * Unpack up to 5 characters into 4 byte integer.
		 */
		int c = ascii85.charAt(0) - '!';
		long n = (c * 85 * 85 * 85 * 85);
	
		c = ascii85.charAt(1) - '!';
		n += (c * 85 * 85 * 85);
	
		if (nChars > 2)
		{
			c = ascii85.charAt(2) - '!';
			n += (c * 85 * 85);
		}
		if (nChars > 3)
		{
			c = ascii85.charAt(3) - '!';
			n += (c * 85);
		}
		if (nChars > 4)
		{
			c = ascii85.charAt(4) - '!';
			n += c;
		}
	
		byte[] buf = new byte[nChars - 1];
	
		/*
		 * Create 4 byte array from 4 byte integer.
		 */
		long b = ((n >> 24) & 255);
		if (b > 127)
			b = b - 256;
		buf[0] = (byte)b;
	
		if (nChars > 2)
		{
			b = ((n >> 16) & 255);
			if (b > 127)
				b = b - 256;
			buf[1] = (byte)b;
		}
	
		if (nChars > 3)
		{
			b = ((n >> 8) & 255);
			if (b > 127)
				b = b - 256;
			buf[2] = (byte)b;
		}
		
		if (nChars > 4)
		{
			b = (n & 255);
			if (b > 127)
				b = b - 256;
			buf[3] = (byte)b;
		}
		
		if (nChars < 5 && (n & 255) > 127)
		{
			/*
			 * Need to increment final digit because ASCII85 conversion loses
			 * some precision by not including all five characters in the set.
			 */
			b = (buf[nChars - 2] & 255);
			b++;
			if (b > 127)
				b = b - 256;
			buf[nChars - 2] = (byte)b;
			
		}
	
		return(buf);
	}
	
	/**
	 * Decode hex digits from PDF content stream.
	 * @param buf hex digits to decode.
	 * @return decoded hex bytes.
	 */
	private byte[] decodeHexBytes(byte []buf)
	{
		StringBuffer hex = new StringBuffer();
		byte[] newBuf = new byte[buf.length];
		int j = 0;
		int nBytes = 0;
		while (j < buf.length && buf[j] != '>')
		{
			if (Character.isLetterOrDigit((char)buf[j]))
			{
				hex.append((char)buf[j]);
				if (hex.length() == 2)
				{
					int k = Integer.parseInt(hex.toString(), 16);
					if (k > 127)
						k = k - 256;
					newBuf[nBytes++] = (byte)k;
					hex.setLength(0);
				}
			}
		}
		if (hex.length() == 1)
		{
			/*
			 * Decompress final single digit.
			 */
			hex.append('0');
			int k = Integer.parseInt(hex.toString(), 16);
			if (k > 127)
				k = k - 256;
			newBuf[nBytes++] = (byte)k;		
		}
	
		/*
		 * Return array that is exactly the size of the decompressed data.
		 */
		buf = new byte[nBytes];
		System.arraycopy(newBuf, 0, buf, 0, nBytes);
		return(buf);
	}

	/**
	 * Read next character from PDF file, skipping over comments.
	 * @param skipComments true if comments are to be skipped.
	 * @return next char, ignoring comments.
	 */
	private int readChar(boolean skipComments) throws IOException, MapyrusException
	{
		int c = mPdfFile.read();
		if (c == -1)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
				": " + mFilename);
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
		long offset = mPdfFile.getFilePointer();
		int c = readChar(skipComments);
		mPdfFile.seek(offset);
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
			long offset = mPdfFile.getFilePointer();
			
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
					mPdfFile.seek(offset);
				}
			}
			else
			{
				retval = new PDFObject(sb.toString());
				mPdfFile.seek(offset);
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
				": " + mFilename + ": " + sb.toString());
		}

		for (int i = 0; i < 2; i++)
		{
			while (Character.isWhitespace((char)c))
				c = readChar(true);

			c = peekChar(true);
			while ((!Character.isWhitespace((char)c)) && c != '<')
			{
				readChar(true);
				c = peekChar(true);
			}
		}
		return(id);
	}

	/**
	 * Read keywords marking the stream for object.
	 * @return file offset of stream for this object.
	 */
	private long readObjectEnd() throws IOException, MapyrusException
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
			retval = mPdfFile.getFilePointer();
		return(retval);
	}

	/**
	 * Lookup value in dictionary.
	 * @param obj dictionary object.
	 * @param key key to lookup in dictionary.
	 * @return value of key.
	 */
	private PDFObject getDictionaryValue(PDFObject dictObj, String key)
		throws MapyrusException
	{
		HashMap dict = dictObj.getDictionary();

		if (dict == null)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_PDF) +
				": " + mFilename);
		}
		PDFObject value = (PDFObject)dict.get(key);
		if (value != null && value.isReference())
		{
			value = (PDFObject)mObjects.get(new Integer(value.getReference()));
		}
		return(value);
	}

	/**
	 * Replace all references in an object by their actual values.
	 * @param obj object in which to replace values.
	 */
	private void resolveAllReferences(PDFObject obj)
		throws IOException, MapyrusException
	{
		if (obj.isDictionary())
		{
			Set keys = obj.getDictionary().keySet();
			Iterator it = keys.iterator();
			while (it.hasNext())
			{
				String key = (String)it.next();
				PDFObject value = (PDFObject)(obj.getDictionary().get(key));
				resolveAllReferences(value);
			}
		}
		else if (obj.isArray())
		{
			PDFObject[] arrayObjects = obj.getArray();
			for (int i = 0; i < arrayObjects.length; i++)
				resolveAllReferences(arrayObjects[i]);
		}
		else if (obj.isReference())
		{
			PDFObject value = (PDFObject)mObjects.get(new Integer(obj.getReference()));
			resolveAllReferences(value);
			obj.setValue(value);
		}
	}

	public static void main(String []args)
	{
		try
		{
			PDFFile pdf = new PDFFile("/tmp/text1.pdf");
			int nPages = pdf.getPageCount();
			for (int i = 1; i <= nPages; i++)
			{
				System.out.println("-- Page " + i);
				int[] box = pdf.getMediaBox(i);
				System.out.println("[" + box[0] + " " + box[1] + " " + box[2] + " " + box[3] + "]");
				System.out.println("-- ExtGState");
				System.out.println(pdf.getExtGState(i));
				System.out.println("-- XObject");
				System.out.println(pdf.getXObject(i));
				System.out.println("-- Contents");
				String contents = new String(pdf.getContents(i));
				System.out.println(contents);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
