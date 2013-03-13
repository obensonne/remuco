# -----------------------------------------------------------------------------
# Makefile intended for end users. It is a wrapper around setup.py.
# -----------------------------------------------------------------------------

#PREFIX ?= /usr/local
#SETUP := python2 setup.py install --prefix=$(PREFIX)

PYTHON := $(shell command -v python2 >/dev/null 2>&1 && echo "python2" || echo "python")
SETUP := $(PYTHON) setup.py install

ADAPTERS := $(shell ls adapter)

help:
	@echo
	@echo "To install a player adapter (and required base components), run:"
	@for PA in $(ADAPTERS); do echo "    make install-$$PA"; done
	@echo
	@echo "To uninstall a player adapter, run:"
	@for PA in $(ADAPTERS); do echo "    make uninstall-$$PA"; done
	@echo
	@echo "To uninstall all components (base and player adapters), run:"
	@echo "    make uninstall-all"
	@echo
	@echo "Of course, be root or use 'sudo' when needed."
	@echo

all: help
	@true

install: help
	@true

uninstall: help
	@true

install-base: clean
	$(PYTHON) base/module/install-check.py
	REMUCO_COMPONENTS="" $(SETUP) --record install-base.log
	@echo "+-----------------------------------------------------------------+"
	@echo "| Installed Remuco base."
	@echo "+-----------------------------------------------------------------+"

install-%: install-base
	@IC=adapter/$(subst install-,,$@)/install-check.py ; \
		[ ! -e $$IC ] || $(PYTHON) $$IC
	REMUCO_COMPONENTS=$(subst install-,,$@) $(SETUP) --record install-tmp.log
	diff --suppress-common-lines -n install-base.log install-tmp.log \
		| grep "^/" > install-$(subst install-,,$@).log
	rm install-tmp.log
	@echo "+-----------------------------------------------------------------+"
	@echo "| Installed player adapter '$(subst install-,,$@)'."
	@[ ! -e adapter/$(subst install-,,$@)/.wip ] || \
		echo "| WARNING: This adapter is still work in progress!"
	@echo "+-----------------------------------------------------------------+"

uninstall-all: $(addprefix uninstall-,$(ADAPTERS)) uninstall-base
	@echo "+-----------------------------------------------------------------+"
	@echo "| Uninstalled all components."
	@echo "+-----------------------------------------------------------------+"

uninstall-%:
	@PA='$(subst uninstall-,,$@)'; \
	if [ -e install-$$PA.log ] ; then \
		cat install-$$PA.log | xargs rm -f || exit 1; \
		rm install-$$PA.log ; \
		echo "+-----------------------------------------------------------------+" ; \
		echo "| Uninstalled component '$$PA'." ; \
		echo "+-----------------------------------------------------------------+" ; \
	else \
		echo "+-----------------------------------------------------------------+" ; \
		echo "| Skipped component '$$PA' (install log does not exist)" ; \
		echo "+-----------------------------------------------------------------+" ; \
	fi

clean:
	$(PYTHON) setup.py clean --all
	@echo "+-----------------------------------------------------------------+"
	@echo "| Clean ok (keep install log files for uninsallation)."
	@echo "+-----------------------------------------------------------------+"
