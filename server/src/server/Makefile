###############################################################################
#
# Install Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= yes

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

###############################################################################
#
# Build Config
#
###############################################################################

PKG_CONFIG_REQ := glib-2.0 dbus-glib-1 bluez Wand

CFLAGS := $(shell pkg-config --cflags $(PKG_CONFIG_REQ) 2>/dev/null)
LFLAGS := $(shell pkg-config --libs $(PKG_CONFIG_REQ) 2>/dev/null)

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

HDR_FILES = $(shell find -maxdepth 1 -type f -name "*.h")
#| sed -e "s,^\./,,"
SRC_FILES := $(shell find -maxdepth 1 -type f -name "*.c")
OBJ_FILES := $(subst .c,.o,$(SRC_FILES))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-server

install: all dbus-install
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-server $(DESTDIR)$(PREFIX)/bin

uninstall: dbus-uninstall
	rm  -f $(DESTDIR)$(PREFIX)/bin/remuco-server

clean: dbus-clean
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f stamp-*
	rm -f remuco-server
	rm -f massif.*

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c $(HDR_FILES)
	$(CC) $(CFLAGS) -c $< -o $@ 

remuco-server: $(OBJ_FILES) 
	$(CC) $(LFLAGS) $(OBJ_FILES) -o remuco-server

###############################################################################
#
# DBUS
#
##############################################################################

DBUS_SERVICE_NAME := net.sf.remuco.Server

DBUS_SERVICE_DIR := $(shell pkg-config dbus-1 --variable=session_bus_services_dir)

dbus-install:
	sed -e "s,PREFIX,$(PREFIX)," -e "s,NAME,$(DBUS_SERVICE_NAME)," \
		service.in > $(DBUS_SERVICE_NAME).service
	$(INSTALL_DIR) $(DESTDIR)$(DBUS_SERVICE_DIR)
	$(INSTALL_DAT) $(DBUS_SERVICE_NAME).service \
		$(DESTDIR)$(DBUS_SERVICE_DIR)

dbus-uninstall:
	rm -f $(DESTDIR)$(DBUS_SERVICE_DIR)/$(DBUS_SERVICE_NAME).service

dbus-server-glue.h: server.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_server \
		server.xml > dbus-server-glue.h
		
dbus-pp-glue.h: pp.xml
	dbus-binding-tool --mode=glib-client pp.xml > dbus-pp-glue.h
	# we want shorter function names:
	sed -i dbus-pp-glue.h -e "s/net_sf_remuco_PP/rem_pp/g"

dbus-clean:
	rm -f $(DBUS_SERVICE_NAME).service
	
###############################################################################
#
# Check Requirements
#
##############################################################################

# We do not check for the development files of libc because they should be
# present if the other requirements are fulfilled.
stamp-build-requirements:
	@echo
	@echo "Build requirements:"
	@for PKGC in $(PKG_CONFIG_REQ); do \
		echo -n "Checking for $$PKGC .. "; \
		if (pkg-config --exists $$PKGC > /dev/null) ; then \
			echo "ok"; \
		else \
			echo "missing!" ; \
			echo "It seems the dev- or header-package of $$PKGC is not installed." ; \
			echo "At least I could not find the pkg-config file of $$PKGC."; \
			exit 1; \
		fi; \
	done
	@echo
	touch stamp-build-requirements

###############################################################################
#
# Debug
#
##############################################################################

VGS ?= no

~/.config/remuco/debug:
	mkdir -p ~/.config/remuco
	touch ~/.config/remuco/debug

memcheck: all ~/.config/remuco/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind -v \
		--tool=memcheck \
		--leak-check=full \
		--leak-resolution=high \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.server.supp \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.magick.supp \
		--suppressions=memcheck.libc.supp \
		./remuco-server

massif: all ~/.config/remuco/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind \
		--tool=massif \
		--depth=20 \
		--format=html \
		--alloc-fn=g_memdup \
		--alloc-fn=g_strdup \
		--alloc-fn=g_strdup_printf \
		--alloc-fn=g_strdupv \
		--alloc-fn=g_malloc \
	        --alloc-fn=g_malloc0 \
		--alloc-fn=g_realloc \
		--alloc-fn=g_try_malloc \
		--alloc-fn=g_try_malloc0 \
		--alloc-fn=g_try_realloc \
		--alloc-fn=dbus_malloc \
		--alloc-fn=dbus_malloc0 \
		--alloc-fn=dbus_realloc \
		--alloc-fn=g_slice_alloc \
		--alloc-fn=g_slice_alloc0 \
		./remuco-server
