###############################################################################
#
# Targets
#
###############################################################################

PREFIX ?= /usr
DEBUG := no

VERSION := 0.7.0
PKG := remuco-server-$(VERSION)

all: check-pkg-config
	make -C src all
	make -C doc all

install: check-pkg-config
	make -C src install
	make -C doc install

uninstall:
	make -C src uninstall
	make -C doc uninstall
	
clean:
	make -C src clean
	make -C doc clean
	rm -rf build
	rm -rf dist
	
dist: clean
	mkdir -p build/$(PKG)
	cp -r Makefile src doc build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf    
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

check-pkg-config:
	@pkg-config --version > /dev/null 2>&1 || \
		{ echo "missing pkg-config, please install pkg-config"; false; }