###############################################################################
#
# Configuraion
#
###############################################################################

include ../Makefile.inc

###############################################################################
#
# Build Config
#
###############################################################################

# pkg-config requirments (check below)
PKG_DEP_SRV := glib-2.0 dbus-glib-1 bluez Wand
PKG_DEP_CTL := glib-2.0 dbus-glib-1
PKG_DEP_BPP := glib-2.0 dbus-glib-1
PKG_DEP_ALL := glib-2.0 dbus-glib-1 bluez Wand

CFLAGS := $(shell $(PKG_CONFIG) --cflags $(PKG_DEP_ALL))
LFLAGS_SRV := $(shell $(PKG_CONFIG) --libs $(PKG_DEP_SRV))
LFLAGS_CTL := $(shell $(PKG_CONFIG) --libs $(PKG_DEP_CTL))
LFLAGS_BPP := $(shell $(PKG_CONFIG) --libs $(PKG_DEP_BPP))

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

HDR_FILES := $(shell find -maxdepth 1 -type f -name "*.h")
HDR_FILES := $(subst ./,,$(HDR_FILES))

SRC_FILES_SRV := bppl.c bt.c config.c daemon.c dbus.c img.c ip.c log.c net.c \
				 serial.c server.c shell.c util.c
OBJ_FILES_SRV := $(subst .c,.o,$(SRC_FILES_SRV))

SRC_FILES_CTL := ctl.c dbus.c
OBJ_FILES_CTL := $(subst .c,.o,$(SRC_FILES_CTL))

SRC_FILES_BPP := bpp.c bpp-main.c dbus.c config.c log.c util.c
OBJ_FILES_BPP := $(subst .c,.o,$(SRC_FILES_BPP))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: remuco-server remuco-bpp remuco

install: all dbus-install
	$(INSTALL_DIR) $(DESTDIR)$(BIN_DIR)
	$(INSTALL_EXE) remuco-server remuco-bpp remuco $(DESTDIR)$(BIN_DIR)

uninstall: dbus-uninstall
	rm -f $(DESTDIR)$(BIN_DIR)/remuco
	rm -f $(DESTDIR)$(BIN_DIR)/remuco-bpp
	rm -f $(DESTDIR)$(BIN_DIR)/remuco-server

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

%.o: %.c $(HDR_FILES) stamp-build-requirements
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

DBUS_SERVICE_NAME := net.sf.remuco.Shell
DBUS_SERVICE_DIR := $(shell $(PKG_CONFIG) dbus-1 \
								--variable=session_bus_services_dir 2>/dev/null)

# break if DBUS_SERVICE_DIR is not set
$(if $(strip $(DBUS_SERVICE_DIR)),,$(error variable session_bus_services_dir in dbus-1 is not set))

dbus-install:
	sed -e "s,PREFIX,$(PREFIX)," -e "s,NAME,$(DBUS_SERVICE_NAME)," \
		service.in > $(DBUS_SERVICE_NAME).service
	$(INSTALL_DIR) $(DESTDIR)$(DBUS_SERVICE_DIR)
	$(INSTALL_DAT) $(DBUS_SERVICE_NAME).service \
		$(DESTDIR)$(DBUS_SERVICE_DIR)

dbus-uninstall:
	rm -f $(DESTDIR)$(DBUS_SERVICE_DIR)/$(DBUS_SERVICE_NAME).service

dbus-clean:
	rm -f $(DBUS_SERVICE_NAME).service

# Glue Headers #

server-glue-s.h: server.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_server \
		server.xml > server-glue-s.h
		
server-glue-c.h: server.xml
	dbus-binding-tool --mode=glib-client server.xml > server-glue-c.h

shell-glue-s.h: shell.xml
	dbus-binding-tool --mode=glib-server --prefix=rem_shell \
		shell.xml > shell-glue-s.h

shell-glue-c.h: shell.xml
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
	$(call tool-check,dbus-binding-tool,--version)
	$(call pkg-config-check,glib-2.0,2.16)
	$(call pkg-config-check,dbus-1,0)
	$(call pkg-config-check,dbus-glib-1,0.74)
	$(call pkg-config-check,bluez,3.29)
	$(call pkg-config-check,Wand,6.3.7)
	$(call pkg-config-check-var,dbus-1,session_bus_services_dir)
	@echo
	touch stamp-build-requirements

###############################################################################
#
# Debug
#
###############################################################################

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

memcheck-ctl: all $(REM_CONFIG_DIR)/debug
	G_SLICE=always-malloc G_DEBUG=gc-friendly PATH=./:$(PATH) \
		valgrind \
		--tool=memcheck \
		--leak-check=full \
		--num-callers=20 \
		--gen-suppressions=$(VGS) \
		--suppressions=memcheck.glib.supp \
		--suppressions=memcheck.magick.supp \
		--suppressions=memcheck.libc.supp \
		./remuco $(ARGS)

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

###############################################################################
#
# Functions
#
###############################################################################

# $(call tool-check,command,param)
define tool-check
	@echo -n "Checking for tool $1 .. "
	@$1 $2 > /dev/null 2>&1 \
		|| { \
			echo "missing" ; \
  			false; \
  			} \
  		&& { \
  			echo "ok"; \
  			}
endef

# $(call pkg-config-check,module,version)
define pkg-config-check
	@echo -n "Checking for $1 (>= $2) .. "
	@$(PKG_CONFIG) --exists $1 > /dev/null 2>&1 \
		|| { \
			echo "missing (need the development files of $1)" ; \
  			false; \
  			}
	@$(PKG_CONFIG) --atleast-version=$2 $1 > /dev/null 2>&1 \
		|| { \
			HAVE_VERSION=`$(PKG_CONFIG) --modversion $1` ; \
			echo "insufficient version (you have $$HAVE_VERSION)" ; \
  			false; \
  			} \
  		&& { \
  			echo "ok"; \
  			}
endef
