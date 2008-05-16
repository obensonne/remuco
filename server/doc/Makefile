###############################################################################
#
# Configuraion
#
###############################################################################

include ../Makefile.inc

MAN_1_FILES 	:= remuco.1 remuco-bpp.1 remuco-server.1
DOC_FILES		:= COPYING README README.html Server.conf.example changelog THANKS
DOC_FILES_GZ	:= COPYING README changelog THANKS

###############################################################################
#
# Standard Targets
#
##############################################################################

all: 

install: all
	# man pages
	$(INSTALL_DIR) $(DESTDIR)$(MAN_DIR)/man1
	$(INSTALL_DAT) $(MAN_1_FILES) $(DESTDIR)$(MAN_DIR)/man1
	@for MAN in $(MAN_1_FILES); do \
		sed -i $(DESTDIR)$(MAN_DIR)/man1/$$MAN -e "s,DOC_DIR,$(DOC_DIR_PKG)," ; \
		$(GZIP) $(DESTDIR)$(MAN_DIR)/man1/$$MAN ; \
	done
	# pkg doc
	$(INSTALL_DIR) $(DESTDIR)$(DOC_DIR_PKG)
	$(INSTALL_DIR) $(DESTDIR)$(DOC_DIR_PKG)/bpp-examples
	$(INSTALL_DAT) bpp-examples/*.example $(DESTDIR)$(DOC_DIR_PKG)/bpp-examples
	$(INSTALL_DAT) $(DOC_FILES) $(DESTDIR)$(DOC_DIR_PKG)
	@for DOC in $(DOC_FILES_GZ); do \
		$(GZIP) $(DESTDIR)$(DOC_DIR_PKG)/$$DOC ; \
	done
	
uninstall:
	# man pages
	@for MAN in $(MAN_1_FILES); do \
		rm -f $(DESTDIR)$(MAN_DIR)/man1/$$MAN.gz ; \
	done
	# pkg doc
	rm -rf $(DESTDIR)$(DOC_DIR_PKG)

clean:

