#!/bin/sh

make -C lib clean
make -C pp clean
ant -f client/build.xml clean

