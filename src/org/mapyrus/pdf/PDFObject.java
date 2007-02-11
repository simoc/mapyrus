/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2007 Simon Chenery.
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
package org.mapyrus.pdf;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An object in a PDF file.
 */
public class PDFObject
{
	private String mValue = null;
	private int mReference = -1;
	private PDFObject[] mArray = null;
	private HashMap mDictionary = null;
	private long mStreamOffset = -1;

	/**
	 * Create new PDF object.
	 * @param simple object such as number or string.
	 */
	public PDFObject(String value)
	{
		mValue = value;
	}

	/**
	 * Create new PDF array object.
	 * @param array of objects.
	 */
	public PDFObject(PDFObject []array)
	{
		mArray = array;
	}

	/**
	 * Create new PDF dictionary object.
	 * @param dictionary of key, value pairs.
	 */
	public PDFObject(HashMap dictionary)
	{
		mDictionary = dictionary;
	}

	/**
	 * Create new PDF reference object.
	 * @param reference reference number of other object.
	 */
	public PDFObject(int reference)
	{
		mReference = reference;
	}

	/**
	 * Set file offset of stream for this object.
	 * @param offset file offset.
	 */
	public void setStreamOffset(long offset)
	{
		mStreamOffset = offset;
	}

	/**
	 * Get value for object.
	 * @return simple value stored for this object.
	 */
	public String getValue()
	{
		return(mValue);
	}

	/**
	 * Set object to be the same as another object
	 * @param anotherObject object to copy.
	 */
	public void setValue(PDFObject otherObject)
	{
		mValue = otherObject.mValue;
		mReference = otherObject.mReference;
		mArray = otherObject.mArray;
		mDictionary = otherObject.mDictionary;
		mStreamOffset = otherObject.mStreamOffset;
	}

	/**
	 * Check if this object is an array.
	 * @return true if object is an array.
	 */
	public boolean isArray()
	{
		return(mArray != null);
	}

	/**
	 * Get array for object.
	 * @return array stored for this object.
	 */
	public PDFObject[] getArray()
	{
		return(mArray);
	}

	/**
	 * Check if this object is a dictionary.
	 * @return true if object is a dictionary.
	 */
	public boolean isDictionary()
	{
		return(mDictionary != null);
	}

	/**
	 * Get dictionary for object.
	 * @return hash map containing key, value pairs.
	 */
	public HashMap getDictionary()
	{
		return(mDictionary);
	}

	/**
	 * Check if this object is a reference to another object.
	 * @return true if object is a reference.
	 */
	public boolean isReference()
	{
		return(mReference != -1);
	}

	/**
	 * Get reference for object.
	 * @return reference to other object.
	 */
	public int getReference()
	{
		return(mReference);
	}

	/**
	 * Get file offset for stream for this object.
	 * @return file offset.
	 */
	public long getStreamOffset()
	{
		return(mStreamOffset);
	}

	/**
	 * Return string representation of object.
	 * @return object as string.
	 */
	public String toString()
	{
		String retval;
		if (mValue != null)
			retval = mValue;
		else if (mArray != null)
		{
			StringBuffer sb = new StringBuffer("[");
			for (int i = 0; i < mArray.length; i++)
			{
				if (i > 0)
					sb.append(" ");
				sb.append(mArray[i]);
			}
			sb.append("]");
			retval = sb.toString();
		}
		else if (mDictionary != null)
		{
			StringBuffer sb = new StringBuffer("<<\n");
			Iterator it = mDictionary.keySet().iterator();
			while (it.hasNext())
			{
				String key = (String)it.next();
				String value = mDictionary.get(key).toString();
				sb.append(key);
				sb.append(" ");
				sb.append(value);
				sb.append("\n");
			}
			sb.append(">>");
			retval = sb.toString();
		}
		else
		{
			retval = mReference + " 0 R";
		}
		return(retval);
	}
}
