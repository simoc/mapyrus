/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
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

import java.util.HashMap;

import org.mapyrus.MapyrusException;

/**
 * Manages all available functions.  Provides function name
 * lookup function.
 */
public class FunctionTable
{
	static HashMap mFunctions = new HashMap();

	/*
	 * Load all internal functions and any additional functions defined by user.
	 */
	static
	{
		Function f;

		f = new Abs();
		mFunctions.put(f.getName(), f);

		f = new Ceil();
		mFunctions.put(f.getName(), f);

		f = new Cos();
		mFunctions.put(f.getName(), f);

		f = new Floor();
		mFunctions.put(f.getName(), f);

		f = new Length();
		mFunctions.put(f.getName(), f);

		f = new Lower();
		mFunctions.put(f.getName(), f);

		f = new Log10();
		mFunctions.put(f.getName(), f);

		f = new Match();
		mFunctions.put(f.getName(), f);

		f = new Max();
		mFunctions.put(f.getName(), f);

		f = new Min();
		mFunctions.put(f.getName(), f);

		f = new Parsewkt();
		mFunctions.put(f.getName(), f);

		f = new Pow();
		mFunctions.put(f.getName(), f);

		f = new Protected();
		mFunctions.put(f.getName(), f);

		f = new Random();
		mFunctions.put(f.getName(), f);

		f = new Replace();
		mFunctions.put(f.getName(), f);

		f = new Round();
		mFunctions.put(f.getName(), f);
		
		f = new Sin();
		mFunctions.put(f.getName(), f);
		
		f = new Split();
		mFunctions.put(f.getName(), f);
		
		f = new Spool();
		mFunctions.put(f.getName(), f);

		f = new Sqrt();
		mFunctions.put(f.getName(), f);
		
		f = new Stringheight();
		mFunctions.put(f.getName(), f);
		
		f = new Stringwidth();
		mFunctions.put(f.getName(), f);
		
		f = new Substr();
		mFunctions.put(f.getName(), f);
		
		f = new Tan();
		mFunctions.put(f.getName(), f);
		
		f = new Tempname();
		mFunctions.put(f.getName(), f);

		f = new Upper();
		mFunctions.put(f.getName(), f);

		f = new Wordwrap();
		mFunctions.put(f.getName(), f);

		try
		{
			/*
			 * Java Topology Suite functions are only available if the
			 * JTS JAR file is available in the classpath.
			 */
			f = new Buffer();
			mFunctions.put(f.getName(), f);

			f = new Contains();
			mFunctions.put(f.getName(), f);

			f = new ConvexHull();
			mFunctions.put(f.getName(), f);

			f = new Intersection();
			mFunctions.put(f.getName(), f);

			f = new Overlaps();
			mFunctions.put(f.getName(), f);

			f = new Union();
			mFunctions.put(f.getName(), f);
		}
		catch (NoClassDefFoundError e)
		{
			/*
			 * Add dummy placeholder functions instead.
			 * They'll fail if they are ever called though.
			 */
			mFunctions.put("buffer", new DummyFunction("buffer"));
			mFunctions.put("contains", new DummyFunction("contains"));
			mFunctions.put("convexhull", new DummyFunction("convexhull"));
			mFunctions.put("intersection", new DummyFunction("intersection"));
			mFunctions.put("overlaps", new DummyFunction("overlaps"));
			mFunctions.put("union", new DummyFunction("union"));
		}
	};

	/**
	 * Lookup function from name and return object
	 * @param name name of function to lookup.
	 * @return object for evaluating this function, or null if not found.
	 */
	public static Function getFunction(String funcName) throws MapyrusException
	{
		Function retval = (Function)(mFunctions.get(funcName));
		return(retval);
	}
}
