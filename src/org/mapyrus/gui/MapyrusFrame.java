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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.mapyrus.Constants;
import org.mapyrus.ContextStack;
import org.mapyrus.FileOrURL;
import org.mapyrus.ImageSelection;
import org.mapyrus.Interpreter;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.Mutex;

/**
 * Mapyrus GUI, allowing user to edit and run commands and see the output on the screen.
 */
public class MapyrusFrame implements MapyrusEventListener
{
	/*
	 * Font for command input and output.
	 */
	private static Font m_fixedFont = new Font("Monospaced", Font.PLAIN, 12);

	private Mutex m_mutex;
	private JFrame m_frame;
	private MapyrusEditorPanel m_editorPanel;
	private JTextArea m_outputTextArea;
	private Thread m_outputThread;
	private JPanel m_displayPanel;
	private LinkedBlockingQueue<Integer> m_actionQueue = null;
	private Thread m_actionThread;
	private BufferedImage m_displayImage;
	private File m_lastOpenedDirectory;

	public MapyrusFrame(String []filenames)
	{
		setLookAndFeel();
		createActionQueue();

		/*
		 * Create frame maximised to fill the whole screen.
		 */
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		m_frame = new JFrame(Constants.PROGRAM_NAME + " " + Constants.getVersion());
		m_frame.setPreferredSize(screenSize);

		m_mutex = new Mutex();
		m_mutex.lock();

		m_frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				/*
				 * If any changes saved successfully then we can exit.
				 */
				if (saveAndExit())
					m_mutex.unlock();
			}
		});

		Container contentPane = m_frame.getContentPane();
		contentPane.setLayout(new BorderLayout());

		/*
		 * Add menubar and menu options.
		 */
		MapyrusMenuBar menuBar = new MapyrusMenuBar();
		menuBar.addListener(this);

		contentPane.add(menuBar, BorderLayout.NORTH);

		/*
		 * Add display panel, toolbar, text editor panel, output panel.
		 */
		JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		m_displayPanel = new JPanel(){
			
			static final long serialVersionUID = 0x3302;

			public void paintComponent(Graphics g)
			{
				/*
				 * Double-buffering.  Redisplay image that Mapyrus has drawn.
				 */
				super.paintComponent(g);
				if (m_displayPanel != null && m_displayImage != null)
					g.drawImage(m_displayImage, 0, 0, null);
			}
		};
		m_displayPanel.setPreferredSize(screenSize);
		splitPane1.add(m_displayPanel);

		JPanel toolBarAndEditorPanel = new JPanel();
		toolBarAndEditorPanel.setLayout(new BorderLayout());

		MapyrusToolBar toolBar = new MapyrusToolBar();
		toolBar.addListener(this);
		toolBarAndEditorPanel.add(toolBar, BorderLayout.NORTH);

		m_editorPanel = new MapyrusEditorPanel();
		m_editorPanel.setFont(m_fixedFont);
		toolBarAndEditorPanel.add(m_editorPanel, BorderLayout.CENTER);
		splitPane2.add(toolBarAndEditorPanel);

		m_outputTextArea = new JTextArea(2, 80);
		m_outputTextArea.setBackground(Color.WHITE);
		m_outputTextArea.setFont(m_fixedFont);
		m_outputTextArea.setEditable(false);
		JScrollPane outputPane = new JScrollPane(m_outputTextArea);
		splitPane2.add(outputPane);

		splitPane1.add(splitPane2);
		contentPane.add(splitPane1, BorderLayout.CENTER);
		m_frame.pack();

		/*
		 * Show mostly the display panel and only a few lines of the editor
		 * and output.
		 */
		splitPane1.setDividerLocation(0.60);

		m_frame.setVisible(true);

		/*
		 * Add each file as a tab.
		 */
		if (filenames != null)
		{
			for (int i = 0; i < filenames.length; i++)
			{
				m_editorPanel.createTab(filenames[i], null);
			}
		}
		else
		{
			m_editorPanel.createTab(null, null);
		}
		m_editorPanel.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e)
			{
				/*
				 * Re-interpret commands in current tab and display output.
				 */
			}
		});

		waitForClose();
	}

	/**
	 * Create new window displaying image.
	 * @param title tile for window.
	 * @param image image to display in window.
	 */
	public MapyrusFrame(String title, BufferedImage image)
	{
		setLookAndFeel();
		createActionQueue();
		m_displayImage = image;
		m_frame = new JFrame(title);

		Container contentPane = m_frame.getContentPane();
		contentPane.setLayout(new BorderLayout());

		/*
		 * Add menubar and menu options.
		 */
		MapyrusMenuBar menuBar = new MapyrusMenuBar();
		menuBar.addListener(this);

		contentPane.add(menuBar, BorderLayout.NORTH);

		ImageIcon icon = new ImageIcon(image);
		final JLabel label = new JLabel(icon);

		/*
		 * Put image on an icon, putting it in a scrollable area if it is bigger
		 * than the screen.
		 */
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (image.getWidth() > screenSize.getWidth() ||
			image.getHeight() > screenSize.getHeight() - 50)
		{
			ScrollPane pane = new ScrollPane();
			pane.add(label);
			contentPane.add(pane, BorderLayout.CENTER);
		}
		else
		{
			contentPane.add(label, BorderLayout.CENTER);
		}

		m_mutex = new Mutex();
		m_mutex.lock();

		m_frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				/*
				 * If any changes saved successfully then we can exit.
				 */
				if (saveAndExit())
					m_mutex.unlock();
			}
		});

		m_frame.pack();
		m_frame.setVisible(true);
		waitForClose();
	}

	private void setLookAndFeel()
	{
		try
		{
			/*
			 * Set operating system specific LookAndFeel.
			 */
			String className = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(className);
		}
		catch (UnsupportedLookAndFeelException e)
		{
		}
		catch (ClassNotFoundException e)
		{
		}
		catch (IllegalAccessException e)
		{
		}
		catch (InstantiationException e)
		{
		}
	}

	private void createActionQueue()
	{
		/*
		 * Create or recreate queue of actions from GUI.
		 */
		if (m_actionQueue != null)
			m_actionQueue.clear();
		else
			m_actionQueue = new LinkedBlockingQueue<Integer>();

		/*
		 * Create another thread to read this queue and process
		 * actions so that the GUI is not blocked.
		 */
		m_actionThread = new Thread(){
			public void run()
			{
				try
				{
					processActions();
				}
				catch (InterruptedException e)
				{
				}
			}
		};
		m_actionThread.start();
	}

	public void actionPerformed(int actionCode)
	{
		if (actionCode == MapyrusEventListener.STOP_ACTION ||
			actionCode == MapyrusEventListener.EXIT_ACTION)
		{
			/*
			 * Interrupt any action that is already running,
			 * clear queue and restart thread that handles events.
			 * 
			 * This ensures that event is handled immediately.
			 */
			m_actionThread.interrupt();

			if (m_outputThread != null)
			{
				m_outputThread.interrupt();
				m_outputThread = null;
			}

			createActionQueue();
		}

		/*
		 * Add action to queue.
		 */
		try
		{
			m_actionQueue.put(Integer.valueOf(actionCode));
		}
		catch (InterruptedException e)
		{
		}
	}

	public void processActions() throws InterruptedException
	{
		while (true)
		{
			Integer actionCode =  m_actionQueue.take().intValue();

			if (actionCode == MapyrusEventListener.NEW_TAB_ACTION)
			{
				/*
				 * Create new tab in editor panel.
				 */
				m_editorPanel.createTab(null, null);
			}
			else if (actionCode == MapyrusEventListener.OPEN_FILE_ACTION)
			{
				/*
				 * Show file chooser dialog for user to select a file
				 * from, show it in a new tab and the output in the
				 * display window.
				 */
				openFile();
			}
			else if (actionCode == MapyrusEventListener.COPY_ACTION)
			{
				/*
				 * Copy display panel output to clipboard.
				 */
				ImageSelection imageSelection = new ImageSelection(m_displayImage);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(imageSelection, null);
			}
			else if (actionCode == MapyrusEventListener.EXPORT_ACTION)
			{
				exportToPNG();
			}
			else if (actionCode == MapyrusEventListener.RUN_ACTION)
			{
				runCommands();
			}
			else if (actionCode == MapyrusEventListener.CLOSE_TAB_ACTION)
			{
				/*
				 * Close currently open tab.
				 */
				if (m_editorPanel.getTabCount() > 0)
				{
					boolean status = true;
					if (m_editorPanel.isSelectedTabEdited())
						status = saveTab(true);
					if (status)
						m_editorPanel.closeSelectedTab();
				}
			}
			else if (actionCode == MapyrusEventListener.SAVE_TAB_ACTION)
			{
				/*
				 * Save currently open tab.
				 */
				if (m_editorPanel.getTabCount() > 0)
				{
					saveTab(false);
				}
			}
			else if (actionCode == MapyrusEventListener.EXIT_ACTION)
			{
				/*
				 * If any changes saved successfully then we can exit.
				 */
				if (saveAndExit())
					m_mutex.unlock();
			}
			else if (actionCode == MapyrusEventListener.ONLINE_HELP_ACTION)
			{
				/*
				 * Show HTML GUI help page.
				 */
				JFrame helpFrame = new JFrame();
				try
				{
					JEditorPane helpPane = new JEditorPane();
					helpPane.setEditable(false);
					URL helpUrl = this.getClass().getResource("onlinehelp.html");
					helpPane.setPage(helpUrl);
					helpFrame.getContentPane().add(helpPane);
					helpFrame.setPreferredSize(new Dimension(600, 600));
					helpFrame.pack();
					helpFrame.setVisible(true);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(m_frame, e.getMessage(),
						Constants.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
					helpFrame.dispose();
				}
			}
			else if (actionCode == MapyrusEventListener.ABOUT_ACTION)
			{
				/*
				 * Show version and license information.
				 */
				StringBuffer sb = new StringBuffer();
				sb.append(Constants.PROGRAM_NAME).append(" ").append(Constants.getVersion());
				sb.append(", ").append(Constants.getReleaseDate());
				sb.append(" ").append(Constants.WEB_SITE);
				sb.append(Constants.LINE_SEPARATOR);
				sb.append(Constants.LINE_SEPARATOR);
	
				String []license = Constants.getLicense();
				for (int i = 0; i < license.length; i++)
					sb.append(license[i]).append(Constants.LINE_SEPARATOR);
				JOptionPane.showMessageDialog(m_frame, sb.toString(),
					Constants.PROGRAM_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/*
	 * File filter limiting file selection to PNG images. 
	 */
	private class PNGImageFilter extends FileFilter
	{
		public boolean accept(File f)
		{
			boolean retval = f.isDirectory();
			if (!retval)
			{
				String name = f.getName();
				retval = name.endsWith(".png") || name.endsWith(".PNG");
			}
			return(retval);
		}

		public String getDescription()
		{
			return(MapyrusMessages.get(MapyrusMessages.PNG_IMAGE_FILES));
		}
	}

	/**
	 * Open file with Mapyrus commands in new tab.
	 */
	private void openFile()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setCurrentDirectory(m_lastOpenedDirectory);
		int status = chooser.showOpenDialog(m_frame);
		if (status == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = chooser.getSelectedFile();
			m_lastOpenedDirectory = selectedFile.getParentFile();
			String filename = selectedFile.getPath();
			m_editorPanel.createTab(filename, null);
		}
	}

	/**
	 * Export image to a PNG file.
	 */
	private void exportToPNG()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(new PNGImageFilter());
		fileChooser.setSelectedFile(m_lastOpenedDirectory);

		int retval = fileChooser.showSaveDialog(m_frame);
		if (retval == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			m_lastOpenedDirectory = selectedFile;
			try
			{
				FileOutputStream outStream = new FileOutputStream(selectedFile);
				ImageIO.write(m_displayImage, "png", outStream);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(m_frame, e.getMessage(), Constants.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
				selectedFile.delete();
			}
		}
	}

	/**
	 * Run commands in currently selected tab, showing output in display window.
	 */
	private void runCommands() throws InterruptedException
	{
		String contents = m_editorPanel.getSelectedTabContents();
		if (contents.length() > 0)
		{
			try
			{
				String title = m_editorPanel.getSelectedTabTitle();
				FileOrURL f = new FileOrURL(new StringReader(contents), title);

				Dimension displayDim = m_displayPanel.getSize();
				m_displayPanel.getGraphics().clearRect(0, 0, displayDim.width, displayDim.height);
				m_displayImage = new BufferedImage((int)displayDim.getWidth(),
					(int)displayDim.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
				Interpreter interpreter = new Interpreter();
				ContextStack context = new ContextStack();
				context.setOutputFormat(m_displayImage, "");

				ByteArrayInputStream stdin = new ByteArrayInputStream(new byte[]{});
				PipedOutputStream outStream = new PipedOutputStream();
				final PipedInputStream inStream = new PipedInputStream(outStream);
				m_outputTextArea.setText("");

				/*
				 * Create thread to read Mapyrus output and append it
				 * to the output panel.
				 */
				m_outputThread = new Thread(){
					public void run()
					{
						try
						{
							byte []buf = new byte[256];
							int nBytes;
							while ((nBytes = inStream.read(buf)) > 0)
							{
								String s = new String(buf, 0, nBytes);
								int caretPosition = m_outputTextArea.getCaretPosition();
								m_outputTextArea.append(s);
								
								/*
								 * Ensure last lines of output are displayed.
								 */
								m_outputTextArea.setCaretPosition(caretPosition + s.length());
								m_outputTextArea.repaint();
							}
						}
						catch (IOException e)
						{
							/*
							 * Reading pipes so should be no IOExceptions.
							 */
							m_outputTextArea.append(e.getMessage());
						}
					}
				};
				m_outputThread.start();

				PrintStream p = new PrintStream(outStream);
				interpreter.interpret(context, f, stdin, p);
				p.close();
				if (m_outputThread != null)
				{
					m_outputThread.join();
					m_outputThread = null;
				}

				m_outputTextArea.repaint();
				m_displayPanel.repaint();
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(m_frame, e.getMessage(), Constants.PROGRAM_NAME,
					JOptionPane.ERROR_MESSAGE);
			}
			catch (MapyrusException e)
			{
				JOptionPane.showMessageDialog(m_frame, e.getMessage(), Constants.PROGRAM_NAME,
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Block until window is closed.
	 */
	private void waitForClose()
	{
		m_mutex.lock();
	}

	/**
	 * Save selected tab to a file.
	 * @param promptSave first show dialog asking user if they want to save.
	 * @return false if user changes their mind and decides not to save tab.
	 */
	private boolean saveTab(boolean promptSave)
	{
		int status = JOptionPane.OK_OPTION;

		String filename = m_editorPanel.getSelectedTabFilename();
		if (promptSave)
		{
			String message = MapyrusMessages.get(MapyrusMessages.SAVE_CHANGES_IN_TAB) +
				" " + m_editorPanel.getSelectedTabTitle();
			if (!filename.startsWith(MapyrusMessages.get(MapyrusMessages.UNTITLED)))
			{
				message = message + "\n" +
					MapyrusMessages.get(MapyrusMessages.TO_FILE) + " " +
					filename;
			}
			status = JOptionPane.showConfirmDialog(m_frame,
				message + "?", Constants.PROGRAM_NAME, JOptionPane.YES_NO_CANCEL_OPTION);
		}

		if (status == JOptionPane.CANCEL_OPTION)
		{
			return(false);
		}
		else if (status == JOptionPane.OK_OPTION)
		{
			/*
			 * Find filename for this tab.
			 */		
			if (filename.startsWith(MapyrusMessages.get(MapyrusMessages.UNTITLED)))
			{
				/*
				 * Give user a chance to specify a different filename.
				 */
				do
				{
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogType(JFileChooser.SAVE_DIALOG);
					chooser.setSelectedFile(new File(m_lastOpenedDirectory, filename));
					status = chooser.showSaveDialog(m_frame);
					if (status != JFileChooser.APPROVE_OPTION)
						return(false);
					File selectedFile = chooser.getSelectedFile();
					m_lastOpenedDirectory = selectedFile.getParentFile();
					filename = selectedFile.getPath();
					if (selectedFile.exists())
					{
						/*
						 * Check that the user wants to overwrite this file.
						 */
						status = JOptionPane.showConfirmDialog(m_frame,
							MapyrusMessages.get(MapyrusMessages.OVERWRITE) + " " + filename +"?",
							Constants.PROGRAM_NAME,
							JOptionPane.YES_NO_OPTION);
					}
				}
				while (status != JOptionPane.YES_OPTION);
			}
			FileWriter f = null;
			try
			{
				/*
				 * Write the tab contents to file.
				 */
				f = new FileWriter(filename);
				String contents = m_editorPanel.getSelectedTabContents();
				f.write(contents);
				f.flush();
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(m_frame, e.getMessage(),
					Constants.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
				return(false);
			}
			finally
			{
				try
				{
					f.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return(true);
	}

	/**
	 * Ask user if they want to save any edited tabs before exiting.
	 * @return true if all tabs saved and/or closed.
	 */
	private boolean saveAndExit()
	{
		boolean retval = true;
		while (retval && m_editorPanel.getTabCount() > 0)
		{
			if (m_editorPanel.isSelectedTabEdited())
				retval = saveTab(true);
			if (retval)
				m_editorPanel.closeSelectedTab();
		}
		return(retval);
	}
}
