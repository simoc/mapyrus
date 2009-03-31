/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2009 Simon Chenery.
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

import java.util.Hashtable;
import java.awt.Color;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Color names and their RGB color components.
 * A name to RGB value lookup table using colors read from a UNIX-style
 * rgb.txt file.
 * Some basic colors are defined if no file can be found to read.
 */
public class ColorDatabase
{
	static private Hashtable<String, Color> m_colors = null;
	
	/**
	 * Load global color name database from a file.
	 */
	private static synchronized void loadColorNames() throws MapyrusException
	{
		String filename, line;
		StringTokenizer st;
		LineNumberReader reader = null;

		/*
		 * Only load colors once.
		 */
		if (m_colors != null)
			return;

		/*
		 * Define basic set of commonly used colors here (values taken
		 * from color name list in SVG specification).
		 */
		m_colors = new Hashtable<String, Color>();
		m_colors.put("aliceblue", new Color(240, 248, 255));
		m_colors.put("antiquewhite", new Color(250, 235, 215));
		m_colors.put("aqua", new Color(0, 255, 255));
		m_colors.put("aquamarine", new Color(127, 255, 212));
		m_colors.put("azure", new Color(240, 255, 255));
		m_colors.put("beige", new Color(245, 245, 220));
		m_colors.put("bisque", new Color(255, 228, 196));
		m_colors.put("black", new Color(0, 0, 0));
		m_colors.put("blanchedalmond", new Color(255, 235, 205));
		m_colors.put("blue", new Color(0, 0, 255));
		m_colors.put("blueviolet", new Color(138, 43, 226));
		m_colors.put("brown", new Color(165, 42, 42));
		m_colors.put("burlywood", new Color(222, 184, 135));
		m_colors.put("cadetblue", new Color(95, 158, 160));
		m_colors.put("chartreuse", new Color(127, 255, 0));
		m_colors.put("chocolate", new Color(210, 105, 30));
		m_colors.put("coral", new Color(255, 127, 80));
		m_colors.put("cornflowerblue", new Color(100, 149, 237));
		m_colors.put("cornsilk", new Color(255, 248, 220));
		m_colors.put("crimson", new Color(220, 20, 60));
		m_colors.put("cyan", new Color(0, 255, 255));
		m_colors.put("darkblue", new Color(0, 0, 139));
		m_colors.put("darkcyan", new Color(0, 139, 139));
		m_colors.put("darkgoldenrod", new Color(184, 134, 11));
		m_colors.put("darkgray", new Color(169, 169, 169));
		m_colors.put("darkgreen", new Color(0, 100, 0));
		m_colors.put("darkgrey", new Color(169, 169, 169));
		m_colors.put("darkkhaki", new Color(189, 183, 107));
		m_colors.put("darkmagenta", new Color(139, 0, 139));
		m_colors.put("darkolivegreen", new Color(85, 107, 47));
		m_colors.put("darkorange", new Color(255, 140, 0));
		m_colors.put("darkorchid", new Color(153, 50, 204));
		m_colors.put("darkred", new Color(139, 0, 0));
		m_colors.put("darksalmon", new Color(233, 150, 122));
		m_colors.put("darkseagreen", new Color(143, 188, 143));
		m_colors.put("darkslateblue", new Color(72, 61, 139));
		m_colors.put("darkslategray", new Color(47, 79, 79));
		m_colors.put("darkslategrey", new Color(47, 79, 79));
		m_colors.put("darkturquoise", new Color(0, 206, 209));
		m_colors.put("darkviolet", new Color(148, 0, 211));
		m_colors.put("deeppink", new Color(255, 20, 147));
		m_colors.put("deepskyblue", new Color(0, 191, 255));
		m_colors.put("dimgray", new Color(105, 105, 105));
		m_colors.put("dimgrey", new Color(105, 105, 105));
		m_colors.put("dodgerblue", new Color(30, 144, 255));
		m_colors.put("firebrick", new Color(178, 34, 34));
		m_colors.put("floralwhite", new Color(255, 250, 240));
		m_colors.put("forestgreen", new Color(34, 139, 34));
		m_colors.put("fuchsia", new Color(255, 0, 255));
		m_colors.put("gainsboro", new Color(220, 220, 220));
		m_colors.put("ghostwhite", new Color(248, 248, 255));
		m_colors.put("gold", new Color(255, 215, 0));
		m_colors.put("goldenrod", new Color(218, 165, 32));
		m_colors.put("gray", new Color(128, 128, 128));
		m_colors.put("green", new Color(0, 128, 0));
		m_colors.put("greenyellow", new Color(173, 255, 47));
		m_colors.put("grey", new Color(128, 128, 128));
		m_colors.put("honeydew", new Color(240, 255, 240));
		m_colors.put("hotpink", new Color(255, 105, 180));
		m_colors.put("indianred", new Color(205, 92, 92));
		m_colors.put("indigo", new Color(75, 0, 130));
		m_colors.put("ivory", new Color(255, 255, 240));
		m_colors.put("khaki", new Color(240, 230, 140));
		m_colors.put("lavender", new Color(230, 230, 250));
		m_colors.put("lavenderblush", new Color(255, 240, 245));
		m_colors.put("lawngreen", new Color(124, 252, 0));
		m_colors.put("lemonchiffon", new Color(255, 250, 205));
		m_colors.put("lightblue", new Color(173, 216, 230));
		m_colors.put("lightcoral", new Color(240, 128, 128));
		m_colors.put("lightcyan", new Color(224, 255, 255));
		m_colors.put("lightgoldenrodyellow", new Color(250, 250, 210));
		m_colors.put("lightgray", new Color(211, 211, 211));
		m_colors.put("lightgreen", new Color(144, 238, 144));
		m_colors.put("lightgrey", new Color(211, 211, 211));
		m_colors.put("lightpink", new Color(255, 182, 193));
		m_colors.put("lightsalmon", new Color(255, 160, 122));
		m_colors.put("lightseagreen", new Color(32, 178, 170));
		m_colors.put("lightskyblue", new Color(135, 206, 250));
		m_colors.put("lightslategray", new Color(119, 136, 153));
		m_colors.put("lightslategrey", new Color(119, 136, 153));
		m_colors.put("lightsteelblue", new Color(176, 196, 222));
		m_colors.put("lightyellow", new Color(255, 255, 224));
		m_colors.put("lime", new Color(0, 255, 0));
		m_colors.put("limegreen", new Color(50, 205, 50));
		m_colors.put("linen", new Color(250, 240, 230));
		m_colors.put("magenta", new Color(255, 0, 255));
		m_colors.put("maroon", new Color(128, 0, 0));
		m_colors.put("mediumaquamarine", new Color(102, 205, 170));
		m_colors.put("mediumblue", new Color(0, 0, 205));
		m_colors.put("mediumorchid", new Color(186, 85, 211));
		m_colors.put("mediumpurple", new Color(147, 112, 219));
		m_colors.put("mediumseagreen", new Color(60, 179, 113));
		m_colors.put("mediumslateblue", new Color(123, 104, 238));
		m_colors.put("mediumspringgreen", new Color(0, 250, 154));
		m_colors.put("mediumturquoise", new Color(72, 209, 204));
		m_colors.put("mediumvioletred", new Color(199, 21, 133));
		m_colors.put("midnightblue", new Color(25, 25, 112));
		m_colors.put("mintcream", new Color(245, 255, 250));
		m_colors.put("mistyrose", new Color(255, 228, 225));
		m_colors.put("moccasin", new Color(255, 228, 181));
		m_colors.put("navajowhite", new Color(255, 222, 173));
		m_colors.put("navy", new Color(0, 0, 128));
		m_colors.put("navyblue", new Color(0, 0, 128));
		m_colors.put("oldlace", new Color(253, 245, 230));
		m_colors.put("olive", new Color(128, 128, 0));
		m_colors.put("olivedrab", new Color(107, 142, 35));
		m_colors.put("orange", new Color(255, 165, 0));
		m_colors.put("orangered", new Color(255, 69, 0));
		m_colors.put("orchid", new Color(218, 112, 214));
		m_colors.put("palegoldenrod", new Color(238, 232, 170));
		m_colors.put("palegreen", new Color(152, 251, 152));
		m_colors.put("paleturquoise", new Color(175, 238, 238));
		m_colors.put("palevioletred", new Color(219, 112, 147));
		m_colors.put("papayawhip", new Color(255, 239, 213));
		m_colors.put("peachpuff", new Color(255, 218, 185));
		m_colors.put("peru", new Color(205, 133, 63));
		m_colors.put("pink", new Color(255, 192, 203));
		m_colors.put("plum", new Color(221, 160, 221));
		m_colors.put("powderblue", new Color(176, 224, 230));
		m_colors.put("purple", new Color(128, 0, 128));
		m_colors.put("red", new Color(255, 0, 0));
		m_colors.put("rosybrown", new Color(188, 143, 143));
		m_colors.put("royalblue", new Color(65, 105, 225));
		m_colors.put("saddlebrown", new Color(139, 69, 19));
		m_colors.put("salmon", new Color(250, 128, 114));
		m_colors.put("sandybrown", new Color(244, 164, 96));
		m_colors.put("seagreen", new Color(46, 139, 87));
		m_colors.put("seashell", new Color(255, 245, 238));
		m_colors.put("sienna", new Color(160, 82, 45));
		m_colors.put("silver", new Color(192, 192, 192));
		m_colors.put("skyblue", new Color(135, 206, 235));
		m_colors.put("slateblue", new Color(106, 90, 205));
		m_colors.put("slategray", new Color(112, 128, 144));
		m_colors.put("slategrey", new Color(112, 128, 144));
		m_colors.put("snow", new Color(255, 250, 250));
		m_colors.put("springgreen", new Color(0, 255, 127));
		m_colors.put("steelblue", new Color(70, 130, 180));
		m_colors.put("tan", new Color(210, 180, 140));
		m_colors.put("teal", new Color(0, 128, 128));
		m_colors.put("thistle", new Color(216, 191, 216));
		m_colors.put("tomato", new Color(255, 99, 71));
		m_colors.put("turquoise", new Color(64, 224, 208));
		m_colors.put("violet", new Color(238, 130, 238));
		m_colors.put("wheat", new Color(245, 222, 179));
		m_colors.put("white", new Color(255, 255, 255));
		m_colors.put("whitesmoke", new Color(245, 245, 245));
		m_colors.put("yellow", new Color(255, 255, 0));
		m_colors.put("yellowgreen", new Color(154, 205, 50));

		/*
		 * Additional color names that are useful.
		 */
		m_colors.put("amber", new Color(255, 140, 0));
		for (int i = 0; i <= 100; i += 10)
		{
			/*
			 * Add grey0 as black, through to grey100 as white.
			 */
			int greyLevel = (int)Math.round(i / 100.0 * 255.0);
			Color grey = new Color(greyLevel,
				greyLevel, greyLevel);
			m_colors.put("grey" + i, grey);
			m_colors.put("gray" + i, grey);
		}

		m_colors.put("pastelblue", new Color(161, 178, 227));
		m_colors.put("pastelpink", new Color(216, 161, 227));
		m_colors.put("pastelgreen", new Color(161, 227, 186));
		m_colors.put("pastelbrown", new Color(227, 213, 161));
		m_colors.put("seablue", new Color(51, 204, 255));
		m_colors.put("lightorange", new Color(255, 204, 0));

		/*
		 * Additional colors found in web programming guide
		 * list at www.cloford.com:
		 */
		m_colors.put("antiquewhite1", new Color(255, 239, 219));
		m_colors.put("antiquewhite2", new Color(238, 223, 204));
		m_colors.put("antiquewhite3", new Color(205, 192, 176));
		m_colors.put("antiquewhite4", new Color(139, 131, 120));
		m_colors.put("aquamarine1", new Color(127, 255, 212));
		m_colors.put("aquamarine2", new Color(118, 238, 198));
		m_colors.put("aquamarine3", new Color(102, 205, 170));
		m_colors.put("aquamarine4", new Color(69, 139, 116));
		m_colors.put("azure1", new Color(240, 255, 255));
		m_colors.put("azure2", new Color(224, 238, 238));
		m_colors.put("azure3", new Color(193, 205, 205));
		m_colors.put("azure4", new Color(131, 139, 139));
		m_colors.put("banana", new Color(227, 207, 87));
		m_colors.put("bisque1", new Color(255, 228, 196));
		m_colors.put("bisque2", new Color(238, 213, 183));
		m_colors.put("bisque3", new Color(205, 183, 158));
		m_colors.put("bisque4", new Color(139, 125, 107));
		m_colors.put("blue2", new Color(0, 0, 238));
		m_colors.put("blue3", new Color(0, 0, 205));
		m_colors.put("blue4", new Color(0, 0, 139));
		m_colors.put("brown1", new Color(255, 64, 64));
		m_colors.put("brown2", new Color(238, 59, 59));
		m_colors.put("brown3", new Color(205, 51, 51));
		m_colors.put("brown4", new Color(139, 35, 35));
		m_colors.put("burlywood1", new Color(255, 211, 155));
		m_colors.put("burlywood2", new Color(238, 197, 145));
		m_colors.put("burlywood3", new Color(205, 170, 125));
		m_colors.put("burlywood4", new Color(139, 115, 85));
		m_colors.put("burntsienna", new Color(138, 54, 15));
		m_colors.put("burntumber", new Color(138, 51, 36));
		m_colors.put("cadetblue1", new Color(152, 245, 255));
		m_colors.put("cadetblue2", new Color(142, 229, 238));
		m_colors.put("cadetblue3", new Color(122, 197, 205));
		m_colors.put("cadetblue4", new Color(83, 134, 139));
		m_colors.put("cadmiumorange", new Color(255, 97, 3));
		m_colors.put("cadmiumyellow", new Color(255, 153, 18));
		m_colors.put("carrot", new Color(237, 145, 33));
		m_colors.put("chartreuse1", new Color(127, 255, 0));
		m_colors.put("chartreuse2", new Color(118, 238, 0));
		m_colors.put("chartreuse3", new Color(102, 205, 0));
		m_colors.put("chartreuse4", new Color(69, 139, 0));
		m_colors.put("chocolate1", new Color(255, 127, 36));
		m_colors.put("chocolate2", new Color(238, 118, 33));
		m_colors.put("chocolate3", new Color(205, 102, 29));
		m_colors.put("chocolate4", new Color(139, 69, 19));
		m_colors.put("cobaltgreen", new Color(61, 145, 64));
		m_colors.put("cobalt", new Color(61, 89, 171));
		m_colors.put("coldgrey", new Color(128, 138, 135));
		m_colors.put("coral1", new Color(255, 114, 86));
		m_colors.put("coral2", new Color(238, 106, 80));
		m_colors.put("coral3", new Color(205, 91, 69));
		m_colors.put("coral4", new Color(139, 62, 47));
		m_colors.put("cornsilk1", new Color(255, 248, 220));
		m_colors.put("cornsilk2", new Color(238, 232, 205));
		m_colors.put("cornsilk3", new Color(205, 200, 177));
		m_colors.put("cornsilk4", new Color(139, 136, 120));
		m_colors.put("cyan2", new Color(0, 238, 238));
		m_colors.put("cyan3", new Color(0, 205, 205));
		m_colors.put("cyan4", new Color(0, 139, 139));
		m_colors.put("darkgoldenrod1", new Color(255, 185, 15));
		m_colors.put("darkgoldenrod2", new Color(238, 173, 14));
		m_colors.put("darkgoldenrod3", new Color(205, 149, 12));
		m_colors.put("darkgoldenrod4", new Color(139, 101, 8));
		m_colors.put("darkolivegreen1", new Color(202, 255, 112));
		m_colors.put("darkolivegreen2", new Color(188, 238, 104));
		m_colors.put("darkolivegreen3", new Color(162, 205, 90));
		m_colors.put("darkolivegreen4", new Color(110, 139, 61));
		m_colors.put("darkorange1", new Color(255, 127, 0));
		m_colors.put("darkorange2", new Color(238, 118, 0));
		m_colors.put("darkorange3", new Color(205, 102, 0));
		m_colors.put("darkorange4", new Color(139, 69, 0));
		m_colors.put("darkorchid1", new Color(191, 62, 255));
		m_colors.put("darkorchid2", new Color(178, 58, 238));
		m_colors.put("darkorchid3", new Color(154, 50, 205));
		m_colors.put("darkorchid4", new Color(104, 34, 139));
		m_colors.put("darkseagreen1", new Color(193, 255, 193));
		m_colors.put("darkseagreen2", new Color(180, 238, 180));
		m_colors.put("darkseagreen3", new Color(155, 205, 155));
		m_colors.put("darkseagreen4", new Color(105, 139, 105));
		m_colors.put("darkslategray1", new Color(151, 255, 255));
		m_colors.put("darkslategray2", new Color(141, 238, 238));
		m_colors.put("darkslategray3", new Color(121, 205, 205));
		m_colors.put("darkslategray4", new Color(82, 139, 139));
		m_colors.put("deeppink1", new Color(255, 20, 147));
		m_colors.put("deeppink2", new Color(238, 18, 137));
		m_colors.put("deeppink3", new Color(205, 16, 118));
		m_colors.put("deeppink4", new Color(139, 10, 80));
		m_colors.put("deepskyblue1", new Color(0, 191, 255));
		m_colors.put("deepskyblue2", new Color(0, 178, 238));
		m_colors.put("deepskyblue3", new Color(0, 154, 205));
		m_colors.put("deepskyblue4", new Color(0, 104, 139));
		m_colors.put("dodgerblue1", new Color(30, 144, 255));
		m_colors.put("dodgerblue2", new Color(28, 134, 238));
		m_colors.put("dodgerblue3", new Color(24, 116, 205));
		m_colors.put("dodgerblue4", new Color(16, 78, 139));
		m_colors.put("eggshell", new Color(252, 230, 201));
		m_colors.put("emeraldgreen", new Color(0, 201, 87));
		m_colors.put("firebrick1", new Color(255, 48, 48));
		m_colors.put("firebrick2", new Color(238, 44, 44));
		m_colors.put("firebrick3", new Color(205, 38, 38));
		m_colors.put("firebrick4", new Color(139, 26, 26));
		m_colors.put("flesh", new Color(255, 125, 64));
		m_colors.put("gold1", new Color(255, 215, 0));
		m_colors.put("gold2", new Color(238, 201, 0));
		m_colors.put("gold3", new Color(205, 173, 0));
		m_colors.put("gold4", new Color(139, 117, 0));
		m_colors.put("goldenrod1", new Color(255, 193, 37));
		m_colors.put("goldenrod2", new Color(238, 180, 34));
		m_colors.put("goldenrod3", new Color(205, 155, 29));
		m_colors.put("goldenrod4", new Color(139, 105, 20));
		m_colors.put("green1", new Color(0, 255, 0));
		m_colors.put("green2", new Color(0, 238, 0));
		m_colors.put("green3", new Color(0, 205, 0));
		m_colors.put("green4", new Color(0, 139, 0));
		m_colors.put("honeydew1", new Color(240, 255, 240));
		m_colors.put("honeydew2", new Color(224, 238, 224));
		m_colors.put("honeydew3", new Color(193, 205, 193));
		m_colors.put("honeydew4", new Color(131, 139, 131));
		m_colors.put("hotpink1", new Color(255, 110, 180));
		m_colors.put("hotpink2", new Color(238, 106, 167));
		m_colors.put("hotpink3", new Color(205, 96, 144));
		m_colors.put("hotpink4", new Color(139, 58, 98));
		m_colors.put("indianred1", new Color(255, 106, 106));
		m_colors.put("indianred2", new Color(238, 99, 99));
		m_colors.put("indianred3", new Color(205, 85, 85));
		m_colors.put("indianred4", new Color(139, 58, 58));
		m_colors.put("ivory1", new Color(255, 255, 240));
		m_colors.put("ivory2", new Color(238, 238, 224));
		m_colors.put("ivory3", new Color(205, 205, 193));
		m_colors.put("ivory4", new Color(139, 139, 131));
		m_colors.put("ivoryblack", new Color(41, 36, 33));
		m_colors.put("khaki1", new Color(255, 246, 143));
		m_colors.put("khaki2", new Color(238, 230, 133));
		m_colors.put("khaki3", new Color(205, 198, 115));
		m_colors.put("khaki4", new Color(139, 134, 78));
		m_colors.put("lavenderblush1", new Color(255, 240, 245));
		m_colors.put("lavenderblush2", new Color(238, 224, 229));
		m_colors.put("lavenderblush3", new Color(205, 193, 197));
		m_colors.put("lavenderblush4", new Color(139, 131, 134));
		m_colors.put("lemonchiffon1", new Color(255, 250, 205));
		m_colors.put("lemonchiffon2", new Color(238, 233, 191));
		m_colors.put("lemonchiffon3", new Color(205, 201, 165));
		m_colors.put("lemonchiffon4", new Color(139, 137, 112));
		m_colors.put("lightblue1", new Color(191, 239, 255));
		m_colors.put("lightblue2", new Color(178, 223, 238));
		m_colors.put("lightblue3", new Color(154, 192, 205));
		m_colors.put("lightblue4", new Color(104, 131, 139));
		m_colors.put("lightcyan1", new Color(224, 255, 255));
		m_colors.put("lightcyan2", new Color(209, 238, 238));
		m_colors.put("lightcyan3", new Color(180, 205, 205));
		m_colors.put("lightcyan4", new Color(122, 139, 139));
		m_colors.put("lightgoldenrod1", new Color(255, 236, 139));
		m_colors.put("lightgoldenrod2", new Color(238, 220, 130));
		m_colors.put("lightgoldenrod3", new Color(205, 190, 112));
		m_colors.put("lightgoldenrod4", new Color(139, 129, 76));
		m_colors.put("lightpink1", new Color(255, 174, 185));
		m_colors.put("lightpink2", new Color(238, 162, 173));
		m_colors.put("lightpink3", new Color(205, 140, 149));
		m_colors.put("lightpink4", new Color(139, 95, 101));
		m_colors.put("lightsalmon1", new Color(255, 160, 122));
		m_colors.put("lightsalmon2", new Color(238, 149, 114));
		m_colors.put("lightsalmon3", new Color(205, 129, 98));
		m_colors.put("lightsalmon4", new Color(139, 87, 66));
		m_colors.put("lightskyblue1", new Color(176, 226, 255));
		m_colors.put("lightskyblue2", new Color(164, 211, 238));
		m_colors.put("lightskyblue3", new Color(141, 182, 205));
		m_colors.put("lightskyblue4", new Color(96, 123, 139));
		m_colors.put("lightslateblue", new Color(132, 112, 255));
		m_colors.put("lightsteelblue1", new Color(202, 225, 255));
		m_colors.put("lightsteelblue2", new Color(188, 210, 238));
		m_colors.put("lightsteelblue3", new Color(162, 181, 205));
		m_colors.put("lightsteelblue4", new Color(110, 123, 139));
		m_colors.put("lightyellow1", new Color(255, 255, 224));
		m_colors.put("lightyellow2", new Color(238, 238, 209));
		m_colors.put("lightyellow3", new Color(205, 205, 180));
		m_colors.put("lightyellow4", new Color(139, 139, 122));
		m_colors.put("magenta2", new Color(238, 0, 238));
		m_colors.put("magenta3", new Color(205, 0, 205));
		m_colors.put("magenta4", new Color(139, 0, 139));
		m_colors.put("manganeseblue", new Color(3, 168, 158));
		m_colors.put("maroon1", new Color(255, 52, 179));
		m_colors.put("maroon2", new Color(238, 48, 167));
		m_colors.put("maroon3", new Color(205, 41, 144));
		m_colors.put("maroon4", new Color(139, 28, 98));
		m_colors.put("mediumorchid1", new Color(224, 102, 255));
		m_colors.put("mediumorchid2", new Color(209, 95, 238));
		m_colors.put("mediumorchid3", new Color(180, 82, 205));
		m_colors.put("mediumorchid4", new Color(122, 55, 139));
		m_colors.put("mediumpurple1", new Color(171, 130, 255));
		m_colors.put("mediumpurple2", new Color(159, 121, 238));
		m_colors.put("mediumpurple3", new Color(137, 104, 205));
		m_colors.put("mediumpurple4", new Color(93, 71, 139));
		m_colors.put("melon", new Color(227, 168, 105));
		m_colors.put("mistyrose1", new Color(255, 228, 225));
		m_colors.put("mistyrose2", new Color(238, 213, 210));
		m_colors.put("mistyrose3", new Color(205, 183, 181));
		m_colors.put("mistyrose4", new Color(139, 125, 123));
		m_colors.put("navajowhite1", new Color(255, 222, 173));
		m_colors.put("navajowhite2", new Color(238, 207, 161));
		m_colors.put("navajowhite3", new Color(205, 179, 139));
		m_colors.put("navajowhite4", new Color(139, 121, 94));
		m_colors.put("olivedrab1", new Color(192, 255, 62));
		m_colors.put("olivedrab2", new Color(179, 238, 58));
		m_colors.put("olivedrab3", new Color(154, 205, 50));
		m_colors.put("olivedrab4", new Color(105, 139, 34));
		m_colors.put("orange1", new Color(255, 165, 0));
		m_colors.put("orange2", new Color(238, 154, 0));
		m_colors.put("orange3", new Color(205, 133, 0));
		m_colors.put("orange4", new Color(139, 90, 0));
		m_colors.put("orangered1", new Color(255, 69, 0));
		m_colors.put("orangered2", new Color(238, 64, 0));
		m_colors.put("orangered3", new Color(205, 55, 0));
		m_colors.put("orangered4", new Color(139, 37, 0));
		m_colors.put("orchid1", new Color(255, 131, 250));
		m_colors.put("orchid2", new Color(238, 122, 233));
		m_colors.put("orchid3", new Color(205, 105, 201));
		m_colors.put("orchid4", new Color(139, 71, 137));
		m_colors.put("palegreen1", new Color(154, 255, 154));
		m_colors.put("palegreen2", new Color(144, 238, 144));
		m_colors.put("palegreen3", new Color(124, 205, 124));
		m_colors.put("palegreen4", new Color(84, 139, 84));
		m_colors.put("paleturquoise1", new Color(187, 255, 255));
		m_colors.put("paleturquoise2", new Color(174, 238, 238));
		m_colors.put("paleturquoise3", new Color(150, 205, 205));
		m_colors.put("paleturquoise4", new Color(102, 139, 139));
		m_colors.put("palevioletred1", new Color(255, 130, 171));
		m_colors.put("palevioletred2", new Color(238, 121, 159));
		m_colors.put("palevioletred3", new Color(205, 104, 137));
		m_colors.put("palevioletred4", new Color(139, 71, 93));
		m_colors.put("peachpuff1", new Color(255, 218, 185));
		m_colors.put("peachpuff2", new Color(238, 203, 173));
		m_colors.put("peachpuff3", new Color(205, 175, 149));
		m_colors.put("peachpuff4", new Color(139, 119, 101));
		m_colors.put("peacock", new Color(51, 161, 201));
		m_colors.put("pink1", new Color(255, 181, 197));
		m_colors.put("pink2", new Color(238, 169, 184));
		m_colors.put("pink3", new Color(205, 145, 158));
		m_colors.put("pink4", new Color(139, 99, 108));
		m_colors.put("plum1", new Color(255, 187, 255));
		m_colors.put("plum2", new Color(238, 174, 238));
		m_colors.put("plum3", new Color(205, 150, 205));
		m_colors.put("plum4", new Color(139, 102, 139));
		m_colors.put("purple1", new Color(155, 48, 255));
		m_colors.put("purple2", new Color(145, 44, 238));
		m_colors.put("purple3", new Color(125, 38, 205));
		m_colors.put("purple4", new Color(85, 26, 139));
		m_colors.put("raspberry", new Color(135, 38, 87));
		m_colors.put("rawsienna", new Color(199, 97, 20));
		m_colors.put("red1", new Color(255, 0, 0));
		m_colors.put("red2", new Color(238, 0, 0));
		m_colors.put("red3", new Color(205, 0, 0));
		m_colors.put("red4", new Color(139, 0, 0));
		m_colors.put("rosybrown1", new Color(255, 193, 193));
		m_colors.put("rosybrown2", new Color(238, 180, 180));
		m_colors.put("rosybrown3", new Color(205, 155, 155));
		m_colors.put("rosybrown4", new Color(139, 105, 105));
		m_colors.put("royalblue1", new Color(72, 118, 255));
		m_colors.put("royalblue2", new Color(67, 110, 238));
		m_colors.put("royalblue3", new Color(58, 95, 205));
		m_colors.put("royalblue4", new Color(39, 64, 139));
		m_colors.put("salmon1", new Color(255, 140, 105));
		m_colors.put("salmon2", new Color(238, 130, 98));
		m_colors.put("salmon3", new Color(205, 112, 84));
		m_colors.put("salmon4", new Color(139, 76, 57));
		m_colors.put("sapgreen", new Color(48, 128, 20));
		m_colors.put("seagreen1", new Color(84, 255, 159));
		m_colors.put("seagreen2", new Color(78, 238, 148));
		m_colors.put("seagreen3", new Color(67, 205, 128));
		m_colors.put("seagreen4", new Color(46, 139, 87));
		m_colors.put("seashell1", new Color(255, 245, 238));
		m_colors.put("seashell2", new Color(238, 229, 222));
		m_colors.put("seashell3", new Color(205, 197, 191));
		m_colors.put("seashell4", new Color(139, 134, 130));
		m_colors.put("sepia", new Color(94, 38, 18));
		m_colors.put("sgibeet", new Color(142, 56, 142));
		m_colors.put("sgibrightgray", new Color(197, 193, 170));
		m_colors.put("sgichartreuse", new Color(113, 198, 113));
		m_colors.put("sgidarkgray", new Color(85, 85, 85));
		m_colors.put("sgigray12", new Color(30, 30, 30));
		m_colors.put("sgigray16", new Color(40, 40, 40));
		m_colors.put("sgigray32", new Color(81, 81, 81));
		m_colors.put("sgigray36", new Color(91, 91, 91));
		m_colors.put("sgigray52", new Color(132, 132, 132));
		m_colors.put("sgigray56", new Color(142, 142, 142));
		m_colors.put("sgigray72", new Color(183, 183, 183));
		m_colors.put("sgigray76", new Color(193, 193, 193));
		m_colors.put("sgigray92", new Color(234, 234, 234));
		m_colors.put("sgigray96", new Color(244, 244, 244));
		m_colors.put("sgilightblue", new Color(125, 158, 192));
		m_colors.put("sgilightgray", new Color(170, 170, 170));
		m_colors.put("sgiolivedrab", new Color(142, 142, 56));
		m_colors.put("sgisalmon", new Color(198, 113, 113));
		m_colors.put("sgislateblue", new Color(113, 113, 198));
		m_colors.put("sgiteal", new Color(56, 142, 142));
		m_colors.put("sienna1", new Color(255, 130, 71));
		m_colors.put("sienna2", new Color(238, 121, 66));
		m_colors.put("sienna3", new Color(205, 104, 57));
		m_colors.put("sienna4", new Color(139, 71, 38));
		m_colors.put("skyblue1", new Color(135, 206, 255));
		m_colors.put("skyblue2", new Color(126, 192, 238));
		m_colors.put("skyblue3", new Color(108, 166, 205));
		m_colors.put("skyblue4", new Color(74, 112, 139));
		m_colors.put("slateblue1", new Color(131, 111, 255));
		m_colors.put("slateblue2", new Color(122, 103, 238));
		m_colors.put("slateblue3", new Color(105, 89, 205));
		m_colors.put("slateblue4", new Color(71, 60, 139));
		m_colors.put("slategray1", new Color(198, 226, 255));
		m_colors.put("slategray2", new Color(185, 211, 238));
		m_colors.put("slategray3", new Color(159, 182, 205));
		m_colors.put("slategray4", new Color(108, 123, 139));
		m_colors.put("snow1", new Color(255, 250, 250));
		m_colors.put("snow2", new Color(238, 233, 233));
		m_colors.put("snow3", new Color(205, 201, 201));
		m_colors.put("snow4", new Color(139, 137, 137));
		m_colors.put("springgreen1", new Color(0, 238, 118));
		m_colors.put("springgreen2", new Color(0, 205, 102));
		m_colors.put("springgreen3", new Color(0, 139, 69));
		m_colors.put("steelblue1", new Color(99, 184, 255));
		m_colors.put("steelblue2", new Color(92, 172, 238));
		m_colors.put("steelblue3", new Color(79, 148, 205));
		m_colors.put("steelblue4", new Color(54, 100, 139));
		m_colors.put("tan1", new Color(255, 165, 79));
		m_colors.put("tan2", new Color(238, 154, 73));
		m_colors.put("tan3", new Color(205, 133, 63));
		m_colors.put("tan4", new Color(139, 90, 43));
		m_colors.put("thistle1", new Color(255, 225, 255));
		m_colors.put("thistle2", new Color(238, 210, 238));
		m_colors.put("thistle3", new Color(205, 181, 205));
		m_colors.put("thistle4", new Color(139, 123, 139));
		m_colors.put("tomato1", new Color(255, 99, 71));
		m_colors.put("tomato2", new Color(238, 92, 66));
		m_colors.put("tomato3", new Color(205, 79, 57));
		m_colors.put("tomato4", new Color(139, 54, 38));
		m_colors.put("turquoise1", new Color(0, 245, 255));
		m_colors.put("turquoise2", new Color(0, 229, 238));
		m_colors.put("turquoise3", new Color(0, 197, 205));
		m_colors.put("turquoise4", new Color(0, 134, 139));
		m_colors.put("turquoiseblue", new Color(0, 199, 140));
		m_colors.put("violetred1", new Color(255, 62, 150));
		m_colors.put("violetred2", new Color(238, 58, 140));
		m_colors.put("violetred3", new Color(205, 50, 120));
		m_colors.put("violetred4", new Color(139, 34, 82));
		m_colors.put("warmgrey", new Color(128, 128, 105));
		m_colors.put("wheat1", new Color(255, 231, 186));
		m_colors.put("wheat2", new Color(238, 216, 174));
		m_colors.put("wheat3", new Color(205, 186, 150));
		m_colors.put("wheat4", new Color(139, 126, 102));
		m_colors.put("yellow1", new Color(255, 255, 0));
		m_colors.put("yellow2", new Color(238, 238, 0));
		m_colors.put("yellow3", new Color(205, 205, 0));
		m_colors.put("yellow4", new Color(139, 139, 0));

		/*
		 * If user gave name of color file as property then use that.
		 */
		try
		{
			filename = System.getProperty(Constants.PROGRAM_NAME + ".rgb.file");
		}
		catch (SecurityException e)
		{
			filename = null;
		}
		
		try
		{
			/*
			 * Look for a rgb.txt file in current directory if no
			 * filename given.
			 */
			if (filename == null)
				filename = "rgb.txt";		
			reader = new LineNumberReader(new FileReader(filename));
		}
		catch (FileNotFoundException e)
		{
			filename = null;
		}
		
		try
		{
			/*
			 * Otherwise try to read X Windows file /usr/lib/X11/rgb.txt.
			 */
			if (filename == null)
			{
				filename = "/usr/lib/X11/rgb.txt";
				if (Constants.getOSName().indexOf("SUNOS") >= 0)
					filename = "/usr/openwin/lib/X11/rgb.txt";
				reader = new LineNumberReader(new FileReader(filename));
			}
		}
		catch (FileNotFoundException e)
		{
			/*
			 * No color file available, just use basic set.
			 */
			return;
		}

		try
		{
			while ((line = reader.readLine()) != null)
			{
				/*
				 * Parse RGB values and color name from each line.
				 */
				st = new StringTokenizer(line);
				if (st.countTokens() >= 4)
				{
					String red = st.nextToken();
					String green = st.nextToken();
					String blue = st.nextToken();
					
					/*
					 * Name may be a single word or multiple words.
					 * Both "green" and "dark green" are accepted.
					 */
					String name = st.nextToken();
					while (st.hasMoreTokens())
					{
						name = name.concat(st.nextToken());
					}
					
					/*
					 * Skip lines that begin with comment character.
					 */
					if (!red.startsWith("!"))
					{
						try
						{
							int r = Integer.parseInt(red);
							int g = Integer.parseInt(green);
							int b = Integer.parseInt(blue);
							m_colors.put(name.toLowerCase(), new Color(r, g, b));
						}
						catch (NumberFormatException e)
						{
							throw new MapyrusException(filename + ":" +
								reader.getLineNumber() + ": " +
								MapyrusMessages.get(MapyrusMessages.INVALID_COLOR));
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new MapyrusException(e.getMessage());
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
			}
		}
	}

	/**
	 * Return color structure from named color.
	 * @param colorName is named color to lookup or hex value.
	 * @param alpha alpha channel value for color.
	 * @param current color.
	 * @return color definition, or null if color not known.
	 */	
	public static Color getColor(String colorName, int alpha, Color currentColor)
		throws MapyrusException
	{
		Color retval;

		if (colorName.startsWith("#") || colorName.startsWith("0x") || colorName.startsWith("0X"))
		{
			int startIndex = (colorName.charAt(0) == '#') ? 1 : 2;

			/*
			 * Parse color from a 6 digit hex value like '#ff0000',
			 * as used in HTML pages.
			 */
			try
			{
				int rgb = Integer.parseInt(colorName.substring(startIndex), 16);
				rgb = (rgb & 0xffffff);
				retval = new Color(rgb | (alpha << 24), true);
			}
			catch (NumberFormatException e)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_COLOR) + ": " + colorName);
			}
		}
		else if (colorName.equals("brighter"))
		{
			int currentAlpha = currentColor.getAlpha();
			retval = currentColor.brighter();
			if (alpha != currentAlpha)
			{
				retval = new Color(retval.getRed(), retval.getGreen(),
					retval.getBlue(), alpha);
			}
		}
		else if (colorName.equals("darker"))
		{
			int currentAlpha = currentColor.getAlpha();
			retval = currentColor.darker();
			if (alpha != currentAlpha)
			{
				retval = new Color(retval.getRed(), retval.getGreen(),
					retval.getBlue(), alpha);
			}
		}
		else if (colorName.equals("softer"))
		{
			/*
			 * Set softer version of current color.
			 */
			int red = 128 + currentColor.getRed() / 2;
			if (red > 255)
				red = 255;
			int green = 128 + currentColor.getGreen() / 2;
			if (green > 255)
				green = 255;
			int blue = 128 + currentColor.getBlue() / 2;
			if (blue > 255)
				blue = 255;
			retval = new Color(red, green, blue, alpha);
		}
		else if (colorName.equals("contrast"))
		{
			/*
			 * Calculate darkness of current color.
			 */
			int darkness = currentColor.getRed() * 3 +
				currentColor.getGreen() * 4 +
				currentColor.getBlue() * 3;

			/*
			 * If color is currently close to black, then contrasting
			 * color is white, otherwise contrasting color is black.
			 */
			if (darkness > (3 + 4 + 3) * 255 / 2)
			{
				if (alpha == 255)
					retval = Color.BLACK;
				else
					retval = new Color(0, 0, 0, alpha);
			}
			else
			{
				if (alpha == 255)
					retval = Color.WHITE;
				else
					retval = new Color(255, 255, 255, alpha);
			}
		}
		else if (colorName.equals("current"))
		{
			retval = currentColor;
		}
		else
		{
			loadColorNames();
			retval = m_colors.get(colorName);
			if (retval == null)
			{
				/*
				 * Convert color name to lower case and
				 * strip whitespace, then look it up again.
				 */
				int nChars = colorName.length();
				char c;
				StringBuffer sb = new StringBuffer(nChars);
				
				for (int i = 0; i < nChars; i++)
				{
					c = colorName.charAt(i);
					if (!Character.isWhitespace(c))
					{
						sb.append(Character.toLowerCase(c));
					}
				}
				retval = m_colors.get(sb.toString());
				if (retval != null)
				{
					/*
					 * Add this variation of color name to database so it can
					 * be looked up directly next time.
					 */
					m_colors.put(colorName, retval);
				}
			}
			if (alpha != 255)
			{
				/*
				 * Add transparency value to color.
				 */
				int rgb = retval.getRGB();
				rgb = (rgb & 0xffffff);
				retval = new Color(rgb | (alpha << 24), true);
			}
		}
		return(retval);
	}

	/**
	 * Convert a color to a hex string.
	 * @param c color to convert.
	 * @return color in form like "#ff0000".
	 */
	public static String toHexString(Color c)
	{
		StringBuffer sb = new StringBuffer("#");
		String r = Integer.toHexString(c.getRed());
		String g = Integer.toHexString(c.getGreen());
		String b = Integer.toHexString(c.getBlue());

		if (r.length() < 2)
			sb.append("0");
		sb.append(r);

		if (g.length() < 2)
			sb.append("0");
		sb.append(g);

		if (b.length() < 2)
			sb.append("0");
		sb.append(b);

		return(sb.toString());
	}
}
