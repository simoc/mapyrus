/**
 * Wrapper around a Reader to read from a file or URL, whilst expanding
 * any included files and URLs.
 * Also allows for read characters to be pushed back onto input stream.
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;

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
	 * Holds file handle, filename and URL information together
	 * for a file or URL that we are reading from.
	 */
	class FileInfo
	{
		private LineNumberReader mLineNumberReader;
		private String mFilename;
		private boolean mIsURL;
		private URL mUrl;

		/*
		 * New reader from a plain file.
		 */
		public FileInfo(LineNumberReader in, String filename)
		{
			mLineNumberReader = in;
			mFilename = filename;
			mIsURL = false;
		}

		/*
		 * New reader from a URL.
		 */
		public FileInfo(LineNumberReader in, String filename, URL url)
		{
			mLineNumberReader = in;
			mFilename = filename;
			mUrl = url;
			mIsURL = true;
		}

		/*
		 * Returns file 
		 */
		public LineNumberReader getReader()
		{
			return(mLineNumberReader);
		}

		/*
		 * Returns name of file being read.
		 */
		public String getFilename()
		{
			return(mFilename);
		}

		/*
		 * Returns true if file being read was accessed through a URL.
		 */
		public boolean isURL()
		{
			return(mIsURL);
		}

		/*
		 * Returns URL that data is being read from.
		 */
		public URL getURL()
		{
			return(mUrl);
		}
	}

	/*
	 * Files we are reading from and their names.
	 * Built up as a stack as we include nested files.
	 */
	private LinkedList mFileStack;
	
	/*
	 * First file we are reading from.
	 */
	private FileInfo mInitialFile;
	
	/*
	 * Line we are currently reading from.
	 */
	private StringBuffer mCurrentLine = null;
	private int mCurrentLineIndex = 0;

	/**
	 * Create stack of files being read.
	 * @param f is the first file to push onto the stack.
	 */
	private void initFileStack(FileInfo f)
	{
		mFileStack = new LinkedList();
		mFileStack.add(f);
		mInitialFile = f;
	}

	/**
	 * Create new user input producer from an already open Reader.
	 * @param in is a source to read input from.
	 * @param filename is filename or URL of in for use in error messages.
	 */
	public Preprocessor(Reader in, String filename)
	{
		LineNumberReader l = new LineNumberReader(in);
		FileInfo f = new FileInfo(l, filename);
		initFileStack(f);
	}

	/**
	 * Create new user input producer from a file.
	 * @param filename is a file to open and read from.
	 */
	public Preprocessor(String filename) throws FileNotFoundException
	{
		LineNumberReader in;

		in = new LineNumberReader(new FileReader(filename));
		FileInfo f = new FileInfo(in, filename);
		initFileStack(f);
	}

	/*
	 * Open new file to read from and push it on stack of files being read.
	 */
	private void openIncludedFile(String filename) throws FileNotFoundException, MalformedURLException, IOException, MapyrusException
	{
		LineNumberReader in;
		InputStream urlStream;
		FileInfo f;

		FileInfo includingFile = (FileInfo)mFileStack.getLast();

		try
		{
			/*
			 * Try filename first as an absolute URL.
			 */
			URL url = new URL(filename);
			if (!url.openConnection().getContentType().startsWith("text/plain"))
			{
				throw new MapyrusException("Not a plain text file");
			}

			urlStream = url.openStream();
			in = new LineNumberReader(new
				InputStreamReader(urlStream));
			f = new FileInfo(in, filename, url);
		}
		catch (MalformedURLException e)
		{
			if (includingFile.isURL())
			{
				/*
				 * Perhaps it a relative URL from the URL that
				 * is including it.
				 */
				URL lastURL = includingFile.getURL();
				URL url = new URL(lastURL, filename);
				if (!url.openConnection().getContentType().startsWith("text/plain"))
				{
					throw new MapyrusException("Not a plain text file");
				}

				urlStream = url.openStream();
				in = new LineNumberReader(new
					InputStreamReader(urlStream));
				f = new FileInfo(in, filename, url);
			}
			else
			{
				/*
				 * Well, maybe it is just a regular file.
				 */
				in = new LineNumberReader(new FileReader(filename));
				f = new FileInfo(in, filename);
			}
		}
		mFileStack.add(f);
	}

	/**
	 * Reads next line.
	 * @return line read, or null if end of stream is already reached.
	 */
	public String readLine() throws IOException, MapyrusException
	{
		StringBuffer retval = new StringBuffer();
		int c;

		do
		{
			/*
			 * If nothing can be read then return null.
			 */
			c = read();
			if (c == -1 && retval.length() == 0)
				return(null);

			/*
			 * Consider '\r', \n', '\r\n' as line terminators.
			 */
			if (c == '\r')
			{
				c = read();
				if (c != '\n')
				{
					unread(c);
					c = '\n';
				}
			}
			else if (c != '\n')
			{
				retval.append((char)c);
			}
		}
		while (c != -1 && c != '\n');
		return(retval.toString());
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
		FileInfo f = (FileInfo)mFileStack.getLast();
		in = f.getReader();
		String s = in.readLine();
		if (s == null)
		{
			/*
			 * Got end-of-file.  Close file and continue reading any file that included
			 * this one.
			 */
			in.close();
			mFileStack.removeLast();
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
					String filename = st.nextToken();
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
						throw new MapyrusException("Cannot include " + filename + " from " +
							getCurrentFilenameAndLineNumber() + ": " + e.getMessage());
					}

					mCurrentLine = null;
					return(read());
				}
				else
				{
					throw new MapyrusException("Missing include filename at " +
						getCurrentFilenameAndLineNumber());
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
	 * @retval file information.
	 */
	private FileInfo getCurrentFileInfo()
	{
		FileInfo retval;

		if (mFileStack.size() > 0)
		{
			retval = (FileInfo)mFileStack.getLast();
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
	 * @retval the name of the file currently being read.
	 */		
	public String getCurrentFilename()
	{
		FileInfo f = getCurrentFileInfo();
		return(f.getFilename());
	}

	/**
	 * Returns name of file being read.
	 * @retval the name of the file currently being read.
	 */		
	public int getCurrentLineNumber()
	{
		FileInfo f = getCurrentFileInfo();
		return(f.mLineNumberReader.getLineNumber());
	}
		
	/**
	 * Returns line number and name of file being read.
	 * @retval the name and line number of the file currently being read.
	 */
	public String getCurrentFilenameAndLineNumber()
	{
		return(getCurrentFilename() + " line " + getCurrentLineNumber());
	}

	private static void processLine(String s) throws IOException, InterruptedException
	{
		/*
		 * Execute lines with commands, echo all other lines.
		 */
		if (s.length() > 5 && s.startsWith("exec") && Character.isWhitespace(s.charAt(4)))
		{
			String command = s.substring(5).trim();
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		}
		else
		{
			System.out.println(s);
		}
	}

	/*
	 * Preprocess files given on command line.
	 */
	public static void main(String []args) throws IOException, MapyrusException,
		InterruptedException
	{
		BufferedReader in;
		int c;
		int i = 0;
		String s;
		Preprocessor p;

		/*
		 * Read from first file given on command line, or standard input
		 * if no files are given.
		 */
		if (args.length == 0 || (args.length == 1 && args[0].equals("-")))
			p = new Preprocessor(new InputStreamReader(System.in), "stdin");
		else
			p = new Preprocessor(args[0]);

		do
		{
			/*
			 * Read each line from each file given on command line.
			 */
			while ((s = p.readLine()) != null)
				processLine(s);
			i++;
			if (i < args.length)
				p = new Preprocessor(args[i]);
		}
		while (i < args.length);
	}
}

