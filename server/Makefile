###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= no

PKG_VERSION := 0.6.0
PKG_BASENAME := remuco-server
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

##############################################################################
#
# Standard Targets
#
##############################################################################

all: remuco.pc
	PREFIX=$(PREFIX) DEBUG=$(DEBUG) make -C src

remuco.pc:
	cp remuco.pc.in remuco.pc
	sed -i remuco.pc \
		-e "s,@PREFIX@,$(PREFIX)," \
		-e "s,@VERSION@,$(PKG_VERSION),"

install: all
	PREFIX=$(PREFIX) DEBUG=$(DEBUG) make -C src install
	install -d $(DESTDIR)$(PREFIX)/lib/pkgconfig
	install -m 644 remuco.pc $(DESTDIR)$(PREFIX)/lib/pkgconfig

uninstall:
	PREFIX=$(PREFIX) make -C src uninstall
	rm  -f $(DESTDIR)$(PREFIX)/lib/pkgconfig/remuco.pc

dist: clean doc
	mkdir -p build/$(PKG_FULLNAME)
	cp api.doxyfile Makefile remuco.pc.in build/$(PKG_FULLNAME)/
	cp -r doc src include build/$(PKG_FULLNAME)/
	find build/$(PKG_FULLNAME) -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	make -C src clean
	rm -rf build dist
	rm  -f remuco.pc svn.diff

##############################################################################
#
# Doc Targets
#
##############################################################################

doc: api

api:
	rm -f doc/api/*
	doxygen api.doxyfile

##############################################################################
#
# Package Building Targets
#
##############################################################################

pkg-version:
	@echo $(PKG_VERSION)
	
pkg-basename:
	@echo $(PKG_BASENAME)

pkg-fullname:
	@echo $(PKG_FULLNAME)
	
lib-soname:
	@make -C src soname
