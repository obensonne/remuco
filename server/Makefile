# -----------------------------------------------------------------------------
# Makefile for user friendly installation of Remuco base and player adapters.
# Packagers should use the Makefiles in 'base' and 'adapter/XXX' directly.
# -----------------------------------------------------------------------------

default: help

# -----------------------------------------------------------------------------

ADAPTERS := $(shell cd adapter; ls)

VERSION := 0.8.0
PKG := remuco-$(VERSION)

clean:
	make -C base clean
	@for PA in $(ADAPTERS); do make -C adapter/$$PA clean; done
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f

dist: clean pydoc
	#[ -z "`svn st`" ] || { echo "ERROR: working copy has local changes" ; exit 1 ; }
	mkdir -p build/$(PKG)
	cp -r base adapters $(ADAPTERS) build/$(PKG)
	cp Makefile api.html README build/$(PKG)
	find build -type d -name ".svn" | xargs rm -rf
	mkdir dist
	cd ../client ; ant dist
	mkdir build/$(PKG)/client
	mv ../client/build/remuco-client-0.8.0/* client/
	tar zcf dist/$(PKG).tar.gz -C build $(PKG)

pydoc: api.html

api.html: base/module/remuco/*.py
	cd base/module; pydoc -w remuco
	mv base/module/remuco.html $@
	sed -i $@ -e "s,[_a-z\.]\+\.html,$@,g"

# -----------------------------------------------------------------------------

help:
	@echo
	@echo "Run one of the following to install Remuco for a specific player:"
	@for PA in $(ADAPTERS); do echo "make install-$$PA"; done
	@echo
	@echo "This automatically also installs the Remuco base which is used by"
	@echo "all player adapters. To uninstall, do a 'make uninstall-...'."
	@echo "The Remuco base can be uninstalled with 'make uninstall-base'."
	@echo
	@echo "Of course, use 'sudo' where needed."
	@echo

all: help
	true

install: help
	true

uninstall: help
	true

install-base:
	python base/module/install-check.py
	make -C base install 
	@echo
	@echo "+-----------------------------------------------------------------+"
	@echo "| Installed Remuco base."
	@echo "| Now install a player adapter with one of the following:"
	@for PA in $(ADAPTERS); do echo "| make install-$$PA"; done
	@echo "+-----------------------------------------------------------------+"
	@echo

uninstall-base:
	make -C base uninstall
	@echo
	@echo "+-----------------------------------------------------------------+"
	@echo "| Uninstalled Remuco base."
	@echo "| Player adapters should get uninstalled too, they won't work
	@echo "| anymore."
	@echo "+-----------------------------------------------------------------+"
	@echo

install-%:
	@[ -d adapter/$(subst install-,,$@) ] || { echo "no such adapter"; exit 1; }
	python base/module/install-check.py
	make -C base install
	@CHECK_FILE=adapter/$(subst install-,,$@)/install-check.py ; \
		if [ -e $$CHECK_FILE ] ; then python $$CHECK_FILE ; fi 
	make -C adapter/$(subst install-,,$@) install

uninstall-%:
	@[ -d adapter/$(subst uninstall-,,$@) ] || { echo "bad player name"; exit 1; }
	make -C adapter/$(subst uninstall-,,$@) uninstall

    