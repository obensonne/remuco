#!/usr/bin/python
"""XMMS2 adapter for Remuco, implemented as an XMMS2 startup script.

__author__ = "Oben Sonne <obensonne@googlemail.com>"
__copyright__ = "Copyright 2009, Oben Sonne"
__license__ = "GPL"
__version__ = "0.0.1"

"""
import os

import gobject

import xmmsclient
import xmmsclient.glib

import remuco
from remuco import log

# =============================================================================
# XMMS2 related constants
# =============================================================================

MINFO_KEYS_ART = ("picture_front", "album_front_large", "album_front_small",
                  "album_front_thumbnail")

MINFO_KEY_TAGS = "tag"
MINFO_KEY_RATING = "rating"

BIN_DATA_DIR = "%s/bindata" % xmmsclient.userconfdir_get()

ERROR_DISCONNECTED = "disconnected"

PLAYLIST_ID_ACTIVE = "_active"

# =============================================================================
# actions
# =============================================================================

ACT_PL_JUMP = remuco.ItemAction("Jump to")
ACT_PL_REMOVE = remuco.ItemAction("Remove", multiple=True)
PLAYLIST_ITEM_ACTIONS = (ACT_PL_JUMP, ACT_PL_REMOVE)

ACT_ML_LOAD_LIST = remuco.ListAction("Load")
MLIB_LIST_ACTIONS = (ACT_ML_LOAD_LIST,)

ACT_ML_APPEND = remuco.ItemAction("Enqueue", multiple=True)
ACT_ML_PLAY_NEXT = remuco.ItemAction("Play next", multiple=True)
MLIB_ITEM_ACTIONS = (ACT_ML_APPEND, ACT_ML_PLAY_NEXT)

# =============================================================================
# helper classes
# =============================================================================

class ItemListRequest():
    
    def __init__(self, client, pa, path):
        """Create a new item list request.
        
        @param client: the requesting client
        @param pa: XMMS2Adapter
        @param path: path of the requested item list
        
        """
        
        self.__pa = pa
        self.__ids_iter = 0
        self.__ids_int = []
        
        self.__client = client
        self.__ids = []
        self.__names = []
        
        self.__path = path
        
        if not path:
            # request mlib root
            self.__pa._x2.playlist_list(cb=self.__handle_list_of_playlists)
        elif len(path) == 1:
            # request contents of specific list
            self.__pa._x2.playlist_list_entries(playlist=path[0],
                                                cb=self.__handle_ids)
        else:
            log.error("** BUG ** unexpected path: %s" % path)

    def __handle_ids(self, result):
        
        if not self.__pa._check_result(result):
            return
        
        self.__ids_int = result.value()
        
        log.debug("playlist ids: %s" % self.__ids_int)
        
        self.__request_next_name()
        
    def __request_next_name(self):
        """Iterates over item IDs of a list to get item names.
        
        If all item names have been collected, the original request is replied.
         
        """
        
        if self.__ids_iter < len(self.__ids_int):
            # proceed in getting item names
            id = self.__ids_int[self.__ids_iter]
            self.__pa._x2.medialib_get_info(id, cb=self.__handle_name)
            return
        
        if self.__path[0] == PLAYLIST_ID_ACTIVE:
            # have all item names, reply playlist request
            self.__pa.reply_playlist_request(self.__client, self.__ids,
                self.__names, item_actions=PLAYLIST_ITEM_ACTIONS)
        else:
            # have all item names, reply mlib list request
            self.__pa.reply_mlib_request(self.__client, self.__path, [],
                self.__ids, self.__names, item_actions=MLIB_ITEM_ACTIONS)
            
            
    def __handle_name(self, result):
        """Callback to handle meta info of a specific list item."""
        
        if not self.__pa._check_result(result):
            return
    
        minfo = result.value()

        id = str(self.__ids_int[self.__ids_iter])
        artist = minfo.get("artist", "??")
        title = minfo.get("title", "??")
        name = "%s - %s" % (artist, title)
        
        self.__ids.append(id)
        self.__names.append(name)
        
        self.__ids_iter += 1
        self.__request_next_name()
        
    def __handle_list_of_playlists(self, result):
        """Callback to handle list of playlists."""

        if not self.__pa._check_result(result):
            return

        list = result.value()
        
        nested = []
        
        for name in list:
            if not name.startswith("_"):
                nested.append(name)
        
        self.__pa.reply_medialib_request(self.__client, self.__path, nested,
            [], [], list_actions=MLIB_LIST_ACTIONS)
    
# =============================================================================
# player adapter
# =============================================================================

