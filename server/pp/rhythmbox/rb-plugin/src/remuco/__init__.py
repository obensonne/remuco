import gobject, gtk
import rb, rhythmdb
import os

REM_USER = os.environ['LOGNAME']
REM_TMP_FILE_BASE = "/var/tmp/remuco-rhythmbox." + REM_USER
REM_QUEUE_FILE = REM_TMP_FILE_BASE + ".queue"
REM_SOURCE_FILE = REM_TMP_FILE_BASE + ".plname"

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
        self.connected_sqm = None
        self.db = shell.get_property("db")
        self.source_change = False
        self.qqm = None
        self.queue_change = False
        self.shell = shell
        self.sp = shell.get_player()
        
        # reset the playlist file:
        f = open(REM_SOURCE_FILE, "w")
        f.close()
        f = open(REM_QUEUE_FILE, "w")
        f.close()

        source = self.get_source()
        self.cb_fn_source_changed(self.sp, source)
        
        source = self.shell.get_property("queue-source")
        if not source:
            self.qqm = None
        else:
            self.qqm = source.props.query_model
        
        ###### register signal callbacks ######
        
        # conncet to 'source changed' signal:
        self.cb_id_source_changed = self.sp.connect(\
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
        f = open(REM_SOURCE_FILE, "w")
        f.close()
        f = open(REM_QUEUE_FILE, "w")
        f.close()
        
        # Attritbue un-declaration:
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
    
    def out_queue(self):
        f = open(REM_QUEUE_FILE, "w")
        self.out_list(self.qqm, f)
        f.close()
        
    def out_source(self):
        f = open(REM_SOURCE_FILE, "w")
        source = self.get_source()
        if source:
            self.out_list(source.props.query_model, f)
        f.close()

    def out_list(self, qm, f):
        if qm:
            for row in qm:
                uri = self.db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                f.write(uri + "\n")
                
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
        
    
    ###########################################################################
    #
    # signal callbacks
    #
    ###########################################################################
    
    def cb_fn_source_changed(self, sp, source):
        
        print "source changed"

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
        
    def cb_fn_source_row_inserted(self, gtp, gti, x):
        print "source row inserted"
        self.source_change = True
    
    def cb_fn_source_row_deleted(self, gtp, x):
        print "source row deleted"
        self.source_change = True
    
    def cb_fn_source_rows_reordered(self, gtp, gti, gp, x):
        print "source rows reordered"
        self.source_change = True
 
    def cb_fn_queue_row_inserted(self, gtp, gti, x):
        print "queue row inserted"
        self.queue_change = True
    
    def cb_fn_queue_row_deleted(self, gtp, x):
        print "queue row deleted"
        self.queue_change = True
    
    def cb_fn_queue_rows_reordered(self, gtp, gti, gp, x):
        print "queue rows reordered"
        self.queue_change = True
 
    def cb_fn_playing_uri_changed(self, gca, x):
        print "playing uri changed"
        if self.source_change:
            self.out_source()
        if self.queue_change:
            self.out_queue()
            
    def cb_fn_playing_changed(self, gb, x):
        print "playing uri changed"
        if self.source_change:
            self.out_source()
        if self.queue_change:
            self.out_queue()

    def cb_fn_source_filter_changed(self, x):
        print "source filter changed"
        source = self.get_source()
        self.source_update(source)
        self.out_source()
