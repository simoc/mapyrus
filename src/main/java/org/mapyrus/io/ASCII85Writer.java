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

package org.mapyrus.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.zip.Deflater;

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
public class ASCII85Writer implements AutoCloseable
{
	/*
	 * Bytes buffered but yet to be encoded.
	 */
	private int []m_unencodedBytes;
	private int m_nUnencodedBytes;

	/*
	 * Encoded bytes ready to be written to file.
	 */
	private char []m_encodedChars;

	/*
	 * File to write bytes to and number of bytes written to current line of file. 
	 */
	private Writer m_writer;
	private int m_nCharsOnLine;
	private int m_nCharsWritten;

	/*
	 * Deflater to ZLIB compress data before converting it to ASCII85 stream.
	 */
	private Deflater m_deflater;
	private byte m_deflateBuffer[];
	private int m_nBytesBuffered;

	/**
	 * Create new ASCII85 filtered output stream.
	 * @param writer writer to build filter on top of.
	 * @param deflate if true then stream is ZLIB compressed too.
	 * @throws IOException if writing to writer fails.
	 */
	public ASCII85Writer(Writer writer, boolean deflate) throws IOException
	{
		m_unencodedBytes = new int[4];
		m_nUnencodedBytes = 0;
		m_encodedChars = new char[5];
		m_writer = writer;
		if (deflate)
		{
			m_deflater = new Deflater();
			m_deflateBuffer = new byte[512];
			m_nBytesBuffered = 0;
		}

		/*
		 * Begin each line with a space so that PostScript filters which
		 * commonly strip all lines beginning with a '%' comment character
		 * do not strip any of our ASCII85 characters. 
		 */
		m_writer.write(' ');
		m_nCharsOnLine = m_nCharsWritten = 1;
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
		l = ((long)m_unencodedBytes[0] << 24);
		l |= ((long)m_unencodedBytes[1] << 16);
		l |= ((long)m_unencodedBytes[2] << 8);
		l |= ((long)m_unencodedBytes[3]);

		if ((!isFinalSet) && l == 0)
		{
			m_writer.write('z');
			m_nCharsOnLine++;
			m_nCharsWritten++;
		}
		else
		{
			m_encodedChars[4] = (char)((l % 85) + '!');
			l /= 85;
			m_encodedChars[3] = (char)((l % 85) + '!');
			l /= 85;
			m_encodedChars[2] = (char)((l % 85) + '!');
			l /= 85;
			m_encodedChars[1] = (char)((l % 85) + '!');
			m_encodedChars[0] = (char)((l / 85) + '!');

			/*
			 * Length of final set of encoded bytes is one byte
			 * more than number of unencoded bytes.
			 */
			if (isFinalSet)
			{
				m_writer.write(m_encodedChars, 0, m_nUnencodedBytes + 1);
				m_nCharsWritten += m_nUnencodedBytes + 1;
			}
			else
			{
				m_writer.write(m_encodedChars);
				m_nCharsOnLine += m_encodedChars.length;
				m_nCharsWritten += m_encodedChars.length;

			}
		}

		/*
		 * Break lines so that they don't become too long.
		 */
		if (m_nCharsOnLine > 72)
		{
			m_writer.write(Constants.LINE_SEPARATOR + " ");
			m_nCharsOnLine = 0;
			m_nCharsWritten += Constants.LINE_SEPARATOR.length() + 1;
		}
	}

	/**
	 * Save next byte to ASCII85 buffer.
	 * @param b byte to write.
	 */
	private void save(int b) throws IOException
	{
		/*
		 * Save next byte.  When we've saved 4 bytes then we can convert
		 * them to a 5 character ASCII string and write this to underlying
		 * stream.
		 */
		m_unencodedBytes[m_nUnencodedBytes++] = b;
		if (m_nUnencodedBytes == m_unencodedBytes.length)
		{
			writeEncoded(false);
			m_nUnencodedBytes = 0;
		}
	}

	/**
	 * Write byte to stream.
	 * @param b byte to write.
	 * @throws IOException if writing to writer fails.
	 */
	public void write(int b) throws IOException
	{
		if (m_deflater != null)
		{
			/*
			 * Fill buffer with bytes for Deflate compression.
			 * Compress the buffer when it is full and write the
			 * compressed bytes to ASCII85 stream. 
			 */
			if (b >= 128)
				b = b - 256;
			m_deflateBuffer[m_nBytesBuffered++] = (byte)b;
			if (m_nBytesBuffered == m_deflateBuffer.length)
			{
				m_deflater.setInput(m_deflateBuffer);

				/*
				 * Java Deflate Compression appears to hold reference to array
				 * of bytes to compress so begin a new buffer to avoid
				 * overwriting it.
				 */
				m_deflateBuffer = new byte[m_deflateBuffer.length];
				m_nBytesBuffered = 0;

				/*
				 * ASCII85 encode any bytes that have finished being compressed.
				 */
				int nBytes;
				while ((nBytes = m_deflater.deflate(m_deflateBuffer)) > 0)
				{
					for (int i = 0; i < nBytes; i++)
						save(m_deflateBuffer[i] & 0xff);
				}
			}
		}
		else
		{
			save(b);
		}
	}

	/**
	 * Flush and close this writer, without closing the underlying writer.
	 * @throws IOException if writing to writer fails.
	 */
	public void close() throws IOException
	{
		if (m_deflater != null)
		{
			/*
			 * Compress and write out any remaining bytes.
			 */
			if (m_nBytesBuffered > 0)
				m_deflater.setInput(m_deflateBuffer, 0, m_nBytesBuffered);
			m_deflater.finish();

			m_deflateBuffer = new byte[m_deflateBuffer.length];
			while (!m_deflater.finished())
			{
				int nBytes = m_deflater.deflate(m_deflateBuffer);
				for (int i = 0; i < nBytes; i++)
					save(m_deflateBuffer[i] & 0xff);
			}
		}

		/*
		 * Complete any group of 4 bytes we were in the middle of writing.
		 */
		if (m_nUnencodedBytes > 0)
		{
			for (int i = m_nUnencodedBytes; i < m_unencodedBytes.length; i++)
				m_unencodedBytes[i] = 0;
			writeEncoded(true);
		}
	}

	/**
	 * Get number of encoded chars written during encoding process.
	 * @return number of chars.
	 */
	public int getEncodedLength()
	{
		return(m_nCharsWritten);
	}

	static public void main(String args[])
	{
		String message = "The quick brown fox jumped over the lazy dog.";

		try (PrintWriter writer = new PrintWriter(new FileWriter("/tmp/ascii85.txt")))
		{
			try (ASCII85Writer ascii85 = new ASCII85Writer(writer, false))
			{
				byte []messageBytes = message.getBytes();
				for (int j = 0; j < 100; j++)
					for (int i = 0; i < messageBytes.length; i++)
						ascii85.write(messageBytes[i]);
			}
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
		}
	}
}
