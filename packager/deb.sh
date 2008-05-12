#!/bin/sh

########################### CHANGE THIS ######################################


[ -z "$1" ] && echo "Missing component name" && exit

CONF=deb.$1.conf

[ ! -e $CONF ] && echo "Missing file $CONF" && exit 1

##############################################################################

die() {
	echo "---- ERROR ----"
	exit 1
}

##############################################################################

. ./$CONF

rm -rf build

mkdir build

svn co $URL build/svn-co || die

if [ "$1" = "client" ] ; then
	ant -f build/svn-co/build.xml dist.bin || die
else
	make -C build/svn-co dist || die
fi

cp build/svn-co/dist/$PKG.tar.gz build/  || die

tar zxf build/$PKG.tar.gz -C build || die

mv build/$PKG.tar.gz build/$PKG_DEB.tar.gz

cp -r deb.$1 build/$PKG/debian

rm -rf build/$PKG/debian/.svn

cd build/$PKG && debuild -S || die

echo "---- DONE ----"


