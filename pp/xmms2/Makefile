###############################################################################
#
# Configuration
#
###############################################################################

DEBUG ?= yes
PREFIX ?= /usr

PKG_VERSION := 0.6.0
PKG_BASENAME := remuco-xmms2
PKG_FULLNAME := $(PKG_BASENAME)-$(PKG_VERSION)

PKG_CONFIG_REQ := remuco xmms2-client xmms2-client-glib glib-2.0

###############################################################################
#
# CFLAGS and LFLAGS
#
###############################################################################

# extend compiler and link flags with pkg-config output
CFLAGS += $(shell pkg-config --cflags $(PKG_CONFIG_REQ))
LFLAGS += $(shell pkg-config --libs $(PKG_CONFIG_REQ))

# debug dependent compile flags and commands
ifneq "$(DEBUG)" ""
CFLAGS += -O0 -g
CFLAGS += -DLOGLEVEL=LL_NOISE
else
CFLAGS += -O2
CFLAGS += -DLOGLEVEL=LL_INFO
endif

CFLAGS += -Wall

###############################################################################
#
# Standard Targets
#
##############################################################################

all: .build-req remuco-xmms2

remuco-xmms2: remuco-xmms2.o
	$(CC) -o remuco-xmms2 remuco-xmms2.o $(LFLAGS)
	chmod +x remuco-xmms2

install:
	install remuco-xmms2 $(DESTDIR)$(PREFIX)/bin

uninstall:
	rm -f $(DESTDIR)$(PREFIX)/bin/remuco-xmms2

dist: clean
	mkdir -p build/$(PKG_FULLNAME)
	cp Makefile build/$(PKG_FULLNAME)/
	cp -r src doc build/$(PKG_FULLNAME)/
	mkdir dist
	tar zcf dist/$(PKG_FULLNAME).tar.gz -C build $(PKG_FULLNAME)

clean:
	find -type f -name "*.o" | xargs rm -f
	rm -rf build dist
	rm -f *.deb
	rm -f .build-req
	rm -f remuco-xmms2

###############################################################################
#
# Special Targets
#
##############################################################################

.build-req:
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
	touch .build-req

valgrind: all
	G_SLICE=always-malloc \
		valgrind --leak-check=yes \
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