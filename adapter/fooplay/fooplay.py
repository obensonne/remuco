#!/usr/bin/env python
"""FooPlay adapter for Remuco, implemented as an executable script.

This is a very simple adapter for the popular media player FooPlay. Use it as
a starting point for a new player adapter. Inspect the adapter API
documentation for help and other player adapters for inspirations. 

__author__ = "Oben Sonne <obensonne@googlemail.com>"
__copyright__ = "Copyright 2009, Oben Sonne"
__license__ = "GPL"
__version__ = "0.0.1"

"""

import remuco
from remuco import log

class FooPlayAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        remuco.PlayerAdapter.__init__(self, "FooPlay",
                                      playback_known=True,
                                      volume_known=True)
        
    def start(self):
        
        remuco.PlayerAdapter.start(self)

        log.debug("here we go")
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)

        log.debug("bye, turning off the light")
        
    def poll(self):
        
        import random
        
        volume = random.randint(0,100)
        self.update_volume(volume)
        
        playing = random.randint(0,1)
        if playing:
            self.update_playback(remuco.PLAYBACK_PLAY)
        else:
            self.update_playback(remuco.PLAYBACK_PAUSE)
        
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        
        log.debug("toggle FooPlay's playing status")
        
    # ...
        
    # =========================================================================
    # request interface
    # =========================================================================
    
    def request_playlist(self, client):
        
        self.reply_playlist_request(client, ["1", "2"],
                ["Joe - Joe's Song", "Sue - Sue's Song"])

    # ...
    
# =============================================================================
# main (example startup using remuco.Manager)
# =============================================================================

if __name__ == '__main__':
    
    pa = FooPlayAdapter() # create the player adapter
    mg = remuco.Manager(pa)# # pass it to a manager
    mg.run() # run the manager (blocks until interrupt signal)
