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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.mapyrus.Constants;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Wrapper around javax.imageio.ImageIO class to provide reading and
 * writing of additional image formats.
 */
public class ImageIOWrapper
{
	/**
	 * Read an image from a file. 
	 * @param file file to read image from.
	 * @param color color for monochrome images.
	 * @return image read from file.
	 */
	public static BufferedImage read(File f, Color color) throws IOException, MapyrusException
	{
		BufferedImage retval;

		String filename = f.getName().toLowerCase();
		if (filename.endsWith(".ppm") || filename.endsWith(".pgm") || filename.endsWith(".pbm"))
			retval = new PNMImage(f.toString()).getBufferedImage();
		else if (filename.endsWith(".bmp"))
			retval = new BMPImage(f.toString()).getBufferedImage();
		else if (filename.endsWith(".pat"))
			retval = new PATImage(f.toString()).getBufferedImage();
		else if (filename.endsWith(".xbm"))
			retval = new XBMImage(f.toString(), color.getRGB()).getBufferedImage();
		else
			retval = ImageIO.read(f);
		return(retval);
	}

	/**
	 * Read an image from URL. 
	 * @param url URL to read image from.
	 * @param color color for monochrome images.
	 * @return image read from URL.
	 */
	public static BufferedImage read(URL url, Color color) throws IOException, MapyrusException
	{
		BufferedImage retval;
		InputStream stream = null;

		try
		{
			/*
			 * Check that URL really is an image before trying to load it.
			 */
			String filename = url.getPath().toLowerCase();
			URLConnection urlConnection = url.openConnection();
			String contentType = urlConnection.getContentType();
			stream = urlConnection.getInputStream();
			if (contentType.startsWith("text/") ||
				contentType.startsWith("application/vnd.ogc."))
			{
				/*
				 * Read textual content from URL and include it in our
				 * error message as it may be helpful in understanding the
				 * problem.
				 */
				StringBuffer sb = new StringBuffer();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				int c;
				while ((c = reader.read()) != -1)
				{
					sb.append((char)c);
					if (sb.length() >= 80 * 17)
					{
						sb.append("...");
						break;
					}
				}
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.URL_RETURNED) +
					": " + contentType + Constants.LINE_SEPARATOR +
					url.toString() + Constants.LINE_SEPARATOR + sb);
			}

			if (contentType.equals("image/x-portable-pixmap") ||
				contentType.equals("image/x-portable-bitmap") ||
				contentType.equals("image/x-portable-graymap"))
			{
				retval = new PNMImage(stream, filename).getBufferedImage();
			}
			else if (contentType.equals("image/bmp"))
			{
				retval = new BMPImage(stream, filename).getBufferedImage();
			}
			else if (contentType.equals("image/x-xbm"))
			{
				retval = new XBMImage(stream, filename, color.getRGB()).getBufferedImage();
			}
			else
			{
				retval = ImageIO.read(stream);
			}
		}
		finally
		{
			try
			{
				if (stream != null)
					stream.close();
			}
			catch (IOException ignore)
			{
				
			}
		}
		return(retval);
	}
	
	/**
	 * Write an image to a file. 
	 * @param image image to write.
	 * @param format name of image format in which to write image. 
	 * @param file file to write image to.
	 */
	public static void write(BufferedImage image, String format, File f)
		throws IOException
	{
		BufferedOutputStream stream = null;
		try
		{
			stream = new BufferedOutputStream(new FileOutputStream(f));
			write(image, format, stream);
		}
		finally
		{
			if (stream != null)
				stream.close();
		}
	}

	/**
	 * Write an image to a stream. 
	 * @param image image to write
	 * @param format name of image format in which to write image. 
	 * @param stream stream to write image to.
	 */
	public static void write(BufferedImage image, String format, OutputStream stream)
		throws IOException
	{
		if (format.equalsIgnoreCase("ppm"))
			PNMImage.write(image, stream);
		else if (format.equalsIgnoreCase("bmp"))
			BMPImage.write(image, stream);
		else
			ImageIO.write(image, format, stream);
	}

	/**
	 * Get list of image format names that can be read and written.
	 * @return list of formats.
	 */
	public static String []getWriterFormatNames()
	{
		String []names = ImageIO.getWriterFormatNames();
		String []retval = new String[names.length + 2];
		System.arraycopy(names, 0, retval, 0, names.length);
		retval[names.length] = "ppm";
		retval[names.length + 1] = "bmp";
		return(retval);
	}
}
