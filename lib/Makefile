###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= yes

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
	cp Makefile remuco.pc.in build/$(PKG_FULLNAME)/
	cp -r doc src include build/$(PKG_FULLNAME)/
	find build/$(PKG_FULLNAME)/ -type d -name ".svn" | \
		xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	make -C src clean
	rm -rf build dist
	rm  -f remuco.pc

##############################################################################
#
# Building debain packages (bin and dev) using alien
#
##############################################################################

DEB_SONAME := $(shell make -C src soname | grep "^[0-9]$$")

DEB_PKG_BASENAME := libremuco
DEB_PKG_BIN := $(DEB_PKG_BASENAME)$(DEB_SONAME)-$(PKG_VERSION)
DEB_PKG_DEV := $(DEB_PKG_BASENAME)-dev-$(PKG_VERSION)

DEB_DESTDIR := $$PWD/build/deb

DEB_EMAIL ?= mondai@users.sourceforge.net

deb: clean doc
	# install in a temp location
	mkdir -p build/deb
	DESTDIR=$(DEB_DESTDIR) make install
	# install doc files for dev package
	mkdir -p $(DEB_DESTDIR)$(PREFIX)/share/doc
	cp -r doc $(DEB_DESTDIR)$(PREFIX)/share/doc/$(DEB_PKG_BASENAME)-dev
	find $(DEB_DESTDIR)$(PREFIX)/share/doc/libremuco-dev \
		-type d -name ".svn" | xargs rm -rf
	# create tgz packages
	cd build/deb && \
		tar zcf $(DEB_PKG_BIN).tar.gz \
			.$(PREFIX)/lib/libremuco.so.* && \
		tar zcf $(DEB_PKG_DEV).tar.gz \
			.$(PREFIX)/lib/libremuco.so \
			.$(PREFIX)/lib/libremuco.a \
			.$(PREFIX)/lib/libremuco.la \
			.$(PREFIX)/include/remuco* \
			.$(PREFIX)/lib/pkgconfig/remuco.pc \
			.$(PREFIX)/share/doc/$(PKG_BASENAME)-dev
	# convert to debian package (with -g option)
	cd build/deb && \
		EMAIL=$(DEB_EMAIL) alien -d -g -k -T $(DEB_PKG_BIN).tar.gz && \
		EMAIL=$(DEB_EMAIL) alien -d -g -k -T $(DEB_PKG_DEV).tar.gz
	# copy some prepared debian files
	cp alien/control build/deb/$(DEB_PKG_BIN)/debian
	cp alien/copyright build/deb/$(DEB_PKG_BIN)/debian
	cp alien/control-dev build/deb/$(DEB_PKG_DEV)/debian/control
	cp alien/copyright build/deb/$(DEB_PKG_DEV)/debian
	# create packages
	cd  build/deb/$(DEB_PKG_BIN) && debian/rules binary
	cd  build/deb/$(DEB_PKG_DEV) && debian/rules binary
	mkdir -p dist
	cp build/deb/*.deb dist/

##############################################################################
#
# Doc Targets
#
##############################################################################

doc: api

api:
	rm -f doc/api/*
	doxygen doc.src/api.doxyfile

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
