/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Filters an java.io.OutputStream, converting bytes to an ASCII85
 * representation of data, using only 85 printable ASCII characters.
 * Each set of 4 bytes written to this stream is converted into
 * 5 ASCII characters.
 *
 * Implements algorithm described in section 3.13.3, 'ASCII85EncodeFilter'
 * of Adobe PostScript Language Reference Manual (2nd Edition).
 */
public class ASCII85OutputStream extends FilterOutputStream
{
	/*
	 * Bytes buffered but yet to be encoded.
	 */
	private int []mUnencodedBytes;
	private int mNUnencodedBytes;

	/*
	 * Encoded bytes ready to be written to stream.
	 */
	private byte []mEncodedBytes;

	/**
	 * Create new ASCII85 filtered output stream.
	 * @param outStream stream to build filter on top of.
	 */
	public ASCII85OutputStream(OutputStream outStream)
	{
		super(outStream);
		mUnencodedBytes = new int[4];
		mNUnencodedBytes = 0;
		mEncodedBytes = new byte[5];
	}

	public void flush()
	{
	}

	public void write(byte []b) throws IOException
	{
		for (int i = 0; i < b.length; i++)
			write(b[i]);
	}

	public void write(byte []b, int off, int len) throws IOException
	{
		for (int i = 0; i < len; i++)
			write(b[off + i]);
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
			out.write('z');
		}
		else
		{
			mEncodedBytes[4] = (byte)((l % 85) + '!');
			l /= 85;
			mEncodedBytes[3] = (byte)((l % 85) + '!');
			l /= 85;
			mEncodedBytes[2] = (byte)((l % 85) + '!');
			l /= 85;
			mEncodedBytes[1] = (byte)((l % 85) + '!');
			mEncodedBytes[0] = (byte)((l / 85) + '!');

			/*
			 * Length of final set of encoded bytes is one byte
			 * more than number of unencoded bytes. 
			 */
			if (isFinalSet)
				out.write(mEncodedBytes, 0, mNUnencodedBytes + 1);
			else
				out.write(mEncodedBytes);
		}
	}

	/**
	 * Write byte to stream.
	 * @param b byte to write.
	 */
	public void write(byte b) throws IOException
	{
		write((int)b);
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
		out.close();
	}

	static public void main(String args[])
	{
		String message = "The quick brown fox jumped over the lazy dog.";
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ASCII85OutputStream ascii85 = new ASCII85OutputStream(b);
		try
		{
			ascii85.write(message.getBytes());
			ascii85.close();
			System.out.println("<~" + b.toString() + "~>");
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
		}
	}
}
