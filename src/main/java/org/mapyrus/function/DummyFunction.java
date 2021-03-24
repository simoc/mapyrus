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

package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Placeholder for a function that is not available.
 * Will throw exception if the evaluate() method is ever called.
 */
public class DummyFunction implements Function
{
	private String m_functionName;

	public DummyFunction(String name)
	{
		m_functionName = name;
	}

	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.CLASS_NOT_FUNCTION));
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(4);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(0);
	}

	@Override
	public String getName()
	{
		return(m_functionName);
	}
}
