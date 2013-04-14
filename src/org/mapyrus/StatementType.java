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
	CONDITIONAL,
	REPEAT_LOOP,
	WHILE_LOOP,
	FOR_LOOP,
	BLOCK,

	COLOR,
	BLEND,
	LINESTYLE,
	FONT,
	JUSTIFY,
	MOVE,
	DRAW,
	RDRAW,
	ARC,
	CIRCLE,
	ELLIPSE,
	CYLINDER,
	RAINDROP,
	BEZIER,
	SINEWAVE,
	WEDGE,
	SPIRAL,
	BOX,
	ROUNDEDBOX,
	BOX3D,
	CHESSBOARD,
	HEXAGON,
	PENTAGON,
	TRIANGLE,
	STAR,
	ADDPATH,
	CLEARPATH,
	CLOSEPATH,
	SAMPLEPATH,
	STRIPEPATH,
	SHIFTPATH,
	PARALLELPATH,
	SELECTPATH,
	REVERSEPATH,
	SINKHOLE,
	GUILLOTINE,
	STROKE,
	FILL,
	GRADIENTFILL,
	EVENTSCRIPT,
	PROTECT,
	UNPROTECT,
	CLIP,
	LABEL,
	FLOWLABEL,
	TABLE,
	TREE,
	ICON,
	GEOIMAGE,
	EPS,
	SVG,
	SVGCODE,
	PDF,
	PDFGROUP,
	SCALE,
	ROTATE,
	WORLDS,
	DATASET,
	FETCH,
	NEWPAGE,
	ENDPAGE,
	SETOUTPUT,
	PRINT,
	LOCAL,
	LET,
	EVAL,
	KEY,
	LEGEND,
	MIMETYPE,
	HTTPRESPONSE,

	/*
	 * Statement type for call and return to/from user defined procedure block.
	 */
	CALL,
	RETURN
}
