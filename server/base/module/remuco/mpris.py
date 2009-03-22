import dbus
import dbus.exceptions
import gobject

from remuco.adapter import PlayerAdapter
from remuco.defs import *
from remuco import log

# =============================================================================
# MPRIS constants
# =============================================================================

CAN_GO_NEXT          = 1 << 0
CAN_GO_PREV          = 1 << 1
CAN_PAUSE            = 1 << 2
CAN_PLAY             = 1 << 3
CAN_SEEK             = 1 << 4
CAN_PROVIDE_METADATA = 1 << 5

STATUS_PLAYING = 0
STATUS_PAUSED  = 1
STATUS_STOPPED = 2

MINFO_KEY_RATING = "rating"

# =============================================================================
# player adapter
# =============================================================================

class MPRISAdapter(PlayerAdapter):
    
    def __init__(self, name, display_name=None, poll=2.5, mime_types=None,
                 rating=False):
        
        display_name = display_name or name
            
        if rating:
            max_rating = 5
        else:
            max_rating = 0
            
        PlayerAdapter.__init__(self, display_name,
                               max_rating=max_rating,
                               playback_known=True,
                               volume_known=True,
                               repeat_known=True,
                               shuffle_known=True,
                               progress_known=True,
                               mime_types=mime_types)
        
        self.__name = name
        
        self.__dbus_signal_handler = ()
        
        self.__repeat = False
        self.__shuffle = False
        self.__tracklist_len = 0
        self.__volume = 0
        self.__progress_now = 0
        self.__progress_max = 0
        self.__can_pause = False
        self.__can_play = False
        self.__can_seek = False
        self.__can_next = False
        self.__can_prev = False
        
        log.debug("init done")

    def start(self):
        
        PlayerAdapter.start(self)
        
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
            self.__mp_player.connect_to_signal("CapsChange",
                                               self.__notify_caps),
            self.__mp_tracklist.connect_to_signal("TrackListChange",
                                                  self.__notify_tracklist_change),
        )
        
        self.__mp_player.GetStatus(reply_handler=self.__notify_status,
                                   error_handler=self.__dbus_error)
        
        self.__mp_player.GetMetadata(reply_handler=self.__notify_track,
                                     error_handler=self.__dbus_error)

        self.__mp_player.GetCaps(reply_handler=self.__notify_caps,
                                 error_handler=self.__dbus_error)

        self.__mp_tracklist.GetLength(reply_handler=self.__notify_tracklist_len,
                                      error_handler=self.__dbus_error)
        
    def stop(self):
        
        PlayerAdapter.stop(self)
        
        for handler in self.__dbus_signal_handler:
            handler.remove()
            
        self.__dbus_signal_handler = ()
        
        self.__mp_player = None
        self.__mp_tracklist = None
        
        self.__tracklist_len = 0

    def poll(self):
        
        log.debug("poll")
        
        self.__poll_volume()
        self.__poll_progress()
        
    def ctrl_toggle_playing(self):
        
        self.__mp_player.Pause(reply_handler=self.__dbus_ignore,
                               error_handler=self.__dbus_error)
    
    def ctrl_toggle_repeat(self):
        
        self.__mp_tracklist.SetLoop(not self.__repeat,
            reply_handler=self.__dbus_ignore, error_handler=self.__dbus_error)
    
    def ctrl_toggle_shuffle(self):
        
        self.__mp_tracklist.SetRandom(not self.__shuffle,
            reply_handler=self.__dbus_ignore, error_handler=self.__dbus_error)
        
    def ctrl_next(self):
        
        if not self.__can_next:
            log.debug("go to next item is currently not possible")
            return
        
        self.__mp_player.Next(reply_handler=self.__dbus_ignore,
                              error_handler=self.__dbus_error)
    
    def ctrl_previous(self):

        if not self.__can_prev:
            log.debug("go to previous is currently not possible")
            return
        
        self.__mp_player.Prev(reply_handler=self.__dbus_ignore,
                              error_handler=self.__dbus_error)
        
    def ctrl_volume(self, direction):
        
        if direction == 0:
            volume = 0
        else:
            volume = self.__volume + 5 * direction
            volume = min(volume, 100)
            volume = max(volume, 0)
            
        self.__mp_player.VolumeSet(volume,
            reply_handler=self.__dbus_ignore, error_handler=self.__dbus_error)
        
        gobject.idle_add(self.__poll_volume)
        
    def ctrl_seek(self, direction):
        
        if not self.__can_seek:
            log.debug("seeking is currently not possible")
            return
        
        self.__progress_now += 5000 * direction
        self.__progress_now = min(self.__progress_now, self.__progress_max)
        self.__progress_now = max(self.__progress_now, 0)
        
        log.debug("new progress: %d" % self.__progress_now)
        
        self.__mp_player.PositionSet(self.__progress_now,
            reply_handler=self.__dbus_ignore, error_handler=self.__dbus_error)
        
        gobject.idle_add(self.__poll_progress)

    def request_playlist(self, client):
        
        # example: 2 tracks in the playlist
        self.reply_playlist_request(client, ["1", "2"],
                                    ["Joe - Joe's Song", "Sue - Sue's Song"])

    def request_queue(self, client):
        
        # example: empty queue
        self.reply_queue_request(client, [], [])

    def _poll_status(self):
        """Poll player status information.
        
        Some MPRIS players to not notify about all status changes, so that
        a poll might be necessary.
        
        """
        self.__mp_player.GetStatus(reply_handler=self.__notify_status,
                                   error_handler=self.__dbus_error)
        
    def __poll_volume(self):
        
        self.__mp_player.VolumeGet(reply_handler=self.__notify_volume,
                                   error_handler=self.__dbus_error)
        
    def __poll_progress(self):
        
        self.__mp_player.PositionGet(reply_handler=self.__notify_progress,
                                     error_handler=self.__dbus_error)
            
    def __notify_track(self, track):
        
        log.debug("track: %s" % str(track))
    
        id = track.get("location", "there must be a location")
        
        info = {}
        info[INFO_ARTIST] = track.get("artist", "")
        info[INFO_ALBUM] = track.get("album", "")
        info[INFO_TITLE] = track.get("title", "")
        info[INFO_GENRE] = track.get("genre", "")
        info[INFO_YEAR] = track.get("year", "")
        info[INFO_LENGTH] = track.get("time", 0)
        info[INFO_RATING] = track.get("rating", 0)
        
        self.__progress_max = track.get("mtime", 0) # for update_progress()
    
        img = track.get("arturl")
        if not img or not img.startswith("file:"):
            img = self.find_image(id)
    
        self.update_item(id, info, img)
    
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
            
        if playing == STATUS_PLAYING:
            self.update_playback(PLAYBACK_PLAY)
        elif playing == STATUS_PAUSED:
            self.update_playback(PLAYBACK_PAUSE)
        else:
            self.update_playback(PLAYBACK_STOP)
        
        self.__shuffle = shuffle > 0 # remember for toggle_shuffle()
        self.__repeat = repeat_one > 0 or repeat_all > 0 # for toggle_repeat()
        
        self.update_shuffle(self.__shuffle)
        self.update_repeat(self.__repeat)
        
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
        
        self.__volume = volume # remember for ctrl_volume()
        self.update_volume(volume)
        
    def __notify_progress(self, progress):
        
        self.__progress_now = progress # remember for ctrl_seek()
        progress = progress // 1000
        length = self.__progress_max // 1000
        self.update_progress(progress, length)
    
    def __notify_caps(self, caps):
        
        self.__can_play = caps & CAN_PLAY != 0
        self.__can_pause = caps & CAN_PAUSE != 0
        self.__can_next = caps & CAN_GO_NEXT != 0
        self.__can_prev = caps & CAN_GO_PREV != 0
        self.__can_seek = caps & CAN_SEEK != 0
    
    def __dbus_error(self, error):
        """ DBus error handler."""
        
        if self.__mp_player is None:
            return # do not log errors when not stopped already
        
        log.warning("DBus error: %s" % error)
        
    def __dbus_ignore(self):
        """ DBus reply handler for methods without reply."""
        
        pass
        
    
