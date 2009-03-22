PREFIX ?= /usr/local

install:
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	install -T amarok.py $(DESTDIR)$(PREFIX)/bin/remuco-amarok
	
uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-amarok

clean: