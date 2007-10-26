###############################################################################
#
# Configuration
#
###############################################################################

PREFIX ?= /usr

PKG_VERSION := $(shell python src/setup.py --version)
PKG_BASENAME := libremuco-python
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

###############################################################################
#
# Standard Targets
#
##############################################################################

all: clean .build-req
	cd src && python setup.py build

install:
	cd src && python setup.py install --prefix=$(DESTDIR)$(PREFIX)

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
	@echo -n "Checking for libremuco .. "
	@pkg-config --exists remuco > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	@echo -n "Checking for python-dev .. "
	@python-config --prefix > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	touch .build-req

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