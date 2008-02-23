###############################################################################
#
# Configuration
#
###############################################################################

PREFIX ?= /usr

PKG_VERSION := $(shell python setup.py --version)
PKG_BASENAME := remuco-python
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

DEBUG ?= no
DO_LOG_NOISE ?= no

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements
	DEBUG=$(DEBUG) DO_LOG_NOISE=$(DO_LOG_NOISE) \
		python setup.py build

install:
	DEBUG=$(DEBUG) DO_LOG_NOISE=$(DO_LOG_NOISE) \
		python setup.py install --prefix=$(DESTDIR)$(PREFIX)

uninstall:
	#python setup.py uninstall --prefix=$(DESTDIR)$(PREFIX)
	@echo "Sorry, uninstall not possible"

dist: pydoc clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile setup.py build/$(PKG_FULLNAME)/
	cp -r src doc build/$(PKG_FULLNAME)/
	find build/$(PKG_FULLNAME) -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	python setup.py clean
	rm -rf build dist
	rm -f stamp-build-requirements

###############################################################################
#
# Special Targets
#
##############################################################################

stamp-build-requirements:
	@echo -n "Checking for remuco .. "
	@pkg-config --exists remuco > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	@echo -n "Checking for python .. "
	@python-config --prefix > /dev/null \
		&& echo "ok" || { echo "missing" ; false; }
	touch stamp-build-requirements

pydoc: all
	pydoc > /dev/null 2>&1 || false
	mkdir -p build/pydoc
	REMPYMOD=`find build -name remuco.so` && \
		PYTHONPATH=`dirname $$REMPYMOD` pydoc -w remuco
	mv -f remuco.html doc/api.html
	

valgrind:
	G_SLICE=always-malloc \
		valgrind --leak-check=yes \
		--gen-suppressions=no \
		--suppressions=valgrind-errors.suppress \
		./test.py

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
