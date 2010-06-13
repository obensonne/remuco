#!/bin/sh

# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
#
#    This file is part of Remuco.
#
#    Remuco is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Remuco is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
#
# =============================================================================

# Setup the client building environment.
#
# This script downloads ProGuard and MicroEmu, installs them in './tools'
# and creates/adjusts build.properties accordingly.

TAR_MICROEMU=microemulator-2.0.3.tar.gz
URL_MICROEMU=http://downloads.sourceforge.net/microemulator/$TAR_MICROEMU
DIR_MICROEMU=microemulator-2.0.3

TAR_PROGUARD=proguard4.3.tar.gz
URL_PROGUARD=http://downloads.sourceforge.net/proguard/$TAR_PROGUARD
DIR_PROGUARD=proguard4.3

cd "`dirname $0`"

mkdir -p tools/download

get_tool() {
	URL=$1
	TAR=$2
	NAME=$3
	if [ ! -f tools/download/$TAR ] ; then
		echo "----------------------------------------------------------"
		echo "+ Downloading $NAME ..."
		echo "----------------------------------------------------------"
		wget $URL -O tools/download/$TAR
		if [ $? != 0 ] ; then
			echo "----------------------------------------------------------"
			echo "+ Downloading $NAME failed."
			echo "+ Try to adjust the URL (for instance version number)."
			echo "----------------------------------------------------------"
			exit 1
		fi
	else
		echo "----------------------------------------------------------"
		echo "+ $NAME already downloaded."
		echo "----------------------------------------------------------"
	fi
	
	echo "----------------------------------------------------------"
	echo "+ Unpacking $NAME tarball ..."
	echo "----------------------------------------------------------"
	tar zxf tools/download/$TAR -C tools
	if [ $? != 0 ] ; then
		echo "----------------------------------------------------------"
		echo "+ Unpacking $NAME failed."
		echo "+ Maybe download failed and $TAR is not a TAR file?"
		echo "+ Try to adjust the URL (for instance version number)."
		echo "----------------------------------------------------------"
		exit 1
	fi
}

get_tool $URL_MICROEMU $TAR_MICROEMU "MicroEmu"
get_tool $URL_PROGUARD $TAR_PROGUARD "ProGuard"

echo "----------------------------------------------------------"
echo "+ Configure build properties ..."
echo "----------------------------------------------------------"

if [ ! -f build.properties ] ; then
	cp build.properties.example build.properties
fi

sed -i"" \
	-e "s,^proguard.jar=.*$,proguard.jar=\${basedir}/tools/$DIR_PROGUARD/lib/proguard.jar," \
	-e "s,^microemu.home=.*$,microemu.home=\${basedir}/tools/$DIR_MICROEMU," \
	build.properties

echo "----------------------------------------------------------"
echo "+ Done"
echo "+ "
echo "+ Run 'ant dist' to build the client."
echo "+ Run 'ant run' to test the client in an emulator."
echo "+ Run 'ant -p' to get a list of available targets."
ant -version > /dev/null 2>&1
if [ $? != 0 ] ; then
	echo "+ "
	echo "+ You still need to install Ant (could not start it)."
fi
echo "----------------------------------------------------------"

