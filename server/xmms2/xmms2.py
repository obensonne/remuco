#!/usr/bin/python

import sys
import os
import signal
import gobject

import xmmsclient
import xmmsclient.glib

import remuco
from remuco import log

# =============================================================================
# helper classes
# =============================================================================

class PlobRequest():
    
    def __init__(self, client, pa, id):
        """ Create a new plob request.
        
        @param client: the requesting client
        @param pa: XMMS2Adapter
        @param id: id (str) of the requested plob
        
        """
        
        self.__client = client
        self.__pa = pa
        self.__id = id
        try:
            id_int = int(id)
        except ValueError:
            log.error("** BUG ** plob id %s (%s) not an int" % (id, type(id)))
            return

        self.__pa._x2.medialib_get_info(id_int, cb=self.__x2cb_info)
        
    def __x2cb_info(self, result):
        
        if not self.__pa._x2_result_check(result):
            return
        
        info, img = self.__pa._x2_result_to_plob(result)
        
        self.__pa.reply_plob_request(self.__client, self.__id, info)
        
class PlaylistRequest():
    
    def __init__(self, client, pa, name, cb):
        """ Create a new playlist request.
        
        @param client: the requesting client
        @param pa: XMMS2Adapter
        @param name: name of the requested playlist
        @param cb: callback function to pass this request to if finished
        
        """
        
        self.__pa = pa
        self.__plob_ids_iter = 0
        self.__plob_ids_int = []
        self.__cb = cb
        
        self.client = client
        self.plob_ids = []
        self.plob_names = []
        
        self.__pa._x2.playlist_list_entries(playlist=name,
                                            cb=self.__x2cb_playlist_ids)

    def __x2cb_playlist_ids(self, result):
        
        if not self.__pa._x2_result_check(result):
            return
        
        self.__plob_ids_int = result.value()
        
        self.__request_next_id()
        
    def __request_next_id(self):
        
        if self.__plob_ids_iter < len(self.__plob_ids_int):
            id = self.__plob_ids_int[self.__plob_ids_iter]
            self.__pa._x2.medialib_get_info(id, cb=self.__x2cb_info)
        else:
            self.__cb(self)
            
    def __x2cb_info(self, result):
        
        if not self.__pa._x2_result_check(result):
            return
    
        info, img = self.__pa._x2_result_to_plob(result)

        id = str(self.__plob_ids_int[self.__plob_ids_iter])
        name = "%s - %s" % (info[remuco.INFO_ARTIST], info[remuco.INFO_TITLE])
        
        self.plob_ids.append(id)
        self.plob_names.append(name)
        
        self.__plob_ids_iter += 1
        self.__request_next_id()
        
class LibraryRequest():
    
    def __init__(self, client, pa, path):
        """ Create a new plob request.
        
        @param client: the requesting client
        @param pa: XMMS2Adapter
        @param path: path of the requested library level
        
        """
        
        self.__client = client
        self.__pa = pa
        self.__path = path
        
        if path is None or len(path) == 0:
            self.__pa._x2.playlist_list(cb=self.__x2cb_playlist_list)
        elif len(path) == 1:
            PlaylistRequest(client, pa, path[0], self.__cb_playlist_request)
        else:
            log.error("** BUG ** library path %s too long" % str(path))
    
    def __cb_playlist_request(self, pr):
        
        self.__pa.reply_library_request(self.__client, self.__path, [],
                                        pr.plob_ids, pr.plob_names)
    
        
    def __x2cb_playlist_list(self, result):

        if not self.__pa._x2_result_check(result):
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

