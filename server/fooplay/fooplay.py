#!/usr/bin/python

import sys
import dircache
import signal

import gobject

import remuco
from remuco import log

class FooPlayAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        # important: this must be called first:
        remuco.PlayerAdapter.__init__(self, "FooPlay")
        
    def start(self):
        
        # important: call super class implementation:
        remuco.PlayerAdapter.start(self)

        # example: periodic player state update function:
        self.__gobject_source_ids = (
            gobject.timeout_add(5000, self.__update_state)
            ,
        )
        
        # example: logging
        log.debug("start done")
        
    def stop(self):
        
        # important: call super class implementation:
        remuco.PlayerAdapter.stop(self)
        
        # example: remove sources we've added to the gobject main loop:
        for source_id in self.__gobject_source_ids:
            gobject.source_remove(source_id)

    def request_playlist(self, client):
        
        # example: 2 tracks in the playlist
        self.reply_playlist_request(client, ["1", "2"],
                                    ["Joe - Joe's Song", "Sue - Sue's Song"])

    def request_queue(self, client):
        
        # example: empty queue
        self.reply_queue_request(client, [], [])

    def request_library(self, client, path):
        
        # example: show file system as media player library
        
        nested = []
        plob_ids = []
        plob_names = []

        dir = "/"
        for elem in path:
            dir += elem + "/"
            
        try:
            list = dircache.listdir(dir)
        except OSError, e:
            self.reply_library_request(client, path, nested, plob_ids, plob_names)
            return
              
        
        list = list[:]
        dircache.annotate(dir, list)
        
        for elem in list:
            if elem.endswith('/'):
                nested.append(elem[:(len(elem)-1)])
            else:
                plob_ids.append(elem)
                plob_names.append(elem)
        
        self.reply_library_request(client, path, nested, plob_ids, plob_names)
        
    def __update_state(self):
        
        # example: update current volume
        self.update_volume(23)
        
        return True

# =============================================================================
# main (example startup using remuco.ScriptManager)
# =============================================================================

if __name__ == '__main__':
    
    # create the player adapter
    pa = FooPlayAdapter()
    
    # pass it to a manager
    mg = remuco.Manager(pa)
    
    # run the manager (blocks until interrupt signal)
    mm.run()
