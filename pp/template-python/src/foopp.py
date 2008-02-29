#!/usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.6.0"
__copyright__ = "Copyright (c) 2008 Christian Bünnig"
__license__ = "GPL2"

__doc__ = """
Template python player proxy. This PP is a stand-alone script, just make it
executable and start it. Of course in its current state it does not do very
much, but at least it is already in a working state.

To adapt to a specific player, implement the functions rcb_...

It is strongly recommended to read the API documentation in the server library
source package and in the python binding source package.

Good luck :) !

Come to the help forum to get support.
"""



import gobject
import os
import sys
import signal

import remuco

# this class is just to box some data we can pass to the server so that it gives
# us back the data when the server calls functions of this PP
class FooPP():

    def __init__(self):
        
        self.conn = None # e.g. a connection to player Foo
        self.server = None
        self.ml = None # the GMainLoop
        self.shutdown_in_progress = False

def gcb_tick(fpp):
    """Timer callback function to periodically check the player for some changes
    in its state (e.g. playback, current plob/song, volume etc.).
    """
    
    # check if there are some changes in the player
    
    # if yes call:
    # remuco.notify(fpp.server)
    # this will result in call to rcb_synchronize()
    
    # note: it's ok always to call remuco.notify(fpp.server) without checking
    #       for changes as the server _also_ checks if there is a change

###########################################################################
#
# Remuco Callbacks
#
###########################################################################

def rcb_synchronize(fpp, ps):
    """The server requests to sync the player state."""
    
    remuco.log_debug("rcb_synchronize called")
    
    # example:
    ps.pbs = remuco.PS_PBS_PAUSE
    ps.volume = 57
    ps.flags = remuco.PS_FLAG_REPEAT | remuco.PS_FLAG_REPEAT
    ps.cap_pid = "14FC28D"
    ps.cap_pos = 2
    ps.playlist = ["34DC22", "14FC28D", "F4432"] # pid list
    ps.queue = []

    remuco.log_debug("rcb_synchronize done")
    
def rcb_get_plob(fpp, pid):
    """The server requests meta data of the plob with id 'pid'."""

    remuco.log_debug("rcb_get_plob(%s) called" % pid)
    
    #example:
    plob = {remuco.PLOB_META_TITLE : "Lonesome Day Blues", \
            remuco.PLOB_META_ARTIST: "Bob Dylan", \
            remuco.PLOB_META_ART : "/path/to/image-file" \
           }

    return plob
    
def rcb_notify(fpp, event):
    """The server wants to say something."""
    
    remuco.log_debug("rcb_notify called ")

    if event == remuco.SERVER_EVENT_DOWN:
        remuco.log_debug("EVENT: server is down now")     
        fpp.ml.quit() # stop the main loop now
    elif even == remuco.SERVER_EVENT_ERROR:
        remuco.log_debug("EVENT: server crashed")        
        remuco.down(fpp.server)
    else:
        remuco.log_warn("EVENT: unknown")
    return

def rcb_simple_control(fpp, cmd, param):
    """The server wants to control the player."""
    
    remuco.log_debug("rcb_simple_control(%i, %i) called" % (cmd, param))

    if cmd == remuco.SCTRL_CMD_STOP:
        
        remuco.log_debug("stop playback")
        
    elif cmd == remuco.SCTRL_CMD_PLAYPAUSE:
        
        remuco.log_debug("toggle play/pause")
        
    elif cmd == remuco.SCTRL_CMD_NEXT:
        
        remuco.log_debug("switch to next song")
        
    elif cmd == remuco.SCTRL_CMD_PREV:
        
        remuco.log_debug("switch to previous song")
        
    elif cmd == remuco.SCTRL_CMD_VOLUME:
        
        remuco.log_debug("set volume")
        
        remuco.notify(fpp.server) # volume change
        
    elif cmd == remuco.SCTRL_CMD_JUMP:
        
        if param > 0:
            remuco.log_debug("jump to a position in playlist")            
        elif param < 0:
            remuco.log_debug("jump to a position in queue")            
        else:
            remuco.log_warn("jump to position 0 is invalid")            
        
    elif cmd == remuco.SCTRL_CMD_RATE:
        
        remuco.log_warn("rate current plob")            

    else:
        remuco.log_warn("command %d not supported" % cmd)

    # all of the above cause a change in player state -> notify server
    remuco.notify(fpp.server)


def rcb_get_library(fpp):
    """The server requests the library."""
    
    plids = [] # a list ids of all ploblists
    names = [] # a list names of all ploblists
    flags = [] # a list of flags of all ploblists
    
    # example:
    plids = ["PL1", "PL2", "PL3" ]
    names = ["Jazz", "MyMix", "Chill" ]
    flags = [ 0, 0, 0 ] # currently only 0 makes sense
    
    return (plids, names, flags)

def rcb_play_ploblist(fpp, plid):
    """The server requests to player a certain ploblist."""

    remuco.log_debug("rcb_play_ploblist(%s) called" % plid)
                    
def rcb_get_ploblist(fpp, plid):
    """The server requests a certain ploblist."""
    
    remuco.log_debug("rcb_get_ploblist(%s) called" % plid)

    # example:
    pl = [ "PLOB1", "PLOB2", "PLOB3" ]

    return pl

###########################################################################
#
# Main
#
###########################################################################

fpp_global = None

def sighandler(signum, frame):
    remuco.log_debug("got signal %i" % signum)
    if not fpp_global.shutdown_in_progress:
        fpp_global.shutdown_in_progress = True
        remuco.down(fpp_global.server)

def main():
    
    remuco.log_info("Foo-PP started")
    
    ###### misc initializations ######

    signal.signal(signal.SIGINT, sighandler)
    signal.signal(signal.SIGTERM, sighandler)
    
    fpp = FooPP()
    
    global fpp_global
    fpp_global = fpp
    
    fpp.ml = gobject.MainLoop()
    
    ###### remuco related ######
    
    # tell the server which functions to call if it wants to talk to us
    callbacks = remuco.PPCallbacks()
    callbacks.snychronize = rcb_synchronize
    callbacks.get_plob = rcb_get_plob
    callbacks.notify = rcb_notify
    callbacks.simple_control = rcb_simple_control
    callbacks.get_library = rcb_get_library # optional
    callbacks.play_ploblist = rcb_play_ploblist # optional
    callbacks.get_ploblist = rcb_get_ploblist # optional
    
    # describe the player/pp
    descriptor = remuco.PPDescriptor()
    descriptor.player_name = "Foo"
    descriptor.max_rating_value = 5
    descriptor.supports_playlist = True
    descriptor.supports_playlist_jump = True
    descriptor.supports_queue = False
    descriptor.supports_queue_jump = False
    
    ###### set up remuco server ######

    try:
        fpp.server = remuco.up(descriptor, callbacks, fpp)
    except:
        fpp.server = None
        remuco.log_error("error starting server: %s" % str(sys.exc_info()))
        sys.exit()

    # initially notify the server that there are changes in player state
    remuco.notify(fpp.server)

    # periodically check for change in player state (if a specific player
    # automatically signals changes, you may not need this)
    gobject.timeout_add(2000, gcb_tick, fpp)

    remuco.log_debug("run mainloop")
    
    fpp.ml.run()
    
    remuco.log_debug("back from mainloop .. bye")

if __name__ == "__main__":
    
    main()
    