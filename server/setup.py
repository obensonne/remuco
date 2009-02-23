from distutils.core import setup

import sys


if len(sys.argv) > 1 and "install" in sys.argv:
    
    # check for required modules
    try:
        import gobject
        import xdg.BaseDirectory
        import Image
        import logging
        import bluetooth
    except ImportError, e:
        print("Missing a required python module: %s" % str(e))
        sys.exit(1)
        
    # ensure installed files get logged:
    sys.argv.append("--record")
    sys.argv.append("install.log")

setup(name='remuco',
      version='0.8.0',
      description='Module for Remuco player adapters.',
      author='Oben Sonne',
      author_email='obensonne@googlemail.com',
      url='http://remuco.sourcefourge.net',
      packages=['remuco'],
      )

