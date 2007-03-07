###############################################################################
#
# PP XMMS2
#
###############################################################################

RELEASE_PP := 2

PP_CFLAGS := $(shell pkg-config --cflags xmms2-client)
# uncomment next line if you use the devel tree of xmms2
#PP_CFLAGS += -DREM_XMMS2_DEVEL
PP_LFLAGS := $(shell pkg-config --libs xmms2-client)

server: prereqs pp/$(PP_NAME)/pp.c
	$(CC) -o remuco-$(PP_NAME) pp/$(PP_NAME)/pp.c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_DIR) $(DESTDIR)$(BIN_DIR)
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	
# Variables used for target 'dist' to create a binary distribution package

PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES :=
