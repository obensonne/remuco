
FILES := doc/* remuco-amarok.spec remuco-amarok.py
VERSION := 0.5.0
PKG_DIR := remuco-amarok-$(VERSION)

dist: clean
	mkdir $(PKG_DIR)
	install -m 644 $(FILES) $(PKG_DIR)/
	chmod 755 $(PKG_DIR)/remuco-amarok.py
	tar zcf $(PKG_DIR).amarokscript.tar.gz $(PKG_DIR)
	
clean:
	rm -rf $(PKG_DIR) $(PKG_DIR).amarokscript.tar.gz
	rm -f *.pyc

###############################################################################	
# DEBUG
###############################################################################	

valgrind:
	valgrind \
		--suppressions=../../bindings/python/valgrind.all-python-errors \
		--gen-suppressions=no \
		--tool=memcheck \
		--leak-check=full \
		./remuco-amarok.py
		
#		--suppressions=../../bindings/python/valgrind.python-errors \
#		--suppressions=../../bindings/python/valgrind.python-errors2 \
	