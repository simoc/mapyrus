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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.mapyrus.Constants;
import org.mapyrus.FileOrURL;
import org.mapyrus.MapyrusMessages;

/**
 * Text editor panel for editing commands to be run by Mapyrus.
 */
public class MapyrusEditorPanel extends JTabbedPane implements KeyListener
{
	static final long serialVersionUID = 0x3301;
	
	private static final Color LINE_NUMBERING_COLOR = new Color(139, 0, 0);

	/*
	 * Text area for each scroll pane and list of panels that have been edited.
	 */
	private HashMap<JScrollPane, JTextArea> m_textAreas = new HashMap<JScrollPane, JTextArea>();
	private HashSet<JScrollPane> m_editedScrollPanes = new HashSet<JScrollPane>();
	private Font m_font = null;

	private int m_tabSequenceNumber = 0;

	/**
	 * Create text editor panel.
	 */
	public MapyrusEditorPanel()
	{
		setPreferredSize(new Dimension(640, 120));
	}

	/**
	 * Set editor font.
	 * @font font for text area.
	 */
	public void setFont(Font font)
	{
		m_font = font;
	}

	/**
	 * Create panel with line number information for a text area. 
	 * @param textArea text area to add line numbering to.
	 * @return line numbering panel.
	 */
	private JComponent createLineNumberingComponent(JScrollPane scrollPane, JTextArea textArea)
	{
		final Font finalFont = textArea.getFont();
		JComponent lineNumberingComponent = new JComponent(){
			static final long serialVersionUID = 0x1;
			public void paintComponent(Graphics g)
			{
				FontMetrics fontMetrics = getFontMetrics(finalFont);
                int lineHeight = fontMetrics.getHeight();
                int startOffset = 3;
                Rectangle box = g.getClipBounds();

                /*
                 * Fill the background.
                 */
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(box.x, box.y, box.width, box.height);

                /*
                 * Work out which line numbers to display.
                 */
                g.setColor(LINE_NUMBERING_COLOR);
                int startLineNumber = (box.y / lineHeight) + 1;
                int endLineNumber = startLineNumber + (box.height / lineHeight);

                int start = (box.y / lineHeight) * lineHeight + lineHeight - startOffset;

                for (int i = startLineNumber; i <= endLineNumber; i++)
                {
                        String lineNumber = String.valueOf(i);
                        g.drawString(lineNumber, 0, start);
                        start += lineHeight;
                }
			}
		};

		lineNumberingComponent.setPreferredSize(new Dimension(34, Integer.MAX_VALUE / 2));

		return(lineNumberingComponent);
	}

	/**
	 * Create new tab in editor.
	 * @param filename name of file to open in tab, if null then empty tab created.
	 * @param tabName name for tab, if null then filename is used.
	 * @param bgColor panel background color, or null for default color.
	 */
	public void createTab(String filename, String tabName, Color bgColor)
	{
		JTextArea textArea = new JTextArea();
		if (bgColor != null)
			textArea.setBackground(bgColor);
		textArea.addKeyListener(this);
		if (m_font != null)
			textArea.setFont(m_font);
		JScrollPane scrollPane = new JScrollPane(textArea);
		JComponent lineNumberingComponent = createLineNumberingComponent(scrollPane, textArea);
		scrollPane.setRowHeaderView(lineNumberingComponent);
		m_textAreas.put(scrollPane, textArea);

		if (filename != null)
		{
			/*
			 * Add tab with contents of a file.
			 */
			Reader r = null;
			try
			{
				FileOrURL f = new FileOrURL(filename);
				r = f.getReader();
				StringBuffer sb = new StringBuffer();
				char []buf = new char[512];
				int nChars;
				while ((nChars = r.read(buf)) > 0)
				{
					sb.append(buf, 0, nChars);
				}
				textArea.setText(sb.toString());
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this, e.getMessage(),
					Constants.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
				return;
			}
			finally
			{
				try
				{
					if (r != null)
						r.close();
				}
				catch (IOException e)
				{
				}
			}

			if (tabName == null)
			{
				/*
				 * Set tab name to just filename, with any .mapyrus suffix removed.
				 */
				File f = new File(filename);
				tabName = f.getName();
				String suffix = "." + Constants.PROGRAM_NAME.toLowerCase();
				if (tabName.toLowerCase().endsWith(suffix))
					tabName = tabName.substring(0, tabName.length() - suffix.length());
			}
			add(tabName, scrollPane);
			setToolTipTextAt(getTabCount() - 1, filename);
		}
		else
		{
			/*
			 * Add empty tab.
			 */
			if (tabName == null)
			{
				tabName = MapyrusMessages.get(MapyrusMessages.UNTITLED);
				if (++m_tabSequenceNumber > 1)
					tabName += "_" + m_tabSequenceNumber;
			}
			add(tabName, scrollPane);
			setToolTipTextAt(getTabCount() - 1,
				tabName + "." + Constants.PROGRAM_NAME.toLowerCase());
		}
		setSelectedComponent(scrollPane);
	}

