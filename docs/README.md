# Mapyrus

http://www.mapyrus.org

Mapyrus is software for
creating plots of points, lines, polygons and labels
to PostScript (high resolution, up to A0 paper size),
Portable Document Format (PDF),
Scalable Vector Graphics (SVG) format
and web image output formats.

Mapyrus is open source software and is implemented entirely in Java
enabling it to run on a wide range of operating systems.

The software combines the following three features.

## Logo Or Turtle Graphics

An imaginary pen is moved around a page,
creating shapes that are drawn into an image file.
Reusable routines are built up using a BASIC-like language.
Branching and looping constructs enable complex shapes, symbols, patterns
and graphs to be be defined.

[turtle1](turtle1.png)
[turtle2](turtle2.png)
[turtle3](turtle3.png)
[turtle4](turtle4.png)
[turtle5](turtle5.png)
[turtle6](turtle6.png)

## Data Display

Reading and displaying of geographic information
system (GIS) datasets,
text files, or tables held in a relational database
(including spatially extended databases such as Oracle Spatial,
PostGIS and MySQL).

Drawing routines are applied to geographic data to produce annotated and
symbolized maps and graphs.  Attributes of the geographic data control
the color, size, annotation and other characteristics of the
appearance of the geographic data.
Scalebars, legends, coordinate grids and north arrows are also available.

[mapview1](mapview1.png)
[mapview1](mapview3.png)
[mapview1](mapview5.png)

[mapview2](mapview2.png)
[mapview2legend](mapview2legend.png)
[mapview2scalebar](mapview2scalebar.png)
[mapview2north](mapview2north.png)

[mapview4](mapview4.png)

## Geometry Algorithms

Integration with the freely-available
[JTS Topology Suite](http://www.tsusiatsoftware.net/jts/main.html).
This library provides geometric algorithms
such as buffering, point-in-polygon test and polygon intersection.

Integration with the freely-available
[JH Labs](http://www.jhlabs.com/java/maps/proj)
Java port of the PROJ.4 projection library.

Further examples are included in the [tutorial](tutorial.md).

The [reference](reference.md) describes how Mapyrus works in detail.

Changes in each released version are listed in [NEWS](NEWS.md).