class XMMS2Adapter(remuco.PlayerAdapter):
    
    MINFO_KEYS_ART = ("picture_front", "album_front_large", "album_front_small",
                      "album_front_thumbnail")
    
    MINFO_KEY_TAGS = "tag"
    MINFO_KEY_RATING = "rating"
    
    BIN_DATA_DIR = "%s/bindata" % xmmsclient.userconfdir_get()
    
    ERROR_DISCONNECTED = "disconnected"
    
    PLAYLIST_ID_ACTIVE = "_active"
    
    def __init__(self):
        
        remuco.PlayerAdapter.__init__(self, "XMMS2", max_rating=5)
        
        self.__sm = None
        
        self.__state_playback = remuco.PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_repeat = False
        self.__state_shuffle = False
        self.__state_position = 0
         
        self.__plob_id_int = None # id as integer
        self.__plob_id = None # id as string
        self.__plob_meta = None       
        
        self._x2 = xmmsclient.XMMS("remuco")
        self.__x2_gc = None

    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        try:
            self._x2.connect(path=os.getenv("XMMS_PATH"),
                              disconnect_func=self.__x2cb_disconnect)
        except IOError, e:
            log.error("failed to connect to XMMS2: %s" % str(e))
            self.__sm.stop()
            return
        
        self.__x2_gc = xmmsclient.glib.GLibConnector(self._x2)
        
        self._x2.broadcast_playback_current_id(self.__x2cb_plob_id)
        self._x2.broadcast_playback_status(self.__x2cb_playback)
        self._x2.broadcast_playback_volume_changed(self.__x2cb_volume)
        self._x2.broadcast_playlist_current_pos(self.__x2cb_position)
        # to dectect all posistion changes:
        self._x2.broadcast_playlist_changed(self.__x2cb_playlist)
        
        # get initial player state (broadcasts only work on changes):
        self._x2.playback_current_id(cb=self.__x2cb_plob_id)
        self._x2.playback_status(cb=self.__x2cb_playback)
        self._x2.playback_volume_get(cb=self.__x2cb_volume)
        self._x2.playlist_current_pos(cb=self.__x2cb_position)
        
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        self._x2 = None
        self.__x2_gc = None
        
    def ctrl_jump_in_playlist(self, position):
        
        self._x2.playlist_set_next(position, cb=self.__x2cb_ignore)
        self._x2.playback_tickle(cb=self.__x2cb_ignore)
        if self.__state_playback != remuco.PLAYBACK_PLAY:
            self._x2.playback_start(cb=self.__x2cb_ignore)
        
    def ctrl_load_playlist(self, path):
        
        if len(path) == 1:
            self._x2.playlist_load(path[0], cb=self.__x2cb_ignore)
        else:
            log.error("** BUG ** bad path: %s" % str(path))
    
    def ctrl_next(self):
        
        self._x2.playlist_set_next_rel(1, cb=self.__x2cb_ignore)
        self._x2.playback_tickle(cb=self.__x2cb_ignore)
    
    def ctrl_previous(self):
        
        if self.__state_position > 0:
            self._x2.playlist_set_next_rel(-1, cb=self.__x2cb_ignore)
            self._x2.playback_tickle(cb=self.__x2cb_ignore)
    
    def ctrl_rate(self, rating):
        
        if self.__plob_id_int == 0:
            return
        
        self._x2.medialib_property_set(self.__plob_id_int,
                                        XMMS2Adapter.MINFO_KEY_RATING, rating,
                                        cb=self.__x2cb_ignore)
             
    def ctrl_toggle_playing(self):
        
            if self.__state_playback == remuco.PLAYBACK_STOP or \
               self.__state_playback == remuco.PLAYBACK_PAUSE:
                self._x2.playback_start(cb=self.__x2cb_ignore)
            else:
                self._x2.playback_pause(cb=self.__x2cb_ignore)
                    
    def ctrl_toggle_repeat(self):
        
        log.info("repeat mode cannot be set for XMMS2")
    
    def ctrl_toggle_shuffle(self):
        
        self._x2.playlist_shuffle(cb=self.__x2cb_ignore)
            
    def ctrl_seek_forward(self):
        
        self._x2.playback_seek_ms_rel(5000, cb=self.__x2cb_ignore)
             
    def ctrl_seek_backward(self):
        
        self._x2.playback_seek_ms_rel(-5000, cb=self.__x2cb_ignore)
    
    def ctrl_tag(self, id, tags):
        
        try:
            id_int = int(id)
        except ValueError:
            log.error("** BUG ** id is not an int")
            return
        
        s = ""
        for tag in tags:
            s = "%s,%s" % (s, tag)
        
        self._x2.medialib_property_set(id_int, XMMS2Adapter.MINFO_KEY_TAGS, s,
                                        cb=self.__x2cb_ignore)
    
    def ctrl_volume(self, volume):
        # TODO: currently this fails, problem relates to xmms2 installation
        for chan in ("right", "left"):
            self._x2.playback_volume_set(chan, volume, cb=self.__x2cb_ignore)

    def request_plob(self, client, id):
        
        PlobRequest(client, self, id)
        
    def request_playlist(self, client):
        
        PlaylistRequest(client, self, XMMS2Adapter.PLAYLIST_ID_ACTIVE,
                        self.__prcb_playlist_request)

    def request_library(self, client, path):
        
        LibraryRequest(client, self, path)
        
    def _set_script_manager(self, sm):
        """ Set the script manager to stop manually when XMMS2 disconnects. """
        
        self.__sm = sm
        
    def _x2_result_to_plob(self, result):
        """ Convert a medialib info result to a plob. """
        
        def get_meta(key):
            try:
                val = minfo[key]
            except KeyError:
                return ""
            if key == "duration":
                val = val / 1000
            if not isinstance(val, str) and not isinstance(val, unicode):
                val = str(val)
            return val
    
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
        meta[remuco.INFO_RATING] = get_meta(XMMS2Adapter.MINFO_KEY_RATING)
        meta[remuco.INFO_TAGS] = get_meta(XMMS2Adapter.MINFO_KEY_TAGS)
    
        img = None
        for img_key in XMMS2Adapter.MINFO_KEYS_ART:
            img = get_meta(img_key)
            if img != "":
                img = "%s/%s" % (XMMS2Adapter.BIN_DATA_DIR, img)
                break
        
        if img == "":
            img = None
        
        log.debug("image: %s" % img)
        
        return (meta, img)
        
    def _x2_result_check(self, result):
        """ Check the result of a request send to XMMS2. """
        
        if not result.iserror():
            return True
        
        err = result.get_error()
        
        if err.lower() == XMMS2Adapter.ERROR_DISCONNECTED:
            log.warning("lost connection to XMMS2")
            self.__sm.stop()
        else:
            log.warning("error result: %s" % err)
        
        return False
    
    def __prcb_playlist_request(self, pr):
        
        self.reply_playlist_request(pr.client, pr.plob_ids, pr.plob_names)

    def __x2cb_plob_id(self, result):
        
        if not self._x2_result_check(result):
            self.update_plob(None, None, None)
            return
        
        self.__plob_id_int = result.value()
        self.__plob_id = str(self.__plob_id_int)
        
        log.debug("new plob id: %u" % self.__plob_id_int)
        
        if self.__plob_id_int == 0:
            self.update_plob(None, None, None)
            return

        self._x2.medialib_get_info(self.__plob_id_int,
                                    cb=self.__x2cb_plob_info)
        
    def __x2cb_plob_info(self, result):
        
        if not self._x2_result_check(result):
            self.__plob_id_int = 0
            self.__plob_id = str(self.__plob_id_int)
            self.update_plob(None, None, None)
            return

        info, img = self._x2_result_to_plob(result)
        
        self.update_plob(self.__plob_id, info, img)
        
    def __x2cb_playback(self, result):
        
        if not self._x2_result_check(result):
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
        
        if not self._x2_result_check(result):
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
                      
        if not self._x2_result_check(result):
            return
        
        self.__state_position = result.value()['position']
        
        self.update_position(self.__state_position)
    
    def __x2cb_playlist(self, result):
        
        if not self._x2_result_check(result):
            return
        
        # change in playlist may result in position change:
        self._x2.playlist_current_pos(cb=self.__x2cb_position)
    
    def __x2cb_ignore(self, result):
        """ Check the result of a control command sent to XMMS2. """
        
        self._x2_result_check(result)
    
    def __x2cb_disconnect(self, result):
        
        log.info("xmms2 disconnected")
        
        self.__sm.stop()
    
# =============================================================================
# main
# =============================================================================

if __name__ == '__main__':
    
    pa = XMMS2Adapter()
    sm = remuco.ScriptManager(pa)
    pa._set_script_manager(sm) # pa stops sm manually on xmms2 disconnect
    sm.run()
    