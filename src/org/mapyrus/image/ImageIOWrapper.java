/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.mapyrus.MapyrusException;

/**
 * Wrapper around javax.imageio.ImageIO class to provide reading and
 * writing of additional image formats.
 */
public class ImageIOWrapper
{
	/**
	 * Read an image from a file. 
	 * @param file file to read image from.
	 * @return image read from file.
	 */
	public static BufferedImage read(File f) throws IOException, MapyrusException
	{
		BufferedImage retval;

		String filename = f.getName().toLowerCase();
		if (filename.endsWith(".ppm") || filename.endsWith(".pgm") || filename.endsWith(".pbm"))
			retval = new PNMImage(f.toString()).getBufferedImage();
		else
			retval = ImageIO.read(f);
		return(retval);
	}
	
	/**
	 * Read an image from URL. 
	 * @param url URL to read image from.
	 * @return image read from URL.
	 */
	public static BufferedImage read(URL url) throws IOException, MapyrusException
	{
		BufferedImage retval;

		String filename = url.getPath().toLowerCase();
		if (filename.endsWith(".ppm") || filename.endsWith(".pgm") || filename.endsWith(".pbm"))
			retval = new PNMImage(url).getBufferedImage();
		else
			retval = ImageIO.read(url);
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
