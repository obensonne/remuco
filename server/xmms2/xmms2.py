import sys
import os
import gobject
import xmmsclient
import xmmsclient.glib

try:
    import remuco
    from remuco import log
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)


# =============================================================================
# helper functions
# =============================================================================

def x2_result_check(result):
    """ Check the result of a request send to XMMS2. """
    
    if not result.iserror():
        return True
    
    err = result.get_error()
    
    if err.lower() == XMMS2.ERROR_DISCONNECTED:
        log.warning("lost connection to XMMS2")
        global ml
        ml.quit()
    else:
        log.warning("error result: %s" % err)
    
    return False

def x2_result_control(result):
    """ Check the result of a control command sent to XMMS2. """
    
    if not result.iserror():
        return
    
    err = result.get_error()
    
    if err.lower() == XMMS2.ERROR_DISCONNECTED:
        log.warning("lost connection to XMMS2")
        global ml
        ml.quit()
    else:
        log.warning("error on XMMS2 control: %s" % err)

def x2_result_to_plob(result):
    """ Convert a medialib info result to a plob. """
    
    def get_meta(key):
        try:
            val = minfo[key]
        except KeyError:
            return ""
        if key == "duration":
            val = val / 1000
        return str(val)

    minfo = result.value()
    meta = {}
    meta[remuco.INFO_ARTIST] = get_meta("artist")
    meta[remuco.INFO_ALBUM] = get_meta("album")
    meta[remuco.INFO_TITLE] = get_meta("title")
    meta[remuco.INFO_GENRE] = get_meta("genre")
    meta[remuco.INFO_COMMENT] = get_meta("comment")
    meta[remuco.INFO_LENGTH] = get_meta("duration")
    meta[remuco.INFO_BITRATE] = get_meta("bitrate")
    meta[remuco.INFO_TRACK] = get_meta("tracknr")
    meta[remuco.INFO_RATING] = get_meta("rating")

    img = get_meta(XMMS2.MINFO_KEY_ART)
    if img != "":
        img = "%s/%s" % (XMMS2.BIN_DATA_DIR, img)
    else:
        img = None
            
    log.debug("image: %s" % img)
    
    return (meta, img)
    
# =============================================================================
# helper classes
# =============================================================================

class PlobRequest():
    
    def __init__(self, client, pa, x2, id):
        """ Create a new plob request.
        
        @param client: the requesting client
        @param pa: the Remuco player adapter
        @param x2: the XMMS2 client connection
        @param id: id (str) of the requested plob
        
        """
        
        self.__client = client
        self.__pa = pa
        self.__x2 = x2
        self.__id = id
        try:
            id_int = int(id)
        except ValueError:
            log.error("** BUG ** plob id %s (%s) not an int" % (id, type(id)))
            return

        self.__x2.medialib_get_info(id_int, cb=self.__x2cb_info)
        
    def __x2cb_info(self, result):
        
        if not x2_result_check(result):
            return
        
        info, img = x2_result_to_plob(result)
        
        self.__pa.reply_plob_request(self.__client, self.__id, info)
        
class PlaylistRequest():
    
    def __init__(self, client, x2, name, cb):
        """ Create a new playlist request.
        
        @param client: the requesting client
        @param x2: the XMMS2 client connection
        @param name: name of the requested playlist
        @param cb: callback function to pass this request to if finished
        
        """
        
        self.__x2 = x2
        self.__plob_ids_iter = 0
        self.__plob_ids_int = []
        self.__cb = cb
        
        self.client = client
        self.plob_ids = []
        self.plob_names = []
        
        self.__x2.playlist_list_entries(playlist=name,
                                        cb=self.__x2cb_playlist_ids)

    def __x2cb_playlist_ids(self, result):
        
        if not x2_result_check(result):
            return
        
        self.__plob_ids_int = result.value()
        
        self.__request_next_id()
        
    def __request_next_id(self):
        
        if self.__plob_ids_iter < len(self.__plob_ids_int):
            id = self.__plob_ids_int[self.__plob_ids_iter]
            self.__x2.medialib_get_info(id, cb=self.__x2cb_info)
        else:
            self.__cb(self)
            
    def __x2cb_info(self, result):
        
        if not x2_result_check(result):
            return
    
        info, img = x2_result_to_plob(result)

        id = str(self.__plob_ids_int[self.__plob_ids_iter])
        name = "%s - %s" % (info[remuco.INFO_ARTIST], info[remuco.INFO_TITLE])
        
        self.plob_ids.append(id)
        self.plob_names.append(name)
        
        self.__plob_ids_iter += 1
        self.__request_next_id()
        
