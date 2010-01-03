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
package org.mapyrus.gui;

public interface MapyrusEventListener
{
	public static int EXIT_ACTION = 1;
	public static int EXPORT_ACTION = 2;
	public static int COPY_ACTION = 3;
	public static int NEW_TAB_ACTION = 4;
	public static int OPEN_FILE_ACTION = 5;
	public static int CLOSE_TAB_ACTION = 6;
	public static int SAVE_TAB_ACTION = 7;
	public static int ONLINE_HELP_ACTION = 8;
	public static int ABOUT_ACTION = 9;
	public static int RUN_ACTION = 10;
	public static int STOP_ACTION = 11;

	public void actionPerformed(int actionCode);
}
