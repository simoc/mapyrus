/*
 * @(#) $Id$
 */
package au.id.chenery.mapyrus;

import java.lang.String;

/**
 * This class indicates exceptions in the Mapyrus software that can be caught.
 */
public class MapyrusException extends Exception
{
	public MapyrusException()
	{
		super();
	}

	public MapyrusException(String s)
	{
		super(s);
	}
}
