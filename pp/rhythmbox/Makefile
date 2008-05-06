###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr

VERSION := 0.7.1
PKG := remuco-rhythmbox-$(VERSION)

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
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/lib/rhythmbox/plugins/remuco
	$(INSTALL_DAT) src/remuco/__init__.py src/remuco/remuco.rb-plugin \
		$(DESTDIR)$(PREFIX)/lib/rhythmbox/plugins/remuco

install-user:
	$(INSTALL_DIR) $(HOME)/.gnome2/rhythmbox/plugins/remuco
	$(INSTALL_DAT) src/remuco/__init__.py src/remuco/remuco.rb-plugin \
		$(HOME)/.gnome2/rhythmbox/plugins/remuco

uninstall:
	rm -rf $(DESTDIR)$(PREFIX)/lib/rhythmbox/plugins/remuco
	
uninstall-user:
	rm -rf $(HOME)/.gnome2/rhythmbox/plugins/remuco

clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean
	mkdir -p build/$(PKG)
	cp -r Makefile doc src build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)
	