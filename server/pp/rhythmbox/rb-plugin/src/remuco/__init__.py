import gobject, gtk
import rb, rhythmdb
import os

REM_USER = os.environ['LOGNAME']
REM_PL_FILE = "/var/tmp/remuco-rhythmbox." + REM_USER

class RemucoPlugin(rb.Plugin):

    ###########################################################################
    #
    # rhythmbox plugin interface
    #
    ###########################################################################

    def __init__(self):
        rb.Plugin.__init__(self)
            
    def activate(self, shell):
        
        print "activating remuco plugin"
        
        # Attribute declaration:
        self.cb_id_elapsed_changed = None
        self.cb_id_playing_changed = None
        self.cb_id_playing_uri_changed = None
        self.cb_id_queue_row_added = None
        self.cb_id_queue_row_deleted = None
        self.cb_id_queue_rows_reordered = None
        self.cb_id_source_changed = None
        self.cb_id_source_filter_changed = None
        self.cb_id_source_row_added = None
        self.cb_id_source_row_deleted = None
        self.cb_id_source_rows_reordered = None
        self.connected_source = None
        self.source_saq = None
        self.connected_sqm = None
        self.db = shell.get_property("db")
        self.source_change = False
        self.qqm = None
        self.queue_change = False
        self.shell = shell
        self.sp = shell.get_player()
        
        # reset the playlist file:
        f = open(REM_PL_FILE, "w")
        f.write("")
        f.close()

        source = self.get_source()
        self.cb_fn_source_changed(self.sp, source)
        
        source = self.shell.get_property("queue-source")
        if not source:
            self.qqm = None
        else:
            self.qqm = source.props.query_model
        
        ###### register signal callbacks ######
        
        # we use this as a ticker to get alive regulary and print out the list
        # if needed
        self.cb_id_elapsed_changed = self.sp.connect( \
                            "elapsed-changed", self.cb_fn_elapsed_changed)
        
        # conncet to 'source changed' signal:
        self.cb_id_source_changed = self.sp.connect( \
                            "playing-source-changed", self.cb_fn_source_changed)
        
        # connect to 'entry change' signals from queue:
        self.cb_id_queue_row_added = self.qqm.connect(\
                        "row-inserted", self.cb_fn_queue_row_inserted)
        self.cb_id_queue_row_deleted = self.qqm.connect(\
                        "row-deleted", self.cb_fn_queue_row_deleted)
        self.cb_id_queue_rows_reordered = self.qqm.connect(\
                        "rows-reordered", self.cb_fn_queue_rows_reordered)
        
        # connect to 'playing_uri_changed' and 'playing_cahnged':
        self.cb_id_playing_changed = self.sp.connect(\
                        "playing_changed", self.cb_fn_playing_changed)
        self.cb_id_playing_uri_changed = self.sp.connect(\
                        "playing_uri_changed", self.cb_fn_playing_uri_changed)
        
    def deactivate(self, shell):
        print "deactivating remuco plugin"
        
        ###### derigister signal callbacks ######
        
        self.sp.disconnect(self.cb_id_elapsed_changed)

        self.sp.disconnect(self.cb_id_source_changed)
        
        self.sp.disconnect(self.cb_id_playing_changed)
        self.sp.disconnect(self.cb_id_playing_uri_changed)
        
        self.sqm_signals_disconnect(self.connected_sqm)
        
        self.qqm.disconnect(self.cb_id_queue_row_added)
        self.qqm.disconnect(self.cb_id_queue_row_deleted)
        self.qqm.disconnect(self.cb_id_queue_rows_reordered)
        
        if self.connected_source:
            self.connected_source.disconnect(self.cb_id_source_filter_changed)
            self.connected_source = None
            self.cb_id_source_filter_changed = None

        ###### other cleanup ######
        
        # reset the playlist file:
        f = open(REM_PL_FILE, "w")
        f.write("")
        f.close()
        
        # Attritbue un-declaration:
        del self.cb_id_elapsed_changed
        del self.cb_id_playing_changed
        del self.cb_id_playing_uri_changed
        del self.cb_id_queue_row_added
        del self.cb_id_queue_row_deleted
        del self.cb_id_queue_rows_reordered
        del self.cb_id_source_changed
        del self.cb_id_source_filter_changed
        del self.cb_id_source_row_added
        del self.cb_id_source_row_deleted
        del self.cb_id_source_rows_reordered
        del self.connected_source
        del self.source_saq
        del self.connected_sqm
        del self.db
        del self.source_change
        del self.qqm
        del self.queue_change
        del self.shell
        del self.sp
    
    
    ###########################################################################
    #
    # misc
    #
    ###########################################################################
    
    def sqm_signals_connect(self, sqm):
        if not sqm:
            return
        print "connect to signals of sqm %s" % str(sqm)
        self.cb_source_row_added_id = sqm.connect( \
                "row-inserted", self.cb_fn_source_row_inserted)
        self.cb_source_row_deleted_id = sqm.connect( \
                "row-deleted", self.cb_fn_source_row_deleted)
        self.cb_source_rows_reordered_id = sqm.connect( \
                "rows-reordered", self.cb_fn_source_rows_reordered)
    
    def sqm_signals_disconnect(self, sqm):
        if not sqm:
            return
        print "disconnect from signals of sqm %s" % str(sqm)
        if self.cb_source_row_added_id:
            sqm.disconnect(self.cb_source_row_added_id)
            self.cb_source_row_added_id = None
        if self.cb_source_row_deleted_id:
            sqm.disconnect(self.cb_source_row_deleted_id)
            self.cb_source_row_deleted_id = None
        if self.cb_source_rows_reordered_id:
            sqm.disconnect(self.cb_source_rows_reordered_id)
            self.cb_source_rows_reordered_id = None
    
    def get_source(self):
        source = self.sp.get_playing_source()
        if not source:
            source = self.shell.get_property("selected-source")
        return source
    
    def source_update(self, source):
        
        self.source_change = True

        # deregister signal callbacks from old source
        if self.connected_sqm:
            self.sqm_signals_disconnect(self.connected_sqm)
            self.connected_sqm = None

        # register signal callbacks to new source
        if source:
            self.connected_sqm = source.props.query_model
            self.sqm_signals_connect(self.connected_sqm)
        
    def out_all(self):
        print "out all"

        self.source_change = False
        self.queue_change = False
        
        pl = []
        source_continue_pos = 0
        pfq = self.sp.get_property("playing-from-queue")
        sqm = self.connected_sqm    
        pe = self.sp.get_playing_entry()    
        # TODO: check the case if we are playing the last song from the source
        # because then the queue must be last though saq is None
        # .. maybe not necessary, it seems there is allways a not None song
        # after the queue: if there is no song in the current source to play
        # after the queue is empty, the last song before the queue will be played
        # 0: check if current song is in source (when playing from source)
        if not pfq and pe:
            if not sqm or not self.source_contains_entry(sqm, pe):
                uri = self.db.entry_get(entry, rhythmdb.PROP_LOCATION)
                pl.append(uri + " s")
        # 1: if there is no song to play after the queue or if the song to play
        #    after the queue is no more in the current source (e.g. if someone
        #    altered the source while playing from the queue), the complete source
        #    comes after the queue, otherwise, everything before source_saq
        #    comes before the queue
        if sqm and self.source_saq and self.source_contains_entry(sqm, self.source_saq):
            for row in sqm:
                if row[0] == self.source_saq : break
                uri = self.db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                pl.append(uri + " s")
                source_continue_pos += 1
        # 2: print queue
        for row in self.qqm:
            uri = self.db.entry_get(row[0], rhythmdb.PROP_LOCATION)
            pl.append(uri + " q")
        # 3: print rest of source
        if sqm:
            i = 0
            for row in sqm:
                if i < source_continue_pos:
                    i += 1
                    continue
                uri = self.db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                pl.append(uri + " s")
                
        f = open(REM_PL_FILE, "w")
        for uri in pl:
            f.write(uri + "\n")
        f.close()
        
        return pl
    
    def source_contains_entry(self, sqm, entry):
        found = False
        for row in sqm:
            if entry == row[0]:
                print "--- found ---"
                found = True
                break
        return found
        
    ###########################################################################
    #
    # signal callbacks
    #
    ###########################################################################
    
    def cb_fn_source_changed(self, sp, source):
        
        # if source has been changed while playing from queue, the next song
        # after the queue is allways the first song of the new source
        # 
        # rhythmbox allways starts with first song in source if source has been
        # changed and queue is empty, so forget the song after queue
        #if self.qqm.q

        # if not pfq:
        #     if queue is empty:
        #         if source != self.connected_source
        #             source_saq = None
        #         else:
        #             source_saq bleibt wie es ist
        #     else:
        #         if source != self.connected_source
        #             source_saq = None
        #         else:
        #             if queue-song is in source:
        #                 source_saq = None
        #                 
        #             else:
        #                 source_saq = None
            
#        if not (self.sp.get_property("playing-from-queue") \
#                and self.qqm.__len__() == 0 \
#                and source == self.connected_source):
#            self.source_saq = None

        if source == self.connected_source:
            return

        print "source changed"

        self.source_saq = None

        # register signal callbacks to new source
        if not source:
            source = self.shell.get_property("selected-source")

        if self.connected_source:
            self.connected_source.disconnect(self.cb_id_source_filter_changed)
            self.connected_source = None
            self.cb_id_source_filter_changed = None
            
        if source:
            self.connected_source = source
            self.cb_id_source_filter_changed = source.connect( \
                    "filter-changed", self.cb_fn_source_filter_changed)
        
        self.source_update(source)
        
        # I guess source changed allways includes that the playing uri changed
        # and so we do not need to output the new source here, this is done in
        # cb_fn_playing_uri_changed()
        
    def cb_fn_source_row_inserted(self, qm, gtp, gti):
        #if not self.connected_sqm.has_pending_changes():
        print "source row inserted"
        self.source_change = True
    
    def cb_fn_source_row_deleted(self, qm, gtp):
        print "source row deleted"
        self.source_change = True
    
    def cb_fn_source_rows_reordered(self, qm, gtp, gti, gp):
        print "source rows reordered"
        self.source_change = True
 
    def cb_fn_queue_row_inserted(self, qm, gtp, gti):
        print "queue row inserted"
        self.queue_change = True
    
    def cb_fn_queue_row_deleted(self, qm, gtp):
        print "queue row deleted"
        self.queue_change = True
    
    def cb_fn_queue_rows_reordered(self, qm, gtp, gti, gp):
        print "queue rows reordered"
        self.queue_change = True
 
    # Note: does not get called if switched from play to stop
    def cb_fn_playing_uri_changed(self, sp, uri):
        print "playing uri changed (" + uri + ")"
        # if we are playing not from queue, lets now save, which song is
        # the first song after the queue (in case we will play from queue on
        # next song we need to know where to continue in source after the
        # queue is empty)
        if not self.sp.get_property("playing-from-queue"):
            pe = self.sp.get_playing_entry()
            self.source_saq = None
            sqm = self.connected_sqm
            if pe and sqm:
                stop_loop = False
                for row in sqm:
                    self.source_saq = row[0] # TODO check this
                    if stop_loop:
                        break
                    elif pe == row[0]:
                        stop_loop = True
            
    def cb_fn_playing_changed(self, sp, b):
        print "playing changed: " + str(b)
        if not b and not self.sp.get_playing_entry(): # means stop
            print "stopped"
            self.source_saq = None
            self.out_all()
        
    def cb_fn_source_filter_changed(self, x):
        """
        This callback functions tells us that there is a new source query model
        active.
        """
        print "source filter changed"
        self.source_change = True
        source = self.get_source()
        self.source_update(source)

    def cb_fn_elapsed_changed(self, sp, u):
        if u % 2 and (self.source_change or self.queue_change):
            self.out_all()
