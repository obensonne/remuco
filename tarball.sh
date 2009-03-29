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

PKG=remuco-$VERSION

rm -rf build dist $PKG $PKG.tar.gz || exit 1

mkdir $PKG

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
	cp -r $ITEM $PKG/
done

# -----------------------------------------------------------------------------
# client
# -----------------------------------------------------------------------------

mkdir $PKG/client

cd client

ant dist

for ITEM in app src res design *.example build.xml ; do
	cp -r $ITEM ../$PKG/client/
done

cd ..

# -----------------------------------------------------------------------------
# clean up and package
# -----------------------------------------------------------------------------

find $PKG -type d -name ".svn" | xargs rm -rf
find $PKG -type f -name "*.pyc" | xargs rm -f
find $PKG -type f -name "*~" | xargs rm -f
find $PKG -type f -name "install*.log" | xargs rm -f


tar zcf $PKG.tar.gz $PKG

rm -rf $PKG
