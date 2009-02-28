#!/usr/bin/python

import remuco
from remuco import log

class FooPlayAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        # important: call super class implementation first:
        remuco.PlayerAdapter.__init__(self, "FooPlay")
        
    def start(self):
        
        # important: call super class implementation first:
        remuco.PlayerAdapter.start(self)

        # example: logging
        log.debug("start done")
        
    def stop(self):
        
        # important: call super class implementation first:
        remuco.PlayerAdapter.stop(self)

    def poll(self):
        
        # example: get volume and update (delete this method if not needed)
        import random
        volume = random.randint(0,100)
        self.update_volume(volume)
        
        return True
    
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        
        log.debug("toggle FooPlay's playing status")
        
    # TODO: implement all 'ctrl_...' methods useful for FooPlay
    
    # =========================================================================
    # request interface
    # =========================================================================
    
    def request_plob(self, client, id):
        
        # example: reply (delete this method if not supported)
        
        self.reply_plob_request(client, id,
                { remuco.INFO_ARTIST: "Joe", remuco.INFO_TITLE: "Joe's Song" })
        
    def request_playlist(self, client):
        
        # example: reply (delete this method if not supported)
        
        self.reply_playlist_request(client, ["1", "2"],
                ["Joe - Joe's Song", "Sue - Sue's Song"])

    # TODO: implement all 'request_...' methods useful for FooPlay
    
    
# =============================================================================
# main (example startup using remuco.ScriptManager)
# =============================================================================

if __name__ == '__main__':
    
    # create the player adapter
    pa = FooPlayAdapter()
    
    # pass it to a manager
    mg = remuco.Manager(pa)
    
    # run the manager (blocks until interrupt signal)
    mg.run()
