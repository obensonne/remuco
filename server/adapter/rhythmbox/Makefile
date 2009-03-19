RB_PLUGIN_DIR ?= $(HOME)/.gnome2/rhythmbox/plugins

RB_MODULE = remythm

install: uninstall
	install -d $(DESTDIR)$(RB_PLUGIN_DIR)/$(RB_MODULE)
	install -m 644 __init__.py rba.py remuco.rb-plugin \
		$(DESTDIR)$(RB_PLUGIN_DIR)/$(RB_MODULE)
	
uninstall:
	rm -rf $(DESTDIR)$(RB_PLUGIN_DIR)/$(RB_MODULE)

clean: