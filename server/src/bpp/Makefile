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

PKG_CONFIG_REQ_BPP := glib-2.0 dbus-glib-1
PKG_CONFIG_REQ_LAUNCHER := glib-2.0
PKG_CONFIG_REQ := $(PKG_CONFIG_REQ_BPP) $(PKG_CONFIG_REQ_LAUNCHER)

CFLAGS := $(shell pkg-config --cflags $(PKG_CONFIG_REQ) 2>/dev/null)
LFLAGS_BPP := $(shell pkg-config --libs $(PKG_CONFIG_REQ_BPP) 2>/dev/null)
LFLAGS_LAUNCHER := $(shell pkg-config --libs $(PKG_CONFIG_REQ_LAUNCHER) 2>/dev/null)

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

HDR_FILES := $(shell find -maxdepth 1 -type f -name "*.h")
HDR_FILES += ../server/log.h ../server/dbus.h ../server/util.h ../server/config.h
#HDR_FILES += dbus-server-glue.h dbus-pp-glue.h

SRC_FILES_BPP := bpp.c
# files borrowed from server:
SRC_FILES_BPP += $(addprefix ../server/,log.c dbus.c util.c config.c)
OBJ_FILES_BPP := $(subst .c,.o,$(SRC_FILES_BPP))

SRC_FILES_LAUNCHER := launcher.c
# files borrowed from server:
SRC_FILES_LAUNCHER += $(addprefix ../server/,log.c util.c)
OBJ_FILES_LAUNCHER := $(subst .c,.o,$(SRC_FILES_LAUNCHER))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-bpp remuco-bpp-launcher

install: all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-bpp remuco-bpp-launcher $(DESTDIR)$(PREFIX)/bin

uninstall:
	rm  -f $(DESTDIR)$(PREFIX)/bin/remuco-bpp{,launcher}

clean:
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f stamp-*
	rm -f remuco-bpp remuco-bpp-launcher

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c $(HDR_FILES)
	$(CC) $(CFLAGS) -c $< -o $@ 

remuco-bpp: $(OBJ_FILES_BPP)
	$(CC) $(LFLAGS_BPP) $(OBJ_FILES_BPP) -o remuco-bpp

remuco-bpp-launcher: $(OBJ_FILES_LAUNCHER)
	$(CC) $(LFLAGS_LAUNCHER) $(OBJ_FILES_LAUNCHER) -o remuco-bpp-launcher
	
###############################################################################
#
# DBUS
#
##############################################################################

dbus-server-glue.h: ../server/server.xml
	dbus-binding-tool --mode=glib-client \
		../server/server.xml > dbus-server-glue.h
	# we want shorter function names:
	sed -i dbus-server-glue.h -e "s/net_sf_remuco_Server/rem_server/g"

dbus-pp-glue.h: ../server/pp.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_pp \
		../server/pp.xml > dbus-pp-glue.h
	# remove some 'const', because bpp has to edit some vars:
	sed -i dbus-pp-glue.h \
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

VGS ?= no

~/.config/remuco/debug:
	mkdir -p ~/.config/remuco
	touch ~/.config/remuco/debug


memcheck-bpp: all ~/.config/remuco/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind -v \
		--tool=memcheck \
		--leak-check=full \
		--leak-resolution=high \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.bpp.supp \
		--suppressions=../server/memcheck.glib.supp \
		--suppressions=../server/memcheck.libc.supp \
		./remuco-bpp $(BPP)

massifi-bpp: all ~/.config/remuco/debug
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

memcheck-launcher: all ~/.config/remuco/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly \
		valgrind -v \
		--tool=memcheck \
		--leak-check=full \
		--leak-resolution=high \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.bpp.supp \
		--suppressions=../server/memcheck.glib.supp \
		--suppressions=../server/memcheck.libc.supp \
		./remuco-bpp-launcher --log-here

massifi-launcher: all ~/.config/remuco/debug
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
		./remuco-bpp-launcher --log-here

