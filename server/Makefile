def: all

###############################################################################
#
# Misc global settings
#
###############################################################################

# List of available PPs
PP_LIST := template_c template_py xmms xmms2 rhythmbox
PP_LIST_EXECUTABLES := $(patsubst %,remuco-%,$(PP_LIST))

# Main release number:
RELEASE_MAIN := 0.4
# Server sub release (relative to main release)
RELEASE_SERVER := 1
# PP sub release (relative to main release)
#RELEASE_PP := gets defined in PP specific make file

# Log level: LL_FATAL, LL_ERROR, LL_WARN, LL_INFO, LL_DEBUG, LL_NOISE
LOGLEVEL ?= LL_DEBUG

###############################################################################
#
# Installation path (already important when building !)
#
###############################################################################

PREFIX	:= /usr/local
LIB_DIR	:= $(PREFIX)/lib
BIN_DIR	:= $(PREFIX)/bin
REM_DIR	:= $(LIB_DIR)/remuco

###############################################################################
#
# Commands
#
###############################################################################

CC		:= gcc

INSTALL := install
INSTALL_PROG := $(INSTALL)
INSTALL_DATA := $(INSTALL) -m 664

STRIP := strip
#STRIP := touch

###############################################################################
#
# Include PP makefile
#
###############################################################################

PP_NAME := ${PP}
ifneq "$(PP_NAME)" ""
include rem-pp-$(PP_NAME).mk
endif

RELEASE := $(RELEASE_MAIN).$(RELEASE_SERVER).$(RELEASE_PP)
PKG_NAME := remuco-server-$(PP_NAME)-$(RELEASE)

###############################################################################
#
# CFLAGS, LFLAGS and special prerequisites
#
###############################################################################

COMMON_OBJ_FILES := rem-bt.o rem-io.o rem-ps-handler.o rem-util.o \
	rem-pp-util.o rem-server.o
COMMON_HDR_FILES := rem-bt.h rem-io.h rem-ps-handler.h rem-util.h \
	rem-pp-util.h rem-pp.h rem.h

CFLAGS  = -DLOGLEVEL=$(LOGLEVEL) -DLOGTS
CFLAGS  += -DREM_RELEASE_MAIN=\"$(RELEASE_MAIN)\"
CFLAGS  += -DREM_RELEASE_SERVER=\"$(RELEASE_SERVER)\"
CFLAGS  += -DREM_RELEASE_PP=\"$(RELEASE_PP)\" -DREM_PP_NAME=\"$(PP_NAME)\"
CFLAGS	+= -DREM_DO_TESTS
CFLAGS	+= -Wall -Wshadow -Wmissing-prototypes -Wextra -O2
LFLAGS	= $(COMMON_OBJ_FILES) -lbluetooth

# Prerequisites needed for every server (independent of specific player proxy)
PREREQS = clean .built-server

# for player proxies reading music meta data from music files:
ifeq "$(strip $(USE_REM_TAG))" "yes"
PREREQS += .built-tag 
TAG_OBJ_FILES := rem-tags.o
TAG_HDR_FILES := rem-tags.h
TAG_CFLAGS := 
TAG_LFLAGS := -lid3tag $(TAG_OBJ_FILES)
CFLAGS += $(TAG_CFLAGS)
LFLAGS += $(TAG_LFLAGS)
endif

# for player proxies using dbus to communicate with music player
ifeq "$(strip $(USE_REM_DBUS))" "yes"
PREREQS += .built-dbus
#DBUS_OBJ_FILES :=
#DBUS_HDR_FILES :=
DBUS_OBJ_FILES := rem-dbus.o
DBUS_HDR_FILES := rem-dbus.h
DBUS_CFLAGS := $(shell pkg-config --cflags dbus-glib-1)
DBUS_LFLAGS := $(shell pkg-config --libs dbus-glib-1) $(DBUS_OBJ_FILES)
CFLAGS += $(DBUS_CFLAGS)
LFLAGS += $(DBUS_LFLAGS)
endif

