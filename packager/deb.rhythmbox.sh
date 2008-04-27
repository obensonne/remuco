#!/bin/sh

. ./config

########################### CHANGE THIS ######################################

PKG=remuco-rhythmbox-0.7.0
PKG_DEB=remuco-rhythmbox_0.7.0.orig

##############################################################################

die() {
	echo "---- ERROR ----"
	exit 1
}

##############################################################################

rm -rf build

mkdir build

svn co $BRANCH/pp/rhythmbox build/svn-co

make -C build/svn-co dist || die

cp build/svn-co/dist/$PKG.tar.gz build/  || die

tar zxf build/$PKG.tar.gz -C build || die

mv build/$PKG.tar.gz build/$PKG_DEB.tar.gz

cp -r deb.rhythmbox build/$PKG/debian

cd build/$PKG && debuild || die

echo "---- DONE ----"


