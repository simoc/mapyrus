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
	 * Characters starting a comment on a line.
	 */
	private static final char COMMENT_CHAR_HASH = '#';
	private static final char COMMENT_CHAR_SLASH = '/';
	private static final char COMMENT_CHAR_ASTERISK = '*';

	/*
	 * Files we are reading from and their names.
	 * Built up as a stack as we include nested files.
	 */
	private LinkedList<FileOrURL> m_fileStack;
	
	/*
	 * First file we are reading from.
	 */
	private FileOrURL m_initialFile;
	
	/*
	 * Line we are currently reading from.
	 */
	private StringBuilder m_currentLine = null;
	private int m_currentLineIndex = 0;

	/*
	 * Are we currently reading a comment?
	 */
	private boolean m_InSingleLineComment = false;
	private boolean m_InMultiLineComment = false;

	/*
	 * Is inclusion of other files allowed?
	 * When running as web application it may be blocked for security.
	 */
	private boolean m_isIncludeAllowed;

	/**
	 * Create stack of files being read.
	 * @param f is the first file to push onto the stack.
	 * @param isIncludeAllowed true if other files may be included.
	 */
	private void initFileStack(FileOrURL f, boolean isIncludeAllowed)
	{
		m_fileStack = new LinkedList<FileOrURL>();
		m_fileStack.add(f);
		m_initialFile = f;
		m_isIncludeAllowed = isIncludeAllowed;
	}

	/**
	 * Create new user input producer from an already open Reader.
	 * @param f is a file or URL to read from.
	 * @param isIncludeAllowed true if other files may be included.
	 */
	public Preprocessor(FileOrURL f, boolean isIncludeAllowed)
	{
		initFileStack(f, isIncludeAllowed);
	}

	/**
	 * Create new user input producer from a file.
	 * @param filename is a file to open and read from.
	 */
	public Preprocessor(String filename) throws IOException, MapyrusException
	{
		FileOrURL f = new FileOrURL(filename);
		initFileStack(f, true);
	}

	/*
	 * Open new file to read from and push it on stack of files being read.
	 */
	private void openIncludedFile(String filename) throws MalformedURLException, IOException, MapyrusException
	{
		FileOrURL f;

		FileOrURL includingFile = (FileOrURL)m_fileStack.getLast();

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
		m_fileStack.add(f);
	}

	/*
	 * Read next character, ignoring comments.
	 */
	private int readSkipComments() throws IOException, MapyrusException
	{
		int c;

		c = read();

		while (m_InSingleLineComment || m_InMultiLineComment || c == COMMENT_CHAR_HASH || c == COMMENT_CHAR_SLASH)
		{
			if (m_InSingleLineComment && (c == '\n' || c == -1))
			{
				/*
				 * Newline ends single line comment.
				 */
				m_InSingleLineComment = false;
			}
			else if (m_InMultiLineComment && c == -1)
			{
				/*
				 * Should not reach end of file in middle of comment.
				 */
				m_InMultiLineComment = false;
				
			}
			else if (m_InMultiLineComment && c == COMMENT_CHAR_ASTERISK)
			{
				c = read();
				if (c == COMMENT_CHAR_SLASH)
				{
					m_InMultiLineComment = false;
					c = read();
				}
			}
			else if (m_InSingleLineComment || m_InMultiLineComment)
			{
				/*
				 * Skip character in comment.
				 */
				c = read();
			}
			else if (c == COMMENT_CHAR_HASH)
			{
				/*
				 * Start of "#" comment, skip characters until the end of the line.
				 */
				m_InSingleLineComment = true;
				c = read();
			}
			else if (c == COMMENT_CHAR_SLASH)
			{
				int c2 = read();
				if (c2 == COMMENT_CHAR_SLASH)
				{
					/*
					 * Start of "//" comment, skip characters until the end of the line.
					 */
					m_InSingleLineComment = true;
					c = read();
				}
				else if (c2 == COMMENT_CHAR_ASTERISK)
				{
					/*
					 * Start of "/*" comment, skip characters until matching closing comment.
					 */
					m_InMultiLineComment = true;
					c = read();
				}
				else
				{
					/*
					 * Just a plain "/", not a "//" comment.
					 */
					unread(c2);
					break;
				}
			}
			else
			{
				/*
				 * Skip character in comment.
				 */
				c = read();
			}
		}
		return(c);
	}

	/**
	 * Reads next character that is not a space, skipping comments too.
	 * @return next non-space character.
	 */
	public int readNonSpace() throws IOException, MapyrusException
	{
		int c;

		do
		{
			c = readSkipComments();
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
		if (m_currentLine != null && m_currentLineIndex < m_currentLine.length())
		{
			c = m_currentLine.charAt(m_currentLineIndex++);
			return(c);
		}

		/*
		 * Need to read a new line.
		 */
		m_currentLineIndex = 0;
		FileOrURL f = (FileOrURL)m_fileStack.getLast();
		reader = f.getReader();
		String s = reader.readLine();
		if (s == null)
		{
			/*
			 * Got end-of-file.  Close file and continue reading any file that included
			 * this one.
			 */
			m_fileStack.removeLast();
			reader.close();
			
			/*
			 * Should not reach end-of-file in middle of comment.
			 */
			if (m_InMultiLineComment)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.UNEXPECTED_EOF) +
					": " + f.toString());
			}

			if (m_fileStack.size() > 0)
			{
				m_currentLine = null;
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
		m_currentLine = new StringBuilder(s);
		while (s != null && s.endsWith("\\"))
		{
			/*
			 * Remove '\' at end of buffer, read and append next line
			 * to buffer.
			 */
			int len = m_currentLine.length();
			m_currentLine.deleteCharAt(len - 1);

			s = reader.readLine();
			if (s != null)
				m_currentLine.append(s);
		}
		m_currentLine.append('\n');

		/*
		 * Check if this line includes another file.
		 */
		String trimmed = m_currentLine.toString().trim();
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
						(filename.startsWith("\u2019") && filename.endsWith("\u2019")) ||
						(filename.startsWith("<") && filename.endsWith(">")))
					{
						filename = filename.substring(1, filenameLen - 1);
					}
				}

				if (!m_isIncludeAllowed)
				{
					throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_IO) + ": " + filename);
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

				m_currentLine = null;
				return(read());
			}
		}

		c = m_currentLine.charAt(m_currentLineIndex++);
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
		if (m_currentLine == null)
		{
			Character cs = Character.valueOf((char)c);
			m_currentLine = new StringBuilder(cs.toString());
			m_currentLineIndex = 0;
		}
		else
		{
			/*
			 * Is the character being pushed back the last one
			 * we read (it should be).  If so, we can just
			 * step back one character so it can be read again.
			 */
			if (m_currentLineIndex > 0 &&
				c == m_currentLine.charAt(m_currentLineIndex - 1))
			{
				m_currentLineIndex--;
			}
			else
			{
				Character cs = Character.valueOf((char)c);
				m_currentLine.insert(m_currentLineIndex, cs.toString());
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

		if (m_fileStack.size() > 0)
		{
			retval = (FileOrURL)m_fileStack.getLast();
		}
		else
		{
			/*
			 * Already read to EOF and stack of files is empty.
			 */
			retval = m_initialFile;
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
		while (!m_fileStack.isEmpty())
		{
			FileOrURL f = (FileOrURL)m_fileStack.removeLast();
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
		m_fileStack = null;
	}
}
