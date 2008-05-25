###############################################################################
#
# Configuration (change if needed)
#
###############################################################################

PKG_NAME := remuco-server
PKG_VERSION := 0.7.1
PKG_FULL := $(PKG_NAME)-$(PKG_VERSION)

PREFIX := /usr

BIN_DIR := $(PREFIX)/bin
DATA_DIR := $(PREFIX)/share

MAN_DIR := $(DATA_DIR)/man
DOC_DIR := $(DATA_DIR)/doc
DOC_DIR_PKG := $(DOC_DIR)/$(PKG_NAME)

# Will be set later with pkg-config. Uncomment to set a custom path.
#DBUS_SERVICE_DIR :=

DEBUG := yes

###############################################################################
#
# Tools
#
###############################################################################

INSTALL := install
INSTALL_DAT := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL) -m 755
INSTALL_DIR := $(INSTALL) -m 755 -d
ifneq "$(DEBUG)" "yes"
INSTALL_EXE += -s
endif

GZIP := gzip -f9

PKG_CONFIG := pkg-config

###############################################################################
#
# Misc
#
###############################################################################

# break if pkg-config is _not_ available
TMP := $(strip $(shell $(PKG_CONFIG) --version 2>/dev/null))
$(if $(TMP),,$(error missing tool: $(PKG_CONFIG)))


