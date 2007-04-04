###############################################################################
#
# PP Rhythmbox (a C PP)
#
###############################################################################

# release version of the PP relative to the main release (see var
# $(RELEASE_MAIN) in 'Makefile')
RELEASE_PP := 2

USE_REM_DBUS := yes

# PP specific compile and link flags
PP_CFLAGS := 
PP_LFLAGS := 

# Rhythmbox plugin dir (perhaps you have to change it!)
RB_PLUGIN_DIR := /usr/lib/rhythmbox/plugins

server: prereqs pp/$(PP_NAME)/pp.c
	$(CC) -o remuco-$(PP_NAME) pp/$(PP_NAME)/pp.c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_DIR) $(DESTDIR)$(BIN_DIR)
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	$(INSTALL_DIR) $(DESTDIR)$(RB_PLUGIN_DIR)/remuco
	cd pp/$(PP_NAME)/rb-plugin/src/remuco && \
		$(INSTALL_DATA) *.py remuco.rb-plugin $(DESTDIR)$(RB_PLUGIN_DIR)/remuco
	
# When calling target 'dist' the file remuco-$(PP_NAME) will be inclueded in
# the distribution packages per default. If further files are needed they must
# be specified here.
PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES :=
