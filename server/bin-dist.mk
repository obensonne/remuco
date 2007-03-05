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
	install -d $(BIN_DIR) $(REM_DIR)/$(PP)
	cd bin ; ls 2>&1 > /dev/null && for F in * ; do \
		install $${F} $(BIN_DIR); \
	done || true
	cd lib ; ls 2>&1 > /dev/null && for F in * ; do \
		install -m 644 $${F} $(REM_DIR)/$(PP); \
	done || true

uninstall:
	rm -rf $(REM_DIR)/$(PP)
	cd bin ; ls 2>&1 > /dev/null && for F in * ; do \
		rm -rf $(BIN_DIR)/$${F}; \
	done || true
