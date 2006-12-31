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
package org.mapyrus.image;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/*
 * Holds an image read from a Windows 3.0 BMP format file.
 */
public class BMPImage
{
	/*
	 * BMP image compression types. 
	 */
	private static final int BI_RGB = 0;
	private static final int BI_RLE8 = 1;
	private static final int BI_RLE4 = 2;

	private BufferedImage mImage;
	private int[] mColorTable;

	/**
	 * Read BMP image from URL.
	 * @param url URL.
	 */
	public BMPImage(URL url) throws MapyrusException, IOException
	{
		DataInputStream stream = new DataInputStream(new BufferedInputStream(url.openStream()));
		init(stream, url.toString());
	}

	/**
	 * Read BMP image from file.
	 * @param filename name of file.
	 */
	public BMPImage(String filename) throws MapyrusException, IOException
	{
		InputStream stream = new FileInputStream(filename);
		init(stream, filename);
	}

	/**
	 * Read BMP image from open stream.
	 * @param stream open stream to read from.
	 * @param filename name of file.
	 */
	public BMPImage(InputStream stream, String filename) throws MapyrusException, IOException
	{
		BufferedInputStream stream2 = new BufferedInputStream(stream);
		init(stream2, filename);
	}

	private void init(InputStream stream, String filename) throws MapyrusException, IOException
	{
		try
		{
			byte headerBuf[] = new byte[14];
			if (stream.read(headerBuf) != headerBuf.length)
			{
				throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + filename);
			}
			ByteBuffer fileHeader = ByteBuffer.wrap(headerBuf);
			fileHeader.order(ByteOrder.LITTLE_ENDIAN);

			/*
			 * Check for 'BM' magic number at start of file.
			 */
			int magic1 = fileHeader.get();
			int magic2 = fileHeader.get();

			if (!(magic1 == 'B' && magic2 == 'M'))
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_BMP_FILE) +
					": " + filename);
			}
			int pixelsOffset = fileHeader.getInt(10);

			byte infoBuf[] = new byte[40];
			if (stream.read(infoBuf) != infoBuf.length)
			{
				throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + filename);
			}
			ByteBuffer infoHeader = ByteBuffer.wrap(infoBuf);
			infoHeader.order(ByteOrder.LITTLE_ENDIAN);

			int nInfoBytes = infoHeader.getInt();
			int width = infoHeader.getInt();
			int height = infoHeader.getInt();
			infoHeader.getShort(); /* planes */
			int bitDepth = infoHeader.getShort();
			int compression = infoHeader.getInt();
			infoHeader.getInt(); /* size image */
			infoHeader.getInt(); /* x pixels per meter */
			infoHeader.getInt(); /* y pixels per meter */
			int nColorsUsed = infoHeader.getInt();

			/*
			 * We currently only read uncompressed images because that is the only
			 * type in common use.
			 */
			if (compression != BI_RGB)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_BMP_FILE) +
					": " + filename);
			}
			int currentOffset = headerBuf.length + infoBuf.length;

			/*
			 * Read color table for color palette images.
			 */
			while (currentOffset < headerBuf.length + nInfoBytes)
			{
				if (stream.read() == -1)
				{
					throw new EOFException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + filename);
				}
				currentOffset++;
			}

			if (bitDepth != 24)
			{
				int nColors = 1 << bitDepth;
				mColorTable = new int[nColors];
				if (nColorsUsed != 0)
					nColors = Math.min(nColors, nColorsUsed);
				for (int i = 0; i < nColors && currentOffset < pixelsOffset; i++)
				{
					int blue = stream.read();
					int green = stream.read();
					int red = stream.read();
					stream.read();
					int pixel = (red << 16) | (green << 8) | blue;
					mColorTable[i] = pixel;
					currentOffset += 4;
				}
			}

			/*
			 * Skip to start of pixel data in image file.
			 */
			while (currentOffset < pixelsOffset)
			{
				stream.read();
				currentOffset++;
			}

			/*
			 * Create image and read pixel values into it.
			 */
			mImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			/*
			 * Calculate length of row, padded it to fill an even number of 4 byte blocks. 
			 */
			int rowLength = ((width * bitDepth) + 7) / 8;
			rowLength = ((rowLength + 3) / 4) * 4;
			int []rowBuf = new int[rowLength];
			for (int y = 0; y < height; y++)
			{
				for (int i = 0; i < rowLength; i++)
					rowBuf[i] = stream.read();

				/*
				 * Unpack row, calculating RGB value for each pixel.
				 */
				for (int x = 0; x < width; x++)
				{
					int pixel = 0;
					switch (bitDepth)
					{
						case 1:
							pixel = rowBuf[x / 8];
							int shift = 7 - (x % 8);
							pixel = pixel >> shift;
							pixel = (pixel & 0x1);
							pixel = mColorTable[pixel];
							break;
						case 4:
							pixel = rowBuf[x / 2];
							if (x % 2 == 0)
								pixel = pixel >> 4;
							pixel = (pixel & 0xf);
							pixel = mColorTable[pixel];
							break;
						case 8:
							pixel = rowBuf[x];
							pixel = mColorTable[pixel];
							break;
						case 24:
							int blue = rowBuf[x * 3];
							int green = rowBuf[x * 3 + 1];
							int red = rowBuf[x * 3 + 2];
							pixel = (red << 16) | (green << 8) | blue;
							break;
					}
					mImage.setRGB(x, height - 1 - y, pixel);
				}
			}
		}
		finally
		{
			try
			{
				stream.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Get BMP image as buffered image.
	 * @return image.
	 */
	public BufferedImage getBufferedImage()
	{
		return(mImage);
	}

	/**
	 * Write BMP image to a stream. 
	 * @param image image to write
	 * @param stream stream to write image to.
	 */
	public static void write(BufferedImage image, OutputStream stream)
		throws IOException
	{
		ByteBuffer fileHeader = ByteBuffer.allocate(14);
		fileHeader.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer infoHeader = ByteBuffer.allocate(40);
		infoHeader.order(ByteOrder.LITTLE_ENDIAN);

		int height = image.getHeight();
		int width = image.getWidth();
		int rowLength = ((width * 3 + 3) / 4) * 4;
		int fileSize = fileHeader.capacity() + infoHeader.capacity() +
			rowLength * height;

		BufferedOutputStream outStream = new BufferedOutputStream(stream);

		/*
		 * Write BMP file header section.
		 */
		fileHeader.put((byte)'B');
		fileHeader.put((byte)'M');
		fileHeader.putInt(fileSize);
		fileHeader.putShort((short)0);
		fileHeader.putShort((short)0);
		fileHeader.putInt(fileHeader.capacity() + infoHeader.capacity());
		fileHeader.rewind();
		outStream.write(fileHeader.array());

		/*
		 * Write file information section.
		 */
		infoHeader.putInt(infoHeader.capacity());
		infoHeader.putInt(width);
		infoHeader.putInt(height);
		infoHeader.putShort((short)1);
		infoHeader.putShort((short)24);
		infoHeader.putInt(BI_RGB);
		infoHeader.putInt(0);
		int pixelsPerM = (int)Math.round(1000.0 /
			Constants.MM_PER_INCH * Constants.getScreenResolution());
		infoHeader.putInt(pixelsPerM);
		infoHeader.putInt(pixelsPerM);
		infoHeader.putInt(0);
		infoHeader.putInt(0);
		infoHeader.rewind();
		outStream.write(infoHeader.array());

		/*
		 * Write pixel values.
		 */
		for (int y = 0; y < height; y++)
		{
			int x = 0;
			while (x < width)
			{
				int pixel = image.getRGB(x, height - y - 1);
				int red = ((pixel >> 16) & 0xff);
				int green = ((pixel >> 8) & 0xff);
				int blue = (pixel & 0xff);
				outStream.write(blue);
				outStream.write(green);
				outStream.write(red);
				x++;
			}

			/*
			 * Pad each row to even number of 4 byte integers.
			 */
			int padding = rowLength - x * 3;
			while (padding-- > 0)
				outStream.write(0);
		}
		outStream.flush();
	}
}
