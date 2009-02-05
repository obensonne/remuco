import sys
import gobject


try:
    import remuco
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)

from remuco import log

class FooPlay(remuco.Player):
    
    pass

def fn(player):
    player.update_volume(23)
    return True

if __name__ == '__main__':
    
    if IMPORT_ERROR != None:
        log.error("missing python module (%s)" % IMPORT_ERROR)
    else:
        log.info("starting")
        
        p = remuco.Player("FooPlay")
        
        ml = gobject.MainLoop()
        
        gobject.timeout_add(20000, fn, p)
        
        ml.run()
    