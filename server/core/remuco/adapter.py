import inspect
import subprocess
import urllib
import urlparse

import gobject

from remuco import art
from remuco import command
from remuco import config
from remuco import files
from remuco import log
from remuco import message
from remuco import net
from remuco import serial

from remuco.defs import *
from remuco.features import *

from remuco.data import PlayerInfo, PlayerState, Progress, ItemList, Item
from remuco.data import Control, Action, Tagging, Request

# =============================================================================
# browser action
# =============================================================================

class ListAction(object):
    """List related action within a client's media browser."""
    
    __id_counter = 1
    
    def __init__(self, label, help):
        """Create a new media browser action.
        
        @param label:
            label of the action (keep short, ideally this is just a single word
            like 'Load', ..)
        @param help:
            description of the action
        
        """
        
        self.__id = ItemAction.__id_counter
        ItemAction.__id_counter += 1
        
        self.__label = label
        self.__help = help
        
    def __str__(self):
        
        return "(%d, %s)" % (self.__id, self.__label)
        
    # === property: id ===
    
    def __pget_id(self):
        """ID of the action (auto-generated, read only)"""
        return self.__id
    
    id = property(__pget_id, None, None, __pget_id.__doc__)

    # === property: label ===
    
    def __pget_label(self):
        """label of the action (read-only)"""
        return self.__label
    
    label = property(__pget_label, None, None, __pget_label.__doc__)

    # === property: help ===
    
    def __pget_help(self):
        """description of the action (read-only)"""
        return self.__help
    
    help = property(__pget_help, None, None, __pget_help.__doc__)

class ItemAction(ListAction):
    """Item related action within a client's media browser."""
    
    def __init__(self, label, help, multiple=False):
        """Create a new media browser action.
        
        @param label:
            label of the action (keep short, ideally this is just a single word
            like 'Enqueue', 'Play', ..)
        @param help:
            description of the action
        @keyword multiple:
            if the action may be applied to multiple items (True) or only to a
            single item (False)
        
        """
        
        super(ItemAction, self).__init__(label, help)

        self.__multiple = multiple
        
    def __pget_multiple(self):
        """scope of the action - multiple or single (read-only)"""
        return self.__multiple
    
    multiple = property(__pget_multiple, None, None,
                        __pget_multiple.__doc__)

ITEM_ACTION_ENQUEUE = ItemAction("Enqueue", "Append items to the play queue.",
                                 multiple=True)

ITEM_ACTION_APPEND = ItemAction("Append", "Append items to the playlist.",
                                multiple=True)

ITEM_ACTION_NEXT = ItemAction("Play next", "Play items after the current one.",
                              multiple=True)

# =============================================================================
# player adapter
# =============================================================================

