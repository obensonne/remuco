import sys
import gobject
import dircache

try:
    import remuco
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)

from remuco import log

class FooPlay(remuco.Player):
    
    def __init__(self):
        remuco.Player.__init__(self, "FooPlay")
    
    def request_playlist(self, client):
        """ Request the content of the currently active playlist. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a PLOB from the player.

        @see: reply_playlist_request() for sending back the result
        """
        log.debug("request_playlist()")
        self.reply_playlist_request(client, ["1", "2"], ["X - X", "AA - BB"])

    def request_queue(self, client):
        """ Request the content of the play queue. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a PLOB from the player.

        @see: reply_queue_request() for sending back the result
        """
        log.debug("request_queue()")
        self.reply_queue_request(client, [], [])

    def request_library(self, client, path):
        log.debug("request_library()")
        
        nested = []
        plob_ids = []
        plob_names = []

        dir = "/"
        for elem in path:
            dir = dir + elem + "/"
            
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

def fn(player):
    player.update_volume(23)
    return True

if __name__ == '__main__':
    
    if IMPORT_ERROR != None:
        log.error("missing python module (%s)" % IMPORT_ERROR)
    else:
        log.info("starting")
        
        p = FooPlay()
        
        ml = gobject.MainLoop()
        
        gobject.timeout_add(20000, fn, p)
        
        ml.run()
    