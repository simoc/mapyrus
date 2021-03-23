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

import java.util.HashMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

/**
 * Key/value pairs containing variables for Mapyrus ScriptEngine.
 */
public class MapyrusBindings extends HashMap<String, Object> implements Bindings
{
	public static final long serialVersionUID = 1;

	/**
	 * The ScriptContext that this Bindings is associated with.
	 */
	ScriptContext m_context;

	public void setScriptContext(ScriptContext context)
	{
		m_context = context;
	}

	@Override
	public void clear()
	{
		super.clear();

		/*
		 * Update changed values directly in Mapyrus context too.
		 */
		if (m_context != null)
			m_context.setBindings(this, ScriptContext.ENGINE_SCOPE);
	}

	@Override
	public Object put(String key, Object value)
	{
		Object retval = super.put(key, value);

		/*
		 * Update changed values directly in Mapyrus context too.
		 */
		if (m_context != null)
			m_context.setBindings(this, ScriptContext.ENGINE_SCOPE);
		return retval;
	}

	@Override
	public Object remove(Object key)
	{
		Object retval = super.remove(key);

		/*
		 * Update changed values directly in Mapyrus context too.
		 */
		if (m_context != null)
			m_context.setBindings(this, ScriptContext.ENGINE_SCOPE);
		return retval;
	}
}