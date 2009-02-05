try:
    import gobject
    import xdg.BaseDirectory
    import Image
    import logging
    import bluetooth
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)
    
if IMPORT_ERROR != None:
    print(IMPORT_ERROR)

