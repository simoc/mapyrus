/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003, 2004, 2005 Simon Chenery.
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

import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * A window displaying an image created by Mapyrus.
 */
public class MapyrusFrame
{
	private Mutex mMutex;

	/**
	 * Create new window displaying image.
	 * @param title tile for window.
	 * @param image image to display in window.
	 */
	public MapyrusFrame(String title, BufferedImage image)
	{
		JFrame frame = new JFrame(title);

		mMutex = new Mutex();
		mMutex.lock();

		Container contentPane = frame.getContentPane();
		ImageIcon icon = new ImageIcon(image);
		JLabel label = new JLabel(icon);
		contentPane.add(label);

		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				mMutex.unlock();
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Block until window is closed.
	 */
	public void waitForClose()
	{
		/*
		 * Wait for window to be closed and lock to be released.
		 */
		mMutex.lock();
	}
}
