PP :=

# DO NOT CHANGE THIS. !!!
# If you would like to use other values here, download the source package and
# recompile with adjusted values.
# This unflexibility may be dropped in later releases of Remuco.

PREFIX	:= /usr/local
LIB_DIR	:= $(PREFIX)/lib
BIN_DIR := $(PREFIX)/bin
REM_DIR := $(LIB_DIR)/remuco

all:
	@echo "Run target"
	@echo "  'install' to install the Remuco server for $(PP) or"
	@echo "  'uninstall' to uninstall the Remuco server for $(PP)"

install:
	ls lib/* 2>&1 > /dev/null && for F in bin/* ; do \
		install $${F} $(BIN_DIR); \
	done || true
	ls lib/* 2>&1 > /dev/null && for F in lib/* ; do \
		install $${F} $(REM_DIR)/$(PP); \
	done || true

uninstall:
	rm -rf $(REM_DIR)/$(PP)
	ls lib/* 2>&1 > /dev/null && for F in bin/* ; do \
		rm -rf $(BIN_DIR)/$${F}; \
	done || true
