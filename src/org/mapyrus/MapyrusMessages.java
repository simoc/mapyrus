/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 - 2010 Simon Chenery.
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

import java.util.ResourceBundle;

/**
 * Wrapper around a java resource file containing messages for various
 * languages.  Provides single function to get a message for the current
 * locale, given the message key identifier.
 */
public class MapyrusMessages
{
	public static final String ABOUT = "about";
	public static final String ACCEPTING_HTTP = "acceptinghttp";
	public static final String BAD_IMAGE_READER_CLASS = "badimagereaderclass";
	public static final String CANNOT_OPEN_URL = "cannotopenurl";
	public static final String CLASS_NOT_FUNCTION = "classnotfunction";
	public static final String CLOSE_TAB = "closetab";
	public static final String COLOR_NOT_FOUND = "colornotfound";
	public static final String COPY = "copy";
	public static final String DEFINE_TRANSFORM = "definetransform";
	public static final String EDIT = "edit";
	public static final String ERROR_PS = "errorps";
	public static final String ERROR_SVG = "errorsvg";
	public static final String EXIT = "exit";
	public static final String EXPECTED = "expected";
	public static final String EXPORT_AS_PNG = "exportaspng";
	public static final String EXTENDED_PDF = "extendedpdf";
	public static final String FAILED_PDF = "failedpdf";
	public static final String FAILED_JAVA_FUNCTION = "failedjavafunction";
	public static final String FIELD_MISMATCH = "fieldmismatch";
	public static final String FIELD_NOT_FOUND = "fieldnotfound";
	public static final String FIELD_NOT_OGC_TEXT = "fieldnotogctext";
	public static final String FILE = "file";
	public static final String FILE_NOT_FOUND = "filenotfound";
	public static final String GEOMETRY_NOT_NUMERIC = "geometrynotnumeric";
	public static final String HASHMAP_NOT_NUMERIC = "hashmapnotnumeric";
	public static final String HELP = "help";
	public static final String HTTP_HEADER = "httpheader";
	public static final String HTTP_NOT_FOUND = "httpnotfound";
	public static final String HTTP_RETURNED = "httpreturned";
	public static final String HTTP_RETURN = "httpreturn";
	public static final String HTTP_THREADED_SERVER = "httpthreadedserver";
	public static final String HTTP_TIMEOUT = "httptimeout";
	public static final String IDLE = "idle";
	public static final String INIT_HTTP_FAILED = "inithttpfailed";
	public static final String INTERRUPTED = "interrupted";
	public static final String INVALID_ARC = "invalidarc";
	public static final String INVALID_ARRAY = "invalidarray";
	public static final String INVALID_BEZIER = "invalidbezier";
	public static final String INVALID_BLEND = "invalidblend";
	public static final String INVALID_BOX = "invalidbox";
	public static final String INVALID_CIRCLE = "invalidcircle";
	public static final String INVALID_COLOR = "invalidcolor";
	public static final String INVALID_COLOR_TYPE = "invalidcolortype";
	public static final String INVALID_COLUMN = "invalidcolumn";
	public static final String INVALID_CONDITIONAL = "invalidconditional";
	public static final String INVALID_COORDINATE = "invalidcoordinate";
	public static final String INVALID_CYLINDER = "invalidcylinder";
	public static final String INVALID_DASH_PHASE = "invaliddashphase";
	public static final String INVALID_DASH_PATTERN = "invaliddashpattern";
	public static final String INVALID_DATASET = "invaliddataset";
	public static final String INVALID_DATASET_TYPE = "invaliddatasettype";
	public static final String INVALID_DISTANCE = "invaliddistance";
	public static final String INVALID_ELLIPSE = "invalidellipse";
	public static final String INVALID_END_CAP = "invalidendcap";
	public static final String INVALID_EPS = "invalideps";
	public static final String INVALID_EVAL = "invalideval";
	public static final String INVALID_EXPRESSION = "invalidexpression";
	public static final String INVALID_FIELD_NAME = "invalidfieldname";
	public static final String INVALID_FONT = "invalidfont";
	public static final String INVALID_FORMAT = "invalidformat";
	public static final String INVALID_GEOGRAPHIC = "invalidgeographic";
	public static final String INVALID_GEOIMAGE = "invalidgeoimage";
	public static final String INVALID_GEOMETRY = "invalidgeometry";
	public static final String INVALID_GRADIENT_FILL = "invalidgradientfill";
	public static final String INVALID_HASHMAP_KEY = "invalidhashmapkey";
	public static final String INVALID_HEXAGON = "invalidhexagon";
	public static final String INVALID_HEX_ICON = "invalidhexicon";
	public static final String INVALID_HTTP_REQUEST = "invalidhttp";
	public static final String INVALID_ICON = "invalidicon";
	public static final String INVALID_IMPORT = "invalidimport";
	public static final String INVALID_INTERVAL = "invalidinterval";
	public static final String INVALID_KEYWORD = "invalidkeyword";
	public static final String INVALID_JUSTIFY = "invalidjustify";
	public static final String INVALID_LEGEND_ENTRY = "invalidlegendentry";
	public static final String INVALID_LEGEND_TYPE = "invalidlegendtype";
	public static final String INVALID_LINE_JOIN = "invalidlinejoin";
	public static final String INVALID_LINE_WIDTH = "invalidlinewidth";
	public static final String INVALID_LINESTYLE = "invalidlinestyle";
	public static final String INVALID_NUMBER = "invalidnumber";
	public static final String INVALID_OGC_WKB = "invalidogcwkb";
	public static final String INVALID_OGC_WKT = "invalidogcwkt";
	public static final String INVALID_OPTION = "invalidoption";
	public static final String INVALID_PAGE = "invalidpage";
	public static final String INVALID_PAGE_NUMBER = "invalidpagenumber";
	public static final String INVALID_PAGE_RANGE = "invalidpagerange";
	public static final String INVALID_PAGE_SIZE = "invalidpagesize";
	public static final String INVALID_PAGE_RESOLUTION = "invalidpageresolution";
	public static final String INVALID_PATH_OFFSET = "invalidpathoffset";
	public static final String INVALID_PATH_SAMPLE = "invalidpathsample";
	public static final String INVALID_PATH_SHIFT = "invalidpathshift";
	public static final String INVALID_PATH_STRIPE = "invalidpathstripe";
	public static final String INVALID_PDF = "invalidpdf";
	public static final String INVALID_PENTAGON = "invalidpentagon";
	public static final String INVALID_RADIUS = "invalidradius";
	public static final String INVALID_RAINDROP = "invalidraindrop";
	public static final String INVALID_RANGE = "invalidrange";
	public static final String INVALID_REGEX = "invalidregex";
	public static final String INVALID_ROTATION = "invalidrotation";
	public static final String INVALID_SCALING = "invalidscaling";
	public static final String INVALID_SCRIPT = "invalidscript";
	public static final String INVALID_SETOUTPUT = "invalidsetoutput";
	public static final String INVALID_SINEWAVE = "invalidsinewave";
	public static final String INVALID_SIZE = "invalidsize";
	public static final String INVALID_SPACING = "invalidspacing";
	public static final String INVALID_SPIRAL = "invalidspiral";
	public static final String INVALID_STAR = "invalidstar";
	public static final String INVALID_SVG = "invalidsvg";
	public static final String INVALID_TABLE = "invalidtable";
	public static final String INVALID_TREE = "invalidtree";
	public static final String INVALID_TRIANGLE = "invalidtriangle";
	public static final String INVALID_VARIABLE = "invalidvariable";
	public static final String INVALID_WEDGE = "invalidwedge";
	public static final String INVALID_WMS_REQUEST = "invalidwmsrequest";
	public static final String INVALID_WORLDS = "invalidworlds";
	public static final String INVALID_WORLD_UNITS = "invalidworldunits";
	public static final String JOINED_THREAD = "joinedthread";
	public static final String MISSING_FIELD = "missingfield";
	public static final String MISSING_FILENAME = "missingfilename";
	public static final String MISSING_HTTP_POST = "missinghttppost";
	public static final String MISSING_VALUE = "missingvalue";
	public static final String MISSING_XML_ATTRIBUTE = "missingxmlattribute";
	public static final String NESTED_PROC = "nestedproc";
	public static final String NEW_TAB = "newtab";
	public static final String NOT_A_AFM_FILE = "notaafmfile";
	public static final String NOT_ASCENDING = "notascending";
	public static final String NOT_A_PAT_FILE = "notapatfile";
	public static final String NOT_A_PFA_FILE = "notapfafile";
	public static final String NOT_A_PFB_FILE = "notapfbfile";
	public static final String NOT_A_PPM_FILE = "notappmfile";
	public static final String NOT_A_TTF_FILE = "notattffile";
	public static final String NOT_NUMERIC_OPERATION = "notnumericoperation";
	public static final String NOT_PS_FILE = "notpsfile";
	public static final String NOT_SHAPE_FILE = "notshapefile";
	public static final String NOT_STRING_OPERATION = "notstringoperation";
	public static final String NOT_TEXT_FILE = "nottextfile";
	public static final String NO_ARC_START = "noarcstart";
	public static final String NO_BEZIER_START = "nobezierstart";
	public static final String NO_CLIP_SIDE = "noclipside";
	public static final String NO_DATASET = "nodataset";
	public static final String NO_DEFAULT_PRINTER = "nodefaultprinter";
	public static final String NO_EXPRESSION = "noexpression";
	public static final String NO_JDBC_CLASS = "nojdbcclass";
	public static final String NO_LEGEND_SIZE = "nolegendsize";
	public static final String NO_MIME_TYPE = "nomimetype";
	public static final String NO_MOVETO = "nomoveto";
	public static final String NO_OUTPUT = "nooutput";
	public static final String NO_ROWS = "norows";
	public static final String NO_SINE_WAVE_START = "nosinewavestart";
	public static final String NUMERIC_FUNCTION = "numericfunction";
	public static final String NUMERIC_OVERFLOW = "numericoverflow";
	public static final String ONLINE_HELP = "onlinehelp";
	public static final String OPEN_DATASET_ERROR = "opendataseterror";
	public static final String OPEN_FILE = "openfile";
	public static final String OVERWRITE = "overwrite";
	public static final String PARSE_ERROR = "parseerror";
	public static final String PNG_IMAGE_FILES = "pngimagefiles";
	public static final String PROCESS_ERROR = "processerror";
	public static final String READ_ONLY = "readonly";
	public static final String RECURSION = "recursion";
	public static final String ROMAN_CONVERSION = "romanconversion";
	public static final String RUN_COMMANDS = "runcommands";
	public static final String SAVE_CHANGES_IN_TAB = "savechangesintab";
	public static final String SAVE_TAB = "savetab";
	public static final String SERVLET_INIT_PARAM = "servletinitparam";
	public static final String STARTED_THREAD = "startedthread";
	public static final String STOP_COMMANDS = "stopcommands";
	public static final String TO_FILE = "tofile";
	public static final String TOO_MANY_EXPRESSIONS = "toomanyexpressions";
	public static final String TRANSFORM_ERROR = "transformerror";
	public static final String UNDEFINED_PROC = "undefinedproc";
	public static final String UNEXPECTED_EOF = "unexpectedeof";
	public static final String UNEXPECTED_VALUES = "unexpectedvalues";
	public static final String UNKNOWN_FIELD_TYPE = "unknownfieldtype";
	public static final String UNKNOWN_PROJECTION = "unknownprojection";
	public static final String UNMATCHED_BRACKET = "unmatchedbracket";
	public static final String UNSUPPORTED_ORACLE = "unsupportedoracle";
	public static final String UNTITLED = "untitled";
	public static final String URL_RETURNED = "urlreturned";
	public static final String VARIABLE_EXPECTED = "variableexpected";
	public static final String VARIABLE_UNDEFINED = "variableundefined";
	public static final String WRONG_COORDINATE = "wrongcoordinate";
	public static final String WRONG_FUNCTION_VALUES = "wrongfunctionvalues";
	public static final String WRONG_PARAMETERS = "wrongparameters";
	public static final String WRONG_TYPES = "wrongtypes";
	public static final String ZERO_WORLD_RANGE = "zeroworldrange";

	private static ResourceBundle m_messages;

	static
	{
		m_messages = ResourceBundle.getBundle("org.mapyrus.Messages");
	}

	/**
	 * Returns message for current locale from message key.
	 * @param key key of message to fetch.
	 * @return full message.
	 */
	public static String get(String key)
	{
		return(m_messages.getString(key));
	}
}
