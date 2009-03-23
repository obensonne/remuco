PREFIX ?= /usr/local

install:
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	install -T audacious.py $(DESTDIR)$(PREFIX)/bin/remuco-audacious
	
uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-audacious

clean: