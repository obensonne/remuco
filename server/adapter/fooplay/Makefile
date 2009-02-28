PREFIX ?= /usr/local

install:
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	install -T fooplay.py $(DESTDIR)$(PREFIX)/bin/remuco-fooplay
	
uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-fooplay
