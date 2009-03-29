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

PKG=remuco-client-$VERSION

rm -rf $PKG $PKG.tar.gz || exit 1

mkdir $PKG

# -----------------------------------------------------------------------------
# client
# -----------------------------------------------------------------------------

cd client

ant dist

cp -r app/* ../$PKG/

cd ..

cat > $PKG/README << EOF
Remuco client binaries.

Please visit http://remuco.sourceforge.net/index.php/Getting_Started for
installation and usage instructions.
EOF

# -----------------------------------------------------------------------------
# clean up and package
# -----------------------------------------------------------------------------

find $PKG -type d -name ".svn" | xargs rm -rf
find $PKG -type f -name "*~" | xargs rm -f


tar zcf $PKG.tar.gz $PKG

rm -rf $PKG

