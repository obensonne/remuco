RB_PLUGIN_DIR ?= $(HOME)/.gnome2/rhythmbox/plugins

RB_MODULE = remythm

install: uninstall
	mkdir -p $(RB_PLUGIN_DIR)/$(RB_MODULE)
	cp __init__.py remuco.rb-plugin $(RB_PLUGIN_DIR)/$(RB_MODULE)
	
uninstall:
	rm -rf $(RB_PLUGIN_DIR)/$(RB_MODULE)
