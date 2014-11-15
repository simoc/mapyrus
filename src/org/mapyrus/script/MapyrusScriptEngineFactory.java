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

package org.mapyrus.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.mapyrus.Constants;

/**
 * Provides meta-data for Mapyrus ScriptEngine.
 */
public class MapyrusScriptEngineFactory implements ScriptEngineFactory
{
	@Override
	public String getEngineName()
	{
		return Constants.PROGRAM_NAME;
	}

	@Override
	public String getEngineVersion()
	{
		return Constants.getVersion();
	}

	@Override
	public List<String> getExtensions()
	{
		ArrayList<String> extensions = new ArrayList<String>();
		extensions.add(Constants.PROGRAM_NAME.toLowerCase());
		return extensions;
	}

	@Override
	public List<String> getMimeTypes()
	{
		ArrayList<String> mimeTypes = new ArrayList<String>();
		mimeTypes.add("text/" + Constants.PROGRAM_NAME.toLowerCase());
		mimeTypes.add("application/" + Constants.PROGRAM_NAME.toLowerCase());
		return mimeTypes;
	}

	@Override
	public List<String> getNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		names.add(Constants.PROGRAM_NAME);
		return names;
	}

	@Override
	public String getLanguageName()
	{
		return Constants.PROGRAM_NAME;
	}

	@Override
	public String getLanguageVersion()
	{
		return Constants.getVersion();
	}

	@Override
	public Object getParameter(String key)
	{
		Object retval = null;

		if (key.equals(ScriptEngine.ENGINE))
			retval = getEngineName();
		else if (key.equals(ScriptEngine.ENGINE_VERSION))
			retval = getEngineVersion();
		else if (key.equals(ScriptEngine.NAME))
			retval = getNames().get(0);
		else if (key.equals(ScriptEngine.LANGUAGE))
			retval = getLanguageName();
		else if (key.equals(ScriptEngine.LANGUAGE_VERSION))
			retval = getLanguageVersion();
		else if (key.equals("THREADING"))
		{
			/*
			 * A single MapyrusScriptEngine instance is not thread-safe.
			 */
			retval = null;
		}
		return retval;
	}

	@Override
	public String getMethodCallSyntax(String obj, String m, String... args)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOutputStatement(String toDisplay)
	{
		toDisplay = toDisplay.replace("'", "\\'");
		toDisplay = toDisplay.replace("\\", "\\\\");
		return "print '" + toDisplay + "'";
	}

	@Override
	public String getProgram(String... statements)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < statements.length; i++)
		{
			sb.append(statements[i]);
			sb.append(Constants.LINE_SEPARATOR);
		}
		return sb.toString();
	}

	@Override
	public ScriptEngine getScriptEngine()
	{
		return new MapyrusScriptEngine(this);
	}
}
