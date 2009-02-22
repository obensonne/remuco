#!/usr/bin/python

import sys
import gobject
import dircache
import signal

try:
    import remuco
    from remuco import log
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)

class FooPlay(remuco.Player):
    
    def __init__(self):
        
        # important: this must be called first:
        remuco.Player.__init__(self, "FooPlay")
        
        # example: periodic player state update function:
        sid = gobject.timeout_add(5000, self.__update_state)
        
        # important: remember source ids (for shutting down):
        self.__gobject_source_ids = (id,)
        
        # example: logging
        log.debug("FooPlay.__init__() done")
        
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
        
    def down(self):
        
        # important: call super class implementation:
        remuco.Player.down(self)
        
        # example: remove sources we've added to the gobject main loop:
        for source_id in self.__gobject_source_ids:
            gobject.source_remove(source_id)

    def __update_state(self):
        
        # example: update current volume
        self.update_volume(23)
        
        return True

# =============================================================================
# main (example startup)
# =============================================================================

ml = None

def sighandler(signum, frame):
    
    global ml
    
    log.info("received signal %i" % signum)
    
    if ml != None:
        ml.quit()
        
def main():
    
    signal.signal(signal.SIGINT, sighandler)
    signal.signal(signal.SIGTERM, sighandler)

    try:
        fp = FooPlay()
    except Exception, e:
        print("Failed to set up FooPlay player adapter: %s" % str(e))
        print("See remuco-fooplay log file for details.")
        return
    
    global ml
    
    ml = gobject.MainLoop()
    
    try:
        ml.run() # Remuco always needs a main loop to run
    except Exception, e:
        print("exception in main loop: %s" % str(e))
    
    # main loop has been stopped
    
    fp.down()

# -----------------------------------------------------------------------------

if __name__ == '__main__':
    
    if IMPORT_ERROR != None:
        log.error("missing a python module (%s)" % IMPORT_ERROR)
    else:
        log.info("starting")
        
    main()
