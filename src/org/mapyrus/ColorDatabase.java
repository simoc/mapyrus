/**
 * Color names and their RGB color components.
 * A name to RGB value lookup table using colors read from a UNIX-style rgb.txt file.
 */

/*
 * $Id$
 */
 
import java.util.Hashtable;

public class ColorDatabase
{
	static private boolean mIsLoaded = false;
	static private Hashtable mColors;
	
	/*
	 * Load colors the first time a color is requested.
	 * Don't worry about race-condition -- two threads loading colors
	 * at same time is harmless.
	 */
	private static void loadColors()
	{
		mColors = new Hashtable();
		
		/*
		 * If user gave name of file as property then read that.
		 */
		 
		/*
		 * Otherwise try to read /usr/lib/X11/rgb.txt.
		 */
	}
	
	public static int []getRGBValues(String colorName)
	{
		if (mIsLoaded == false)
		{
			loadColors();
			mIsLoaded = true;
		}
		
 		int []retval = {255, 0, 0};
		
		return(retval);
	}
}
