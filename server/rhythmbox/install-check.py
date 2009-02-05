try:
    import gobject
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)
    
if IMPORT_ERROR != None:
    print(IMPORT_ERROR)

