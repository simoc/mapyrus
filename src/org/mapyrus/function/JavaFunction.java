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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;
import org.mapyrus.NumericalAnalysis;

/**
 * A function implemented as a Java method.
 */
public class JavaFunction implements Function
{
	private String m_className;
	private String m_methodName;
	private ArrayList<Method> m_methods;
	private int m_minArgs, m_maxArgs;

	/**
	 * Create function that calls a Java method.
	 */
	public JavaFunction(String className, String methodName, ArrayList<Method> methods)
	{
		m_className = className;
		m_methodName = methodName;
		m_methods = methods;
		m_minArgs = Integer.MAX_VALUE;
		m_maxArgs = 0;
		for (Method method : methods)
		{
			int nParams = method.getParameterTypes().length;
			if (nParams < m_minArgs)
				m_minArgs = nParams;
			if (nParams > m_maxArgs)
				m_maxArgs = nParams;
		}
	}

	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException, InterruptedException
	{
		if (!context.getThrottle().isIOAllowed())
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.NO_JAVA_FUNCTIONS));
		}

		Argument retval = null;
		int nArgs = args.size();
		Object methodArgs[] = new Object[nArgs];

		int i = 0;
		for (i = 0; i < m_methods.size() && retval == null; i++)
		{
			Method method = m_methods.get(i);
			Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length == nArgs)
			{
				/*
				 * Translate each argument passed to function into correct Java type.
				 */
				boolean validParamterTypes = true;
				for (int j = 0; j < nArgs && validParamterTypes; j++)
				{
					String paramName = parameters[j].getName();
					if (paramName.equals("java.lang.Object") || paramName.equals("java.lang.String"))
					{
						methodArgs[j] = args.get(j).toString();
					}
					else if (paramName.equals(Argument.class.getSimpleName()))
					{
						methodArgs[j] = args.get(j);
					}
					else if (paramName.equals("boolean") || paramName.equals("java.lang.Boolean"))
					{
						boolean isTrue;
						String s = args.get(j).toString();
						if (s.equalsIgnoreCase("true"))
							isTrue = true;
						else if (s.equalsIgnoreCase("false"))
							isTrue = false;
						else
						{
							double d = args.get(j).getNumericValue();
							boolean isZero = NumericalAnalysis.equals(d, 0);
							isTrue = !isZero;
						}
						methodArgs[j] = Boolean.valueOf(isTrue);
					}
					else if (paramName.equals("byte") || paramName.equals("java.lang.Byte"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Byte.valueOf((byte)d);
					}
					else if (paramName.equals("char") || paramName.equals("java.lang.Character"))
					{
						String s = args.get(j).toString();
						char c;
						if (s.length() > 0)
							c = s.charAt(0);
						else
							c = 0;
						methodArgs[j] = Character.valueOf(c);
					}
					else if (paramName.equals("short") || paramName.equals("java.lang.Short"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Short.valueOf((short)d);
					}
					else if (paramName.equals("int") || paramName.equals("java.lang.Integer"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Integer.valueOf((int)d);
					}
					else if (paramName.equals("long") || paramName.equals("java.lang.Long"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Long.valueOf((long)d);
					}
					else if (paramName.equals("float") || paramName.equals("java.lang.Float"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Float.valueOf((float)d);
					}
					else if (paramName.equals("double") || paramName.equals("java.lang.Double"))
					{
						double d = args.get(j).getNumericValue();
						methodArgs[j] = Double.valueOf(d);
					}
					else
					{
						/*
						 * A parameter type that we do not understand.
						 * Do not attempt to call this method.
						 */
						validParamterTypes = false;
					}
				}

				if (validParamterTypes)
				{
					try
					{
						Object methodRetval = method.invoke(null, methodArgs);
						if (methodRetval == null)
							retval = Argument.emptyString;
						else
							retval = new Argument(Argument.STRING, methodRetval.toString());
					}
					catch (IllegalAccessException e)
					{
						throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_JAVA_FUNCTION) +
							": " + e.getMessage());
					}
					catch (InvocationTargetException e)
					{
						Throwable cause = e.getCause();
						StringBuilder message = new StringBuilder();
						message.append(MapyrusMessages.get(MapyrusMessages.FAILED_JAVA_FUNCTION));
						message.append(": ");
						if (cause != null)
							message.append(cause.getMessage());
						else
							message.append(e.getMessage());

						if (cause instanceof InterruptedException)
							throw new InterruptedException(message.toString());
						else
							throw new MapyrusException(message.toString());
					}
				}
			}
		}
		
		if (retval == null)
		{
			throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.FAILED_JAVA_FUNCTION));
		}
		return(retval);
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(m_maxArgs);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(m_minArgs);
	}

	@Override
	public String getName()
	{
		return(m_className + "." + m_methodName);
	}
}
