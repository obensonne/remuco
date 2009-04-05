# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
#
#    This file is part of Remuco.
#
#    Remuco is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Remuco is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
#
# =============================================================================

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

from remuco.manager import DummyManager

# =============================================================================
# media browser actions
# =============================================================================

class ListAction(object):
    """List related action for a client's media browser.
    
    A list action defines an action a client may apply to a list from the
    player's media library. If possible, player adapters may define list
    actions and send them to clients via PlayerAdapter.replay_mlib_request()
    Clients may then use these actions which results in a call to
    PlayerAdapter.action_mlib_list().
    
    @see: PlayerAdapter.action_mlib_list()
     
    """
    __id_counter = 0
    
    def __init__(self, label):
        """Create a new action for lists from a player's media library.
        
        @param label:
            label of the action (keep short, ideally this is just a single word
            like 'Load', ..)
        
        """
        ListAction.__id_counter -= 1
        self.__id = ListAction.__id_counter
        
        self.label = label
        self.help = None
        
    def __str__(self):
        
        return "(%d, %s)" % (self.__id, self.__label)
        
    # === property: id ===
    
    def __pget_id(self):
        """ID of the action (auto-generated, read only)"""
        return self.__id
    
    id = property(__pget_id, None, None, __pget_id.__doc__)
    
