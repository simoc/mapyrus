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

package org.mapyrus;

import java.util.ArrayList;

/**
 * A row read from a geographic dataset containing one geometrical object plus
 * its attributes.
 * Or as the OGIS people would say, a "simple feature".
 */
public class Row extends ArrayList<Argument>
{
	private static final long serialVersionUID = 0x4a510001;

	/**
	 * Create a new row.
	 */
	public Row()
	{
		super();
	}

	/**
	 * Create a new row.
	 * @param size initial number of fields for row.
	 */
	public Row(int size)
	{
		super(size);
	}
}
