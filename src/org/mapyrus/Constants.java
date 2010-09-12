/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
package org.mapyrus;

import java.awt.Toolkit;
import java.awt.HeadlessException;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Globally useful constants including fixed distance measurements.
 */
public class Constants
{
	public static final String PROGRAM_NAME = "Mapyrus";
	public static final String WEB_SITE = "http://mapyrus.sourceforge.net";
	
	/**
	 * Return version number of software.
	 * @return version number.
	 */
	public static String getVersion()
	{
		/*
		 * Current software version number set by ant Replace task during build.
		 */
		return("1.007");
	}

	/**
	 * Return release date of software.
	 * @return release date.
	 */
	public static String getReleaseDate()
	{
		/*
		 * Release date set by ant Replace task during build.
		 */
		return("12-September-2010");
	}

	/**
	 * Return license terms of software.
	 * @return licence terms.
	 */
	public static String []getLicense()
	{
		String []license =
		{
			Constants.PROGRAM_NAME + " comes with ABSOLUTELY NO WARRANTY, not even for MERCHANTABILITY or",
			"FITNESS FOR A PARTICULAR PURPOSE.  You may redistribute copies of " + Constants.PROGRAM_NAME,
			"under the terms of the GNU Lesser General Public License.  For more",
			"information about these matters, see the file named COPYING.",
			"",
			"Report bugs to <simoc@users.sourceforge.net>."
		};
		return(license);
	}

	/*
	 * American number formatting used for all output, with '.'
	 * as the decimal separator.
	 */
	public static final DecimalFormatSymbols US_DECIMAL_FORMAT_SYMBOLS =
		new DecimalFormatSymbols(Locale.US); 

	/*
	 * Identity matrix for coordinate transformations.
	 */
	public static final AffineTransform IDENTITY_MATRIX = new AffineTransform();

	/*
	 * Maximum number of threads to run simultaneously handling HTTP requests.
	 */
	public static final int MAX_HTTP_THREADS = 8;

	/*
	 * Time in milliseconds to wait to begin handling an HTTP request.  If
	 * HTTP server is too busy to begin handling request within this time
	 * then request is cancelled and an error is returned to HTTP client.
	 */
	public static final int HTTP_TIMEOUT = 30 * 1000;

	/*
	 * Length of time in milliseconds that temporary files are retained
	 * when running as an HTTP server.
	 */
	public static final int HTTP_TEMPFILE_LIFESPAN = 300 * 1000;

	/*
	 * Timeout in milliseconds for socket communication between Mapyrus
	 * and HTTP client.  A read/write takes longer than this time fails. 
	 */
	public static final int HTTP_SOCKET_TIMEOUT = 30 * 1000;

 	/*
  	 * Maximum amount of time an HTTP or servlet request may take to
	 * complete. Requests will be stopped and return a failure if they
	 * run for longer than this time.
  	 */
  	public static final int MAX_HTTP_REQUEST_TIME = 30 * 1000;

	/*
	 * Timeout in seconds for connecting to RDBMS and for executing
	 * SQL queries.
	 */
	public static final int DB_CONNECTION_TIMEOUT = 30;

 	/*
 	 * Timeout in seconds after which an idle database connection
 	 * is closed and a new connection is made.
 	 * This prevents a socket timeout.
 	 */
 	public static final int DB_IDLE_TIMEOUT = 600;

	/*
	 * Maximum number of icons to cache in memory.
	 */
	public static final int ICON_CACHE_SIZE = 64;

	/*
	 * Number of points and millimetres per inch.
	 */
	public static final int POINTS_PER_INCH = 72;
	public static final double MM_PER_INCH = 25.4;

	private static int mScreenResolution;

	/**
	 * Get screen resolution, in dots per inch.
	 * @return DPI value. 
	 */
	public static final int getScreenResolution()
	{
		if (mScreenResolution == 0)
		{
			try
			{
				mScreenResolution = Toolkit.getDefaultToolkit().getScreenResolution();
			}
			catch (InternalError e)
			{
				/*
				 * No display, or bad display.  So use a reasonable default.
				 */
				mScreenResolution = 96;
			}
			catch (HeadlessException e)
			{
				/*
				 * No display, or bad display.  So use a reasonable default.
				 */
				mScreenResolution = 96;
			}
		}
		return(mScreenResolution);
	}

	/**
	 * Get screen width, in millimetres.
	 * @return screen width.
	 */
	public static final double getScreenWidth()
	{
		int resolution = getScreenResolution();
		double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		return(width / (double)resolution * MM_PER_INCH);
	}

	/**
	 * Get screen height, in millimetres.
	 * @return screen width.
	 */
	public static final double getScreenHeight()
	{
		int resolution = getScreenResolution();
		double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		return(height / (double)resolution * MM_PER_INCH);
	}

	/**
	 * Get name of operating system being used.
	 * @return name of operating system, or empty string if not available.
	 */
	public static String getOSName()
	{
		String retval = System.getProperty("os.name");
		if (retval == null)
			retval = "";
		else
			retval = retval.toUpperCase();
		return(retval);
	}

	/*
	 * Line separator in text files.
	 */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
}