class ItemAction(object):
    """Item related action for a client's media browser.
    
    An item action defines an action a client may apply to a file from the
    local file system, to an item from the playlist, to an item from the play
    queue or to an item from the player's media library.
      
    If possible, player adapters should define item actions and send them to
    clients by setting the keyword 'file_actions' in PlayerAdapter.__init__(),
    via PlayerAdapter.reply_playlist_request(), via
    PlayerAdapter.reply_queue_request() or via
    PlayerAdapter.reply_mlib_request(). Clients may then use these actions
    which results in a call to PlayerAdapter.action_files(),
    PlayerAdapter.action_playlist_item(), PlayerAdapter.action_queue_item() or
    PlayerAdapter.action_mlib_item().
    
    @see: PlayerAdapter.action_files()
    @see: PlayerAdapter.action_playlist()
    @see: PlayerAdapter.action_queue()
    @see: PlayerAdapter.action_mlib_item() 
    
    """
    __id_counter = 0
    
    def __init__(self, label, multiple=False):
        """Create a new action for items or files.
        
        @param label:
            label of the action (keep short, ideally this is just a single word
            like 'Enqueue', 'Play', ..)
        @keyword multiple:
            if the action may be applied to multiple items/files or only to a
            single item/file
        
        """
        ItemAction.__id_counter += 1
        self.__id = ItemAction.__id_counter
        
        self.label = label
        self.help = None
        
        self.multiple = multiple
        
    def __str__(self):
        
        return "(%d, %s, %s)" % (self.id, self.label, self.__multiple)
        
    # === property: id ===
    
    def __pget_id(self):
        """ID of the action (auto-generated, read only)"""
        return self.__id
    
    id = property(__pget_id, None, None, __pget_id.__doc__)
    
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
    
        * ctrl_toggle_playing()
        * ctrl_toggle_repeat()
        * ctrl_toggle_shuffle()
        * ctrl_toggle_fullscreen()
        * ctrl_next()
        * ctrl_previous()
        * ctrl_seek()
        * ctrl_volume()
        * ctrl_rate()
        * ctrl_tag()
        
        * action_files()
        * action_playlist_item()
        * action_queue_item()
        * action_mlib_item()
        * action_mlib_list()
        
        Player adapters only need to implement only a *subset* of these
        methods - depending on what is possible and what makes sense.
        
        Remuco checks which methods have been overridden and uses this
        information to notify Remuco clients about capabilities of player
        adapters. 

    ===========================================================================
    Methods to override to provide information from the media player:
    ===========================================================================
    
        * request_playlist()
        * request_queue()
        * request_mlib()
    
        As above, only override the methods which make sense for the
        corresponding media player.
    
    ===========================================================================
    Methods to call to respond to the requests above:
    ===========================================================================

        * reply_playlist_request()
        * reply_queue_request()
        * reply_mlib_request()
        
    ===========================================================================
    Methods to call to synchronize media player state information with clients:
    ===========================================================================
    
        * update_playback()
        * update_repeat()
        * update_shuffle()
        * update_item()
        * update_position()
        * update_progress()
        
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
        """Create a new player adapter and configure its capabilities.
        
        Just does some early initializations. Real job starts with start().
        
        @param name:
            name of the media player
        @keyword playback_known:
            indicates if the player's playback state can be provided (see
            update_playback())
        @keyword volume_known:
            indicates if the player's volume can be provided (see
            update_volume())
        @keyword repeat_known:
            indicates if the player's repeat mode can be provided (see
            update_repeat())
        @keyword shuffle_known:
            indicates if the player's shuffle mode can be provided (see
            update_shuffle())
        @keyword progress_known:
            indicates if the player's playback progress can be provided (see
            update_progress())
        @keyword max_rating:
            maximum possible rating value for items
        @keyword poll:
            interval in seconds to call poll()
        @keyword file_actions:
            list of ItemAction which can be applied to files from the local
            file system (actions like play a file or append files to the
            playlist) - this keyword is only relevant if the method
            action_files() gets overridden
        @keyword mime_types:
            list of mime types specifying the files to which the actions given
            by the keyword 'file_actions' can be applied, this may be general
            types like 'audio' or 'video' but also specific types like
            'audio/mp3' or 'video/quicktime' (setting this to None means all
            mime types are supported) - this keyword is only relevant if the
            method action_files() gets overridden
        
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
        self.__item = Item(img_size=self.__config.image_size,
                           img_type=self.__config.image_type)
        
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
        
        self.__manager = DummyManager()
        
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
                    self.__info, self.__handle_message, self.__config)
        else:
            self.__server_bluetooth = None

        if self.__config.wifi:
            self.__server_wifi = net.WifiServer(self.__clients,
                    self.__info, self.__handle_message, self.__config)
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
        the current item and then call update_progress(). It can also be used
        to poll any other player state information when a player does not
        provide signals for all or certain state information changes.
        
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
        files (e.g. 'cover.png', 'front.jpg', ...). If there is no such file it
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
            * -1: seek backward 
            * +1: seek forward
        
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
            *  0: mute volume
            * +1: increase by some percent (5 is a good value)
        
        @note: Override if it is possible and makes sense.
               
        """
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
    
    def action_files(self, action_id, files, uris):
        """Do an action on one or more files.
        
        The files are specified redundantly by 'files' and 'uris' - use
        whatever fits better. If the specified action is not applicable to
        multiple files, then 'files' and 'uris' are one element lists.
         
        The files in 'files' and 'uris' may be any files from the local file
        system that have one of the mime types specified by the keyword
        'mime_types' in __init__().
        
        @param action_id:
            ID of the action to do - this specifies one of the actions passed
            previously to __init__() by the keyword 'file_actions'
        @param files:
            list of files to apply the action to (regular path names) 
        @param uris:
            list of files to apply the action to (URI notation) 
        
        @note: Override if file item actions gets passed to __init__().
        
        """
        log.error("** BUG ** action_files() not implemented")
    
    def action_playlist_item(self, action_id, positions, ids):
        """Do an action on one or more items from the playlist.
        
        The items are specified redundantly by 'positions' and 'ids' - use
        whatever fits better. If the specified action is not applicable to
        multiple items, then 'positions' and 'ids' are one element lists. 
        
        @param action_id:
            ID of the action to do - this specifies one of the actions passed
            previously to reply_playlist_request() by the keyword 'item_actions'
        @param positions:
            list of positions to apply the action to
        @param ids:
            list of IDs to apply the action to

        @note: Override if item actions gets passed to reply_playlist_request().
        
        """
        log.error("** BUG ** action_item() not implemented")
    
    def action_queue_item(self, action_id, positions, ids):
        """Do an action on one or more items from the play queue.
        
        The items are specified redundantly by 'positions' and 'ids' - use
        whatever fits better. If the specified action is not applicable to
        multiple items, then 'positions' and 'ids' are one element lists. 
        
        @param action_id:
            ID of the action to do - this specifies one of the actions passed
            previously to reply_queue_request() by the keyword 'item_actions'
        @param positions:
            list of positions to apply the action to 
        @param ids:
            list of IDs to apply the action to

        @note: Override if item actions gets passed to reply_queue_request().
        
        """
        log.error("** BUG ** action_item() not implemented")
    
    def action_mlib_item(self, action_id, path, positions, ids):
        """Do an action on one or more items from the player's media library.
        
        The items are specified redundantly by 'positions' and 'ids' - use
        whatever fits better. If the specified action is not applicable to
        multiple items, then 'positions' and 'ids' are one element lists. 
        
        @param action_id:
            ID of the action to do - this specifies one of the actions passed
            previously to reply_mlib_request() by the keyword 'item_actions'
        @param path:
            the library path that contains the items
        @param positions:
            list of positions to apply the action to 
        @param ids:
            list of IDs to apply the action to

        @note: Override if item actions gets passed to reply_mlib_request().
                
        """
        log.error("** BUG ** action_item() not implemented")
    
    def action_mlib_list(self, action_id, path):
        """Do an action on a list from the player's media library.
        
        @param action_id:
            ID of the action to do - this specifies one of the actions passed
            previously to reply_mlib_request() by the keyword 'list_actions'
        @param path:
            path specifying the list to apply the action to
            
        @note: Override if list actions gets passed to reply_mlib_request().
                
        """
        log.error("** BUG ** action_mlib_list() not implemented")
    
    # =========================================================================
    # request interface 
    # =========================================================================
    
    def __request_item(self, client, id):
        info = {}
        info[INFO_TITLE] = "Sorry, item request is disabled."
        self.__reply_item_request(client, "NoID", info)
        
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

    def request_mlib(self, client, path):
        """Request contents of a specific level from the player's media library.
        
        @param client: the requesting client (needed for reply)
        @param path: path of the requested level (string list)
        
        @note: path is a list of strings which describe a specific level in
            the player's playlist tree. If path is an empty list, the root of
            the player's library (all top level playlists) are requested.

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
            image / cover art (either a file name or URI or an instance of
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
            self.__sync_trigger(self.__sync_item)
            
    # =========================================================================
    # request replies
    # =========================================================================    

    def __reply_item_request(self, client, id, info):
        """Currently unused."""
        
        if self.__stopped:
            return
        
        item = Item()
        item.id = id
        item.info = info

        msg = net.build_message(message.REQ_ITEM, item)
        
        gobject.idle_add(self.__reply, client, msg, "item")
        
    def reply_playlist_request(self, client, ids, names, item_actions=None):
        """Send the reply to a playlist request back to the client.
        
        @param client:
            the client to reply to
        @param ids:
            IDs of the items contained in the playlist
        @param names:
            names of the items contained in the playlist
        @keyword item_actions:
            a list of ItemAction which can be applied to items in the playlist
        
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
        @keyword item_actions:
            a list of ItemAction which can be applied to items in the queue
        
        @see: request_queue()
        
        """ 
        if self.__stopped:
            return
        
        queue = ItemList(None, None, ids, names, item_actions, None)
        
        msg = net.build_message(message.REQ_QUEUE, queue)
        
        gobject.idle_add(self.__reply, client, msg, "queue")
    
    def reply_mlib_request(self, client, path, nested, ids, names,
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
        @keyword item_actions:
            a list of ItemAction which can be applied to items at the
            requested path 
        @keyword list_actions:
            a list of ListAction which can be applied to nested lists at the
            requested path 
        
        @see: request_mlib()
        
        """ 
        if self.__stopped:
            return
        
        lib = ItemList(path, nested, ids, names, item_actions, list_actions)
        
        msg = net.build_message(message.REQ_MLIB, lib)
        
        gobject.idle_add(self.__reply, client, msg, "mlib")
        
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
    
    def __sync_item(self):

        msg = net.build_message(message.SYNC_ITEM, self.__item)
        
        self.__sync(msg, self.__sync_item, "item", self.__item)
        
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

        elif id == message.CTRL_SHUTDOWN:
            
            self.__ctrl_shutdown_system()
            
        else:
            log.error("** BUG ** unexpected control message: %d" % id)
            
    def __handle_message_action(self, id, bindata):
    
        a = serial.unpack(Action, bindata)
        if a is None:
            return
        
        if id == message.ACT_PLAYLIST:
            
            self.action_playlist_item(a.id, a.positions, a.items)
            
        elif id == message.ACT_QUEUE:
            
            self.action_queue_item(a.id, a.positions, a.items)
            
        elif id == message.ACT_MLIB and a.id < 0:
            
            self.action_mlib_list(a.id, a.path)
                
        elif id == message.ACT_MLIB and a.id > 0:
            
            self.action_mlib_item(a.id, a.path, a.positions, a.items)
                
        elif id == message.ACT_FILES:
        
            uris = self.__util_files_to_uris(a.items)
            
            self.action_files(a.id, a.items, uris)
        
        else:
            log.error("** BUG ** unexpected action message: %d" % id)
            
    def __handle_message_request(self, client, id, bindata):

        if id == message.REQ_ITEM:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            self.__request_item(client, request.id)
        
        elif id == message.REQ_PLAYLIST:
            
            self.request_playlist(client)
            
        elif id == message.REQ_QUEUE:
            
            self.request_queue(client)
            
        elif id == message.REQ_MLIB:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            self.request_mlib(client, request.path)
            
        elif id == message.REQ_FILES:
            
            request = serial.unpack(Request, bindata)    
            if request is None:
                return
            
            nested, ids, names = self.__filelib.get_level(request.path)
            
            self.__reply_files_request(client, request.path, nested, ids, names)
            
        else:
            log.error("** BUG ** unexpected request message: %d" % id)
            
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
            ftc(self.ctrl_rate, FT_CTRL_RATE),
            ftc(self.ctrl_toggle_repeat, FT_CTRL_REPEAT),
            ftc(self.ctrl_toggle_shuffle, FT_CTRL_SHUFFLE),
            ftc(self.ctrl_next, FT_CTRL_NEXT),
            ftc(self.ctrl_previous, FT_CTRL_PREV),
            ftc(self.ctrl_toggle_fullscreen, FT_CTRL_FULLSCREEN),
        
            # --- request features ---

            ftc(self.__request_item, FT_REQ_ITEM),
            ftc(self.request_playlist, FT_REQ_PL),
            ftc(self.request_queue, FT_REQ_QU),
            ftc(self.request_mlib, FT_REQ_MLIB),

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

    # === property: manager ===
    
    def __pget_manager(self):
        """The Manager controlling this adapter.
        
        This property may be used to call the method stop() on to stop and
        completely shutdown the adapter from within an adapter. Calling
        Manager.stop() has the same effect as if the Manager process
        received a SIGINT or SIGTERM. 
        
        If this adapter is not controlled by or has not yet assigned a Manager
        then this property refers to a dummy manager - so it is allways safe
        to call stop() on this manager.
        
        @see: Manager
        
        """
        return self.__manager
    
    def __pset_manager(self, value):
        self.__manager = value
    
    manager = property(__pget_manager, __pset_manager, None,
                       __pget_manager.__doc__)

