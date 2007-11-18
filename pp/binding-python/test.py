#!/usr/bin/python

import remuco
import gobject, gtk
import sys

class PlayerProxyPriv:
    def __init__(self):
        self.ml = None
    

def rcb_synchronize(priv, ps):
    print "rcb_synchronize called"
    return

def rcb_get_plob(priv, pid):
    print "rcb_synchronize called"
    return

def rcb_get_library():
    print "rcb_synchronize called"
    return

def rcb_get_ploblist():
    print "rcb_synchronize called"    
    return

def rcb_notify(priv, event):
    print "rcb_synchronize called"
    return

def rcb_play_ploblist():
    print "rcb_synchronize called"
    return

def rcb_search():
    print "rcb_synchronize called"
    return

def rcb_simple_control():
    print "rcb_synchronize called"
    return

def rcb_update_plob():
    print "rcb_synchronize called"
    return

def rcb_update_ploblist():
    print "rcb_synchronize called"
    return

if __name__ == "__main__":
    
    priv = PlayerProxyPriv()
    
    callbacks = remuco.PPCallbacks()
    
    callbacks.snychronize = rcb_synchronize
    callbacks.get_plob = rcb_get_plob
    callbacks.notify = rcb_notify

    descriptor = remuco.PPDescriptor()
    
    descriptor.player_name = "test"
    
    print "here 1"
    
    try:
        server = remuco.up(descriptor, callbacks, None)
    except:
        print "error starting server"
        sys.exit()
    
    print "here 2"
    
    remuco.poll()
    
    priv.ml = gobject.MainLoop()
    
    priv.ml.run()
    
    print "hm"
    