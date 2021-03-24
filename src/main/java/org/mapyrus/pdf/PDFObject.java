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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.pdf;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.io.ASCII85Writer;

/**
 * An object in a PDF file.
 */
public class PDFObject
{
	private String m_value = null;
	private int m_reference = -1;
	private PDFObject[] m_array = null;
	private HashMap<String, PDFObject> m_dictionary = null;
	private long m_streamOffset = -1;

	/**
	 * Create new PDF object.
	 * @param simple object such as number or string.
	 */
	public PDFObject(String value)
	{
		m_value = value;
	}

	/**
	 * Create new PDF array object.
	 * @param array of objects.
	 */
	public PDFObject(PDFObject []array)
	{
		m_array = array;
	}

	/**
	 * Create new PDF dictionary object.
	 * @param dictionary of key, value pairs.
	 */
	public PDFObject(HashMap<String, PDFObject> dictionary)
	{
		m_dictionary = dictionary;
	}

	/**
	 * Create new PDF reference object.
	 * @param reference reference number of other object.
	 */
	public PDFObject(int reference)
	{
		m_reference = reference;
	}

	/**
	 * Set file offset of stream for this object.
	 * @param offset file offset.
	 */
	public void setStreamOffset(long offset)
	{
		m_streamOffset = offset;
	}

	/**
	 * Get value for object.
	 * @return simple value stored for this object.
	 */
	public String getValue()
	{
		return(m_value);
	}

	/**
	 * Set object to be the same as another object
	 * @param anotherObject object to copy.
	 */
	public void setValue(PDFObject otherObject)
	{
		m_value = otherObject.m_value;
		m_reference = otherObject.m_reference;
		m_array = otherObject.m_array;
		m_dictionary = otherObject.m_dictionary;
		m_streamOffset = otherObject.m_streamOffset;
	}

	/**
	 * Check if this object is an array.
	 * @return true if object is an array.
	 */
	public boolean isArray()
	{
		return(m_array != null);
	}

	/**
	 * Get array for object.
	 * @return array stored for this object.
	 */
	public PDFObject[] getArray()
	{
		return(m_array);
	}

	/**
	 * Check if this object is a dictionary.
	 * @return true if object is a dictionary.
	 */
	public boolean isDictionary()
	{
		return(m_dictionary != null);
	}

	/**
	 * Get dictionary for object.
	 * @return hash map containing key, value pairs.
	 */
	public HashMap<String, PDFObject> getDictionary()
	{
		return(m_dictionary);
	}

	/**
	 * Check if this object is a reference to another object.
	 * @return true if object is a reference.
	 */
	public boolean isReference()
	{
		return(m_reference != -1);
	}

	/**
	 * Get reference for object.
	 * @return reference to other object.
	 */
	public int getReference()
	{
		return(m_reference);
	}

	/**
	 * Get file offset for stream for this object.
	 * @return file offset.
	 */
	public long getStreamOffset()
	{
		return(m_streamOffset);
	}

