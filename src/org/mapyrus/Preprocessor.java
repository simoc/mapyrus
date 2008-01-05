/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2008 Simon Chenery.
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

import java.io.*;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.lang.String;

/**
 * Wrapper around a Reader to read from a file or URL, whilst expanding
 * any included files and URLs.
 * Also allows for read characters to be pushed back onto input stream.
 */
class Preprocessor
{
	private static final String INCLUDE_KEYWORD = "include";

	/*
	 * Files we are reading from and their names.
	 * Built up as a stack as we include nested files.
	 */
	private LinkedList<FileOrURL> mFileStack;
	
	/*
	 * First file we are reading from.
	 */
	private FileOrURL mInitialFile;
	
	/*
	 * Line we are currently reading from.
	 */
	private StringBuffer mCurrentLine = null;
	private int mCurrentLineIndex = 0;

	/**
	 * Create stack of files being read.
	 * @param f is the first file to push onto the stack.
	 */
	private void initFileStack(FileOrURL f)
	{
		mFileStack = new LinkedList<FileOrURL>();
		mFileStack.add(f);
		mInitialFile = f;
	}

	/**
	 * Create new user input producer from an already open Reader.
	 * @param f is a file or URL to read from.
	 */
	public Preprocessor(FileOrURL f)
	{
		initFileStack(f);
	}

	/**
	 * Create new user input producer from a file.
	 * @param filename is a file to open and read from.
	 */
	public Preprocessor(String filename) throws IOException, MapyrusException
	{
		FileOrURL f = new FileOrURL(filename);
		initFileStack(f);
	}

