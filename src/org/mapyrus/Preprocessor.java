/**
 * Wrapper around a Reader to read from a file, whilst expanding any included files.
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
	 * Last character read.
	 */
	private int mLastChar;
	private int mPushedChar;
	private boolean mCharPushedBack;

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

		mLastChar = '\n';
		mCharPushedBack = false;
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
	 * Reads next character.
	 * Blocks if another character is not available.
	 * @return next character from wherever user input is coming from, or -1
	 * if at EOF.
	 */
	public int read() throws IOException, GfException
	{
		int c;
		LineNumberReader in;

		if (mCharPushedBack)
		{
			/*
			 * Return the character that was pushed back.
			 */
			mCharPushedBack = false;
			return(mPushedChar);
		}

		in = (LineNumberReader)(mFileStack.getLast());
		c = in.read();

		/*
		 * If we just read a newline then check if next line is an include
		 * line.
		 */
		if (mLastChar == '\n')
		{
			/*
			 * Read whole line and see if we should include another file.
			 */
			in.mark(2048);
			Character cs = new Character((char)c);
			String nextLine = cs.toString() + in.readLine();
			StringTokenizer st = new StringTokenizer(nextLine);
            		if (st.hasMoreTokens())
			{
				String keyword = st.nextToken();
				keyword.toLowerCase();
				if (INCLUDE_KEYWORD.equals(keyword))
				{
					if (st.hasMoreTokens())
					{
						openIncludedFile(st.nextToken());
						return(read());
					}
					else
					{
						throw new GfException("Missing include filename at " + getCurrentFilenameAndLine());
					}
				}
			}

			/*
			 * It's not an include line so rewind back to the
			 * character we just read.
			 */
			in.reset();
		}

		if (c == -1)
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
				return(read());
			}
		}

		mLastChar = c;
		return(c);
	}

	/*
	 * Pushes a single character that was read back to the reader.
	 */
	public void unread(int c)
	{
		mPushedChar = c;
		mCharPushedBack = true;
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
				s.concat(":" + in.getLineNumber());
			}
			catch(NoSuchElementException e)
			{
			}
		}
		return(s);
	}
}

