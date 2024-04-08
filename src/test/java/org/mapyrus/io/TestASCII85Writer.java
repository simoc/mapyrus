/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2024 Simon Chenery.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.mapyrus.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class TestASCII85Writer
{
	@Test
	public void testWriteZeroBytes() throws IOException
	{
		StringWriter writer = new StringWriter();
		try (ASCII85Writer ascii85 = new ASCII85Writer(writer, false))
		{
			for (int i = 0; i < 20; i++)
			{
				ascii85.write(0);
			}
		}
		assertEquals(writer.getBuffer().toString().trim(), "zzzzz");
	}

	@Test
	public void testWriteHello() throws IOException
	{
		StringWriter writer = new StringWriter();
		try (ASCII85Writer ascii85 = new ASCII85Writer(writer, false))
		{
			String s = "hello";
			for (char c : s.toCharArray())
			{
				ascii85.write(c);
			}
		}
		assertEquals(writer.getBuffer().toString().trim(), "BOu!rDZ");
	}
}
