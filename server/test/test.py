import Image
import logging
import os, sys
import xdg.BaseDirectory
import tempfile
import gobject

import m1
import m2

def cb(name):
    
    print("Hallo %s" % name)
    
    return

if __name__ == '__main__':
    
    gobject.idle_add(cb, "hans")
    
    ml = gobject.MainLoop()
    
    ml.run()

    