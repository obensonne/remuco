###############################################################################
#
# Settings (common / global)
#
###############################################################################

VERSION := 0.4.0

PREFIX	:= /usr/local
LIB_DIR	:= $(PREFIX)/lib
BIN_DIR := $(PREFIX)/bin
REM_DIR := $(LIB_DIR)/remuco

LOGLEVEL := LL_NOISE

STRIP := strip
#STRIP := touch

COMMON_OBJ_FILES := rem-bt.o rem-io.o rem-ps-handler.o rem-util.o rem-pp-util.c rem-server.o
COMMON_HDR_FILES := rem-bt.h rem-io.h rem-ps-handler.h rem-util.h rem-pp-util.h rem-pp.h rem.h

PP_LIST := xmms template xmms2 amarok template_py
PP_LIST_EXECUTABLES := $(patsubst %,remuco-%,$(PP_LIST))

CC		:= gcc
CFLAGS  := -DLOGLEVEL=$(LOGLEVEL) -DLOGTS -DREM_VER=\"$(VERSION)\"
CFLAGS	+= -DREM_DO_TESTS
CFLAGS	+= -Wall -Wshadow -Wmissing-prototypes -Wextra -O2
LFLAGS	:= $(COMMON_OBJ_FILES) -lbluetooth

###############################################################################
#
# Settings (for player proxies, reading music meta data from music files)
#
###############################################################################

TAG_OBJ_FILES := rem-tags.o
TAG_HDR_FILES := rem-tags.h
TAG_LFLAGS := -lid3tag $(TAG_OBJ_FILES)

###############################################################################
#
# Misc targets
#
###############################################################################

all: help

.built-server: $(COMMON_OBJ_FILES) $(COMMON_HDR_FILES)
	touch .built-server

.built-tag: $(TAG_OBJ_FILES) $(TAG_HDR_FILES)
	touch .built-tag

all-pp: $(PP_LIST)

clean:
	rm -f *~ *.o *.pyc $(PP_LIST_EXECUTABLES) .built-*
	rm -rf build

rebuild: clean all-pp

help:
	@echo "Run target"
	@echo "  PLAYER to build the server binary for the music player PLAYER"
	@echo "  PLAYER_install to install the server binary for the music player PLAYER"
	@echo "  PLAYER_bindist to create a binary distribution package of the"
	@echo "    server binary for the music player PLAYER including all related"
	@echo "    files."
	@echo ""
	@echo "Replace PLAYER by a specific music player."
	@echo "Supported music players: $(PP_LIST)"

###############################################################################
#
# PP XMMS
#
###############################################################################

XMMS_CFLAGS := $(shell xmms-config --cflags)
XMMS_LFLAGS := $(shell xmms-config --libs) $(TAG_LFLAGS)

xmms: .built-server .built-tag rem-pp-xmms.c
	$(CC) -o remuco-xmms rem-pp-xmms.c $(CFLAGS) $(LFLAGS) \
		$(XMMS_CFLAGS) $(XMMS_LFLAGS)
	$(STRIP) remuco-xmms
	chmod +x remuco-xmms

xmms_install: xmms
	install remuco-xmms $(DESTDIR)$(BIN_DIR)
	
xmms_bindist:
	BINDIST_PP=xmms BINDIST_BIN_FILES=remuco-xmms make bindist
		
###############################################################################
#
# PP XMMS2
#
###############################################################################

XMMS2_CFLAGS := $(shell pkg-config --cflags xmms2-client)
XMMS2_LFLAGS := $(shell pkg-config --libs xmms2-client)

xmms2: .built-server rem-pp-xmms2.c
	$(CC) -o remuco-xmms2 rem-pp-xmms2.c $(CFLAGS) $(LFLAGS) \
		$(XMMS2_CFLAGS) $(XMMS2_LFLAGS)
	$(STRIP) remuco-xmms2
	chmod +x remuco-xmms2

xmms2_install: xmms2
	install remuco-xmms2 $(DESTDIR)$(BIN_DIR)	

xmms2_bindist:
	BINDIST_PP=xmms2 BINDIST_BIN_FILES=remuco-xmms2 make bindist

###############################################################################
#
# PP TEMPLATE (for a C pp)
#
# This is template target to use for a player proxy (PP) written in C, i.e. a
# PP based on rem-pp-template.c.
# Replace 'template' with the name of the corresponding music player.
#
###############################################################################

TEMPLATE_CLFAGS :=
TEMPLATE_LFLAGS :=

template: .built-server rem-pp-template.o
	$(CC) -o remuco-template rem-pp-template.o $(CFLAGS) $(LFLAGS) \
		$(TEMPLATE_CLFAGS) $(TEMPLATE_LFLAGS)
	$(STRIP) remuco-template
	chmod +x remuco-template

template_install: template
	install remuco-template $(DESTDIR)$(BIN_DIR)

template_bindist:
	BINDIST_PP=template BINDIST_BIN_FILES=remuco-template \
		BINDIST_LIB_FILES= make bindist

###############################################################################
#
# PP PYTHON (not to call directly)
#
###############################################################################

