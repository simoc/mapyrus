Mapyrus Change History
Copyright (C) 2003 - 2021 Simon Chenery <simoc@users.sourceforge.net>

2.106, April 2021

* Update to Java 8 (oldest supported Java version).
* Build with Maven instead of Ant.
* Upload to Maven Central.

1.802, May 2018

* Improve PARALLELPATH calculation for circular arcs, generating smoother line
  segments and avoiding numerical precision problems at start and end of arc.
* Calculate Mapyrus.path.start.angle more accurately for circular arcs.

1.801, April 2018

* Define page size in PDF output more accurately with decimal values.

1.601, August 2016

* Enable inclusion of OpenType fonts with Unicode characters in PDF output.

1.401, November 2014

* Add glyphfile= option to NEWPAGE command for alternate Adode glyph file.
* Add preservenewlines=true option to WORDWRAP function.
* Add includedelimiters=true option to SPLIT function.

1.302, June 2013

* Fix Edit->Export as A4 PDF File Selection for Windows.
* Add JSR 223 javax.script.ScriptEngine interface

1.301, May 2013

* Accept MULTIPOINT ((1 2),(3 4)) format OGC WKT geometry strings too.
* Upgrade to Java version 1.6.
* Upgrade to JTS Topology Suite version 1.13.
* Upgrade to Servlet API 3.0 (Tomcat 7).
* Add LOGSPIRAL command.
* Add Edit->Export as A4 PDF menu option in GUI.
* Add CMYK colors for better color control in PostScript and PDF output.

1.202, July 2012

* Added PDFGROUP command to enable definition of groups/layers in PDF output.

1.201, February 2012

* Allow ESRI Shape files to be accessed using a URL.
* Use better countries data from www.naturalearthdata.com.
* Add world relief image from www.naturalearthdata.com.

1.105, August 2011

* Change syntax for arrays with keys and values to {"AU":"red", "NZ":"blue"}.
* Add GEOJSON function to create a feature in GeoJSON JavaScript format.
* Allow multiple commands on one line, separated by semi-colons.
* Accept OGC Web Mapping Server (WMS) BBOX parameter for world coordinates
  in WORLDS command.
* Allow Mapyrus web server to access files in subdirectories.

1.104, June 2011

* Block invalid octal character codes such as \099.
* Add CHR function to create string from a Unicode character code.
* Fix parsing of font names from PostScript .pfa and .pfb files.
* Enable fonts with ISO-8859-15 (ISO Latin 9), ISO-8859-16 (ISO Latin 10),
  Windows 1250, Windows 1251, Windows 1252, Windows 1253, Windows 1254
  character encodings to be used in PDF output.

1.103, May 2011

* Correct display of characters with ISO-8859-2 (ISO Latin 2) encoding
  in PDF output.

1.102, May 2011

* Display extended characters with ISO-8859-2, CP1250 or other European
font encoding in PostScript and PDF output.

1.101, May 2011

* Accept Unicode characters in format \u20AC
* Allow page size in NEWPAGE to be defined with units such as "256px" or "11in"
* Allow character set to be specified for textfile and shapefile datasets.
* Allow character set to be specified for SPOOL function.

1.007, September 2010

* Fix labelling of multi-line labels containing blank lines
* Add maximumimagememory= option to NEWPAGE command for PDF output
* Use temporary files instead of memory when creating PDF output containing
  large images to avoid running out of memory

1.006, September 2010

* Change FLOWPATH command syntax to allow disabling of 180 degree label rotation
* Added STRINGASCENT, STRINGDESCENT functions so label size can be
  determined more accurately
* Fix calculation of string width for accented characters in PostScript output

1.005, August 2010

* Add HTML page to mapyrus.war demonstrating use of Mapyrus servlet.

1.004, July 2010

* Simplify Mapyrus Servlet so it can be used on internet.
* Make Mapyrus Servlet more secure for running on internet.
* Fix EVAL command when running as a servlet so Mapyrus commands can be
  passed in an HTTP request.

1.003, June 2010

* Add easier initialisation of arrays: [66, 22, 34] or ["AU":"red", "NZ":"blue"]
* Add dataset of US states.

1.002, May 2010

* Avoid SecurityException problems when running GUI with Java Web Start.
* Add REPROJECT function to transform coords. using jhlabs.com PROJ.4 library.