class PlayerAdapter(object):
    '''Base class for Remuco player adapters.
    
    Remuco player adapters must subclass this class and override certain
    methods to implement player specific behavior. Additionally PlayerAdapter
    provides methods to interact with Remuco clients. Following is a summary
    of all relevant methods, grouped by functionality. 
    
    ===========================================================================
    Methods to extend to manage life cycle
    ===========================================================================
    
        * start()
        * stop()
    
        A PlayerAdapter can be started and stopped with start() and stop().
        The same instance of a PlayerAdapter should be startable and stoppable
        multiple times.
        
        Subclasses of PlayerAdapter may override these methods as needed but
        must always call the super class implementations too!
    
    ===========================================================================
    Methods to override to control the media player:
    ===========================================================================
    
        Basic playback control:

        * ctrl_toggle_playing()
        * ctrl_next()
        * ctrl_previous()
        * ctrl_seek_forward()
        * ctrl_seek_backward()
        * ctrl_jump_in_playlist()
        * ctrl_jump_in_queue()
        * ctrl_toggle_repeat()
        * ctrl_toggle_shuffle()
        
        Miscellaneous:

        * ctrl_volume()
        * ctrl_volume_up()
        * ctrl_volume_down()
        * ctrl_toggle_fullscreen()
        * ctrl_play_file()
        * ctrl_play_id()
        
        Edit an item's meta data:
        
        * ctrl_rate()
        * ctrl_tag()
        
        The following methods may be implemented to adjust a player's
        current playlist:

        * ctrl_playlist_remove()
        * ctrl_playlist_next_ids()
        * ctrl_playlist_next_files()
        * ctrl_playlist_append_ids()
        * ctrl_playlist_append_files()
        * ctrl_playlist_set_ids()
        * ctrl_playlist_set_files()
        * ctrl_playlist_load()

        If a player has a global play queue, the following methods may be
        implemented to adjust the play queue:

        * ctrl_queue_remove()
        * ctrl_queue_prepend_ids()
        * ctrl_queue_prepend_files()
        * ctrl_queue_append_ids()
        * ctrl_queue_append_files()
        * ctrl_queue_clear()

        -----------------------------------------------------------------------

        Don't feel daunted by the vast number of methods :), a player adapter
        should implement only a *subset* of these methods - depending on what
        is possible and what makes sense.
        
        Remuco checks which methods have been overridden and uses this
        information to notify Remuco clients about capabilities of player
        adapters. 

    ===========================================================================
    Methods to override to provide information from the media player:
    ===========================================================================
    
        * request_playlist()
        * request_queue()
        * request_medialib()
    
        As above, only override the methods which make sense for the
        corresponding media player.
    
    ===========================================================================
    Methods to call to respond to the requests above:
    ===========================================================================

        * reply_playlist_request()
        * reply_queue_request()
        * reply_medialib_request()
        
    ===========================================================================
    Methods to call to synchronize media player state information with clients:
    ===========================================================================
    
        * update_playback()
        * update_repeat()
        * update_shuffle()
        * update_item()
        * update_position()
        * update_progress()
        * update_playlist_mutability()
        
        These methods should be called whenever the corresponding information
        has changed in the media player (it is safe to call these methods also
        if there actually is no change, internally a change check is done
        before sending any data to clients).
        
        Subclasses of PlayerAdapter may override the method poll() to
        periodically check a player's state.
        
    ===========================================================================
    Finally some utility methods:
    ===========================================================================
    
        * find_image()
        
    '''

    # =========================================================================
    # constructor 
    # =========================================================================
    
    def __init__(self, name, playback_known=False, volume_known=False,
                 repeat_known=False, shuffle_known=False, progress_known=False,
                 max_rating=0, poll=2.5, file_actions=None, mime_types=None):
        """Create a new player adapter.
        
        Just does some early initializations. Real job starts with start().
        
        @param name:
            name of the media player
        @keyword playback_known:
            indicates if the player's playback state can be provided (see
            update_playback())
        @keyword progress_known:
            indicates if the player's playback progress can be provided (see
            update_progress())
        @keyword max_rating:
            maximum possible rating value for items
        @keyword mime_types:
            list of mime types which can be handled by the player (for instance
            ['audio', 'video/quicktime', ...] or None for all mime types) - is
            only relevant if one of the control methods ending with '_file' or
            '_files' are overridden, e.g. ctrl_play_file(), in that case this
            list filters the files which may be passed to these methods
        @keyword poll:
            interval in seconds to call poll()
        
        @attention: When overriding, call super class implementation first!
        
        """
        
        self.__name = name
        
        # init config (config inits logging)
        
        self.__config = config.Config(self.__name)

        # init misc fields
        
        serial.Bin.HOST_ENCODING = self.__config.encoding
        
        self.__clients = []
        
        self.__state = PlayerState()
        self.__progress = Progress()
        self.__item = Item()
        
        flags = self.__util_calc_flags(playback_known, volume_known,
            repeat_known, shuffle_known, progress_known)
        
        self.__info = PlayerInfo(name, flags, max_rating, file_actions)
        
        self.__sync_triggers = {}
        
        self.__poll_ival = max(500, int(poll * 1000))
        self.__poll_sid = 0
        
        self.__stopped = True
        
        self.__server_bluetooth = None
        self.__server_wifi = None
        
        if self.__config.fb:
            self.__filelib = files.FileSystemLibrary(
                self.__config.fb_root_dirs, mime_types,
                use_user_dirs=self.__config.fb_xdg_user_dirs, 
                show_extensions=self.__config.fb_extensions)
            
        log.debug("init done")
    
    def start(self):
        """Start the player adapter.
        
        @attention: When overriding, call super class implementation first!
        
        """
        
        if not self.__stopped:
            log.debug("ignore start, already running")
            return
        
        self.__stopped = False
        
        # set up server
        
        if self.__config.bluetooth:
            self.__server_bluetooth = net.BluetoothServer(self.__clients,
                    self.__info, self.__handle_message, self.__config.ping)
        else:
            self.__server_bluetooth = None

        if self.__config.wifi:
            self.__server_wifi = net.WifiServer(self.__clients,
                    self.__info, self.__handle_message, self.__config.ping)        
        else:
            self.__server_wifi = None
            
        # set up polling
        
        if self.__poll_ival > 0:
            log.debug("poll every %d milli seconds" % self.__poll_ival)
            self.__poll_sid = gobject.timeout_add(self.__poll_ival, self.__poll)
            
        
        log.debug("start done")
    
    def stop(self):
        """Shutdown the player adapter.
        
        Disconnects all clients and shuts down the Bluetooth and WiFi server.
        Also ignores any subsequent calls to an update or reply method (e.g.
        update_volume(), ..., reply_playlist_request(), ...). 
        
        @note: The same player adapter instance can be started again with
            start().

        @attention: When overriding, call super class implementation first!
        
        """

        if self.__stopped: return
        
        self.__stopped = True
        
        for c in self.__clients:
            c.disconnect(remove_from_list=False, send_bye_msg=True)
            
        self.__clients = []
        
        if self.__server_bluetooth is not None:
            self.__server_bluetooth.down()
            self.__server_bluetooth = None
        if self.__server_wifi is not None:
            self.__server_wifi.down()
            self.__server_wifi = None
            
        for sid in self.__sync_triggers.values():
            if sid is not None:
                gobject.source_remove(sid)
                
        self.__sync_triggers = {}

        if self.__poll_sid > 0:
            gobject.source_remove(self.__poll_sid)
            
        log.debug("stop done")
    
    def poll(self):
        """Does nothing by default.
        
        If player adapters override this method, it gets called periodically
        in the interval specified by the keyword 'poll' in __init__().
        
        A typical use case of this method is to detect the playback progress of
        the current item and then call update_progress().
        
        """
        raise NotImplementedError
    
    def __poll(self):
        
        try:
            self.poll()
        except NotImplementedError:
            return False
        
        return True
    
    # =========================================================================
    # utility methods which may be useful for player adapters
    # =========================================================================
    
    def find_image(self, resource, prefer_thumbnail=False):
        """Find a local art image file related to a resource.
        
        This method first looks in the resource' folder for typical art image
        files (e.g. 'cover.png', 'front,jpg', ...). If there is no such file it
        then looks into the user's thumbnail directory (~/.thumbnails).
        
        @param resource:
            resource to find an art image for (may be a file name or URI)
        @keyword prefer_thumbnail:
            True means first search in thumbnails, False means first search in
            the resource' folder
                                   
        @return: an image file name (which can be used for update_item()) or
            None if no image file has been found or if 'resource' is not local
        
        """
        
        file = art.get_art(resource, prefer_thumbnail=prefer_thumbnail)
        log.debug("image for '%s': %s" % (resource, file))
        return file
    
    # =========================================================================
    # control interface 
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        """Toggle play and pause. 
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_toggle_repeat(self):
        """Toggle repeat mode. 
        
        @note: Override if it is possible and makes sense.
        
        @see: update_repeat()
               
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_toggle_shuffle(self):
        """Toggle shuffle mode. 
        
        @note: Override if it is possible and makes sense.
        
        @see: update_shuffle()
               
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_toggle_fullscreen(self):
        """Toggle full screen mode. 
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")

    def ctrl_next(self):
        """Play the next item. 
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_previous(self):
        """Play the previous item. 
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_seek(self, direction):
        """Seek forward or backward some seconds. 
        
        The number of seconds to seek should be reasonable for the current
        item's length (if known).
        
        If the progress of the current item is known, it should get
        synchronized immediately with clients by calling update_progress().
        
        @param direction:
            * > 0: seek forward
            * < 0: seek backward 
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_rate(self, rating):
        """Rate the currently played item. 
        
        @param rating:
            rating value (int)
        
        @note: Override if it is possible and makes sense.
        
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_tag(self, id, tags):
        """Attach some tags to an item.
        
        @param id:
            ID of the item to attach the tags to
        @param tags:
            a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
            idea of tags (e.g. like used at last.fm). 

        @note: Override if it is possible and makes sense.
               
        """
        log.error("** BUG ** in feature handling")
    
    def ctrl_volume(self, direction):
        """Adjust volume. 
        
        @param volume:
            * -1: decrease by some percent (5 is a good value)
            * +1: increase by some percent (5 is a good value)
            *  0: mute volume
        
        @note: Override if it is possible and makes sense.
               
        """
        log.error("** BUG ** in feature handling")
        
    def ctrl_clear_playlist(self):
        log.error("** BUG ** in feature handling")
        
    def ctrl_clear_queue(self):
        log.error("** BUG ** in feature handling")
    
    def __ctrl_shutdown_system(self):
        
        shutdown_cmd = config.get_system_shutdown_command()
        if shutdown_cmd:
            log.debug("run shutdown command")
            try:
                subprocess.Popen(shutdown_cmd, shell=True)
            except OSError, e:
                log.warning("failed to run shutdown command (%s)", e)
                return
            self.stop()

    # =========================================================================
    # actions interface
    # =========================================================================
    
    def action_playlist(self, action_id, positions, ids):
        """
        @param positions: list of positions to apply the action to
        """
        log.error("** BUG ** action_playlist() not implemented")
    
    def action_queue(self, action_id, positions, ids):
        """
        @param action_id:
            ID of the action to apply to 'positions' (specifies one of the
            actions set with keyword 'queue_actions' in __init__())
        @param positions:
            list of positions to apply the action to (if the action's scope
            is Action.SINGLE_ITEM then this is a one element list) 
        
        """
        log.error("** BUG ** action_queue() not implemented")
    
    def action_files(self, action_id, files, uris):
        """
        
        @param files:
            list of file names (these may be any files from the local
            file system that have one of the mime types specified by the
            keyword 'mime_types' in __init__())
        @param uris:
            file names in URI notation (preferred by some players)
        """
        log.error("** BUG ** action_files() not implemented")
    
    def action_medialib(self, action_id, path, positions, ids):
        """
        @param ids:
            list of item IDs (identifying items in the player's media library)
            
        if the action denotes to a ListAction, 'positions' and 'ids' are
        empty lists
        
        """
        log.error("** BUG ** action_medialib() not implemented")
    
    # =========================================================================
    # request interface 
    # =========================================================================
    
    def request_item(self, client, id):
        # maybe we enable this later
        log.error("** BUG ** in feature handling")
        
    def request_playlist(self, client):
        """Request the content of the currently active playlist.
        
        @param client:
            the requesting client (needed for reply)
        
        @note: Override if it is possible and makes sense.
               
        @see: reply_playlist_request() for sending back the result
        
        """
        log.error("** BUG ** in feature handling")

    def request_queue(self, client):
        """Request the content of the play queue. 
        
        @param client:
            the requesting client (needed for reply)
        
        @note: Override if it is possible and makes sense.
               
        @see: reply_queue_request() for sending back the result
        
        """
        log.error("** BUG ** in feature handling")

    def request_medialib(self, client, path):
        """Request contents of a specific level from the player's media library.
        
        @param client: the requesting client (needed for reply)
        @param path: path of the requested level (string list)
        
        @note: path is a list of strings which describes a specific level in
            the player's playlist tree. If path is an empty list, the root of
            the player's library, i.e. all top level playlists are requested.

            A player may have a media library structure like this:

               |- Radio
               |- Genres
                  |- Jazz
                  |- ...
               |- Dynamic
                  |- Never played
                  |- Played recently
                  |- ...
               |- Playlists
                  |- Party
                     |- Sue's b-day
                     |- ...
               |- ...

             Here possibles values for path are [ "Radio" ] or
             [ "Playlists", "Party", "Sue's b-day" ] or ...
               
        @note: Override if it is possible and makes sense.
               
        @see: reply_list_request() for sending back the result
        
        """
        log.error("** BUG ** in feature handling")
        
    # =========================================================================
    # player side synchronization
    # =========================================================================    

    def update_position(self, position, queue=False):
        """Set the current item's position in the playlist or queue. 
        
        @param position:
            position of the currently played item (starting at 0)
        @keyword queue:
            True if currently played item is from the queue, False if it is
            from the currently active playlist
                        
        @note: Call to synchronize player state with remote clients.
        
        """
        change = self.__state.queue != queue
        change |= self.__state.position != position
        
        if change:
            self.__state.queue = queue
            self.__state.position = position
            self.__sync_trigger(self.__sync_state)
        
    def update_playback(self, playback):
        """Set the current playback state.
        
        @param playback:
            playback mode
            
        @see: remuco.PLAYBACK_...
        
        @note: Call to synchronize player state with remote clients.
        
        """
        change = self.__state.playback != playback
        
        if change:
            self.__state.playback = playback
            self.__sync_trigger(self.__sync_state)
    
    def update_repeat(self, repeat):
        """Set the current repeat mode. 
        
        @param repeat: True means play indefinitely, False means stop after the
            last playlist item
        
        @note: Call to synchronize player state with remote clients.
        
        """
        change = self.__state.repeat != repeat
        
        if change:
            self.__state.repeat = repeat
            self.__sync_trigger(self.__sync_state)
    
    def update_shuffle(self, shuffle):
        """Set the current shuffle mode. 
        
        @param shuffle: True means play in non-linear order, False means play
            in linear order
        
        @note: Call to synchronize player state with remote clients.
        
        """
        change = self.__state.shuffle != shuffle
        
        if change:
            self.__state.shuffle = shuffle
            self.__sync_trigger(self.__sync_state)
    
    def update_volume(self, volume):
        """Set the current volume.
        
        @param volume: the volume in percent
        
        @note: Call to synchronize player state with remote clients.
        
        """
        volume = int(volume)
        change = self.__state.volume != volume
        
        if change:
            self.__state.volume = volume
            self.__sync_trigger(self.__sync_state)
    
    def update_progress(self, progress, length):
        """Set the current playback progress.
        
        @param progress:
            number of currently elapsed seconds
        @keyword length:
            item length in seconds (maximum possible progress value)
        
        @note: Call to synchronize player state with remote clients.
        
        """
        # sanitize progress (to a multiple of 5)
        length = max(0, int(length))
        progress = max(0, int(progress))
        off = progress % 5
        if off < 3:
            progress -= off
        else:
            progress += (5 - off)
        progress = min(length, progress)
        
        #diff = abs(self.__progress.progress - progress)
        
        change = self.__progress.length != length
        #change |= diff >= 5
        change |= self.__progress.progress != progress
        
        if change:
            self.__progress.progress = progress
            self.__progress.length = length
            self.__sync_trigger(self.__sync_progress)
    
    def update_item(self, id, info, img):
        """Set currently played item.
        
        @param id:
            item ID (str)
        @param info:
            meta information (dict)
        @param img:
            image / cover art (either a file name or an instance of
            Image.Image)
        
        @note: Call to synchronize player state with remote clients.

        @see: find_image() for finding image files for an item.
        
        @see: remuco.INFO_... for keys to use for 'info'
               
        """
        
        log.debug("new item: (%s, %s %s)" % (id, info, img))
        
        change = self.__item.id != id
        change |= self.__item.info != info
        change |= self.__item.img != img
        
        if change:
            self.__item.id = id
            self.__item.info = info
            self.__item.img = img
            self.__sync_trigger(self.__sync_plob)
            
    # =========================================================================
    # request replies
    # =========================================================================    

    def reply_item_request(self, client, id, info):
        
        if self.__stopped:
            return
        
        plob = Item()
        plob.id = id
        plob.info = info

        msg = net.build_message(message.REQ_ITEM, plob)
        
        gobject.idle_add(self.__reply, client, msg, "plob")
        
    def reply_playlist_request(self, client, ids, names, item_actions=None):
        """Send the reply to a playlist request back to the client.
        
        @param client:
            the client to reply to
        @param ids:
            IDs of the items contained in the playlist
        @param names:
            names of the items contained in the playlist
        
        @see: request_playlist()
        
        """ 
        if self.__stopped:
            return
        
        playlist = ItemList(None, None, ids, names, item_actions, None)
        
        msg = net.build_message(message.REQ_PLAYLIST, playlist)
        
        gobject.idle_add(self.__reply, client, msg, "playlist")
    
    def reply_queue_request(self, client, ids, names, item_actions=None):
        """Send the reply to a queue request back to the client.
        
        @param client:
            the client to reply to
        @param ids:
            IDs of the items contained in the queue
        @param names:
            names of the items contained in the queue
        
        @see: request_queue()
        
        """ 
        if self.__stopped:
            return
        
        queue = ItemList(None, None, ids, names, item_actions, None)
        
        msg = net.build_message(message.REQ_QUEUE, queue)
        
        gobject.idle_add(self.__reply, client, msg, "queue")
    
    def reply_medialib_request(self, client, path, nested, ids, names,
                               item_actions=None, list_actions=None):
        """Send the reply to a media library request back to the client.
        
        @param client:
            the client to reply to
        @param path:
            path of the requested library level
        @param nested:
            names of nested lists at the requested path
        @param ids:
            IDs of the items at the requested path
        @param names:
            names of the items at the requested path
        @keyword actions:
            IDs of the media library actions possible at the requested path
            (default is None, which means all actions specified by the keyword
            'medialib_actions' in __init__() are possible - use the empty list
            to disable any media library actions at the requested path)
        
        @see: request_medialib()
        
        """ 
        if self.__stopped:
            return
        
        lib = ItemList(path, nested, ids, names, item_actions, list_actions)
        
        msg = net.build_message(message.REQ_MLIB, lib)
        
        gobject.idle_add(self.__reply, client, msg, "medialib")
        
    def __reply_files_request(self, client, path, nested, ids, names):

        files = ItemList(path, nested, ids, names, None, None)
        
        msg = net.build_message(message.REQ_FILES, files)
        
        gobject.idle_add(self.__reply, client, msg, "files")
        
    def __reply(self, client, msg, name):

        log.debug("send %s reply to %s" % (name, client))
        
        client.send(msg)
    
    # =========================================================================
    # synchronization (outbound communication)
    # =========================================================================
    
    def __sync_trigger(self, sync_fn):
        
        if self.__stopped:
            return
        
        if sync_fn in self.__sync_triggers:
            log.debug("trigger for %s already active" % sync_fn.func_name)
            return
        
        self.__sync_triggers[sync_fn] = \
            gobject.idle_add(sync_fn, priority=gobject.PRIORITY_LOW)
        
    def __sync_state(self):
        
        msg = net.build_message(message.SYNC_STATE, self.__state)
        
        self.__sync(msg, self.__sync_state, "state", self.__state)
        
        return False
    
    def __sync_progress(self):
        
        msg = net.build_message(message.SYNC_PROGRESS, self.__progress)
        
        self.__sync(msg, self.__sync_progress, "progress", self.__progress)
        
        return False
    
    def __sync_plob(self):

        msg = net.build_message(message.SYNC_ITEM, self.__item)
        
        self.__sync(msg, self.__sync_plob, "plob", self.__item)
        
        return False
    
    def __sync(self, msg, sync_fn, name, data):
        
        del self.__sync_triggers[sync_fn]
        
        if msg is None:
            return
        
        log.debug("broadcast new %s to clients: %s" % (name, data))
        
        for c in self.__clients: c.send(msg)
        
    # =========================================================================
    # handling client message (inbound communication)
    # =========================================================================
    
    def __handle_message(self, client, id, bindata):
        
        if message.is_control(id):

            log.debug("control from client %s" % client)

            self.__handle_message_control(id, bindata)
            
        elif message.is_action(id):

            log.debug("action from client %s" % client)

            self.__handle_message_action(id, bindata)
            
        elif message.is_request(id):
            
            log.debug("request from client %s" % client)

            self.__handle_message_request(client, id, bindata)
            
        elif id == message.PRIV_INITIAL_SYNC:
            
            msg = net.build_message(message.SYNC_STATE, self.__state)
            client.send(msg)
            
            msg = net.build_message(message.SYNC_PROGRESS, self.__progress)
            client.send(msg)
            
            msg = net.build_message(message.SYNC_ITEM, self.__item)
            client.send(msg)
            
        else:
            log.error("** BUG ** unexpected message: %d" % id)
    
    def __handle_message_control(self, id, bindata):
    
        if id == message.CTRL_PLAYPAUSE:
            
            self.ctrl_toggle_playing()
            
        elif id == message.CTRL_NEXT:
            
            self.ctrl_next()
            
        elif id == message.CTRL_PREV:
            
            self.ctrl_previous()
            
        elif id == message.CTRL_SEEK:
            
            control = serial.unpack(Control, bindata)
            if control is None:
                return
            
            self.ctrl_seek(control.param)
            
        elif id == message.CTRL_VOLUME:
            
            control = serial.unpack(Control, bindata)
            if control is None:
                return
            
            self.ctrl_volume(control.param)
            
        elif id == message.CTRL_REPEAT:
            
            self.ctrl_toggle_repeat()
            
        elif id == message.CTRL_SHUFFLE:
            
            self.ctrl_toggle_shuffle()

        elif id == message.CTRL_RATE:
            
            control = serial.unpack(Control, bindata)
            if control is None:
                return
            
            self.ctrl_rate(control.param)
            
        elif id == message.CTRL_TAG:
            
            tag = serial.unpack(Tagging, bindata)
            if tag is None:
                return
            
            self.ctrl_tag(tag.id, tag.tags)
            
        elif id == message.CTRL_FULLSCREEN:
            
            self.ctrl_toggle_fullscreen()

        elif id == message.CTRL_CLEAR_PL:
            
            self.ctrl_clear_playlist()

        elif id == message.CTRL_CLEAR_QU:
            
            self.ctrl_clear_queue()

        elif id == message.CTRL_SHUTDOWN:
            
            self.__ctrl_shutdown_system()
            
        else:
            log.error("** BUG ** unexpected control message (%d)" % id)
            
    def __handle_message_action(self, id, bindata):
    
        action = serial.unpack(Action, bindata)
        if action is None:
            return
        
        if id == message.ACT_PLAYLIST:
            
            self.action_playlist(action.id, action.positions, action.items)
            
        elif id == message.ACT_QUEUE:
            
            self.action_queue(action.id, action.positions, action.items)
            
        elif id == message.ACT_MEDIALIB:

            self.action_medialib(action.id, action.path, action.positions,
                                 action.items)
            
        elif id == message.ACT_FILES:
        
            uris = self.__util_files_to_uris(action.items)
            
            self.action_files(action.id, action.items, uris)
            
    def __handle_message_request(self, client, id, bindata):

        if id == message.REQ_ITEM:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            self.request_item(client, request.id)
        
        elif id == message.REQ_PLAYLIST:
            
            self.request_playlist(client)
            
        elif id == message.REQ_QUEUE:
            
            self.request_queue(client)
            
        elif id == message.REQ_MLIB:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            self.request_medialib(client, request.path)
            
        elif id == message.REQ_FILES:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            nested, ids, names = self.__filelib.get_level(request.path)
            
            self.__reply_files_request(client, request.path, nested, ids, names)
            
        else:
            log.error("** BUG ** unexpected request message (%d)" % id)
            
    # =========================================================================
    # miscellaneous 
    # =========================================================================
    
    def __util_files_to_uris(self, files):
        
        def file_to_uri(file):
            url = urllib.pathname2url(file)
            return urlparse.urlunparse(("file", None, url, None, None, None))
        
        if not files:
            return []
        
        uris = []
        for file in files:
            uris.append(file_to_uri(file))
            
        return uris
    
    def __util_calc_flags(self, playback_known, volume_known, repeat_known,
                          shuffle_known, progress_known):
        """ Check player adapter capabilities.
        
        Most capabilities get detected by testing which methods have been
        overridden by a subclassing player adapter.
        
        """ 
        
        def ftc(cond, feature):
            if inspect.ismethod(cond): # check if overridden
                enabled = cond.__module__ != __name__
            else:
                enabled = cond
            if enabled:
                return feature
            else:
                return 0  
        
        features = (
                                           
            # --- 'is known' features ---
            
            ftc(playback_known, FT_KNOWN_PLAYBACK),
            ftc(volume_known, FT_KNOWN_VOLUME),
            ftc(repeat_known, FT_KNOWN_REPEAT),
            ftc(shuffle_known, FT_KNOWN_SHUFFLE),
            ftc(progress_known, FT_KNOWN_PROGRESS),

            # --- misc control features ---

            ftc(self.ctrl_toggle_playing, FT_CTRL_PLAYBACK),
            ftc(self.ctrl_volume, FT_CTRL_VOLUME),
            ftc(self.ctrl_seek, FT_CTRL_SEEK),
            ftc(self.ctrl_tag, FT_CTRL_TAG),
            ftc(self.ctrl_clear_playlist, FT_CTRL_CLEAR_PL),
            ftc(self.ctrl_clear_queue, FT_CTRL_CLEAR_QU),
            ftc(self.ctrl_rate, FT_CTRL_RATE),
            ftc(self.ctrl_toggle_repeat, FT_CTRL_REPEAT),
            ftc(self.ctrl_toggle_shuffle, FT_CTRL_SHUFFLE),
            ftc(self.ctrl_next, FT_CTRL_NEXT),
            ftc(self.ctrl_previous, FT_CTRL_PREV),
            ftc(self.ctrl_toggle_fullscreen, FT_CTRL_FULLSCREEN),
        
            # --- request features ---

            ftc(self.request_item, FT_REQ_ITEM),
            ftc(self.request_playlist, FT_REQ_PL),
            ftc(self.request_queue, FT_REQ_QU),
            ftc(self.request_medialib, FT_REQ_MLIB),

            ftc(config.get_system_shutdown_command(), FT_SHUTDOWN),
        
        )
        
        flags = 0
        
        for feature in features:
            flags |= feature
             
        log.debug("flags: %X" % flags)
        
        return flags

    # =========================================================================
    # properties 
    # =========================================================================
    
    # === property: clients ===
    
    def __pget_clients(self):
        """A descriptive list of connected clients.
        
        May be useful to integrate connected clients in a media player UI.

        """ 
        l = []
        for c in self.__clients:
            l.append(str(c))
        return l
    
    clients = property(__pget_clients, None, None, __pget_clients.__doc__)

    # === property: config ===
    
    def __pget_config(self):
        """Player adapter specific configuration (instance of Config).
        
        This mirrors the configuration in ~/.config/remuco/PLAYER/conf. Any
        change to 'config' is saved immediately into the configuration file.
        
        """
        return self.__config
    
    config = property(__pget_config, None, None, __pget_config.__doc__)

    
