package org.mapyrus.script;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;

import org.mapyrus.ContextStack;
import org.mapyrus.Interpreter;

/**
 * Holds state/context for a Mapyrus ScriptEngine.
 */
public class MapyrusScriptContext implements ScriptContext
{
	Reader m_reader;
	Writer m_writer;
	Writer m_errorWriter;
	Interpreter m_interpreter;
	ContextStack m_context;

	public MapyrusScriptContext()
	{
		m_interpreter = new Interpreter();
		m_context = new ContextStack();
	}

	@Override
	public void setBindings(Bindings bindings, int scope)
	{
		if (scope == ENGINE_SCOPE)
		{
			m_context.setBindings(bindings);

			/*
			 * Ensure any changes to the Bindings are immediately made in Mapyrus too.
			 */
			((MapyrusBindings)bindings).setScriptContext(this);
		}
	}

	@Override
	public Bindings getBindings(int scope)
	{
		MapyrusBindings retval = null;
		if (scope == ENGINE_SCOPE)
		{
			retval = (MapyrusBindings)m_context.getBindings();

			/*
			 * Ensure any changes to the Bindings are immediately made in Mapyrus too.
			 */
			retval.setScriptContext(this);
		}
		return retval;
	}

	@Override
	public void setAttribute(String name, Object value, int scope)
	{
		Bindings bindings = getBindings(scope);
		if (bindings != null)
		{
			/*
			 * Addition will be automatically made in Mapyrus context too.
			 */
			bindings.put(name, value);
		}
	}

	@Override
	public Object getAttribute(String name, int scope)
	{
		Object retval = null;
		Bindings bindings = getBindings(scope);
		if (bindings != null)
			retval = bindings.get(name);
		return retval;
	}

	@Override
	public Object removeAttribute(String name, int scope)
	{
		Object retval = null;
		Bindings bindings = getBindings(scope);
		if (bindings != null)
		{
			/*
			 * Removal will be automatically made in Mapyrus context too.
			 */
			retval = bindings.remove(name);
		}
		return retval;
	}

	@Override
	public Object getAttribute(String name)
	{
		return getAttribute(name, ENGINE_SCOPE);
	}

	@Override
	public int getAttributesScope(String name)
	{
		int retval = -1;
		if (getAttribute(name) != null)
			retval = ENGINE_SCOPE;
		return retval;
	}

	@Override
	public Writer getWriter()
	{
		return m_writer;
	}

	@Override
	public void setWriter(Writer writer)
	{
		m_writer = writer;
	}

	@Override
	public Writer getErrorWriter()
	{
		return m_errorWriter;
	}

	@Override
	public void setErrorWriter(Writer writer)
	{
		m_errorWriter = writer;
	}

	@Override
	public Reader getReader()
	{
		return m_reader;
	}

	@Override
	public void setReader(Reader reader)
	{
		m_reader = reader;
	}

	@Override
	public List<Integer> getScopes()
	{
		ArrayList<Integer> scopes = new ArrayList<Integer>();
		scopes.add(ScriptContext.ENGINE_SCOPE);
		return scopes;
	}

	public Interpreter getInterpreter()
	{
		return m_interpreter;
	}

	public ContextStack getContextStack()
	{
		return m_context;
	}
}
