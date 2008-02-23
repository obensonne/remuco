#!/bin/bash

###############################################################################

function die {
	echo "${1:-error}"
	exit 1
}

###############################################################################


STANDARDS_VERSION="`apt-cache show debian-policy | grep Version | awk {' print $2 '}`"

. `dirname ${0}`/deb.config
. deb/config

MAINTAINER_EMAIL=${MAINTAINER_EMAIL:-not@all}

function initial {
	
	cd build/${PKG_FULLNAME} || die
	
	echo "now dh make"
	
	dh_make -e ${MAINTAINER_EMAIL} \
		-f ../../dist/${PKG_FULLNAME}.tar.gz \
		--copyright gpl \
		--library || die
	
	rm debian/README.Debian || die
	
	#cp ../../deb/{changelog,dirs,docs} debian
	
}

###############################################################################

make clean || die
	
PKG_VERSION=`make pkg-version` || die
PKG_BASENAME=`make pkg-basename` || die
PKG_FULLNAME=`make pkg-fullname` || die

make dist
	
rm -rf build && mkdir build || die

tar zxf dist/${PKG_FULLNAME}.tar.gz -C build || die

if [ "$1" == "init" ] ; then
	initial
	echo -e "\nOk, created initial debian files in build/${PKG_FULLNAME}/debian!\n"
	exit
fi

cp dist/${PKG_FULLNAME}.tar.gz build/${PKG_BASENAME}_${PKG_VERSION}.orig.tar.gz

cp -r debian build/${PKG_FULLNAME}/debian

rm -rf build/${PKG_FULLNAME}/debian/.svn

cd build/${PKG_FULLNAME}

debuild
#dpkg-buildpackage -rfakeroot -k${MAINTAINER_KEYID}  || die

mkdir -p ../../dist
cp ../*.deb ../../dist

echo "ok"
