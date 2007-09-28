###############################################################################
#
# Configuraion
#
###############################################################################

LIB_FILE := libremuco.so
LIB_VERSION_MAJOR := 0
LIB_VERSION_MINOR := 5
LIB_VERSION := $(LIB_VERSION_MAJOR).$(LIB_VERSION_MINOR)

###############################################################################

OBJ_FILES := 

CFLAGS := -fPIC
LFLAGS := -shared
#LFLAGS += -Wl,-soname,$(LIB_FILE).$(SONAME_MAJOR)

INSTALL := install
INSTALL_LIB := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL)
INSTALL_DIR := $(INSTALL) -d

ifneq "$(DEBUG)" ""
CFLAGS += -O0 -g
CFLAGS += -DLOGLEVEL=LL_DEBUG
STRIP := touch
else
CFLAGS += -O2
CFLAGS += -DLOGLEVEL=LL_INFO
STRIP := strip --strip-unneeded
endif

CFLAGS += $(shell pkg-config --cflags glib-2.0 gthread-2.0)
CFLAGS += -I.
CFLAGS += -Wall
LFLAGS += $(shell pkg-config --libs glib-2.0 gthread-2.0)

PKG_CONFIG_REQ := glib-2.0 gthread-2.0

include remuco/data/data.mk
include remuco/layer/layer.mk
include remuco/util/util.mk

###############################################################################
#
# Standard Targets
#
##############################################################################

all: .check-build-requirements $(LIB_FILE)

# for now we do not seperate between normal and dev packages
install: install-dev all
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/lib
	$(INSTALL_LIB) $(LIB_FILE) $(DESTDIR)$(PREFIX)/lib/

install-dev:
	# Install header files
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/include
	find -type f -name "*.h" | grep -vF ".svn" | \
		xargs cp --parents -t $(DESTDIR)$(PREFIX)/include
	# Generate and install pkg-config file	
	cp remuco.pc.in remuco.pc
	sed -i remuco.pc \
		-e "s,@PREFIX@,$(PREFIX)," \
		-e "s,@VERSION@,$(LIB_VERSION),"
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/lib/pkgconfig
	$(INSTALL_LIB) -m 644 remuco.pc $(DESTDIR)$(PREFIX)/lib/pkgconfig

uninstall: uninstall-dev
	rm  -f $(DESTDIR)$(PREFIX)/lib/libremuco.so

uninstall-dev:
	rm -rf $(DESTDIR)$(PREFIX)/include/remuco
	rm  -f $(DESTDIR)$(PREFIX)/lib/pkgconfig/remuco.pc

clean:
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	rm -f $(LIB_FILE) remuco.pc
	rm -f .check-build-requirements

###############################################################################
#
# Special Targets
#
##############################################################################

$(LIB_FILE): $(OBJ_FILES)
	$(CC) $(LFLAGS) -o $(LIB_FILE) $(OBJ_FILES)
	$(STRIP) $(LIB_FILE)

.check-build-requirements:
	
	@echo
	@echo "Checking if all needed development packages are installed:"
	@for PKGC in $(PKG_CONFIG_REQ) ; do \
		echo -n "Checking for $$PKGC .. "; \
		if (pkg-config --exists $$PKGC > /dev/null) ; then \
			echo "ok"; \
		else \
			echo "missing!" ; \
			echo "It seems the dev- or header-package of $$PKGC is not installed." ;\
			echo "At least I could not find the pkg-config file of $$PKGC."; \
			echo; \
			exit 1; \
		fi; \
	done
	@echo
	touch .check-build-requirements
	


