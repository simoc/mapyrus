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
	 * Determines if only one color is used in an image.
	 * @param image image to check.
	 * @return true if non-transparent color used in an image, or null if
	 * image has many colors.
	 */
	private static boolean isSingleColorImage(BufferedImage image)
	{
		Color singleColor = Color.BLACK;
		boolean foundDifferentColors = false;
		boolean foundFirstColor = false;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		/*
		 * Check if all pixels are the same color, or transparent.
		 */
		int y = 0;
		while (y < imageHeight && (!foundDifferentColors))
		{
			int x = 0;
			while (x < imageWidth && (!foundDifferentColors))
			{
				int pixel = image.getRGB(x, y);
				if ((pixel & 0xff000000) != 0)
				{
					/*
					 * Pixel is not transparent.
					 */
					if (!foundFirstColor)
					{
						foundFirstColor = true;
						singleColor = new Color(pixel & 0xffffff);
					}
					else
					{
						foundDifferentColors = (pixel != singleColor.getRGB());
					}
				}
				x++;
			}
			y++;
		}

		return(!foundDifferentColors);
	}

	/**
	 * Read an image from a file. 
	 * @param f file to read image from.
	 * @param color color for monochrome images.
	 * @return image read from file.
	 */
	public static ColorIcon read(File f, Color color) throws IOException, MapyrusException
	{
		ColorIcon retval = null;
		BufferedImage image = null;

		try
		{
			String filename = f.getName().toLowerCase();
			if (filename.endsWith(".ppm") || filename.endsWith(".pgm") || filename.endsWith(".pbm"))
				image = new PNMImage(f.toString()).getBufferedImage();
			else if (filename.endsWith(".pat"))
				image = new PATImage(f.toString()).getBufferedImage();
			else if (filename.endsWith(".xbm"))
				image = new XBMImage(f.toString(), color.getRGB()).getBufferedImage();
			else
				image = ImageIO.read(f);

			if (image != null)
			{	
				/*
				 * Check if image is a single color.
				 * Some output formats have more efficient rendering for single color images.
				 */
				if (!isSingleColorImage(image))
					color = null;
				retval = new ColorIcon(image, color);
			}
			else
			{
				retval = null;
			}
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + f.getPath());
		}
		return(retval);
	}

	/**
	 * Read an image from URL. 
	 * @param url URL to read image from.
	 * @param color color for monochrome images.
	 * @return image read from URL.
	 */
	public static ColorIcon read(URL url, Color color) throws IOException, MapyrusException
	{
		ColorIcon retval;
		InputStream stream = null;
		BufferedImage image = null;

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
				StringBuilder sb = new StringBuilder();
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
				image = new PNMImage(stream, filename).getBufferedImage();
			}
			else if (contentType.equals("image/x-xbm"))
			{
				image = new XBMImage(stream, filename, color.getRGB()).getBufferedImage();
			}
			else
			{
				image = ImageIO.read(stream);
			}
			
			if (image != null)
			{	
				/*
				 * Check if image is a single color.
				 * Some output formats have more efficient rendering for single color images.
				 */
				if (!isSingleColorImage(image))
					color = null;
				retval = new ColorIcon(image, color);
			}
			else
			{
				retval = null;
			}
		}
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + url.toString());
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
	 * @param f file to write image to.
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
		catch (SecurityException e)
		{
			throw new IOException(e.getClass().getName() + ": " + e.getMessage() + ": " + f.getPath());
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
		String []retval = new String[names.length + 1];
		System.arraycopy(names, 0, retval, 0, names.length);
		retval[names.length] = "ppm";
		return(retval);
	}
}
