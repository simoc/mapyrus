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

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Hold an image being copied to system clipboard for a cut-and-paste operation. 
 * Taken from example at http://forum.java.sun.com.
 */
public class ImageSelection implements Transferable
{
	private Image mImage;
	
	public ImageSelection(Image image)
	{
		mImage = image;
	}
	
	public DataFlavor []getTransferDataFlavors()
	{
		return new DataFlavor []{DataFlavor.imageFlavor};
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return(DataFlavor.imageFlavor.equals(flavor));
	}
	
	public Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		if (!DataFlavor.imageFlavor.equals(flavor))
		{
			throw new UnsupportedFlavorException(flavor);
		}
		return(mImage);
	}	
}
