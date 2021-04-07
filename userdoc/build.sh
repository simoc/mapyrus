#!/bin/sh
#
# Build documentation and examples (requires LaTeX)
#

# Delete documentation generated previously
rm mapyrus.pdf 2> /dev/null
rm *.dvi *.aux *.log 2> /dev/null
rm tutorialfirst*-no-newpage.mapyrus tutorialfirst4-pdf.mapyrus 2> /dev/null

# We need additional JAR files in classpath for examples using
# 'JTS  Topology Suite' or PROJ.4 library,
CLASSPATH=../target/mapyrus-1.0-SNAPSHOT.jar:../javaproj-1.0.9.jar:../jts-1.14.jar
export CLASSPATH

java org.mapyrus.Mapyrus -e "print Mapyrus.version" > version.txt
date "+%Y-%m-%d" > timestamp.txt

# Generate all examples, doing tutorial examples separately so
# it is not necessary to show path being cleared in each example
java org.mapyrus.Mapyrus turtle1.mapyrus turtle2.mapyrus turtle3.mapyrus turtle4.mapyrus turtle5.mapyrus turtle6.mapyrus turtle7.mapyrus mapview1.mapyrus mapview2.mapyrus mapview3.mapyrus mapview4.mapyrus mapview5.mapyrus

java org.mapyrus.Mapyrus turtle1.mapyrus turtle2.mapyrus turtle3.mapyrus turtle4.mapyrus turtle5.mapyrus turtle6.mapyrus turtle7.mapyrus mapview1.mapyrus mapview2.mapyrus mapview3.mapyrus mapview4.mapyrus mapview5.mapyrus

java org.mapyrus.Mapyrus tutorialfirst1.mapyrus

# Make a copy of the most basic examples without the NEWPAGE
# command so the user does not have to worry about it
grep -v newpage tutorialfirst1.mapyrus > tutorialfirst1-no-newpage.mapyrus

java org.mapyrus.Mapyrus tutorialfirst2.mapyrus

java org.mapyrus.Mapyrus tutorialfirst3.mapyrus

java org.mapyrus.Mapyrus tutorialfirst4.mapyrus

java org.mapyrus.Mapyrus tutorialvar1.mapyrus tutorialvar2.mapyrus

java org.mapyrus.Mapyrus tutoriallines1.mapyrus tutoriallines2.mapyrus tutoriallines3.mapyrus tutoriallines4.mapyrus tutoriallines5.mapyrus tutoriallines6.mapyrus tutoriallines7.mapyrus tutoriallines8.mapyrus tutoriallines9.mapyrus tutoriallines10.mapyrus tutorialpolygons1.mapyrus tutorialpolygons2.mapyrus tutorialpolygons3.mapyrus tutorialpolygons4.mapyrus tutorialgradient.mapyrus tutorialprocedures1.mapyrus tutorialprocedures2.mapyrus tutoriallabels1.mapyrus tutoriallabels2.mapyrus tutoriallabels3.mapyrus tutorialflowlabel1.mapyrus tutorialflowlabel2.mapyrus tutorialsinkhole.mapyrus tutorialdatasets1.mapyrus tutorialdatasets2.mapyrus tutorialdatasets3.mapyrus tutorialdatasets4.mapyrus tutorialgeoimage1.mapyrus tutorialgeoimage2.mapyrus tutoriallegend1.mapyrus tutoriallegend2.mapyrus tutoriallegend3.mapyrus tutorialattribute1.mapyrus tutorialattribute2.mapyrus tutorialscalebar1.mapyrus tutorialtrans1.mapyrus tutorialtrans2.mapyrus tutorialcolor1.mapyrus tutorialshadow1.mapyrus tutorialwordwrap1.mapyrus tutorialformatting1.mapyrus tutorialtable1.mapyrus tutorialtable2.mapyrus tutorialicon1.mapyrus tutorialicon2.mapyrus tutorialicon3.mapyrus tutorialeps1.mapyrus

# Copy file used to demonstrate file updating so we don't modify original
cp tutorialdatasets1.eps tutorialexisting1.eps

java org.mapyrus.Mapyrus tutorialexisting1.mapyrus

java org.mapyrus.Mapyrus tutorialrand1.mapyrus tutorialrand2.mapyrus tutorialrand3.mapyrus tutorialpiechart1.mapyrus tutorialprotect1.mapyrus tutorialprotect2.mapyrus tutorialturnpage1.mapyrus tutoriallayout1.mapyrus

java org.mapyrus.Mapyrus tutorialjts1.mapyrus tutorialjts2.mapyrus tutorialjts3.mapyrus

java org.mapyrus.Mapyrus tutorialjts1.mapyrus tutorialjts2.mapyrus tutorialjts3.mapyrus

java org.mapyrus.Mapyrus tutorialproj1.mapyrus

# Create PostScript file showing legend for all sample symbols
java -Dshow_symbols_legend=1 org.mapyrus.Mapyrus symbols.mapyrus

# Build LaTex documentation into PDF file
# Run LaTeX three times so that all cross references are resolved.
latex mapyrus.tex

latex mapyrus.tex

latex mapyrus.tex

dvipdf mapyrus.dvi mapyrus.pdf