	/**
	 * Append text to currently selected tab.
	 * @param newText text to append.
	 */
	public void appendToSelectedTextArea(String newText)
	{
		JTextArea textArea = getSelectedTextArea();
		if (textArea != null)
			textArea.append(newText);
	}

	/**
	 * Get text area of currently selected tab.
	 * @return text area.
	 */
	private JTextArea getSelectedTextArea()
	{
		JTextArea retval = null;
		JScrollPane scrollPane = (JScrollPane)getSelectedComponent();
		if (scrollPane != null)
			retval = m_textAreas.get(scrollPane);
		return(retval);
	}

	/**
	 * Get contents of currently selected tab.
	 * @return contents.
	 */
	public String getSelectedTabContents()
	{
		String retval = "";
		JTextArea textArea = getSelectedTextArea();
		if (textArea != null)
			retval = textArea.getText();
		return(retval);
	}

	/**
	 * Get title of currently selected tab.
	 * @return tab title.
	 */
	public String getSelectedTabTitle()
	{
		String retval = "";
		int index = getSelectedIndex();
		if (index >= 0)
			retval = getTitleAt(index);
		return(retval);
	}

	/**
	 * Get filename of currently selected tab.
	 * @return filename.
	 */
	public String getSelectedTabFilename()
	{
		String retval = "";
		int index = getSelectedIndex();
		if (index >= 0)
			retval = getToolTipTextAt(index);
		return(retval);
	}

	/**
	 * Close currently selected tab.
	 */
	public void closeSelectedTab()
	{
		JScrollPane scrollPane = (JScrollPane)getSelectedComponent();
		if (scrollPane != null)
		{
			m_textAreas.remove(scrollPane);
			remove(scrollPane);
		}
	}

	/**
	 * Check if text in selected tab has been edited (and needs to be saved before exiting).
	 * @return true if text has been edited.
	 */
	public boolean isSelectedTabEdited()
	{
		boolean retval = false;
		JScrollPane scrollPane = (JScrollPane)getSelectedComponent();
		if (scrollPane != null)
			retval = m_editedScrollPanes.contains(scrollPane);
		return(retval);
	}

	/**
	 * Mark selected tab as edited or not.
	 * @param isEdited true if tab is to be marked as edited.
	 */
	public void setSelectedTabEdited(boolean isEdited)
	{
		JScrollPane scrollPane = (JScrollPane)getSelectedComponent();
		if (scrollPane != null)
		{
			if (isEdited)
				m_editedScrollPanes.add(scrollPane);
			else
				m_editedScrollPanes.remove(scrollPane);
		}
	}

	public void keyPressed(KeyEvent event)
	{
		/*
		 * User typed in a tab, mark that tab as modified.
		 */
		setSelectedTabEdited(true);
	}

	public void keyReleased(KeyEvent event)
	{	
	}

	public void keyTyped(KeyEvent event)
	{
	}
}
