# Reference

## Software Requirements

Mapyrus requires:

* Java 2 Runtime Environment, Standard Edition, (J2RE) 8 or higher,
  or Java 2 Software Developers Kit, Standard Edition (J2SDK) 8 or higher.
* The \texttt{\$DISPLAY} environment variable set to an X-Windows display,
  if running on Linux or a UNIX operating system.  If a real X-Windows display
  is not available, use the \texttt{-Djava.awt.headless=true} startup variable.
* The [JTS Topology Suite](http://www.tsusiatsoftware.net/jts/main.html),
  if geometric functions are required
  (see Table \ref{functions} on page \pageref{functions}).
* The [Java PROJ.4 library](http://www.jhlabs.com/java/maps/proj),
  if the \texttt{REPROJECT} function is required for reprojecting coordinates.

## Usage

The Mapyrus software is contained in a single Java JAR file.
Start Mapyrus in a Java interpreter with the following command.

\vspace{10pt}
\texttt{java -classpath _install-dir_/mapyrus.jar org.mapyrus.Mapyrus _filename_ ...}
\vspace{10pt}

_install-path_ is the directory in which
Mapyrus is installed.  _filename_ is the name of a file
or a URL for Mapyrus to read from.  If _filename_ is \texttt{-}
then standard input is read.  If several filenames and URLs are
given then they are read in turn.

Environment variables
and variables passed to Mapyrus using the Java \texttt{-D} option
are available in the Mapyrus interpreter.  The
_JTS Topology Suite_ JAR file and other JAR files
to be used in combination with Mapyrus are included in the
\texttt{-classpath} option.

\texttt{java -D_variable_=_value_ ... -classpath _install-dir_/mapyrus.jar:_jts-dir_/jts-1.13.jar:_other-jarfile_ org.mapyrus.Mapyrus _filename_}

Mapyrus runs as an HTTP Server when started with the
\texttt{-s} option.

\texttt{java -classpath _install-dir_/mapyrus.jar:_jarfile_ org.mapyrus.Mapyrus -s _port_ _filename_ ...}

\label{morememory}
Use the
\texttt{-Xmx} Java option
to make more memory available when running Mapyrus.
To increase available memory to 256Mb, use the following command:

\texttt{java -Xmx256m -classpath _install-dir_/mapyrus.jar org.mapyrus.Mapyrus _filename_ ...}

### Startup Configuration

The variables available for configuring Mapyrus at startup are
shown in the table below.

Variable | Description
-------- | -----------
\texttt{Mapyrus.rgb.file=_filename_} | Defines an X Windows color names file containing additional color names for the \texttt{color} command.  Default value is \texttt{/usr/lib/X11/rgb.txt}
\texttt{java.awt.headless=true} | Run in headless mode.  Required when running on a server with no graphics display.
\texttt{java.io.tmpdir=_dir_} | Defines directory to use for temporary files.  Large images in PDF output are temporarily saved in this directory until output is complete.
\texttt{jdbc.drivers=_class_} | Defines class containing JDBC 1.0 (or higher) driver to load at startup.  A JDBC driver is required for connecting to a relational database and is provided as part of a relational database.  See the Java JDBC [DriverManager API documentation](https://docs.oracle.com/javase/8/docs/api/java/sql/DriverManager.html) for details.  The JAR file containing the class must be included in the \texttt{-classpath} option when starting Mapyrus.

## Language

Mapyrus interprets commands read from one or more plain text files.
Each command begins on a separate line or after a semi-colon (\texttt{;}).

Any part of a line following a hash (\texttt{\#}) or
a pair of slashes (\texttt{//}) that is not part of
a literal string is interpreted as a comment and is ignored.
C programming \texttt{/*} ... \texttt{*/} style
comments are also ignored.
Leading and
trailing spaces or tabs on a line are ignored too.  A backslash
(`\\`) character at the end of a line is interpreted as a
line continuation and the line and next line are joined into a single line.

A line beginning with the word \texttt{include}, followed by a filename
or URL includes commands from another file.

Each command is passed zero or more arguments separated by commas.
An argument is a number, a string literal in single quotes (\texttt{\'})
or double quotes (\texttt{\"}), a variable name, an array, an
array element or an expression.

In a string literal,
the character sequence \\_nnn_
is interpreted as an octal character code (where
\texttt{_nnn_} is one to three digits) and the character
sequence \\u_nnnn_ is interpreted
as a Unicode character (where _nnnn_ is four hexadecimal
digits).

An expression contains arguments and operators and functions
on those arguments, like in BASIC, C, or Python.
Available operators are shown in Table \ref{operators}.
Pre-defined functions are shown in Table \ref{functions}.
Java methods are also available as functions by giving the class name
and method name separated by a dot.  Only Java methods
declared as \texttt{public} and \texttt{static} are available
as functions.

Operator | Description
-------- | -----------
\texttt{(}, \texttt{)} | parentheses
\texttt{++}, \texttt{--} | increments or decrements variable
\texttt{*}, \texttt{/}, \texttt{\%}, \texttt{x} | numeric multiplication, numeric division, modulo (works with non-integer values too), string repetition
\texttt{+}, \texttt{-}, \texttt{.} | numeric addition, numeric subtraction, string concatenation
\texttt{<=}, \texttt{<}, \texttt{==}, \texttt{!=}, \texttt{>}, \texttt{>=}, \texttt{lt}, \texttt{le}, \texttt{eq}, \texttt{ne}, \texttt{gt}, \texttt{ge} | numeric comparisons and string comparisons
\texttt{?:} | ternary conditional operator
\texttt{and}, \texttt{or}, \texttt{not} | Logical and, or, not

Function Name | Description
------------- | -----------
\texttt{abs(_n_)} | Returns the absolute value of _n_.
\texttt{axis(_min_, _max_, _intervals_)} | Generates a set of numbers that are suitable for an axis of a graph containing values in the range _min_ to _max_.  _intervals_ sets the maximum number of values for the axis.  An array is returned with each value for the axis.
\texttt{buffer(_g_, _dist_, _cap_)} | Returns a geometry containing a buffer calculated at a distance _dist_ around the perimeter of geometry _g_.  The value of _cap_ defines the method of closing buffers at line endpoints, either \texttt{butt}, \texttt{round} or \texttt{square}.  This function requires the _JTS Topology Suite_.
\texttt{ceil(_n_)} | Returns the smallest integer value that is not less than _n_.
\texttt{chr(_n_)} | Returns a string containing the single Unicode character code _n_.
\texttt{contains(_g1_, _x_, _y_)} \texttt{contains(_g1_, _g2_)} | If point (_x_, _y_) or geometry _g2_ is contained inside _g1_ then 1 is returned.  Otherwise 0 is returned.  Geometries may be the same type or different types.  This function requires the _JTS Topology Suite_.
\texttt{convexhull(_g_)} | Returns a convex hull geometry that surrounds geometry _g_.
\texttt{cos(_n_)} | Returns the cosine of angle _n_, given in degrees.
\texttt{crosses(_g1_, _g2_)} | If geometry _g2_ crosses _g1_ then 1 is returned.  Otherwise 0 is returned.  Geometries must be of different types.  To compare geometries of the same type, use \texttt{overlaps}.  This function requires the _JTS  Topology Suite_.
\texttt{difference(_g1_, _g2_)} | Returns a geometry containing the difference between geometry _g1_ and geometry _g2_.  That is, parts of geometry _g1_ that are not part of geometry _g2_.  This function requires the _JTS Topology Suite_.
\texttt{dir(_p_)} | Returns an array of all filenames matching the wildcard
pattern _p_ containing asterisk (\texttt{*}) characters.
\texttt{floor(_n_)} | Returns the largest integer value that is not larger than _n_.
\texttt{format(_str_, _n_)} | Returns the number _n_ formatted using format string _str_.  Format string is given using hash characters and zeroes for digits and an optional decimal point.  For example, \texttt{00000} for a five digit number with leading zeroes, or \texttt{\#\#.\#\#\#} for a number rounded to three decimal places.
\texttt{geojson(_g_)} \texttt{geojson(_g_, _p_)} \texttt{geojson(_g_, _p_, _id_)} | Returns a feature containing geometry _g_ in GeoJSON format.  Additional key and value properties and a feature identifier are included, if array _p_ and identifier _id_ are given.
\texttt{interpolate(_str_, _n_)} | Returns value calculated from _n_ using linear interpolation.  _str_ contains list of numbers (given in increasing numeric order) and corresponding values: $n_{1}$ $v_{1}$ $n_{2}$ $v_{2}$ ...  Result is found by finding range $n_{i}$ to $n_{i+1}$ containing \texttt{n} and using linear interpolation to calculate a value between $v_{i}$ and $v_{i+1}$.  Each value $v_{i}$ is either a number, named color, hex digits color or CMYK color in parentheses.
\texttt{intersection(_g1_, _g2_)} | Returns a geometry containing the intersection of geometry _g1_ and geometry _g2_.  This function requires the _JTS  Topology Suite_.
\texttt{length(_v_)} | If _v_ is an array, then the number of elements in the array is returned.  Otherwise the string length of _v_ is returned.
\texttt{log10(_n_)} | Returns the base 10 logarithm of _n_.
\texttt{lower(_str_)} | Returns _str_ converted to lower case.
\texttt{lpad(_str_, _len_, _pad_)} \texttt{lpad(_str_, _len_)} | Returns string _str_ left padded to length _len_ using characters from string _pad_.  Spaces are used for padding if _pad_ is not given.  String is truncated on the left if longer than length _len_.
\texttt{match(_str_, _regex_)} | Returns the index in the string _str_, where the regular expression _regex_ is first matched.  The index of the first character is 1.  If the regular expression does not match _str_, then 0 is returned.  The [Java API documentation](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) describes the syntax of regular expressions.
\texttt{max(_a_, _b_)} | Returns the larger of values _a_ and _b_.
\texttt{min(_a_, _b_)} | Returns the smaller of values _a_ and _b_.
\texttt{overlaps(_g1_, _g2_)} | If geometry _g1_ and geometry _g2_ are the same type and overlap then 1 is returned.  Otherwise 0 is returned.  This function requires the _JTS Topology Suite_.
\texttt{parsegeo(_n_)} | Parses string _n_ containing a latitude or longitude position into a decimal value and returns it.  Strings of many forms are accepted, including \texttt{42.196597N}, \texttt{42\textdegree{} 11\'{} 47.75\"}, \texttt{42d 11m 47.75s}, \texttt{N 42d 11m 47.75s} and \texttt{42deg 11min 47.75sec}.
\texttt{pow(_a_, _b_)} | Returns _a_ to the power _b_.
\texttt{protected(_x1_, _y1_, _x2_, _y2_)} \texttt{protected(_g_)} \texttt{protected()} | For points (_x1_, _y1_) and (_x2_, _y2_) defining any two opposite corners of a rectangle, returns 1 if any part of this rectangle has been protected using the \texttt{protect} command.  For polygon geometry _g_, returns 1 if any part of the polygon has been protected.  When no rectangle or geometry is given then 1 is returned if any part of the current path has been protected.  Otherwise 0 is returned.
\texttt{random(_n_)} | Generates a random floating point number between 0 and _n_.
\texttt{readable(_filename_)} | Returns 1 if the file _filename_ exists and is readable.  Otherwise 0 is returned.
\texttt{replace(_str_, _regex_, _replacement_)} | Returns the string _str_, with all occurrences of the regular expression _regex_ replaced by _replacement_.
\texttt{reproject(_p1_, _p2_, _g_)} \texttt{reproject(_p1_, _p2_, _x_, _y_)} | Reprojects the geometry _g_ or point (_x_, _y_) from projection _p1_ to projection _p2_.  Projections are names in the PROJ.4 database or a list of PROJ.4 projection parameters separated by whitespace.  This function requires the _Java PROJ.4 library_.
\texttt{roman(_n_)} | Returns the number _n_ converted to a Roman numerals string.
\texttt{round(_n_)} | Returns _n_ rounded to nearest whole number.
\texttt{rpad(_str_, _len_, _pad_)} \texttt{rpad(_str_, _len_)} | Returns string _str_ right padded to length _len_ using characters from string _pad_.  Spaces are used for padding if _pad_ is not given.  String is truncated if longer than length _len_.
\texttt{sin(_n_)} | Returns the sine of angle _n_, given in degrees.
\texttt{split(_str_, _regex_)} \texttt{split(_str_, _regex_, _extras_)} \texttt{split(_str_)} | Splits the string _str_ into an array of strings, delimited by the regular expression _regex_ or whitespace if no regular expression given.  The array of split strings is returned, with the first string having array index 1, the second string having index 2, and so on.  If _extras_ contains \texttt{includedelimiters=true} then delimiters are included in the array too.
\texttt{spool(_filename_)} \texttt{spool(_filename_, _extras_)} | Returns string containing contents of text file _filename_.  If _filename_ has suffix \texttt{.gz} or \texttt{.zip} then it is automatically decompressed as it is read.  If _extras_ contains \texttt{encoding=}_charset_ then characters in file are interpreted with that character set encoding.
\texttt{sqrt(_n_)} | Returns square root of _n_.
\texttt{stringascent(_str_)} | Returns the ascent of the string _str_ if it were displayed using the \texttt{label} command.  The ascent is the distance above the label position of the tallest character in the string.  For strings containing several lines, the total ascent of all lines is returned.  The ascent is returned in world coordinate units if set with a \texttt{worlds} command, otherwise in page coordinates.
\texttt{stringdescent(_str_)} | Returns the descent of the string _str_ if it were displayed using the \texttt{label} command.  The descent is the distance below the label position used by characters such as _g_ and _y_ and is returned as a negative number.  For strings containing several lines, the descent of the first line is returned.  The descent is returned in world coordinate units if set with a \texttt{worlds} command, otherwise in page coordinates.
\texttt{stringheight(_str_)} | Returns the height of the string _str_ if it were displayed using the \texttt{label} command.  For strings containing several lines, the total height of all lines is returned.  The height is returned in world coordinate units if set with a \texttt{worlds} command, otherwise in page coordinates.
\texttt{stringwidth(_str_)} | Returns the width of the string _str_ if it were displayed using the \texttt{label} command.  For strings containing several lines, the width of the longest line is returned.  The width is returned in world coordinate units if set with a \texttt{worlds} command, otherwise in page coordinates.
\texttt{sum(_a_)} | Returns the sum of values in array _a_.
\texttt{substr(_str_, _offset_, _n_)} \texttt{substr(_str_, _offset_)} | Returns a substring of the string _str_, beginning at the character with index _offset_ that is _n_ characters long, or all characters from index _offset_ if _n_ is not given.  The first character in _str_ has an index of 1.
\texttt{tan(_n_)} | Returns the trigonometric tangent of angle _n_, given in degrees.
\texttt{tempname(_suffix_)} | Returns a unique temporary filename with given file suffix, for use when running as an HTTP server.  Temporary files returned by this function are automatically deleted after 5 minutes.
\texttt{timestamp(_n_)} | Returns a time stamp containing the current GMT date and time plus _n_ seconds, for use in setting expiry dates when running as an HTTP server.
\texttt{topage(_x_, _y_)} \texttt{topage(_g_)} | Transforms the point (_x_, _y_) or geometry _g_ from the current world coordinate system to page coordinates.
\texttt{toworlds(_x_, _y_)} \texttt{toworlds(_g_)} | Transforms the point (_x_, _y_) or geometry _g_ from page coordinates to the current world coordinate system.  This is the inverse of the \texttt{topage} function.
\texttt{trim(_str_)} | Returns string _str_ with whitespace trimmed from start and end.
\texttt{union(_g1_, _g2_)} | Returns a geometry containing the union of of geometry _g1_ and geometry _g2_.  This function requires the _JTS Topology Suite_.
\texttt{upper(_str_)} | Returns _str_ converted to upper case.
\texttt{wordwrap(_str_, _width_)} \texttt{wordwrap(_str_, _width_, _extras_)} | Returns _str_ broken into several lines for use in a \texttt{label} command.  Each line will not be longer than _width_ millimeters wide.  If _extras_ contains \texttt{hyphenation=}_str_ then words containing the hyphenation string may also be split onto two lines at that point using a hyphen.  If _extras_ contains \texttt{adjustspacing=true} then additional spaces are added between words so that each line has the required width.  If _extras_ contains \texttt{preservenewlines=true} then newlines in _str_ are preserved.

An argument or expression is assigned to a named variable using the
\texttt{let} command and an equals sign (\texttt{=}).  A variable name begins
with a letter or dollar sign (\texttt{\$}) and contains only letters, numbers,
dots (\texttt{.}), underbars (\texttt{\_}) and colons (\texttt{:}).
Variable names are
case-sensitive.  Variables and array elements that are accessed without having
been defined have a value of zero, or an empty string.

Variables accepted as parameters to a function or procedure, or declared in the
function or procedure with the local command are local to that function or
procedure and not visible outside the function or procedure.  All other
variables are global.

Individual array elements are accessed by giving the index between
square brackets (\texttt{[} and \texttt{]}).
The index is any string or number value.
Multi-dimension arrays are available by using
indexes named _index1_\texttt{."}_c_\texttt{".}_index2_,
where _c_ is any character that never appears in an index.

An array with sequential indexes starting at 1 is defined as a single
argument by surrounding the values by square brackets
\texttt{[_value1_, _value2_, ... ]}.
To define both keys and values for an array use
\texttt{\{"_key1_": _value1_, "_key2_": _value2_, ... \}}.

The \texttt{if}, \texttt{repeat}, \texttt{while}, \texttt{for}
and \texttt{function} flow control structures
found in other languages are available:

```
if _condition_ then
  _then-commands_ ...
else
  _else-commands_ ...
endif
```

```
if _condition_ then _command_; _command_; else _command_; _command_; endif
```

Executes _then-commands_ if _condition_ evaluates to
a non-zero value, otherwise _else-commands_ are executed.
The \texttt{else} part is optional.
Compound tests are built using the \texttt{elif} keyword:

```
if _condition_ then
  _commands_ ...
elif _condition_ then
  _commands_ ...
endif
```

The \texttt{repeat} keyword defines a loop in which
_commands_ will be executed _count_
times:

```
repeat _count_ do
  _commands_ ...
done
```

```
repeat _count_ do _command_; _command_; done
```

The \texttt{while} keyword defines a loop in which
_commands_ will be executed for as long
as _condition_ continues to evaluate to a non-zero value:

```
while _condition_ do
  _commands_ ...
done
```

```
while _condition_ do _command_; _command_; done
```

The \texttt{for} ... \texttt{in} keywords define a loop in which
each element of _array_ is assigned to variable _var_
and _commands_ are executed.
Elements in _array_ are accessed in numerical index order if
indexes are numeric, otherwise in alphabetical index order:

```
for _var_ in _array_ do
  _commands_ ...
done
```

```
for _var_ in _array_ do _command_; _command_; done
```

Functions are used to repeat commonly used calculations and to
return a value:

```
function _name_ [_arg1_, ...]
  _commands_ ...
  \texttt{return} _arg_
end
```

```
function _name_ [_arg1_, ...]; _command_; _command_; \texttt{return} _arg_; end
```

Procedures group frequently used commands together, save
graphics state when they begin and restore it when they end,
isolating the calling procedure from any changes:

```
begin _name_ [_arg1_, ...]
  _commands_ ...
end
```

```
begin _name_ [_arg1_, ...]; _command_; _command_; end
```

A procedure is defined to take a fixed number of arguments.
All procedure names are global and following the same naming
rules as variables.
Procedure definitions within a procedure are not allowed.
The \texttt{return} keyword returns a
from a procedure to the calling procedure.

A procedure is called from any place where a command is accepted
and the number of arguments passed must match the number 
in the procedure definition.

Before commands in the procedure are executed
the graphics state is saved.
\label{graphicsstate}
The graphics state is restored when the procedure finishes.
The graphics state contains:

\begin{enumerate}
\item
The path defined with
\texttt{move},
\texttt{draw},
\texttt{box},
\texttt{arc},
\texttt{bezier},
\texttt{circle},
\texttt{cylinder},
\texttt{ellipse},
\texttt{hexagon},
\texttt{pentagon},
\texttt{sinewave},
\texttt{spiral},
\texttt{star},
\texttt{triangle},
\texttt{wedge}
and
\texttt{addpath}
commands.

\item
The clip path defined with
\texttt{clip} commands.

\item
Current drawing settings set by
\texttt{color},
\texttt{linestyle},
\texttt{font} and \texttt{justify}
commands.

\item
Transformations set by \texttt{rotate} and \texttt{scale} commands.

\item
The output file set by a \texttt{setoutput} command.
\end{enumerate}

Any new page created in a procedure with a
\texttt{newpage}
command is completed when the procedure finishes and output
returns to the page being created before the procedure was called.

Any dataset being read is global and protected regions are global.
They are not saved and not restored.

Any world coordinate system set with the \texttt{worlds} command is cleared
before the commands in the procedure are executed.  This enables the calling
procedure to work in a world coordinate system and the called procedure to draw
at these world coordinate positions using measurements in millimeters.

If the current path contains only \texttt{move} points and no straight line or
arc segments when a procedure is called then the procedure is called
repeatedly, one time for each move point, with the origin (0, 0) translated to
the \texttt{move} point each time and the path reset to empty.  Therefore,
coordinates in the called procedure are relative to the move point.  This
enables drawing commands in the called procedure to be given in millimeters,
relative to each \texttt{move} point.

## Internal Variables

All environment variables and
Java standard system properties (\texttt{os.arch}, \texttt{user.dir}, etc.)
are defined as variables in Mapyrus.

Mapyrus maintains the internal variables shown in the table below.

Variable Name | Description
------------- | -----------
\texttt{Mapyrus.dataset.fieldnames} | An array containing the names of fields being read from the current dataset.  The first fieldname has array index 1.
\texttt{Mapyrus.dataset.projection} | A description of the projection (coordinate system) in which coordinates of the current dataset are stored.  Projection descriptions are not standardised between dataset formats.  Different dataset formats will return different descriptions for the same projection.
\texttt{Mapyrus.dataset.min.x}, \texttt{Mapyrus.dataset.min.y}, \texttt{Mapyrus.dataset.max.x}, \texttt{Mapyrus.dataset.max.y}, \texttt{Mapyrus.dataset.center.x}, \texttt{Mapyrus.dataset.center.y} | The bounding rectangle of all data in the current dataset.
\texttt{Mapyrus.fetch.count} | The number of records already fetched from the current dataset.
\texttt{Mapyrus.fetch.more} | Flag value set to 1 if another record is available for \texttt{fetch} command, or 0 if no more records available.
\texttt{Mapyrus.filename} | The name of the file or URL being interpreted.
\texttt{Mapyrus.freeMemory} | The amount of free memory that Java has available, in bytes.
\texttt{Mapyrus.http.header} | An array containing header information passed in the HTTP request when running as an HTTP server.  Useful values are \texttt{Mapyrus.http.header['Referer']} giving the name of the referring HTML page, \texttt{Mapyrus.http.header['Cookie']} giving the contents of a cookie set by a previous HTTP request and \texttt{Mapyrus.http.header['User-Agent']} giving the name of the web browser making the HTTP request.
\texttt{Mapyrus.imagemap.x}, \texttt{Mapyrus.imagemap.y} | The pixel position of the point clicked in an HTML imagemap and passed to Mapyrus, for use when running as an HTTP server.  Both values are set to -1 if no imagemap point passed in current URL.
\texttt{Mapyrus.key.count} | The number of legend entries defined with \texttt{key} commands that have not yet been displayed with a \texttt{legend} command.
\texttt{Mapyrus.key.next} | The name of the of the next procedure to be displayed by the \texttt{legend} command.
\texttt{Mapyrus.page.format}, \texttt{Mapyrus.page.height}, \texttt{Mapyrus.page.width}, \texttt{Mapyrus.page.resolution.dpi}, \texttt{Mapyrus.page.resolution.mm} | The file format, page height, page width and resolution that were passed to the \texttt{newpage} command.  File format is in lowercase.  Height and width are in millimeters.  Resolution is available as either a dots-per-inch value, or a distance in millimeters between dots.
\texttt{Mapyrus.path} | The current path as an OGC WKT geometry with coordinates measured in millimetres.
\texttt{Mapyrus.path.length}, \texttt{Mapyrus.path.area}, \texttt{Mapyrus.path.start.angle}, \texttt{Mapyrus.path.start.x}, \texttt{Mapyrus.path.start.y}, \texttt{Mapyrus.path.end.angle}, \texttt{Mapyrus.path.end.x}, \texttt{Mapyrus.path.end.y}, \texttt{Mapyrus.path.min.x}, \texttt{Mapyrus.path.min.y}, \texttt{Mapyrus.path.max.x}, \texttt{Mapyrus.path.max.y}, \texttt{Mapyrus.path.center.x}, \texttt{Mapyrus.path.center.y}, \texttt{Mapyrus.path.width}, \texttt{Mapyrus.path.height} | The length of the current path on the page measured in millimeters, the area of the current path measured in square millimeters, the coordinates and angles at the start and end of the path in degrees measured counter-clockwise, and the bounding rectangle of the current path.
\texttt{Mapyrus.rotation} | The current rotation angle in degrees set by \texttt{rotate} command.  Returned value is normalised to fall in the range -180 to +180 degrees.
\texttt{Mapyrus.scale} | The current scale factor set by \texttt{scale} command.
\texttt{Mapyrus.screen.height}, \texttt{Mapyrus.screen.width}, \texttt{Mapyrus.screen.resolution.dpi}, \texttt{Mapyrus.screen.resolution.mm} | The height, width and resolution of the screen in which Mapyrus is running.  Height and width are in millimeters.  Resolution is available as either a dots-per-inch value, or a distance in millimeters between dots.
\texttt{Mapyrus.time.day}, \texttt{Mapyrus.time.month}, \texttt{Mapyrus.time.year}, \texttt{Mapyrus.time.hour}, \texttt{Mapyrus.time.minute}, \texttt{Mapyrus.time.second}, \texttt{Mapyrus.time.day.of.week}, \texttt{Mapyrus.time.day.name}, \texttt{Mapyrus.time.month.name}, \texttt{Mapyrus.time.week.of.year}, \texttt{Mapyrus.time.stamp} | Components of the current date and time.  Day of week has value 1 for Monday through to 7 for Sunday.
\texttt{Mapyrus.timer} | The elapsed processing time, measured in seconds.
\texttt{Mapyrus.totalMemory} | The total amount of memory available to Java, in bytes.
\texttt{Mapyrus.version} | The version of the software.
\texttt{Mapyrus.worlds.min.x}, \texttt{Mapyrus.worlds.min.y}, \texttt{Mapyrus.worlds.max.x}, \texttt{Mapyrus.worlds.max.y}, \texttt{Mapyrus.worlds.center.x}, \texttt{Mapyrus.worlds.center.y}, \texttt{Mapyrus.worlds.width}, \texttt{Mapyrus.worlds.height} | The bounding rectangle of world coordinates set with the \texttt{worlds} command.
\texttt{Mapyrus.worlds.scale} | The real-world scale factor, determined by dividing of the X axis world coordinate range by the page width.

## Commands

Commands are listed alphabetically.  The arguments required for each command
are given.  Some commands accept arguments in several ways.  For these
commands, each combination of arguments is given.

### addpath

addpath _geometry-field_ [, _geometry-field_ ...]

Adds geometry in each _geometry-field_ to current path.
A _geometry-field_ is geometry fetched from a dataset
with a \texttt{fetch} command or a string containing an
OGC WKT geometry.

Coordinates are transformed through any
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### arc

arc _direction_, _xCenter_, _yCenter_, _xEnd_, _yEnd_

Adds a circular arc to the current path.  The arc begins at
the last point added to the path and ends at (_xEnd_, _yEnd_)
with center at (_xCenter_, _yCenter_).
If _direction_ is a positive number, the arc travels clockwise,
otherwise the arc travels in an anti-clockwise direction.
If the begin and end points are the same then the arc is a complete circle.
A straight line segment is first added to the path if
the distance from the beginning point to the center is different
to the distance from the center to the end point.

Points are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### bezier

bezier _xControl1_, _yControl1_, _xControl2_, _yControl2_, _xEnd_, _yEnd_

Adds a Bezier curve (a spline curve) to the current path.  The curve begins at
the last point added to the path and ends at (_xEnd_, _yEnd_)
with control points (_xControl1_, _yControl1_) and
(_xControl2_, _yControl2_).

The control points define the direction of the line at the start and
end points of the Bezier curve.
At the start of the Bezier curve, the direction of the curve is towards
the first control point.
At the end of the Bezier curve, the direction of the curve is from
the second control point.

Points are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### blend

blend _mode_

Sets the blend mode for transparent colors.
Transparent colors are mixed differently with background
colors depending on the blend mode.

Blend mode is one of \texttt{Normal},
\texttt{Multiply},
\texttt{Screen},
\texttt{Overlay},
\texttt{Darken},
\texttt{Lighten},
\texttt{ColorDodge},
\texttt{ColorBurn},
\texttt{HardLight},
\texttt{SoftLight},
\texttt{Difference} or
\texttt{Exclusion}.

The effect of each blend mode is described in the [PDF Reference Manual](http://www.adobe.com).
Only the first six blend modes are available for SVG format output.

### box

box _x1_, _y1_, _x2_, _y2_

Adds a rectangle to the current path.
The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of the rectangle.

The two corner points of the box
are first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### box3d

box3d _x1_, _y1_, _x2_, _y2_ [, _depth_]

Adds a rectangle to the current path in the same way as
the \texttt{box} command.  The right side and top sides of the
box are also added to the current path to give a 3 dimensional effect.

The depth of the right and top sides is optional.  If not given
then the the smaller of box height and box width is used.

The two corner points and depth of the box
are first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### chessboard

chessboard _x1_, _y1_, _x2_, _y2_, _size_

Adds squares in a chessboard pattern to the current path.
The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of a rectangular area for the pattern, with
_size_ defining the size of each square.

The two corner points and size of squares
are first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### circle

circle _xCenter_, _yCenter_, _radius_

Adds a circle to the current path, with center
point (_xCenter_, _yCenter_) and radius
_radius_.

The center point and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### clearpath

clearpath

Removes all points from the current path.

### clip

clip _side_

Sets a clip path to the area covered by the current path,
or excluding the area covered by the current path, depending
on the value _side_.

If _side_ has value \texttt{inside} then
later drawing commands are limited to draw only inside the area
covered by current path.
If _side_ has value \texttt{outside} then
later drawing commands are limited to draw only outside the area
covered by current path.

If the path is clipped in a procedure, then the area remains
clipped until the procedure is complete.  Otherwise, the area
remains permanently clipped for the page.
When more than one path is clipped, drawing is limited to
areas that satisfy all clip paths.

The current path is not modified by this command.

### closepath

closepath

Closes the current path by adding a straight line segment
back to the last point added with a \texttt{move} command.

### color

color _name_ [, _alpha_]

color "contrast" [, _alpha_]

color "brighter" [, _alpha_]

color "darker" [, _alpha_]

color "softer" [, _alpha_]

color "current" [, _alpha_]

color "\#_hexdigits_" [, _alpha_]

color "0x_hexdigits_" [, _alpha_]

color "cmyk(_cyan_,_magenta_,_yellow_,_black_)" [, _alpha_]

color "rgb", _red_, _green_, _blue_ [, _alpha_]

color "hsb", _hue_, _saturation_, _brightness_ [, _alpha_]

color "cmyk", _cyan_, _magenta_, _yellow_, _black_ [, _alpha_]

Sets color for drawing.  Around 500 commonly used color names are defined,
additional color names are defined in a file given as a startup variable (see
Table \ref{startupvariables} on page \pageref{startupvariables}).
Color names are case-insensitive.

If color name is \texttt{contrast} then color is set to either black or white,
whichever constrasts more with the current color.

If color name is \texttt{brighter}, \texttt{darker} or \texttt{softer}
then color is set to a brighter, darker or softer version of the current color.

If color name is \texttt{current} then the current color is set again.

_hexdigits_ is a 6 digit hexadecimal
value defining RGB values, as used in HTML pages.

_red_, _green_ and _blue_ values for RGB colors and
_hue_, _saturation_ and _brightness_ values for
Hue-saturation-brightness (HSB) colors are given as intensities in the range
0-1.

_cyan_, _magenta_, _yellow_ and _black_
values for CMYK colors are in the range 0-1.

The alpha value is optional and defines transparency as a value in the range
0-1.  An alpha value of 1 is completely opaque and the color overwrites
underlying colors.  An alpha value of 0 is completely transparent and the color
is not visible.  Intermediate values are partially transparent and the color is
blended with colors of underlying shapes on the page.  The
\texttt{blend} command controls how transparent colors are mixed
with background colors.

Colors are opaque if an alpha value is not given.

Transparent colors are only available for BMP, JPEG, PNG, PPM, SVG, PDF
and Encapsulated PostScript format image (\texttt{epsimage}) output.

The PostScript language does not contain any functions for setting
transparency.
All colors in PostScript and Encapsulated PostScript
files will be opaque.

The spelling \texttt{colour} is also accepted for this command.

### cylinder

cylinder _xCenter_, _yCenter_, _radius_, _height_

Adds a cylindrical shape to the current path, with center
point (_xCenter_, _yCenter_) and given radius and height.

The center point, radius and height are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### dataset

dataset _format_, _name_ [, _extras_]

Defines a dataset to read from.  A dataset contains geographic data,
geometry, attributes, a lookup table, data to write to standard
output, or a combination of these.

_dataset_ is the filename of the dataset to read.
_format_ is the format of the dataset and
_extras_ defines further options for accessing the dataset, given
as _variable=value_ values, separated by whitespace.
Data formats and options are shown in Table \ref{datasettypes}.


\begin{longtable}{|p{3cm}|p{10cm}|}
\hline
\label{datasettypes}
Dataset Format & Description and Extras \\
\hline
\hline
\endfirsthead
\hline
\caption{Dataset Formats} \\
\endfoot

\hline
Dataset Format & Description and Extras \\
\hline
\hline
\endhead

\texttt{internal} &
Reads a dataset included inside Mapyrus.
Available dataset names are \texttt{countries},
\texttt{capitals} and \texttt{usa}.

Dataset \texttt{countries} contains the following
fields for each record:

* \texttt{GEOMETRY} Polygon with country border
* \texttt{ISOCODE} Country ISO code
* \texttt{COUNTRY} Country name
* \texttt{POP2005} Population in year 2005
* \texttt{COLORCODE} number in range 1 - 6, with neighbouring countries
  having different color codes.

Dataset \texttt{capitals} contains the following
fields for each record:

* \texttt{GEOMETRY} Point with capital
* \texttt{COUNTRY} Country name
* \texttt{CAPITAL} Capital city name

Dataset \texttt{usa} contains the following
fields for each record:

* \texttt{GEOMETRY} Polygon with US state border
* \texttt{STATE} US state name
* \texttt{STATECODE} two letter US state code
* \texttt{COLORCODE} number in range 1 - 6, with neighbouring states
  having different color codes.

\vspace{10pt}
Extras:

\texttt{xmin=_x1_},
\texttt{ymin=_y1_},
\texttt{xmax=_x2_},
\texttt{ymax=_y2_}

Bounding rectangle of data to fetch.  Data outside this rectangle is
not fetched.  Setting bounding rectangle to same values as world
coordinate values in \texttt{worlds} command improves performance. \\

\hline

\texttt{jdbc} &
Accesses data held in a relational database with
an SQL \texttt{select} statement via JDBC.
_name_ contains the SQL query to execute.
For each fetched record, field values are assigned to variables
with the name of the fields.
Field values that are NULL are converted to either an empty string,
or numeric zero, depending on their type.
Binary and blob fields are interpreted as OGC WKB geometry values.

Some databases convert all field names to upper case, or to lowercase.
Use a field name alias for fields that are the result of an expression.

\vspace{10pt}
Extras:

\texttt{driver=_string_}

The name of the Java class containing a JDBC 1.0 (or higher)
driver for connecting to the database.
This class name is required if not given in the startup variable
\texttt{jdbc.drivers} (see Table \ref{startupvariables}
on page \pageref{startupvariables}).
The JAR file containing the class must be included in the \texttt{-classpath}
option when starting Mapyrus.

\vspace{10pt}
\texttt{url=_string_}

URL containing the database name, host and other information for identifying
the database to connect to.
The format of this string is database dependent.
The database remains connected after use and the connection is reused in later
\texttt{dataset} commands with the same \texttt{url} value.
When Mapyrus is run using the HTTP server option, a pool of
database connections are used for each \texttt{url} value to avoid
continually reconnecting to the database.
Mapyrus automatically closes bad and idle connections
and Mapyrus will reconnect if the database is restarted.

\vspace{10pt}
\texttt{user=_string_}

Username for connecting to the database.

\vspace{10pt}
\texttt{password=_string_}

Password for connecting to the database.

\vspace{10pt}
\texttt{jndiname=_string_}

Java JNDI resource name of DataSource to use for database connection.
Available when running as a servlet within Apache Tomcat.
Tomcat provides database connection
pooling with database connections obtained using a JNDI lookup.
JNDI resource names are commonly of the form \texttt{java:/comp/env/jdbc/MYDB1}.
No other options are required when using a JNDI name.  The database
driver, user, password and URL are set in \texttt{mapyrus.war}, in
files \texttt{WEB-INF/web.xml} and \texttt{META-INF/context.xml}.

\vspace{10pt}
Other values are set as properties for the JDBC driver. \\

\hline

\texttt{osm} &
Reads from OpenStreetMap URL or file _name_.
Each node or way is fetched as a separate record.  For each node or way,
the variable \texttt{TYPE} is set to either \texttt{node}
or \texttt{way} to indicate the type of data, \texttt{ID}
is set to the ID of the node or way,
\texttt{GEOMETRY} is set to the geometry of the
node or way, and \texttt{TAGS} is created
as an array containing the tag information for the node
or way. \\

\hline

\texttt{shapefile} &
Reads from ESRI Shape format file with URL or filename _name_.
The geometry for each fetched record is assigned to a variable named
\texttt{GEOMETRY}, attribute field values are assigned to
variables with attribute field names.

\vspace{10pt}
Extras:

\texttt{dbffields=_field1_,_field2_,...}

Comma-separated list of
attribute fields to read from the DBF database file accompanying the
Shape file.  By default, all fields are read.  Reading fewer attribute
fields improves performance.

\vspace{10pt}
\texttt{encoding=_charset_}

Character set encoding of file.
Common character set encodings are \texttt{UTF-8} (Unicode) and
\texttt{ISO-8859-1} (also known as ISO Latin1).

\vspace{10pt}
\texttt{xmin=_x1_},
\texttt{ymin=_y1_},
\texttt{xmax=_x2_},
\texttt{ymax=_y2_}

Bounding rectangle of data to fetch.  Data outside this rectangle is
not fetched.  Setting bounding rectangle to same values as world
coordinate values in \texttt{worlds} command improves performance. \\

\hline

\texttt{textfile} &
Reads from delimited text file or URL _name_, with one
record per line.  Fields in fetched record
are assigned to variables
\texttt{\$1}, \texttt{\$2}, \texttt{\$3}, ...
and the whole record is assigned to variable
\texttt{\$0}.
If _name_ is \texttt{-} then standard input is read.
If _name_ has suffix \texttt{.gz} or \texttt{.zip}
then it is automatically decompressed as it is read.

\vspace{10pt}
Extras:

\texttt{comment=_string_}

Character string at start of a line marking a comment line that
is to be ignored.  Default value is a hash character (\texttt{\#}).

\vspace{10pt}
\texttt{delimiter=_character_}

Character separating fields in the text file.  Default value
is all whitespace characters.

\vspace{10pt}
\texttt{encoding=_charset_}

Character set encoding of file.
Common character set encodings are \texttt{UTF-8} (Unicode) and
\texttt{ISO-8859-1} (also known as ISO Latin1). \\

\end{longtable}

### draw

draw _x_, _y_, ...

Adds one or more straight line segments to the current path.
A straight line segment is added from the previously defined point
to (_x_, _y_) and then to each further point given.
Points are first transformed through any world coordinate
transformation set with a \texttt{worlds} command
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### ellipse

ellipse _xCenter_, _yCenter_, _xRadius_, _yRadius_

Adds an ellipse to the current path, with center
point (_xCenter_, _yCenter_).  The radius of the ellipse
in the horizontal direction is _xRadius_ and in the vertical
direction _yRadius_.

The center point and radius values are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### endpage

endpage

Closes output file created with \texttt{newpage} command.

### eps

eps _filename_ [, _size_]

Displays an Encapsulated PostScript file at each \texttt{move} point in
the current path.  The file is centered at each point.

Encapsulated PostScript files can only be displayed when creating
PostScript or Encapsulated PostScript output.  For other formats,
a grey box is drawn showing where the Encapsulated PostScript file
would be drawn.

_filename_ is the name of an Encapsulated PostScript file.

_size_ is the optional size for the Encapsulated PostScript file
in millimeters.  If no size
is given or size is zero then the file is displayed at its natural size,
as defined in the Encapsulated PostScript file.
The file is scaled and rotated according to the current \texttt{scale}
and \texttt{rotate} settings.

### eval

eval _command_

Evaluates any variables in _command_ and then
runs the result as a new command.
This command is identical to the \texttt{eval} command found in
UNIX scripting and Perl and
enables commands to be built and executed while Mapyrus runs.

### eventscript

eventscript _tags_ ...

This command is used in combination with the \texttt{imagemap}
option of the \texttt{newpage} command to create an HTML imagemap.

This command creates an entry in the imagemap with the given HTML tags
for the area covered by the current path.
Useful HTML tags include hyperlinks and callbacks for mouse events.
Example HTML tags are:

```
href="australia.html"

onMouseClicked="return alert('Message!');"
```

To create an imagemap entry for a single point, first use the \texttt{box}
command to define a box a few pixels in size around the point.

To create an imagemap entry for a line, first create a polygon
around the line using the \texttt{buffer} function.

See section
\ref{tutorialjavascript}
for an example displaying tooltips as the mouse is moved over an image.

### fetch

fetch

Fetches next record from current dataset.
For each field in the record, a variable is defined with the name
of the field and the value of the field for the next record.
Before fetching a record, check the variable
\texttt{Mapyrus.fetch.more}
to ensure that another record is available from the dataset.

### fill

fill [_xml-attributes_]

Flood fills the current path with the current color.
The winding rule is used for determining the inside and outside
regions of polygons containing islands.
The current path is not modified by this command.

For SVG output, any XML attributes given in
_xml-attributes_ are
included in the \texttt{<path>} XML element for the filled path.

### flowlabel

flowlabel _spacing_, _offset_ [,_extras_ ], _string_ [, _string_ ...]

Draws a label following the current path,
using the font set with the \texttt{font} command.
_string_ values are
separated by spaces.
_offset_ is the distance along the path at which to begin
the label, given in millimeters.
_spacing_ is the spacing distance
between each letter, given in millimeters.
_extras_ defines whether labels that
would appear upside down on the page are rotated
180 degrees so as to be readable.  By default, labels are rotated.
If _extras_ contains \texttt{rotate=false} then labels are
not rotated.

### font

font _name_, _size_ [, _extras_ ...]

Sets font for labelling with the \texttt{label} command.
Font _name_ and _size_ are the name and size in
millimeters of the font to use.

If a scale factor was set with the
\texttt{scale} command
then font size is scaled by this factor.

If a rotation was set with the
\texttt{rotate} command
then labels follow current rotation angle.
If no rotation is set then labels are displayed
horizontally.

Font _name_
depends on the output format set with the
\texttt{newpage} command.
For PostScript output, _name_ is the name of a PostScript Type 1
font.
For output to an image format, _name_ is one of the Java Logical
font names (\texttt{Serif}, \texttt{SansSerif},
\texttt{Monospaced}, \texttt{Dialog}, or \texttt{DialogInput}) or a TrueType
font name.

Tutorial Sections \ref{psfonts}, \ref{ttffonts} and \ref{svgfonts}
describe different font formats.

_extras_ defines further options for the font, given as
_variable=value_ values, separated by whitespace.
See Table \ref{fontextras}
for available options.

\begin{longtable}{|l|p{7cm}|}
\hline
\label{fontextras}
Extra & Description \\
\hline
\hline
\endfirsthead
\hline
\caption{Font Extras} \\
\endfoot

\hline
Extra & Description \\
\hline
\hline
\endhead

\texttt{outlinewidth=_width_} &

Sets line width to use for drawing outline of each letter in label.
Only the outline of each letter is drawn, no part of the letter is filled. \\

\texttt{linespacing=_spacing_} &

Sets spacing between lines for labels with multiple lines.
Line spacing is given as a multiple of the font size.
The default line spacing is 1. \\

\hline
\end{longtable}

### geoimage

geoimage _filename_ [, _extras_ ]

geoimage _url_ [, _extras_ ]

geoimage _WebMapServiceUrl_ [, _extras_ ]

Displays a geo-referenced image.

_filename_ or
_url_
is the name of a BMP, GIF, JPEG, PNG, PPM or XBM format image file.
An associated "worlds" file with suffix \texttt{.tfw} must exist,
defining the world coordinate range covered by the image.
The \texttt{extras} option \texttt{readerclass} enables additional
image formats to be read using external Java classes.

_webMapServiceUrl_ is a URL request to an
OGC Web Mapping Service (WMS)
for an image.  The request type must be \texttt{GetMap}.
The world coordinate range for the image is parsed from the
\texttt{BBOX} parameter in the URL.
See the [Web Map Service Implementation](http://www.opengis.org)
specification
for details of all parameters that must be included in the URL.

_extras_ defines further options for the image, given as
_variable=value_ values, separated by whitespace.
See Table \ref{geoimageextras}
for available options.

Images cannot be displayed when creating SVG format output.

For PDF format output, large output images are saved as temporary
files in the Java temporary directory
(see Table \ref{startupvariables} on page \pageref{startupvariables})
until output is complete.

Extra | Description
----- | -----------
\texttt{clipfile=_filename_} | Gives name of a text file containing a clip polygon for the image.  Each line of the text file defines one (X, Y) world coordinate of the clip polygon.  Only image data inside the clip polygon is displayed.  Using a clip polygon prevents display of a non-rectangular image from overwriting a neighbouring image.
\texttt{hue=_factor_} \texttt{saturation=_factor_} \texttt{brightness=_factor_} | Defines a hue, saturation or brightness multiplication factor for the image.
\texttt{readerclass=_classname_} | Gives the name of a Java class to read the image and the world coordinate range covered by the image.  The Java class must be included in the Java classpath and must contain the following methods: \texttt{constructor(String filename, String extras)} \texttt{java.awt.image.BufferedImage read()} \texttt{java.awt.geom.Rectangle2D getBounds()}. The methods may throw any type of Java exception on error.  This option enables Mapyrus to be extended to read additional image formats.

### gradientfill

gradientfill _color1_, _color2_, _color3_, _color4_ [, _color5_ ...]

Fills the current path with a gradient fill pattern.
Color names _color1_, _color2_, _color3_ and
_color4_ define the color for the lower-left corner,
lower-right corner, upper-left corner and upper-right corner of
the polygon.

If _color5_ is given then it defines an additional color at the
the center of the polygon.

Colors in the interior of the polygon fade from the color defined
in each corner to the colors in the other corners.

The winding rule is used for determining the inside and outside
regions of polygons containing islands.
The current path and current color are not modified by this command.

For SVG output only gradient fill patterns that fade in one dimension
(vertically or horizontally) are possible.  This is a limitation
of the SVG format.

### guillotine

guillotine _x1_, _y1_, _x2_, _y2_

Cuts path against a rectangle.
Any part of the path inside or
on the boundary of the rectangle remains.
Any part of the path outside the rectangle is removed.
The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of the rectangle to cut against.

The four corner points of the rectangle
are first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

The path is always cut against a rectangle
aligned with the X and Y axes of the page, regardless of any rotation angle.

### hexagon

hexagon _xCenter_, _yCenter_, _radius_

Adds a hexagon shape to the current path, with center
point (_xCenter_, _yCenter_) and distance
_radius_
from the center point to each vertex.

The center point and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### httpresponse

httpresponse _header_

Sets the complete header to return from an HTTP request
when Mapyrus is running as an HTTP server.  The header
must include at least two lines containing an HTTP server
response code and a MIME content type.  See section \ref{cookies}
for example HTTP headers.

### icon

icon _filename_, [, _size_]

icon "_binarydigits_" [, _size_]

icon "0x_hexdigits_" [, _size_]

icon "resource:_resourcename_" [, _size_]

Displays an image icon at each \texttt{move} point in
the current path.  The icon is centered at each point.

_filename_ is a the name of a file, URL or Java resource
containing the icon.
The icon must be either \texttt{BMP}, \texttt{GIF}, \texttt{JPEG},
\texttt{PAT}\footnote{The Gimp pattern file format},
\texttt{PNG},
\texttt{PPM} or
\texttt{XBM} image format.

_binarydigits_ are 16, 64 or 256 binary digits (all 0's and 1's)
defining a square single color bitmap of size 4x4, 8x8 or 16x16 pixels.
Any other characters in the string are ignored.

_hexdigits_ are 4, 16 or 64 hexadecimal digits defining
a square single color bitmap image of size 4x4, 8x8 or 16x16 pixels.
Any non-hexadecimal characters in the string are ignored.

_resourcename_ is the name of the Java resource containing the
image, in the form \texttt{au/com/company/filename.png}.
This option enables images from a Java JAR file included in the 
in the \texttt{-classpath} startup option to be displayed.

_size_ is the optional size for the icon in millimeters.  If no size
is given or size is zero then the icon is displayed at its natural size,
as it would appear in an image viewer with one image pixel per display
pixel.
The image is scaled and rotated according to the current \texttt{scale}
and \texttt{rotate} settings.

In PostScript and PDF files, icons with more than one color
are displayed with an opaque white background.  This is a limitation
of PostScript and PDF output.

Icon images are loaded into memory.
Loading very large images will use a large amount of memory.
Page \pageref{morememory} describes how to make more memory available
for Mapyrus.

Icons cannot be displayed when creating SVG format output.

### justify

justify _justification_

Sets justification for labelling with the \texttt{label} command.
_justification_ is a string containing either
\texttt{left}, \texttt{right}, \texttt{center} for justifying labels
horizontally and/or
\texttt{top}, \texttt{middle}, \texttt{bottom} for justifying labels
vertically.

### key

key _type_, _description_, [_arg1_, _arg2_ ...]

Defines an entry for a legend.  The procedure containing this command will be
called with arguments _arg1_, _arg2_ ... to display a sample of
the symbol when a legend is generated with a \texttt{legend} command.  This
command is ignored if used outside of a procedure.

If _description_ contains the string \texttt{(\#)} then it will
be replaced in a legend by the number of times that the legend entry
is defined.

_type_ is either
\texttt{point} to display the legend entry as a single point,
\texttt{line} to display the legend entry as a horizontal line,
\texttt{zigzag} to display the legend entry as a zig-zag line,
or
\texttt{box} to display the legend as a box.
_description_ is the label for the legend entry.

If a procedure displays more than one type of type of symbol depending
on the arguments passed to it then use a separate
\texttt{key} command for each, with different descriptions
and different arguments.

### label

label _string_ [, _string_ ...]

Draws a label at each point in the path set with the \texttt{move} command,
using the font, justification and rotation set with the \texttt{font},
\texttt{justify} and \texttt{rotate} commands.  _string_ values are
separated by spaces.  If _string_ contains newline characters
(`\\n`) then labels are displayed as multiple lines, one
below the other.

### legend

legend _size_

Displays legend entries defined with
\texttt{key} commands at points defined with
\texttt{move} commands.

Each legend entry corresponds to a procedure and a set of arguments.  The first
legend entry is displayed at the first \texttt{move} point by calling the
procedure in which the first legend entry was defined.  Then the second legend
entry is displayed at the second \texttt{move} point.  This continues until
either all legend entries are displayed or all move points are used.

The variable
\texttt{Mapyrus.key.count}
contains the number of legend entries that remain to be displayed.

If there are more legend entries than 
\texttt{move} points then some legend entries
remain undisplayed and will be displayed in the next legend.

Legend entries are displayed in the order in which they
are encountered in called procedures.

Legend entries in procedures that were never called are
not included in the legend.  Therefore, the legend only shows
entries that were actually displayed.

_size_ defines the size of each legend entry, in millimeters.

The description label is displayed to the right of each legend entry, using the
current \texttt{color}, \texttt{font} and \texttt{justify} settings.

### let

let _var_ = _expression_, ...

Assigns result of evaluating _expression_ to a variable with name
_var_.  The variable is globally accessible unless defined as local to
the current procedure with a \texttt{local} command.

Variable _var_ is either a simple variable name, an array, or an array
element of the form _var_[_index_].

Several variables are assigned by separating each 
_var_ and _expression_ pair by a comma.

### linestyle

linestyle _width_

linestyle _width_, _cap_, _join_

linestyle _width_, _cap_, _join_, _phase_, _dash length_, ...

Sets style line drawing by the \texttt{stroke}
command.
Line _width_ given in millimeters.
_cap_ is the style to use at the ends of lines, either
\texttt{butt}, \texttt{round} or \texttt{square}.
_join_ is the style to use where lines join, either
\texttt{bevel}, \texttt{miter} or \texttt{round}.
One or more _dash length_ values are given, alternating
between the length of one dash and the length of space between
dashes in a dash pattern.  Each _dash length_ is given in millimeters.
_phase_ is the offset in millimeters into the dash pattern 
at which to begin.

### local

local _name_, [_name_ ...]

Declares the listed variable names as local to a procedure.
The variables are not visible outside the enclosing procedure
and their values are lost when the procedure ends.

### logspiral

logspiral _xCenter_, _yCenter_, _a_, _b_, _revolutions_, _startAngle_

Adds a logarithmic spiral to the current path, with center
point (_xCenter_, _yCenter_).  The values _a_
and _b_ are used in the polar coordinates formula
r = a * e ^ (b * &#x03B8;)
to generate the spiral points.

_revolutions_ defines the number of loops of the spiral.
If _revolutions_
is a positive number then the spiral is drawn in a anti-clockwise direction.
If _revolutions_ is a negative number then the spiral
is drawn in an clockwise direction.

_startAngle_ defines the
angle at which the inner revolution of the spiral starts.

The center point and start angle are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### mimetype

mimetype _type_

Sets MIME type for content being returned from HTTP request
when Mapyrus is running as an HTTP server.  A more general
solution is to use the \texttt{httpresponse} command to set
the complete header returned from the HTTP request.

### move

move _x_, _y_

Adds the point (_x_, _y_) to the current path.  The
point is first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### newpage

newpage _format_, _filename_, _width_, _height_, _extras_

newpage _format_, _filename_, _paper_, _extras_

Begins output of a new page to a file.  Any previous output is closed.  The
path, clipping path and world coordinates are cleared.  The origin of
the new page is in the lower-left corner.  _format_ is the
file format to use for output, one of:

* \texttt{eps} for Encapsulated PostScript output,
  with shapes and labels defined geometrically.
* \texttt{ps}, \texttt{postscript} or \texttt{application/postscript}
  for PostScript output,
  with shapes and labels defined geometrically.
* \texttt{pdf} or \texttt{application/pdf} for Portable Document
  Format output.
* \texttt{screen} to display output in a window on the screen.
* \texttt{bmp} or \texttt{image/bmp} for BMP image output.
* \texttt{jpeg} or \texttt{image/jpeg} for JPEG image output.
* \texttt{png} or \texttt{image/png} for PNG image output.
* \texttt{ppm} or \texttt{image/x-portable-pixmap} for Netpbm PPM image output.
* \texttt{epsimage} for Encapsulated PostScript image output.
  Output contains
  a single image in which all shapes and labels have been drawn.
* \texttt{svg} or \texttt{image/svg+xml} for Scalable Vector Graphics
  (SVG) output.

_paper_ is a paper size name for the page.
Alternatively, _width_ and _height_ are the dimensions of the page
in millimeters, or as values such as \texttt{256px} or \texttt{11in}
including the units.

_filename_ is the name of the file to write the page to.
If _filename_ is a dash (\texttt{-})
then the page is written to standard output.
If _filename_ begins with a pipe (\texttt{|}) then the rest
of _filename_ is interpreted as an operating system
command.  The operating system command is executed and Mapyrus
writes the page to the standard input of the executing
operating system command.

_extras_ defines further options for the new page, given as
_variable=value_ values, separated by whitespace.
See Table \ref{outputformats}
for options available for each type of output.

\begin{longtable}{|p{4cm}|p{10cm}|}
\hline
\label{outputformats}
File Format & Extras \\
\hline
\hline
\endfirsthead
\hline
\caption{Output Formats} \\
\endfoot

\hline
File Format & Extras \\
\hline
\hline
\endhead

PostScript, Encapsulated PostScript &

\texttt{background=_color_}

Background color for page, as a named color, hex digits or CMYK values
in parentheses.

\vspace{10pt}
\texttt{afmfiles=_filename_,_filename2_,...}

Comma-separated list of PostScript Type 1 font metrics filenames
to include in this PostScript file.
PostScript Type 1 font metrics are defined in a file
with suffix \texttt{.afm}.  Include
files for all PostScript fonts to be used in this
page that are not known by the printer.

See section \ref{psfonts} for
information on converting TrueType fonts to PostScript Type 1 format.

\vspace{10pt}
\texttt{pfafiles=_filename_,_filename2_,...}

Comma-separated list of ASCII PostScript Type 1 font definition filenames
to include in this PostScript file.
An ASCII PostScript Type 1 font definition is defined in a file
with suffix \texttt{.pfa}.  Include
files for all PostScript fonts to be used in this
page that are not known by the printer.

See section \ref{psfonts} for
information on converting TrueType fonts to ASCII PostScript Type 1 format.

\vspace{10pt}
\texttt{isolatinfonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which ISO Latin1 character encoding
(also known as ISO-8859-1 encoding)
is required.  Use ISO Latin1 encoding
when extended characters such as accented characters
or a copyright symbol are to be displayed from the font.

\vspace{10pt}
\texttt{glyphfile=_filename_}

Name of file replacing default Adobe Glyph List file \texttt{glyphlist.txt}
that is included in Mapyrus.
This file defines glyph names in PostScript fonts for all characters.

\vspace{10pt}
\texttt{minimumlinewidth=_value_}

Sets a minimum line width.  Thinner lines will be changed to
this width.  This avoids very thin lines which appear differently
in different output formats.

\vspace{10pt}
\texttt{resolution=_value_}

Resolution for page, given as a dots-per-inch value.  Replaces
default value of 300.

\vspace{10pt}
\texttt{turnpage=_flag_}

If _flag_ is \texttt{true} then
turns a landscape orientation page 90 degrees so that it appears
as a portrait page.

\vspace{10pt}
\texttt{update=_flag_}

If _flag_ is \texttt{true} then the file with name
_filename_ is an existing PostScript 
file that is opened for editing.  The existing file must be an
Encapsulated PostScript file or a PostScript file containing only
a single page.
Drawing commands will draw over the top of the existing page.
Page size is set to the size of the existing page,
_width_ and _height_ of the new page are ignored.  \\

\hline

PDF &

\vspace{10pt}
\texttt{afmfiles=_filename_,_filename2_,...}

Comma-separated list of PostScript Type 1 font metrics filenames
to include in this PDF file.
PostScript Type 1 font metrics are defined in a file
with suffix \texttt{.afm}.  Include
files for all PostScript fonts to be used in this
page that are not one of the 14 standard PDF fonts.

See section \ref{pdffonts} for
information on converting TrueType fonts to binary PostScript Type 1 format.

\vspace{10pt}
\texttt{pfbfiles=_filename_,_filename2_,...}

Comma-separated list of binary PostScript Type 1 font definition filenames
to include in this PDF file.
A binary PostScript Type 1 font definition is defined in a file
with suffix \texttt{.pfb}.  Include
files for all PostScript fonts to be used in this
page that are not one of the 14 standard PDF fonts.

See section \ref{pdffonts} for
information on converting TrueType fonts to binary PostScript Type 1 format.

\vspace{10pt}
\texttt{isolatinfonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which ISO Latin1 character encoding
(also known as ISO-8859-1 encoding)
is required.  Use ISO Latin1 encoding
when extended characters such as accented characters
or a copyright symbol are to be displayed from the font.

\vspace{10pt}
\texttt{isolatin2fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which ISO Latin2 character encoding
(also known as ISO-8859-2 encoding)
is required.  ISO Latin2 fonts contain characters used in
Central European languages.

\vspace{10pt}
\texttt{isolatin9fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which ISO Latin9 character encoding
(also known as ISO-8859-15 encoding)
is required.

\vspace{10pt}
\texttt{isolatin10fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which ISO Latin10 character encoding
(also known as ISO-8859-16 encoding)
is required.

\vspace{10pt}
\texttt{glyphfile=_filename_}

Name of file replacing default Adobe Glyph List file \texttt{glyphlist.txt}
that is included in Mapyrus.
This file defines glyph names in PostScript fonts for all characters. \\

PDF &

\vspace{10pt}
\texttt{otffiles=_filename_,_filename2_,...}

Comma-separated list of OpenType font filenames
to include in this PDF file.
An OpenType font file
has suffix \texttt{.otf}.

\vspace{10pt}
\texttt{background=_color_}

Background color for page, as a named color, hex digits or CMYK values
in parentheses.

\vspace{10pt}
\texttt{maximumimagememory=_value_}

Sets the maximum amount of memory in megabytes
to use for holding PDF image output.  If images in PDF output
exceed this limit then further images are stored in temporary files
until PDF output is complete and the PDF output file is saved.
By default, a maximum of 16MB of memory is used.

\vspace{10pt}
\texttt{minimumlinewidth=_value_}

Sets a minimum line width.  Thinner lines will be changed to
this width.  This avoids very thin lines which appear differently
in different output formats.

\vspace{10pt}
\texttt{resolution=_value_}

Resolution for page, given as a dots-per-inch value.  Replaces
default value of 72.

\vspace{10pt}
\texttt{turnpage=_flag_}

If _flag_ is \texttt{true} then
turns a landscape orientation page 90 degrees so that it appears
as a portrait page.

\vspace{10pt}
\texttt{windows1250fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which Windows 1250 character encoding is required.

\vspace{10pt}
\texttt{windows1251fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which Windows 1251 character encoding is required.

\vspace{10pt}
\texttt{windows1252fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which Windows 1252 character encoding is required.

\vspace{10pt}
\texttt{windows1253fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which Windows 1253 character encoding is required.

\vspace{10pt}
\texttt{windows1254fonts=_fontname_,_fontname2_,...}

Comma-separated list of PostScript Type 1 font names for
which Windows 1254 character encoding is required.  \\

\hline

Scalable Vector Graphics (SVG) &

\texttt{background=_color_}

Background color for page, as a named color, hex digits or CMYK values
in parentheses.

\vspace{10pt}
\texttt{compress=_flag_}

If _flag_ is \texttt{true} then output is compressed with GZIP
compression.

\vspace{10pt}
\texttt{minimumlinewidth=_value_}

Sets a minimum line width.  Thinner lines will be changed to
this width.  This avoids very thin lines which appear differently
in different output formats.

\vspace{10pt}
\texttt{resolution=_value_}

Resolution for page, given as a dots-per-inch value.  Replaces
default value of screen resolution.

\vspace{10pt}
\texttt{scriptfile=_filename_}

Name of file containing an XML \texttt{<script> ... </script>}
element to add to SVG file. \\

\hline

BMP, JPEG, PNG, PPM images, output to a window on screen
and Encapsulated PostScript images &

\texttt{background=_color_}

Background color for image, as a named color, hex digits or CMYK values
in parentheses.

\vspace{10pt}
\texttt{fractionalfontmetrics=_flag_}

If _flag_ is \texttt{true} then slower, more accurate
calculations are made for positioning letters in labels.
Fractional font metrics are not used by default.

\vspace{10pt}
\texttt{imagemap=_filename_}

Creates a file containing an HTML imagemap for the image.
An HTML imagemap contains hyperlinks to jump to and JavaScript
functions to execute when the mouse is moved or clicked over the image.
Entries in the imagemap are defined using the \texttt{eventscript} command.

A completed imagemap file is surrounded by an HTML \texttt{<map>} tag
and included in an HTML file.

\vspace{10pt}
\texttt{labelantialiasing=_flag_}

If _flag_ is \texttt{true} then
labels are drawn with anti-aliasing, improving readability.
Labels are drawn with anti-aliasing by default.

\vspace{10pt}
\texttt{lineantialiasing=_flag_}

If _flag_ is \texttt{true} then
lines are drawn with anti-aliasing.
Lines are not drawn with anti-aliasing by default.

\vspace{10pt}
\texttt{minimumlinewidth=_value_}

Sets a minimum line width.  Thinner lines will be changed to
this width.  This avoids very thin lines which appear differently
in different output formats.

\vspace{10pt}
\texttt{resolution=_value_}

Resolution for page, given as a dots-per-inch value.  Replaces
default value of screen resolution.

\vspace{10pt}
\texttt{ttffiles=_filename_,_filename2_,...}

Comma-separated list of TrueType font filenames
to load for this page.
A TrueType font is defined in a file
with suffix \texttt{.ttf}.

Do not use this option on operating systems that support
TrueType fonts (Windows, Mac).  All TrueType
fonts are already available from the operating system.

On operating systems that do not support TrueType fonts
(Linux, UNIX) include filenames of all TrueType
fonts to be used on this page.  These fonts are loaded
by Mapyrus.

\vspace{10pt}
\texttt{update=_flag_}

If _flag_ is \texttt{true} then the file with name
_filename_ is an existing file that is opened for editing.
Drawing commands will draw over the top of the existing image
in the file.
Page size is set to the size of the existing image,
_width_ and _height_ of the new page are ignored.  \\

\hline

\end{longtable}

### parallelpath

parallelpath _distance_ [, _distance_ ...]

Replaces current path with new paths parallel to current path.
For each given distance, a new path is created at _distance_
millimeters to the right of current path.  If a _distance_ is
negative then path is created to the left of the current path.

When used on complex paths with sharp angles, this command creates
paths that self-intersect.

### pdf

pdf _filename_, _page_ [, _size_]

Displays a Portable Document Format (PDF) file at each \texttt{move} point in
the current path.  

_filename_ is the name of a PDF file.
_page_ is the page number from the PDF file to display.

_size_ is the optional size for the PDF file
in millimeters.  If no size
is given or size is zero then the file is displayed at its natural size,
as defined in the PDF file.
The file is scaled and rotated according to the current \texttt{scale}
and \texttt{rotate} settings.

PDF files can only be displayed when creating
PDF output.  For other formats,
a grey box is drawn showing where the PDF file
would be drawn.

### pdfgroup

pdfgroup _action_ [, _groupName_]

Controls creation of groups in PDF output files (known as
_Optional Content Groups_ in PDF terminology).

A PDF viewer such as Acrobat Reader enables each group in a PDF file to
be turned on and off independently of other groups.

The _action_ \texttt{begin} and a group name begins a group.
All following output until the matching \texttt{end} action is
included in the group.

The _action_ \texttt{end} completes a group.  Any open groups are
automatically ended when a PDF output file is finished.

Nesting of groups is permitted.

For formats other than PDF, this command has no effect.

### pentagon

pentagon _xCenter_, _yCenter_, _radius_

Adds a pentagon shape to the current path, with center
point (_xCenter_, _yCenter_) and distance
_radius_
from the center point to each vertex.

The center point and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### print

print _string_ [, _string_ ...]

Prints each _string_ to standard output,
separated by spaces.
A newline is added after the final _string_.

Standard output is redirected to a different file
or destination using the \texttt{setoutput} command.

### protect

protect _x1_, _y1_, _x2_, _y2_

protect _geometry_

protect

Marks a region of the page as protected.
The function \texttt{protected} will then return 1 for any
point in this region.

The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of the rectangle to mark as protected.

If _geometry_ containing a polygon is given, then the region
covered by that polygon is protected.

If no arguments are given then the region inside the current path is
protected.

The rectangle or geometry
is first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### raindrop

raindrop _xCenter_, _yCenter_, _radius_

Adds a raindrop shape to the current path, with center
point (_xCenter_, _yCenter_) and radius
_radius_.

The center point and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### reversepath

reversepath

Reverses the direction of the current path.

### rotate

rotate _angle_

Rotates the coordinate system, adding to any existing rotation.  _angle_
is given in degrees, measured counter-clockwise.  All later coordinates given
in \texttt{move}, \texttt{draw}, \texttt{arc} and \texttt{addpath} commands are
rotated by this angle.

### rdraw

rdraw _dx_, _dy_, ...

Adds one or more straight line segments to the current path
using relative distances.
A straight line segment is added from the previously defined point
a relative distance (_dx_, _dy_).  Each further
point adds a line segment relative to the point before.
Points are first transformed through any world coordinate
transformation set with a \texttt{worlds} command
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### roundedbox

roundedbox _x1_, _y1_, _x2_, _y2_

roundedbox _x1_, _y1_, _x2_, _y2_, _radius_

Adds a rectangle with rounded corners to the current path.
The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of the rectangle.

The radius of circular arcs at the rounded corners is 
_radius_, or 10\% of the size of the rectangle if not given.

The points and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### samplepath

samplepath _spacing_, _offset_

Replaces current path with equally spaced points along the path.
_offset_ is the distance along the path at which to place first point,
given in millimeters.  _spacing_ is the distance between points, given
in millimeters.  The sign of _spacing_ controls the direction in which
the path is travelled.  If _spacing_ is a positive value, the path is
travelled from the beginning towards the end.  If _spacing_ is a
negative value, then the absolute value of _spacing_ is used and the
path is travelled from the end towards the beginning.  Using a very large
positive or negative value for _spacing_ results in current path being
replaced by a single point at the beginning or end of the path.

### scale

scale _factor_

Scales the coordinate system, adding to any existing scaling.  _factor_
is scale factor for X and Y axes.  All later coordinates given in
\texttt{move}, \texttt{draw}, \texttt{arc} and \texttt{addpath} commands are
scaled by this factor.

### selectpath

selectpath _offset_, _length_ [, _offset_, _length_  ... ]

Selects one or more parts of the current path.

Each _offset_ is a distance along the path at which to begin selecting
the path, measured in millimeters.
_length_ is the length of path to select at that offset, measured
in millimeters.

Offsets and lengths are scaled by \texttt{scale} values but are independent of
any world coordinate transformation.

### setoutput

setoutput _filename_

Sets file that all \texttt{print} commands will be written to.
_filename_ is the name of a file to write to, overwriting any
existing file with this name.

### shiftpath

shiftpath _x_, _y_

Shifts all points in the current path _x_ millimeters along the X axis
and _y_ millimeters along the Y axis.  Shift values are scaled and
rotated by \texttt{scale} and \texttt{rotate} values but are independent
of any world coordinate transformation.

Use this command
repeatedly following a \texttt{clip "outside"} command to produce a shadow
effect for polygons, as shown in 
Section \ref{tutorialshadow} on page \pageref{tutorialshadow}.

### sinewave

sinewave _xEnd_, _yEnd_, _repeats_, _height_

Adds a a sine wave curve to the current path.  The curve begins at
the last point added to the path and ends at (_xEnd_, _yEnd_)

The sine wave is repeated _repeats_ number of times.

_height_ defines the height of the sine wave.
If _height_ is a negative number then a mirror image
of the sine wave is produced.

The end point and height are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### sinkhole

sinkhole

Replaces the current path containing a polygon with a single point in the
middle of the polygon, farthest from the polygon perimeter.

### spiral

spiral _xCenter_, _yCenter_, _radius_, _revolutions_, _startAngle_

Adds a spiral to the current path, with center
point (_xCenter_, _yCenter_) and given outer radius.

_revolutions_ defines the number of loops of the spiral.
If _revolutions_
is a positive number then the spiral is drawn in an anti-clockwise direction.
If _revolutions_ is a negative number then the spiral
is drawn in a clockwise direction.

_startAngle_ defines the
angle at which the outer revolution of the spiral starts.

The center point, radius and start angle are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### star

star _xCenter_, _yCenter_, _radius_, _points_

Adds a star shape to the current path, with center
point (_xCenter_, _yCenter_), distance
_radius_
from the center to each point of the star.
_points_ is the number of points for the star.

The center point and radius are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### stripepath

stripepath _spacing_, _angle_

Replaces current path with equally spaced parallel lines that completely cover
the path.  _spacing_ is the distance between lines, measured in
millimeters.  _angle_ is angle of each line, measured
counter-clockwise in degrees, with zero being horizontal.  Follow this command
with a \texttt{clip} command to produce a hatched fill pattern.

### stroke

stroke [_xml-attributes_]

Draws the current path using the current color and linestyle.
The current path is not modified by this command.

For SVG output, any XML attributes given in
_xml-attributes_ are
included in the \texttt{<path>} XML element for the drawn path.

### svg

svg _filename_, [, _size_]

Displays a Scalable Vector Graphics (SVG) file at each \texttt{move} point in
the current path.  

_filename_ is the name of an SVG file, with 
either an \texttt{.svg} or \texttt{.svgz} suffix.

_size_ is the optional size for the SVG file
in millimeters.  If no size
is given or size is zero then the file is displayed at its natural size,
as defined in the SVG file.
The file is scaled and rotated according to the current \texttt{scale}
and \texttt{rotate} settings.

SVG files can only be displayed when creating
SVG output.  For other formats,
a grey box is drawn showing where the SVG file
would be drawn.

### svgcode

svgcode _xml_

Adds XML code to the output file.

XML code can only be added to Scalable Vector Graphics (SVG) files.

This enables the default Mapyrus settings in SVG files to
be overridden and for graphics to be grouped together as layers.

### table

table _extras_, _column1_, _column2_ ...

Draws a table at each point in the path set with the \texttt{move} command,
One or more arrays are given defining values for each column of the table.
Values in each column array are displayed as one column in the table.

Labels in the table are drawn using the current color and font settings.

_extras_ defines further options for the table, given as
_variable=value_ values, separated by whitespace.
See Table \ref{tableextras}
for available options.

\begin{longtable}{|l|p{7cm}|}
\hline
\label{tableextras}
Extra & Description \\
\hline
\hline
\endfirsthead
\hline
\caption{Table Extras} \\
\endfoot

\hline
Extra & Description \\
\hline
\hline
\endhead

\texttt{background=_colors_} &

Comma-separated list of colors to use as background for entries
in the table, as named colors, hex digits or CMYK values in parentheses.
The colors are used in turn for each column in each row.
When the end of the list is reached, the list is repeated.
By default the background is not displayed. \\

\texttt{borders=_flag_} &

If _flag_ is \texttt{true} then a border is drawn around
each entry in the table using the current linestyle and color.
By default borders are drawn. \\

\texttt{justify=_justifications_} &

A comma-separated list of horizontal justification values for each column
in the table.  Each justification is one of
\texttt{left}, \texttt{right} or \texttt{center}. \\

\texttt{sortcolumn=_index_} &

Index of column to sort on, with first column having index 1.
Values in given column are sorted and all columns are displayed
in the order of the sorted column.
By default values are not sorted. \\

\texttt{sortorder=_order_} &

Ordering for sort column.  Either \texttt{asc} for ascending
order, or \texttt{desc} for descending order.  Default is ascending order. \\

\hline
\end{longtable}


### tree

tree _extras_, _entries_

Draws a tree of labels at each point in the path set with the
\texttt{move} command.
An array is given defining tree entries.
Each entry is split using the delimiter.  An entry starting
with the same values as a previous entry is indented to the
right with an arrow linking it to the previous entry.

Labels are drawn using the current color and font settings.

_extras_ defines further options for the tree, given as
_variable=value_ values, separated by whitespace.
See Table \ref{treeextras}
for available options.

\begin{longtable}{|l|p{7cm}|}
\hline
\label{treeextras}
Extra & Description \\
\hline
\hline
\endfirsthead
\hline
\caption{Tree Extras} \\
\endfoot

\hline
Extra & Description \\
\hline
\hline
\endhead

\texttt{delimiter=_string_} &

Delimiter used to determine indentation of labels.
By default whitespace is used as the delimiter. \\

\hline
\end{longtable}


### triangle

triangle _xCenter_, _yCenter_, _radius_, _rotation_

Adds an equilateral triangle to the current path, with center
point (_xCenter_, _yCenter_) and distance
_radius_
from the center point to each vertex.

The triangle is rotated clockwise _rotation_ degrees.

The center point, radius and rotation are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### unprotect

unprotect _x1_, _y1_, _x2_, _y2_

unprotect _geometry_

unprotect

Clears all protected regions from an area on the page.

The points
(_x1_, _y1_) and (_x2_, _y2_) define
any two opposite corners of the rectangle to clear.

If _geometry_ containing a polygon is given then the region
inside that polygon is cleared.

If no arguments are given then the region inside the current path is cleared.

The rectangle or geometry
is first transformed through any world coordinate
transformation set with a \texttt{worlds} command,
then scaled and rotated by \texttt{scale}
and \texttt{rotate} values.

### wedge

wedge _xCenter_, _yCenter_, _radius_, _angle_, _sweep_ [, _height_ ]

Adds a wedge (pie slice) shape to the current path, with center
point (_xCenter_, _yCenter_) and radius
_radius_.
The wedge begins at angle _angle_ measured
counter-clockwise in degrees, with zero being horizontal.
The wedge is open _sweep_ degrees in a counter-clockwise direction.
If _sweep_ is negative then the wedge opens in a clockwise direction.

If _height_ is given then the wedge is extended downwards
by this value to produce 3 dimensional effect.

The center point, radius and height are transformed through any
transformation set with a _worlds_ command,
then scaled and rotated by _scale_
and _rotate_ values.

### worlds

worlds _wx1_, _wy1_, _wx2_, _wy2_ [, _extras_ ]

worlds _wx1_, _wy1_, _wx2_, _wy2_, _px1_, _py1_, _px2_, _py2_ [, _extras_ ]

worlds _wx1,wy1,wx2,wy2_ [, _extras_ ]

Defines a world coordinate system for the page.

The coordinates
(_wx1_, _wy1_) 
and
(_wx2_, _wy2_)
define the lower-left and upper-right world coordinate values.

The coordinates
(_px1_, _py1_) 
and
(_px2_, _py2_)
define the lower-left and upper-right positions on the page
in millimetres.  The world coordinates are mapped into this area
of the page.  If page coordinates are not given then the world
coordinates are mapped to the whole page.

The new world coordinates replace any world coordinates
set with a previous \texttt{worlds} command.

A single comma-separated string with world coordinate values is
also accepted, as passed in the BBOX URL parameter in a OGC Web
Mapping Service (WMS) request.

_extras_ defines further options, given as
_variable=value_ values, separated by whitespace.
See the table below
for available options.

Extra | Description
----- | -----------
\texttt{units=_units_} | Defines the units of the world coordinates, either \texttt{metres}, \texttt{meters} or \texttt{feet}.  If not given, units are assumed to be meters.
\texttt{distortion=_flag_} | If \texttt{true} then non-uniform scaling in X and Y axes is allowed.  If \texttt{false} then the world coordinate range is expanded, if necessary, to maintain uniform scaling.  If not given, then scaling is uniform.

# Error Handling

If Mapyrus encounters an error when interpreting commands,
an error message is printed including the filename and line number
at which the error occurred and Mapyrus exits immediately.
The Java interpreter exits with a non-zero status.

If an error occurs when using the HTTP Server
option, an HTTP failure status and the error message
are returned to the HTTP client.  The HTTP server continues,
handling the next request.

## Mapyrus HTTP Server

Mapyrus runs as an HTTP server when started with the \texttt{-s} command line
option.

The HTTP server accepts and replies to requests from HTTP clients on the given
port number.  If port number is 0 then any free port number is used.  The port
number used is written to the log file or to standard output.

The HTTP server is multi-threaded to enable several requests to be handled
simultaneously.  If the HTTP server receives an HTTP request for a file with a
suffix matching a well-known MIME type (such as \texttt{html}, \texttt{txt},
\texttt{ps}, \texttt{pdf}, \texttt{svg}, \texttt{zip}
or a web image format), then the
contents of that file are returned by the HTTP server to the HTTP client.
Requests for files with no suffix, or with unknown file suffix
such as \texttt{.mapyrus} are interpreted
by Mapyrus using the following steps.

\begin{enumerate}
\item
Set any parameters passed in the URL (following the \texttt{?} character in
the URL or passed in an HTML form) as variables in Mapyrus.
Variable names are converted to uppercase.
\item
Read and execute the commands from the filename given in the URL.
\item
Capture the standard output of these commands and return it to the
HTTP client.
An image file is returned if the
\texttt{newpage} command is used with output file set
to standard output.
Otherwise the output of any \texttt{print} commands is returned.
The HTTP header information set in a
\texttt{mimetype}
or
\texttt{httpresponse}
command is returned to the HTTP client.

An HTTP error state is returned if the request fails.
\end{enumerate}

Requests using either GET or POST methods are accepted by Mapyrus.

The HTTP server runs forever and
is stateless.  Each HTTP request is independent and
variables, graphics state and legend entries are not shared between
requests.
Any files or URLs given on the command line when Mapyrus is started
are interpreted before accepting HTTP requests.
Procedures defined in these files
are available when interpreting HTTP requests.  This enables
common procedures to be loaded only once at startup and not with every
HTTP request.

Files are not cached and are read for each HTTP request.

For security, the HTTP server only replies to requests
from the directory in which Mapyrus was started and its subdirectories.
Requests for files from other directories return an error to the HTTP client.
If communication between HTTP client and Mapyrus is blocked for longer than
5 minutes then the HTTP request is cancelled.
When all threads in the HTTP server are busy handling requests,
further requests are queued.

Logging of HTTP requests is controlled by the \texttt{-l} command line option.

## Mapyrus Servlet

The file \texttt{mapyrus.war} provided with Mapyrus
is a web application archive.

After being deployed in a web server, the Mapyrus servlet handles HTTP
requests to the following URL.

```
http://localhost:8080/mapyrus/servlet
```

An example web page with an HTML form for the user
to enter Mapyrus commands, submit them to the Mapyrus servlet
and obtain the output is provided at the following URL.

```
http://localhost:8080/mapyrus
```

The commands to be run by Mapyrus servlet are passed in
the URL parameter \texttt{commands} in the HTTP request.

Any other parameters passed in the URL (following the \texttt{?} character
in the URL or passed in an HTML form) are set as variables in Mapyrus.
Variable names are converted to uppercase.

Normally, the Mapyrus commands in the HTTP request will be generated
programmatically or entered in an HTML form.  Mapyrus commands
passed programmatically must be converted to a form suitable for
a URL.  In Java, this is done using methods from
the Java class \texttt{java.net.URLEncoder}.

By default, Mapyrus servlet blocks access to files and URLs.
To enable access, set the servlet initialisation
parameter \texttt{io} to \texttt{true} in
the servlet configuration file \texttt{WEB-INF/web.xml}
contained in the web application file \texttt{mapyrus.war}.

The standard output of the Mapyrus commands is returned from
the HTTP request.  A
\texttt{mimetype}
or
\texttt{httpresponse}
command must be used to define the type of output being returned.

If a JNDI DataSource is setup in the web server to provide 
database connection pooling then database connections from the
pool are available using the \texttt{jndiname} option in
the \texttt{dataset} command.

If JTS Topology Suite functions or a JDBC driver are used then the
_JTS Topology Suite_ or JDBC JAR file must be made available
to the web server by copying it into a directory that the web server
includes in the Java classpath.

The Mapyrus servlet throws a Java ServletException if there is an error
in the Mapyrus commands, or an error running the Mapyrus commands.
