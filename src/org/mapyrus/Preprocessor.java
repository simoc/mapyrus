/**
 * Wrapper around a Reader to read from a file, whilst expanding
 * any included files.
 * Also allows for read characters to be pushed back onto input stream.
 */

/*
 * $Id$
 */

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.lang.String;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

class Preprocessor
{
	private static final String INCLUDE_KEYWORD = "include";

	/*
	 * Files we are reading from and their names.
	 * Built up as a stack as we include nested files.
	 */
	private LinkedList mFileStack;
	private LinkedList mFilenameStack;

	/*
	 * Line we are currently reading from.
	 */
	private StringBuffer mCurrentLine;
	private int mCurrentLineIndex;

	/**
	 * Create new user input producer.
	 * @param in is a source to read input from.
	 * @param filename is the name of the file being read.
	 */
	public Preprocessor(Reader in, String filename)
	{
		mFileStack = new LinkedList();
		mFilenameStack = new LinkedList();

		mFileStack.add(new LineNumberReader(in));
		mFilenameStack.add(filename);

		mCurrentLine = null;
		mCurrentLineIndex = 0;
	}

	/*
	 * Open new file to read from and push it on stack of files being read.
	 */
	private void openIncludedFile(String filename) throws IOException
	{
		BufferedReader in;

		/*
		 * Open file being included and start read from it instead.
		 */
		try
		{
			/*
			 * Try filename as an absolute URL.
			 */
			URL url = new URL(filename);
			InputStream urlStream = url.openStream();
			in = new BufferedReader(new
				InputStreamReader(urlStream));
		}
		catch (MalformedURLException e)
		{
			/*
			 * Well, maybe it is just a regular file.
			 */
			in = new BufferedReader(new FileReader(filename));
		}
		mFileStack.add(new LineNumberReader(in));
		mFilenameStack.add(filename);
	}

	/**
	 * Reads next character that is not a space.
	 * @return next non-space character.
	 */
	public int readNonSpace() throws IOException, GfException
	{
		int c;

		do
		{
			c = read();
		}
		while (Character.isWhitespace((char)c) && c != '\n');
		return(c);
	}

	/**
	 * Reads next character.
	 * Blocks if another character is not available.
	 * @return next character from wherever user input is coming from, or -1
	 * if at EOF.
	 */
	public int read() throws IOException, GfException
	{
		int c;
		LineNumberReader in;

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
		in = (LineNumberReader)(mFileStack.getLast());
		String s = in.readLine();
		if (s == null)
		{
			/*
			 * Got end-of-file.  Continue reading any file that included
			 * this one.
			 */
			in.close();
			mFileStack.removeLast();
			mFilenameStack.removeLast();
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
		 * Check if this line includes another file.
		 */
		mCurrentLine = new StringBuffer(s);
		mCurrentLine.append('\n');
		StringTokenizer st = new StringTokenizer(s);
		if (st.hasMoreTokens())
		{
			String keyword = st.nextToken();
			keyword.toLowerCase();
			if (INCLUDE_KEYWORD.equals(keyword))
			{
				if (st.hasMoreTokens())
				{
					/*
					 * Open included file and start reading from
					 * it.
					 */
					openIncludedFile(st.nextToken());
					mCurrentLine = null;
					return(read());
				}
				else
				{
					throw new GfException("Missing include filename at " + getCurrentFilenameAndLine());
				}
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
		Character cs = new Character((char)c);

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
				mCurrentLine.insert(mCurrentLineIndex, cs.toString());
			}
		}
	}

	/**
	 * Returns line number and name of file being read.
	 * @retval the name of the file currently being read.
	 */
	public String getCurrentFilenameAndLine()
	{
		String s = (String)mFilenameStack.getLast();
		if (s == null)
			return("");
		else
		{
			LineNumberReader in;
			try
			{
				in = (LineNumberReader)mFileStack.getLast();
				s = s.concat(":" + in.getLineNumber());
			}
			catch(NoSuchElementException e)
			{
			}
		}
		return(s);
	}
}

