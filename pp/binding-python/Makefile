###############################################################################
#
# Configuration
#
###############################################################################

PREFIX ?= /usr

PKG_VERSION := $(shell python src/setup.py --version)
PKG_BASENAME := libremuco-python
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

DEBUG ?= yes

###############################################################################
#
# Standard Targets
#
##############################################################################

all: .build-req
	cd src && DEBUG=$(DEBUG) python setup.py build

install:
	cd src && DEBUG=$(DEBUG)  \
		python setup.py install --prefix=$(DESTDIR)$(PREFIX)

uninstall:
	#python setup.py uninstall --prefix=$(DESTDIR)$(PREFIX)
	@echo "Sorry, uninstall not possible"

dist: clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile build/$(PKG_FULLNAME)/
	cp -r src doc build/$(PKG_FULLNAME)/
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	#cd src && python setup.py clean
	rm -rf build dist src/build
	rm -f *.deb
	rm -f .build-req

###############################################################################
#
# Special Targets
#
##############################################################################

.build-req:
	@echo -n "Checking for remuco .. "
	@pkg-config --exists remuco > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	@echo -n "Checking for python .. "
	@python-config --prefix > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	touch .build-req

valgrind:
	G_SLICE=always-malloc \
		valgrind --leak-check=yes \
		--gen-suppressions=no \
		--suppressions=valgrind-errors.suppress \
		./test.py

##############################################################################
#
# Building debain packages (bin and dev) using alien
#
##############################################################################

DEB_PKG_BASENAME := python-remuco
DEB_PKG := $(DEB_PKG_BASENAME)-$(PKG_VERSION)

DEB_BUILDDIR := $(CURDIR)/build/deb

DEB_DESTDIR := $(DEB_BUILDDIR)/fsroot
DEB_PREFIX := /usr

DEB_EMAIL ?= mondai@users.sourceforge.net

deb: clean doc
	mkdir -p $(DEB_BUILDDIR)
	mkdir -p $(DEB_DESTDIR)
	# install in a temp location
	DESTDIR=$(DEB_DESTDIR) PREFIX=$(DEB_PREFIX) DEBUG=no make install
	# install doc files
	mkdir -p $(DEB_DESTDIR)$(PREFIX)/share/doc
	cp -r doc $(DEB_DESTDIR)$(PREFIX)/share/doc/$(DEB_PKG_BASENAME)
	find $(DEB_DESTDIR)$(PREFIX)/share/doc/$(DEB_PKG_BASENAME) \
		-type d -name ".svn" | xargs rm -rf 
	# next is for lintian
	cd $(DEB_DESTDIR)$(PREFIX)/share/doc/$(DEB_PKG_BASENAME) && \
		mv ChangeLog changelog && gzip --best changelog && \
		rm -f COPYING
	# create tgz packages
	tar zcf $(DEB_BUILDDIR)/$(DEB_PKG).tar.gz -C $(DEB_DESTDIR) \
		.$(PREFIX)
	# convert to debian package (with -g option)
	cd $(DEB_BUILDDIR) && \
		EMAIL=$(DEB_EMAIL) alien -d -g -k -T $(DEB_PKG).tar.gz
	sed -i $(DEB_BUILDDIR)/$(DEB_PKG)/debian/rules \
		-e "s/#\(\t*dh_strip\) *$$/\1/"
	# copy some prepared debian files
	cp alien/control $(DEB_BUILDDIR)/$(DEB_PKG)/debian
	cp alien/copyright $(DEB_BUILDDIR)/$(DEB_PKG)/debian
	# create package
	cd $(DEB_BUILDDIR)/$(DEB_PKG) && debian/rules binary
	mkdir -p dist
	cp $(DEB_BUILDDIR)/*.deb dist

###############################################################################
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
