import gobject
import os
import os.path
import sys
import traceback
import urllib
import urlparse

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
        
        print("remuco plugin activated")

    def deactivate(self, shell):
    
        print("deactivate Remuco plugin ..")

        if self.__rem is not None:
            self.__rem.down()
            self.__rem = None

###############################################################################
#
# Player Proxy
#
###############################################################################



class Remythm(remuco.Player):

    def __init__(self, shell):
        
        remuco.Player.__init__(self, "Rhythmbox")
        
        self.__shell = shell
        
        self.__cover_file = "%s/cover.png" % self.get_cache_dir()

        ###### shortcuts to RB data ###### 
        
        self.__plob_id = None
        self.__plob_entry = None
        self.__playlist_sc = None
        self.__playlist_qm = None
        self.__queue_sc = None
        self.__queue_qm = None
        
        ###### callback IDs ###### 

        self.__cb_ids_sp = ()
        self.__cb_ids_qm_queue = ()
        self.__cb_ids_sc_playlist = ()
        
        self.__cb_id_mc_update_playlist = None
        self.__cb_id_mc_update_queue = None

        ###### connect to shell player signals ######

        sp = self.__shell.props.shell_player
        
        self.__cb_ids_sp = (
            sp.connect("playing_changed", self.__cb_sp_playing_changed),
            sp.connect("playing_uri_changed", self.__cb_sp_playing_uri_changed),
            sp.connect("playing-source-changed", self.__cb_sp_playlist_changed)
        )

        ###### connect to playlist signals ######

        self.__cb_sp_playlist_changed(sp, sp.props.source)

        ###### connect to queue signals ######

        # FIXME: assuming queue source and query model is never None

        sc_queue = self.__shell.props.queue_source
        qm_queue = sc_queue.props.query_model
        
        self.__queue_sc = sc_queue
        self.__queue_qm = qm_queue

        self.__cb_ids_qm_queue = (
            qm_queue.connect("row-inserted", self.__cb_qm_queue_row_inserted),
            qm_queue.connect("row-deleted", self.__cb_qm_queue_row_deleted),
            qm_queue.connect("rows-reordered", self.__cb_qm_queue_rows_reordered)
        )

        ###### periodically check for changes which have no signals ######

        self.__cb_id_mc_poll_misc = \
            gobject.timeout_add(3000, self.__cb_mc_poll_misc)

        ###### initially trigger server synchronization ######
        
        # state sync will happen by timeout
        # playlist sync already triggered above 
        self.__cb_sp_playing_uri_changed(sp, sp.get_playing_path()) # plob sync
        self.__update_queue() # queue sync
        
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
    
    def jump_to(self, playlist, position):
        """ Jump to a specific position in a specific playlist.
        
        For some players it is better to say 'load a specific playlist and then
        jump to a specific position'.
        
        @param playlist: ID of the playlist to jump into (string)
        @param position: position within the playlist to jump to (int)
        
        @note: playlist may be one of the well known IDs PLID_PLAYLIST and
               PLID_QUEUE - this means a jump within the current playlist or
               queue.

        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        self.__do_jump(playlist, position)
    
    def play_next(self):
        """ Play the next item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.props.shell_player
        if sp is not None:
            sp.do_next()
    
    def play_previous(self):
        """ Play the previous item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.props.shell_player
        if sp is not None:
            sp.do_previous()
    
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
        sp = self.__shell.props.shell_player
        if sp is not None:
            sp.playpause()
                
    
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
        sp = self.__shell.props.shell_player
        if sp is not None:
            sp.seek(SEEK_STEP)
    
    def seek_backward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        sp = self.__shell.props.shell_player
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
        sp = self.__shell.props.shell_player
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
        
    def request_playlist(self, id, client):
        """ Request contents of a specific playlist 
        
        @param id: ID of the requested playlist (string)
        @param client: the requesting client - use reply_list_request()
                       to send back the requested playlist
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client request a playlist from the player.
               
        @see reply_list_request()
        """
        log.debug("called RequestPloblist(%s)" % id)

        nested_ids = []
        nested_names = []
        ids = []
        names = []
        
        slm = self.__shell.props.sourcelist_model
        
        if not slm:
            gobject.idle_add(self.reply_list_request, client, id,
                             nested_ids, nested_names, ids, names)
            return

        ### root ? ###

        if id is None or len(id) == 0:
            for group in slm:
                group_name = group[2]
                nested_ids.append(group_name)
                nested_names.append(group_name)
                gobject.idle_add(self.reply_list_request, client, id,
                                 nested_ids, nested_names, ids, names)
                return
        
        ### group ? ###
        
        if id.find(DELIM) == -1:
            for group in slm:
                group_name = group[2]
                if id == group_name:
                    for source in group.iterchildren():
                        source_name = source[2]
                        log.debug("append %s%s%s" %
                                  (group_name, DELIM, source_name))
                        nested_ids.append("%s%s%s" %
                                          (group_name, DELIM, source_name))
                        nested_names.append(source_name)
                    break
            gobject.idle_add(self.reply_list_request, client, id,
                             nested_ids, nested_names, ids, names)
            return
            
        ### regular playlist (source) ! ###
        
        source = self.__get_source_from_plid(id)

        if not source:
            gobject.idle_add(self.reply_list_request, client, id,
                             nested_ids, nested_names, ids, names)
            return

        model = source.get_entry_view().props.model
        
        ids, names = self.__get_tracks_from_qmodel(model)
        
        gobject.idle_add(self.reply_list_request, client, id,
                         nested_ids, nested_names, ids, names)

        
    def down(self):
        
        remuco.Player.down(self)

        if self.__shell is None:
            return

        ###### disconnect from shell player signals ######

        # FIXME: assuming shell player is never None
        
        sp = self.__shell.props.shell_player

        for cb_id in self.__cb_ids_sp:
            
            sp.disconnect(cb_id)
            
        self.__cb_ids_sp = ()

        ###### disconnect from playlist signals ######

        self.__cb_sp_playlist_changed(sp, None)
        
        ###### disconnect from queue signals ######
        
        # FIXME: assuming queue query model is never None
        
        qmodel = self.__queue_qm
        
        for cb_id in self.__cb_ids_qm_queue:
            
            qmodel.disconnect(cb_id)
            
        self.__cb_ids_qm_queue = ()
        
        ###### remove gobject sources ######
        
        if self.__cb_id_mc_poll_misc > 0:
            gobject.source_remove(self.__cb_id_mc_poll_misc)

        if self.__cb_id_mc_update_playlist is not None:
            gobject.source_remove(self.__cb_id_mc_update_playlist)
            
        if self.__cb_id_mc_update_queue is not None:
            gobject.source_remove(self.__cb_id_mc_update_queue)

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

        source_old = self.__playlist_sc
        
        if source_old == source_new:
            return
        
        log.debug("sp_playlist_changed: %s" % str(source_new))
        
        ###### disconnect signals of old source ######
        
        if source_old != None:
            for cb_id in self.__cb_ids_sc_playlist:
                source_old.disconnect(cb_id)
            self.__cb_ids_sc_playlist = ()
        
        self.__playlist_sc = source_new
        
        ###### connect to signals of new source and its query model ######
        
        if source_new != None:

            self.__cb_ids_sc_playlist = (
                source_new.connect("filter-changed", self.__cb_sc_playlist_filter_changed),
            )
            
            qmodel = source_new.get_entry_view().props.model
             
        else:
            
            qmodel = None

        self.__handle_playlist_qmodel_changed(qmodel)

    def __cb_sc_playlist_filter_changed(self, source):
        """Source signal callback to handle a playlist filter change."""
        
        log.debug("sc_playlist_filter_changed: %s" % str(source))
        
        ###### handle the new query model (if there is one) ######

        try:
            qmodel = self.__playlist_sc.get_entry_view().props.model 
        except:
            qmodel = None

        self.__handle_playlist_qmodel_changed(qmodel)

    def __cb_qm_playlist_row_inserted(self, qm, gtp, gti):
        #log.debug("playlist row inserted (last)")
        self.__update_playlist()
    
    def __cb_qm_playlist_row_deleted(self, qm, gtp):
        #log.debug("playlist row deleted")
        self.__update_playlist()
    
    def __cb_qm_playlist_rows_reordered(self, qm, gtp, gti, gp):
        #log.debug("playlist rows reordered")
        self.__update_playlist()
 
    def __cb_qm_queue_row_inserted(self, qm, gtp, gti):
        log.debug("queue row inserted")
        self.__update_queue()
    
    def __cb_qm_queue_row_deleted(self, qm, gtp):
        log.debug("queue row deleted")
        self.__update_queue()
    
    def __cb_qm_queue_rows_reordered(self, qm, gtp, gti, gp):
        log.debug("queue rows reordered")
        self.__update_queue()

    #==========================================================================
    # Main context callbacks
    #==========================================================================

    def __cb_mc_poll_misc(self):
        """Timeout callback to periodically poll RB for state information
        without change signals.
        """ 
        
        sp = self.__shell.props.shell_player
        
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

        return True

    #==========================================================================
    # helper methods
    #==========================================================================

    def __do_jump(self, id, index):
        """Jump to a specific song (index) in a specific source (id)."""
        
        source = self.__get_source_from_plid(id)
        
        if not source:
            return
        
        qmodel = source.get_entry_view().props.model
        
        # FIXME: assuming entry view and query model are never None
        
        sp = self.__shell.props.shell_player

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

    def __handle_playlist_qmodel_changed(self, qmodel_new):
        """Connect to signals from the new playlist query model.
        
        This includes disconnecting from signals of the old query model and.
        """
        
        qmodel_old = self.__playlist_qm
        
        if qmodel_old == qmodel_new:
            return

        log.debug("playlist query model changed: %s" % str(qmodel_new))

        ###### disconnect from signals of old query model ######

        if qmodel_old is not None:
            
            for cb_id in self.__cb_ids_qm_playlist:
                qmodel_old.disconnect(cb_id)
                
        self.__cb_ids_qm_playlist = ()
            
        self.__playlist_qm = qmodel_new
        
        ###### connect to signals of new query model ######

        if qmodel_new is not None:
            
            self.__cb_ids_qm_playlist = (
                qmodel_new.connect("row-inserted", self.__cb_qm_playlist_row_inserted),
                qmodel_new.connect("row-deleted", self.__cb_qm_playlist_row_deleted),
                qmodel_new.connect("rows-reordered", self.__cb_qm_playlist_rows_reordered)
            )
        
        ###### sync with the server (playlist change) ######

        self.__update_playlist()
        
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

    def __get_source_from_plid(self, id):
        """Get the source object of source 'id'.
        
        'id' is either a combination of the source' group and name or one of
        the well known IDs 'remuco.PLID_PLAYLIST' and 'remuco.PLID_QUEUE'.
        """
        
        if id == remuco.PLID_PLAYLIST:
            return self.__playlist_sc
        
        if id == PLID_QUEUE:
            return self.__queue_sc
        
        slm = self.__shell.props.sourcelist_model
        
        # FIXME: assuming 'slm' is never None
        
        group_name, source_name = id.split(DELIM)
        
        if not group_name or not source_name:
            return None
        
        for group in slm:
            if group_name == group[2]:
                for source in group.iterchildren():
                    if source_name == source[2]:
                        return source[3]

    #==========================================================================
    # update wrapper
    #==========================================================================

    def __update_position(self):
        """Determine the current position and update."""

        sp = self.__shell.props.shell_player

        db = self.__shell.props.db

        position = 0
        queue = False
        
        id_now = self.__plob_id
        
        if id_now is not None:
            
            if sp.props.playing_from_queue:
                queue = True
                qmodel = self.__queue_qm
            else:
                qmodel = self.__playlist_qm
                
            if qmodel is not None:
                for row in qmodel:
                    id = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                    if id_now == id:
                        break
                    position += 1
                    
        log.debug ("position: %i" % position)
        
        self.update_play_position(position)

    def __update_playlist(self):
        """Get current playlist and update."""

        if self.__cb_id_mc_update_playlist is None:
            self.__cb_id_mc_update_playlist = gobject.idle_add(
                self.__update_playlist_delayed, priority=gobject.PRIORITY_LOW)
            
    def __update_playlist_delayed(self):
        
        log.debug("playlist query model: %s" % str(self.__playlist_qm))

        ids, names = self.__get_tracks_from_qmodel(self.__playlist_qm)
        
        self.update_playlist(ids, names)

        # a new playlist may result in a new position:
        self.__update_position()
        
        self.__cb_id_mc_update_playlist = None

    def __update_queue(self):
        """Get current queue and update."""

        if self.__cb_id_mc_update_queue is None:
            self.__cb_id_mc_update_queue = gobject.idle_add(
                self.__update_queue_delayed, priority=gobject.PRIORITY_LOW)

    def __update_queue_delayed(self):

        log.debug("queue query model: %s" % str(self.__queue_qm))

        ids, names = self.__get_tracks_from_qmodel(self.__queue_qm)
        
        self.update_queue(ids, names)

        # a new playlist may result in a new position:
        self.__update_position()

        self.__cb_id_mc_update_queue = None

if __name__ == "__main__":
    
    print "This python module only works within Rhythmbox."
    
