#!/bin/sh

VERSION=$1

[ -z "$VERSION" ] && echo "need version" && exit 1

if [ -n "`svn st`" ] ; then
	echo
	echo "+-----------------------------------------------------------"
	echo "| WARNING: working copy has local changes"
	echo "+-----------------------------------------------------------"
	echo
	sleep 2
fi

# -----------------------------------------------------------------------------
# preparations
# -----------------------------------------------------------------------------

PKG_DEFAULT=remuco-$VERSION

rm -rf tarballs build dist $PKG_DEFAULT || exit 1

mkdir tarballs
mkdir $PKG_DEFAULT

# -----------------------------------------------------------------------------
# update api.html
# -----------------------------------------------------------------------------

cd base/module
pydoc -w remuco
cd ../..

mv base/module/remuco.html doc/api.html

sed -i doc/api.html -e "s,[_a-z\.]\+\.html,api.html,g"

# -----------------------------------------------------------------------------
# base, adpaters, doc and top level files
# -----------------------------------------------------------------------------

for ITEM in base adapter doc setup.py Makefile ; do
	cp -r $ITEM $PKG_DEFAULT/
done

# -----------------------------------------------------------------------------
# client
# -----------------------------------------------------------------------------

mkdir $PKG_DEFAULT/client

cd client

for ITEM in src res design *.example build.xml setup.sh libgen ; do
	cp -r $ITEM ../$PKG_DEFAULT/client/
done

mkdir ../$PKG_DEFAULT/client/app
ant dist
cp dist/remuco.jar dist/remuco.jad ../$PKG_DEFAULT/client/app

cd ..

# -----------------------------------------------------------------------------
# clean up and package
# -----------------------------------------------------------------------------

find $PKG_DEFAULT -type d -name ".svn" | xargs rm -rf
find $PKG_DEFAULT -type f -name "*.pyc" | xargs rm -f
find $PKG_DEFAULT -type f -name "*.pyo" | xargs rm -f
find $PKG_DEFAULT -type f -name "*~" | xargs rm -f
find $PKG_DEFAULT -type f -name "install*.log" | xargs rm -f

tar zcf tarballs/$PKG_DEFAULT.tar.gz $PKG_DEFAULT

# -----------------------------------------------------------------------------
# extra package: server source
# -----------------------------------------------------------------------------

PKG_SOURCE=remuco-source-$VERSION

rm -rf $PKG_SOURCE

cp -r $PKG_DEFAULT $PKG_SOURCE

rm -rf $PKG_SOURCE/client/app

tar zcf tarballs/$PKG_SOURCE.tar.gz $PKG_SOURCE

# -----------------------------------------------------------------------------

rm -rf $PKG_DEFAULT $PKG_SOURCE
