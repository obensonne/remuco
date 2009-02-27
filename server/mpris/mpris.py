#!/usr/bin/python

import sys
import dircache
import signal

import gobject

import remuco
from remuco import log

class MPRISAdapter(remuco.PlayerAdapter):
    
    def __init__(self, name):
        
        remuco.PlayerAdapter.__init__(self, name)
        
        self.__name = name
        self.__gobject_source_ids = ()
        
        log.debug("init done")

    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        self.__gobject_source_ids = (
            gobject.timeout_add(5000, self.__update_state),
            )
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        for source_id in self.__gobject_source_ids:
            gobject.source_remove(source_id)
            
        self.__gobject_source_ids = ()

    def request_playlist(self, client):
        
        # example: 2 tracks in the playlist
        self.reply_playlist_request(client, ["1", "2"],
                                    ["Joe - Joe's Song", "Sue - Sue's Song"])

    def request_queue(self, client):
        
        # example: empty queue
        self.reply_queue_request(client, [], [])

    def __update_state(self):
        
        # example: update current volume
        self.update_volume(23)
        
        return True

# =============================================================================
# main
# =============================================================================

if __name__ == '__main__':
    
    if len(sys.argv) >= 2:
        name = sys.argv
        pa = MPRISAdapter(name)
        mg = remuco.Manager(pa, "org.mpris.%s", name)
        mg.run()
    else:
        print "Need a player name!"
