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
package org.mapyrus;

/**
 * List of all statement types.
 */
public enum StatementType
{
	CONDITIONAL("//conditional//", true),
	REPEAT_LOOP("//repeat_loop//", true),
	WHILE_LOOP("//while_loop//", true),
	FOR_LOOP("//for_loop//", true),
	BLOCK("//block//", true),

	COLOR("color", false),
	COLOUR("colour", false),
	BLEND("blend", false),
	LINESTYLE("linestyle", false),
	FONT("font", false),
	JUSTIFY("justify", false),
	MOVE("move", false),
	DRAW("draw", false),
	RDRAW("rdraw", false),
	ARC("arc", false),
	CIRCLE("circle", false),
	ELLIPSE("ellipse", false),
	CYLINDER("cylinder", false),
	RAINDROP("raindrop", false),
	BEZIER("bezier", false),
	SINEWAVE("sinewave", false),
	WEDGE("wedge", false),
	SPIRAL("spiral", false),
	BOX("box", false),
	ROUNDEDBOX("roundedbox", false),
	BOX3D("box3d", false),
	CHESSBOARD("chessboard", false),
	HEXAGON("hexagon", false),
	PENTAGON("pentagon", false),
	TRIANGLE("triangle", false),
	STAR("star", false),
	ADDPATH("addpath", false),
	CLEARPATH("clearpath", false),
	CLOSEPATH("closepath", false),
	SAMPLEPATH("samplepath", false),
	STRIPEPATH("stripepath", false),
	SHIFTPATH("shiftpath", false),
	PARALLELPATH("parallelpath", false),
	SELECTPATH("selectpath", false),
	REVERSEPATH("reversepath", false),
	SINKHOLE("sinkhole", false),
	GUILLOTINE("guillotine", false),
	STROKE("stroke", false),
	FILL("fill", false),
	GRADIENTFILL("gradientfill", false),
	EVENTSCRIPT("eventscript", false),
	PROTECT("protect", false),
	UNPROTECT("unprotect", false),
	CLIP("clip", false),
	LABEL("label", false),
	FLOWLABEL("flowlabel", false),
	TABLE("table", false),
	TREE("tree", false),
	ICON("icon", false),
	GEOIMAGE("geoimage", false),
	EPS("eps", false),
	SVG("svg", false),
	SVGCODE("svgcode", false),
	PDF("pdf", false),
	PDFGROUP("pdfgroup", false),
	SCALE("scale", false),
	ROTATE("rotate", false),
	WORLDS("worlds", false),
	DATASET("dataset", false),
	FETCH("fetch", false),
	NEWPAGE("newpage", false),
	ENDPAGE("endpage", false),
	SETOUTPUT("setoutput", false),
	PRINT("print", false),
	LOCAL("local", false),
	LET("let", false),
	EVAL("eval", false),
	KEY("key", false),
	LEGEND("legend", false),
	MIMETYPE("mimetype", false),
	HTTPRESPONSE("httpresponse", false),

	/*
	 * Statement type for call and return to/from user defined procedure block.
	 */
	CALL("//call//", true),
	RETURN("return", false);
	
	private String m_name;
	private boolean m_isControl;

	private StatementType(String name, boolean isControl)
	{
		this.m_name = name;
		this.m_isControl = isControl;
	}

	public String getName()
	{
		return m_name;
	}
	
	public boolean isControl()
	{
		return m_isControl;
	}
}
