/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2012 Simon Chenery.
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

import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Returns unique filenames for use as temporary files.
 * Files expire after a given length of time and are automatically
 * deleted.
 */
public class TransientFileFactory
{
	/*
	 * Sequence number used in each unique filename.
	 */
	private static int m_counter = 0;

	/*
	 * List of generated filenames, sorted by expiry date (with
	 * file that is next to expire at start of list).
	 */
	private static LinkedList<TransientFile> m_generatedFilenames = new LinkedList<TransientFile>();

	/**
	 * Generate unique filename for use as temporary filename
	 * and schedule it to be automatically deleted.
	 * @param suffix is suffix for temporary filename.
	 * @param lifespan number of milliseconds after which the file
	 * is to be automatically deleted.
	 * @return unique filename.
	 */
	public static synchronized String generate(String suffix, int lifespan)
	{
		String retval;
		long random = (Double.doubleToLongBits(Math.random()) % 0xfffffff);
		long now = System.currentTimeMillis();
		long expiry = now + lifespan;

		/*
		 * Delete any previously generated temporary files that have expired.
		 */
		if (!m_generatedFilenames.isEmpty())
		{
			TransientFile expired = m_generatedFilenames.getFirst();
			while (expired != null && now >= expired.m_expiryTimestamp)
			{
				File f = new File(expired.m_filename);
				f.delete();
	
				m_generatedFilenames.removeFirst();
	
				if (m_generatedFilenames.isEmpty())
					expired = null;
				else
					expired = m_generatedFilenames.getFirst();
			}
		}

		/*
		 * Include random number and timestamp in filename to
		 * make it unpredictable.
		 */
		random += (now % 397);
		StringBuffer sb = new StringBuffer("tmp");
		sb.append(m_counter);
		sb.append("a");
		sb.append(Long.toString(random, Character.MAX_RADIX));
		if (suffix.length() > 0)
		{
			if (!suffix.startsWith("."))
				sb.append(".");
			sb.append(suffix);
		}
		retval = sb.toString();
		m_counter++;

		/*
		 * Insert newly created filename into list,
		 * maintaining the list in order of expiry.
		 */
		ListIterator<TransientFile> li = m_generatedFilenames.listIterator();
		int insertIndex = 0;
		boolean found = false;

		if (!m_generatedFilenames.isEmpty())
		{
			/*
			 * If entry expires at a later time than all other files we've
			 * generated then we can just add it to end of list.
			 */
			TransientFile t = m_generatedFilenames.getLast();
			if (t.m_expiryTimestamp <= expiry)
			{
				found = true;
				insertIndex = m_generatedFilenames.size();
			}
		}

		while (found == false && li.hasNext())
		{
			TransientFile t = li.next();
			if (t.m_expiryTimestamp > expiry)
			{
				insertIndex = li.nextIndex() - 1;
				found = true;
			}
		}

		TransientFile t = new TransientFile(retval, expiry);
		if (found)
			m_generatedFilenames.add(insertIndex, t);
		else
			m_generatedFilenames.addLast(t);

		return(retval);
	}
}
