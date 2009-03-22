PREFIX ?= /usr
RB_PLUGIN_DIR ?= $(PREFIX)/lib/rhythmbox/plugins
#RB_PLUGIN_DIR ?= $(HOME)/.gnome2/rhythmbox/plugins

install: uninstall
	install -d $(DESTDIR)$(RB_PLUGIN_DIR)/remuco
	install -m 644 remythm.py remuco.rb-plugin \
		$(DESTDIR)$(RB_PLUGIN_DIR)/remuco
	
uninstall:
	rm -rf $(DESTDIR)$(RB_PLUGIN_DIR)/remuco

clean: