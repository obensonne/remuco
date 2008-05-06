###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr

###############################################################################
#
# Tools
#
###############################################################################

INSTALL := install
INSTALL_DAT := $(INSTALL) -m 644
INSTALL_DIR := $(INSTALL) -m 755 -d

###############################################################################
#
# Standard Targets
#
##############################################################################

all: 

install: all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/share/man/man1
	$(INSTALL_DAT) remuco.1 $(DESTDIR)$(PREFIX)/share/man/man1
	gzip -f9 $(DESTDIR)$(PREFIX)/share/man/man1/remuco.1
	ln -sf remuco.1.gz $(DESTDIR)$(PREFIX)/share/man/man1/remuco-server.1.gz
	ln -sf remuco.1.gz $(DESTDIR)$(PREFIX)/share/man/man1/remuco-bpp.1.gz
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	cp -r bpp-examples $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	$(INSTALL_DAT) COPYING README README.html Server.conf.example changelog \
		$(DESTDIR)$(PREFIX)/share/doc/remuco-server
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/COPYING
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/changelog
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/README
	
uninstall:
	rm -rf $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco.1.gz
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco-server.1.gz
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco-bpp.1.gz

clean:

