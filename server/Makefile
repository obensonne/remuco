###############################################################################
#
# Targets
#
###############################################################################

include Makefile.inc

all: check-pkg-config
	make -C src all
	make -C doc all

install: check-pkg-config
	make -C src install
	make -C doc install

uninstall:
	make -C src uninstall
	make -C doc uninstall
	
clean:
	make -C src clean
	make -C doc clean
	rm -rf build
	rm -rf dist
	
dist: clean
	mkdir -p build/$(PKG_FULL)
	cp -r Makefile src doc build/$(PKG_FULL)
	find build -type d -name ".svn" | xargs rm -rf    
	mkdir dist
	tar zcf dist/$(PKG_FULL).tar.gz -C build $(PKG_FULL)

check-pkg-config:
	@$(PKG_CONFIG) --version > /dev/null 2>&1 || \
		{ echo "missing $(PKG_CONFIG)"; false; }