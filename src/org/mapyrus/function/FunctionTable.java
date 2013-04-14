/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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

package org.mapyrus.function;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Manages all available functions.  Provides function name
 * lookup function.
 */
public class FunctionTable
{
	static Hashtable<String, Function> mFunctions = new Hashtable<String, Function>();

	/*
	 * Load all internal functions and any additional functions defined by user.
	 */
	static
	{
		Function f;

		f = new Abs();
		mFunctions.put(f.getName(), f);

		f = new Axis();
		mFunctions.put(f.getName(), f);

		f = new Ceil();
		mFunctions.put(f.getName(), f);

		f = new Chr();
		mFunctions.put(f.getName(), f);

		f = new Cos();
		mFunctions.put(f.getName(), f);

		f = new Dir();
		mFunctions.put(f.getName(), f);

		f = new Floor();
		mFunctions.put(f.getName(), f);

		f = new Format();
		mFunctions.put(f.getName(), f);

		f = new Geojson();
		mFunctions.put(f.getName(), f);

		f = new Interpolate();
		mFunctions.put(f.getName(), f);

		f = new Length();
		mFunctions.put(f.getName(), f);

		f = new Lower();
		mFunctions.put(f.getName(), f);

		f = new Lpad();
		mFunctions.put(f.getName(), f);

		f = new Log10();
		mFunctions.put(f.getName(), f);

		f = new Match();
		mFunctions.put(f.getName(), f);

		f = new Max();
		mFunctions.put(f.getName(), f);

		f = new Min();
		mFunctions.put(f.getName(), f);

		f = new Parsegeo();
		mFunctions.put(f.getName(), f);

		f = new Pow();
		mFunctions.put(f.getName(), f);

		f = new Protected();
		mFunctions.put(f.getName(), f);

		f = new Random();
		mFunctions.put(f.getName(), f);

		f = new Readable();
		mFunctions.put(f.getName(), f);

		f = new Replace();
		mFunctions.put(f.getName(), f);

		f = new Roman();
		mFunctions.put(f.getName(), f);

		f = new Round();
		mFunctions.put(f.getName(), f);

		f = new Rpad();
		mFunctions.put(f.getName(), f);

		f = new Sin();
		mFunctions.put(f.getName(), f);

		f = new Split();
		mFunctions.put(f.getName(), f);

		f = new Spool();
		mFunctions.put(f.getName(), f);

		f = new Sqrt();
		mFunctions.put(f.getName(), f);

		f = new Stringascent();
		mFunctions.put(f.getName(), f);

		f = new Stringdescent();
		mFunctions.put(f.getName(), f);

		f = new Stringheight();
		mFunctions.put(f.getName(), f);

		f = new Stringwidth();
		mFunctions.put(f.getName(), f);

		f = new Substr();
		mFunctions.put(f.getName(), f);

		f = new Sum();
		mFunctions.put(f.getName(), f);

		f = new Tan();
		mFunctions.put(f.getName(), f);

		f = new Tempname();
		mFunctions.put(f.getName(), f);

		f = new Timestamp();
		mFunctions.put(f.getName(), f);

		f = new Topage();
		mFunctions.put(f.getName(), f);

		f = new Toworlds();
		mFunctions.put(f.getName(), f);

		f = new Trim();
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

			f = new Crosses();
			mFunctions.put(f.getName(), f);

			f = new Difference();
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
			mFunctions.put("difference", new DummyFunction("difference"));
			mFunctions.put("intersection", new DummyFunction("intersection"));
			mFunctions.put("overlaps", new DummyFunction("overlaps"));
			mFunctions.put("union", new DummyFunction("union"));
		}

		try
		{	
			/*
			 * Reprojection only available if jhlabs.com PROJ.4 JAR file is in classpath.
			 */
			f = new Reproject();
			mFunctions.put(f.getName(), f);
		}
		catch (NoClassDefFoundError e)
		{
			mFunctions.put("reproject", new DummyFunction("reproject"));
		}
	}

	/**
	 * Lookup Java method using reflection.
	 * @param funcName name of class and method to lookup.
	 * @return function for calling this method, or null if no method found.
	 */
	private static Function getJavaFunction(String funcName, int dotIndex)
	{
		String methodName = funcName.substring(dotIndex + 1);
		String className = funcName.substring(0, dotIndex);
		Function retval = null;

		try
		{
			/*
			 * Try to find Java class.
			 */
			Class<?> clazz = Class.forName(className);

			/*
			 * Now try to find method in the Java class.
			 */
			Method[] methods = clazz.getDeclaredMethods();
			ArrayList<Method> possibleMethods = new ArrayList<Method>();
			for (int i = 0; i < methods.length; i++)
			{
				int modifiers = methods[i].getModifiers();
				if ((modifiers & Modifier.STATIC) != 0 &&
					(modifiers & Modifier.PUBLIC) != 0 &&
					methodName.equals(methods[i].getName()))
				{
					possibleMethods.add(methods[i]);
				}
			}
			if (!possibleMethods.isEmpty())
			{
				retval = new JavaFunction(className, methodName, possibleMethods); 

				/*
				 * Add function to standard function table to avoid having to search
				 * for it again next time it is used. 
				 */
				mFunctions.put(funcName, retval);
			}
		}
		catch (ClassNotFoundException e)
		{
		}
		return(retval);
	}

	/**
	 * Lookup function from name and return object
	 * @param name name of function to lookup.
	 * @return object for evaluating this function, or null if not found.
	 */
	public static Function getFunction(String funcName)
	{
		Function retval = mFunctions.get(funcName);
		
		if (retval == null)
		{
			/*
			 * Try and find a Java method with this name instead.
			 */
			int index = funcName.lastIndexOf('.');
			if (index >= 0)
				retval = getJavaFunction(funcName, index);
		}
		return(retval);
	}
}