1.001, April 2010

* Add crosshairs in GUI showing world coordinates.
* Update tutorial to demonstrate Mapyrus using GUI.

0.902, February 2010

* Add xmin, ymin, xmax, ymax to allow box when reading world countries
  and capital cities datasets.
* Add GUI with command entry and Mapyrus output for easier experimenting.
* Java function Mapyrus.interpret() now throws InterruptedException too.
* Remove grass and ogrinfo dataset types.

0.901, November 2009

* Allow database connection through JNDI.
* Include map of world countries and capital cities datasets.
* Allow C "/* comment */" and C++ "// comment" style comments.
* Remove grass and ogrinfo dataset types from documentation.  They
  will be removed from software in a future version.

0.807, December 2008

* Add AXIS function to generate suitable values for a graph axis.
* Add CROSSES function from Java Topology Suite.
* Add '++' and '--' for incrementing and decrementing variable values.
* Add ability to run as a servlet.

0.806, September 2008

* Add support for OpenStreetMap format data.
* Enable display of additional geo-referenced image formats using
  external Java classes.

0.805, July 2008

* Add support for Oracle Spatial geometry types (MDSYS.SDO_GEOMETRY).

0.804, March 2008

* Improve PARALLELPATH command to avoid creating some self-intersecting paths.

0.803, March 2008

* Fix clipping in SVG output.
* Upgrade to current Java Topology Suite library version 1.8.

0.802, March 2008

* Add variables Mapyrus.path.start.angle, Mapyrus.path.end.angle giving
  angle of the start and end of the path.
* Add variables Mapyrus.path.start.x, Mapyrus.path.start.y,
  Mapyrus.path.end.x, Mapyrus.path.end.y giving start and end points of path.
* Add REVERSEPATH command to reverse the direction of the path.

0.801, February 2008

* Enable Java functions to be called directly from Mapyrus.

0.704, August 2007

* Simplify PDF output so that it can be read by ImageMagick software.

0.703, June 2007

* Upgrade to Java version 1.5.
* Enable shell environment variables to be used directly in Mapyrus.
* Fix reading of ESRI Shape files to correctly read zero width attribute
  fields, files containing only a single point, and MULTIPOINT geometry
  having only one point.

0.702, March 2007

* Add PDF command for including other PDF files in PDF output.

0.701, January 2007

* Set miter limit so sharp "/\" points in letters are not visible when
  displaying labels with font outlinewidth= option.
* Add minimumlinewidth option to NEWPAGE command.  This prevents very thin
  lines that appear differently in different output formats.

0.614, December 2006

* Add newpage option for fractional font metrics for more accurate
  label positioning.
* Use image smoothing to improve display of icons in image files.

0.613, November 2006

* Add Mapyrus.path variable to return current path as OGC WKT string.
* For PROTECT command, allow any shape to be protected not just rectangles.

0.612, November 2006

* Fix creation of HTML imagemaps to add polygons with holes correctly.

0.611, October 2006

* Compress all data when creating PDF files to reduce file size.
* Extend BLEND command to set color compositing for output to image
  and SVG files.
* Change third parameter to WORDWRAP function for new adjustspacing= option
  that adds extra spaces to align label on both left and right margins.

0.610, September 2006

* Add option to define where words may be split (hyphenation)
  in WORDWRAP function.
* Add option to define justification of entries in a table.

0.609, September 2006

* Add new FUNCTION keyword for user-defined functions.
* Add manual section demonstrating how Mapyrus is used in a Jython application.

0.608, July 2006

* Add scriptfile= option to NEWPAGE command to include JavaScript functions
  in SVG output.
* Add SVGCODE command for embedding extra XML tags in SVG format output.
* Enable XML attributes defining event handling to be added to shapes
  in SVG output for FILL and STROKE commands.

0.607, May 2006

* Add TREE command to draw a tree of labels on the page.
* Fix FLOWLABEL command to avoid Java error when path has only one point.

0.606, April 2006

* Add chessboard command to easily add a grid to the page.
* Enable several colors to be used for table background for coloring
  rows and/or columns differently.
* Add SVG command for including other SVG files in SVG output.

0.605, March 2006

