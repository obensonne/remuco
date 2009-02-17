import gobject
import os
import os.path
import sys
import traceback
import urllib
import urlparse
import time

import rb, rhythmdb

import remuco
from remuco import log


###############################################################################

PLAYER = "Rhythmbox"

###############################################################################
#
# Rhythmbox related constants
#
###############################################################################

PLAYORDER_SHUFFLE = "shuffle"
PLAYORDER_SHUFFLE_ALT = "random" # starts with ..
PLAYORDER_REPEAT = "linear-loop"
PLAYORDER_NORMAL = "linear"

COVER_FILE_NAMES = ("folder", "front", "album", "cover")
COVER_FILE_TYPES = ("png", "jpeg", "jpg")

RATING_MAX = 5

SEEK_STEP = 5

DELIM = "__:;:___"

###############################################################################
#
# Remuco Plugin
#
###############################################################################

class RemucoPlugin(rb.Plugin):
    
    def __init__(self):
        
        try:
            
            print("initialize remuco plugin ..")

            rb.Plugin.__init__(self)
        
            self.__rem = None
            
        except Exception, e:
            
            print("failed to init plugin (%s)" % e)
        
    def activate(self, shell):
        
        print("activate remuco plugin ..")

        if self.__rem is not None:
            print("remuco plugin already active!")
            return

        try:
            
            self.__rem = Remythm(shell)
            
            print("see remuco-rhythmbox log file for further logging")
            
        except Exception, e:
            
            print("failed to init rhythmbox player object (%s)" % e)
            traceback.print_exc()
        
        print("remuco plugin activated")

    def deactivate(self, shell):
    
        print("deactivate Remuco plugin ..")

        if self.__rem is not None:
            self.__rem.down()
            self.__rem = None

        print("deactivate Remuco plugin .. done")
###############################################################################
#
# Player Proxy
#
###############################################################################

def rblog(s):
    print (s)

