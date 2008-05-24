#!/usr/bin/python
# -*- coding: UTF-8 -*-

###############################################################################
#
# Imports
#
###############################################################################

from dbus.exceptions import DBusException
from dbus.mainloop.glib import DBusGMainLoop
import dbus
import dbus.service
import gobject
import os
import signal
import traceback

###############################################################################

PLAYER = "Foop"

###############################################################################
#
# Constants related to XMMS2
#
###############################################################################

# --- ADJUST ---
# We assume in Foop it is possible to rate songs with a mximum rating of 5.
# If your player does not support rating, set this to 0. 
RATING_MAX = 5

###############################################################################
#
# Constants defined by Remuco Server-PP protocol
# 
# The protocol is described in detail at:
# http://remuco.sourceforge.net/index.php/Server_-_Player_Proxy_-_Protocol
#
###############################################################################

# The server-pp protocol version. The version is used late to ensure server
# and player proxy have compatible versions. 
SERVER_PP_PROTO_VERSION = 2

# Following are the D-Bus names we need to contact the server.

# These are the names to connect to the server's shell service. The shell
# service is used to start up the server. 
DBUS_SHELL_SERVICE = "net.sf.remuco.Shell"
DBUS_SHELL_PATH = "/net/sf/remuco/Shell"
DBUS_SHELL_IFACE = "net.sf.remuco.Shell"

# These are the names to connect to the server's server service. The server
# service is used to register and unregister our proxy at the server and to
# provide the server with up-to-date information about Foop. 
DBUS_SERVER_SERVICE = "net.sf.remuco.Server"
DBUS_SERVER_PATH = "/net/sf/remuco/Server"
DBUS_SERVER_IFACE = "net.sf.remuco.Server"

# These are the names the server can use to connect to our proxy. This happens
# when the server issues player control commands or when it request certain
# information about Foop. 
DBUS_PP_SERVICE = "net.sf.remuco.%s" % PLAYER
DBUS_PP_PATH = "/net/sf/remuco/%s" % PLAYER
DBUS_PP_IFACE = "net.sf.remuco.%s" % PLAYER

# These are the names of some D-Bus errors we need to detect.
DBUS_ERR_NO_SERVICE = "org.freedesktop.DBus.Error.ServiceUnknown"
DBUS_ERR_NO_REPLY = "org.freedesktop.DBus.Error.NoReply"

# Below are some further constants defined by the server-pp protocol. You'll
# understand their meanind later when we use these constants.

# Well known playlist IDs:
PLAYLIST_ID = "__PLAYLIST__"
QUEUE_ID = "__QUEUE__"

# Playback state codes:
PLAYBACK_STOP = 0
PLAYBACK_PAUSE = 1
PLAYBACK_PLAY = 2

# Keys for plob meta information:
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

# Control command codes:
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

# The next lines detect if we should log in debug or in normal mode.

_home_dir = os.getenv("HOME", "/var/tmp")
_config_dir = os.getenv("XDG_CONFIG_HOME", "%s/.config" % _home_dir)

_log_debug_file = "%s/remuco/debug" % _config_dir # indicates log level

if os.path.isfile(_log_debug_file):
    print("debug log is enabled")
    LOG_DEBUG = True
else:
    print("debug log is disabled")
    LOG_DEBUG = False

###############################################################################

# Now some rather simple logging functions:

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
# Player proxy class (provides Remuco PP DBus interface)
#
###############################################################################

# To enable the server to talk to us, we must make our proxy accessible via
# D-Bus. The following class implements the methods a player proxy has to
# provide for the server. We will register an object of this class at D-Bus
# and then some methods of the class can be called by the server using D-Bus.

class PP(dbus.service.Object):

    def __init__(self):
        
        # The init functions prepares the object to be exportable via D-Bus.
        # Further it initializes some variables we will use later.

        ###### init dbus ######

        DBusGMainLoop(set_as_default=True)

        dbus.service.Object.__init__(self, None, None)

        ###### init vars ######

        # These variables describe the current state of Foop.
        self.__state_playback = PLAYBACK_STOP
        self.__state_volume = 0
        self.__state_repeat = False
        self.__state_shuffle = False
        self.__state_position = 0
        self.__state_queue = False
         
        # These variables describe the plob currently played by Foop.
        self.__plob_id = None
        self.__plob_meta = None
        
        # These variables are used to hold the IDs and names of the plobs
        # contained in Foop's currently active playlist.
        self.__playlist_ids = None
        self.__playlist_names = None
        
        # These variables are used to hold the IDs and names of the plobs
        # contained in Foop's queue.
        self.__queue_ids = None
        self.__queue_names = None

        # These flags will be explained later some methods below.
        self.__fast_state_check_triggered = False
        self.__fast_plob_check_triggered = False
        self.__fast_playlist_check_triggered = False
        
        # --- ADJUST ---
        # You may want to add further initializations needed for your player.

    ###########################################################################
    
    # The following methods are the methods which may be called by the server
    # via D-Bus.
        
    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='uis', out_signature='')
    def Control(self, command, paramI, paramS):
        
        # This method is called by the server to control Foop. This happens
        # if a client issued a certain control command. The first parameter
        # specifies the control to execute and the parameters 'paramI' and
        # 'paramS' hold command specific additional information.
        
        # More information about this method at:
        # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#Control
        
        log_debug("called Control(%u, %i, %s)" % (command, paramI, paramS))

        if command == CTL_STOP:
            
            # --- ADJUST ---
            # Tell Foop to stop playback.
            log_msg("not implemented")
            
        elif command == CTL_PLAYPAUSE: ########################################
            
            # --- ADJUST ---
            # Tell Foop to toggle play/pause.
            log_msg("not implemented")
            
        elif command == CTL_NEXT: #############################################
            
            # --- ADJUST ---
            # Tell Foop to switch to the next song, video or whatever.
            log_msg("not implemented")
                        
        elif command == CTL_PREV: #############################################
            
            # --- ADJUST ---
            # Tell Foop to switch to the previous song, video or whatever.
            log_msg("not implemented")
            
        elif command == CTL_SEEK_FWD: #########################################
            
            # --- ADJUST ---
            # Tell Foop to the seek forward some seconds. For a song
            # something like 5 seconds is a good idea. For a video some greater
            # seek steps may be better, e.g. 30 seconds.
            log_msg("not implemented")
            
        elif command == CTL_SEEK_BWD: #########################################
            
            # --- ADJUST ---
            # Tell Foop to the seek backward some seconds. For a song
            # something like 5 seconds is a good idea. For a video some greater
            # seek steps may be better, e.g. 30 seconds.
            log_msg("not implemented")
            
        elif command == CTL_VOLUME: ###########################################
            
            # --- ADJUST ---
            # Tell Foop to set the volume to the value specified in 'paramI'.
            log_msg("not implemented")
            
        elif command == CTL_JUMP: #############################################
            
            # --- ADJUST ---
            # Tell Foop to jump to a specific plob within a specifc ploblist.
            
            if paramS != PLAYLIST_ID:
                # Tell Foop to play plob number 'paramI' in the current playlist.
                log_msg("not implemented")
            elif paramS != QUEUE_ID:
                # Tell Foop to play plob number 'paramI' in the queue.
                log_msg("not implemented")
            else:
                # Tell Foop to replace the current playlist by the ploblist
                # specified by 'paramS' and then to jump to the position
                # 'paramI'.
                # The ploblist specified by 'paramS' is an ID we previously
                # supplied to the server when the server called our method
                # RequestPloblist(..).
                log_msg("not implemented")

        elif command == CTL_RATE: #############################################
            
            # --- ADJUST ---
            # Tell Foop to rate the currently played plob with the value
            # specified in 'paramI'.
            log_msg("not implemented")
            
        elif command == CTL_REPEAT: ##########################################
            
            # --- ADJUST ---
            # Tell Foop to toggle repeat mode.
            log_msg("not implemented")
            
        elif command == CTL_SHUFFLE: ##########################################
            
            # --- ADJUST ---
            # Tell Foop to toggle shuffle mode.
            log_msg("not implemented")
            
        elif command == CTL_IGNORE: ###########################################
            
            # Simply ignore this.
            pass

        else: #################################################################
            
            # Ehm ..
            log_msg("command %d not supported", cmd)

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='a{ss}')
    def RequestPlob(self, id):
        
        # This method is called by the server if a client requested detailed
        # information about a plob. Parameter 'id' is the ID of the plob.

        # More information about this method at:
        # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#RequestPlob
        
        log_debug("called RequestPlob(%s)" % id)   

        # --- ADJUST ---
        # Get some meta information about the specified plob from Foop.
        
        # The following returns some some dummy data to the server:
        
        meta = { PLOB_META_ARTIST : "John Doe",
                 PLOB_META_TITLE : "Doe's Song",
                 PLOB_META_RATING : "3" }
             
        return meta 

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='asasasas')
    def RequestPloblist(self, id):
        
        # This method is called by the server if a client requests the contents
        # of a ploblist. Parameter 'id' is the ID of the ploblist. If 'id'
        # is the empty string, the root of your player's library is requested,
        # that means all top level ploblists.

        # More information about this method at:
        # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#RequestPloblist
        
        log_debug("called RequestPloblist(%s)" % id)
        
        # --- ADJUST ---
        # Get the contents of the specified ploblist from Foo.

        # The following returns some dummy data to the server:
        
        if id == "":
            # The root of your player's media library is requested. This means
            # all top level ploblists.
            nested_ids = ["TopLevelList1", "TopLevelList2", "TopLevelList3" ]
            nested_names = ["Radio Stations", "Genres", "Static Playlists"]
            # Usually there no plobs in the root of the player's library.
            # So in most cases the following is o.k. as it is.
            ids = []
            names = []
        else:
            if id == "TopLevelList1":
                # Example: no nested playlists in this playlist:
                nested_ids = []
                nested_names = []
                # Example: some radio stations
                ids = [ "Radio1", "Radio2" ]
                names = [ "Foo FM", "Dub FM" ]
            elif id == "TopLevelList2":
                # Example: a nested playlist for each genre
                nested_ids = [ "TopLevelList2:Funk", "TopLevelList2:Jazz" ]
                nested_names = [ "Funk", "Jazz" ]
                # Example: no plobs in this playlist.
                ids = []
                names = []
            else:
                # .. and so on
                # if cannot provide content of a ploblist:
                nested_ids = []
                nested_names = []
                ids = []
                names = []
            
        return nested_ids, nested_names, ids, names
            
    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='', out_signature='')
    def Bye(self):

        # This method is called by the server if the server shuts down or if
        # a user explicitly selected this player proxy to stop.

        # More information about this method at:
        # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#Bye_2

        log_msg("server said bye")
        
        # Stop the main loop:
        self.__ml.quit()
        
        # This will cause the call to self.__ml.run() we've done in the method
        # PP.run() to return. Any cleanup is done in method PP.run() after the
        # call self.__ml.run().

    ###########################################################################

    def update(self):
        
        # This method gets set up in method PP.run() to be called in certain
        # intervals. Its purpose is to check for changes in Foop and forward
        # new sate information about Foop to the server.
        
        change = False
        
        # --- ADJUST ---
        # Get up-to-date information about Foop's playback state, volume,
        # repeat mode, shuffle mode, current song position and queue mode.

        # Example: get some dummy data from Foop:
        
        st_playback = PLAYBACK_PAUSE
        st_volume = 56 # in percent
        st_repeat = False
        st_shuffle = True
        st_position = 2 # position of current song in playlist/queue
        st_queue = False # currently playing form queue?

        # Check if values have changed:
        
        if self.__state_playback != st_playback:
            change = True 
            self.__state_playback = st_playback

        if self.__state_volume != st_volume:
            change = True 
            self.__state_volume = st_volume
            
        if self.__state_repeat != st_repeat:
            change = True 
            self.__state_repeat = st_repeat
            
        if self.__state_shuffle != st_shuffle:
            change = True 
            self.__state_shuffle = st_shuffle
            
        if self.__state_position != st_position:
            change = True 
            self.__state_position = st_position
            
        if self.__state_queue != st_queue:
            change = True 
            self.__state_queue = st_queue
        
        # If there is a change, forward new data to the server:
        
        if change:
            log_debug("sync state")
            self.__server.UpdateState(PLAYER,
                self.__state_playback, self.__state_volume,
                self.__state_repeat, self.__state_shuffle,
                self.__state_position, self.__state_queue,
                reply_handler = self.__server_reply_normal,
                error_handler = self.__server_reply_error)
            # More information about this server method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#UpdateState

        #----------------------------------------------------------------------
        
        change = False
        
        # --- ADJUST ---
        # Get up-to-date information about Foop's currently played plob.
        
        # Example: get some dummy data from Foop:
        
        plob_id = "123"
        
        if self.__plob_id != plob_id:
            change = True
            self.__plob_id = plob_id 
            plob_img = "" # this may be a path to an image file
            plob_meta = { PLOB_META_ARTIST : "Frank Foo",
                          PLOB_META_TITLE : "Sing a Song",
                          PLOB_META_ALBUM : "Universal Album" }
        
        # If there is a change, forward new data to the server:

        if change:
            log_debug("sync plob")
            self.__server.UpdatePlob(PLAYER,
                plob_id, plob_img, plob_meta,
                reply_handler = self.__server_reply_normal,
                error_handler = self.__server_reply_error)
            # More information about this server method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#UpdatePlob

        #----------------------------------------------------------------------

        change = False

        # --- ADJUST ---
        # Get up-to-date information about Foop's playlist.
        
        # Example: get some dummy data from Foop:
        
        playlist_ids = [ "Song1", "Song2" ]
        playlist_names = [ "Paul - Paul's Song", "Barfoo - Foobar" ]
        
        if self.__playlist_ids != playlist_ids:
            change = True
            self.__playlist_ids = playlist_ids
            self.__playlist_names = playlist_names 
        
        # If there is a change, forward new data to the server:

        if change:
            log_debug("sync playlist")
            self.__server.UpdatePlaylist(PLAYER,
                playlist_ids, playlist_names,
                reply_handler = self.__server_reply_normal,
                error_handler = self.__server_reply_error)
            # More information about this server method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#UpdatePlaylist
        
        # --- ADJUST ---
        # Handle the queue similar to the playlist.
        # You are done if your player has no queue.
        
        return True

    def run(self):
        
        # This method sets up D-Bus, connects to the Remuco server and export
        # this player proxy to D-Bus so that the server can talk to this proxy.
        # Finally it sets up and runs a main loop.

        # Connect to D-Bus:
        
        bus = dbus.SessionBus()
        
        # Start the server:
        
        try:
            # The server provides a service called 'Shell'. We use this service
            # to start the server and check version compatibility between our
            # proxy and the server. Versions incompatibility is caught by the
            # except block below. 
            shell_proxy = bus.get_object(DBUS_SHELL_SERVICE, DBUS_SHELL_PATH)
            shell = dbus.Interface(shell_proxy, DBUS_SHELL_IFACE)
            shell.Start(SERVER_PP_PROTO_VERSION)
            # More information about this shell method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#Start
        except DBusException, e:
            log_exc("failed to connect to Remuco server")
            return False

        # Register Foop at the server:
        
        try:
            # From now on any interaction with the server is done via the server's
            # 'Server' service. We now say hello to the server.
            server_proxy = bus.get_object(DBUS_SERVER_SERVICE, DBUS_SERVER_PATH)
            server = dbus.Interface(server_proxy, DBUS_SERVER_IFACE)
            server.Hello(PLAYER, 0, RATING_MAX)
            # More information about this server method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#Hello
        except DBusException, e:
            log_exc("failed to say hello to Remuco server")
            return False
        
        self.__server = server
        
        # Export this proxy to D-Bus. Once this is done, the server is able to
        # talk to this proxy via the methods above (Control(), RequestPlob(),
        # RequestPloblist() and Bye()).
        
        dbus.service.Object.add_to_connection(self, bus, DBUS_PP_PATH)

        dbus_service_name = dbus.service.BusName(DBUS_PP_SERVICE, bus)
        
        # Set up a main loop. The main loop is used for two pruposes. First it
        # automatically checks if the server wants to talk to this proxy and
        # then calls the appropriate methods above. Second it periodically
        # calls a method we use to check for changes in Foop.

        self.__ml = gobject.MainLoop()

        # Check for changes in Foop every 5 seconds:
        
        gobject.timeout_add(5000, self.update)

        # --- NOTE ---
        # This isn't very smooth. Most players provide notifications about
        # changes in player state. In that case it is not necessary to poll
        # the player for changes like it is done here.

        # Ok, now let's start the main loop.
        
        log_msg("go ..")

        self.__ml.run() # blocks until self.__ml.quit() is called somewhere else
        
        log_msg("shutting down")
        
        # Shutting down -> do some clean up

        try:
            # Say bye to the server:
            server.Bye(PLAYER)
            # More information about this server method at:
            # http://remuco.sf.net/index.php/Server_-_Player_Proxy_-_Protocol#Bye
        except DBusException:
            pass

        try:
            # Unregister this player proxy from D-Bus:
            dbus.service.Object.remove_from_connection(self, path=DBUS_PP_PATH)
        except LookupError:
            pass

        return True
    
    ###########################################################################

    # The following two functions are used to handle a reply when this proxy
    # called a method of the server's 'Server' service.

    def __server_reply_normal(self):
        # nothing to do - all called server methods return nothing
        pass
    
    def __server_reply_error(self, e):
        # 'e' is an instance of DBusException
        if e.get_dbus_name() == DBUS_ERR_NO_REPLY:
            log_msg("no reply from Remuco server (probably busy)")
            return True
        else:
            log_exc("failed to talk to Remuco server")
            self.__ml.quit()
            return False

###############################################################################
#
# Main
#
###############################################################################

pp_global = None

def sighandler(signum, frame):
    
    global pp_global
    
    log_msg("received signal %i" % signum)
    
    if pp_global != None:
        pp_global.down()

if __name__ == "__main__":
    
    signal.signal(signal.SIGINT, sighandler)
    signal.signal(signal.SIGTERM, sighandler)

    pp = PP()
    
    pp_global = pp
    
    ok = pp.run()
    
    if not ok:
        log_msg("Remuco Foop failed")
    else:
        log_msg("Remuco Foop is down")

    