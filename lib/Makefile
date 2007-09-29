###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= yes

PKG_VERSION := 0.6.0
PKG_BASENAME := libremuco
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

INSTALL := install
INSTALL_LIB := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL)
INSTALL_DIR := $(INSTALL) -d

##############################################################################
#
# Standard Targets
#
##############################################################################

all:
	PREFIX=$(PREFIX) DEBUG=$(DEBUG) make -C src

install: all
	PREFIX=$(PREFIX) DEBUG=$(DEBUG) make -C src install

uninstall:
	PREFIX=$(PREFIX) make -C src uninstall

dist: clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile build/$(PKG_FULLNAME)/
	cp -r doc src build/$(PKG_FULLNAME)/
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	make -C src clean
	rm -rf build dist



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