	/*
	 * Open new file to read from and push it on stack of files being read.
	 */
	private void openIncludedFile(String filename) throws MalformedURLException, IOException, MapyrusException
	{
		FileOrURL f;

		FileOrURL includingFile = (FileOrURL)mFileStack.getLast();

		if (includingFile.isURL())
			f = new FileOrURL(filename, includingFile);
		else
			f = new FileOrURL(filename);

		if (f.isURL())
		{
			/*
			 * Check that we are reading a plain text type of URL.
			 */
			String contentType;
			try
			{
				contentType = f.getURLContentType();
			}
			catch (IOException e)
			{
				f.getReader().close();
				throw e;
			}

			if (!contentType.startsWith("text/"))
			{
				f.getReader().close();
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NOT_TEXT_FILE) +
					": " + f.toString());
			}
		}
		mFileStack.add(f);
	}


	/**
	 * Reads next character that is not a space.
	 * @return next non-space character.
	 */
	public int readNonSpace() throws IOException, MapyrusException
	{
		int c;

		do
		{
			c = read();
		}
		while (c != -1 && Character.isWhitespace((char)c) && c != '\n');
		return(c);
	}

	/**
	 * Reads next character.
	 * Blocks if another character is not available.
	 * @return next character from wherever user input is coming from, or -1
	 * if at EOF.
	 */
	public int read() throws IOException, MapyrusException
	{
		int c;
		LineNumberReader reader;

		/*
		 * Return next character from current line.
		 */
		if (mCurrentLine != null && mCurrentLineIndex < mCurrentLine.length())
		{
			c = mCurrentLine.charAt(mCurrentLineIndex++);
			return(c);
		}

		/*
		 * Need to read a new line.
		 */
		mCurrentLineIndex = 0;
		FileOrURL f = (FileOrURL)mFileStack.getLast();
		reader = f.getReader();
		String s = reader.readLine();
		if (s == null)
		{
			/*
			 * Got end-of-file.  Close file and continue reading any file that included
			 * this one.
			 */
			mFileStack.removeLast();
			reader.close();
			if (mFileStack.size() > 0)
			{
				mCurrentLine = null;
				return(read());
			}
			else
			{
				return(-1);
			}
		}

		/*
		 * Join line with next line if it ends with '\'.
		 */
		mCurrentLine = new StringBuffer(s);
		while (s != null && s.endsWith("\\"))
		{
			/*
			 * Remove '\' at end of buffer, read and append next line
			 * to buffer.
			 */
			int len = mCurrentLine.length();
			mCurrentLine.deleteCharAt(len - 1);

			s = reader.readLine();
			if (s != null)
				mCurrentLine.append(s);
		}
		mCurrentLine.append('\n');

		/*
		 * Check if this line includes another file.
		 */
		String trimmed = mCurrentLine.toString().trim();
		if (trimmed.toLowerCase().startsWith(INCLUDE_KEYWORD))
		{
			if (trimmed.length() == INCLUDE_KEYWORD.length())
			{
				throw new MapyrusException(getCurrentFilenameAndLineNumber() +
					": " + MapyrusMessages.get(MapyrusMessages.MISSING_FILENAME));
			}

			if (Character.isWhitespace(trimmed.charAt(INCLUDE_KEYWORD.length())))
			{
				String filename = trimmed.substring(INCLUDE_KEYWORD.length() + 1).trim();
				int filenameLen = filename.length();
				if (filenameLen > 1)
				{
					/*
					 * Strip any quotes or angle brackets around the filename.
					 */
					if ((filename.startsWith("\"") && filename.endsWith("\"")) ||
						(filename.startsWith("'") && filename.endsWith("'")) ||
						(filename.startsWith("<") && filename.endsWith(">")))
					{
						filename = filename.substring(1, filenameLen - 1);
					}
				}

				/*
				 * Open included file and start
				 * reading from it.
				 */
				try
				{
					openIncludedFile(filename);
				}
				catch (Exception e)
				{
					throw new MapyrusException(getCurrentFilenameAndLineNumber() +
						": " + e.getMessage());
				}

				mCurrentLine = null;
				return(read());
			}
		}

		c = mCurrentLine.charAt(mCurrentLineIndex++);
		return(c);
	}

	/**
	 * Pushes a single character that was read back to the reader.
	 */
	public void unread(int c)
	{
		/*
		 * Push character back into line we are reading from.
		 */
		if (c == -1)
		{
			/*
			 * Don't allow EOF sentinel to be pushed back.
			 */
		}
		if (mCurrentLine == null)
		{
			Character cs = new Character((char)c);
			mCurrentLine = new StringBuffer(cs.toString());
			mCurrentLineIndex = 0;
		}
		else
		{
			/*
			 * Is the character being pushed back the last one
			 * we read (it should be).  If so, we can just
			 * step back one character so it can be read again.
			 */
			if (mCurrentLineIndex > 0 &&
				c == mCurrentLine.charAt(mCurrentLineIndex - 1))
			{
				mCurrentLineIndex--;
			}
			else
			{
				Character cs = new Character((char)c);
				mCurrentLine.insert(mCurrentLineIndex, cs.toString());
			}
		}
	}

	/**
	 * Returns information about file currently being read.
	 * @return file information.
	 */
	private FileOrURL getCurrentFileOrURL()
	{
		FileOrURL retval;

		if (mFileStack.size() > 0)
		{
			retval = (FileOrURL)mFileStack.getLast();
		}
		else
		{
			/*
			 * Already read to EOF and stack of files is empty.
			 */
			retval = mInitialFile;
		}
		return(retval);
	}
	
	/**
	 * Returns name of file being read.
	 * @return the name of the file currently being read.
	 */		
	public String getCurrentFilename()
	{
		FileOrURL f = getCurrentFileOrURL();
		return(f.toString());
	}

	/**
	 * Returns name of file being read.
	 * @return the name of the file currently being read.
	 */		
	public int getCurrentLineNumber()
	{
		FileOrURL f = getCurrentFileOrURL();
		return(f.getReader().getLineNumber());
	}
		
	/**
	 * Returns line number and name of file being read.
	 * @retval the name and line number of the file currently being read.
	 */
	public String getCurrentFilenameAndLineNumber()
	{
		return(getCurrentFilename() + ":" + getCurrentLineNumber());
	}

	/**
	 * Close preprocessor, closing all files it was reading.
	 * The preprocessor cannot be used again after calling this method.
	 */
	public void close()
	{
		while (!mFileStack.isEmpty())
		{
			FileOrURL f = (FileOrURL)mFileStack.removeLast();
			Reader reader = f.getReader();
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
				/*
				 * Ignore any errors, we just want to close all open files we were reading.
				 */
			}
		}
		mFileStack = null;
	}
}
