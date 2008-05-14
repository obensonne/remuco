###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr

AMAROK_SCRIPT_DIR ?= /usr/share/amarok/scripts

VERSION := 0.7.1
PKG := remuco-amarok-$(VERSION)

###############################################################################
#
# Tools
#
###############################################################################

INSTALL := install
INSTALL_DAT := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL) -m 755
INSTALL_DIR := $(INSTALL) -m 755 -d

###############################################################################
#
# Targets
#
###############################################################################

all:

install: all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_DAT) src/remuco-amarok $(DESTDIR)$(PREFIX)/bin

uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-amarok
	
clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean
	mkdir -p build/$(PKG)
	cp -r Makefile doc src build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)
	