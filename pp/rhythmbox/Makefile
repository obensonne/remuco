###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr

PKG_VERSION := 0.6.0
PKG_BASENAME := remuco-rhythmbox
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

RB_PLUGIN_DIR := $(HOME)/.gnome2/rhythmbox/plugins

###############################################################################
#
# Standard Targets
#
##############################################################################

all:
	true

install:
	cp -r src/remuco-rhythmbox $(RB_PLUGIN_DIR)

uninstall:
	rm -rf $(RB_PLUGIN_DIR)/remuco-rhythmbox

dist: clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile build/$(PKG_FULLNAME)/
	cp -r src doc build/$(PKG_FULLNAME)/
	find build/$(PKG_FULLNAME) -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	rm -rf build dist
	rm -f *.deb
	find -type f -name "*.pyc" | xargs rm -f

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