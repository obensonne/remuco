import gobject, gtk
import rb, rhythmdb
import os
import remuco
import sys

PLAYORDER_SHUFFLE = "shuffle"
PLAYORDER_SHUFFLE2 = "random-by-age-and-rating"
PLAYORDER_REPEAT = "linear-loop"
PLAYORDER_NORMAL = "linear"

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
        
        self.descriptor = remuco.PPDescriptor()
        self.descriptor.player_name = "Rhythmbox"
        self.descriptor.max_rating_value = 5
        self.descriptor.supports_playlist = True
        self.descriptor.supports_playlist_jump = True
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
        self.__eview = None
        self.__qmodel = None
         
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

        ###### initially trigger the server to synchronize ######
        
        self.cb_rb_source_changed(sp, sp.props.source)
        
        remuco.notify(self.server)
        
    def deactivate(self, shell):
        
        remuco.log_info("Rhythmbox-Remuco plugin deactivated")
        
        sp = self.shell.props.shell_player

        ###### disconnect from rhythmbox callback sources ######
        
        sp.disconnect(self.cb_id_playing_changed)
        sp.disconnect(self.cb_id_playing_uri_changed)        

        sp.disconnect(self.cb_id_source_changed)
        
        # this disconnects from callbacks of entry view model:
        self.cb_rb_source_changed(sp, None)
        
        ###### shutdown the server ######
        
        if self.server != None:
            server = self.server
            self.server = None
            remuco.down(server)
        
    def tick(self):
        
        #gobject.timeout_add(2000, self.tick)
        remuco.log_debug("tick")
        if not self.server:
            return False
        else:
            remuco.notify(self.server)
            return True
        
    def idle_cb(self):
        remuco.log_debug("idle")
        return False
        
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
        
        ##### flags #####

        order = sp.props.play_order
        
        ps.flags = remuco.PS_FLAG_NONE
        
        if order == PLAYORDER_REPEAT:
            ps.flags = remuco.PS_FLAG_REPEAT
        elif order == PLAYORDER_SHUFFLE or order == PLAYORDER_SHUFFLE2:
            ps.flags = remuco.PS_FLAG_SHUFFLE
        
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

        ps.cap_pos = 0
        
        if self.__source != None:
            
            ### playlist ###
            
            model = self.__source.get_entry_view().props.model
            remuco.log_debug("query model: %s" % str(model))
            for row in model:
                uri = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                ps.playlist.append(uri)
        
            ### playlist postion ###
            
            try:
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
            file = None
            remuco.log_debug("image: no")
        else:
            img.save(self.__cover_cache, "png")
            file = self.__cover_cache
            remuco.log_debug("image: yes (%s)" % str(file))
        
        plob = {remuco.PLOB_META_TITLE : db.entry_get(entry, rhythmdb.PROP_TITLE), \
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
            remuco.log_error("ERROR: unknown event")
            self.deactivate(self.shell)
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
                
            elif cmd == remuco.SCTRL_CMD_JUMP:
                
                if sp.props.source != None:
                    i = 1
                    model = sp.props.source.get_entry_view().props.model
                    for row in model:
                        if i == param:
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

        if self.__source == source:
            return
        
        remuco.log_debug("cb_rb_source_changed: %s" % str(source))
        
        ###### disconnect callbacks of old entry view model ######
        
        if self.__source != None:
            
            self.__source.disconnect(self.cb_id_pl_filter_changed)
        
        self.__source = source
        
        ###### connect to callbacks of new entry view model ######
        
        if self.__source != None:

            self.cb_id_pl_filter_changed = self.__source.connect(\
                            "filter-changed", self.cb_rb_source_filter_changed)

            self.__eview_changed(self.__source.get_entry_view())
        
        else:
            
            self.__eview_changed(None)
        
        if not self.server: return
        remuco.notify(self.server)

    def __eview_changed(self, eview):
        
        if self.__eview == eview:
            return
        
        remuco.log_debug("__eview_changed: %s" % str(eview))

        if self.__eview != None:
            
            self.__eview.disconnect(self.cb_id_pl_entry_added)
            self.__eview.disconnect(self.cb_id_pl_entry_deleted)
            self.__eview.disconnect(self.cb_id_pl_selction_changed)
            self.__eview.disconnect(self.cb_id_pl_sort_order_changed)            
            
        self.__eview = eview
        
        if self.__eview != None:
        
            self.cb_id_pl_entry_added = self.__eview.connect(\
                            "entry-added", self.cb_rb_eview_entry_added)
            self.cb_id_pl_entry_deleted = self.__eview.connect(\
                            "entry-deleted", self.cb_rb_eview_entry_deleted)
            self.cb_id_pl_selction_changed = self.__eview.connect(\
                            "selection-changed", self.cb_rb_eview_selection_changed)
            self.cb_id_pl_sort_order_changed = self.__eview.connect(\
                            "sort-order-changed", self.cb_rb_eview_sort_order_changed)
        
            self.__qmodel_changed(self.__eview.props.model)
            
        else:

            self.__qmodel_changed(None)
        
        return
        
    def __qmodel_changed(self, qmodel):

        if self.__qmodel == qmodel:
            return

        remuco.log_debug("__qmodel_changed: %s" % str(qmodel))

        if self.__qmodel != None:
            
            self.__qmodel.disconnect(self.cb_id_pl_row_added)
            self.__qmodel.disconnect(self.cb_id_pl_row_deleted)
            self.__qmodel.disconnect(self.cb_id_pl_rows_reordered)
            
        self.__qmodel = qmodel
        
        if self.__qmodel != None:
            
            self.cb_id_pl_row_added = self.__qmodel.connect(\
                            "row-inserted", self.cb_rb_qmodel_row_inserted)
            self.cb_id_pl_row_deleted = self.__qmodel.connect(\
                            "row-deleted", self.cb_rb_qmodel_row_deleted)
            # wird aufgerufen, wenn man mit der muas einen song nach oben oder
            # unten schiebt
            self.cb_id_pl_rows_reordered = self.__qmodel.connect(\
                            "rows-reordered", self.cb_rb_qmodel_rows_reordered)
            
        if self.server != None:
            remuco.notify(self.server)
        
        return

    def cb_rb_qmodel_row_inserted(self, qm, gtp, gti):
        #if not self.connected_sqm.has_pending_changes():
        remuco.log_debug("qmodel row inserted (last)")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_qmodel_row_deleted(self, qm, gtp):
        remuco.log_debug("qmodel row deleted")
        if not self.server: return
        remuco.notify(self.server)
    
    def cb_rb_qmodel_rows_reordered(self, qm, gtp, gti, gp):
        remuco.log_debug("qmodel rows reordered")
        if not self.server: return
        remuco.notify(self.server)
 
    def cb_rb_source_filter_changed(self, source):
        remuco.log_debug("source filter changed")
        if not self.server: return
        remuco.notify(self.server)

    def cb_rb_eview_entry_added(self, view, entry):
        remuco.log_debug("eview entry added")
        if not self.server: return
        remuco.notify(self.server)

    def cb_rb_eview_entry_deleted(self, view, entry):
        remuco.log_debug("eview entry deleted")
        if not self.server: return
        remuco.notify(self.server)

    def cb_rb_eview_selection_changed(self, eview):
        remuco.log_debug("eview selection changed")
        if eview != None:
            self.__qmodel_changed(eview.props.model)
        else:
            self.__qmodel_changed(None)

    def cb_rb_eview_sort_order_changed(self, view):
        remuco.log_debug("eview sort order changed")
        if not self.server: return
        remuco.notify(self.server)


if __name__ == "__main__":
    
    print "hallo"
    