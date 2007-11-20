#!/usr/bin/python

import remuco
import gobject
import sys
import signal

ppp = None

class PlayerProxyPriv:
    def __init__(self):
        self.ml = None
        self.server = None

    def rcb_synchronize(self, ps):
        # self == priv !
        print "rcb_synchronize (as method) called"
        return
    
    def xxx(self):
        return

def rcb_synchronize(priv, ps):
    ps.cap_pid = "112"
    print "rcb_synchronize called"
    return

def rcb_get_plob(priv, pid):
    print "rcb_get_plob(%s) called" % pid
    return {'artist':'hans'}

def rcb_get_library(priv):
    print "rcb_get_library called"
    return (['pl1', 'pl2'], ['dub', 'jazz'], [0 , 0])

def rcb_get_ploblist(priv, plid):
    print "rcb_get_ploblist called"    
    return ['333', '4444']

def rcb_notify(priv, event):
    
    print "rcb_notify called "

    if event == remuco.SERVER_EVENT_DOWN:
        print "EVENT: server is down now"        
        priv.ml.quit()
    elif even == remuco.SERVER_EVENT_ERROR:
        print "EVENT: server crashed"        
        remuco.down(priv.server)
    else:
        print "ERROR: unknown event"
        sys.exit()
    return

def rcb_play_ploblist(priv, plid):
    print "rcb_play_ploblist called"
    return

def rcb_search(priv, plob):
    print "rcb_search called"
    return

def rcb_simple_control(priv, cmd, param):
    print "rcb_simple_control called"
    return

def rcb_update_plob(priv, plob):
    print "rcb_update_plob called"

def rcb_update_ploblist(priv, plid, pids):
    print "rcb_update_ploblist called"
    return

def sighandler(signum, frame):
    remuco.down(ppp.server)

if __name__ == "__main__":
    
    priv = PlayerProxyPriv()
    
    global ppp
    ppp = priv
    
    callbacks = remuco.PPCallbacks()
    
    callbacks.snychronize = rcb_synchronize
    callbacks.get_plob = rcb_get_plob
    callbacks.notify = rcb_notify
    callbacks.simple_control = rcb_simple_control
    
    descriptor = remuco.PPDescriptor()
    
    descriptor.player_name = "test"
    
    try:
        priv.server = remuco.up(descriptor, callbacks, priv)
    except:
        print "error starting server"
        sys.exit()
    
    signal.signal(signal.SIGINT, sighandler)
    
    remuco.poll(priv.server)
    
    priv.ml = gobject.MainLoop()
    
    print "run mainloop"

    priv.ml.run()
    
    print "back from mainloop"
    
