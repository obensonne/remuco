ADAPTERS := fooplay rhythmbox

VERSION_BASE := 0.8

help:
	@echo "This make file is only used for building distribution packages."
	@echo "To install a player, run the script install.sh:"
	@echo "\$$ ./install.sh fooplay|rhythmbox|..."

all: help
	@echo -n ""

install: help
	@echo -n ""

clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

REVISION := $(shell svn info -R | grep "^Revision" | sed -e "s/^Revision: //" | sort -n | tail -n 1)
VERSION := $(VERSION_BASE).$(REVISION)
PKG := remuco-source-$(VERSION)

dist: pydoc clean
	#[ -z "`svn st`" ] || { echo "ERROR: working copy has local changes" ; exit 1 ; }
	mkdir -p build/$(PKG)
	cp -r doc test remuco $(ADAPTERS) build/$(PKG)
	cp install.sh Makefile build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

pydoc: doc/player-adapter-api.html

doc/player-adapter-api.html: remuco/__init__.py
	pydoc -w remuco
	mv remuco.html doc/player-adapter-api.html

	

	