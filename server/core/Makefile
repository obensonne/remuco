all:
	python setup.py build --root=$(DESTDIR)/

install:
	python setup.py install --root=$(DESTDIR)/

install.log:

uninstall.files: install.log
	rm -rf $@
	touch $@
	@echo "create file list"
	@if [ -e install.log ] ; then \
		cat install.log | while read FILE ; do \
			if [ ! -d "$(DESTDIR)/$$FILE" ] ; then \
				echo "$(DESTDIR)/$$FILE" >> $@ ; \
			fi; \
		done; \
	fi

uninstall: uninstall.files
	@cat uninstall.files | while read FILE ; do \
		echo "rm -f $$FILE" ; \
		rm -f $$FILE || exit 1; \
	done;
	rm -f install.log
	
clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f
	@if [ -e install.log ] ; then \
		echo "Keep file 'install.log' for uninstallation."; \
	fi
	
