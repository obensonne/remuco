# -*- coding: UTF-8 -*-
""" Remuco player adapter module.

This module provides classes and constants for Remuco player adapters.

Class PlayerAdapter:
    Base class for player adapter.

Class DBusManager:
    Helper class for player adapters using DBus for player communication.

Class ScriptManager:
    Helper class for player adapters running as scripts.

Constants:
    The constants starting with 'INFO' are the keys to use for the dictionary
    describing a PLOB (PLOB means playable object - a song, video, slide or
    whatever may be played in that spirit).
    
    The constants starting with 'PLAYBACK' are the values used by Remuco to
    describe a playback state.

Logging:
    It is recommended to use the remuco logging system within player adapters.
    To do so, import the module 'remuco.log' and use the functions
    
    * remuco.log.debug(),
    * remuco.log.info(),
    * remuco.log.warning() and
    * remuco.log.error().
    
    Then all messages of the player adapter will be written into the same file
    as used internally by the remuco module - that makes debugging a lot easier.
    
    Internally Remuco uses the module 'logging' for all its logging messages.
    Messages go into a player specific log file (usually
    ~/.cache/remuco/PLAYER/log). The log level is defined in a player specific
    configuration file (usually ~/.config/remuco/PLAYER/conf).

"""
#===============================================================================
# imports
#===============================================================================

import subprocess
import os

import gobject

import log
import message
import command
import config
import serial
import net

from data import PlayerState, Library, Control, Plob, \
                 SerialString, PlayerInfo

#===============================================================================
# remuco constants
#===============================================================================

PLAYBACK_PAUSE = 1
PLAYBACK_PLAY = 2
PLAYBACK_STOP = 0

__INFO_ABSTRACT = "__abstract__"
INFO_ALBUM = "album"
INFO_ARTIST = "artist"
INFO_BITRATE = "bitrate"
INFO_COMMENT = "comment"
INFO_GENRE = "genre"
INFO_LENGTH = "length"
INFO_RATING = "rating"
INFO_TAGS = "tags"
INFO_TITLE = "title"
INFO_TRACK = "track"
INFO_YEAR = "year"
INFO_TYPE = "type"

INFO_TYPE_AUDIO = 1
INFO_TYPE_VIDEO = 2
INFO_TYPE_OTHER = 3


#===============================================================================
# player class
#===============================================================================

