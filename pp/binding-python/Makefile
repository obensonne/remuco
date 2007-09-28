###############################################################################
#
# Configuraion
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

all: check-build-requirements
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
	rm -rf build dist
	rm -f *.deb

###############################################################################
#
# Special Targets
#
##############################################################################

check-build-requirements:
	@echo -n "Checking for libremuco .. "
	@pkg-config --exists remuco > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	@echo -n "Checking for python-dev .. "
	@python-config --prefix > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }

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