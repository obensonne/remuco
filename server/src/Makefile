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
PKG_CONFIG_REQ_BPP := glib-2.0 dbus-glib-1

CFLAGS := $(shell pkg-config --cflags $(PKG_CONFIG_REQ) 2>/dev/null)
LFLAGS := $(shell pkg-config --libs $(PKG_CONFIG_REQ) 2>/dev/null)
LFLAGS_BPP := $(shell pkg-config --libs $(PKG_CONFIG_REQ_BPP) 2>/dev/null)

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

HDR_FILES = $(shell find -maxdepth 1 -type f -name "*.h")
#| sed -e "s,^\./,,"
SRC_FILES := $(shell find -maxdepth 1 -type f -name "*.c")
SRC_FILES := $(subst ./bpp.c,,$(SRC_FILES))
OBJ_FILES := $(subst .c,.o,$(SRC_FILES))

SRC_FILES_BPP := bpp.c dbus.c util.c config.c log.c
OBJ_FILES_BPP := $(subst .c,.o,$(SRC_FILES_BPP))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-server remuco-bpp

install: all dbus-install
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-server remuco-bpp $(DESTDIR)$(PREFIX)/bin

uninstall: dbus-uninstall
	rm  -f $(DESTDIR)$(PREFIX)/bin/remuco-server

clean: dbus-clean
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f stamp-*
	rm -f remuco-server remuco-bpp
	rm -f massif.*

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c $(HDR_FILES)
	$(CC) $(CFLAGS) -c $< -o $@ 

remuco-server: dbus-server-glue.h dbus-pp-glue.h $(OBJ_FILES) 
	$(CC) $(LFLAGS) $(OBJ_FILES) -o remuco-server

remuco-bpp: bpp-dbus-server-glue.h bpp-dbus-pp-glue.h $(OBJ_FILES_BPP)
	$(CC) $(LFLAGS_BPP) $(OBJ_FILES_BPP) -o remuco-bpp

###############################################################################
#
# DBUS - server
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
# DBUS - basic player proxy
#
##############################################################################

bpp-dbus-server-glue.h: server.xml
	dbus-binding-tool --mode=glib-client server.xml > bpp-dbus-server-glue.h
	# we want shorter function names:
	sed -i bpp-dbus-server-glue.h -e "s/net_sf_remuco_Server/rem_server/g"

bpp-dbus-pp-glue.h: pp.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_pp \
		pp.xml > bpp-dbus-pp-glue.h
	# remove some 'const', because bpp has to edit some vars:
	sed -i bpp-dbus-pp-glue.h \
		-e "s/^static const DBusGMethodInfo/static DBusGMethodInfo/" \
		-e "s/^const DBusGObjectInfo/static DBusGObjectInfo/"

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

XDG_CONFIG_HOME ?= $(HOME)/.config
REM_CONFIG_DIR := $(XDG_CONFIG_HOME)/remuco

VGS ?= no

$(REM_CONFIG_DIR)/debug:
	mkdir -p $(REM_CONFIG_DIR)
	touch $(REM_CONFIG_DIR)/debug

###### Server ######

memcheck: all $(REM_CONFIG_DIR)/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind -v \
		--tool=memcheck \
		--leak-check=full \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.server.supp \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.magick.supp \
		--suppressions=memcheck.libc.supp \
		./remuco-server

massif: all $(REM_CONFIG_DIR)/debug
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

###### BPP ######

memcheck-bpp: all $(REM_CONFIG_DIR)/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind -v \
		--tool=memcheck \
		--leak-check=full \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.bpp.supp \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.libc.supp \
		./remuco-bpp $(BPP)

massifi-bpp: all $(REM_CONFIG_DIR)/debug
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
		./remuco-bpp $(BPP)
