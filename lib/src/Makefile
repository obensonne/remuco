###############################################################################
#
# Configuraion
#
###############################################################################

LIB_FILE_BASE := libremuco.so
LIB_FILE := $(LIB_FILE_BASE)

LT_CURRENT := 0
LT_REVISION := 0
LT_AGE := 0
PREFIX ?= /usr

###############################################################################
#
# Tools
#
###############################################################################

# install commands
INSTALL := install
INSTALL_LIB := $(INSTALL) -m 644
INSTALL_EXE := $(INSTALL) -m 755
INSTALL_DIR := $(INSTALL) -m 755 -d

ifeq "$(strip $(DEBUG))" ""
STRIP := strip --strip-unneeded
else
STRIP := touch
endif

###############################################################################
#
# Initial CFLAGS and LFLAGS
#
###############################################################################

CFLAGS := -DREM_LIB_MAJOR=0 -DREM_LIB_MINOR=6
LFLAGS := 

# shared library compile and link flags
#CFLAGS += -fPIC
#LFLAGS += -shared
#LFLAGS += -Wl,-soname,$(LIB_FILE_SO)

# debug dependent compile flags and commands
ifneq "$(DEBUG)" ""
CFLAGS += -O0 -g
CFLAGS ?= -DLOGLEVEL=LL_DEBUG
#CFLAGS ?= -DLOGLEVEL=LL_NOISE
else
CFLAGS += -O2
CFLAGS += -DLOGLEVEL=LL_INFO
endif

CFLAGS += -I../include
CFLAGS += -Wall

###############################################################################
#
# Include make statements of sub directories
# 
# These statements extend the variables SRC_FILES and PKG_CONFIG_REQ
#
###############################################################################

# all source files
SRC_FILES :=

# the packages we want to get info about via pkg-config
PKG_CONFIG_REQ := glib-2.0

# include the make statements of the sub directories
include $(shell find . -name module.mk | grep -vF ".svn")

# extend compiler and link flags with pkg-config output
CFLAGS += $(shell pkg-config --cflags $(PKG_CONFIG_REQ))
LFLAGS += $(shell pkg-config --libs $(PKG_CONFIG_REQ))

# all object files
OBJ_FILES := $(subst .c,.o,$(SRC_FILES))
# all libtool object files
OBJ_FILES_LT := $(subst .o,.lo,$(OBJ_FILES))

###############################################################################
#
# Standard Targets
#
##############################################################################

all: .build-req libremuco.la

# for now we do not seperate between normal and dev packages
install: all install-headers
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/lib
	libtool --mode=install $(INSTALL_LIB) libremuco.la \
		$(DESTDIR)$(PREFIX)/lib/

install-headers:
	# Install header files
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/include
	find ../include -type f -name "*.h" | grep -vF ".svn" | \
		xargs cp --parents -t $(DESTDIR)$(PREFIX)/include
	
uninstall: uninstall-headers
	rm  -f $(DESTDIR)$(PREFIX)/lib/libremuco.so*
	ldconfig -n $(DESTDIR)$(PREFIX)/lib/

uninstall-headers:
	rm -rf $(DESTDIR)$(PREFIX)/include/remuco
	rm  -f $(DESTDIR)$(PREFIX)/include/remuco.h
	
clean:
	find -type f -name "*.o" | xargs rm -f
	find -type f -name "*.lo" | xargs rm -f
	find -type f -name "*.loT" | xargs rm -f
	find -type f -name "*~" | xargs rm -f
	find -type d -name ".libs" | xargs rm -rf
	rm -f libremuco*
	rm -f .build-req

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c
	libtool --mode=compile $(CC) $(CFLAGS) -c $< -o $@ 

libremuco.la: $(OBJ_FILES)
	libtool --mode=link $(CC) \
		-version-info=$(LT_CURRENT):$(LT_REVISION):$(LT_AGE) \
		-rpath /usr/lib \
		-o libremuco.la $(OBJ_FILES_LT) \
		$(LFLAGS)
#	$(CC) $(LFLAGS) -o $(LIB_FILE) $(OBJ_FILES)
#	$(STRIP) $(LIB_FILE)

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
	
soname:
	@echo $(LT_CURRENT)

