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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import org.mapyrus.MapyrusMessages;

public class MapyrusToolBar extends JToolBar
{
	static final long serialVersionUID = 0x331f;
	private ArrayList<MapyrusEventListener> m_toolBarListeners = new ArrayList<MapyrusEventListener>();

	public MapyrusToolBar()
	{
		/*
		 * Create green arrow icon.
		 */
		BufferedImage startImage = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics g = startImage.getGraphics();
		g.setColor(new Color(0, 192, 0));
		int []xPoints = {2, 14, 2, 2};
		int []yPoints = {2, 8, 14, 2};
		g.fillPolygon(xPoints, yPoints, xPoints.length);

		JButton runButton = new JButton(new ImageIcon(startImage));
		runButton.setToolTipText(MapyrusMessages.get(MapyrusMessages.RUN_COMMANDS));
		add(runButton);
		runButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fireEvent(MapyrusEventListener.STOP_ACTION);
				fireEvent(MapyrusEventListener.RUN_ACTION);
			}
		});

		/*
		 * Create red stop icon.
		 */
		BufferedImage stopImage = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		g = stopImage.getGraphics();
		g.setColor(Color.RED);
		g.fillRect(2, 2, 12, 12);

		JButton stopButton = new JButton(new ImageIcon(stopImage));
		stopButton.setToolTipText(MapyrusMessages.get(MapyrusMessages.STOP_COMMANDS));
		add(stopButton);
		stopButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fireEvent(MapyrusEventListener.STOP_ACTION);
			}
		});
	}

	public void addListener(MapyrusEventListener listener)
	{
		m_toolBarListeners.add(listener);
	}

	private void fireEvent(int actionCode)
	{
		for (MapyrusEventListener listener : m_toolBarListeners)
		{
			listener.actionPerformed(actionCode);
		}
	}
}