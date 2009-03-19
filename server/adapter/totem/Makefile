PREFIX ?= /usr

TOTEM_PLUGIN_DIR ?= $(PREFIX)/lib/totem/plugins

install: uninstall
	mkdir -p $(DESTDIR)$(TOTEM_PLUGIN_DIR)/remuco
	cp remotem.py remuco.totem-plugin $(DESTDIR)$(TOTEM_PLUGIN_DIR)/remuco
	
uninstall:
	rm -rf $(DESTDIR)$(TOTEM_PLUGIN_DIR)/$(RB_MODULE)/remuco

clean: