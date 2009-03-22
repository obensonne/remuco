# -----------------------------------------------------------------------------
# Make file for creating a distribution package.
# -----------------------------------------------------------------------------

ADAPTERS := $(shell ls adapter)

VERSION := 0.8.0
PKG := remuco-$(VERSION)

clean:
	@for PA in $(ADAPTERS) ; do make -C adapter/$$PA clean ; done
	make -C base clean
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f
	cd client; ant clean

dist: clean pydoc
	#[ -z "`svn st`" ] || { echo "ERROR: working copy has local changes" ; exit 1 ; }
	
	mkdir -p build/$(PKG)
	cp -r base adapter doc Makefile build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	find build -type f -name "install.log" | xargs rm -f
	
	mkdir build/$(PKG)/client
	cd client ; ant dist.optimized
	cp client/build/jar/remuco.jar client/build/jar/remuco.jad \
		build/$(PKG)/client/
	mkdir build/$(PKG)/client/non-optimized
	cd client ; ant dist.chary
	cp client/build/jar/remuco.jar client/build/jar/remuco.jad \
		build/$(PKG)/client/non-optimized
		
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

pydoc: doc/api.html

doc/api.html: base/module/remuco/*.py
	cd base/module; pydoc -w remuco
	mv base/module/remuco.html $@
	sed -i $@ -e "s,[_a-z\.]\+\.html,api.html,g"
