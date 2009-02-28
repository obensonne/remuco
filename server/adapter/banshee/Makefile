PREFIX ?= /usr/local

install:
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	install -T banshee.py $(DESTDIR)$(PREFIX)/bin/remuco-banshee
	
uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-banshee
