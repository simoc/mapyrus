/**
 * This class indicates exceptions in the Mapyrus software that can be caught.
 */
 
/*
 * @(#) $Id$
 */
package net.sourceforge.mapyrus;

import java.lang.String;

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
