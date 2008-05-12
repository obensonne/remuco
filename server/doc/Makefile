###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr

DOC_DIR ?= $(PREFIX)/share/doc/remuco-server
MAN_DIR ?= $(PREFIX)/share/man

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
		$(DESTDIR)$(MAN_DIR)/man1
	sed -i $(DESTDIR)$(MAN_DIR)/man1/remuco.1 -e "s,DOC_DIR,$(DOC_DIR),"
	gzip -f9 $(DESTDIR)$(MAN_DIR)/man1/remuco.1
	gzip -f9 $(DESTDIR)$(MAN_DIR)/man1/remuco-bpp.1
	gzip -f9 $(DESTDIR)$(MAN_DIR)/man1/remuco-server.1
	$(INSTALL_DIR) $(DESTDIR)$(DOC_DIR)
	cp -r bpp-examples $(DESTDIR)$(DOC_DIR)
	$(INSTALL_DAT) COPYING README README.html Server.conf.example changelog \
		THANKS $(DESTDIR)$(DOC_DIR)
	gzip -f9 $(DESTDIR)$(DOC_DIR)/COPYING
	gzip -f9 $(DESTDIR)$(DOC_DIR)/changelog
	gzip -f9 $(DESTDIR)$(DOC_DIR)/README
	gzip -f9 $(DESTDIR)$(DOC_DIR)/THANKS
	
uninstall:
	rm -rf $(DESTDIR)$(DOC_DIR)
	rm -f $(DESTDIR)$(MAN_DIR)/man1/remuco.1.gz
	rm -f $(DESTDIR)$(MAN_DIR)/man1/remuco-server.1.gz
	rm -f $(DESTDIR)$(MAN_DIR)/man1/remuco-bpp.1.gz

clean:

