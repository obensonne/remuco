#!/bin/sh

. ./config

########################### CHANGE THIS ######################################

PKG=remuco-server-0.7.0
PKG_DEB=remuco-server_0.7.0.orig

##############################################################################

die() {
	echo "---- ERROR ----"
	exit 1
}

##############################################################################

rm -rf build

mkdir build

svn co $BRANCH/server build/svn-co

make -C build/svn-co dist || die

cp build/svn-co/dist/$PKG.tar.gz build/  || die

tar zxf build/$PKG.tar.gz -C build || die

mv build/$PKG.tar.gz build/$PKG_DEB.tar.gz

cp -r deb.server build/$PKG/debian

cd build/$PKG && debuild -S || die

echo "---- DONE ----"


