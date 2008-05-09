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

PKG_CONFIG_REQ_SRV := glib-2.0 dbus-glib-1 bluez Wand
PKG_CONFIG_REQ_CTL := glib-2.0 dbus-glib-1
PKG_CONFIG_REQ_BPP := glib-2.0 dbus-glib-1
PKG_CONFIG_REQ_ALL := glib-2.0 dbus-glib-1 bluez Wand

CFLAGS := $(shell pkg-config --cflags $(PKG_CONFIG_REQ_ALL) 2>/dev/null)
LFLAGS_SRV := $(shell pkg-config --libs $(PKG_CONFIG_REQ_SRV) 2>/dev/null)
LFLAGS_CTL := $(shell pkg-config --libs $(PKG_CONFIG_REQ_CTL) 2>/dev/null)
LFLAGS_BPP := $(shell pkg-config --libs $(PKG_CONFIG_REQ_BPP) 2>/dev/null)

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

HDR_FILES = $(shell find -maxdepth 1 -type f -name "*.h")

SRC_FILES_SRV := $(shell find -maxdepth 1 -type f -name "*.c")
SRC_FILES_SRV := $(subst ./bpp.c,,$(SRC_FILES_SRV))
SRC_FILES_SRV := $(subst ./bpp-main.c,,$(SRC_FILES_SRV))
SRC_FILES_SRV := $(subst ./ctl.c,,$(SRC_FILES_SRV))
OBJ_FILES_SRV := $(subst .c,.o,$(SRC_FILES_SRV))

SRC_FILES_CTL := ctl.c dbus.c
OBJ_FILES_CTL := $(subst .c,.o,$(SRC_FILES_CTL))

SRC_FILES_BPP := bpp.c bpp-main.c dbus.c util.c config.c log.c
OBJ_FILES_BPP := $(subst .c,.o,$(SRC_FILES_BPP))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-server remuco-bpp remuco

install: all dbus-install
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-server remuco-bpp remuco $(DESTDIR)$(PREFIX)/bin

uninstall: dbus-uninstall
	rm  -f $(DESTDIR)$(PREFIX)/bin/remuco-server

clean: dbus-clean
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f stamp-*
	rm -f remuco-server remuco-bpp remuco
	rm -f massif.*

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c $(HDR_FILES)
	$(CC) $(CFLAGS) -c $< -o $@ 

remuco: shell-glue-c.h server-glue-c.h $(OBJ_FILES_CTL)
	$(CC) $(LFLAGS_CTL) $(OBJ_FILES_CTL) -o remuco

remuco-server: server-glue-s.h shell-glue-s.h pp-glue-c.h $(OBJ_FILES_SRV) 
	$(CC) $(LFLAGS_SRV) $(OBJ_FILES_SRV) -o remuco-server

remuco-bpp: pp-glue-s.h server-glue-c.h $(OBJ_FILES_BPP)
	$(CC) $(LFLAGS_BPP) $(OBJ_FILES_BPP) -o remuco-bpp

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

server-glue-s.h: server.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_server \
		server.xml > server-glue-s.h
		
server-glue-c.h: server.xml
	dbus-binding-tool --mode=glib-client server.xml > server-glue-c.h

shell-glue-s.h: shell.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_shell \
		shell.xml > shell-glue-s.h

shell-glue-c.h: pp.xml
	dbus-binding-tool --mode=glib-client shell.xml > shell-glue-c.h

pp-glue-s.h: pp.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_pp \
		pp.xml > pp-glue-s.h
	# remove some 'const', because bpp has to edit some vars:
	sed -i pp-glue-s.h \
		-e "s/^static const DBusGMethodInfo/static DBusGMethodInfo/" \
		-e "s/^const DBusGObjectInfo/static DBusGObjectInfo/"

pp-glue-c.h: pp.xml
	dbus-binding-tool --mode=glib-client pp.xml > pp-glue-c.h

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
	@for PKGC in $(PKG_CONFIG_REQ_ALL); do \
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
	G_SLICE=always-malloc G_DEBUG=gc-friendly PATH=./:$(PATH) \
		valgrind \
		--tool=memcheck \
		--leak-check=full \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.server.supp \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.magick.supp \
		--suppressions=memcheck.libc.supp \
		./remuco-server -fl

massif: all $(REM_CONFIG_DIR)/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly PATH=./:$(PATH) \
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
		./remuco-server -fl

###### BPP ######

memcheck-bpp: all $(REM_CONFIG_DIR)/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind \
		--tool=memcheck \
		--leak-check=full \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.bpp.supp \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.libc.supp \
		./remuco-bpp -fl $(BPP)

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
		./remuco-bpp -fl $(BPP)