PYTHON := python

PYTHON_VERSION := $(shell $(PYTHON) -V 2>&1 | awk {' print $$2 '} | \
		sed -e "s/^\([0-9]\+\.[0-9]\+\).*$$/\1/")

PYTHON_FILES := ${PYTHON_FILES}

PP_NAME := ${PP_NAME}
PP_VERSION := ${PP_VERSION}

PYTHON_CLFAGS := -DPP_NAME=\"$(PP_NAME)\" -DPP_VERSION=\"$(PP_VERSION)\"
PYTHON_CLFAGS += -DREM_PP_PYTHON_MODULE=\"rem-pp-$(PP_NAME)\"
PYTHON_CLFAGS += -DPYTHON_VERSION=$(subst .,,$(PYTHON_VERSION))
PYTHON_CLFAGS += -DPYTHON_PATH=\"$(REM_DIR)/$(PP_NAME)\"
PYTHON_LFLAGS := -lpython$(PYTHON_VERSION)

.notthere:

python: .built-server rem-pp-python.c .notthere
	$(CC) -o remuco-$(PP_NAME) rem-pp-python.c $(CFLAGS) $(LFLAGS) \
		$(PYTHON_CLFAGS) $(PYTHON_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

python_install: python
	install remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	install -d $(DESTDIR)$(REM_DIR)/$(PP_NAME)
	cp $(PY_EXTRA_FILES) rem.py rem-pp-$(PP_NAME).py \
		$(DESTDIR)$(REM_DIR)/$(PP_NAME)

###############################################################################
#
# PP TEMPLATE (for a Python pp)
#
# This is template target to use for a player proxy (PP) written in Python,
# i.e. a PP based on rem-pp-template.py.
# Replace 'template_py' and 'template' with the name of the corresponding music
# player.
#
###############################################################################

template_py:
	PP_NAME="template" PP_VERSION="0.4.0" make python

template_py_install: template_py
	PP_NAME="template" PP_VERSION="0.4.0" \
		PY_EXTRA_FILES="" make python_install

template_py_bindist:
	BINDIST_PP=template BINDIST_BIN_FILES=remuco-template \
		BINDIST_LIB_FILES="rem.py rem-pp-template.py" make bindist
		
	
###############################################################################
#
# PP AMAROK (a python pp)
#
# TODO: create rem-pp-amarok.py !
#
###############################################################################

amarok:
	PP_NAME="amarok" PP_VERSION="0.4.0" make python

amarok_install: amarok
	PP_NAME="amarok" PP_VERSION="0.4.0" make python_install

amarok_bindist:
	BINDIST_PP=amarok BINDIST_BIN_FILES=remuco-amarok \
		BINDIST_LIB_FILES="rem.py rem-pp-amarok.py" make bindist

###############################################################################
#
# Create binary distribution package (not to call directly, instead call
# target PLAYER_bindist)
#
###############################################################################

BINDIST_BIN_FILES := ${BINDIST_BIN_FILES}
BINDIST_LIB_FILES := ${BINDIST_LIB_FILES}
BINDIST_PP := ${BINDIST_PP}

bindist: clean $(BINDIST_PP)
	mkdir -p build/remuco-$(BINDIST_PP)-$(VERSION)/{bin,lib}
	if [ -n "$(BINDIST_BIN_FILES)" ] ; then \
		cp $(BINDIST_BIN_FILES) build/remuco-$(BINDIST_PP)-$(VERSION)/bin; \
	fi
	if [ -n "$(BINDIST_LIB_FILES)" ] ; then \
		cp $(BINDIST_LIB_FILES) build/remuco-$(BINDIST_PP)-$(VERSION)/lib; \
	fi
	cp COPYING build/remuco-$(BINDIST_PP)-$(VERSION)/lib
	if [ -e "README.$(BINDIST_PP)" ] ; then \
		cp README.$(BINDIST_PP) build/remuco-$(BINDIST_PP)-$(VERSION)/lib; \
	fi
	sed Makefile.bindist -e "s/PP :=.*/PP := $(BINDIST_PP)/" \
		> build/remuco-$(BINDIST_PP)-$(VERSION)/Makefile
	mkdir -p dist
	tar zcf dist/remuco-$(BINDIST_PP)-$(VERSION).tgz -C build \
		remuco-$(BINDIST_PP)-$(VERSION)

###############################################################################
#
# Temp / Test
#
###############################################################################

#xmms2_ld: .built-server rem-pp-xmms2.c rem-pp-util.c
#	$(CC) -fPIC -c rem-pp-xmms2.c $(XMMS2_CFLAGS) $(XMMS2_LFLAGS)
#	$(CC) -fPIC -c rem-pp-util.c $(XMMS2_CFLAGS) $(XMMS2_LFLAGS)
#	$(CC) -shared -o libremppxmms2.so rem-pp-xmms2.o  rem-pp-util.o \
#		$(XMMS2_CFLAGS) $(XMMS2_LFLAGS)
#	$(CC) -o remuco $(LFLAGS) -lremppxmms2
