ADAPTERS := fooplay rhythmbox xmms2

VERSION_BASE := 0.8
REVISION := $(shell svn info -R | grep "^Revision" | sed -e "s/^Revision: //" | sort -n | tail -n 1)
VERSION := $(VERSION_BASE).$(REVISION)
PKG := remuco-source-$(VERSION)


help:
	@echo "To install the Remuco module, run 'make install'."
	@echo "Subsequent install a player adapter with one of the following:"
	@for PA in $(ADAPTERS); do \
		echo "make install-$$PA"; \
		done

all: help
	@echo -n ""

install:
	python setup.py install --root=$(DESTDIR)/
	@echo
	@echo "Installed Remuco module."
	@echo "You can now install a player adapter with one of the following:"
	@for PA in $(ADAPTERS); do \
		echo "make install-$$PA"; \
		done
	@echo

uninstall:
	@echo "Please uninstall the Remuco module manually. Delete the files "
	@echo "listed in 'install.log'. To uninstall a player adapter, run one of"
	@echo "the following:"
	@for PA in $(ADAPTERS); do \
		echo "make uninstall-$$PA"; \
		done
	@echo

clean:
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

pydoc: pa-api.html

pa-api.html: remuco/__init__.py
	pydoc -w remuco
	mv remuco.html pa-api.html
	sed -i pa-api.html -e "s/remuco.html/pa-api.html/g"

check:
	@cd /; echo "import remuco" | python 2>/dev/null || \
		{ echo "ERROR: module remuco not yet installed (run 'make install' first)"; \
		exit 1; } \

install-%: check
	@[ -d $(subst install-,,$@) ] || { echo "bad player name"; exit 1; }
	make -C $(subst install-,,$@) install

uninstall-%:
	@[ -d $(subst uninstall-,,$@) ] || { echo "bad player name"; exit 1; }
	make -C $(subst uninstall-,,$@) uninstall
	