class XMMS2Adapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        remuco.PlayerAdapter.__init__(self, "XMMS2",
                                      max_rating=5,
                                      shuffle_known=True,
                                      playback_known=True,
                                      volume_known=True)
        
        self.__lcmgr = None
        
        self.__state_playback = remuco.PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_position = 0
         
        self.__item_id_int = None # id as integer
        self.__item_id = None # id as string
        self.__item_meta = None
        
        self.__shuffle_off_sid = 0
        
        self._x2 = xmmsclient.XMMS("remuco")
        self.__x2_glib_connector = None

    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        try:
            self._x2.connect(path=os.getenv("XMMS_PATH"),
                              disconnect_func=self.__notify_disconnect)
        except IOError, e:
            log.error("failed to connect to XMMS2: %s" % str(e))
            self.__lcmgr.stop()
            return
        
        self.__x2_glib_connector = xmmsclient.glib.GLibConnector(self._x2)
        
        self._x2.broadcast_playback_current_id(self.__notify_id)
        self._x2.broadcast_playback_status(self.__notify_playback)
        self._x2.broadcast_playback_volume_changed(self.__notify_volume)
        self._x2.broadcast_playlist_current_pos(self.__notify_position)
        # to dectect all posistion changes:
        self._x2.broadcast_playlist_changed(self.__notify_playlist_change)
        
        # get initial player state (broadcasts only work on changes):
        self._x2.playback_current_id(cb=self.__notify_id)
        self._x2.playback_status(cb=self.__notify_playback)
        self._x2.playback_volume_get(cb=self.__notify_volume)
        self._x2.playlist_current_pos(cb=self.__notify_position)
        
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        if self.__shuffle_off_sid > 0:
            gobject.source_remove(self.__shuffle_off_sid)
            self.__shuffle_off_sid = 0
        
        self._x2 = None
        self.__x2_glib_connector = None
        
    # =========================================================================
    # control interface 
    # =========================================================================
    
    def ctrl_next(self):
        
        self._x2.playlist_set_next_rel(1, cb=self.__ignore_result)
        self._x2.playback_tickle(cb=self.__ignore_result)
    
    def ctrl_previous(self):
        
        if self.__state_position > 0:
            self._x2.playlist_set_next_rel(-1, cb=self.__ignore_result)
            self._x2.playback_tickle(cb=self.__ignore_result)
    
    def ctrl_toggle_playing(self):
        
            if self.__state_playback == remuco.PLAYBACK_STOP or \
               self.__state_playback == remuco.PLAYBACK_PAUSE:
                self._x2.playback_start(cb=self.__ignore_result)
            else:
                self._x2.playback_pause(cb=self.__ignore_result)
                    
    def ctrl_toggle_shuffle(self):
        
        self._x2.playlist_shuffle(cb=self.__ignore_result)
        self.update_shuffle(True)

        # emulate shuffle mode: show shuffle state for a second
        if self.__shuffle_off_sid > 0:
            gobject.source_remove(self.__shuffle_off_sid)
        self.__shuffle_off_sid = gobject.timeout_add(1000, self.__shuffle_off)
        
    def ctrl_seek(self, direction):
        
        self._x2.playback_seek_ms_rel(direction * 5000, cb=self.__ignore_result)
    
    def ctrl_volume(self, direction):
        
        # TODO: currently this fails, problem relates to xmms2 installation
        
        if direction == 0:
            volume = 0
        else:
            volume = self.__state_volume + 5 * direction
            volume = min(volume, 100)
            volume = max(volume, 0)
        
        for chan in ("right", "left"):
            self._x2.playback_volume_set(chan, volume, cb=self.__ignore_result)

    def ctrl_rate(self, rating):
        
        if self.__item_id_int == 0:
            return
        
        self._x2.medialib_property_set(self.__item_id_int, MINFO_KEY_RATING,
                                       rating, cb=self.__ignore_result)
             
    def ctrl_tag(self, id, tags):
        
        try:
            id_int = int(id)
        except ValueError:
            log.error("** BUG ** id is not an int")
            return
        
        s = ""
        for tag in tags:
            s = "%s,%s" % (s, tag)
        
        self._x2.medialib_property_set(id_int, MINFO_KEY_TAGS, s,
                                       cb=self.__ignore_result)
    
    def ctrl_clear_playlist(self):
        
        self._x2.playlist_clear(playlist=PLAYLIST_ID_ACTIVE,
                                cb=self.__ignore_result)
        
    # =========================================================================
    # actions interface
    # =========================================================================
    
    def action_playlist_item(self, action_id, positions, ids):

        if action_id == ACT_PL_JUMP.id:
            
            self._x2.playlist_set_next(positions[0], cb=self.__ignore_result)
            self._x2.playback_tickle(cb=self.__ignore_result)
            if self.__state_playback != remuco.PLAYBACK_PLAY:
                self._x2.playback_start(cb=self.__ignore_result)
                
        elif action_id == ACT_PL_REMOVE.id:
            
            positions.sort()
            positions.reverse()
            for pos in positions:
                log.debug("remove %d from playlist" % pos)
                self._x2.playlist_remove_entry(pos, cb=self.__ignore_result)
        else:
            log.error("** BUG ** unexpected playlist item action")

    def action_mlib_item(self, action_id, path, positions, ids):
        
        if action_id == ACT_ML_APPEND.id:
            
            for id in ids:
                id = int(id)
                self._x2.playlist_add_id(id, cb=self.__ignore_result)
                
        elif action_id == ACT_ML_PLAY_NEXT.id:
            
            pos = self.__state_position + 1
            ids.reverse()
            for id in ids:
                id = int(id)
                self._x2.playlist_insert_id(pos, id, cb=self.__ignore_result)
                
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    def action_mlib_list(self, action_id, path):

        if action_id == ACT_ML_LOAD_LIST.id:
            
            if len(path) == 1:
                self._x2.playlist_load(path[0], cb=self.__ignore_result)
                self._x2.playlist_set_next(0, cb=self.__ignore_result)
                self._x2.playback_tickle(cb=self.__ignore_result)
                if self.__state_playback != remuco.PLAYBACK_PLAY:
                    self._x2.playback_start(cb=self.__ignore_result)
            else:
                log.error("** BUG ** unexpected path: %s" % path)
                
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
                
    # =========================================================================
    # request interface 
    # =========================================================================
    
    def request_playlist(self, client):
        
        ItemListRequest(client, self, [PLAYLIST_ID_ACTIVE])

    def request_mlib(self, client, path):
        
        ItemListRequest(client, self, path)
        
    # =========================================================================
    # internal methods
    # =========================================================================
    
    def _set_lcmgr(self, lcmgr):
        """ Set the life cycle manager to stop when XMMS2 disconnects. """
        
        self.__lcmgr = lcmgr
        
    def _check_result(self, result):
        """ Check the result of a request send to XMMS2. """
        
        if not result.iserror():
            return True
        
        err = result.get_error()
        
        if err.lower() == ERROR_DISCONNECTED:
            log.warning("lost connection to XMMS2")
            self.__lcmgr.stop()
        else:
            log.warning("error result: %s" % err)
        
        return False
    
    def __notify_id(self, result):
        
        if not self._check_result(result):
            self.update_item(None, None, None)
            return
        
        self.__item_id_int = result.value()
        self.__item_id = str(self.__item_id_int)
        
        log.debug("new item id: %u" % self.__item_id_int)
        
        if self.__item_id_int == 0:
            self.update_item(None, None, None)
            return

        self._x2.medialib_get_info(self.__item_id_int, cb=self.__handle_info)
        
    def __handle_info(self, result):
        """Callback to handle meta data requested for the current item.""" 
        
        def get_meta(key):
            val = minfo.get(key, "")
            if key == "duration":
                val = int(val // 1000)
            if not isinstance(val, basestring):
                val = str(val)
            return val
    
        if not self._check_result(result):
            self.__item_id_int = 0
            self.__item_id = str(self.__item_id_int)
            self.update_item(None, None, None)
            return

        minfo = result.value()

        info = {}
        info[remuco.INFO_ARTIST] = get_meta("artist")
        info[remuco.INFO_ALBUM] = get_meta("album")
        info[remuco.INFO_TITLE] = get_meta("title")
        info[remuco.INFO_GENRE] = get_meta("genre")
        info[remuco.INFO_COMMENT] = get_meta("comment")
        info[remuco.INFO_LENGTH] = get_meta("duration")
        info[remuco.INFO_BITRATE] = get_meta("bitrate")
        info[remuco.INFO_TRACK] = get_meta("tracknr")
        info[remuco.INFO_RATING] = get_meta(MINFO_KEY_RATING)
        info[remuco.INFO_TAGS] = get_meta(MINFO_KEY_TAGS)
    
        img = ""
        for img_key in MINFO_KEYS_ART:
            img = get_meta(img_key)
            if img != "":
                img = "%s/%s" % (BIN_DATA_DIR, img)
                break
        
        if img == "":
            url = get_meta("url").replace("+", "%20")
            img = self.find_image(url)
        
        self.update_item(self.__item_id, info, img)
        
    def __notify_playback(self, result):
        
        if not self._check_result(result):
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
        
    def __notify_volume(self, result):
        
        if not self._check_result(result):
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
        
    def __notify_position(self, result):
                      
        if not self._check_result(result):
            return
        
        self.__state_position = result.value()['position']
        
        self.update_position(self.__state_position)
    
    def __notify_playlist_change(self, result):
        
        if not self._check_result(result):
            return
        
        # change in playlist may result in position change:
        self._x2.playlist_current_pos(cb=self.__notify_position)
    
    def __notify_disconnect(self, result):
        
        log.info("xmms2 disconnected")
        
        self.__lcmgr.stop()
    
    def __ignore_result(self, result):
        """Handle an XMMS2 result which is not of interest."""
        
        self._check_result(result)
    
    def __shuffle_off(self):
        """Timeout callback to disable the pseudo shuffle."""
        
        self.update_shuffle(False)
        self.__shuffle_off_sid = 0
            
# =============================================================================
# main
# =============================================================================

if __name__ == '__main__':
    
    pa = XMMS2Adapter()
    mg = remuco.Manager(pa)
    pa._set_lcmgr(mg) # pa stops mg manually on xmms2 disconnect
    mg.run()
    