###############################################################################
#
# PP XMMS
#
###############################################################################

RELEASE_PP := 0

PP_CFLAGS := $(shell xmms-config --cflags)
PP_LFLAGS := $(shell xmms-config --libs) $(TAG_LFLAGS)

server: .built-server .built-tag rem-pp-$(PP_NAME).c
	$(CC) -o remuco-$(PP_NAME) rem-pp-$(PP_NAME).c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	
PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES :=