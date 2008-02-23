#!/usr/bin/python

import gobject
import rb, rhythmdb
import os
import remuco
import sys
import urlparse
import urllib

PLAYORDER_SHUFFLE = "shuffle"
PLAYORDER_SHUFFLE2 = "random-by-age-and-rating"
PLAYORDER_REPEAT = "linear-loop"
PLAYORDER_NORMAL = "linear"

COVER_FILE_NAMES = ("folder", "front", "album", "cover")
COVER_FILE_TYPES = ("png", "jpeg", "jpg")

DELIM = "__:;:___"

class RemucoPlugin(rb.Plugin):

    ###########################################################################
    #
    # rhythmbox plugin interface
    #
    ###########################################################################
    
    def __init__(self):
        
        rb.Plugin.__init__(self)
        
        remuco.log_info("Rhythmbox PP initialized.")
        
        self.callbacks = remuco.PPCallbacks()
        self.callbacks.snychronize = RemucoPlugin.cb_rem_synchronize
        self.callbacks.get_plob = RemucoPlugin.cb_rem_get_plob
        self.callbacks.notify = RemucoPlugin.cb_rem_notify
        self.callbacks.simple_control = RemucoPlugin.cb_rem_simple_control
        self.callbacks.get_library = RemucoPlugin.cb_rem_get_library
        self.callbacks.play_ploblist = RemucoPlugin.cb_rem_play_ploblist
        self.callbacks.get_ploblist = RemucoPlugin.cb_rem_get_ploblist
        
        self.descriptor = remuco.PPDescriptor()
        self.descriptor.player_name = "Rhythmbox"
        self.descriptor.max_rating_value = 5
        self.descriptor.supports_playlist = True
        self.descriptor.supports_playlist_jump = True
        self.descriptor.supports_queue = True
        self.descriptor.supports_queue_jump = False
        #descriptor.supports_seek = True

        ###### get file for cover cache ######
        
        home = os.getenv("HOME", "/var/tmp")
        cache = os.getenv("XDG_CACHE_HOME", "%s/.cache/remuco" % home)
        self.__cover_cache = "%s/rhythmbox-cover.png" % cache
        remuco.log_debug("cover cache: %s" % self.__cover_cache)

    def activate(self, shell):
        
        remuco.log_info("Rhythmbox-Remuco plugin activated")
        
        ###### init some vars ######
        
        self.shell = shell
        self.server = None
        
        self.__cap_pid = None
        self.__cap_entry = None
        self.__source = None
        self.__qmodel = None
        self.__queue = None
        self.__queue_qmodel = None
        self.__porder = None
        self.__volume = None
         
        sp = self.shell.props.shell_player
        
        ###### set up remuco server ######

        try:
            self.server = remuco.up(self.descriptor, self.callbacks, self)
        except:
            self.server = None
            remuco.log_error("error starting server: %s" % str(sys.exc_info()))
            raise RuntimeError("Remuco server could not be started")

        ###### register RB callback to get notified about changes ######

        self.cb_id_playing_changed = sp.connect( \
                        "playing_changed", self.cb_rb_playing_changed)
        self.cb_id_playing_uri_changed = sp.connect( \
                        "playing_uri_changed", self.cb_rb_playing_uri_changed)
            
        self.cb_id_source_changed = sp.connect( \
                        "playing-source-changed", self.cb_rb_source_changed)

        self.__queue = self.shell.props.queue_source
        
        if not self.__queue:
            self.__queue_qmodel = None
        else:
            self.__queue_qmodel = self.__queue.props.query_model

        # connect to 'entry change' signals from queue:
        self.cb_id_queue_row_inserted = self.__queue_qmodel.connect(\
                        "row-inserted", self.cb_rb_queue_row_inserted)
        self.cb_id_queue_row_deleted = self.__queue_qmodel.connect(\
                        "row-deleted", self.cb_rb_queue_row_deleted)
        self.cb_id_queue_rows_reordered = self.__queue_qmodel.connect(\
                        "rows-reordered", self.cb_rb_queue_rows_reordered)

        self.cb_id_poll_misc = gobject.timeout_add(3000, self.__cb_poll_misc)

        ###### initially trigger the server to synchronize ######
        
        self.cb_rb_source_changed(sp, sp.props.source)
        
        remuco.notify(self.server)
        
    def deactivate(self, shell):

        if not self.server: return
        
        remuco.log_info("Rhythmbox-Remuco plugin deactivated")
        
        server = self.server
        self.server = None

        sp = self.shell.props.shell_player

        ###### disconnect from rhythmbox callback sources ######
        
        gobject.source_remove(self.cb_id_poll_misc)
        
        sp.disconnect(self.cb_id_playing_changed)
        sp.disconnect(self.cb_id_playing_uri_changed)        

        sp.disconnect(self.cb_id_source_changed)
        
        # this disconnects from callbacks of current source and query model:
        self.cb_rb_source_changed(sp, None)
        
        # say bye to the queue
        self.__queue_qmodel.disconnect(self.cb_id_queue_row_inserted)
        self.__queue_qmodel.disconnect(self.cb_id_queue_row_deleted)
        self.__queue_qmodel.disconnect(self.cb_id_queue_rows_reordered)
        
        ###### shutdown the server ######
        
        remuco.down(server)
        
    ###########################################################################
    #
    # Remuco Callbacks
    #
    ###########################################################################
    
    def cb_rem_synchronize(self, ps):
        remuco.log_debug("rcb_synchronize called")
        
        sp = self.shell.props.shell_player
        db = self.shell.props.db
        
        ##### pbs #####
        
        if sp.props.playing:
            ps.pbs = remuco.PS_PBS_PLAY
        else:
            ps.pbs = remuco.PS_PBS_PAUSE
            
        ##### volume #####

        ps.volume = int(sp.get_volume() * 100)
        
        self.__volume = ps.volume
        
        ##### flags #####

        order = sp.props.play_order
        
        ps.flags = remuco.PS_FLAG_NONE
        
        if order == PLAYORDER_REPEAT:
            ps.flags = remuco.PS_FLAG_REPEAT
        elif order == PLAYORDER_SHUFFLE or order == PLAYORDER_SHUFFLE2:
            ps.flags = remuco.PS_FLAG_SHUFFLE
        
        self.__porder = order # checked later in self.__cb_poll_misc()
        
        ##### pid #####
        
        cap_entry = sp.get_playing_entry()
        if not cap_entry:
            ps.cap_pid = None
        else:
            ps.cap_pid = db.entry_get(cap_entry, rhythmdb.PROP_LOCATION)
        
        self.__cap_pid = ps.cap_pid
        self.__cap_entry = cap_entry
        
        ##### playlist #####
        
        ps.playlist = []

        if self.__source != None:
            
            ### playlist ###
            
            model = self.__source.get_entry_view().props.model
            remuco.log_debug("query model: %s" % str(model))
            for row in model:
                uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                ps.playlist.append(uri)
        
        ##### queue #####
        
        ps.queue = []

        if self.__queue_qmodel != None:
            
            for row in self.__queue_qmodel:
                uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                ps.queue.append(uri)
        
        ##### cap position #####
        
        try:
            if sp.props.playing_from_queue:
                ps.cap_pos = -(ps.queue.index(ps.cap_pid) + 1)
            else:
                ps.cap_pos = ps.playlist.index(ps.cap_pid) + 1
        except:
            ps.cap_pos = 0
        
        remuco.log_debug("rcb_synchronize done")
        
    def cb_rem_get_plob(self, pid):
    
        db = self.shell.props.db

        remuco.log_debug("cb_rem_get_plob(%s) called" % pid)
        
        if pid == self.__cap_pid:
            entry = self.__cap_entry # this one contains an image
        else:
            entry = db.entry_lookup_by_location(pid)
        
        if not entry:
            return { remuco.PLOB_META_TITLE : "No information" }
        
        img = db.entry_request_extra_metadata(entry, "rb:coverArt")
        if not img:
            file = self.__cover_from_pid(pid)
        else:
            img.save(self.__cover_cache, "png")
            file = self.__cover_cache

        remuco.log_debug("image: %s" % str(file))

        plob = {remuco.PLOB_META_TITLE : db.entry_get(entry, rhythmdb.PROP_TITLE), \
                remuco.PLOB_META_ARTIST: db.entry_get(entry, rhythmdb.PROP_ARTIST), \
                remuco.PLOB_META_ALBUM : db.entry_get(entry, rhythmdb.PROP_ALBUM), \
                remuco.PLOB_META_GENRE : db.entry_get(entry, rhythmdb.PROP_GENRE), \
                remuco.PLOB_META_BITRATE : db.entry_get(entry, rhythmdb.PROP_BITRATE), \
                remuco.PLOB_META_LENGTH : db.entry_get(entry, rhythmdb.PROP_DURATION), \
                remuco.PLOB_META_RATING : int(db.entry_get(entry, rhythmdb.PROP_RATING)), \
                remuco.PLOB_META_TRACK : db.entry_get(entry, rhythmdb.PROP_TRACK_NUMBER), \
                remuco.PLOB_META_YEAR : db.entry_get(entry, rhythmdb.PROP_YEAR), \
                remuco.PLOB_META_ART : file \
               }

        return plob
        
    def cb_rem_notify(self, event):
        
        remuco.log_debug("cb_rem_notify called ")
    
        if event == remuco.SERVER_EVENT_DOWN:
            remuco.log_debug("EVENT: server is down now")        
        elif even == remuco.SERVER_EVENT_ERROR:
            remuco.log_debug("EVENT: server crashed")        
            remuco.down(self.server)
        else:
            remuco.log_warn("EVENT: unknown")
        return
    
    def cb_rem_simple_control(self, cmd, param):
        
        remuco.log_debug("cb_rem_simple_control(%i, %i) called" % (cmd, param))
    
        sp = self.shell.props.shell_player
        db = self.shell.props.db
    
        try:
            if cmd == remuco.SCTRL_CMD_STOP:
                
                sp.stop()
                
            elif cmd == remuco.SCTRL_CMD_PLAYPAUSE:
                
                sp.playpause()
                
            elif cmd == remuco.SCTRL_CMD_NEXT:
                
                sp.do_next()
                
            elif cmd == remuco.SCTRL_CMD_PREV:
                
                sp.do_previous()
                
            elif cmd == remuco.SCTRL_CMD_VOLUME:
                
                sp.set_volume(float(param) / 100)
                
                remuco.notify(self.server) # volume change
                
            elif cmd == remuco.SCTRL_CMD_JUMP:
                
                # jump to a queue position is not supported
                if param < 0: return
                
                # RB forgets to remove a song from the queue if it is playing
                # and we jump tp another song, so we remove it now manually:
                if sp.props.playing_from_queue:
                    if self.__cap_pid != None:
                        sp.pause()
                        remuco.log_debug("remove %s from queue" % self.__cap_pid)
                        self.shell.remove_from_queue(self.__cap_pid)
                
                if self.__qmodel != None:
                    i = 1
                    for row in self.__qmodel:
                        if i == param:
                            #if sp.get_playing_source != self.__source:
                            #    sp.set_playing_source(self.__source)
                            sp.play_entry(row[0])
                            break
                        i += 1
                
            elif cmd == remuco.SCTRL_CMD_RATE:
                
                entry = sp.get_playing_entry()
                db.set(entry, rhythmdb.PROP_RATING, param)
                
#            elif cmd == remuco.SCTRL_CMD_SEEK:
            else:
                remuco.log_warn("command %d not supported", cmd)
        except:
            remuco.log_error(str(sys.exc_info()))
        else:
            remuco.log_debug("sctrl processed")

    def cb_rem_get_library(self):
        
        plids = []
        names = []
        flags = []
        
        slm = self.shell.props.sourcelist_model
        
        if not slm:
            return (plids, names, flags)
        
        for group in slm:
            group_name = group[2]
            for source in group.iterchildren():
                source_name = source[2]
                plids.append("%s%s%s" % (group_name, DELIM, source_name))
                remuco.log_debug(plids[plids.__len__() - 1])
                names.append(source_name)
                flags.append(0)
        
        return (plids, names, flags)

    def cb_rem_play_ploblist(self, plid):
        
        source = self.__get_source(plid)
        
        if not source:
            return
        
        sp = self.shell.props.shell_player
        sp.set_selected_source(source)
        sp.set_playing_source(source)
        sp.do_next()
                        
    def cb_rem_get_ploblist(self, plid):
        
        pl = []
        
        source = self.__get_source(plid)
        
        if not source:
            return pl
        
        db = self.shell.props.db

        model = source.get_entry_view().props.model
        
        for row in model:
            uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
            pl.append(uri)

        return pl

    ###########################################################################
    #
    # Rhythmbox Callbacks
    #
    ###########################################################################
    
    def cb_rb_playing_uri_changed(self, sp, uri):
        remuco.log_debug("cb_rb_playing_uri_changed: %s" % uri)
        if not self.server: return
        remuco.notify(self.server)
            
    def cb_rb_playing_changed(self, sp, b):
        remuco.log_debug("cb_rb_playing_changed: %s" % str(b))
        if not self.server: return
        remuco.notify(self.server)
        
    def cb_rb_source_changed(self, sp, source):
        """Obtains the query model related to the new source."""

        if self.__source == source:
            return
        
        remuco.log_debug("cb_rb_source_changed: %s" % str(source))
        
        ###### disconnect callbacks of old source ######
        
        if self.__source != None:
            
            self.__source.disconnect(self.cb_id_pl_filter_changed)
        
        self.__source = source
        
        ###### connect to callbacks of new source ######
        
        if self.__source != None:

            self.cb_id_pl_filter_changed = self.__source.connect(\
                            "filter-changed", self.cb_rb_source_filter_changed)

        ###### handle the new query model (if there is one) ######
        
        try:
            qmodel = self.__source.get_entry_view().props.model 
        except:
            qmodel = None

        self.__qmodel_changed(qmodel)

    def cb_rb_source_filter_changed(self, source):
        """Obtains the query model related to the source' new filter."""
        
        remuco.log_debug("source filter changed: %s" % str(source))
        
        ###### handle the new query model (if there is one) ######

        try:
            qmodel = self.__source.get_entry_view().props.model 
        except:
            qmodel = None

        self.__qmodel_changed(qmodel)

    def cb_rb_qmodel_row_inserted(self, qm, gtp, gti):
        #remuco.log_debug("qmodel row inserted (last)")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_qmodel_row_deleted(self, qm, gtp):
        #remuco.log_debug("qmodel row deleted")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_qmodel_rows_reordered(self, qm, gtp, gti, gp):
        #remuco.log_debug("qmodel rows reordered")
        if not self.server: return
        remuco.notify(self.server)
 
    def cb_rb_queue_row_inserted(self, qm, gtp, gti):
        remuco.log_debug("queue row inserted")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_queue_row_deleted(self, qm, gtp):
        remuco.log_debug("queue row deleted")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_queue_rows_reordered(self, qm, gtp, gti, gp):
        remuco.log_debug("queue rows reordered")
        if not self.server: return
        remuco.notify(self.server) 

    ###########################################################################
    #
    # Misc
    #
    ###########################################################################

    def __cb_poll_misc(self):
        """Polls RB for state information without change signals.""" 
        
        sp = self.shell.props.shell_player
        
        order = sp.props.play_order
        
        if order != self.__porder:
            remuco.notify(self.server)
        
        volume = int(sp.get_volume() * 100)

        if volume != self.__volume:
            remuco.notify(self.server)
        
        return True
 
    def __qmodel_changed(self, qmodel):
        """Configures callback functions for the new query model.
        
        Disconnects from callbacks of old query model and connects to callbacks
        of the new query model. The callbacks get called if entries get inserted,
        removed or reordered in the query model."""
        
        if self.__qmodel == qmodel:
            return

        remuco.log_debug("__qmodel_changed: %s" % str(qmodel))

        ###### disconnect callbacks of old query model ######

        if self.__qmodel != None:
            
            self.__qmodel.disconnect(self.cb_id_pl_row_added)
            self.__qmodel.disconnect(self.cb_id_pl_row_deleted)
            self.__qmodel.disconnect(self.cb_id_pl_rows_reordered)
            
        self.__qmodel = qmodel
        
        ###### connect to callbacks of new query model ######

        if self.__qmodel != None:
            
            self.cb_id_pl_row_added = self.__qmodel.connect(\
                            "row-inserted", self.cb_rb_qmodel_row_inserted)
            self.cb_id_pl_row_deleted = self.__qmodel.connect(\
                            "row-deleted", self.cb_rb_qmodel_row_deleted)
            self.cb_id_pl_rows_reordered = self.__qmodel.connect(\
                            "rows-reordered", self.cb_rb_qmodel_rows_reordered)
            
        ###### notify the server (playlist change) ######

        if self.server != None:
            remuco.notify(self.server)
        
        return

    def __cover_from_pid(self, pid):
        """Returns the full path to a cover file related to pid.
        
        Returns 'None' if no cover has been found in the songs folder.
        """
        
        elems = urlparse.urlparse(pid)
        
        if elems[0] != "file":
            return None
        
        path = urllib.url2pathname(elems[2])
        path = os.path.dirname(path)
        
        for name in COVER_FILE_NAMES:
            for type in COVER_FILE_TYPES:
                file = os.path.join(path, "%s.%s" % (name, type))
                if os.path.isfile(file):
                    return file
                file_cap = os.path.join(path, "%s.%s" % (name.capitalize(), type))
                if os.path.isfile(file_cap):
                    return file_cap
                
        return None

    def __get_source(self, plid):
        """Get the source object of source 'plid'."""
        
        slm = self.shell.props.sourcelist_model
        
        if not slm:
            return None

        group_name, source_name = plid.split(DELIM)
        
        if not group_name or not source_name:
            return None
        
        for group in slm:
            if group_name == group[2]:
                for source in group.iterchildren():
                    if source_name == source[2]:
                        return source[3]

if __name__ == "__main__":
    
    print "This is a python module."
    