class PlayerAdapter:
    
    """ Base class for Remuco player adapters.
    
    Remuco player adapters must subclass this class and overwrite certain
    methods to implement player specific behavior. Additionally PlayerAdapter
    provides methods to interact with Remuco clients. 
    
    Methods to overwrite to manage life cycle:
    
        * start()
        * stop()
    
        A PlayerAdapter can be started and stopped with start() and stop().
        Subclasses of PlayerAdapter may override these methods as needed but
        must always call the super class implementations too! The same instance
        of a PlayerAdapter should be startable and stoppable multiple times.
    
    Methods to overwrite to control the media player:
    
        * jump_in_playlist()
        * jump_in_queue()
        * load_playlist()
        * play_next()
        * play_previous()
        * rate_current()
        * toggle_play_pause()
        * toggle_repeat()
        * toggle_shuffle()
        * seek_backward()
        * seek_forward()
        * set_tags()
        * set_volume()
    
        Not all methods must be overwritten. Some even do not make sense for
        certain players, e.g. jump_in_queue() only should be overwritten if the
        media player has a play queue. Do not overwrite methods you finally do
        not implement because Remuco checks which methods has been overwritten
        and uses this information to notify Remuco clients about capabilities
        of player adapters.

    Methods to overwrite to provide information from the media player:
    
        * get_rating_max()
        * request_plob()
        * request_playlist()
        * request_queue()
        * request_library()
    
        As above, only overwrite the methods that makes sense for the
        corresponding media player. 
    
    Methods to call to send media player information to clients:
    
        * update_play_position()
        * update_playback()
        * update_repeat_mode()
        * update_shuffle_mode()
        * update_plob()
        
        These methods should be called whenever the corresponding information
        has changed in the media player.
        
        * reply_plob_request()
        * reply_playlist_request()
        * reply_queue_request()
        * reply_library_request()
        
        These methods should be called to reply to calls to one of the
        request methods above.
        
    Finally some not so important utility methods:
    
        * get_cache_dir()
        * get_log_file()
        * get_clients()
        
    """

    #==========================================================================
    # constructor 
    #==========================================================================
    
    def __init__(self, name):
        """Create a new player.
        
        Just does some early initializations. Real job starts with start().
        
        @param name: name of the player
        
        @attention: When overwriting, call super class implementation first! 
        """
        
        self.__name = name
        
        # init config and logging
        
        self.__config = config.Config(self.__name)

        log.set_file(self.__config.get_log_file())
        log.set_level(self.__config.get_log_level())
        
        self.__config.set_custom("dummy", "ymmud") # force a config save
        
        # init misc fields
        
        serial.Bin.HOST_ENCODING = self.__config.get_encoding()
        
        self.__clients = []
        
        self.__state = PlayerState()
        self.__plob = Plob()
        self.__info = PlayerInfo(name, self.__get_flags(), self.get_rating_max())
        
        self.__sync_trigger_source_ids = {}
        self.__ping_source_id = 0
        
        self.__stopped = True
        
        self.__server_bluetooth = None
        self.__server_wifi = None
            
        log.debug("PlayerAdapter.__init__() done")
    
    def start(self):
        """ Start the player adapter.
        
        @attention: When overwriting, call super class implementation first! 
        """
        
        if not self.__stopped:
            log.debug("ignore start, already running")
            return
        
        self.__stopped = False
        
        # set up server
        
        if self.__config.get_bluetooth():
            self.__server_bluetooth = net.BluetoothServer(self.__clients,
                    self.__info, self.__handle_message_from_client)
        else:
            self.__server_bluetooth = None

        if self.__config.get_wifi():
            self.__server_wifi = net.WifiServer(self.__clients,
                    self.__info, self.__handle_message_from_client)        
        else:
            self.__server_wifi = None
            
        # set up client ping
        
        ping_msg = net.build_message(message.MSG_ID_IGNORE, None)
        ping = self.__config.get_ping()
        if ping > 0:
            log.debug("ping clients every %d seconds" % ping)
            self.__ping_source_id = gobject.timeout_add(ping * 1000, self.__ping,
                                                  ping_msg)
        else:
            self.__ping_source_id = 0
        
    
    def stop(self):
        """Shutdown the player adapter.
        
        Disconnects all clients and shuts down the Bluetooth and WiFi server.
        Also ignores any subsequent calls to an update or reply method (e.g.
        update_volume(), ..., reply_plob_request(), ...). 
        
        @note: The same player adapter instance can be started again with
               start().

        @attention: When overwriting, call super class implementation first! 
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
            
        for id in self.__sync_trigger_source_ids.values():
            if id is not None:
                gobject.source_remove(id)
        self.__sync_trigger_source_ids = {}

        if self.__ping_source_id > 0:
            gobject.source_remove(self.__ping_source_id)
        
    #==========================================================================
    # some utility functions which may be useful for player adapters
    #==========================================================================
    
    def get_cache_dir(self):
        """ Get a player specific cache directory.
        
        In most cases this is ~/.cache/remuco/PLAYER

        @return: cache dir name
        """
        
        return self.__config.get_cache_dir()
    
    def get_log_file(self):
        """ Get a player specific log file.
        
        @return: log file name
        
        @note: Do not use this file to write log messages into. This should be
               done with the module log (contained within Remuco). The idea
               of the method is to integrate log information in the media
               player UI.
        """
        
        return self.__config.get_log_file()

    def get_name(self):
        """ Get the name of the player.
        
        Probably not that useful for player adapters (used internally).
        """
        return self.__name
    
    def get_clients(self):
        """ Get a descriptive list of connected clients.
        
        May be useful to integrate connected clients in a media player UI.

        @return: a list of client names (or addresses)
        """ 
        
        l = []
        for c in self.__clients:
            l.append(c.get_address())
        return l
    
    #==========================================================================
    # client side player control (to be implemented by sub classes) 
    #==========================================================================
    
    def get_rating_max(self):
        """ Get the maximum possible rating value.
        
        @return: the player's maximum rating value
        
        @note: This method should be overwritten by sub classes of PlayerAdapter
               if the corresponding player supports rating.
        """
        return 0
    
    def jump_in_playlist(self, position):
        """ Jump to a specific position in the currently active playlist.
        
        @param postion: the position (starting form 0) 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("jump_in_playlist() not yet implemented")
        
    def jump_in_queue(self, position):
        """ Jump to a specific position in the queue.
        
        @param postion: the position (starting form 0) 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("jump_in_queue() not yet implemented")
    
    def load_playlist(self, path):
        """ Load a playlist.
        
        For some players this means 'switch to another playlist', for others
        it means 'put the content of another playlist into the active playlist'.
        
        @param path: the path of the playlist to load (path is a library
                     level as described in request_library() ) 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("load_playlist() not yet implemented")
    
    def play_next(self):
        """ Play the next item. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("play_next() not yet implemented")
    
    def play_previous(self):
        """ Play the previous item. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("play_previous() not yet implemented")
    
    def rate_current(self, rating):
        """ Rate the currently played item. 
        
        @param rating: rating value (int)
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("rate_current() not yet implemented")
    
    def toggle_play_pause(self):
        """ Toggle play and pause. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("toggle_play_pause() not yet implemented")
    
    def toggle_repeat(self):
        """ Toggle repeat mode. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("toggle_repeat() not yet implemented")
    
    def toggle_shuffle(self):
        """ Toggle shuffle mode. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("toggle_shuffle() not yet implemented")
    
    def seek_forward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("seek_forward() not yet implemented")
    
    def seek_backward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("seek_backward() not yet implemented")
    
    def set_tags(self, id, tags):
        """ Attach some tags to a PLOB. 
        
        @param id: ID of the PLOB to attach the tags to
        @param tags: a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
               idea of tags (e.g. like used at last.fm). 

        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("set_tags() not yet implemented")
    
    def set_volume(self, volume):
        """ Set volume. 
        
        @param volume: the new volume in percent
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client wants to
               control the player.
        """
        log.warning("set_volume() not yet implemented")
        
    def request_playlist(self, client):
        """ Request the content of the currently active playlist. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client requests the
               player's current playlist.

        @see: reply_playlist_request() for sending back the result
        """
        log.warning("request_playlist() not yet implemented")

    def request_queue(self, client):
        """ Request the content of the play queue. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client requests the
               player's current queue.

        @see: reply_queue_request() for sending back the result
        """
        log.warning("request_queue() not yet implemented")

    def request_plob(self, client, id):
        """ Request information about a specific PLOB. 
        
        @param client: the requesting client (needed for reply)
        @param id: ID of the requested PLOB (string)
        
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client requests
               information about a PLOB from the player.

        @see: reply_plob_request() for sending back the result
        """
        log.warning("request_plob() not yet implemented")
        
    def request_library(self, client, path):
        """ Request contents of a specific level from the player's library.
        
        @param client: the requesting client (needed for reply)
        @param path: path of the requested level (string list)
        
        @note: path is a list of strings which describes a specific level in the
               player's playlist tree. If path is an empty list, the
               root of the player's library, i.e. all top level playlists are
               requested.

               A player may have a library structure like this:

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
               
        @note: This method should be overwritten by sub classes of
               PlayerAdapter. It gets called if a remote client requests a
               specific level of the player's media library.
               
        @see: reply_list_request() for sending back the result
        """
        log.warning("request_library() not yet implemented")
        
    #==========================================================================
    # player side synchronization
    #==========================================================================    

    def update_play_position(self, position, queue=False):
        """Set the position of the current PLOB. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param position: position of the currently player item (starting from 0)
        @keyword queue: True if currently played item is from the queue, False
                        if it is from the currently active playlist (default)
        """
        
        change = self.__state.get_queue() != queue
        change |= self.__state.get_position() != position
        
        if change:
            self.__state.set_queue(queue)
            self.__state.set_position(position)
            self.__trigger_sync(self.__sync_state)
        
    def update_playback(self, playback):
        """Set the current playback state.
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param playback: playback mode (see constants)
        """
        
        change = self.__state.get_playback() != playback
        
        if change:
            self.__state.set_playback(playback)
            self.__trigger_sync(self.__sync_state)
    
    def update_repeat_mode(self, repeat):
        """Set the current repeat mode. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param repeat: true means on, false means off
        """
        
        change = self.__state.get_repeat() != repeat
        
        if change:
            self.__state.set_repeat(repeat)
            self.__trigger_sync(self.__sync_state)
    
    def update_shuffle_mode(self, shuffle):
        """Set the current shuffle mode. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param shuffle: true means on, false means off
        """
        
        change = self.__state.get_shuffle() != shuffle
        
        if change:
            self.__state.set_shuffle(shuffle)
            self.__trigger_sync(self.__sync_state)
    
    def update_volume(self, volume):
        """Set the current volume.
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param volume: the volume in percent
        """
        
        change = self.__state.get_volume() != volume
        
        if change:
            self.__state.set_volume(volume)
            self.__trigger_sync(self.__sync_state)
    
    def update_plob(self, id, info, img):
        """Set current PLOB.
        
        This method is intended to be called by player adapters to sync the
        currently played item with remote clients.
        
        @param id: PLOB ID (string)
        @param info: PLOB meta information (dict)
        @param img: image / cover art file (either a file name or an
                    instance of Image.Image)
        
        @note: For the PLOB meta information dictionary use one of the constants
               starting with 'INFO_' (e.g. INFO_ARTIST) as keys. 
        """
        
        change = self.__plob.get_id() != id
        
        if change:
            self.__plob.set_id(id)
            self.__plob.set_info(info)
            self.__plob.set_img(img)
            self.__trigger_sync(self.__sync_plob)
        
    def reply_playlist_request(self, client, plob_ids, plob_names):
        """Send the reply to a playlist request back to the client.
        
        @param client: the client to reply to
        @param plob_ids: IDs of the PLOBs contained in the playlist
        @param plob_names: names of the PLOBs contained in the playlist
        
        @see: request_playlist()        
        """ 
        
        if self.__stopped:
            return

        playlist = Library(Library.PATH_PLAYLIST, [], plob_ids, plob_names)
        
        msg = net.build_message(message.MSG_ID_REQ_LIBRARY, playlist)
        
        client.send(msg)
    
    def reply_queue_request(self, client, plob_ids, plob_names):
        """Send the reply to a queue request back to the client.
        
        @param client: the client to reply to
        @param plob_ids: IDs of the PLOBs contained in the queue
        @param plob_names: names of the PLOBs contained in the queue
        
        @see: request_queue()        
        """ 
        
        if self.__stopped:
            return

        queue = Library(Library.PATH_QUEUE, [], plob_ids, plob_names)
        
        msg = net.build_message(message.MSG_ID_REQ_LIBRARY, queue)
        
        client.send(msg)
    
    def reply_library_request(self, client, path, nested, plob_ids, plob_names):
        """Send the reply to a library request back to the client.
        
        @param client: the client to reply to
        @param path: path of the requested library level (string list)
        @param nested: nested playlists (string list)
        @param plob_ids: IDs of the PLOBs contained in the requested library level
        @param plob_names: names of the PLOBs contained in the requested library level
        
        @see: request_library()
        """ 
        
        if self.__stopped:
            return

        library = Library(path, nested, plob_ids, plob_names)
        
        msg = net.build_message(message.MSG_ID_REQ_LIBRARY, library)
        
        client.send(msg)
    
    def reply_plob_request(self, client, id, info):
        """Send a reply to a PLOB request back to client.
        
        @param client: the client to reply to
        @param id: ID of the requested PLOB
        @param info: a dictionary with PLOB meta information (see update_plob())
                     
        @see: request_plob()
        """
        
        if self.__stopped:
            return
        
        plob = Plob()
        plob.set_id(id)
        plob.set_info(info)

        msg = net.build_message(message.MSG_ID_REQ_PLOB, plob)
        
        client.send(msg)
    
    #==========================================================================
    # synchronization (outbound communication)
    #==========================================================================
    
    def __trigger_sync(self, sync_fn):
        
        if self.__stopped:
            return
        
        try:
            id = self.__sync_trigger_source_ids[sync_fn]
        except KeyError:
            id = None
        
        if id is not None:
            log.debug("trigger for %s already active" % sync_fn.func_name)
            return
        
        self.__sync_trigger_source_ids[sync_fn] = \
            gobject.idle_add(sync_fn, priority=gobject.PRIORITY_LOW)
        
    def __sync_state(self):
        
        msg = net.build_message(message.MSG_ID_SYN_STATE, self.__state)
        
        self.__sync(msg, self.__sync_state)
        
        return False
    
    def __sync_plob(self):

        msg = net.build_message(message.MSG_ID_SYN_PLOB, self.__plob)
        
        self.__sync(msg, self.__sync_plob)
        
        return False
    
    def __sync(self, msg, calling_sync_fn):
        
        self.__sync_trigger_source_ids[calling_sync_fn] = None
        
        if msg is None: return
        
        log.debug("broadcast new %s to clients" % calling_sync_fn.func_name[7:])
        
        for c in self.__clients: c.send(msg)
        
    #==========================================================================
    # handling client message (inbound communication)
    #==========================================================================
    
    def __handle_message_from_client(self, id, bindata, client):
        
        if id == message.MSG_ID_CTL:

            self.__handle_message_control(bindata)
            
        elif id == message.MSG_ID_REQ_PLOB:
            
            self.__handle_message_request_plob(bindata, client)
            
        elif id == message.MSG_ID_REQ_LIBRARY:
            
            self.__handle_message_request_list(bindata, client)
            
        elif id == message.MSG_ID_PRIV_REQ_INITIAL_DATA:
            
            msg = net.build_message(message.MSG_ID_SYN_STATE, self.__state)
            client.send(msg)
            
            msg = net.build_message(message.MSG_ID_SYN_PLOB, self.__plob)
            client.send(msg)
            
        else:
            log.warning("unsupported message id: %d" % id)
    
    def __handle_message_control(self, bindata):
    
        control = Control()
        
        ok = serial.unpack(control, bindata)    
        if not ok: return
        
        cmd = control.get_cmd()
        param_i = control.get_param_i()
        param_s = control.get_param_s()
        if param_s is None:
            param_s = ""
        
        if cmd == command.CMD_IGNORE:
            return
        elif cmd == command.CMD_PLAYPAUSE:
            self.toggle_play_pause()
        elif cmd == command.CMD_VOLUME:
            self.set_volume(param_i)
        elif cmd == command.CMD_NEXT:
            self.play_next()
        elif cmd == command.CMD_PREV:
            self.play_previous()
        elif cmd == command.CMD_JUMP:
            self.__handle_message_control_jump(param_s.split("/"), param_i)
        elif cmd == command.CMD_RATE:
            self.rate_current(param_i)
        elif cmd == command.CMD_REPEAT:
            self.toggle_repeat()
        elif cmd == command.CMD_SHUFFLE:
            self.toggle_shuffle()
        elif cmd == command.CMD_SETTAGS:
            id, tags = param_s.split(":")
            tags = tags.split(",")
            self.set_tags(id, tags)
        elif cmd == command.CMD_SEEK_FWD:
            self.seek_forward()
        elif cmd == command.CMD_SEEK_BWD:
            self.seek_backward()
        elif cmd == command.CMD_SHUTDOWN:
            self.__shutdown_system()
            
    def __handle_message_control_jump(self, path, position):
        
        if path == Library.PATH_PLAYLIST:
            self.jump_in_playlist(position)
        elif path == Library.PATH_QUEUE:
            self.jump_in_queue(position)
        else:
            self.load_playlist(path)
            self.jump_in_playlist(position)

    def __handle_message_request_plob(self, bindata, client):
    
        ss = SerialString()
        
        ok = serial.unpack(ss, bindata)
        if not ok: return
        
        self.request_plob(client, ss.get())
        
    def __handle_message_request_list(self, bindata, client):

        ss = SerialString()
        
        ok = serial.unpack(ss, bindata)
        if not ok: return
        
        s = ss.get()
        if s is None or len(s) == 0:
            path = []
        else:
            path = s.split("/")
        
        log.debug("list request: %s" % str(path))
        
        if path == Library.PATH_PLAYLIST:
            self.request_playlist(client)
        elif path == Library.PATH_QUEUE:
            self.request_queue(client)
        else:
            self.request_library(client, path)
            
    #==========================================================================
    # miscellaneous 
    #==========================================================================
    
    def __shutdown_system(self):
        
        shutdown_cmd = self.__config.get_shutdown_system_command()
        if shutdown_cmd is not None:
            log.debug("run shutdown command")
            try:
                subprocess.Popen(shutdown_cmd, shell=True)
            except OSError, e:
                log.warning("failed to run shutdown command (%s)", e)
                return
            self.stop()

    def __get_flags(self):
        """ Check player adapter capabilities.
        
        Capabilities are detected by testing which methods have been overwritten
        by the subclassing player adapter.
        """ 
        
        flags = 0
        
        me = str(self.__get_flags.__module__)
        
        if str(self.request_playlist.__module__) != me:
            flags |= PlayerInfo.FEATURE_PLAYLIST
        if str(self.request_queue.__module__) != me:
            flags |= PlayerInfo.FEATURE_QUEUE
        if str(self.request_library.__module__) != me:
            flags |= PlayerInfo.FEATURE_LIBRARY
        if str(self.request_plob.__module__) != me:
            flags |= PlayerInfo.FEATURE_PLOBINFO
        if str(self.set_tags.__module__) != me:
            flags |= PlayerInfo.FEATURE_TAGS
        if str(self.jump_in_playlist.__module__) != me:
            flags |= PlayerInfo.FEATURE_JUMP_PLAYLIST
        if str(self.jump_in_queue.__module__) != me:
            flags |= PlayerInfo.FEATURE_JUMP_QUEUE
        if str(self.load_playlist.__module__) != me:
            flags |= PlayerInfo.FEATURE_LOAD_PLAYLIST
             
        shutdown_cmd = self.__config.get_shutdown_system_command()
        if shutdown_cmd is not None and os.access(shutdown_cmd, os.X_OK):
            flags |= PlayerInfo.FEATURE_SHUTDOWN_HOST
             
        log.debug("features: %X" % flags)
        
        return flags

    def __ping(self, msg):
        """ Ping clients to keep connection alive."""
        
        for c in self.__clients:
            log.debug("ping client %s" % c.get_address())
            c.send(msg)
            
        return True

# =============================================================================
# dbus stuff
# =============================================================================

import dbus
import dbus.exceptions
import dbus.mainloop.glib

class DBusManager():
    """ Helper class for player adapters talking to media players via DBus.
    
    A DBusManager automatically starts and stops a player adapter if the
    corresponding media player starts or stops.
    """
    
    def __init__(self, pa, dbus_name):
        """ Create a new DBusManager.
        
        @param pa: the PlayerAdapter to automatically start and stop
        @param dbus_name: the bus name used by the adapter's media player
        """

        dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)

        self.__pa = pa
        self.__dbus_name = dbus_name
        
        bus = dbus.SessionBus()

        proxy = bus.get_object("org.freedesktop.DBus", "/org/freedesktop/DBus")
        self.__dbus = dbus.Interface(proxy, "org.freedesktop.DBus")

        self.__handlers = (
            self.__dbus.connect_to_signal("NameOwnerChanged",
                                          self.__notify_owner_change,
                                          arg0=self.__dbus_name)
            ,
        )

        self.__dbus.NameHasOwner(self.__dbus_name,
                                 reply_handler=self.__set_has_owner,
                                 error_handler=self.__dbus_error)
        
    def __notify_owner_change(self, name, old, new):
        
        log.info("dbus name owner change: '%s' -> '%s'" % (old, new))
        
        self.__pa.stop()
        
        if new is not None and len(new) > 0:
            try:
                self.__pa.start()
            except Exception, e:
                pass
    
    def __set_has_owner(self, has_owner):
        
        if has_owner:
            self.__pa.start()
    
    def __dbus_error(self, error):
        log.error("dbus error: %s" % error)
        
    def start(self):
        """ This does nothing.
        
        Only defined to handle a DBusManager like a PlayerAdapter in the
        ScriptManager.
        """
    
    def stop(self):
        """ Shut down the DBusManager.
        
        This also shuts down the player adapter.
        """
        
        if self.__pa is not None:
            self.__pa.stop()
        
        for handler in self.__handlers:
            handler.remove()
        self.__dbus_handlers = ()
        
        self.__dbus = None
            
# =============================================================================
# script stuff
# =============================================================================

import signal

_ml = gobject.MainLoop()

def _sighandler(signum, frame):
    """ Used internally by the ScriptManager. """
    
    log.info("received signal %i" % signum)
    global _ml
    _ml.quit()

class ScriptManager():
    """ Helper class for player adapters working as scripts.
    
    A ScriptManager is intended for player adapters which run as scripts (in
    contrast to e.g. player adapters implemented as plugins for their
    corresponding media players). It automatically sets up a main loop, starts
    the player adapter and stops it on SIGINT or SIGTERM.
    """
    
    def __init__(self, pa_dm):
        """ Create a new ScriptManager.
        
        @param pa_dm: a PlayerAdapter or DBusManager instance
        """
        
        self.__pa_dm = pa_dm

        signal.signal(signal.SIGINT, _sighandler)
        signal.signal(signal.SIGTERM, _sighandler)

    def run(self):
        """ Run the script manager.
        
        This method blocks until SIGINT or SIGTERM arrives. Then it stops the
        player adapter and returns .. you are done :).
        """
    
        self.__pa_dm.start()

        global _ml
        log.info("start main loop")
        _ml.run()
        log.info("stopped main loop")

        self.__pa_dm.stop()
        