PREFIX ?= /usr

#X2_STARTUP_DIR ?= $(HOME)/.config/xmms2/startup.d
X2_STARTUP_DIR ?= $(PREFIX)/share/xmms2/scripts/startup.d

install:
	mkdir -p $(DESTDIR)$(X2_STARTUP_DIR)
	install -T xmms2.py $(DESTDIR)$(X2_STARTUP_DIR)/remuco-xmms2
	
uninstall:
	rm -f $(DESTDIR)$(X2_STARTUP_DIR)/remuco-xmms2

clean: