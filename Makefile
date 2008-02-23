clean:
	make -C server clean
	make -C pp clean
	ant -f client/build.xml

dist:
	make -C server dist
	make -C pp dist
	ant -f client/build.xml dist

