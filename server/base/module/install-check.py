import sys

try:
    import gobject
    import xdg.BaseDirectory
    import Image
    import logging
    import bluetooth
    import dbus
except ImportError, e:
    print("")
    print("+-----------------------------------------------------------------+")
    print("| Unsatisfied Python requirement: %s." % e)
    print("| Please install the missing module and then retry.")
    print("+-----------------------------------------------------------------+")
    print("")
    sys.exit(1)