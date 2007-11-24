###############################################################################
#
# Configuraion
#
###############################################################################

PREFIX ?= /usr
DEBUG ?= yes

LIB_FILE_BASE := libremuco

LT_CURRENT := 0
LT_REVISION := 0
LT_AGE := 0

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
# Initial CFLAGS and LFLAGS
#
###############################################################################

CFLAGS := -DREM_LIB_MAJOR=0 -DREM_LIB_MINOR=6
LFLAGS := 

ifeq "$(DEBUG)" "yes"
CFLAGS += -O0 -g
CFLAGS += -DLOGLEVEL=LL_DEBUG
#CFLAGS += -DLOGLEVEL=LL_NOISE
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

all: stamp-build-requirements $(LIB_FILE_BASE).la

install: all install-headers
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/lib
	libtool --mode=install $(INSTALL_LIB) $(LIB_FILE_BASE).la \
		$(DESTDIR)$(PREFIX)/lib/

install-headers:
	# Install header files
	$(INSTALL_DIR) $(DESTDIR)$(PREFIX)/include
	find ../include -type f -name "*.h" | grep -vF ".svn" | \
		xargs cp --parents -t $(DESTDIR)$(PREFIX)/include
	
uninstall: uninstall-headers
	rm  -f $(DESTDIR)$(PREFIX)/lib/$(LIB_FILE_BASE).so*
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
	rm -f $(LIB_FILE_BASE)*
	rm -f stamp-build-requirements

###############################################################################
#
# Special Targets
#
##############################################################################

%.o: %.c
	libtool --mode=compile $(CC) $(CFLAGS) -c $< -o $@ 

$(LIB_FILE_BASE).la: $(OBJ_FILES)
	libtool --mode=link $(CC) \
		-version-info=$(LT_CURRENT):$(LT_REVISION):$(LT_AGE) \
		-rpath /usr/lib \
		-o $(LIB_FILE_BASE).la $(OBJ_FILES_LT) \
		$(LFLAGS)

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
	
soname:
	@echo $(LT_CURRENT)