	/**
	 * Get object and all references in format for inclusion in a PDF file.
	 * @param objectNumber first object number to use in PDF file.
	 * @param addObjectHeader if true then object header included in PDF object.
	 * @param addDictionaryMarkers if true then << and >> tokens added around PDF dictionary object.
	 * @param pdfObjects table of all objects for resolving references.
	 * @param pdfFile PDF file to read streams from.
	 * @param filename name of PDF file being read.
	 * @return object and its referenced objects as a list of StringBuffers.
	 */
	public ArrayList<StringBuffer> toPDFString(int objectNumber, boolean addObjectHeader,
		boolean addDictionaryMarkers, HashMap<Integer, PDFObject> pdfObjects, RandomAccessFile pdfFile,
		String filename) throws IOException, MapyrusException
	{
		StringBuffer sb = new StringBuffer();
		ArrayList<StringBuffer> retval = new ArrayList<StringBuffer>();

		if (addObjectHeader)
		{
			sb.append(objectNumber).append(" 0 obj\r\n");
			objectNumber++;
		}
		if (m_value != null)
		{
			sb.append(m_value).append("\r\n");
		}
		else if (m_array != null)
		{
			sb.append("[");
			for (int i = 0; i < m_array.length; i++)
			{
				ArrayList<StringBuffer> referencedObjects = m_array[i].toPDFString(objectNumber + retval.size(),
					false, true, pdfObjects, pdfFile, filename);

				sb.append(referencedObjects.get(0).toString());
				if (referencedObjects.size() > 1)
				{
					/*
					 * Add other objects referenced by this array element.
					 */
					for (int j = 1; j < referencedObjects.size(); j++)
						retval.add(referencedObjects.get(j));
				}
			}
			sb.append("]\r\n");
		}
		else if (m_dictionary != null)
		{
			if (addDictionaryMarkers)
				sb.append("<<\r\n");
			for (String key : m_dictionary.keySet())
			{
				PDFObject value = (PDFObject)m_dictionary.get(key);
				if (!(key.equals("/Length") || key.equals("/Filter")))
				{
					sb.append(key);
					sb.append(" ");

					ArrayList<StringBuffer> referencedObjects = value.toPDFString(objectNumber + retval.size(),
						false, true, pdfObjects, pdfFile, filename);

					sb.append(referencedObjects.get(0).toString());
					if (referencedObjects.size() > 1)
					{
						/*
						 * Add other objects referenced by this array element.
						 */
						for (int j = 1; j < referencedObjects.size(); j++)
							retval.add(referencedObjects.get(j));
					}
				}
			}
		}
		else if (m_reference >= 0)
		{
			/*
			 * Add reference to object and then add the referenced object.
			 */
			sb.append(objectNumber + retval.size()).append(" 0 R\r\n");
			PDFObject referencedObject = (PDFObject)pdfObjects.get(Integer.valueOf(m_reference));
			ArrayList<StringBuffer> referencedObjects = referencedObject.toPDFString(objectNumber + retval.size(),
				true, true, pdfObjects, pdfFile, filename);
			retval.addAll(referencedObjects);
		}

		/*
		 * Include any stream for the object too.
		 */
		if (m_streamOffset >= 0)
		{
			byte []buf = getStream(pdfFile, filename, pdfObjects);
			StringWriter sw = new StringWriter(buf.length);
			PrintWriter pw = new PrintWriter(sw);
			ASCII85Writer ascii85 = new ASCII85Writer(pw, true);
			for (int i = 0; i < buf.length; i++)
				ascii85.write(buf[i] & 0xFF);
			ascii85.close();
			pw.write("~>\r\n");
			pw.flush();
			buf = null;

			StringBuffer encodedBuf = sw.getBuffer();
			sb.append("/Filter [/ASCII85Decode /FlateDecode]\r\n");
			sb.append("/Length ").append(encodedBuf.length());
			sb.append("\r\n");
			sb.append(">>\r\n");
			sb.append("stream\r\n");
			sb.append(encodedBuf);
			sb.append("endstream\r\n");
		}
		else if (m_dictionary != null)
		{
			if (addDictionaryMarkers)
				sb.append(">>\r\n");
		}
		if (addObjectHeader)
			sb.append("endobj\r\n");

		retval.add(0, sb);
		return(retval);
	}

	/**
	 * Get stream of for this object.
	 * @param pdfFile PDF file to read from.
	 * @param filename filename of PDf file.
	 * @param pdfObjects offsets of all PDf objects in PDF file.
	 * @return decoded stream.
	 */
	public byte[] getStream(RandomAccessFile pdfFile, String filename,
		HashMap<Integer, PDFObject> pdfObjects) throws IOException, MapyrusException
	{
		PDFObject value = (PDFObject)m_dictionary.get("/Length");
		if (value.isReference())
			value = (PDFObject)pdfObjects.get(Integer.valueOf(value.m_reference));
		int streamLength = Integer.parseInt(value.m_value);

		PDFObject filter = (PDFObject)m_dictionary.get("/Filter");
		if (value.isReference())
			value = (PDFObject)pdfObjects.get(Integer.valueOf(value.m_reference));

		byte []buf = new byte[streamLength];
		pdfFile.seek(m_streamOffset);
		pdfFile.readFully(buf);

		if (filter != null)
		{
			/*
			 * Build list of filters to use to decode stream.
			 */
			String[] filterNames;
			if (!filter.isArray())
			{
				filterNames = new String[]{filter.getValue()};
			}
			else
			{
				PDFObject[] filterArray = filter.getArray();
				filterNames = new String[filterArray.length];
				for (int i = 0; i < filterArray.length; i++)
				{
					filter = filterArray[i];
					if (filter.isReference())
						filter = (PDFObject)pdfObjects.get(Integer.valueOf(filter.getReference()));
					filterNames[i] = filter.getValue();
				}
			}
			
			/*
			 * Decode stream using each filter in turn.
			 */
			for (int i = 0; i < filterNames.length; i++)
			{
				if (filterNames[i].equals("/FlateDecode"))
				{
					buf = decodeDeflatedBytes(buf, filename);
				}
				else if (filterNames[i].equals("/ASCII85Decode"))
				{
					buf = decodeASCII85Bytes(buf);
				}
				else if (filterNames[i].equals("/ASCIIHexDecode"))
				{
					buf = decodeHexBytes(buf);
				}
				else
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.EXTENDED_PDF) +
						": " + filename + ": " + filterNames[i]);
				}
			}
		}
		return(buf);
	}
	
	/**
	 * Decode deflated bytes.
	 * @param buf bytes to uncompress.
	 * @param filename name of PDF file being read.
	 * @return uncompressed bytes.
	 */
	private byte[] decodeDeflatedBytes(byte []buf, String filename) throws MapyrusException
	{
		Inflater inflater = new Inflater();
		inflater.setInput(buf);
		int count = 0;
		buf = new byte[buf.length * 5];
		try
		{
			while(!inflater.finished())
			{
				int nBytes = inflater.inflate(buf, count, buf.length - count);
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
				": " + filename + ": " + e.getMessage());
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
			j++;
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
}
