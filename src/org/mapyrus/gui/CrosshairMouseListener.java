/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import javax.swing.JPanel;

import org.mapyrus.Constants;

/**
 * Listener to display a crosshair cursor as mouse is moved around a panel.
 */
public class CrosshairMouseListener implements MouseMotionListener, MouseListener
{
	private static DecimalFormat m_crosshairFormat = new DecimalFormat("#.####", Constants.US_DECIMAL_FORMAT_SYMBOLS);
	private static Color m_crosshairBgColor = new Color(240, 240, 150);
	private static Font m_fixedFont = new Font("Monospaced", Font.PLAIN, 12);

	private BufferedImage m_displayImage;
	private Rectangle2D.Double m_displayImageWorlds;

	/**
	 * Set image being displayed in panel.
	 * @param displayImage image being displayed.
	 */
	public void setImage(BufferedImage displayImage)
	{
		m_displayImage = displayImage;
	}

	/**
	 * Set world coordinates of panel.
	 * @param worlds world coordinates being displayed in panel.
	 */
	public void setWorlds(Rectangle2D.Double worlds)
	{
		m_displayImageWorlds = worlds;
	}

	public void mouseMoved(MouseEvent e)
	{
		if (m_displayImage == null || m_displayImageWorlds == null)
			return;

		JPanel displayPanel = (JPanel)e.getComponent();

		/*
		 * Draw horizontal and vertical crosshairs.
		 */
		int x = e.getX();
		int y = e.getY();
		int panelWidth = displayPanel.getWidth();
		int panelHeight = displayPanel.getHeight();
		Graphics g = displayPanel.getGraphics();
		g.drawImage(m_displayImage, 0, 0, null);
		g.setColor(Color.BLACK);
		g.drawLine(0, y, panelWidth, y);
		g.drawLine(x, 0, x, panelHeight);
		g.setColor(m_crosshairBgColor);
		g.setFont(m_fixedFont);
		double dx = m_displayImageWorlds.getMinX() + (double)x / panelWidth * m_displayImageWorlds.getWidth();
		double dy = m_displayImageWorlds.getMinY() + (double)(panelHeight - 1 - y) / panelHeight * m_displayImageWorlds.getHeight();
		
		StringBuffer label = new StringBuffer();
		if (Math.abs(dx) < 1000)
			label.append(m_crosshairFormat.format(dx));
		else
			label.append(Math.round(dx));

		label.append(", ");

		if (Math.abs(dy) < 1000)
			label.append(m_crosshairFormat.format(dy));
		else
			label.append(Math.round(dy));

		/*
		 * Work out if we label is best displayed left or right, and
		 * above or below crosshairs.  Then display label highlighted
		 * one pixel all around so it is visible regardless of the
		 * background.
		 */
		Rectangle2D bounds = g.getFontMetrics().getStringBounds(label.toString(), g);
		int xOffset = (x <= panelWidth / 2) ? 2 : -(int)(bounds.getWidth() + 1);
		int yOffset = (y <= panelHeight / 2) ? (int)(bounds.getHeight() + 1) : -2;
		for (int x1 = -1; x1 <= 1; x1++)
		{
			for (int y1 = -1; y1 <= 1; y1++)
			{
				if (x1 != 0 || y1 != 0)
					g.drawString(label.toString(), x + xOffset + x1, y + yOffset + y1);
			}
		}
		g.setColor(Color.BLACK);
		g.drawString(label.toString(), x + xOffset, y + yOffset);
	}

	public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
		/*
		 * Remove the cursor when the mouse leaves the panel.
		 */
		JPanel displayPanel = (JPanel)e.getComponent();
		displayPanel.repaint();
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}
}