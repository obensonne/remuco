###############################################################################
#
# imports
#
###############################################################################

from dbus.exceptions import DBusException
import dbus
import dbus.mainloop.glib
import dbus.service
import gobject
import os
import os.path
import rb, rhythmdb
import sys
import traceback
import urllib
import urlparse

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
# constants defined by Remuco Server-PP protocol
#
###############################################################################

SERVER_PP_PROTO_VERSION = 2

# dbus constants

DBUS_SHELL_SERVICE = "net.sf.remuco.Shell"
DBUS_SHELL_PATH = "/net/sf/remuco/Shell"
DBUS_SHELL_IFACE = "net.sf.remuco.Shell"

DBUS_SERVER_SERVICE = "net.sf.remuco.Server"
DBUS_SERVER_PATH = "/net/sf/remuco/Server"
DBUS_SERVER_IFACE = "net.sf.remuco.Server"

DBUS_PP_SERVICE = "net.sf.remuco.%s" % PLAYER
DBUS_PP_PATH = "/net/sf/remuco/%s" % PLAYER
DBUS_PP_IFACE = "net.sf.remuco.%s" % PLAYER

# dbus errors

DBUS_ERR_NO_SERVICE = "org.freedesktop.DBus.Error.ServiceUnknown"
DBUS_ERR_NO_REPLY = "org.freedesktop.DBus.Error.NoReply"

# well known ploblist ids

PLAYLIST_ID = "__PLAYLIST__"
QUEUE_ID = "__QUEUE__"

# playback state codes

PLAYBACK_STOP = 0
PLAYBACK_PAUSE = 1
PLAYBACK_PLAY = 2

# keys for plob meta information

PLOB_META_ALBUM = "album"
PLOB_META_ARTIST = "artist"
PLOB_META_BITRATE = "bitrate"
PLOB_META_COMMENT = "comment"
PLOB_META_GENRE = "genre"
PLOB_META_LENGTH = "length"
PLOB_META_TITLE = "title"
PLOB_META_TRACK = "track"
PLOB_META_YEAR = "year"
PLOB_META_RATING = "rating"
PLOB_META_TAGS = "tags"
PLOB_META_TYPE = "type"
PLOB_META_TYPE_AUDIO = "audio"
PLOB_META_TYPE_VIDEO = "video"
PLOB_META_TYPE_OTHER = "other"

# control command codes

CTL_IGNORE = 0
CTL_PLAYPAUSE = 1
CTL_STOP = 2
CTL_NEXT = 3
CTL_PREV = 4
CTL_JUMP = 5
CTL_SEEK_FWD = 6
CTL_SEEK_BWD = 7
CTL_VOLUME = 8
CTL_RATE = 9
CTL_PLAYNEXT = 10
CTL_SETTAGS = 12
CTL_REPEAT = 13
CTL_SHUFFLE = 14

###############################################################################
#
# Misc initializations
#
###############################################################################

_home_dir = os.getenv("HOME", "/var/tmp")
_cache_dir = os.getenv("XDG_CACHE_HOME", "%s/.cache/remuco" % _home_dir)
_config_dir = os.getenv("XDG_CONFIG_HOME", "%s/.config/remuco" % _home_dir)

COVER_CACHE_FILE = "%s/rhythmbox-cover.png" % _cache_dir

_log_debug_file = "%s/debug" % _config_dir # indicates log level

if os.path.isfile(_log_debug_file):
    print("debug log is enabled")
    LOG_DEBUG = True
else:
    print("debug log is disabled")
    LOG_DEBUG = False

###############################################################################
#
# Logging
#
###############################################################################

def log_debug(msg):
    if LOG_DEBUG:    
        print(msg)

def log_msg(msg):
    print(msg)
    
def log_exc(msg):
    print(msg)
    print("------------------ EXC ------------------")
    traceback.print_exc()
    print("-----------------------------------------")
    
###############################################################################
#
# Remuco Plugin
#
###############################################################################

