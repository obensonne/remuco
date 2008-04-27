#!/bin/sh

. ./config

########################### CHANGE THIS ######################################

PKG=remuco-xmms2-0.7.0
PKG_DEB=remuco-xmms2_0.7.0.orig

##############################################################################

die() {
	echo "---- ERROR ----"
	exit 1
}

##############################################################################

rm -rf build

mkdir build

svn co $BRANCH/pp/xmms2 build/svn-co

make -C build/svn-co dist || die

cp build/svn-co/dist/$PKG.tar.gz build/  || die

tar zxf build/$PKG.tar.gz -C build || die

mv build/$PKG.tar.gz build/$PKG_DEB.tar.gz

cp -r deb.xmms2 build/$PKG/debian

cd build/$PKG && debuild || die

echo "---- DONE ----"


