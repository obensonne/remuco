ADAPTERS := $(shell cd adapter; ls)

VERSION_BASE := 0.8
REVISION := $(shell svn info -R | grep "^Revision" | sed -e "s/^Revision: //" | sort -n | tail -n 1)
VERSION := $(VERSION_BASE).$(REVISION)
PKG := remuco-source-$(VERSION)


help:
	@echo
	@echo "Run 'make install-core' to install the Remuco python module."
	@echo
	@echo "Subsequent install one or more player adapters of your choice with:"
	@for PA in $(ADAPTERS); do echo "make install-$$PA"; done
	@echo
	@echo "To uninstall, do a 'make uninstall-...' for each component."
	@echo
	@echo "Of course, use 'sudo' where needed."
	@echo

all: help
	@echo -n ""

install: help
	@echo -n ""

uninstall: help
	@echo -n ""

install-core:
	make -C core install 
	@echo
	@echo "Installed Remuco module."
	@echo "You can now install a player adapter with one of the following:"
	@for PA in $(ADAPTERS); do echo "make install-$$PA"; done
	@echo

uninstall-core:
	make -C core uninstall
	@echo
	@echo "Player adapters should get uninstalled too, they won't work anymore."
	@echo

clean:
	make -C core clean
	@for PA in $(ADAPTERS); do make -C adapter/$$PA clean; done
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean pydoc
	#[ -z "`svn st`" ] || { echo "ERROR: working copy has local changes" ; exit 1 ; }
	mkdir -p build/$(PKG)
	cp -r test remuco $(ADAPTERS) build/$(PKG)
	cp Makefile pa-api.html README build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

pydoc: api.html

api.html: core/remuco/*.py
	cd core; pydoc -w remuco
	mv core/remuco.html $@
	sed -i $@ -e "s,[_a-z\.]\+\.html,$@,g"

check:
	@cd /; echo "import remuco" | python 2>/dev/null || \
		{ echo "ERROR: module remuco not yet installed (run 'make install' first)"; \
		exit 1; } \

install-%: check
	@[ -d adapter/$(subst install-,,$@) ] || { echo "no such adapter"; exit 1; }
	make -C adapter/$(subst install-,,$@) install

uninstall-%:
	@[ -d adapter/$(subst uninstall-,,$@) ] || { echo "bad player name"; exit 1; }
	make -C adapter/$(subst uninstall-,,$@) uninstall
	