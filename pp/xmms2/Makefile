###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr

VERSION := 0.7.2
PKG := remuco-xmms2-$(VERSION)

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
	$(INSTALL_EXE) src/remuco-xmms2 $(DESTDIR)$(PREFIX)/bin

install-user:
	$(INSTALL_DIR) $(HOME)/.config/xmms2/startup.d
	$(INSTALL_EXE) src/remuco-xmms2 $(HOME)/.config/xmms2/startup.d

uninstall:
	rm -rf $(DESTDIR)$(PREFIX)/bin/remuco-xmms2
	
uninstall-user:
	rm -rf $(HOME)/.config/xmms2/startup.d/remuco-xmms2

clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean
	mkdir -p build/$(PKG)
	cp -r Makefile doc src build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)
	