* Add httpresponse command to set complete header for response
  to an HTTP request.
* Add timestamp() function for setting dates in HTTP request responses.
* Add readable() function to check if file is readable.
* Add color names "current", "softer".
* Print an error message if a bad color name is passed to a command.

0.604, February 2006

* When running as HTTP server, fill Mapyrus.http.header array with
  header information passed in each HTTP request.

0.603, February 2006

* Add -r command line option to run more slowly reducing CPU load.
* Demonstrate using -Djava.awt.headless=true option for creating
  PNG image output on "headless" computers with no graphics display.

0.602, January 2006

* Enable inclusion of PostScript Type 1 fonts in PDF output pfbfiles=
  option to newpage command.
* Character encoding for PDF files now controlled using isolatinfonts=
  option to newpage command.
* Add sinewave command to draw sine wave shapes.

0.601, January 2006

* Compress images in PDF output.
* Fix toworlds() function, add sum() function.
* Fix use of Symbols font in PDF output.

0.593, December 2005

* Add more color names.
* Add drawing of 3 dimensional wedge shapes.
* Add toworlds(), topage() functions to transform between
  page and world coordinates.
* Change alignment of multi-line labels so whole label matches value given
  in justify command, not each line.
* Fix Mapyrus.worlds. variables to return correct values when world
  coordinate system added for only part of the page.
* Enable creation of PDF output with no X Windows display set.
* Allow geometry to be passed to protect, unprotect commands
  and protected function.
* Move source code to cvs.sourceforge.net and remove it from released ZIP file.

0.592, November 2005

* Add setting of line spacing in font command for multi-line labels.
* Change stripepath so that stripes in neighbouring polygons match up.

0.591, October 2005

* Fix image and icon display in PDF output.
* Add transparency to PDF output.
* Add blend command to control transparency in PDF output.

0.59, October 2005

* Fix bugs with wordwrap(), stringwidth() functions.
* Add direct creation of PDF output, without using GhostScript (still
  problems with displaying images and icons).

0.58, September 2005

* Add setoutput command to send output of print command to a file.

0.57, August 2005

* Add format() function to format numbers.
* Add creation of HTML imagemaps for images.
* Add eventscript command to allow hyperlinks and Javascript functions to be
  included in HTML imagemaps so that objects in images are
  clickable and interactive.
* Upgrade to Java Topology Suite version 1.5.

0.56, July 2005

* Add endpage command to simplify creation of PDF output when
  running as HTTP server.
* Allow named paper sizes to be used.  For example, "A4" and "Letter".
* Added box3d command to draw boxes with 3 dimensional appearance.
* Add spiral, pentagon, cylinder, raindrop commands to draw new shapes.
* Add parsgeo() function to parse various degree-minutes-seconds formats,
  such as 42.196597N or 42� 11' 47.75"

0.55, June 2005

* Add reading and writing of BMP format images.
* Add reading of GIMP pattern (.pat) image files for use in polygon fills.
* Add table command to draw labels on page in rows and columns.

0.54, April 2005

* Enable display of images fetched from an OGC Web Mapping Server (WMS).
* Add Reading and writing of Netpbm PPM format images for better integration
  with Netpbm programs.
* Improve error handling when running as HTTP server.

0.53, March 2005

* Fix bugs in SVG output in saving state, text alignment.
* Implement gradientfill for SVG output.
* Add compression option for SVG output.
* Enable icons inside a Java JAR file to be displayed using
  resource:au/com/company/filename.png syntax.
* Use pool of database connections to improve performance when running as an
  HTTP server.  Also enables Mapyrus to continue running if database restarted.

0.52, March 2005

* Add display of geo-referenced images with .tfw "worlds" file.
* Add Scalable Vector Graphics (SVG) format output.

0.51, January 2005

* Moved project to new site: http://mapyrus.sourceforge.net.
* Add selectpath command to select out parts of path for display.
* Add bezier command to draw Bezier curves (splines).
* Add eps command to include Encapsulate PostScript files in PostScript output.
* Add difference() function to calculate difference between two geometries.
* Add menus to popup window for "screen" output for cut'n'pasting and
  saving image.

0.50, December 2004

