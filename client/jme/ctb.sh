#!/bin/sh

# build client versions between 2 revisions

if [ $# -gt 2 ] ; then

	REVS="$@"

else
	REV1=$1
	REV2=$2
	
	[ -z "$REV1" -o -z "$REV2" ] && echo "error: need 2 revisions" && exit 1
	
	REVS=`hg log -I . -r $REV1:$REV2 --template="{rev}\n"`
fi

echo "$REVS"

TBD=tests.build

rm -rf $TBD
mkdir $TBD

cat > $TBD/README << EOF
Client test builds between revisions $REV1 and $REV2.
EOF

REV0=`hg id -i`

I=0

for REV in $REVS; do

	ID=`hg id -i -r $REV`

	I=$((I+1))
	
	echo "-> Rev $REV ($ID)"

	hg up -C $REV

	ant dist >/dev/null 2>&1 || { echo "build failed"; }

	[ -e app/remuco.jar ] && mv app/remuco.jar $TBD/remuco-client-$I-$ID.jar
	[ -e dist/remuco.jar ] && mv dist/remuco.jar $TBD/remuco-client-$I-$ID.jar

done

echo "Update to revision before running this script.."

hg up -C $REV0

