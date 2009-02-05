#!/bin/sh

PLAYER="$1"

usage() {
	echo "Usage: install <PLAYER>"
	echo "<PLAYER> names one of the existent player directories."
}

[ -z "$PLAYER" ] && usage && exit 1

[ ! -d "$PLAYER" ] && usage && exit 1

CHECK=`python install-check.py`

if [ -n "$CHECK" ] ; then
	echo "CHECK FAILED: $CHECK"
	exit 1
fi

CHECK=`python "$PLAYER"/install-check.py`

if [ -z "$CHECK" ] ; then
	echo "CHECK OK"
else
	echo "CHECK FAILED: $CHECK"
	exit 1
fi

sh "$PLAYER"/install.sh

