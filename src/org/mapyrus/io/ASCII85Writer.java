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
package org.mapyrus.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.mapyrus.Constants;

/**
 * Converts bytes to ASCII85 representation and writes them to a file.
 * representation of data, using only 85 printable ASCII characters.
 * Each set of 4 bytes written to this stream is converted into
 * 5 ASCII characters.
 *
 * Implements algorithm described in section 3.13.3, 'ASCII85EncodeFilter'
 * of Adobe PostScript Language Reference Manual (2nd Edition).
 */
public class ASCII85Writer
{
	/*
	 * Bytes buffered but yet to be encoded.
	 */
	private int []mUnencodedBytes;
	private int mNUnencodedBytes;

	/*
	 * Encoded bytes ready to be written to file.
	 */
	private char []mEncodedChars;

	/*
	 * File to write bytes to and number of bytes written to current line of file. 
	 */
	private Writer mWriter;
	private int mNCharsOnLine;

	/**
	 * Create new ASCII85 filtered output stream.
	 * @param outStream stream to build filter on top of.
	 */
	public ASCII85Writer(Writer writer) throws IOException
	{
		mUnencodedBytes = new int[4];
		mNUnencodedBytes = 0;
		mEncodedChars = new char[5];
		mWriter = writer;

		/*
		 * Begin each line with a space so that PostScript filters which
		 * commonly strip all lines beginning with a '%' comment character
		 * do not strip any of our ASCII85 characters. 
		 */
		mWriter.write(' ');
		mNCharsOnLine = 1;
	}

	/**
	 * Convert 4 bytes to 5 byte ASCII85 encoded string and write it
	 * to underlying stream.
	 * @param isFinalSet flag true when final set of bytes are being written.
	 */
	private void writeEncoded(boolean isFinalSet) throws IOException
	{
		/*
		 * Pack 4 bytes together into an integer value.  Use a long
		 * to avoid problems with sign bit of integer.
		 */
		long l;
		l = ((long)mUnencodedBytes[0] << 24);
		l |= ((long)mUnencodedBytes[1] << 16);
		l |= ((long)mUnencodedBytes[2] << 8);
		l |= ((long)mUnencodedBytes[3]);

		if ((!isFinalSet) && l == 0)
		{
			mWriter.write('z');
			mNCharsOnLine++;
		}
		else
		{
			mEncodedChars[4] = (char)((l % 85) + '!');
			l /= 85;
			mEncodedChars[3] = (char)((l % 85) + '!');
			l /= 85;
			mEncodedChars[2] = (char)((l % 85) + '!');
			l /= 85;
			mEncodedChars[1] = (char)((l % 85) + '!');
			mEncodedChars[0] = (char)((l / 85) + '!');

			/*
			 * Length of final set of encoded bytes is one byte
			 * more than number of unencoded bytes.
			 */
			if (isFinalSet)
			{
				mWriter.write(mEncodedChars, 0, mNUnencodedBytes + 1);
			}
			else
			{
				mWriter.write(mEncodedChars);
				mNCharsOnLine += mEncodedChars.length;

			}
		}

		/*
		 * Break lines so that they don't become too long.
		 */
		if (mNCharsOnLine > 72)
		{
			mWriter.write(Constants.LINE_SEPARATOR + " ");
			mNCharsOnLine = 0;
		}
	}

	/**
	 * Write byte to stream.
	 * @param b byte to write.
	 */
	public void write(int b) throws IOException
	{
		/*
		 * Save next byte.  When we've saved 4 bytes then we can convert
		 * them to a 5 character ASCII string and write this to underlying
		 * stream.
		 */
		mUnencodedBytes[mNUnencodedBytes++] = b;
		if (mNUnencodedBytes == mUnencodedBytes.length)
		{
			writeEncoded(false);
			mNUnencodedBytes = 0;
		}
	}

	/**
	 * Flush and close this writer, without closing the underlying writer.
	 */
	public void close() throws IOException
	{
		/*
		 * Complete any group of 4 bytes we were in the middle of writing.
		 */
		if (mNUnencodedBytes > 0)
		{
			for (int i = mNUnencodedBytes; i < mUnencodedBytes.length; i++)
				mUnencodedBytes[i] = 0;
			writeEncoded(true);
		}
	}

	static public void main(String args[])
	{
		String message = "The quick brown fox jumped over the lazy dog.";

		try
		{
			PrintWriter writer = new PrintWriter(new FileWriter("ascii85.txt"));
			ASCII85Writer ascii85 = new ASCII85Writer(writer);
			byte []messageBytes = message.getBytes();
			for (int i = 0; i < messageBytes.length; i++)
				ascii85.write(messageBytes[i]);
			ascii85.close();
			writer.close();
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
		}
	}
}