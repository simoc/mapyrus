/**
 * A row read from a geographic dataset containing one geometrical object plus
 * its attributes.
 * Or as the OGIS people would say, a "simple feature".
 */

/*
 * $Id$
 */
package net.sourceforge.mapyrus;

import java.util.Vector;

public class Row extends Vector 
{
	/*
	 * Types of attributes for a feature.
	 */
	public static final int NUMBER = 1;
	public static final int STRING = 2;
	public static final int GEOMETRY = 3;
}
