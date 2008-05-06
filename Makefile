clean:
	make -C server clean
	make -C pp/rhythmbox clean
	make -C pp/xmms2 clean
	ant -f client/build.xml

dist:
	make -C server dist
	make -C pp/rhythmbox dist
	make -C pp/xmms2 dist
	ant -f client/build.xml dist

