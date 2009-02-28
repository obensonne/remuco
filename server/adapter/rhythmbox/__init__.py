import os.path
import traceback
import urllib
import urlparse
import time

import rb, rhythmdb

import remuco
from remuco import log

# =============================================================================
# constants
# =============================================================================

PLAYORDER_SHUFFLE = "shuffle"
PLAYORDER_SHUFFLE_ALT = "random" # starts with ..
PLAYORDER_REPEAT = "linear-loop"
PLAYORDER_NORMAL = "linear"

SECTION_LIBRARY = "Library"

COVER_FILE_NAMES = ("folder", "front", "album", "cover")
COVER_FILE_TYPES = ("png", "jpeg", "jpg")

SEEK_STEP = 5

# =============================================================================
# plugin
# =============================================================================

class RemucoPlugin(rb.Plugin):
    
    def __init__(self):
        
        rb.Plugin.__init__(self)
        
        self.__rba = None
        
    def activate(self, shell):
        
        if self.__rba is not None:
            return
        
        print("create RhythmboxAdapter")
        self.__rba = RhythmboxAdapter()
        print("RhythmboxAdapter created")

        print("start RhythmboxAdapter")
        self.__rba.start(shell)
        print("RhythmboxAdapter started")
        
    def deactivate(self, shell):
    
        if self.__rba is None:
            return
        
        print("stop RhythmboxAdapter")
        self.__rba.stop()
        print("RhythmboxAdapter stopped")
        
        self.__rba = None
        
# =============================================================================
# player adapter
# =============================================================================

