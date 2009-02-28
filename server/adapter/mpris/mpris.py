#!/usr/bin/python

import sys # for command line arguments

import dbus
import dbus.exceptions

import remuco
from remuco import log

class MPRISAdapter(remuco.PlayerAdapter):
    
    CAN_GO_NEXT          = 1 << 0
    CAN_GO_PREV          = 1 << 1
    CAN_PAUSE            = 1 << 2
    CAN_PLAY             = 1 << 3
    CAN_SEEK             = 1 << 4
    CAN_PROVIDE_METADATA = 1 << 5

    STATUS_PLAYING = 0
    STATUS_PAUSED  = 1
    STATUS_STOPPED = 2

    def __init__(self, name, display_name=None):
        
        if display_name is None:
            display_name = name
            
        remuco.PlayerAdapter.__init__(self, display_name)
        
        self.__name = name
        
        self.__dbus_signal_handler = ()
        
        self.__tracklist_len = 0
        
        log.debug("init done")

    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        bus = dbus.SessionBus()

        proxy = bus.get_object("org.mpris.%s" % self.__name, "/Player")
        self.__mp_player = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")

        proxy = bus.get_object("org.mpris.%s" % self.__name, "/TrackList")
        self.__mp_tracklist = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")

        self.__dbus_signal_handler = (
            self.__mp_player.connect_to_signal("TrackChange",
                                               self.__notify_track),
            self.__mp_player.connect_to_signal("StatusChange",
                                               self.__notify_status),
            self.__mp_tracklist.connect_to_signal("TrackListChange",
                                                  self.__notify_tracklist_change),
        )
        
        self.__mp_player.GetStatus(reply_handler=self.__notify_status,
                                   error_handler=self.__dbus_error)
        
        self.__mp_player.GetMetadata(reply_handler=self.__notify_track,
                                     error_handler=self.__dbus_error)

        self.__mp_tracklist.GetLength(reply_handler=self.__notify_tracklist_len,
                                      error_handler=self.__dbus_error)
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        for handler in self.__dbus_signal_handler:
            handler.remove()
            
        self.__dbus_signal_handler = ()
        
        self.__mp_player = None
        self.__mp_tracklist = None
        
        self.__tracklist_len = 0

    def poll(self):
        
        log.debug("poll")
        
        self.__mp_player.VolumeGet(reply_handler=self.__notify_volume,
                                   error_handler=self.__dbus_error)
        
        if self.__name == "amarok":
            # amarok does not signal change in shuffle state
            self.__mp_player.GetStatus(reply_handler=self.__notify_status,
                                       error_handler=self.__dbus_error)
        
        return True
    
    def request_playlist(self, client):
        
        # example: 2 tracks in the playlist
        self.reply_playlist_request(client, ["1", "2"],
                                    ["Joe - Joe's Song", "Sue - Sue's Song"])

    def request_queue(self, client):
        
        # example: empty queue
        self.reply_queue_request(client, [], [])

    def __notify_track(self, meta):
        
        log.debug("track: %s" % str(meta))
    
    def __notify_status(self, status):
        
        log.debug("status: %s " % str(status))
        
        playing, shuffle, repeate_one, repeat_all = 0, 0, 0, 0
        
        if isinstance(status, dbus.Struct):
            try:
                playing = status[0]
                shuffle = status[1]
                repeat_one = status[2]
                repeat_all = status[3]
            except IndexError: 
                log.warning("MPRIS status malformed")
                return
        else:
            log.warning("MPRIS status malformed")
            playing = status
            
        if playing == self.STATUS_PLAYING:
            self.update_playback(remuco.PLAYBACK_PLAY)
        elif playing == self.STATUS_PAUSED:
            self.update_playback(remuco.PLAYBACK_PAUSE)
        else:
            self.update_playback(remuco.PLAYBACK_STOP)
        
        self.update_shuffle(shuffle > 0)
        self.update_repeat(repeat_one > 0 or repeat_all > 0)
        
    def __notify_tracklist_change(self, new_len):
        
        log.debug("tracklist change")
        
        self.__notify_tracklist_len(new_len)
    
    def __notify_tracklist_len(self, len):
        
        log.debug("tracklist len: %d" % len)
        
        self.__tracklist_len = len
        
        if len == 0:
            self.__notify_tracklist_pos(0)
        else:
            self.__mp_tracklist.GetCurrentTrack(
                                    reply_handler=self.__notify_tracklist_pos,
                                    error_handler=self.__dbus_error)

    def __notify_tracklist_pos(self, position):
        
        log.debug("tracklist pos: %d" % position)
        
        self.update_position(position)
    
    def __notify_volume(self, volume):
        
        self.update_volume(volume)
        
    def __dbus_error(self, error):
        """ DBus error handler. """
        
        if self.__mp_player is None:
            return # do not log errors when not stopped already
        
        log.warning("dbus error (%s)" % error)
        
    def __dbus_ignore(self):
        """ DBus reply handler for methods without reply. """
        
        pass
        
    
    

# =============================================================================
# main
# =============================================================================

def usage():
    
    print "Usage: remuco-mpris DBUS_NAME [NICE_NAME]"
    print "Example: remuco-mpris amarok Amarok"
    
    print "Currently running players with MPRIS support:"
    
    try:
        bus = dbus.SessionBus()
        proxy = bus.get_object("org.freedesktop.DBus", "/org/freedesktop/DBus")
        fdo = dbus.Interface(proxy, "org.freedesktop.DBus")
        list = fdo.ListNames()
    except dbus.exceptions.DBusException, e:
        print("dbus error: %s" % e.message)
        return
    
    for name in list:
        if name.startswith("org.mpris."):
            print("* %s" % name[10:])


if __name__ == '__main__':
    
    if len(sys.argv) >= 2:
        name = sys.argv[1]
        if len(sys.argv) >= 3:
            pa = MPRISAdapter(name, display_name=sys.argv[2])
        else:
            pa = MPRISAdapter(name)
        mg = remuco.Manager(pa, player_dbus_name="org.mpris.%s" % name)
        mg.run()
    else:
        usage()