class RemucoPlugin(rb.Plugin):
    
    def __init__(self):
        
        log_debug("initialize Remuco plugin ..")

        rb.Plugin.__init__(self)
        
        self.__pp = PP()
        
    def activate(self, shell):
        
        log_msg("activate Remuco plugin ..")

        return self.__pp.activate(shell)
        
    def deactivate(self, shell):
    
        log_msg("deactivate Remuco plugin ..")

        return self.__pp.deactivate(shell)

###############################################################################
#
# Player Proxy
#
###############################################################################

class PP(dbus.service.Object):

    ###########################################################################
    # Delegated plugin interface
    ###########################################################################
    
    def __init__(self):
        
        ###### init dbus ######

        # FIXME: assuming this is necessary in a RB plugin
        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)

        dbus.service.Object.__init__(self, None, None)

    def activate(self, shell):
        
        ###### init object vars ######

        self.__server = None
        
        self.__state_playback = PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_repeat = False
        self.__state_shuffle = False
        self.__state_position = 0
        self.__state_queue = False
         
        self.__plob_id = None
        self.__plob_entry = None
        self.__playlist_sc = None
        self.__playlist_qm = None
        self.__queue_sc = None
        self.__queue_qm = None
        
        self.__cb_id_ml_sync_state = 0 
        self.__cb_id_ml_sync_plob = 0 
        self.__cb_id_ml_sync_playlist = 0 
        self.__cb_id_ml_sync_queue = 0 
         
        self.__cb_ids_sp = ()
        self.__cb_ids_qm_queue = ()
        self.__cb_ids_sc_playlist = ()
        
        ###### set up dbus and connect to server ######
        
        bus = dbus.SessionBus()
        
        
        try:
            shell_proxy = bus.get_object(DBUS_SHELL_SERVICE, DBUS_SHELL_PATH)
            rem_shell = dbus.Interface(shell_proxy, DBUS_SHELL_IFACE)
            rem_shell.Start(SERVER_PP_PROTO_VERSION)
        except DBusException, e:
            if e.get_dbus_name() == DBUS_ERR_NO_SERVICE:
                rb.error_dialog(
                    title = "Remuco Error",
                    message = "It looks like the Remuco server is not installed."
                )
            else:
                rb.error_dialog(
                    title = "Remuco Error",
                    message = "Failed to connect to server (%s)." % e.message
                )
            return False
        
        
        try:
            server_proxy = bus.get_object(DBUS_SERVER_SERVICE, DBUS_SERVER_PATH)
            server = dbus.Interface(server_proxy, DBUS_SERVER_IFACE)
            server.Hello(PLAYER, 0, RATING_MAX)
        except DBusException, e:
            rb.error_dialog(
                title = "Remuco Error",
                message = "Failed to talk to server (%s)." % e.message
            )
            return False
        
        ###### critical work is done, assign object global vars ######

        dbus.service.Object.add_to_connection(self, bus, DBUS_PP_PATH)

        self.__dbus_service_name = dbus.service.BusName(DBUS_PP_SERVICE, bus)
        
        self.__server = server

        self.__shell = shell
    
        ###### connect to shell player signals ######

        sp = shell.props.shell_player
        
        self.__cb_ids_sp = (
            sp.connect("playing_changed", self.sp_playing_changed),
            sp.connect("playing_uri_changed", self.sp_playing_uri_changed),
            sp.connect("playing-source-changed", self.sp_playlist_changed)
        )

        ###### connect to playlist signals ######

        self.sp_playlist_changed(sp, sp.props.source)

        ###### connect to queue signals ######

        # FIXME: assuming queue source and query model is never None

        sc_queue = shell.props.queue_source
        qm_queue = sc_queue.props.query_model
        
        self.__queue_sc = sc_queue
        self.__queue_qm = qm_queue

        self.__cb_ids_qm_queue = (
            qm_queue.connect("row-inserted", self.qm_queue_row_inserted),
            qm_queue.connect("row-deleted", self.qm_queue_row_deleted),
            qm_queue.connect("rows-reordered", self.qm_queue_rows_reordered)
        )

        ###### periodically check for changes which have no signals ######

        self.__cb_id_ml_poll_misc = gobject.timeout_add(3000, self.ml_poll_misc)

        ###### initially trigger server synchronization ######
        
        # state sync will happen by timeout
        # playlist sync already triggered above 
        self.sp_playing_uri_changed(sp, sp.get_playing_path()) # plob sync
        self.__trigger_sync_queue() # queue sync
        
    def deactivate(self, shell):

        server = self.__server

        if not server: return
        
        self.__server = None

        ###### disconnect from server and dbus ######

        try:
            server.Bye(PLAYER)
        except DBusException:
            pass

        try:
            dbus.service.Object.remove_from_connection(self, path=DBUS_PP_PATH)
        except LookupError:
            pass
        
        self.__dbus_service_name = None

        ###### disconnect from shell player signals ######

        # FIXME: assuming shell player is never None
        
        sp = shell.props.shell_player

        for cb_id in self.__cb_ids_sp:
            
            sp.disconnect(cb_id)
            
        self.__cb_ids_sp = ()

        ###### disconnect from playlist signals ######

        self.sp_playlist_changed(sp, None)
        
        ###### disconnect from queue signals ######
        
        # FIXME: assuming queue query model is never None
        
        qmodel = self.__queue_qm
        
        for cb_id in self.__cb_ids_qm_queue:
            
            qmodel.disconnect(cb_id)
            
        self.__cb_ids_qm_queue = ()
        
        ###### remove gobject sources ######
        
        if self.__cb_id_ml_poll_misc > 0:
            gobject.source_remove(self.__cb_id_ml_poll_misc)
        
        if self.__cb_id_ml_sync_state > 0:
            gobject.source_remove(self.__cb_id_ml_sync_state)
            
        if self.__cb_id_ml_sync_plob > 0:
            gobject.source_remove(self.__cb_id_ml_sync_plob)
            
        if self.__cb_id_ml_sync_playlist > 0:
            gobject.source_remove(self.__cb_id_ml_sync_playlist)
            
        if self.__cb_id_ml_sync_queue > 0:
            gobject.source_remove(self.__cb_id_ml_sync_queue)
                
        # release shell
        self.__shell = None
        
    ###########################################################################
    # Remuco PP interface
    ###########################################################################
    
    # These methods should always return valid data to the server.
    
    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='uis', out_signature='')
    def Control(self, command, paramI, paramS):
        """Remuco PP DBus interface method.
        
        Called by the Remuco server to control RB.
        """
        
        server = self.__server
        
        if not server:
            return None
        try:
            
            log_debug("called Control(%u, %i, %s)" % (command, paramI, paramS))
            
            sp = self.__shell.props.shell_player
            
            db = self.__shell.props.db
        
            if command == CTL_STOP:
                
                sp.stop()
                
            elif command == CTL_PLAYPAUSE:
                
                sp.playpause()
                
            elif command == CTL_NEXT:
                
                sp.do_next()
                
            elif command == CTL_PREV:
                
                sp.do_previous()
                
            elif command == CTL_SEEK_FWD:
                
                sp.seek(SEEK_STEP)
                
            elif command == CTL_SEEK_BWD:
                
                sp.seek(- SEEK_STEP)
                
            elif command == CTL_VOLUME:
                
                sp.set_volume(float(paramI) / 100)
                
            elif command == CTL_JUMP:
                
                self.__do_jump(paramS, paramI)
                        
            elif command == CTL_RATE:
                
                entry = self.__plob_entry
                if entry != None:
                    db.set(entry, rhythmdb.PROP_RATING, paramI)
                
            elif command == CTL_IGNORE:
                
                pass

            else:
                
                log_msg("command %d not supported", cmd)
                
        except:
            
            log_exc("error while controlling RB (but we are lenient):")
            return None

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='a{ss}')
    def RequestPlob(self, id):
        """Remuco PP DBus interface method.
        
        Called by the Remuco server to get a certain plob (song).
        """
        
        server = self.__server
        
        if not server:
            return {}
        
        try:
            
            log_debug("called RequestPlob(%s)" % id)        
    
            db = self.__shell.props.db
    
            entry = db.entry_lookup_by_location(id)
            
            return self.__get_plob_meta(entry)
        
        except:
            
            log_exc("error while requesting a plob (but we are lenient):")
            return {}

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='asasasas')
    def RequestPloblist(self, id):
        """Remuco PP DBus interface method.
        
        Called by the Remuco server to get a certain ploblist (source).
        """
        
        server = self.__server
        
        if not server:
            return ([], [], [], [])
        
        try:
                
            log_debug("called RequestPloblist(%s)" % id)
    
            nested_ids = []
            nested_names = []
            ids = []
            names = []
            
            slm = self.__shell.props.sourcelist_model
            
            if not slm:
                return (nested_ids, nested_names, ids, names)
    
            ### root ? ###
            
            if not id or id == "":
                for group in slm:
                    group_name = group[2]
                    nested_ids.append(group_name)
                    nested_names.append(group_name)
                return (nested_ids, nested_names, ids, names)
            
            ### group ? ###
            
            if id.find(DELIM) == -1:
                for group in slm:
                    group_name = group[2]
                    if id == group_name:
                        for source in group.iterchildren():
                            source_name = source[2]
                            log_debug("append %s%s%s" %
                                      (group_name, DELIM, source_name))
                            nested_ids.append("%s%s%s" %
                                              (group_name, DELIM, source_name))
                            nested_names.append(source_name)
                        break
                return (nested_ids, nested_names, ids, names)
                
            ### regular playlist (source) ! ###
            
            source = self.__get_source_from_ploblist_id(id)
    
            if not source:
                return (nested_ids, nested_names, ids, names)
    
            model = source.get_entry_view().props.model
            
            ids, names = self.__get_ploblist_from_qmodel(model)
            
            return (nested_ids, nested_names, ids, names)

        except:
            
            log_exc("error while requesting a ploblist (but we are lenient):")
            return ([], [], [], [])

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='', out_signature='')
    def Bye(self):
        """Remuco PP DBus interface method.
        
        Called by the Remuco server to tell us we should shut down.
        """
        
        log_msg("server said bye")
        self.__suicide(None)
        
    ###########################################################################
    # Rhythmbox signal callbacks
    ###########################################################################
    
    def sp_playing_uri_changed(self, sp, uri):
        """Shell player signal callback to handle a plob change."""
        
        log_debug("rb_playing_uri_changed: %s" % uri)
        
        db = self.__shell.props.db

        entry = sp.get_playing_entry()
        if not entry:
            self.__plob_id = None
            self.__plob_entry = None
        else:
            self.__plob_id = db.entry_get(entry, rhythmdb.PROP_LOCATION)
            self.__plob_entry = entry
            
        self.__trigger_sync_plob()
            
    def sp_playing_changed(self, sp, b):
        """Shell player signal callback to handle a change in playback."""
        
        log_debug("sp_playing_changed: %s" % str(b))
        
        if b:
            self.__state_playback = PLAYBACK_PLAY
        elif not self.__plob_id:
            self.__state_playback = PLAYBACK_STOP
        else:
            self.__state_playback = PLAYBACK_PAUSE
            
        self.__trigger_sync_state()

    def sp_playlist_changed(self, sp, source_new):
        """Shell player signal callback to handle a playlist switch."""

        source_old = self.__playlist_sc
        
        if source_old == source_new:
            return
        
        log_debug("sp_playlist_changed: %s" % str(source_new))
        
        ###### disconnect signals of old source ######
        
        if source_old != None:
            for cb_id in self.__cb_ids_sc_playlist:
                source_old.disconnect(cb_id)
            self.__cb_ids_sc_playlist = ()
        
        self.__playlist_sc = source_new
        
        ###### connect to signals of new source and its query model ######
        
        if source_new != None:

            self.__cb_ids_sc_playlist = (
                source_new.connect("filter-changed", self.sc_playlist_filter_changed),
            )
            
            qmodel = source_new.get_entry_view().props.model
             
        else:
            
            qmodel = None

        self.__handle_playlist_qmodel_changed(qmodel)

    def sc_playlist_filter_changed(self, source):
        """Source signal callback to handle a playlist filter change."""
        
        log_debug("sc_playlist_filter_changed: %s" % str(source))
        
        ###### handle the new query model (if there is one) ######

        try:
            qmodel = self.__playlist_sc.get_entry_view().props.model 
        except:
            qmodel = None

        self.__handle_playlist_qmodel_changed(qmodel)

    def qm_playlist_row_inserted(self, qm, gtp, gti):
        #log_debug("playlist row inserted (last)")
        self.__trigger_sync_playlist()
    
    def qm_playlist_row_deleted(self, qm, gtp):
        #log_debug("playlist row deleted")
        self.__trigger_sync_playlist()
    
    def qm_playlist_rows_reordered(self, qm, gtp, gti, gp):
        #log_debug("playlist rows reordered")
        self.__trigger_sync_playlist()
 
    def qm_queue_row_inserted(self, qm, gtp, gti):
        log_debug("queue row inserted")
        self.__trigger_sync_queue()
    
    def qm_queue_row_deleted(self, qm, gtp):
        log_debug("queue row deleted")
        self.__trigger_sync_queue()
    
    def qm_queue_rows_reordered(self, qm, gtp, gti, gp):
        log_debug("queue rows reordered")
        self.__trigger_sync_queue()

    ###########################################################################
    # Regular mainloop callbacks
    ###########################################################################

    def ml_poll_misc(self):
        """Timeout callback to periodically poll RB for state information
        without change signals.
        """ 
        
        sp = self.__shell.props.shell_player
        
        change = False
        
        ###### check repeat and shuffle ######
        
        order = sp.props.play_order
        
        repeat = order == PLAYORDER_REPEAT or order.startswith(PLAYORDER_SHUFFLE_ALT)
        if repeat != self.__state_repeat:
            self.__state_repeat = repeat
            change = True
            
        shuffle = order == PLAYORDER_SHUFFLE or order.startswith(PLAYORDER_SHUFFLE_ALT)
        if shuffle != self.__state_shuffle:
            self.__state_shuffle = shuffle
            change = True

        ###### check volume ######

        volume = int(sp.get_volume() * 100)

        if volume != self.__state_volume:
            self.__state_volume = volume
            change = True
        
        ###### trigger sync if needed ######
        
        if change:
            self.__trigger_sync_state()
        
        return True

    def ml_sync_state(self):
        """Idle callback to sync the state with the server."""
        
        self.__cb_id_ml_sync_state = 0
        
        server = self.__server
        
        if not server:
            return False
        
        server.UpdateState(PLAYER,
            self.__state_playback, self.__state_volume,
            self.__state_repeat, self.__state_shuffle,
            self.__state_position, self.__state_queue,
            reply_handler = self.__server_reply_normal,
            error_handler = self.__server_reply_error)
            
        return False

    def ml_sync_plob(self):
        """Idle callback to sync the plob with the server."""

        self.__cb_id_ml_sync_plob = 0
        
        server = self.__server

        if not server:
            return False

        # a new plob may result in a new position:
        self.__check_for_position_change()

        db = self.__shell.props.db

        id = self.__plob_id
        entry = self.__plob_entry
        
        if id != None and entry != None:

            meta = self.__get_plob_meta(entry)
    
            img = db.entry_request_extra_metadata(entry, "rb:coverArt")
            if not img:
                img_file = self.__get_cover_from_plob_id(id)
            else:
                try:
                    img.save(COVER_CACHE_FILE, "png")
                    img_file = COVER_CACHE_FILE
                except:
                    img_file = ""
    
            log_debug("image: %s" % str(img_file))
    
            if not img_file:
                img_file = ""
        
        else:
            id = ""
            img_file = ""
            meta = {}

        server.UpdatePlob(PLAYER, id, img_file, meta,
            reply_handler = self.__server_reply_normal,
            error_handler = self.__server_reply_error)
        
        return False

    def ml_sync_playlist(self):
        """Idle callback to sync the playlist with the server."""

        self.__cb_id_ml_sync_playlist = 0
        
        server = self.__server

        if not server:
            return False

        # a new playlist may result in a new position:
        self.__check_for_position_change()

        log_debug("playlist query model: %s" % str(self.__playlist_qm))

        ids, names = self.__get_ploblist_from_qmodel(self.__playlist_qm)
        
        server.UpdatePlaylist(PLAYER, ids, names,
            reply_handler = self.__server_reply_normal,
            error_handler = self.__server_reply_error)

        return False

    def ml_sync_queue(self):
        """Idle callback to sync the queue with the server."""

        self.__cb_id_ml_sync_queue = 0

        server = self.__server

        if not server:
            return False

        # a new queue may result in a new position:
        self.__check_for_position_change()

        log_debug("queue query model: %s" % str(self.__queue_qm))

        ids, names = self.__get_ploblist_from_qmodel(self.__queue_qm)
        
        server.UpdateQueue(PLAYER, ids, names,
            reply_handler = self.__server_reply_normal,
            error_handler = self.__server_reply_error)

        return False

    ###########################################################################
    # Misc
    ###########################################################################

    def __suicide(self, msg):
        """Shut down the Remuco plugin.
        
        To be called when a serious error occurred. 'msg' shall describe the
        error - it will be displayed to the user. 
        """

        log_debug("suicide reason: %s" % msg)
        
        shell = self.__shell
        
        if not shell: return
        
        self.deactivate(shell)

        if msg != None:
            rb.error_dialog(title = "Remuco Error", message = msg)
        
    def __do_jump(self, id, index):
        """Jump to a specific song (index) in a specific source (id)."""
        
        source = self.__get_source_from_ploblist_id(id)
        
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
            log_debug("remove %s from queue" % id_to_remove_from_queue)
            self.__shell.remove_from_queue(id_to_remove_from_queue)

        # this works almost perfect now .. the only difference to directly
        # doing jumps in the RB UI is that when jumping to a song in the queue
        # which is not the first in the queue, the song does not get moved
        # to the top of the queue .. that's ok

    def __check_for_position_change(self):
        """Determine the current position and trigger a state sync if position
        has changed.
        """

        sp = self.__shell.props.shell_player

        db = self.__shell.props.db

        position = 0
        queue = False
        
        id_now = self.__plob_id
        
        if id_now != None:
            
            if sp.props.playing_from_queue:
                queue = True
                qmodel = self.__queue_qm
            else:
                qmodel = self.__playlist_qm
                
            if qmodel != None:
                for row in qmodel:
                    position += 1
                    id = db.entry_get(row[0], rhythmdb.PROP_LOCATION)
                    if id_now == id:
                        break
                position -= 1
                    
        log_debug ("position: %i" % position)

        if position != self.__state_position or queue != self.__state_queue:
            self.__state_position = position
            self.__state_queue = queue
            self.__trigger_sync_state()

    def __handle_playlist_qmodel_changed(self, qmodel_new):
        """Connect to signals from the new playlist query model.
        
        This includes disconnecting from signals of the old query model and.
        """
        
        qmodel_old = self.__playlist_qm
        
        if qmodel_old == qmodel_new:
            return

        log_debug("playlist query model changed: %s" % str(qmodel_new))

        ###### disconnect from signals of old query model ######

        if qmodel_old != None:
            for cb_id in self.__cb_ids_qm_playlist:
                qmodel_old.disconnect(cb_id)
        self.__cb_ids_qm_playlist = ()
            
        self.__playlist_qm = qmodel_new
        
        ###### connect to signals of new query model ######

        if qmodel_new != None:
            
            self.__cb_ids_qm_playlist = (
                qmodel_new.connect("row-inserted", self.qm_playlist_row_inserted),
                qmodel_new.connect("row-deleted", self.qm_playlist_row_deleted),
                qmodel_new.connect("rows-reordered", self.qm_playlist_rows_reordered)
            )
        
        ###### sync with the server (playlist change) ######

        self.__trigger_sync_playlist()
        
        return

    def __get_ploblist_from_qmodel(self, qmodel):
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
            title = db.entry_get(row[0], rhythmdb.PROP_TITLE)
            if title != None and title != "" and artist != None and artist != "":
                names.append("%s - %s" % (artist, title))
            elif title != None and title != "":
                names.append(title)
            elif artist != None and artist != "":
                names.append(artist)
            else:
                names.append("Unknown")

        return (ids, names)

    def __get_plob_meta(self, entry):
        """Get meta information for a plob.
        """
        
        if not entry:
            return { PLOB_META_TITLE : "No information" }
        
        db = self.__shell.props.db
        
        meta = {
            PLOB_META_TITLE : str(db.entry_get(entry, rhythmdb.PROP_TITLE)),
            PLOB_META_ARTIST: str(db.entry_get(entry, rhythmdb.PROP_ARTIST)),
            PLOB_META_ALBUM : str(db.entry_get(entry, rhythmdb.PROP_ALBUM)),
            PLOB_META_GENRE : str(db.entry_get(entry, rhythmdb.PROP_GENRE)),
            PLOB_META_BITRATE : str(db.entry_get(entry, rhythmdb.PROP_BITRATE)),
            PLOB_META_LENGTH : str(db.entry_get(entry, rhythmdb.PROP_DURATION)),
            PLOB_META_RATING : str(int(db.entry_get(entry, rhythmdb.PROP_RATING))),
            PLOB_META_TRACK : str(db.entry_get(entry, rhythmdb.PROP_TRACK_NUMBER)),
            PLOB_META_YEAR : str(db.entry_get(entry, rhythmdb.PROP_YEAR))
        }

        return meta 
    
    def __get_cover_from_plob_id(self, id):
        """Get the full path to a cover file related to 'id'.
        
        Returns 'None' if no cover has been found in the songs folder.
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

    def __get_source_from_ploblist_id(self, id):
        """Get the source object of source 'id'.
        
        'id' is either a combination of the source' group and name or one of
        the well known IDs 'PLAYLIST_ID' and 'QUEUE_ID'.
        """
        
        if id == PLAYLIST_ID:
            return self.__playlist_sc
        
        if id == QUEUE_ID:
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

    ###########################################################################
    # Remuco server sync methods
    ###########################################################################
    
    def __trigger_sync_state(self):
        """Trigger a state sync with the server, if not done already."""
        if self.__cb_id_ml_sync_state == 0:
            self.__cb_id_ml_sync_state = gobject.idle_add(self.ml_sync_state)
    
    def __trigger_sync_plob(self):
        """Trigger a plob sync with the server, if not done already."""
        if self.__cb_id_ml_sync_plob == 0: 
            self.__cb_id_ml_sync_plob = gobject.idle_add(self.ml_sync_plob)
        
    def __trigger_sync_playlist(self):
        """Trigger a playlist sync with the server, if not done already."""
        if self.__cb_id_ml_sync_playlist == 0:
            self.__cb_id_ml_sync_playlist = gobject.idle_add(
                        self.ml_sync_playlist, priority = gobject.PRIORITY_LOW)
        
    def __trigger_sync_queue(self):
        """Trigger a queue sync with the server, if not done already."""
        if self.__cb_id_ml_sync_queue == 0:
            self.__cb_id_ml_sync_queue = gobject.idle_add(
                        self.ml_sync_queue, priority = gobject.PRIORITY_LOW)
    

    ###########################################################################
    # Remuco server reply methods
    ###########################################################################

    def __server_reply_normal(self):
        # nothing to do - all called server methods return nothing
        pass
    
    def __server_reply_error(self, e):
        # 'e' is an instance of DBusException
        if e.get_dbus_name() == DBUS_ERR_NO_REPLY:
            log_msg("no reply from Remuco server (probably busy)")
            return True
        elif e.get_dbus_name() == DBUS_ERR_NO_SERVICE:
            log_msg("server is down")
            self.__suicide()
            return False
        else:
            log_exc("failed to talk to Remuco server")
            self.__suicide("Failed to talk to server (%s)" % str(e.message))
            return False

if __name__ == "__main__":
    
    print "This is a python module."
    
