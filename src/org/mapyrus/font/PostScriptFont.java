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
package org.mapyrus.font;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.io.IOException;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Constants;

/**
 * A PostScript Type 1 font, read from a .pfa font definition file.
 * Provides methods to parse the font file and include it in another
 * PostScript file.
 */
public class PostScriptFont
{
	/*
	 * Name of font given in header of font file.
	 */
	private String mFontName;

	/*
	 * Contents of font file.
	 */
	private StringBuffer mFileContents;

	/**
	 * Create PostScript Type 1 font from a .pfa file.
	 * @param filename name of .pfa file.
	 */
	public PostScriptFont(String filename) throws IOException, MapyrusException
	{
		/*
		 * Only accept filenames with .pfa suffix.
		 */
		if (!filename.toLowerCase().endsWith(".pfa"))
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
				": " + filename);

		BufferedReader bufferedReader = null;
		
		try
		{
			bufferedReader = new BufferedReader(new FileReader(filename));
	
			/*
			 * First line of file contains PostScript keyword, then font name.  For example,
			 * %!PS-AdobeFont-1.0: LuxiSerif 1.1000
			 */
			String firstLine = bufferedReader.readLine();
			if (firstLine == null)
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
					": " + filename);
	
			String magicToken = null;
			StringTokenizer st = new StringTokenizer(firstLine);
			if (st.countTokens() > 1)
			{
				magicToken = st.nextToken();
				mFontName = st.nextToken();
			}
			if (magicToken == null || (!magicToken.startsWith("%!PS-AdobeFont")))
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_A_PFA_FILE) +
					": " + filename);
	
			/*
			 * Read entire .pfa file into memory, most files are about 100kb in size.
			 */
			mFileContents = new StringBuffer(128 * 1024);
			mFileContents.append(firstLine);
			mFileContents.append(Constants.LINE_SEPARATOR);
			
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				mFileContents.append(line);
				mFileContents.append(Constants.LINE_SEPARATOR);
			}
		}
		finally
		{
			if (bufferedReader != null)
				bufferedReader.close();
		}		
	}

	/**
	 * Return name of font, parsed from .pfa file.
	 * @return font name.
	 */
	public String getName()
	{
		return(mFontName);
	}

	/**
	 * String representation of PostScript font.
	 * @return font name.
	 */
	public String toString()
	{
		return("PostScript Font " + mFontName);
	}

	/**
	 * Return definition of font read from .pfa file, suitable for inclusion
	 * in a PostScript file.
	 * @return font definition.
	 */	
	public String getFontDefinition()
	{
		return(mFileContents.toString());
	}
}