class Remythm(remuco.Player):

    def __init__(self, shell):
        
        remuco.Player.__init__(self, "Rhythmbox")
        
        self.__shell = shell
        
        self.__cover_file = "%s/cover.png" % self.get_cache_dir()

        sp = self.__shell.get_player()
        
        #log.set_file(None) # log to std-out
        #log.set_functions(rblog, rblog, rblog, rblog)
        
        ###### shortcuts to RB data ###### 
        
        self.__plob_id = None
        self.__plob_entry = None
        self.__playlist_sc = sp.get_playing_source()
        self.__queue_sc = self.__shell.props.queue_source
        
        ###### connect to shell player signals ######

        self.__cb_ids_sp = (
            sp.connect("playing_changed", self.__cb_sp_playing_changed),
            sp.connect("playing_uri_changed", self.__cb_sp_playing_uri_changed),
            sp.connect("playing-source-changed", self.__cb_sp_playlist_changed)
        )

        ###### periodically check for changes which have no signals ######

        self.__cb_id_mc_poll_misc = \
            gobject.timeout_add(3000, self.__cb_mc_poll_misc)

        ###### initially trigger server synchronization ######
        
        # state sync will happen by timeout
        self.__cb_sp_playing_uri_changed(sp, sp.get_playing_path()) # plob sync
        
        log.debug("Remythm() done")

    #==========================================================================
    # client side player control (to be implemented by sub classes) 
    #==========================================================================
    
    def get_rating_max(self):
        """ Get the maximum possible rating value.
        
        @return: the player's maximum rating value
        
        @note: This method should be overwritten by sub classes of Player if the
               corresponding player supports rating.
        """
        return RATING_MAX
    
    def jump_in_playlist(self, position):
        
        self.__jump_in_plq(self.__playlist_sc, position)
        
    def jump_in_queue(self, position):

        self.__jump_in_plq(self.__queue_sc, position)

    def __jump_in_plq(self, sc, position):

        if not sc:
            return
        
        qm = sc.get_entry_view().props.model
        
        # FIXME: assuming entry view and query model are never None
        
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

    def load_playlist(self, path):
        """Jump to a specific song (index) in a specific source (path)."""
        
        sc = self.__get_source_from_path(path)
        
        if not sc:
            return
        
        qm = sc.get_entry_view().props.model
        
        # FIXME: assuming entry view and query model are never None
        
        sp = self.__shell.get_player()

        if sc != self.__playlist_sc:
            sp.set_selected_source(sc)
            sp.set_playing_source(sc)
            
    def play_next(self):
        """ Play the next item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        
        sp = self.__shell.get_player()
        if sp is not None:
            try:
                sp.do_next()
            except:
                pass
    
    def play_previous(self):
        """ Play the previous item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        
        sp = self.__shell.get_player()
        if sp is not None:
            try:
                sp.set_playing_time(0)
                time.sleep(0.1)
                sp.do_previous()
            except:
                pass # no previous song
    
    def rate_current(self, rating):
        """ Rate the currently played item. 
        
        @param rating: rating value (int)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        db = self.__shell.props.db
        if self.__plob_entry is not None:
            db.set(self.__plob_entry, rhythmdb.PROP_RATING, rating)
    
    def toggle_play_pause(self):
        """ Toggle play and pause. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.get_player()
        if sp is not None:
            try:
                sp.playpause()
            except:
                pass
                
    
    def toggle_repeat(self):
        """ Toggle repeat mode. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        logging.warning("toggle_repeat() not yet implemented")
        
    
    def toggle_shuffle(self):
        """ Toggle shuffle mode. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        logging.warning("toggle_shuffle() not yet implemented")
    
    def seek_forward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.get_player()
        if sp is not None:
            sp.seek(SEEK_STEP)
    
    def seek_backward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.get_player()
        if sp is not None:
            sp.seek(- SEEK_STEP)
    
    def set_tags(self, id, tags):
        """ Attach some tags to a PLOB. 
        
        @param id: ID of the PLOB to attach the tags to
        @param tags: a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
               idea of tags (e.g. like used at last.fm). 

        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        logging.warning("set_tags() not yet implemented")
    
    def set_volume(self, volume):
        """ Set volume. 
        
        @param volume: the new volume in percent
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.get_player()
        if sp is not None:
            sp.set_volume(float(volume) / 100)
        
    def request_plob(self, id, client):
        """ Request information about a specific PLOB. 
        
        @param id: ID of the requested PLOB (string)
        @param client: the requesting client - use reply_plob_request()
                       to send back the requested PLOB
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client request a PLOB from the player.
               
        @see reply_plob_request()
        """
        try:
            
            log.debug("called request_plob(%s)" % id)        
    
            db = self.__shell.props.db
    
            entry = db.entry_lookup_by_location(id)
            
        except Exception, e:
            log.warning("error while requesting a plob (%s)" % e)
            entry = None
            
        info = self.__get_plob_info(entry)
        
        gobject.idle_add(self.reply_plob_request, client, id, info)
        
    def request_playlist(self, client):
        
        try:
            qm = self.__playlist_sc.get_entry_view().props.model 
            ids, names = self.__get_tracks_from_qmodel(qm)
        except:
            ids, names = [], []
        
        gobject.idle_add(self.reply_playlist_request, client, ids, names)

    def request_queue(self, client):
        
        sc = self.__queue_sc
        qm = sc.props.query_model

        ids, names = self.__get_tracks_from_qmodel(qm)
        
        gobject.idle_add(self.reply_queue_request, client, ids, names)

    def request_library(self, client, path):
        log.debug("called RequestPloblist(%s)" % path)

        nested = []
        plob_ids = []
        plob_names = []
        
        slm = self.__shell.props.sourcelist_model
        
        if not slm:
            log.debug("no source list model")
            gobject.idle_add(self.reply_library_request, client, path, nested,
                             plob_ids, plob_names)
            return

        ### root ? ###
        
        # TODO: include Library/* here in root

        if path is None or len(path) == 0:
            for group in slm:
                group_name = group[2]
                nested.append(group_name)
            gobject.idle_add(self.reply_library_request, client, path,
                             nested, plob_ids, plob_names)
            return
        
        ### group ? ### Library, Playlists

        if len(path) == 1:
            for group in slm:
                group_name = group[2]
                if path[0] == group_name:
                    for source in group.iterchildren():
                        source_name = source[2]
                        log.debug("append %s" % source_name)
                        nested.append(source_name)
                    break
            gobject.idle_add(self.reply_library_request, client, path, nested,
                             plob_ids, plob_names)
            return
            
        ### regular playlist (source) ! ### Library/???, Playlists/???
        
        source = self.__get_source_from_path(path)

        if not source:
            gobject.idle_add(self.reply_library_request, client, path, nested,
                             plob_ids, plob_names)
            return

        model = source.get_entry_view().props.model
        
        plob_ids, plob_names = self.__get_tracks_from_qmodel(model)
        
        gobject.idle_add(self.reply_library_request, client, path, nested,
                         plob_ids, plob_names)

        
    def down(self):
        
        remuco.Player.down(self)

        if self.__shell is None:
            return

        ###### disconnect from shell player signals ######

        # FIXME: assuming shell player is never None
        
        sp = self.__shell.get_player()

        for cb_id in self.__cb_ids_sp:
            
            sp.disconnect(cb_id)
            
        self.__cb_ids_sp = ()

        ###### remove gobject sources ######
        
        if self.__cb_id_mc_poll_misc > 0:
            gobject.source_remove(self.__cb_id_mc_poll_misc)

        # release shell
        self.__shell = None
        
    #==========================================================================
    # Rhythmbox signal callbacks
    #==========================================================================
    
    def __cb_sp_playing_uri_changed(self, sp, uri):
        """Shell player signal callback to handle a plob change."""
        
        log.debug("rb_playing_uri_changed: %s" % uri)
        
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
                    logging.warning("failed to save RB cover (%s)" % e)
                    img_file = None
    
            log.debug("image: %s" % str(img_file))
    
        else:
            id = None
            img_file = None
            info = None

        self.update_plob(id, info, img_file)
        
        # a new plob may result in a new position:
        self.__update_position()

    def __cb_sp_playing_changed(self, sp, b):
        """Shell player signal callback to handle a change in playback."""
        
        log.debug("sp_playing_changed: %s" % str(b))
        
        if b:
            self.update_playback(remuco.PLAYBACK_PLAY)
        else:
            self.update_playback(remuco.PLAYBACK_PAUSE)

    def __cb_sp_playlist_changed(self, sp, source_new):
        """Shell player signal callback to handle a playlist switch."""
        log.debug("new source: %s" % str(source_new))
        self.__playlist_sc = source_new
        
    #==========================================================================
    # Main context callbacks
    #==========================================================================

    def __cb_mc_poll_misc(self):
        """Timeout callback to periodically poll RB for state information
        without change signals.
        """ 
        
        sp = self.__shell.get_player()
        
        ###### check repeat and shuffle ######
        
        order = sp.props.play_order
        
        repeat = order == PLAYORDER_REPEAT or \
                 order.startswith(PLAYORDER_SHUFFLE_ALT)
                 
        self.update_repeat_mode(repeat)
        
        shuffle = order == PLAYORDER_SHUFFLE or \
                  order.startswith(PLAYORDER_SHUFFLE_ALT)
                  
        self.update_shuffle_mode(shuffle)
        
        ###### check volume ######

        volume = int(sp.get_volume() * 100)
        
        self.update_volume(volume)
        
        # TODO: test if __update_position() should be called here 

        return True

    #==========================================================================
    # helper methods
    #==========================================================================

    def __do_jump(self, path, index):
        """Jump to a specific song (index) in a specific source (path)."""
        
        source = self.__get_source_from_path(path)
        
        if not source:
            return
        
        qmodel = source.get_entry_view().props.model
        
        # FIXME: assuming entry view and query model are never None
        
        sp = self.__shell.get_player()

        id_to_remove_from_queue = None
        
        if sp.props.playing_from_queue:
            id_to_remove_from_queue = self.__plob_id

        if sp.props.playing_from_queue or source != self.__playlist_sc:
            sp.set_selected_source(source)
            sp.set_playing_source(source)
            
        found = False
        i = 0
        for row in qmodel:
            if i == index:
                sp.play_entry(row[0])
                found = True
                break
            i += 1
        
        if not found:
            sp.do_next()
        
        if id_to_remove_from_queue != None:
            log.debug("remove %s from queue" % id_to_remove_from_queue)
            self.__shell.remove_from_queue(id_to_remove_from_queue)

        # this works almost perfect now .. the only difference to directly
        # doing jumps in the RB UI is that when jumping to a song in the queue
        # which is not the first in the queue, the song does not get moved
        # to the top of the queue .. that's ok

    def __get_tracks_from_qmodel(self, qmodel):
        """Get all tracks in a query model.
        
        Returns 2 lists, first with IDs, second with names of the tracks.
        """
        
        db = self.__shell.props.db

        ids = []
        names = []

        if not qmodel:
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
        
        if not entry:
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
        
        'path' contains either the source' group and name (2 element list) or
        one of the well known IDs 'remuco.PLID_CURRENT' and 'remuco.PLID_QUEUE'
        (1 element list).
        """
        
        slm = self.__shell.props.sourcelist_model
        
        # FIXME: assuming 'slm' is never None
        
        group_name, source_name = path
        
        if not group_name or not source_name:
            return None
        
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

    #==========================================================================
    # update wrapper
    #==========================================================================

    def __update_position(self):
        """Determine the current position and update."""

        pfq = self.__shell.get_player().props.playing_from_queue

        self.update_play_position(self.__get_position(), queue=pfq)

if __name__ == "__main__":
    
    print "This python module only works within Rhythmbox."
    
