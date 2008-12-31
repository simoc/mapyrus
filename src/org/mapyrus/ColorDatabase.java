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
	static private Hashtable<String, Color> mColors = null;
	
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
		if (mColors != null)
			return;

		/*
		 * Define basic set of commonly used colors here (values taken
		 * from color name list in SVG specification).
		 */
		mColors = new Hashtable<String, Color>();
		mColors.put("aliceblue", new Color(240, 248, 255));
		mColors.put("antiquewhite", new Color(250, 235, 215));
		mColors.put("aqua", new Color(0, 255, 255));
		mColors.put("aquamarine", new Color(127, 255, 212));
		mColors.put("azure", new Color(240, 255, 255));
		mColors.put("beige", new Color(245, 245, 220));
		mColors.put("bisque", new Color(255, 228, 196));
		mColors.put("black", new Color(0, 0, 0));
		mColors.put("blanchedalmond", new Color(255, 235, 205));
		mColors.put("blue", new Color(0, 0, 255));
		mColors.put("blueviolet", new Color(138, 43, 226));
		mColors.put("brown", new Color(165, 42, 42));
		mColors.put("burlywood", new Color(222, 184, 135));
		mColors.put("cadetblue", new Color(95, 158, 160));
		mColors.put("chartreuse", new Color(127, 255, 0));
		mColors.put("chocolate", new Color(210, 105, 30));
		mColors.put("coral", new Color(255, 127, 80));
		mColors.put("cornflowerblue", new Color(100, 149, 237));
		mColors.put("cornsilk", new Color(255, 248, 220));
		mColors.put("crimson", new Color(220, 20, 60));
		mColors.put("cyan", new Color(0, 255, 255));
		mColors.put("darkblue", new Color(0, 0, 139));
		mColors.put("darkcyan", new Color(0, 139, 139));
		mColors.put("darkgoldenrod", new Color(184, 134, 11));
		mColors.put("darkgray", new Color(169, 169, 169));
		mColors.put("darkgreen", new Color(0, 100, 0));
		mColors.put("darkgrey", new Color(169, 169, 169));
		mColors.put("darkkhaki", new Color(189, 183, 107));
		mColors.put("darkmagenta", new Color(139, 0, 139));
		mColors.put("darkolivegreen", new Color(85, 107, 47));
		mColors.put("darkorange", new Color(255, 140, 0));
		mColors.put("darkorchid", new Color(153, 50, 204));
		mColors.put("darkred", new Color(139, 0, 0));
		mColors.put("darksalmon", new Color(233, 150, 122));
		mColors.put("darkseagreen", new Color(143, 188, 143));
		mColors.put("darkslateblue", new Color(72, 61, 139));
		mColors.put("darkslategray", new Color(47, 79, 79));
		mColors.put("darkslategrey", new Color(47, 79, 79));
		mColors.put("darkturquoise", new Color(0, 206, 209));
		mColors.put("darkviolet", new Color(148, 0, 211));
		mColors.put("deeppink", new Color(255, 20, 147));
		mColors.put("deepskyblue", new Color(0, 191, 255));
		mColors.put("dimgray", new Color(105, 105, 105));
		mColors.put("dimgrey", new Color(105, 105, 105));
		mColors.put("dodgerblue", new Color(30, 144, 255));
		mColors.put("firebrick", new Color(178, 34, 34));
		mColors.put("floralwhite", new Color(255, 250, 240));
		mColors.put("forestgreen", new Color(34, 139, 34));
		mColors.put("fuchsia", new Color(255, 0, 255));
		mColors.put("gainsboro", new Color(220, 220, 220));
		mColors.put("ghostwhite", new Color(248, 248, 255));
		mColors.put("gold", new Color(255, 215, 0));
		mColors.put("goldenrod", new Color(218, 165, 32));
		mColors.put("gray", new Color(128, 128, 128));
		mColors.put("green", new Color(0, 128, 0));
		mColors.put("greenyellow", new Color(173, 255, 47));
		mColors.put("grey", new Color(128, 128, 128));
		mColors.put("honeydew", new Color(240, 255, 240));
		mColors.put("hotpink", new Color(255, 105, 180));
		mColors.put("indianred", new Color(205, 92, 92));
		mColors.put("indigo", new Color(75, 0, 130));
		mColors.put("ivory", new Color(255, 255, 240));
		mColors.put("khaki", new Color(240, 230, 140));
		mColors.put("lavender", new Color(230, 230, 250));
		mColors.put("lavenderblush", new Color(255, 240, 245));
		mColors.put("lawngreen", new Color(124, 252, 0));
		mColors.put("lemonchiffon", new Color(255, 250, 205));
		mColors.put("lightblue", new Color(173, 216, 230));
		mColors.put("lightcoral", new Color(240, 128, 128));
		mColors.put("lightcyan", new Color(224, 255, 255));
		mColors.put("lightgoldenrodyellow", new Color(250, 250, 210));
		mColors.put("lightgray", new Color(211, 211, 211));
		mColors.put("lightgreen", new Color(144, 238, 144));
		mColors.put("lightgrey", new Color(211, 211, 211));
		mColors.put("lightpink", new Color(255, 182, 193));
		mColors.put("lightsalmon", new Color(255, 160, 122));
		mColors.put("lightseagreen", new Color(32, 178, 170));
		mColors.put("lightskyblue", new Color(135, 206, 250));
		mColors.put("lightslategray", new Color(119, 136, 153));
		mColors.put("lightslategrey", new Color(119, 136, 153));
		mColors.put("lightsteelblue", new Color(176, 196, 222));
		mColors.put("lightyellow", new Color(255, 255, 224));
		mColors.put("lime", new Color(0, 255, 0));
		mColors.put("limegreen", new Color(50, 205, 50));
		mColors.put("linen", new Color(250, 240, 230));
		mColors.put("magenta", new Color(255, 0, 255));
		mColors.put("maroon", new Color(128, 0, 0));
		mColors.put("mediumaquamarine", new Color(102, 205, 170));
		mColors.put("mediumblue", new Color(0, 0, 205));
		mColors.put("mediumorchid", new Color(186, 85, 211));
		mColors.put("mediumpurple", new Color(147, 112, 219));
		mColors.put("mediumseagreen", new Color(60, 179, 113));
		mColors.put("mediumslateblue", new Color(123, 104, 238));
		mColors.put("mediumspringgreen", new Color(0, 250, 154));
		mColors.put("mediumturquoise", new Color(72, 209, 204));
		mColors.put("mediumvioletred", new Color(199, 21, 133));
		mColors.put("midnightblue", new Color(25, 25, 112));
		mColors.put("mintcream", new Color(245, 255, 250));
		mColors.put("mistyrose", new Color(255, 228, 225));
		mColors.put("moccasin", new Color(255, 228, 181));
		mColors.put("navajowhite", new Color(255, 222, 173));
		mColors.put("navy", new Color(0, 0, 128));
		mColors.put("navyblue", new Color(0, 0, 128));
		mColors.put("oldlace", new Color(253, 245, 230));
		mColors.put("olive", new Color(128, 128, 0));
		mColors.put("olivedrab", new Color(107, 142, 35));
		mColors.put("orange", new Color(255, 165, 0));
		mColors.put("orangered", new Color(255, 69, 0));
		mColors.put("orchid", new Color(218, 112, 214));
		mColors.put("palegoldenrod", new Color(238, 232, 170));
		mColors.put("palegreen", new Color(152, 251, 152));
		mColors.put("paleturquoise", new Color(175, 238, 238));
		mColors.put("palevioletred", new Color(219, 112, 147));
		mColors.put("papayawhip", new Color(255, 239, 213));
		mColors.put("peachpuff", new Color(255, 218, 185));
		mColors.put("peru", new Color(205, 133, 63));
		mColors.put("pink", new Color(255, 192, 203));
		mColors.put("plum", new Color(221, 160, 221));
		mColors.put("powderblue", new Color(176, 224, 230));
		mColors.put("purple", new Color(128, 0, 128));
		mColors.put("red", new Color(255, 0, 0));
		mColors.put("rosybrown", new Color(188, 143, 143));
		mColors.put("royalblue", new Color(65, 105, 225));
		mColors.put("saddlebrown", new Color(139, 69, 19));
		mColors.put("salmon", new Color(250, 128, 114));
		mColors.put("sandybrown", new Color(244, 164, 96));
		mColors.put("seagreen", new Color(46, 139, 87));
		mColors.put("seashell", new Color(255, 245, 238));
		mColors.put("sienna", new Color(160, 82, 45));
		mColors.put("silver", new Color(192, 192, 192));
		mColors.put("skyblue", new Color(135, 206, 235));
		mColors.put("slateblue", new Color(106, 90, 205));
		mColors.put("slategray", new Color(112, 128, 144));
		mColors.put("slategrey", new Color(112, 128, 144));
		mColors.put("snow", new Color(255, 250, 250));
		mColors.put("springgreen", new Color(0, 255, 127));
		mColors.put("steelblue", new Color(70, 130, 180));
		mColors.put("tan", new Color(210, 180, 140));
		mColors.put("teal", new Color(0, 128, 128));
		mColors.put("thistle", new Color(216, 191, 216));
		mColors.put("tomato", new Color(255, 99, 71));
		mColors.put("turquoise", new Color(64, 224, 208));
		mColors.put("violet", new Color(238, 130, 238));
		mColors.put("wheat", new Color(245, 222, 179));
		mColors.put("white", new Color(255, 255, 255));
		mColors.put("whitesmoke", new Color(245, 245, 245));
		mColors.put("yellow", new Color(255, 255, 0));
		mColors.put("yellowgreen", new Color(154, 205, 50));

		/*
		 * Additional color names that are useful.
		 */
		mColors.put("amber", new Color(255, 140, 0));
		for (int i = 0; i <= 100; i += 10)
		{
			/*
			 * Add grey0 as black, through to grey100 as white.
			 */
			int greyLevel = (int)Math.round(i / 100.0 * 255.0);
			Color grey = new Color(greyLevel,
				greyLevel, greyLevel);
			mColors.put("grey" + i, grey);
			mColors.put("gray" + i, grey);
		}

		mColors.put("pastelblue", new Color(161, 178, 227));
		mColors.put("pastelpink", new Color(216, 161, 227));
		mColors.put("pastelgreen", new Color(161, 227, 186));
		mColors.put("pastelbrown", new Color(227, 213, 161));
		mColors.put("seablue", new Color(51, 204, 255));
		mColors.put("lightorange", new Color(255, 204, 0));

		/*
		 * Additional colors found in web programming guide
		 * list at www.cloford.com:
		 */
		mColors.put("antiquewhite1", new Color(255, 239, 219));
		mColors.put("antiquewhite2", new Color(238, 223, 204));
		mColors.put("antiquewhite3", new Color(205, 192, 176));
		mColors.put("antiquewhite4", new Color(139, 131, 120));
		mColors.put("aquamarine1", new Color(127, 255, 212));
		mColors.put("aquamarine2", new Color(118, 238, 198));
		mColors.put("aquamarine3", new Color(102, 205, 170));
		mColors.put("aquamarine4", new Color(69, 139, 116));
		mColors.put("azure1", new Color(240, 255, 255));
		mColors.put("azure2", new Color(224, 238, 238));
		mColors.put("azure3", new Color(193, 205, 205));
		mColors.put("azure4", new Color(131, 139, 139));
		mColors.put("banana", new Color(227, 207, 87));
		mColors.put("bisque1", new Color(255, 228, 196));
		mColors.put("bisque2", new Color(238, 213, 183));
		mColors.put("bisque3", new Color(205, 183, 158));
		mColors.put("bisque4", new Color(139, 125, 107));
		mColors.put("blue2", new Color(0, 0, 238));
		mColors.put("blue3", new Color(0, 0, 205));
		mColors.put("blue4", new Color(0, 0, 139));
		mColors.put("brown1", new Color(255, 64, 64));
		mColors.put("brown2", new Color(238, 59, 59));
		mColors.put("brown3", new Color(205, 51, 51));
		mColors.put("brown4", new Color(139, 35, 35));
		mColors.put("burlywood1", new Color(255, 211, 155));
		mColors.put("burlywood2", new Color(238, 197, 145));
		mColors.put("burlywood3", new Color(205, 170, 125));
		mColors.put("burlywood4", new Color(139, 115, 85));
		mColors.put("burntsienna", new Color(138, 54, 15));
		mColors.put("burntumber", new Color(138, 51, 36));
		mColors.put("cadetblue1", new Color(152, 245, 255));
		mColors.put("cadetblue2", new Color(142, 229, 238));
		mColors.put("cadetblue3", new Color(122, 197, 205));
		mColors.put("cadetblue4", new Color(83, 134, 139));
		mColors.put("cadmiumorange", new Color(255, 97, 3));
		mColors.put("cadmiumyellow", new Color(255, 153, 18));
		mColors.put("carrot", new Color(237, 145, 33));
		mColors.put("chartreuse1", new Color(127, 255, 0));
		mColors.put("chartreuse2", new Color(118, 238, 0));
		mColors.put("chartreuse3", new Color(102, 205, 0));
		mColors.put("chartreuse4", new Color(69, 139, 0));
		mColors.put("chocolate1", new Color(255, 127, 36));
		mColors.put("chocolate2", new Color(238, 118, 33));
		mColors.put("chocolate3", new Color(205, 102, 29));
		mColors.put("chocolate4", new Color(139, 69, 19));
		mColors.put("cobaltgreen", new Color(61, 145, 64));
		mColors.put("cobalt", new Color(61, 89, 171));
		mColors.put("coldgrey", new Color(128, 138, 135));
		mColors.put("coral1", new Color(255, 114, 86));
		mColors.put("coral2", new Color(238, 106, 80));
		mColors.put("coral3", new Color(205, 91, 69));
		mColors.put("coral4", new Color(139, 62, 47));
		mColors.put("cornsilk1", new Color(255, 248, 220));
		mColors.put("cornsilk2", new Color(238, 232, 205));
		mColors.put("cornsilk3", new Color(205, 200, 177));
		mColors.put("cornsilk4", new Color(139, 136, 120));
		mColors.put("cyan2", new Color(0, 238, 238));
		mColors.put("cyan3", new Color(0, 205, 205));
		mColors.put("cyan4", new Color(0, 139, 139));
		mColors.put("darkgoldenrod1", new Color(255, 185, 15));
		mColors.put("darkgoldenrod2", new Color(238, 173, 14));
		mColors.put("darkgoldenrod3", new Color(205, 149, 12));
		mColors.put("darkgoldenrod4", new Color(139, 101, 8));
		mColors.put("darkolivegreen1", new Color(202, 255, 112));
		mColors.put("darkolivegreen2", new Color(188, 238, 104));
		mColors.put("darkolivegreen3", new Color(162, 205, 90));
		mColors.put("darkolivegreen4", new Color(110, 139, 61));
		mColors.put("darkorange1", new Color(255, 127, 0));
		mColors.put("darkorange2", new Color(238, 118, 0));
		mColors.put("darkorange3", new Color(205, 102, 0));
		mColors.put("darkorange4", new Color(139, 69, 0));
		mColors.put("darkorchid1", new Color(191, 62, 255));
		mColors.put("darkorchid2", new Color(178, 58, 238));
		mColors.put("darkorchid3", new Color(154, 50, 205));
		mColors.put("darkorchid4", new Color(104, 34, 139));
		mColors.put("darkseagreen1", new Color(193, 255, 193));
		mColors.put("darkseagreen2", new Color(180, 238, 180));
		mColors.put("darkseagreen3", new Color(155, 205, 155));
		mColors.put("darkseagreen4", new Color(105, 139, 105));
		mColors.put("darkslategray1", new Color(151, 255, 255));
		mColors.put("darkslategray2", new Color(141, 238, 238));
		mColors.put("darkslategray3", new Color(121, 205, 205));
		mColors.put("darkslategray4", new Color(82, 139, 139));
		mColors.put("deeppink1", new Color(255, 20, 147));
		mColors.put("deeppink2", new Color(238, 18, 137));
		mColors.put("deeppink3", new Color(205, 16, 118));
		mColors.put("deeppink4", new Color(139, 10, 80));
		mColors.put("deepskyblue1", new Color(0, 191, 255));
		mColors.put("deepskyblue2", new Color(0, 178, 238));
		mColors.put("deepskyblue3", new Color(0, 154, 205));
		mColors.put("deepskyblue4", new Color(0, 104, 139));
		mColors.put("dodgerblue1", new Color(30, 144, 255));
		mColors.put("dodgerblue2", new Color(28, 134, 238));
		mColors.put("dodgerblue3", new Color(24, 116, 205));
		mColors.put("dodgerblue4", new Color(16, 78, 139));
		mColors.put("eggshell", new Color(252, 230, 201));
		mColors.put("emeraldgreen", new Color(0, 201, 87));
		mColors.put("firebrick1", new Color(255, 48, 48));
		mColors.put("firebrick2", new Color(238, 44, 44));
		mColors.put("firebrick3", new Color(205, 38, 38));
		mColors.put("firebrick4", new Color(139, 26, 26));
		mColors.put("flesh", new Color(255, 125, 64));
		mColors.put("gold1", new Color(255, 215, 0));
		mColors.put("gold2", new Color(238, 201, 0));
		mColors.put("gold3", new Color(205, 173, 0));
		mColors.put("gold4", new Color(139, 117, 0));
		mColors.put("goldenrod1", new Color(255, 193, 37));
		mColors.put("goldenrod2", new Color(238, 180, 34));
		mColors.put("goldenrod3", new Color(205, 155, 29));
		mColors.put("goldenrod4", new Color(139, 105, 20));
		mColors.put("green1", new Color(0, 255, 0));
		mColors.put("green2", new Color(0, 238, 0));
		mColors.put("green3", new Color(0, 205, 0));
		mColors.put("green4", new Color(0, 139, 0));
		mColors.put("honeydew1", new Color(240, 255, 240));
		mColors.put("honeydew2", new Color(224, 238, 224));
		mColors.put("honeydew3", new Color(193, 205, 193));
		mColors.put("honeydew4", new Color(131, 139, 131));
		mColors.put("hotpink1", new Color(255, 110, 180));
		mColors.put("hotpink2", new Color(238, 106, 167));
		mColors.put("hotpink3", new Color(205, 96, 144));
		mColors.put("hotpink4", new Color(139, 58, 98));
		mColors.put("indianred1", new Color(255, 106, 106));
		mColors.put("indianred2", new Color(238, 99, 99));
		mColors.put("indianred3", new Color(205, 85, 85));
		mColors.put("indianred4", new Color(139, 58, 58));
		mColors.put("ivory1", new Color(255, 255, 240));
		mColors.put("ivory2", new Color(238, 238, 224));
		mColors.put("ivory3", new Color(205, 205, 193));
		mColors.put("ivory4", new Color(139, 139, 131));
		mColors.put("ivoryblack", new Color(41, 36, 33));
		mColors.put("khaki1", new Color(255, 246, 143));
		mColors.put("khaki2", new Color(238, 230, 133));
		mColors.put("khaki3", new Color(205, 198, 115));
		mColors.put("khaki4", new Color(139, 134, 78));
		mColors.put("lavenderblush1", new Color(255, 240, 245));
		mColors.put("lavenderblush2", new Color(238, 224, 229));
		mColors.put("lavenderblush3", new Color(205, 193, 197));
		mColors.put("lavenderblush4", new Color(139, 131, 134));
		mColors.put("lemonchiffon1", new Color(255, 250, 205));
		mColors.put("lemonchiffon2", new Color(238, 233, 191));
		mColors.put("lemonchiffon3", new Color(205, 201, 165));
		mColors.put("lemonchiffon4", new Color(139, 137, 112));
		mColors.put("lightblue1", new Color(191, 239, 255));
		mColors.put("lightblue2", new Color(178, 223, 238));
		mColors.put("lightblue3", new Color(154, 192, 205));
		mColors.put("lightblue4", new Color(104, 131, 139));
		mColors.put("lightcyan1", new Color(224, 255, 255));
		mColors.put("lightcyan2", new Color(209, 238, 238));
		mColors.put("lightcyan3", new Color(180, 205, 205));
		mColors.put("lightcyan4", new Color(122, 139, 139));
		mColors.put("lightgoldenrod1", new Color(255, 236, 139));
		mColors.put("lightgoldenrod2", new Color(238, 220, 130));
		mColors.put("lightgoldenrod3", new Color(205, 190, 112));
		mColors.put("lightgoldenrod4", new Color(139, 129, 76));
		mColors.put("lightpink1", new Color(255, 174, 185));
		mColors.put("lightpink2", new Color(238, 162, 173));
		mColors.put("lightpink3", new Color(205, 140, 149));
		mColors.put("lightpink4", new Color(139, 95, 101));
		mColors.put("lightsalmon1", new Color(255, 160, 122));
		mColors.put("lightsalmon2", new Color(238, 149, 114));
		mColors.put("lightsalmon3", new Color(205, 129, 98));
		mColors.put("lightsalmon4", new Color(139, 87, 66));
		mColors.put("lightskyblue1", new Color(176, 226, 255));
		mColors.put("lightskyblue2", new Color(164, 211, 238));
		mColors.put("lightskyblue3", new Color(141, 182, 205));
		mColors.put("lightskyblue4", new Color(96, 123, 139));
		mColors.put("lightslateblue", new Color(132, 112, 255));
		mColors.put("lightsteelblue1", new Color(202, 225, 255));
		mColors.put("lightsteelblue2", new Color(188, 210, 238));
		mColors.put("lightsteelblue3", new Color(162, 181, 205));
		mColors.put("lightsteelblue4", new Color(110, 123, 139));
		mColors.put("lightyellow1", new Color(255, 255, 224));
		mColors.put("lightyellow2", new Color(238, 238, 209));
		mColors.put("lightyellow3", new Color(205, 205, 180));
		mColors.put("lightyellow4", new Color(139, 139, 122));
		mColors.put("magenta2", new Color(238, 0, 238));
		mColors.put("magenta3", new Color(205, 0, 205));
		mColors.put("magenta4", new Color(139, 0, 139));
		mColors.put("manganeseblue", new Color(3, 168, 158));
		mColors.put("maroon1", new Color(255, 52, 179));
		mColors.put("maroon2", new Color(238, 48, 167));
		mColors.put("maroon3", new Color(205, 41, 144));
		mColors.put("maroon4", new Color(139, 28, 98));
		mColors.put("mediumorchid1", new Color(224, 102, 255));
		mColors.put("mediumorchid2", new Color(209, 95, 238));
		mColors.put("mediumorchid3", new Color(180, 82, 205));
		mColors.put("mediumorchid4", new Color(122, 55, 139));
		mColors.put("mediumpurple1", new Color(171, 130, 255));
		mColors.put("mediumpurple2", new Color(159, 121, 238));
		mColors.put("mediumpurple3", new Color(137, 104, 205));
		mColors.put("mediumpurple4", new Color(93, 71, 139));
		mColors.put("melon", new Color(227, 168, 105));
		mColors.put("mistyrose1", new Color(255, 228, 225));
		mColors.put("mistyrose2", new Color(238, 213, 210));
		mColors.put("mistyrose3", new Color(205, 183, 181));
		mColors.put("mistyrose4", new Color(139, 125, 123));
		mColors.put("navajowhite1", new Color(255, 222, 173));
		mColors.put("navajowhite2", new Color(238, 207, 161));
		mColors.put("navajowhite3", new Color(205, 179, 139));
		mColors.put("navajowhite4", new Color(139, 121, 94));
		mColors.put("olivedrab1", new Color(192, 255, 62));
		mColors.put("olivedrab2", new Color(179, 238, 58));
		mColors.put("olivedrab3", new Color(154, 205, 50));
		mColors.put("olivedrab4", new Color(105, 139, 34));
		mColors.put("orange1", new Color(255, 165, 0));
		mColors.put("orange2", new Color(238, 154, 0));
		mColors.put("orange3", new Color(205, 133, 0));
		mColors.put("orange4", new Color(139, 90, 0));
		mColors.put("orangered1", new Color(255, 69, 0));
		mColors.put("orangered2", new Color(238, 64, 0));
		mColors.put("orangered3", new Color(205, 55, 0));
		mColors.put("orangered4", new Color(139, 37, 0));
		mColors.put("orchid1", new Color(255, 131, 250));
		mColors.put("orchid2", new Color(238, 122, 233));
		mColors.put("orchid3", new Color(205, 105, 201));
		mColors.put("orchid4", new Color(139, 71, 137));
		mColors.put("palegreen1", new Color(154, 255, 154));
		mColors.put("palegreen2", new Color(144, 238, 144));
		mColors.put("palegreen3", new Color(124, 205, 124));
		mColors.put("palegreen4", new Color(84, 139, 84));
		mColors.put("paleturquoise1", new Color(187, 255, 255));
		mColors.put("paleturquoise2", new Color(174, 238, 238));
		mColors.put("paleturquoise3", new Color(150, 205, 205));
		mColors.put("paleturquoise4", new Color(102, 139, 139));
		mColors.put("palevioletred1", new Color(255, 130, 171));
		mColors.put("palevioletred2", new Color(238, 121, 159));
		mColors.put("palevioletred3", new Color(205, 104, 137));
		mColors.put("palevioletred4", new Color(139, 71, 93));
		mColors.put("peachpuff1", new Color(255, 218, 185));
		mColors.put("peachpuff2", new Color(238, 203, 173));
		mColors.put("peachpuff3", new Color(205, 175, 149));
		mColors.put("peachpuff4", new Color(139, 119, 101));
		mColors.put("peacock", new Color(51, 161, 201));
		mColors.put("pink1", new Color(255, 181, 197));
		mColors.put("pink2", new Color(238, 169, 184));
		mColors.put("pink3", new Color(205, 145, 158));
		mColors.put("pink4", new Color(139, 99, 108));
		mColors.put("plum1", new Color(255, 187, 255));
		mColors.put("plum2", new Color(238, 174, 238));
		mColors.put("plum3", new Color(205, 150, 205));
		mColors.put("plum4", new Color(139, 102, 139));
		mColors.put("purple1", new Color(155, 48, 255));
		mColors.put("purple2", new Color(145, 44, 238));
		mColors.put("purple3", new Color(125, 38, 205));
		mColors.put("purple4", new Color(85, 26, 139));
		mColors.put("raspberry", new Color(135, 38, 87));
		mColors.put("rawsienna", new Color(199, 97, 20));
		mColors.put("red1", new Color(255, 0, 0));
		mColors.put("red2", new Color(238, 0, 0));
		mColors.put("red3", new Color(205, 0, 0));
		mColors.put("red4", new Color(139, 0, 0));
		mColors.put("rosybrown1", new Color(255, 193, 193));
		mColors.put("rosybrown2", new Color(238, 180, 180));
		mColors.put("rosybrown3", new Color(205, 155, 155));
		mColors.put("rosybrown4", new Color(139, 105, 105));
		mColors.put("royalblue1", new Color(72, 118, 255));
		mColors.put("royalblue2", new Color(67, 110, 238));
		mColors.put("royalblue3", new Color(58, 95, 205));
		mColors.put("royalblue4", new Color(39, 64, 139));
		mColors.put("salmon1", new Color(255, 140, 105));
		mColors.put("salmon2", new Color(238, 130, 98));
		mColors.put("salmon3", new Color(205, 112, 84));
		mColors.put("salmon4", new Color(139, 76, 57));
		mColors.put("sapgreen", new Color(48, 128, 20));
		mColors.put("seagreen1", new Color(84, 255, 159));
		mColors.put("seagreen2", new Color(78, 238, 148));
		mColors.put("seagreen3", new Color(67, 205, 128));
		mColors.put("seagreen4", new Color(46, 139, 87));
		mColors.put("seashell1", new Color(255, 245, 238));
		mColors.put("seashell2", new Color(238, 229, 222));
		mColors.put("seashell3", new Color(205, 197, 191));
		mColors.put("seashell4", new Color(139, 134, 130));
		mColors.put("sepia", new Color(94, 38, 18));
		mColors.put("sgibeet", new Color(142, 56, 142));
		mColors.put("sgibrightgray", new Color(197, 193, 170));
		mColors.put("sgichartreuse", new Color(113, 198, 113));
		mColors.put("sgidarkgray", new Color(85, 85, 85));
		mColors.put("sgigray12", new Color(30, 30, 30));
		mColors.put("sgigray16", new Color(40, 40, 40));
		mColors.put("sgigray32", new Color(81, 81, 81));
		mColors.put("sgigray36", new Color(91, 91, 91));
		mColors.put("sgigray52", new Color(132, 132, 132));
		mColors.put("sgigray56", new Color(142, 142, 142));
		mColors.put("sgigray72", new Color(183, 183, 183));
		mColors.put("sgigray76", new Color(193, 193, 193));
		mColors.put("sgigray92", new Color(234, 234, 234));
		mColors.put("sgigray96", new Color(244, 244, 244));
		mColors.put("sgilightblue", new Color(125, 158, 192));
		mColors.put("sgilightgray", new Color(170, 170, 170));
		mColors.put("sgiolivedrab", new Color(142, 142, 56));
		mColors.put("sgisalmon", new Color(198, 113, 113));
		mColors.put("sgislateblue", new Color(113, 113, 198));
		mColors.put("sgiteal", new Color(56, 142, 142));
		mColors.put("sienna1", new Color(255, 130, 71));
		mColors.put("sienna2", new Color(238, 121, 66));
		mColors.put("sienna3", new Color(205, 104, 57));
		mColors.put("sienna4", new Color(139, 71, 38));
		mColors.put("skyblue1", new Color(135, 206, 255));
		mColors.put("skyblue2", new Color(126, 192, 238));
		mColors.put("skyblue3", new Color(108, 166, 205));
		mColors.put("skyblue4", new Color(74, 112, 139));
		mColors.put("slateblue1", new Color(131, 111, 255));
		mColors.put("slateblue2", new Color(122, 103, 238));
		mColors.put("slateblue3", new Color(105, 89, 205));
		mColors.put("slateblue4", new Color(71, 60, 139));
		mColors.put("slategray1", new Color(198, 226, 255));
		mColors.put("slategray2", new Color(185, 211, 238));
		mColors.put("slategray3", new Color(159, 182, 205));
		mColors.put("slategray4", new Color(108, 123, 139));
		mColors.put("snow1", new Color(255, 250, 250));
		mColors.put("snow2", new Color(238, 233, 233));
		mColors.put("snow3", new Color(205, 201, 201));
		mColors.put("snow4", new Color(139, 137, 137));
		mColors.put("springgreen1", new Color(0, 238, 118));
		mColors.put("springgreen2", new Color(0, 205, 102));
		mColors.put("springgreen3", new Color(0, 139, 69));
		mColors.put("steelblue1", new Color(99, 184, 255));
		mColors.put("steelblue2", new Color(92, 172, 238));
		mColors.put("steelblue3", new Color(79, 148, 205));
		mColors.put("steelblue4", new Color(54, 100, 139));
		mColors.put("tan1", new Color(255, 165, 79));
		mColors.put("tan2", new Color(238, 154, 73));
		mColors.put("tan3", new Color(205, 133, 63));
		mColors.put("tan4", new Color(139, 90, 43));
		mColors.put("thistle1", new Color(255, 225, 255));
		mColors.put("thistle2", new Color(238, 210, 238));
		mColors.put("thistle3", new Color(205, 181, 205));
		mColors.put("thistle4", new Color(139, 123, 139));
		mColors.put("tomato1", new Color(255, 99, 71));
		mColors.put("tomato2", new Color(238, 92, 66));
		mColors.put("tomato3", new Color(205, 79, 57));
		mColors.put("tomato4", new Color(139, 54, 38));
		mColors.put("turquoise1", new Color(0, 245, 255));
		mColors.put("turquoise2", new Color(0, 229, 238));
		mColors.put("turquoise3", new Color(0, 197, 205));
		mColors.put("turquoise4", new Color(0, 134, 139));
		mColors.put("turquoiseblue", new Color(0, 199, 140));
		mColors.put("violetred1", new Color(255, 62, 150));
		mColors.put("violetred2", new Color(238, 58, 140));
		mColors.put("violetred3", new Color(205, 50, 120));
		mColors.put("violetred4", new Color(139, 34, 82));
		mColors.put("warmgrey", new Color(128, 128, 105));
		mColors.put("wheat1", new Color(255, 231, 186));
		mColors.put("wheat2", new Color(238, 216, 174));
		mColors.put("wheat3", new Color(205, 186, 150));
		mColors.put("wheat4", new Color(139, 126, 102));
		mColors.put("yellow1", new Color(255, 255, 0));
		mColors.put("yellow2", new Color(238, 238, 0));
		mColors.put("yellow3", new Color(205, 205, 0));
		mColors.put("yellow4", new Color(139, 139, 0));

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
							mColors.put(name.toLowerCase(), new Color(r, g, b));
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
			retval = mColors.get(colorName);
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
				retval = mColors.get(sb.toString());
				if (retval != null)
				{
					/*
					 * Add this variation of color name to database so it can
					 * be looked up directly next time.
					 */
					mColors.put(colorName, retval);
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
