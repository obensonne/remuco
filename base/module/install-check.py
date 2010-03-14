import sys

try:
    import gobject
    if sys.platform.startswith("linux"):
        import xdg.BaseDirectory
        import dbus
    import Image
    import logging
    import bluetooth
except ImportError, e:
    print("")
    print("+-----------------------------------------------------------------+")
    print("| Unsatisfied Python requirement: %s." % e)
    print("| Please install the missing module and then retry.")
    print("+-----------------------------------------------------------------+")
    print("")
    sys.exit(1)