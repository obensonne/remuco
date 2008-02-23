###############################################################################
#
# Configuration
#
###############################################################################

DEBUG ?= no
PREFIX ?= /usr

XDG_CONFIG_HOME ?= $(HOME)/.config
X2_CONFIG_DIR := $(XDG_CONFIG_HOME)/xmms2

PKG_VERSION := 0.6.0
PKG_BASENAME := remuco-xmms2
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

PKG_CONFIG_REQ := remuco xmms2-client xmms2-client-glib glib-2.0

###############################################################################
#
# Tools
#
###############################################################################

INSTALL := install
INSTALL_LIB := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL) -m 755
INSTALL_DIR := $(INSTALL) -m 755 -d
ifneq "$(DEBUG)" "yes"
INSTALL_LIB += -s
INSTALL_EXE += -s
endif

###############################################################################
#
# CFLAGS and LFLAGS
#
###############################################################################

# extend compiler and link flags with pkg-config output
CFLAGS += $(shell pkg-config --cflags $(PKG_CONFIG_REQ))
LFLAGS += $(shell pkg-config --libs $(PKG_CONFIG_REQ))

# debug dependent compile flags and commands
ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
CFLAGS += -DDEBUG
#CFLAGS += -DDO_LOG_NOISE
else
CFLAGS += -O2
endif

CFLAGS += -Wall

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-xmms2

remuco-xmms2: remuco-xmms2.o
	$(CC) -o remuco-xmms2 remuco-xmms2.o $(LFLAGS)
	chmod +x remuco-xmms2

install-bin: all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-xmms2 $(DESTDIR)$(PREFIX)/bin

install: all
	install -d $(DESTDIR)$(X2_CONFIG_DIR)/startup.d
	install remuco-xmms2 $(DESTDIR)$(X2_CONFIG_DIR)/startup.d

uninstall-bin:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-xmms2

uninstall:
	rm -f $(DESTDIR)$(X2_CONFIG_DIR)/startup.d/remuco-xmms2

dist: clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile build/$(PKG_FULLNAME)/
	cp -r remuco-xmms2.c doc build/$(PKG_FULLNAME)/
	find build/$(PKG_FULLNAME) -type d -name ".svn" | xargs rm -rf
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	find -type f -name "*.o" | xargs rm -f
	rm -rf build dist
	rm -f *.deb
	rm -f stamp-build-requirements
	rm -f remuco-xmms2

###############################################################################
#
# Special Targets
#
##############################################################################

stamp-build-requirements:
	@echo
	@echo "Build requirements:"
	@for PKGC in $(PKG_CONFIG_REQ) ; do \
		echo -n "Checking for $$PKGC .. "; \
		if (pkg-config --exists $$PKGC > /dev/null) ; then \
			echo "ok"; \
		else \
			echo "missing!" ; \
			echo "It seems the dev- or header-package of $$PKGC is not installed." ; \
			echo "At least I could not find the pkg-config file of $$PKGC."; \
			echo; \
			exit 1; \
		fi; \
	done
	@echo -n "Checking for libtool .. "; \
	if (libtool --version > /dev/null 2>&1) ; then \
		echo "ok"; \
	else \
		echo "missing!" ; \
		echo "It seems libtool is not installed." ; \
		echo "At least I could not start it." ; \
	fi
	@echo
	touch stamp-build-requirements

valgrind: all
	G_SLICE=always-malloc \
		valgrind --leak-check=yes \
		--gen-suppressions=no \
		--suppressions=valgrind-errors.suppress \
		./remuco-xmms2

###############################################################################
#
# Package Building Targets
#
##############################################################################

pkg-version:
	@echo $(PKG_VERSION)
	
pkg-basename:
	@echo $(PKG_BASENAME)

pkg-fullname:
	@echo $(PKG_FULLNAME)
