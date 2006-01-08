/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005, 2006 Simon Chenery.
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
package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Base class extended by all classes that implement internal functions in Mapyrus. 
 */
public interface Function
{
	/**
	 * Get name of function.
	 * @return function name.
	 */
	public abstract String getName();

	/**
	 * Get minimum number of arguments accepted by this function.
	 * @return minimum argument count.
	 */
	public abstract int getMinArgumentCount();

	/**
	 * Get maximum number of arguments accepted by this function.
	 * Functions are limited to accept at most four arguments.
	 * @return maximum argument count.
	 */
	public abstract int getMaxArgumentCount();

	/**
	 * Evaluate function.
	 * @param context context containing variables and other state information.
	 * @param args arguments to function.
	 * @return evaluated function value.
	 * @throws MapyrusException
	 */
	public Argument evaluate(ContextStack context, ArrayList args)
		throws MapyrusException;
}
