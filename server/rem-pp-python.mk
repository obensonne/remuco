PYTHON := python

PYTHON_VERSION := $(shell $(PYTHON) -V 2>&1 | awk {' print $$2 '} | \
		sed -e "s/^\([0-9]\+\.[0-9]\+\).*$$/\1/")

PP_CFLAGS := -DREM_PP_PYTHON_MODULE=\"rem-pp-$(PP_NAME)\"
PP_CFLAGS += -DPYTHON_VERSION=$(subst .,,$(PYTHON_VERSION))
PP_CFLAGS += -DPYTHON_PATH=\"$(REM_DIR)/$(PP_NAME)\"
PP_LFLAGS := -lpython$(PYTHON_VERSION)

.PHONY: server

server: prereqs rem-pp-python.c
	$(CC) -o remuco-$(PP_NAME) rem-pp-python.c $(CFLAGS) $(LFLAGS) \
		$(PP_CFLAGS) $(PP_LFLAGS)
	$(STRIP) remuco-$(PP_NAME)
	chmod +x remuco-$(PP_NAME)

install: server
	$(INSTALL_PROG) remuco-$(PP_NAME) $(DESTDIR)$(BIN_DIR)
	$(INSTALL) -d $(DESTDIR)$(REM_DIR)/$(PP_NAME)
	$(INSTALL_DATA) $(PY_EXTRA_FILES) rem.py rem-pp-$(PP_NAME).py \
		$(DESTDIR)$(REM_DIR)/$(PP_NAME)

PP_EXTRA_BIN_FILES :=
PP_EXTRA_LIB_FILES := rem.py rem-pp-$(PP_NAME).py $(PY_EXTRA_FILES)