class LibraryRequest():
    
    def __init__(self, client, pa, x2, path):
        """ Create a new plob request.
        
        @param client: the requesting client
        @param pa: the Remuco player adapter
        @param x2: the XMMS2 client connection
        @param path: path of the requested library level
        
        """
        
        self.__client = client
        self.__pa = pa
        self.__x2 = x2
        self.__path = path
        
        if path is None or len(path) == 0:
            self.__x2.playlist_list(cb=self.__x2cb_playlist_list)
        elif len(path) == 1:
            PlaylistRequest(client, x2, path[0], self.__cb_playlist_request)
        else:
            log.error("** BUG ** library path %s too long" % str(path))
    
    def __cb_playlist_request(self, pr):
        
        self.__pa.reply_library_request(self.__client, self.__path, [],
                                        pr.plob_ids, pr.plob_names)
    
        
    def __x2cb_playlist_list(self, result):

        if not x2_result_check(result):
            return

        list = result.value()
        
        nested = []
        
        for name in list:
            if not name.startswith("_"):
                nested.append(name)
        
        self.__pa.reply_library_request(self.__client, self.__path, nested,
                                        [], [])
    
# =============================================================================
# player adapter
# =============================================================================

class XMMS2(remuco.Player):
    
    MINFO_KEY_ART = "picture_front"
    
    BIN_DATA_DIR = "%s/bindata" % xmmsclient.userconfdir_get()
    
    ERROR_DISCONNECTED = "disconnected"
    
    PLAYLIST_ID_ACTIVE = "_active"
    
    def __init__(self):
        
        remuco.Player.__init__(self, "XMMS2")
        
        self.__state_playback = remuco.PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_repeat = False
        self.__state_shuffle = False
        self.__state_position = 0
         
        self.__plob_id_int = None # id as integer
        self.__plob_id = None # id as string
        self.__plob_meta = None       
        
        self.__x2 = xmmsclient.XMMS("remuco")
        try:
            self.__x2.connect(path=os.getenv("XMMS_PATH"),
                                disconnect_func=self.__x2cb_disconnect)
        except IOError, e:
            log.error("failed to connect to XMMS2: %s" % str(e))
            self.down()
            raise Exception(e)
        
        self.__conn = xmmsclient.glib.GLibConnector(self.__x2)
        
        self.__x2.broadcast_playback_current_id(self.__x2cb_plob_id)
        self.__x2.broadcast_playback_status(self.__x2cb_playback)
        self.__x2.broadcast_playback_volume_changed(self.__x2cb_volume)
        self.__x2.broadcast_playlist_current_pos(self.__x2cb_position)
        # to dectect all posistion changes:
        self.__x2.broadcast_playlist_changed(self.__x2cb_playlist)
        
        # get initial player state (broadcasts only work on changes):
        self.__x2.playback_current_id(cb=self.__x2cb_plob_id)
        self.__x2.playback_status(cb=self.__x2cb_playback)
        self.__x2.playback_volume_get(cb=self.__x2cb_volume)
        self.__x2.playlist_current_pos(cb=self.__x2cb_position)
        
    def __x2cb_plob_id(self, result):
        
        if not x2_result_check(result):
            self.update_plob(None, None, None)
            return
        
        self.__plob_id_int = result.value()
        self.__plob_id = str(self.__plob_id_int)
        
        log.debug("new plob id: %u" % self.__plob_id_int)
        
        if self.__plob_id_int == 0:
            self.update_plob(None, None, None)
            return

        self.__x2.medialib_get_info(self.__plob_id_int,
                                      cb=self.__x2cb_plob_info)
        
    def __x2cb_plob_info(self, result):
        
        if not x2_result_check(result):
            self.__plob_id_int = 0
            self.__plob_id = str(self.__plob_id_int)
            self.update_plob(None, None, None)
            return

        info, img = x2_result_to_plob(result)
        
        self.update_plob(self.__plob_id, info, img)
        
    def __x2cb_playback(self, result):
        
        if not x2_result_check(result):
            return
        
        val = result.value()
        if val == xmmsclient.PLAYBACK_STATUS_PAUSE:
            self.__state_playback = remuco.PLAYBACK_PAUSE
        elif val == xmmsclient.PLAYBACK_STATUS_PLAY:
            self.__state_playback = remuco.PLAYBACK_PLAY
        elif val == xmmsclient.PLAYBACK_STATUS_STOP:
            self.__state_playback = remuco.PLAYBACK_STOP
        else:
            log.error("** BUG ** unknown XMMS2 playback status: %d", val)
            return
            
        self.update_playback(self.__state_playback)
        
    def __x2cb_volume(self, result):
        
        if not x2_result_check(result):
            return
        
        val = result.value()
        volume = 0
        i = 0
        for v in val.values():
            volume += v
            i += 1
        volume = volume / i
        
        self.__state_volume = volume
        
        self.update_volume(self.__state_volume)
        
    def __x2cb_position(self, result):
                      
        if not x2_result_check(result):
            return
        
        self.__state_position = result.value()['position']
        
        self.update_play_position(self.__state_position)
    
    def __x2cb_playlist(self, result):
        
        if not x2_result_check(result):
            return
        
        # change in playlist may result in position change:
        self.__x2.playlist_current_pos(cb=self.__x2cb_position)
    
    def __x2cb_disconnect(self, result):
        
        log.info("xmms2 disconnected")
        
        global ml
        
        ml.quit()
    
    def get_rating_max(self):
        
        return 5
    
    def jump_in_playlist(self, position):
        
        self.__x2.playlist_set_next(position, cb=x2_result_control)
        self.__x2.playback_tickle(cb=x2_result_control)
        if self.__state_playback != remuco.PLAYBACK_PLAY:
            self.__x2.playback_start(cb=x2_result_control)
        
    def load_playlist(self, path):
        
        if len(path) == 1:
            self.__x2.playlist_load(path[0], cb=x2_result_control)
        else:
            log.error("** BUG ** bad path: %s" % str(path))
    
    def play_next(self):
        
        self.__x2.playlist_set_next_rel(1, cb=x2_result_control)
        self.__x2.playback_tickle(cb=x2_result_control)
    
    def play_previous(self):
        
        if self.__state_position > 0:
            self.__x2.playlist_set_next_rel(-1, cb=x2_result_control)
            self.__x2.playback_tickle(cb=x2_result_control)
    
    def rate_current(self, rating):
        
        if self.__plob_id_int == 0:
            return
        
        self.__x2.medialib_property_set(self.__plob_id_int, "rating", rating,
                                          cb=x2_result_control)
             
    def toggle_play_pause(self):
        
            if self.__state_playback == remuco.PLAYBACK_STOP or \
               self.__state_playback == remuco.PLAYBACK_PAUSE:
                self.__x2.playback_start(cb=x2_result_control)
            else:
                self.__x2.playback_pause(cb=x2_result_control)
                    
    def toggle_repeat(self):
        
        log.info("repeat mode cannot be set for XMMS2")
    
    def toggle_shuffle(self):
        
        self.__x2.playlist_shuffle(cb=x2_result_control)
            
    def seek_forward(self):
        
        self.__x2.playback_seek_ms_rel(5000, cb=x2_result_control)
             
    def seek_backward(self):
        
        self.__x2.playback_seek_ms_rel(-5000, cb=x2_result_control)
    
    def set_tags(self, id, tags):
        """ Attach some tags to a PLOB. 
        
        @param id: ID of the PLOB to attach the tags to
        @param tags: a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
               idea of tags (e.g. like used at last.fm). 

        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("set_tags() not yet implemented")
    
    def set_volume(self, volume):

        # TODO: currently this fails, problem relates to xmms2 installation
        for chan in ("right", "left"):
            self.__x2.playback_volume_set(chan, volume, cb=x2_result_control)

    def request_plob(self, client, id):
        
        PlobRequest(client, self, self.__x2, id)
        
    def request_playlist(self, client):
        
        PlaylistRequest(client, self.__x2, XMMS2.PLAYLIST_ID_ACTIVE,
                        self.__cb_playlist_request)

    def __cb_playlist_request(self, pr):
        
        self.reply_playlist_request(pr.client, pr.plob_ids, pr.plob_names)

    def request_library(self, client, path):
        
        LibraryRequest(client, self, self.__x2, path)
        
# =============================================================================
# main
# =============================================================================

ml = None

def main():
    
    global ml
    
    ml = gobject.MainLoop()
    
    try:
        x2 = XMMS2()
    except Exception, e:
        sys.exc_info()
        print("%s" % str(e))
        print("See remuco-xmms2 log file for details.")
        return
    
    try:
        ml.run()
    except Exception, e:
        print("exception in main loop: %s" % str(e))
    
    x2.down()


if __name__ == '__main__':
    
    if IMPORT_ERROR != None:
        log.error("missing a python module (%s)" % IMPORT_ERROR)
    else:
        log.info("starting")
        
    main()
    