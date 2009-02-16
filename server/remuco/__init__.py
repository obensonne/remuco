# -*- coding: UTF-8 -*-
""" Remuco player adapter module.

The module 'remuco' is the interface between a media player and the Remuco
system. It provides one class and several constants.

The class Player is supposed to be sub classed by Remuco adapters for specific
media players.

Some notes on logging:

Remuco uses the module 'logging' for all its logging messages. By default
logging is configured to log into a player specific log file (usually
~/.cache/remuco/PLAYER/log). The log level is defined in the player specific
configuration file (usually ~/.config/remuco/PLAYER/conf).

To use the remuco logging system within a player adapter, import the module
'log' and use the function log.debug(), log.info(), log.warning() and
log.error(). Then all messages of the player adapter will be written into the
same file which is used for internal remuco logging messages.

"""
#===============================================================================
# imports
#===============================================================================

import gobject
import subprocess

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

INFO_ABSTRACT = "__abstract__"
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

class Player:
    
    """ Class for Remuco player adapters to derive from. 
    
    Player is a class any media player adapter is supposed to derive from.
    It provides some methods media player adapters can use to interact with
    Remuco clients and some methods which must be overwritten by sub classes
    in order to implement player specific behavior.
    
    The following methods can be used to interact with clients:
    
    * update_play_position()
    * update_playback()
    * update_repeat_mode()
    * update_shuffle_mode()
    * update_plob()
    
    * reply_plob_request()
    * reply_playlist_request()
    * reply_queue_request()
    * reply_library_request()
    
    The term PLOB means playable object and simply refers to a song, a video
    or whatever you can imagine as a playable object. 

    The following methods may be overwritten by sub classes to implement
    media player specific behavior:
    
    * get_rating_max()
    * jump_to()
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
    
    * request_plob()
    * request_playlist()
    * request_queue()
    * request_library()
    
    Not all methods must be overwritten. Some even do not make sense for
    certain players, e.g. get_rating_max() only should be overwritten if the
    media player supports rating. Do not overwrite methods you finally do not
    implement because Remuco checks which methods has been overwritten and uses
    this information to notify Remuco clients about capabilities of player
    adapters.
    
    If a player adapter needs a cache directory, it can get one via
    get_cache_dir(). If a player adapter wants to integrate log information
    into the media player UI, it has to call get_log_file() to get the name
    of the log file used within remuco to log messages.
    
    """

    #==========================================================================
    # constructor 
    #==========================================================================
    
    def __init__(self, name):
        """Create a new player.
        
        @param name: name of the player
        
        @attention: If sub classes overwrite the constructor, they must call
                    this constructor in the overwritten constructor! 
        """
        
        self.__name = name
        
        self.__config = config.Config(self.__name)

        log.set_file(self.__config.get_log_file())
        log.set_level(self.__config.get_log_level())
        
        self.__config.set_custom("dummy", "ymmud") # force a config save
        
        serial.Bin.HOST_ENCODING = self.__config.get_encoding()
        
        self.__clients = []
        
        self.__state = PlayerState()
        self.__plob = Plob()
        self.__info = PlayerInfo(name, self.__get_flags(), self.get_rating_max())
        
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
            
        self.__sync_trigger_ids = {}
        
        self.__shutting_down = False
        
        log.debug("Player() done")
        
    #==========================================================================
    # configuration
    #==========================================================================
    
    def get_cache_dir(self):
        """ Get a player specific cache directory.
        
        In most cases this is ~/.cache/remuco/PLAYER

        @return: cache dir name
        
        @attention: Do not overwrite!
        """
        
        return self.__config.get_cache_dir()
    
    def get_log_file(self):
        """ Get a player specific log file.
        
        @return: log file name
        
        @note: Do not use this file to write log messages into. This should be
               done with the module log (contained within Remuco). The idea
               of the method is that it can be used by player adapters to
               integrate log information in the media player UI
        
        @attention: Do not overwrite!
        """
        
        return self.__config.get_log_file()

    #==========================================================================
    # client side player control (to be implemented by sub classes) 
    #==========================================================================
    
    def get_rating_max(self):
        """ Get the maximum possible rating value.
        
        @return: the player's maximum rating value
        
        @note: This method should be overwritten by sub classes of Player if the
               corresponding player supports rating.
        """
        return 0
    
    def jump_in_playlist(self, position):
        """ Jump to a specific position in the currently active playlist.
        
        @param postion: the position (starting form 0) 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("jump_in_playlist() not yet implemented")
        
    def jump_in_queue(self, position):
        """ Jump to a specific position in the queue.
        
        @param postion: the position (starting form 0) 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("jump_in_queue() not yet implemented")
    
    def load_playlist(self, path):
        """ Load a playlist.
        
        For some players this means 'switch to another playlist', for others
        it means 'put the content of another playlist into the active playlist'.
        
        @param path: the path of the playlist to load (path is a library
                     level as described in request_library() ) 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("load_playlist() not yet implemented")
    
    def play_next(self):
        """ Play the next item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("play_next() not yet implemented")
    
    def play_previous(self):
        """ Play the previous item. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("play_previous() not yet implemented")
    
    def rate_current(self, rating):
        """ Rate the currently played item. 
        
        @param rating: rating value (int)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("rate_current() not yet implemented")
    
    def toggle_play_pause(self):
        """ Toggle play and pause. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("toggle_play_pause() not yet implemented")
    
    def toggle_repeat(self):
        """ Toggle repeat mode. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("toggle_repeat() not yet implemented")
    
    def toggle_shuffle(self):
        """ Toggle shuffle mode. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("toggle_shuffle() not yet implemented")
    
    def seek_forward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("seek_forward() not yet implemented")
    
    def seek_backward(self):
        """ Seek forward some seconds. 
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("seek_backward() not yet implemented")
    
    def set_tags(self, id, tags):
        """ Attach some tags to a PLOB. 
        
        @param id: ID of the PLOB to attach the tags to
        @param tags: a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
               idea of tags (e.g. like used at last.fm). 

        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("set_tags() not yet implemented")
    
    def set_volume(self, volume):
        """ Set volume. 
        
        @param volume: the new volume in percent
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client wants to control the player.
        """
        log.warning("set_volume() not yet implemented")
        
    def request_playlist(self, client):
        """ Request the content of the currently active playlist. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a PLOB from the player.

        @see: reply_playlist_request() for sending back the result
        """
        log.warning("request_playlist() not yet implemented")

    def request_queue(self, client):
        """ Request the content of the play queue. 
        
        @param client: the requesting client (needed for reply)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a PLOB from the player.

        @see: reply_queue_request() for sending back the result
        """
        log.warning("request_queue() not yet implemented")

    def request_plob(self, client, id):
        """ Request information about a specific PLOB. 
        
        @param client: the requesting client (needed for reply)
        @param id: ID of the requested PLOB (string)
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a PLOB from the player.

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
               
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client requests a playlist from the
               player.
               
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
        
        @attention: Do not overwrite!
        """
        
        self.__state.set_queue(mode)
        self.__state.set_position(position)
        self.__trigger_sync(self.__sync_state)
        
    def update_playback(self, playback):
        """Set the current playback state.
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param playback: playback mode (see constants)
        
        @attention: Do not overwrite!
        """
        
        self.__state.set_playback(playback)
        self.__trigger_sync(self.__sync_state)
    
    def update_repeat_mode(self, repeat):
        """Set the current repeat mode. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param repeat: true means on, false means off
        
        @attention: Do not overwrite!
        """
        
        self.__state.set_repeat(repeat)
        self.__trigger_sync(self.__sync_state)
    
    def update_shuffle_mode(self, shuffle):
        """Set the current shuffle mode. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param shuffle: true means on, false means off
        
        @attention: Do not overwrite!
        """
        
        self.__state.set_shuffle(shuffle)
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
        
        @attention: Do not overwrite!
        """
        
        self.__plob.set_id(id)
        self.__plob.set_info(info)
        self.__plob.set_img(img)
        self.__trigger_sync(self.__sync_plob)
        
    def update_volume(self, volume):
        """Set the current volume.
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param volume: the volume in percent
         
        @attention: Do not overwrite!
        """
        
        self.__state.set_volume(volume)
        self.__trigger_sync(self.__sync_state)
    
    def reply_playlist_request(self, client, plob_ids, plob_names):
        """Send the reply to a playlist request back to the client.
        
        @param client: the client to reply to
        @param plob_ids: IDs of the PLOBs contained in the playlist
        @param plob_names: names of the PLOBs contained in the playlist
        
        @see: request_playlist()        
        """ 
        
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
        
        plob = Plob()
        plob.set_id(id)
        plob.set_info(info)

        msg = net.build_message(message.MSG_ID_REQ_PLOB, plob)
        
        client.send(msg)
    
    def down(self):
        """Shutdown Remuco.
        
        Disconnects all clients and shuts down the Bluetooth and WiFi server.
        Also ignores any subsequent calls to an update method (e.g.
        update_volume(), ...). 
        
        This method is intended to be called by sub classes of Player (e.g.
        if the music player goes down or the Remuco plugin shall be disabled).

        @attention: Do not overwrite!
        """

        if self.__shutting_down: return
        
        self.__shutting_down = True
        
        for c in self.__clients:
            c.disconnect(remove_from_list=False)
            
        self.__clients = []
        
        if self.__server_bluetooth is not None:
            self.__server_bluetooth.down()
        if self.__server_wifi is not None:
            self.__server_wifi.down()
            
        for id in self.__sync_trigger_ids.values():
            if id is not None:
                gobject.source_remove(id)
                
        self.__sync_trigger_ids = {}
        
        #if self.__lfh is not None:
        #    self.__lfh.close()
        
    #==========================================================================
    # synchronization (outbound communication)
    #==========================================================================
    
    def __trigger_sync(self, sync_fn):
        
        if self.__shutting_down:
            return
        
        try:
            id = self.__sync_trigger_ids[sync_fn]
        except KeyError:
            id = None
        
        if id is not None:
            log.debug("trigger for %s already active" % sync_fn.func_name)
            return
        
        self.__sync_trigger_ids[sync_fn] = \
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
        
        self.__sync_trigger_ids[calling_sync_fn] = None
        
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
        elif cmd == command.CMD_SEEK_FWD:
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
        
        self.request_plob(ss.get(), client)
        
    def __handle_message_request_list(self, bindata, client):

        ss = SerialString()
        
        ok = serial.unpack(ss, bindata)
        if not ok: return
        
        path = ss.get().split("/")
        
        if path == Library.PATH_PLAYLIST:
            self.request_playlist(client)
        elif path == Library.PATH_QUEUE:
            self.request_queue(client)
        else:
            self.request_library(client, path)
            
    #==========================================================================
    # miscellaneous 
    #==========================================================================
    
    def get_name(self):
        """ Get the name of the player.
        
        Probably not that useful for player adapters (used internally).
        
        @attention: Do not overwrite!
        """
        return self.__name
    
    def get_clients(self):
        """ Get a descriptive list of connected clients.
        
        This method may be called by sub classes of Player to integrate
        connected clients in a UI.

        @return: a list of client names (or addresses)

        @attention: Do not overwrite!
        """ 
        
        l = []
        for c in self.__clients:
            l.append(c.get_address())
        return l
    
    def __shutdown_system(self):
        
        shutdown_cmd = self.__config.get_shutdown_system_command()
        if shutdown_cmd is not None:
            log.debug("run shutdown command")
            try:
                subprocess.Popen(shutdown_cmd, shell=True)
            except OSError, e:
                log.warning("failed to run shutdown command (%s)", e)
                return
            self.down()

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
             
        log.debug("features: %X" % flags)
        
        return flags
