/*
 * $Id$
 */

package au.id.chenery.mapyrus;

import java.util.ResourceBundle;

/**
 * Wrapper around a java resource file containing messages for various
 * locales.  Provides single function to get a message for the current
 * locale, given the message key identifier.  
 */
public class MapyrusMessages
{
	public static final String BAD_OUTPUT_FORMAT = "badoutputformat";
	
	private static ResourceBundle messages;

	static
	{
		messages = ResourceBundle.getBundle("MapyrusMessages");
	}

	/**
	 * Returns message for current locale for message a key.
	 * @return full message
	 */
	public static String get(String key)
	{
		return(messages.getString(key));
	}
}
