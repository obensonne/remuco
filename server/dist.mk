# -----------------------------------------------------------------------------
# Make file for creating a distribution package
# -----------------------------------------------------------------------------

ADAPTERS := $(shell ls adapter)

VERSION := 0.8.0
PKG := remuco-$(VERSION)

clean:
	@for PA in $(ADAPTERS) ; do make -C adapter/$$PA clean ; done
	make -C base clean
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean pydoc
	#[ -z "`svn st`" ] || { echo "ERROR: working copy has local changes" ; exit 1 ; }
	mkdir -p build/$(PKG)
	cp -r base adapter build/$(PKG)
	cp Makefile api.html README build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	cd ../client ; ant dist
	mkdir build/$(PKG)/client
	mv ../client/build/remuco-client-0.8.0/* build/$(PKG)/client/
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

pydoc: api.html

api.html: base/module/remuco/*.py
	cd base/module; pydoc -w remuco
	mv base/module/remuco.html $@
	sed -i $@ -e "s,[_a-z\.]\+\.html,$@,g"
