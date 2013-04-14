/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2013 Simon Chenery.
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

package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.Constants;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.TransientFileFactory;

/**
 * Function returning unique name for temporary file.
 * For example, tempname('.jpg') = 'tmpABC123.jpg'.
 */
public class Tempname implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		/*
 		 * Generate name for temporary file with given suffix.
 		 */
		Argument arg1 = args.get(0);
		Argument retval = new Argument(Argument.STRING,
			TransientFileFactory.generate(arg1.toString(), Constants.HTTP_TEMPFILE_LIFESPAN));
		return(retval);
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(1);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("tempname");
	}
}