class RhythmboxAdapter(remuco.PlayerAdapter):

    def __init__(self):
        
        self.__shell = None
        
        remuco.PlayerAdapter.__init__(self, "Rhythmbox", max_rating=5)
        
        self.__cover_file = "%s/cover.png" % self.get_cache_dir()

        self.__plob_id = None
        self.__plob_entry = None
        self.__playlist_sc = None
        self.__queue_sc = None
        
        self.__signal_ids = ()
        
        log.debug("init done")

    def start(self, shell):
        
        if self.__shell is not None:
            log.warning("already started")
            return
        
        remuco.PlayerAdapter.start(self)
        
        self.__shell = shell
        
        sp = self.__shell.get_player()
        
        ###### shortcuts to RB data ###### 
        
        self.__plob_id = None
        self.__plob_entry = None
        self.__playlist_sc = sp.get_playing_source()
        self.__queue_sc = self.__shell.props.queue_source
        
        ###### connect to shell player signals ######

        self.__signal_ids = (
            sp.connect("playing_changed", self.__notify_playing_changed),
            sp.connect("playing_uri_changed", self.__notify_playing_uri_changed),
            sp.connect("playing-source-changed", self.__notify_source_changed)
        )

        ###### initially trigger server synchronization ######
        
        # state sync will happen by timeout
        self.__notify_playing_uri_changed(sp, sp.get_playing_path()) # plob sync
        
        log.debug("start done")

    def stop(self):
        
        remuco.PlayerAdapter.stop(self)

        if self.__shell is None:
            return

        ###### disconnect from shell player signals ######

        sp = self.__shell.get_player()

        for sid in self.__signal_ids:
            sp.disconnect(sid)
            
        self.__signal_ids = ()

        ###### release shell ######
        
        self.__shell = None
        
        log.debug("stop done")
        
    def poll(self):
        
        sp = self.__shell.get_player()
        
        ###### check repeat and shuffle ######
        
        order = sp.props.play_order
        
        repeat = order == PLAYORDER_REPEAT or \
                 order.startswith(PLAYORDER_SHUFFLE_ALT)
                 
        self.update_repeat(repeat)
        
        shuffle = order == PLAYORDER_SHUFFLE or \
                  order.startswith(PLAYORDER_SHUFFLE_ALT)
                  
        self.update_shuffle(shuffle)
        
        ###### check volume ######

        volume = int(sp.get_volume() * 100)
        
        self.update_volume(volume)
        
        return True

    # =========================================================================
    # client side player control
    # =========================================================================
    
    def ctrl_jump_in_playlist(self, position):
        
        try:
            self.__jump_in_plq(self.__playlist_sc, position)
        except Exception, e:
            log.debug("playlist jump failed: %s" % str(e))
        
    def ctrl_jump_in_queue(self, position):

        try:
            self.__jump_in_plq(self.__queue_sc, position)
        except Exception, e:
            log.debug("queue jump failed: %s" % str(e))

    def __jump_in_plq(self, sc, position):

        if sc is None:
            return
        
        qm = sc.get_entry_view().props.model
        
        id_to_remove_from_queue = None
        
        sp = self.__shell.get_player()

        if sp.props.playing_from_queue:
            id_to_remove_from_queue = self.__plob_id

        found = False
        i = 0
        for row in qm:
            if i == position:
                sp.set_selected_source(sc)
                sp.set_playing_source(sc)
                sp.play_entry(row[0])
                found = True
                break
            i += 1
        
        if not found:
            sp.do_next()
        
        if id_to_remove_from_queue != None:
            log.debug("remove %s from queue" % id_to_remove_from_queue)
            self.__shell.remove_from_queue(id_to_remove_from_queue)

    def ctrl_load_playlist(self, path):
        
        sc = self.__get_source_from_path(path)
        
        if sc is None:
            return
        
        qm = sc.get_entry_view().props.model
        
        sp = self.__shell.get_player()

        if sc != self.__playlist_sc:
            try:
                sp.set_selected_source(sc)
                sp.set_playing_source(sc)
            except Exception, e:
                log.debug("switching source failed: %s" % str(e))
            
            
    def ctrl_next(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.do_next()
        except Exception, e:
            log.debug("do next failed: %s" % str(e))
    
    def ctrl_previous(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.set_playing_time(0)
            time.sleep(0.1)
            sp.do_previous()
        except Exception, e:
            log.debug("do previous failed: %s" % str(e))
    
    def ctrl_rate(self, rating):
        
        if self.__plob_entry is not None:
            db = self.__shell.props.db
            try:
                db.set(self.__plob_entry, rhythmdb.PROP_RATING, rating)
            except Exception, e:
                log.debug("rating failed: %s" % str(e))
    
    def ctrl_toggle_playing(self):
        
        sp = self.__shell.get_player()
        
        try:
            sp.playpause()
        except Exception, e:
            log.debug("toggle play pause failed: %s" % str(e))
                
    
    def ctrl_toggle_repeat(self):
        # http://mail.gnome.org/archives/rhythmbox-devel/2008-April/msg00078.html
        log.warning("toggle repeat mode not possible")
        
    
    def ctrl_toggle_shuffle(self):
        # http://mail.gnome.org/archives/rhythmbox-devel/2008-April/msg00078.html
        log.warning("toggle shuffle mode not possible")
    
    def ctrl_seek_forward(self):
        
        sp = self.__shell.get_player()

        try:
            sp.seek(SEEK_STEP)
        except Exception, e:
            log.debug("seek fwd failed: %s" % str(e))
    
    def ctrl_seek_backward(self):

        sp = self.__shell.get_player()
        
        try:
            sp.seek(- SEEK_STEP)
        except Exception, e:
            log.debug("seek bwd failed: %s" % str(e))
    
    def ctrl_volume(self, volume):
        
        sp = self.__shell.get_player()

        try:
            sp.set_volume(float(volume) / 100)
        except Exception, e:
            log.debug("set volume failed: %s" % str(e))
        
    # =========================================================================
    # client side requests
    # =========================================================================
    
    def request_plob(self, client, id):
        
        try:
            db = self.__shell.props.db
            entry = db.entry_lookup_by_location(id)
        except Exception, e:
            log.warning("requesting plob failed: %s" % e)
            entry = None
            
        info = self.__get_plob_info(entry)
        
        self.reply_plob_request(client, id, info)
        
    def request_playlist(self, client):
        
        try:
            qm = self.__playlist_sc.get_entry_view().props.model 
            ids, names = self.__get_tracks_from_qmodel(qm)
        except:
            ids, names = [], []
        
        self.reply_playlist_request(client, ids, names)

    def request_queue(self, client):
        
        sc = self.__queue_sc
        qm = sc.props.query_model

        ids, names = self.__get_tracks_from_qmodel(qm)
        
        self.reply_queue_request(client, ids, names)

    def request_library(self, client, path):

        nested = []
        ids = []
        names = []
        
        slm = self.__shell.props.sourcelist_model
        
        ### root ? ###
        
        if path is None or len(path) == 0:
            for group in slm:
                group_name = group[2]
                nested.append(group_name)
            self.reply_library_request(client, path, nested, ids, names)
            return
        
        ### group ? ### Library, Playlists

        if len(path) == 1:
            for group in slm:
                group_name = group[2]
                if path[0] == group_name:
                    for sc in group.iterchildren():
                        source_name = sc[2]
                        log.debug("append %s" % source_name)
                        nested.append(source_name)
                    break
            self.reply_library_request(client, path, nested, ids, names)
            return
            
        ### regular playlist (source) ! ### Library/???, Playlists/???
        
        sc = self.__get_source_from_path(path)

        if sc is None:
            
            self.reply_library_request(client, path, nested, ids, names)
            return
        
        qm = sc.get_entry_view().props.model
            
        ids, names = self.__get_tracks_from_qmodel(qm)
        
        self.reply_library_request(client, path, nested, ids, names)
        
    # ==========================================================================
    # callbacks
    # ==========================================================================
    
    def __notify_playing_uri_changed(self, sp, uri):
        """Shell player signal callback to handle a plob change."""
        
        log.debug("playing uri changed: %s" % uri)
        
        db = self.__shell.props.db

        entry = sp.get_playing_entry()
        if entry is None:
            id = None
        else:
            id = db.entry_get(entry, rhythmdb.PROP_LOCATION)
            # FIXME: id == uri ???
        
        self.__plob_id = id
        self.__plob_entry = entry
        
        db = self.__shell.props.db

        if id is not None and entry is not None:

            info = self.__get_plob_info(entry)
    
            img_data = db.entry_request_extra_metadata(entry, "rb:coverArt")
            if img_data is None:
                img_file = self.__get_plob_img_from_id(id)
            else:
                try:
                    img_data.save(self.__cover_file, "png")
                    img_file = self.__cover_file
                except IOError, e:
                    log.warning("failed to save RB cover (%s)" % e)
                    img_file = None
    
            log.debug("image: %s" % str(img_file))
    
        else:
            id = None
            img_file = None
            info = None

        self.update_plob(id, info, img_file)
        
        # a new plob may result in a new position:
        pfq = self.__shell.get_player().props.playing_from_queue
        self.update_position(self.__get_position(), queue=pfq)

    def __notify_playing_changed(self, sp, b):
        """Shell player signal callback to handle a change in playback."""
        
        log.debug("playing changed: %s" % str(b))
        
        if b:
            self.update_playback(remuco.PLAYBACK_PLAY)
        else:
            self.update_playback(remuco.PLAYBACK_PAUSE)

    def __notify_source_changed(self, sp, source_new):
        """Shell player signal callback to handle a playlist switch."""
        
        log.debug("source changed: %s" % str(source_new))
        
        self.__playlist_sc = source_new
        
    # =========================================================================
    # helper methods
    # =========================================================================

    def __get_tracks_from_qmodel(self, qmodel):
        """Get all tracks in a query model.
        
        Returns 2 lists, first with IDs, second with names of the tracks.
        """
        
        db = self.__shell.props.db

        ids = []
        names = []

        if qmodel is None:
            return (ids, names)

        for row in qmodel:
            uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
            ids.append(uri)
            artist = db.entry_get(row[0], rhythmdb.PROP_ARTIST)
            artist_set = artist is not None and len(artist) > 0
            title = db.entry_get(row[0], rhythmdb.PROP_TITLE)
            title_set = title is not None and len(title) > 0
            if artist_set and title_set:
                names.append("%s - %s" % (artist, title))
            elif title_set:
                names.append(title)
            elif artist_set:
                names.append(artist)
            else:
                names.append("Unknown")

        return (ids, names)

    def __get_plob_info(self, entry):
        """Get meta information for a plob.
        
        @return: meta information (dictionary) - also if entry is None (in this
                 case dummy information is returned)
        """
        
        if entry is None:
            return { remuco.INFO_TITLE : "No information" }
        
        db = self.__shell.props.db
        
        meta = {
            remuco.INFO_TITLE : str(db.entry_get(entry, rhythmdb.PROP_TITLE)),
            remuco.INFO_ARTIST: str(db.entry_get(entry, rhythmdb.PROP_ARTIST)),
            remuco.INFO_ALBUM : str(db.entry_get(entry, rhythmdb.PROP_ALBUM)),
            remuco.INFO_GENRE : str(db.entry_get(entry, rhythmdb.PROP_GENRE)),
            remuco.INFO_BITRATE : str(db.entry_get(entry, rhythmdb.PROP_BITRATE)),
            remuco.INFO_LENGTH : str(db.entry_get(entry, rhythmdb.PROP_DURATION)),
            remuco.INFO_RATING : str(int(db.entry_get(entry, rhythmdb.PROP_RATING))),
            remuco.INFO_TRACK : str(db.entry_get(entry, rhythmdb.PROP_TRACK_NUMBER)),
            remuco.INFO_YEAR : str(db.entry_get(entry, rhythmdb.PROP_YEAR))
        }

        return meta 
    
    def __get_plob_img_from_id(self, id):
        """Get the full path to a cover file related to 'id'.
        
        This looks for image files in the directory where the plob is located.
        
        @return: image path or 'None' if no cover has been found in the plob's
                 folder
        """
        
        elems = urlparse.urlparse(id)
        
        if elems[0] != "file":
            return None
        
        path = urllib.url2pathname(elems[2])
        path = os.path.dirname(path)
        
        for name in COVER_FILE_NAMES:
            for type in COVER_FILE_TYPES:
                file = os.path.join(path, "%s.%s" % (name, type))
                if os.path.isfile(file):
                    return file
                file = os.path.join(path, "%s.%s" % (name.capitalize(), type))
                if os.path.isfile(file):
                    return file
                
        return None

    def __get_source_from_path(self, path):
        """Get the source object of source 'id'.
        
        'path' contains the source' group and name (2 element list).
        """
        
        if len(path) != 2:
            log.error("** BUG ** invalid path length: %s" % str(path))
            return None
        
        group_name, source_name = path
        
        if group_name is None or source_name is None:
            return None
        
        slm = self.__shell.props.sourcelist_model
        
        for group in slm:
            if group_name == group[2]:
                for source in group.iterchildren():
                    if source_name == source[2]:
                        return source[3]

    def __get_position(self):

        sp = self.__shell.get_player()

        db = self.__shell.props.db

        position = 0
        queue = False
        
        id_now = self.__plob_id
        
        if id_now is not None:
            
            if sp.props.playing_from_queue:
                queue = True
                qmodel = self.__queue_sc.props.query_model
            else:
                qmodel = self.__playlist_sc.get_entry_view().props.model
                
            if qmodel is not None:
                for row in qmodel:
                    id = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                    if id_now == id:
                        break
                    position += 1
                    
        log.debug ("position: %i" % position)
        
        return position

