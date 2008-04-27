###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= yes

PLAYER := Fooc
PLAYER_LC := fooc

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
# init variables
#
###############################################################################

CFLAGS := 
LFLAGS := 

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
else
CFLAGS += -O2
#CFLAGS += -DG_DISABLE_ASSERT
endif

CFLAGS += -Wall

# the packages we want to get info about via pkg-config
PKG_CONFIG_REQ := glib-2.0 dbus-glib-1 dbus-1

# extend compiler and link flags with pkg-config output
CFLAGS += $(shell pkg-config --cflags $(PKG_CONFIG_REQ))
LFLAGS += $(shell pkg-config --libs $(PKG_CONFIG_REQ))

# all source files
SRC_FILES := fooc.c

# all object files
OBJ_FILES := $(subst .c,.o,$(SRC_FILES))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: stamp-build-requirements remuco-$(PLAYER_LC)

install: all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/bin
	$(INSTALL_EXE) remuco-$(PLAYER_LC) $(DESTDIR)$(PREFIX)/bin

uninstall:
	rm  -f $(DESTDIR)$(PREFIX)/bin/remuco
	rm -rf $(UIN_FILES)

clean:
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f stamp-build-requirements
	rm -f remuco-$(PLAYER_LC)
	rm -f server-glue.h pp-glue.h pp-edit.xml

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@ 

remuco-$(PLAYER_LC): server-glue.h pp-glue.h $(OBJ_FILES)
	$(CC) $(LFLAGS) $(OBJ_FILES) -o remuco-$(PLAYER_LC)

pp-glue.h: pp-edit.xml
	rm -f *.o # make sure we rebuild
	dbus-binding-tool --mode=glib-server --prefix=rem_pp \
		pp-edit.xml > pp-glue.h

# set actual player name in dbus interface description
pp-edit.xml: pp.xml
	sed pp.xml -e "s/PP/$(PLAYER)/g" > pp-edit.xml
		
server-glue.h: server.xml
	dbus-binding-tool --mode=glib-client \
		server.xml > server-glue.h
	sed -i server-glue.h -e "s/net_sf_remuco_Server/rem_server/g"
	# the following is just to fix a missing CDT feature, remove for prod.
	sed -i server-glue.h \
		-e "s/^static *//" \
		-e "s/^#ifdef G_HAVE_INLINE.*$$//" \
		-e "s/^inline.*$$//" \
		-e "s/^#endif$$//"

# We do not check for the development files of libc because they should be
# present if the other requirements are fulfilled.
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

# set the global debug flag for all remuco components
~/.config/remuco/debug:
	mkdir -p ~/.config/remuco
	touch ~/.config/remuco/debug

valgrind: all ~/.config/remuco/debug
	G_SLICE=always-malloc \
		valgrind --leak-check=yes \
		--gen-suppressions=$(VGS) \
		--suppressions=valgrind.suppress \
		./remuco-$(PLAYER_LC)
