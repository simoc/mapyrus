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

package org.mapyrus.font;

/**
 * Holds height, width, ascent and descent of a string as double precision values.
 * Provides get and set methods for these values.
 */
public class StringDimension
{
	private double m_height;
	private double m_width;
	private double m_ascent;
	private double m_descent;

	/**
	 * Create new dimension, with zero height and width.
	 */
	public StringDimension()
	{
		m_height = m_width = m_ascent = m_descent = 0.0; 
	}

	/**
	 * Set dimension.
	 * @param width width to set.
	 * @param height height to set.
	 */
	public void setSize(double width, double height, double ascent, double descent)
	{
		m_height = height;
		m_width = width;
		m_ascent = ascent;
		m_descent = descent;
	}

	/**
	 * Get string height.
	 * @return string height.
	 */
	public double getHeight()
	{
		return(m_height);
	}

	/**
	 * Get string width.
	 * @return string width.
	 */
	public double getWidth()
	{
		return(m_width);
	}

	/**
	 * Get string ascent.
	 * @return string ascent.
	 */
	public double getAscent()
	{
		return(m_ascent);
	}

	/**
	 * Get string descent.
	 * @return string descent.
	 */
	public double getDescent()
	{
		return(m_descent);
	}

	/**
	 * Get dimension as a string.
	 * @return dimension string.
	 */
	public String toString()
	{
		return(m_width + " x " + m_height);
	}
}
