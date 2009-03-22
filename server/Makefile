# -----------------------------------------------------------------------------
# Makefile intended for end users.
# Packagers should use the setup.py script in ./base/module and the make files
# in './adapter/XXX' directly.
# -----------------------------------------------------------------------------

ADAPTERS := $(shell ls adapter)

help:
	@echo
	@echo "Install a specific player adapter (also installs Remuco base,"
	@echo "which is needed for all player adpaters):"
	@for PA in $(ADAPTERS); do echo "    make install-$$PA"; done
	@echo
	@echo "Uninstall a specific player adapter:"
	@for PA in $(ADAPTERS); do echo "    make uninstall-$$PA"; done
	@echo ""
	@echo "Uninstall all Remuco components (base and all player adapters):"
	@echo "    make uninstall-all"
	@echo
	@echo "Of course, use 'sudo' when needed."
	@echo

all: help
	@true

install: help
	@true

uninstall: help
	@true

install-%:
	@[ -d adapter/$(subst install-,,$@) ] || { echo "no such adapter"; exit 1; }
	python base/module/install-check.py
	make -C base install
	@echo "+-----------------------------------------------------------------+"
	@echo "| Installed Remuco base."
	@echo "+-----------------------------------------------------------------+"
	@CHECK_FILE=adapter/$(subst install-,,$@)/install-check.py ; \
		if [ -e $$CHECK_FILE ] ; then python $$CHECK_FILE ; fi 
	make -C adapter/$(subst install-,,$@) install
	@echo "+-----------------------------------------------------------------+"
	@echo "| Installed player adapter '$(subst install-,,$@)'."
	@echo "+-----------------------------------------------------------------+"

uninstall-all:
	@for PA in $(ADAPTERS); do make uninstall-$$PA; done
	make -C base uninstall
	@echo "+-----------------------------------------------------------------+"
	@echo "| Unnstalled Remuco base."
	@echo "+-----------------------------------------------------------------+"

uninstall-%:
	@[ -d adapter/$(subst uninstall-,,$@) ] || { echo "bad player name"; exit 1; }
	make -C adapter/$(subst uninstall-,,$@) uninstall
	@echo "+-----------------------------------------------------------------+"
	@echo "| Uninstalled player adapter '$(subst uninstall-,,$@)'."
	@echo "+-----------------------------------------------------------------+"

clean:
	for PA in $(ADAPTERS); do make -C adapter/$$PA clean; done
	make -C base clean
	find -type f -name "*.pyc" | xargs rm -f
	@echo "+-----------------------------------------------------------------+"
	@echo "| Clean ok."
	@echo "+-----------------------------------------------------------------+"
