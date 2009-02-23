#!/usr/bin/python

import sys
import dircache
import signal
import gobject

import remuco
from remuco import log

class MPRIS(remuco.Player):
    
    def __init__(self, name):
        
        # important: this must be called first:
        remuco.Player.__init__(self, name)
        
        # example: periodic player state update function:
        sid = gobject.timeout_add(5000, self.__update_state)
        
        # important: remember source ids (for shutting down):
        self.__gobject_source_ids = (sid,)
        
        # example: logging
        log.debug("FooPlay.__init__() done")
        
    def request_playlist(self, client):
        
        # example: 2 tracks in the playlist
        self.reply_playlist_request(client, ["1", "2"],
                                    ["Joe - Joe's Song", "Sue - Sue's Song"])

    def request_queue(self, client):
        
        # example: empty queue
        self.reply_queue_request(client, [], [])

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
        
def main(name):
    
    signal.signal(signal.SIGINT, sighandler)
    signal.signal(signal.SIGTERM, sighandler)

    try:
        pa = MPRIS(name)
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
    
    pa.down()

# -----------------------------------------------------------------------------

if __name__ == '__main__':
    
    if len(sys.argv) >= 2:
        main(sys.argv[1])
    else:
        print "Need a player name!"
