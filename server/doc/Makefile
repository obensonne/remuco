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
	$(INSTALL_DAT) remuco.1 remuco-bpp.1 remuco-server.1 \
		$(DESTDIR)$(PREFIX)/share/man/man1
	sed -i $(DESTDIR)$(PREFIX)/share/man/man1/remuco.1 -e "s,PREFIX,$(PREFIX),"
	sed -i $(DESTDIR)$(PREFIX)/share/man/man1/remuco-bpp.1 -e "s,PREFIX,$(PREFIX),"
	sed -i $(DESTDIR)$(PREFIX)/share/man/man1/remuco-server.1 -e "s,PREFIX,$(PREFIX),"
	gzip -f9 $(DESTDIR)$(PREFIX)/share/man/man1/remuco.1
	gzip -f9 $(DESTDIR)$(PREFIX)/share/man/man1/remuco-bpp.1
	gzip -f9 $(DESTDIR)$(PREFIX)/share/man/man1/remuco-server.1
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	cp -r bpp-examples $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	$(INSTALL_DAT) COPYING README README.html Server.conf.example changelog \
		THANKS $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/COPYING
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/changelog
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/README
	gzip -f9 $(DESTDIR)$(PREFIX)/share/doc/remuco-server/THANKS
	
uninstall:
	rm -rf $(DESTDIR)$(PREFIX)/share/doc/remuco-server
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco.1.gz
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco-server.1.gz
	rm -f $(DESTDIR)$(PREFIX)/share/man/man1/remuco-bpp.1.gz

clean:

