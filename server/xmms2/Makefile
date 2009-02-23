X2_STARTUP_DIR ?= $(HOME)/.config/xmms2/startup.d

MODULES := xmmsclient

check:
	@for MOD in $(MODULES) ; do \
		echo "import $$MOD" | python 2>/dev/null || \
			{ echo "ERROR: missing python module $$MOD"; exit 1; } \
	done

install: check
	mkdir -p $(X2_STARTUP_DIR)
	install -T xmms2.py $(X2_STARTUP_DIR)/remuco-xmms2
	
uninstall:
	rm -f $(X2_STARTUP_DIR)/remuco-xmms2
