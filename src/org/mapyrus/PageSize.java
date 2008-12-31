/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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
package org.mapyrus;

import java.awt.geom.Point2D;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

/**
 * A standard page size such as A4 or Letter.
 */
public class PageSize extends MediaSizeName
{
	private static final long serialVersionUID = 0x4a510002;

	private Point2D mDimension;

	/**
	 * Create page size from name.
	 * @param name name of page size.
	 */
	public PageSize(String name) throws MapyrusException
	{
		super(MediaSizeName.ISO_A4.getValue());

		/*
		 * Search list of all available paper sizes for name given by user.
		 */
		String isoName = "iso-" + name;
		String jisName = "jis-" + name;
		String naName = "na-" + name;
		String names[] = getStringTable();
		EnumSyntax enums[] = getEnumValueTable();
		int i = 0;
		while (i < names.length && mDimension == null)
		{
			if
			(
				name.equalsIgnoreCase(names[i]) ||
				isoName.equalsIgnoreCase(names[i]) ||
				jisName.equalsIgnoreCase(names[i]) ||
				naName.equalsIgnoreCase(names[i])
			)
			{
				MediaSize m = MediaSize.getMediaSizeForName((MediaSizeName)enums[i]);
				mDimension = new Point2D.Double(m.getX(Size2DSyntax.MM),
					m.getY(Size2DSyntax.MM));
			}
			i++;
		}
		if (mDimension == null)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_PAGE_SIZE) +
				": " + name);
		}
	}

	/**
	 * Get dimension of page in millimetres.
	 * @return page size in mm.
	 */
	public Point2D getDimension()
	{
		return(mDimension);
	}
}
