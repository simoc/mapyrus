package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;

/**
 * Function that creates a single string from a character code.
 * For example, chr(33) = '!'.
 */
public class Chr implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument arg1 = args.get(0);
		int c = (int)arg1.getNumericValue();
		StringBuffer sb = new StringBuffer();
		sb.append((char)c);
		Argument retval = new Argument(Argument.STRING, sb.toString());
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
		return("chr");
	}
}
