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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * A window displaying an image created by Mapyrus.
 */
public class MapyrusFrame
{
	private Mutex mMutex;
	JFrame mFrame;
	BufferedImage mImage;

	/**
	 * Create new window displaying image.
	 * @param title tile for window.
	 * @param image image to display in window.
	 */
	public MapyrusFrame(String title, BufferedImage image)
	{
		mImage = image;

		/*
		 * Put image on an icon, reducing it to a reasonable size if it is bigger
		 * than the screen.
		 */
		double screenWidth = Constants.getScreenWidth() / Constants.MM_PER_INCH *
			Constants.getScreenResolution();
		double screenHeight = Constants.getScreenHeight() / Constants.MM_PER_INCH *
			Constants.getScreenResolution();
		double widthReduction = image.getWidth() / screenWidth;
		double heightReduction = image.getHeight() / screenHeight;
		Image reducedImage;
		if (widthReduction > 1 || heightReduction > 1)
		{
			int reduction = (int)Math.max(Math.ceil(widthReduction), Math.ceil(heightReduction));
			long reducedWidth = Math.round((double)image.getWidth() / reduction);
			long reducedHeight = Math.round((double)image.getHeight() / reduction);
			reducedImage = image.getScaledInstance((int)reducedWidth,
				(int)reducedHeight, Image.SCALE_SMOOTH);
				
			title = title + " (" + Math.round(100.0 / reduction) + "% of actual size)";
		}
		else
		{
			reducedImage = image;
		}

		ImageIcon icon = new ImageIcon(reducedImage);
		final JLabel label = new JLabel(icon);

		mFrame = new JFrame(title);

		mMutex = new Mutex();
		mMutex.lock();

		Container contentPane = mFrame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		JMenuBar menubar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem pngExportItem = new JMenuItem("Export as PNG");
		fileMenu.add(pngExportItem);
		pngExportItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				export("png");
			}
		});

		JMenuItem jpegExportItem = new JMenuItem("Export as JPEG");
		fileMenu.add(jpegExportItem);
		jpegExportItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				export("jpeg");
			}
		});

		JMenuItem printItem = new JMenuItem("Print");
		fileMenu.addSeparator();
		fileMenu.add(printItem);
		printItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				print();
			}
		});

		JMenuItem exitItem = new JMenuItem("Exit");
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		exitItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				mMutex.unlock();
			}
		});

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		JMenuItem copyItem = new JMenuItem("Copy");
		copyItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable t = clipboard.getContents(this);
				//TODO find out how to do this from the internet.
				//clipboard.setContents(new Transferable(), this);
				//TransferHandler handler = label.getTransferHandler();
				//handler.exportToClipboard(label, clipboard, TransferHandler.COPY);

				//Transferable contents = 
				//clipboard.setContents(contents, owner);
				//DropTargetContext c = new DropTargetContext();
				//.TransferableProxy();
				//clipboard.
				//ClipboardTransferable.
				System.err.println("Copying to clipboard");
			}
		});
		editMenu.add(copyItem);

		menubar.add(fileMenu);
		menubar.add(editMenu);
		contentPane.add(menubar, BorderLayout.NORTH);


		contentPane.add(label, BorderLayout.CENTER);

		mFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				mMutex.unlock();
			}
		});

		mFrame.pack();
		mFrame.setVisible(true);
	}

	/**
	 * Print image.
	 */
	private void print()
	{
		PrintService defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();

		if (defaultPrintService == null)
		{
			JOptionPane.showMessageDialog(mFrame, "No default printer found",
				"Print Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			DocPrintJob pj = defaultPrintService.createPrintJob();
			DocFlavor flavor = new DocFlavor("application/x-java-jvm-local-objectref",
				"java.awt.image.renderable.RenderableImage");
			HashDocAttributeSet attribSet = new HashDocAttributeSet();
			SimpleDoc doc = new SimpleDoc(mImage, flavor, attribSet);

			HashPrintRequestAttributeSet attribs = new HashPrintRequestAttributeSet();
			pj.print(doc, attribs);
		}
		catch (PrintException e)
		{
			JOptionPane.showMessageDialog(mFrame, e.getMessage(),
				"Print Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Export image to a file.
	 * @param format file format to export to.
	 */
	private void export(String format)
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		int retval = fileChooser.showSaveDialog(mFrame);
		if (retval == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				File selectedFile = fileChooser.getSelectedFile();
				ImageIO.write(mImage, format, selectedFile);
			}
			catch (IOException e)
			{
				System.err.println(e.getMessage());
			}
		}
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
