all:
	python setup.py build --root=$(DESTDIR)/

install: install.log

install.log: setup.py remuco/*.py
	python setup.py install --root=$(DESTDIR)/ --record install.log 

uninstall:
	@echo ""
	@echo "+----------------------------------------------------------------+"
	@echo "| To uninstall the Remuco Python module, delete the files listed |"
	@echo "| in 'install.log' manually.                                     |"
	@echo "+----------------------------------------------------------------+"
	@echo ""
	
clean:
	rm -rf build dist
	find -type f -name "*.pyc" | xargs rm -f
	@if [ -e install.log ] ; then \
		echo ""; \
		echo "+----------------------------------------------------------------+"; \
		echo "| Keeping 'install.log' for uninstallation.                      |"; \
		echo "+----------------------------------------------------------------+"; \
		echo ""; \
	fi
