import sys
import os
import gobject
import xmmsclient
import xmmsclient.glib

try:
    import remuco
    IMPORT_ERROR = None
except ImportError, e:
    IMPORT_ERROR = str(e)

from remuco import log

class XMMS2(remuco.Player):
    
    MINFO_KEY_ART = "picture_front"
    
    BIN_DATA_DIR = "%s/bindata" % xmmsclient.userconfdir_get()
    
    ERROR_DISCONNECTED = "disconnected"
    
    def __check(self, result):
        
        if not result.iserror():
            log.debug("result: %s (%s)"
                      % (str(result.value()), str(type(result.value()))))
            return True
        
        err = result.get_error()
        
        if err.lower() == XMMS2.ERROR_DISCONNECTED:
            log.warning("lost connection to XMMS2")
            self.__ml.quit()
        else:
            log.warning("error result: %s" % err)
        
        return False
        
    
    def __init__(self, ml):
        
        remuco.Player.__init__(self, "XMMS2")
        
        self.__ml = ml
        
        self.__state_playback = remuco.PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_repeat = False
        self.__state_shuffle = False
        self.__state_position = 0
         
        self.__plob_id_int = None # id as integer
        self.__plob_id = None # id as string
        self.__plob_meta = None        
            
        self.__xmms = xmmsclient.XMMS("remuco")
        try:
            self.__xmms.connect(path=os.getenv("XMMS_PATH"),
                                disconnect_func=self.__x2cb_disconnect)
        except IOError, e:
            log.error("failed to connect to XMMS2: %s" % str(e))
            self.down()
            raise Exception(e)
        
        self.__conn = xmmsclient.glib.GLibConnector(self.__xmms)
        
        self.__xmms.broadcast_playback_current_id(self.__x2cb_plob_id)
        self.__xmms.broadcast_playback_status(self.__x2cb_playback)
        self.__xmms.broadcast_playback_volume_changed(self.__x2cb_volume)
        self.__xmms.broadcast_playlist_current_pos(self.__x2cb_position)
        self.__xmms.broadcast_playlist_changed(self.__x2cb_playlist)
        
    def __x2cb_plob_id(self, result):
        
        if not self.__check(result):
            self.update_plob(None, None, None)
            return
        
        self.__plob_id_int = result.value()
        self.__plob_id = str(self.__plob_id_int)
        
        log.debug("new plob id: %u" % self.__plob_id_int)
        
        if self.__plob_id_int == 0:
            self.update_plob(None, None, None)
            return

        self.__xmms.medialib_get_info(self.__plob_id_int,
                                      cb=self.__x2cb_plob_info)
        
    def __x2cb_plob_info(self, result):
        
        def get_meta(key):
            try:
                val = minfo[key]
            except KeyError:
                return ""
            if key == "duration":
                val = val / 1000
            return str(val)

        if not self.__check(result):
            self.__plob_id_int = 0
            self.__plob_id = str(self.__plob_id_int)
            self.update_plob(None, None, None)
            return
        
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
    
        self.update_plob(self.__plob_id, meta, img)
        
    def __x2cb_playback(self, result):
        
        if not self.__check(result):
            return
        
        val = result.value()
        if val == xmmsclient.PLAYBACK_STATUS_PAUSE:
            self.__state_playback = remuco.PLAYBACK_PAUSE
        elif val == xmmsclient.PLAYBACK_STATUS_PLAY:
            self.__state_playback = remuco.PLAYBACK_PLAY
        elif val == xmmsclient.PLAYBACK_STATUS_STOP:
            self.__state_playback = remuco.PLAYBACK_STOP
        else:
            log_msg("unknown XMMS2 playback status: %d", val)
            
        self.update_playback(self.__state_playback)
        
    def __x2cb_volume(self, result):
        
        if not self.__check(result):
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
                      
        if not self.__check(result):
            return
        
        self.__state_position = result.value()['position']
        
        self.update_play_position(self.__state_position)
    
    def __x2cb_playlist(self, result):
        
        if not self.__check(result):
            return
        
        # TODO: set len if possible
        log.debug("playlist change result: %s (%s)" %
                  (str(result.value()), str(type(result.value()))))
        
        # change in playlist may result in position change
        
        self.__xmms.playlist_current_pos(cb=self.__x2cb_position)
    
    def __x2cb_disconnect(self, result):
        
        log.info("xmms2 disconnected")
        self.__ml.quit()
    
    def get_rating_max(self):
        
        return 5
    
    def jump_in_playlist(self, position):
        
        self.__xmms.playlist_set_next(position, cb=self.__x2cb_errcheck) 
        self.__xmms.playback_tickle(cb=self.__x2cb_errcheck)
        
    def load_playlist(self, path):
        
        self.__xmms.playlist_load(path[0]) 
    
    def play_next(self):
        
        self.__xmms.playlist_set_next_rel(1, cb=self.__x2cb_errcheck)
        self.__xmms.playback_tickle(cb=self.__x2cb_errcheck)
    
    def play_previous(self):
        
        if self.__state_position > 0:
            self.__xmms.playlist_set_next_rel(-1, cb=self.__x2cb_errcheck)
            self.__xmms.playback_tickle(cb=self.__x2cb_errcheck)
    
    def rate_current(self, rating):
        
        if self.__plob_id_int == 0:
            return
        
        self.__xmms.medialib_property_set(self.__plob_id_int, "rating", rating,
                                          cb=self.__x2cb_errcheck)
             
    def toggle_play_pause(self):
        
            if self.__state_playback == remuco.PLAYBACK_STOP or \
               self.__state_playback == remuco.PLAYBACK_PAUSE:
                self.__xmms.playback_start(cb=self.__x2cb_errcheck)
            else:
                self.__xmms.playback_pause(cb=self.__x2cb_errcheck)
                    
    def toggle_repeat(self):
        
        log.info("repeat mode cannot be set for XMMS2")
    
    def toggle_shuffle(self):
        
        self.__xmms.playlist_shuffle(cb=self.__x2cb_errcheck)
            
    def seek_forward(self):
        
        self.__xmms.playback_seek_ms_rel(5000, cb=self.__x2cb_errcheck)
             
    def seek_backward(self):
        
        self.__xmms.playback_seek_ms_rel(-5000, cb=self.__x2cb_errcheck)
    
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
        for channel in ("right", "left"):
            self.__xmms.playback_volume_set(channel, volume,
                                            cb=self.__x2cb_errcheck)
    
    def __x2cb_errcheck(self, result):
    
        if not result.iserror():
            return
        
        err = result.get_error()
        
        if err.lower() == XMMS2.ERROR_DISCONNECTED:
            log.warning("lost connection to XMMS2")
            self.__ml.quit()
        else:
            log.warning("error on XMMS2 control: %s" % err)
        
if __name__ == '__main__':
    
    if IMPORT_ERROR != None:
        log.error("missing a python module (%s)" % IMPORT_ERROR)
    else:
        log.info("starting")
        
        
    ml = gobject.MainLoop()
    
    try:
        x2 = XMMS2(ml)
    except Exception, e:
        print("%s" % str(e))
        print("See remuco-xmms2 log file for details.")
        sys.exit(1)
    
    try:
        ml.run()
    except Exception, e:
        print("exception in main loop: %s" % str(e))
    
    x2.down()
    