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
        remuco.log_noise("rcb_synchronize (as method) called")
        return
    
    def xxx(self):
        return

def rcb_synchronize(priv, ps):
    ps.cap_pid = "112"
    remuco.log_noise("rcb_synchronize called")
    return

def rcb_get_plob(priv, pid):
    remuco.log_debug("rcb_get_plob(%s) called" % pid)
    return {'artist':'hans'}

def rcb_get_library(priv):
    remuco.log_debug("rcb_get_library called")
    return (['pl1', 'pl2'], ['dub', 'jazz'], [0 , 0])

def rcb_get_ploblist(priv, plid):
    remuco.log_debug("rcb_get_ploblist called")    
    return ['333', '4444']

def rcb_notify(priv, event):
    
    remuco.log_debug("rcb_notify called")

    if event == remuco.SERVER_EVENT_DOWN:
        remuco.log_debug("EVENT: server is down now")        
        priv.ml.quit()
    elif even == remuco.SERVER_EVENT_ERROR:
        remuco.log_debug("EVENT: server crashed")        
        remuco.down(priv.server)
    else:
        remuco.log_error("EVENT: unknown")
        sys.exit()
    return

def rcb_play_ploblist(priv, plid):
    remuco.log_debug("rcb_play_ploblist called")
    return

def rcb_search(priv, plob):
    remuco.log_debug("rcb_search called")
    return

def rcb_simple_control(priv, cmd, param):
    remuco.log_debug("rcb_simple_control called")
    return

def rcb_update_plob(priv, plob):
    remuco.log_debug("rcb_update_plob called")

def rcb_update_ploblist(priv, plid, pids):
    remuco.log_debug("rcb_update_ploblist called")
    return

def sighandler(signum, frame):
    remuco.log_debug("catched sig %i" % signum)
    remuco.down(ppp.server)

def main():
    
    global ppp

    priv = PlayerProxyPriv()
    
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
        remuco.log_error("error starting server")
        sys.exit()
    
    signal.signal(signal.SIGINT, sighandler)
    
    remuco.poll(priv.server)
    
    priv.ml = gobject.MainLoop()
    
    remuco.log_debug("run mainloop")

    priv.ml.run()
    
    remuco.log_debug("back from mainloop")
    
if __name__ == "__main__":
    
    main()