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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.mapyrus.FileOrURL;
import org.mapyrus.Interpreter;
import org.mapyrus.MapyrusException;

/**
 * ScriptEngine for Mapyrus.
 */
public class MapyrusScriptEngine implements ScriptEngine
{
	MapyrusScriptEngineFactory m_factory;
	MapyrusScriptContext m_scriptContext;

	public MapyrusScriptEngine(MapyrusScriptEngineFactory factory)
	{
		m_factory = factory;
		m_scriptContext = new MapyrusScriptContext();
	}

	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException
	{
		setContext(context);
		return eval(script);
	}

	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException
	{
		setContext(context);
		return eval(reader);
	}

	@Override
	public Object eval(String script) throws ScriptException
	{
		return eval(new StringReader(script));
	}

	@Override
	public Object eval(Reader reader) throws ScriptException
	{
		try
		{
			FileOrURL f = new FileOrURL(reader, getClass().getSimpleName());

			byte []emptyBuffer = new byte[0];
			ByteArrayInputStream stdin = new ByteArrayInputStream(emptyBuffer);

			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(stdout);

			Interpreter interpreter = m_scriptContext.getInterpreter();
			interpreter.interpret(m_scriptContext.getContextStack(), f, stdin, printStream);
			Writer stdoutWriter = m_scriptContext.getWriter();
			if (stdoutWriter != null)
			{
				stdoutWriter.write(stdout.toString());
				stdoutWriter.flush();
			}
		}
		catch (MapyrusException e)
		{
			throw new ScriptException(e);
		}
		catch (IOException e)
		{
			throw new ScriptException(e);
		}
		catch (InterruptedException e)
		{
			throw new ScriptException(e);
		}
		return null;
	}

	@Override
	public Object eval(String script, Bindings n) throws ScriptException
	{
		Object retval = null;
		Bindings savedBindings = getBindings(ScriptContext.ENGINE_SCOPE);

		try
		{
			setBindings(n, ScriptContext.ENGINE_SCOPE);
			retval = eval(script);
		}
		finally
		{
			setBindings(savedBindings, ScriptContext.ENGINE_SCOPE);
		}
		return retval;
	}

	@Override
	public Object eval(Reader reader, Bindings n) throws ScriptException
	{
		Object retval = null;
		Bindings savedBindings = getBindings(ScriptContext.ENGINE_SCOPE);

		try
		{
			setBindings(n, ScriptContext.ENGINE_SCOPE);
			retval = eval(reader);
		}
		finally
		{
			setBindings(savedBindings, ScriptContext.ENGINE_SCOPE);
		}
		return retval;
	}

	@Override
	public void put(String key, Object value)
	{
		getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
	}

	@Override
	public Object get(String key)
	{
		return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
	}

	@Override
	public Bindings getBindings(int scope)
	{
		return getContext().getBindings(scope);
	}

	@Override
	public void setBindings(Bindings bindings, int scope)
	{
		if (scope == ScriptContext.ENGINE_SCOPE)
			getContext().setBindings(bindings, scope);
	}

	@Override
	public Bindings createBindings()
	{
		return new MapyrusBindings();
	}

	@Override
	public ScriptContext getContext()
	{
		return m_scriptContext;
	}

	@Override
	public void setContext(ScriptContext context)
	{
		m_scriptContext = (MapyrusScriptContext)context;
	}

	@Override
	public ScriptEngineFactory getFactory()
	{
		return m_factory;
	}
}
