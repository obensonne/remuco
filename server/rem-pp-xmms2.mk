###############################################################################
#
# PP XMMS2
#
###############################################################################

RELEASE_PP := 0

PP_CFLAGS := $(shell pkg-config --cflags xmms2-client)
PP_LFLAGS := $(shell pkg-config --libs xmms2-client)

server: prereqs rem-pp-$(PP_NAME).c
	$(CC) -o remuco-$(PP_NAME) rem-pp-$(PP_NAME).c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	
# Variables used for target 'dist' to create a binary distribution package

PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES :=