* Change scalebar procedure to take (X, Y) position as parameters.
* Downsample images displayed in PostScript files to reduce file size.
* Fix non-square images to display in correct position in PostScript file.
* Fix display of rotated ellipses.
* Correctly handle binary database table fields returned as WKB geometry
  values by PostGIS "AsBinary(the_geom)".
* Add manual section demonstrating how Mapyrus is used in a PHP application.
* Add repeat keyword for simple loops.
* Add bounding box test to make contains() and overlaps() functions work
  more efficiently.
* Add "screen" output type to show output in a window on the screen.

0.49, November 2004

* Add display of GIS data provided by ogrinfo program (part of OGR library).
* Add newpage "update=true" option to enable drawing to existing output files.
* Change "delimiters=" option to "delimiter=" when opening textfile with
  dataset command, allowing only a single delimiter character.
* Add gradientfill to draw polygons with gradient fill pattern.
* Add ellipse command, wedge command to draw pie slices.
* Add more examples to manual.
* Make display of images more efficient.
* Fix display of very small polygons (about 1/300 inch size) in PostScript file.
* Renamed listfiles function to dir.

0.48, September 2004

* Add eval command, like the eval command found in Perl and UNIX scripting.
* Add manual example demonstrating use of transparent colors.
* Change syntax of worlds command to allow world coordinates
  to be added to part of page.  Now units must be given as "units=meters".
* Add display of character outlines of labels.
* Add -e startup option giving commands to run, instead of reading a file.
* Add reading of standard input as textfile dataset making it possible
  to read output piped from another program.

0.47, September 2004

* Add star command for drawing star shapes.
* Add option to newpage command to set background color for page.
* Fix setting of named, transparent colors.
* Extend manual, describing how to increase available memory.
* Extend manual, describing how to set -classpath option when accessing RDBMS.
* Add newpage options to make anti-aliasing of lines and text optional.

0.46, August 2004

* Add roundedbox, triangle commands for drawing shapes.
* Add parallelpath command for parallel line linestyles.
* Add flowlabel command for labels following streets, rivers, etc.
* Add interpolate function for interpolating sizes, colors, etc. from
  attribute values.
* Add optional display of frequency of each item shown in legend.
* Add manual example demonstrating how to display histograms on map.

0.45, July 2004

* Add listfiles function so that multiple datasets can be displayed more easily.
* Add bookmarks to PDF manual.
* Interpret BLOB and BINARY fields fetched from an RDBMS as OGC WKB geometries.
* Change to use uppercase variable names for parameters passed in a URL
  when running as an HTTP server.
* Remove Java API.
* Require use of new mimetype command to set MIME type for output being
  returned to HTTP client.
* Add return keyword.
* Allow MIME types to be given for output format type in newpage command.
* Add reading of MapInfo Interchange Format (MIF) format files.

0.44, June 2004

* Removed parsewkt function.  Commands and functions that require a geometry
  will now parse geometry strings automatically.
* Add wordwrap, stringheight, rpad, lpad functions for labelling sentences.
* Add Mapyrus.screen.* variables to query screen resolution and size.
* Improve cleanup on error to avoid wasted resources in HTTP server.
* Read GRASS sites list datasets.

0.43, May 2004

* Add rdraw, circle, hexagon commands.
* Add protect, unprotect commands to avoid overlapping labels.
* Allow hexadecimal digits to be used to define an icon.
* Add library of sample shapes and patterns.

0.42, March 2004

* Add Java API and change licensing to LGPL.
* Added stringwidth function.
* Added closepath, guillotine commands.
* Improved tutorial examples.
* Added geometric functions using Vivid Solutions 'Java Topology Suite'.

0.41, February 2004

* Added legends.
* Converted documentation to LaTeX and generate manual in PDF format.
* Added transparent colors.
* Use ISOLatin1Encoding for PostScript fonts to display extended characters.
* Added box, icon commands.
* Changed package name in JAR file to org.mapyrus.
* Changed arguments to newpage command so that resolution is optional.

0.32, December 2003

* Improved HTML documentation.
* Improved PostScript and TrueType font handling.
* Added arrays, for ... in array looping and split function.
* Added mathematical functions ceil, cos floor, log10, max, min, pow,
  sin, sqrt, tan.
* Added access to datasets in an RDBMS through JDBC.
* Added shiftpath command.

0.31, October 2003

* First released version.

