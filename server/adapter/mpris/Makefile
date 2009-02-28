PREFIX ?= /usr/local

install:
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	install -T mpris.py $(DESTDIR)$(PREFIX)/bin/remuco-mpris
	
uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-mpris

clean: