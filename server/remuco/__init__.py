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

from data import PlayerState, Playlist, Control, SimplePlaylist, Plob, \
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

PLID_PLAYLIST = "__PLAYLIST__";
PLID_QUEUE = "__QUEUE__";

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
    * update_playlist()
    * update_queue()
    * update_queue_mode()
    * update_repeat_mode()
    * update_shuffle_mode()
    * update_plob()
    
    * reply_plob_request()
    * reply_list_request()
    
    The term PLOB means playable object and simply refers to a song, a video
    or whatever you can imagine as a playable object. 

    The following methods should be overwritten by sub classes to implement
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
    
    Not all methods must be overwritten. Some even do not make sense for
    certain players, e.g. get_rating_max() only should be overwritten if the
    media player supports rating.
    
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
        
        self.__clients = []
        
        self.__state = PlayerState()
        self.__plob = Plob()
        self.__playlist = SimplePlaylist()
        self.__queue = SimplePlaylist()
        self.__player_info = PlayerInfo(name, 0, self.get_rating_max())
        
        self.__config = config.Config(self.__name)

        log.set_file(self.__config.get_log_file())
        log.set_level(self.__config.get_log_level())
        
        self.__config.set_custom("dummy", "ymmud") # force a config save
        
        serial.Bin.HOST_ENCODING = self.__config.get_encoding()
        
        if self.__config.get_bluetooth():
            self.__server_bluetooth = net.BluetoothServer(self.__clients,
                    self.__player_info, self.__handle_message_from_client)
        else:
            self.__server_bluetooth = None

        if self.__config.get_wifi():
            self.__server_wifi = net.WifiServer(self.__clients,
                    self.__player_info, self.__handle_message_from_client)        
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
        log.warning("jump_to() not yet implemented")
    
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
        
    def request_plob(self, id, client):
        """ Request information about a specific PLOB. 
        
        @param id: ID of the requested PLOB (string)
        @param client: the requesting client - use reply_plob_request()
                       to send back the requested PLOB
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client request a PLOB from the player.
               
        @see reply_plob_request()
        """
        log.warning("request_plob() not yet implemented")
        
    def request_playlist(self, id, client):
        """ Request contents of a specific playlist from the player's library.
        
        @param id: ID of the requested playlist (string)
        @param client: the requesting client - use reply_list_request()
                       to send back the requested playlist
        
        @note: The meaning of playlist here is something different than in
               update_playlist(). The latter one means the currently active
               playlist while this one means a playlist from the player's media
               library.
        
        @note: This method should be overwritten by sub classes of Player. It
               gets called if a remote client request a playlist from the player.
               
        @see reply_list_request()
        """
        log.warning("request_playlist() not yet implemented")
        
    #==========================================================================
    # player side synchronization
    #==========================================================================    

    def update_play_position(self, position):
        """Set the position of the current PLOB on the current playlist/queue. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param position: starting from zero
        
        @attention: Do not overwrite!
        """
        
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
    
    def update_playlist(self, ids, names):
        """Set current playlist. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param ids: list of the PLOB IDs (IDs must be strings)
        @param names: list of the PLOB names (e.g. 'ARTIST - TITLE')
        
        @attention: Do not overwrite!
        """
        
        self.__playlist.set_ids(ids)
        self.__playlist.set_names(names)
        self.__trigger_sync(self.__sync_playlist)
    
    def update_queue(self, ids, names):
        """Set current queue. 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param ids: list of the PLOB IDs (IDs must be strings)
        @param names: list of the PLOB names (e.g. 'ARTIST - TITLE')
        
        @attention: Do not overwrite!
        """
        
        self.__queue.set_ids(ids)
        self.__queue.set_names(names)        
        self.__trigger_sync(self.__sync_queue)
    
    def update_queue_mode(self, mode):
        """Set the current queue mode 
        
        This method is intended to be called by player adapters to sync player
        state with remote clients.
        
        @param mode: true means playing from queue, false means playlist

        @attention: Do not overwrite!
        """
        
        self.__state.set_queue(mode)
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
    
    def reply_list_request(self, client, id, nested_ids, nested_names, ids, names):
        """Send a reply to a playlist request back to the client.
        
        @param client: the client to reply to
        @param id: ID of the requested playlist
        @param nested_ids: IDs of the playlists nested within the requested playlist
        @param nested_names: names of playlists nested within the requested playlist
        @param ids: IDs of the PLOBs contained in the requested playlist
        @param names: names of the PLOBs contained in the requested playlist
        
        @see: request_playlist()        
        """ 
        
        playlist = Playlist(id, nested_ids, nested_names, ids, names)
        
        msg = net.build_message(message.MSG_ID_REQ_PLOBLIST, playlist)
        
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
    
    def __sync_playlist(self):

        msg = net.build_message(message.MSG_ID_SYN_PLAYLIST, self.__playlist)
        
        self.__sync(msg, self.__sync_playlist)
        
        return False
    
    def __sync_queue(self):

        msg = net.build_message(message.MSG_ID_SYN_QUEUE, self.__queue)
        
        self.__sync(msg, self.__sync_queue)
        
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
            
        elif id == message.MSG_ID_REQ_PLOBLIST:
            
            self.__handle_message_request_playlist(bindata, client)
            
        else:
            log.warning("unsupported message id: %d" % id)
    
    def __handle_message_control(self, bindata):
    
        control = Control()
        
        ok = serial.unpack(control, bindata)    
        if not ok: return
        
        cmd = control.get_cmd()
        param_i = control.get_param_i()
        param_s = control.get_param_s()
        
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
            self.jump_to(param_s, param_i)
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

    def __handle_message_request_plob(self, bindata, client):
    
        ss = SerialString()
        
        ok = serial.unpack(ss, bindata)
        if not ok: return
        
        self.request_plob(ss.get(), client)
        
    def __handle_message_request_playlist(self, bindata, client):

        ss = SerialString()
        
        ok = serial.unpack(ss, bindata)
        if not ok: return
        
        self.request_playlist(ss.get(), client)
            
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

    
