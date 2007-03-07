###############################################################################
#
# PP XMMS
#
###############################################################################


RELEASE_PP := 0

USE_REM_TAG := yes

PP_CFLAGS := $(shell xmms-config --cflags)
PP_LFLAGS := $(shell xmms-config --libs)

server: prereqs pp/$(PP_NAME)/pp.c
	$(CC) -o remuco-$(PP_NAME) pp/$(PP_NAME)/pp.c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_DIR) $(DESTDIR)$(BIN_DIR)
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	
PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES :=