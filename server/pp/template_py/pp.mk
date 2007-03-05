###############################################################################
#
# PP TEMPLATE (for a Python pp)
#
###############################################################################

# Release version of the PP relative to the main release (see var
# $(RELEASE_MAIN) in 'Makefile')
RELEASE_PP := 0

# Thats for the targets 'install' and 'dist'. Per default the server binary
# 'remuco-$(PP_NAME)' and the python files 'pp/$(PP_NAME)/pp.py' and 'rem.py'
# are included in the installation process. If further file are needed they
# must be listed here (relative to pp/$(PP_NAME)/). Example:
# PY_EXTRA_FILES := pputil.py
PY_EXTRA_FILES :=

######################## DO NOT CHANGE ########################################

include rem-pp-python.mk

