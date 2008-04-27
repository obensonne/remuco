#!/usr/bin/python
# -*- coding: UTF-8 -*-

###############################################################################
#
# Imports
#
###############################################################################

import gobject
import dbus
import dbus.service
from dbus.mainloop.glib import DBusGMainLoop
from dbus.exceptions import DBusException
import traceback
import sys

###############################################################################
#
# Constants defined by Remuco Server-PP protocol
#
###############################################################################

PLAYER = "Foop" # TODO adjust

SERVER_PP_PROTO_VERSION = 1

DBUS_SERVER_SERVICE = "net.sf.remuco.Server"
DBUS_SERVER_PATH = "/net/sf/remuco/Server"
DBUS_SERVER_IFACE = "net.sf.remuco.Server"

DBUS_PP_SERVICE = "net.sf.remuco.%s" % PLAYER
DBUS_PP_PATH = "/net/sf/remuco/%s" % PLAYER
DBUS_PP_IFACE = "net.sf.remuco.%s" % PLAYER

SERVER_ERR_INVALID_DATA = "rem_server_invalid_data"
SERVER_ERR_VERSION_MISMATCH = "rem_server_version_mismatch"
SERVER_ERR_UNKNOWN_PLAYER = "rem_server_unknown_player"

PLAYER_FLAGS = 0
PLAYER_RATING = 0

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
# Foop object (provides Remuco PP DBus interface)
#
###############################################################################

class Foop(dbus.service.Object):

    ###########################################################################
    # Constructor
    ###########################################################################

    def __init__(self):
        
        ###### early DBus initializations ######
        
        DBusGMainLoop(set_as_default=True)
        
        bus = dbus.SessionBus()
        
        dbus.service.Object.__init__(self, bus, DBUS_PP_PATH)

        ###### get a DBus server proxy and do a first check ###### 
        
        server_proxy = bus.get_object(DBUS_SERVER_SERVICE, DBUS_SERVER_PATH,
                                      follow_name_owner_changes=True)
        server = dbus.Interface(server_proxy, DBUS_SERVER_IFACE)
        try:
            server.Check(SERVER_PP_PROTO_VERSION)
        except DBusException, e:
            print >> sys.stderr, "server check failed (%s)" % str(e.message)
            raise
        
        ###### export ourselves as a service ######
        
        dbus_service_name = dbus.service.BusName(DBUS_PP_SERVICE, bus)
        
        ###### say hello to server #####
        
        try:
            server.Hello(PLAYER, PLAYER_FLAGS, PLAYER_RATING)
        except DBusException:
            dbus.service.Object.remove_from_connection(self, path=DBUS_PP_PATH)
            print >> sys.stderr, "server hello failed (%s)" % str(e.message)
            raise
        
        ###### hard work is done :) ######
        
        self.__server = server
        
        self.__dbus_service_name = dbus_service_name

        self.ml = gobject.MainLoop()
        
        self.__cb_id_ml_sync = gobject.timeout_add(3000, self.ml_sync)

    def destroy(self):
        
        print("destroy(%s)" % str(self))
        
        server = self.__server

        if not server: return
        
        self.__server = None
 
        if self.__cb_id_ml_sync > 0:
            gobject.source_remove(self.__cb_id_ml_sync)
 
        try:
            server.Bye(PLAYER)
        except DBusException:
            pass

        try:
            dbus.service.Object.remove_from_connection(self, path=DBUS_PP_PATH)
        except LookupError:
            pass
        
        if self.ml != None:
            self.ml.quit()

    ###########################################################################
    # Remuco PP interface
    ###########################################################################
        
    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='uis', out_signature='')
    def Control(self, command, paramI, paramS):
        
        print("called Control(%u, %i, %s)" % (command, paramI, paramS))

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='a{ss}')
    def RequestPlob(self, id):
        
        print("called RequestPlob(%s)" % id)   
             
        # example reply:
        
        return { PLOB_META_ARTIST : "Some Artist",
                 PLOB_META_TITLE : "Some Title",
                 PLOB_META_ALBUM : "Some Album"
        }

    @dbus.service.method(dbus_interface=DBUS_PP_IFACE,
                         in_signature='s', out_signature='asasasas')
    def RequestPloblist(self, id):
        
        print("called RequestPloblist(%s)" % id)
        
        # example reply:
        
        return ( ["List1", "List2"], ["A nested list", "Another nested list"],
                 ["Plob1", "PlobX"], ["A plob", "One more plob"] )

    ###########################################################################
    # Synchronization with server
    ###########################################################################

    def ml_sync(self):
        """MainLoop callback to periodically sync player state with server."""
        
        print ("syncing")
        
        ok = True
        
        ok = self.__sync_state()
        if not ok: return False
        
        ok = self.__sync_plob()
        if not ok: return False

        ok = self.__sync_playlist()
        if not ok: return False

        ok = self.__sync_queue()
        if not ok: return False
        
        return True
        
    def __sync_state(self):
        
        # example:
        
        playback = PLAYBACK_PAUSE
        volume = 55
        repeat = False
        shuffle = True
        position = 2 # starting from 1 (negative means position in queue)

        try:
            self.__server.UpdateState(PLAYER,
                playback, volume, repeat, shuffle, position)
        except DBusException, e:
            name = e.get_dbus_name()
            msg = e.message
            print ("server: %s (%s)" % (dir(self.__server), str(self.__server)))
            
            print("failed to talk to server (%s)" % str(e.message))
            print(dir(e))
            print(str(e.args))
            print(str(e.get_dbus_name()))
            self.destroy()
            return False
        
        return True

    def __sync_plob(self):
        
        # example:
        
        id = "15"
        img_file = "" # or a pass to an image file
        meta = {
            PLOB_META_ARTIST : "System of a Down",
            PLOB_META_TITLE : "War",
            PLOB_META_ALBUM: "Аа Бб Вв" 
            # ...
        }
        
        try:
            self.__server.UpdatePlob(PLAYER, id, img_file, meta)
        except DBusException, e:
            print("failed to talk to server (%s)" % str(e.message))
            self.destroy()
            return False
        
        return True

    def __sync_playlist(self):
        
        # example:

        ids = ["15", "234"]
        names = ["Moloko - Bring it Back", "System of a Down - War" ]
        
        try:
            self.__server.UpdatePlaylist(PLAYER, ids, names)
        except DBusException, e:
            print("failed to talk to server (%s)" % str(e.message))
            self.destroy()
            return False
        
        return True

    def __sync_queue(self):
        
        # example:

        ids = ["123"]
        names = ["Adam Green - Friends of Mine"]
        
        try:
            self.__server.UpdateQueue(PLAYER, ids, names)
        except DBusException, e:
            print("failed to talk to server (%s)" % str(e.message))
            self.destroy()
            return False
        
        return True
        
    ###########################################################################
    # Misc
    ###########################################################################

###############################################################################
#
# Main
#
###############################################################################

def main():
    
    try:
        foop = Foop()
    except:
        traceback.print_exc()
        return
    
    print ("here we go ..")
    
    foop.ml.run()
    
    print ("done")
    
if __name__ == "__main__":
    
    main()
    
    print("ok")
    