###############################################################################
#
# Targets
#
###############################################################################

.PHONY: def all check clean server

all: check clean server

# Make the server prerequisites.
prereqs: $(PREREQS)

# Checks if a PP has been specified.
check:
	@if [ -z "$(PP_NAME)" ] ; then \
		echo "" ; \
		echo "You must specify a player proxy (PP) to build the server for"; \
		echo "Available PPs:"; \
		echo "$(PP_LIST)"; \
		echo "For example do 'make PP=xmms ...'"; \
		echo "" ; \
		@false; \
	fi

# Builds the common server components.
.built-server: $(COMMON_OBJ_FILES) $(COMMON_HDR_FILES)
	touch .built-server

# Builds the ID3 tag components.
.built-tag: $(TAG_OBJ_FILES) $(TAG_HDR_FILES)
	touch .built-tag

# Builds the DBUS components.
.built-dbus: $(DBUS_OBJ_FILES) $(DBUS_HDR_FILES)
	touch .built-dbus

clean:
	rm -f *~ *.o *.pyc $(PP_LIST_EXECUTABLES) .built-*
	rm -rf build

# Creates a binary duistribution package.
dist: clean server
	mkdir -p build/$(PKG_NAME)/bin build/$(PKG_NAME)/lib
	cp remuco-$(PP_NAME) $(PP_EXTRA_BIN_FILES) build/$(PKG_NAME)/bin
	if [ -n "$(PP_EXTRA_LIB_FILES)" ] ; then \
		cp $(PP_EXTRA_LIB_FILES) build/$(PKG_NAME)/lib; \
	fi
	echo "Server" > build/$(PKG_NAME)/lib/changelog
	echo >> build/$(PKG_NAME)/lib/changelog
	cat changelog.server >> build/$(PKG_NAME)/lib/changelog
	echo >> build/$(PKG_NAME)/lib/changelog
	echo "PP $(PP_NAME)" >> build/$(PKG_NAME)/lib/changelog
	echo >> build/$(PKG_NAME)/lib/changelog
	cat changelog.pp.$(PP_NAME) >> build/$(PKG_NAME)/lib/changelog
	cp COPYING build/$(PKG_NAME)/lib
	if [ -e "README.$(PP_NAME)" ] ; then \
		cp README.$(PP_NAME) build/$(PKG_NAME)/lib/README; \
	fi
	sed bin-dist.mk \
		-e "s/PP :=.*/PP := $(PP_NAME)/" > build/$(PKG_NAME)/Makefile
	mkdir -p dist
	tar zcf dist/$(PKG_NAME).tar.gz -C build $(PKG_NAME)

# This targets creates a new PP based on the template for C PPs.
# The user gets prompted for the PP's name.
create_pp_c:
	@echo -n "Enter PP name: " && read PPN ; \
	cp rem-pp-template_c.mk rem-pp-$${PPN}.mk ; \
	cp rem-pp-template_c.c rem-pp-$${PPN}.c ; \
	cp README.template_c README.$${PPN} ; \
	cp changelog.pp.template_c changelog.pp.$${PPN} ; \
	sed -i Makefile -e "s/^PP_LIST :=\(.*\)$$/PP_LIST :=\1 $${PPN}/"

# This targets creates a new PP based on the template for Python PPs.
# The user gets prompted for the PP's name.
create_pp_py:
	@echo -n "Enter PP name: " && read PPN ; \
	cp rem-pp-template_py.mk rem-pp-$${PPN}.mk ; \
	cp rem-pp-template_py.py rem-pp-$${PPN}.py ; \
	cp README.template_py README.$${PPN} ; \
	cp changelog.pp.template_py changelog.pp.$${PPN} ; \
	sed -i Makefile -e "s/^PP_LIST :=\(.*\)$$/PP_LIST :=\1 $${PPN}/"

# Prints out the release number of a specific server - PP combination.
release_no: check
	@echo "$(RELEASE)"
