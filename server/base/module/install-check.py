import sys

try:
    import gobject
    import xdg.BaseDirectory
    import Image
    import logging
    import bluetooth
    import dbus
except ImportError, e:
    print("--> ERROR: Missing a required Python module (%s)!" % e)
    sys.exit(1)