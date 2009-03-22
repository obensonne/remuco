all:
	python setup.py build --root=$(DESTDIR)/

install: install.log

install.log: setup.py remuco/*.py
	python setup.py install --root=$(DESTDIR)/ --record install.log

uninstall:
	@if [ ! -e install.log ] ; then \
		echo "No 'install.log' -> not yet installed or already uninstalled."; \
	else \
		for FILE in `cat install.log` ; do \
			FILE=$(DESTDIR)/$$FILE ; \
			if [ -f $$FILE ] ; then rm -f $$FILE || exit 1; fi ; \
		done ; \
		rm -f install.log || exit 1; \
	fi

clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f
	@if [ -e install.log ] ; then \
		echo "Preserve 'install.log' for uninstallation."; \
	fi

test-module:
	PYTHONPATH=$$PYTHONPATH:. python test/testall.py