def: all

###############################################################################
#
# Settings (common / global)
#
###############################################################################

# list of available PPs
PP_LIST := template_c template_py xmms xmms2 
PP_LIST_EXECUTABLES := $(patsubst %,remuco-%,$(PP_LIST))

# main release number:
RELEASE_MAIN := 0.4
# server sub release (relative to main release)
RELEASE_SERVER := 0
# player proxy sub release (relative to main release) gets defined below within
# the specific PP targets

# loglevel: LL_FATAL, LL_ERROR, LL_WARN, LL_INFO, LL_DEBUG, LL_NOISE
LOGLEVEL ?= LL_DEBUG

# installation destination (already important when building !)
PREFIX	:= /usr/local
LIB_DIR	:= $(PREFIX)/lib
BIN_DIR := $(PREFIX)/bin
REM_DIR := $(LIB_DIR)/remuco

# --- COMMANDS -------------------------------------------------------------- #

INSTALL := install
INSTALL_PROG := $(INSTALL)
INSTALL_DATA := $(INSTALL) -m 664

STRIP := strip
#STRIP := touch

# --- COMPILE AND LINK FLAGS ------------------------------------------------ #

COMMON_OBJ_FILES := rem-bt.o rem-io.o rem-ps-handler.o rem-util.o \
	rem-pp-util.o rem-server.o
COMMON_HDR_FILES := rem-bt.h rem-io.h rem-ps-handler.h rem-util.h \
	rem-pp-util.h rem-pp.h rem.h

# for player proxies reading music meta data from music files:
TAG_OBJ_FILES := rem-tags.o
TAG_HDR_FILES := rem-tags.h
TAG_LFLAGS := -lid3tag $(TAG_OBJ_FILES)

# --- TARGETS 1 ------------------------------------------------------------- #

# --- PP specific data ------------------------------------------------------ #

PP_NAME := ${PP}
ifneq "$(PP_NAME)" ""
include rem-pp-$(PP_NAME).mk
endif

RELEASE := $(RELEASE_MAIN).$(RELEASE_SERVER).$(RELEASE_PP)
PKG_NAME := remuco-server-$(PP_NAME)-$(RELEASE)

# --- MORE COMPILE AND LINK FLAGS ------------------------------------------- #

CC		:= gcc
CFLAGS  := -DLOGLEVEL=$(LOGLEVEL) -DLOGTS
CFLAGS  += -DREM_RELEASE_MAIN=\"$(RELEASE_MAIN)\"
CFLAGS  += -DREM_RELEASE_SERVER=\"$(RELEASE_SERVER)\"
CFLAGS  += -DREM_RELEASE_PP=\"$(RELEASE_PP)\" -DREM_PP_NAME=\"$(PP_NAME)\"
CFLAGS	+= -DREM_DO_TESTS
CFLAGS	+= -Wall -Wshadow -Wmissing-prototypes -Wextra -O2
LFLAGS	:= $(COMMON_OBJ_FILES) -lbluetooth

###############################################################################
#
# Targets 2
#
###############################################################################

.PHONY: def all check clean server

all: check clean server

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

.built-server: $(COMMON_OBJ_FILES) $(COMMON_HDR_FILES)
	touch .built-server

.built-tag: $(TAG_OBJ_FILES) $(TAG_HDR_FILES)
	touch .built-tag

clean:
	rm -f *~ *.o *.pyc $(PP_LIST_EXECUTABLES) .built-*
	rm -rf build

dist: clean server
	mkdir -p build/$(PKG_NAME)/{bin,lib}
	cp remuco-$(PP_NAME) $(PP_EXTRA_BIN_FILES) build/$(PKG_NAME)/bin
	if [ -n "$(PP_EXTRA_LIB_FILES)" ] ; then \
		cp $(PP_EXTRA_LIB_FILES) build/$(PKG_NAME)/lib; \
	fi
	cp COPYING build/$(PKG_NAME)/lib
	if [ -e "README.$(PP_NAME)" ] ; then \
		cp README.$(PP_NAME) build/$(PKG_NAME)/lib/README; \
	fi
	sed bin-dist.mk \
		-e "s/PP :=.*/PP := $(PP_NAME)/" > build/$(PKG_NAME)/Makefile
	mkdir -p dist
	tar zcf dist/$(PKG_NAME).tar.gz -C build $(PKG_NAME)

release_no: check
	@echo "$(RELEASE)"