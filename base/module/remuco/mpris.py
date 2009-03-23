import dbus
from dbus.exceptions import DBusException
import gobject

from remuco.adapter import PlayerAdapter, ItemAction
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
CAN_HAS_TRACKLIST    = 1 << 6

STATUS_PLAYING = 0
STATUS_PAUSED  = 1
STATUS_STOPPED = 2

MINFO_KEY_RATING = "rating"

# =============================================================================
# actions
# =============================================================================

IA_APPEND = ItemAction("Append", multiple=True)
IA_APPEND_PLAY = ItemAction("Append and play", multiple=True)
FILE_ACTIONS = (IA_APPEND, IA_APPEND_PLAY)

IA_JUMP = ItemAction("Jump to") # __jump_to() is ambiguous on dynamic playlists  
IA_REMOVE = ItemAction("Remove", multiple=True)
PLAYLIST_ACTIONS = (IA_REMOVE, )

# =============================================================================
# player adapter
# =============================================================================

class MPRISAdapter(PlayerAdapter):
    
    def __init__(self, name, display_name=None, poll=2.5, mime_types=None,
                 rating=False, extra_file_actions=()):
        
        display_name = display_name or name
            
        if rating:
            max_rating = 5
        else:
            max_rating = 0
        
        all_file_actions = FILE_ACTIONS + tuple(extra_file_actions or [])
        
        PlayerAdapter.__init__(self, display_name,
                               max_rating=max_rating,
                               playback_known=True,
                               volume_known=True,
                               repeat_known=True,
                               shuffle_known=True,
                               progress_known=True,
                               file_actions=all_file_actions,
                               mime_types=mime_types)
        
        self.__name = name
        
        self.__dbus_signal_handler = ()
        
        self.__repeat = False
        self.__shuffle = False
        self.__volume = 0
        self.__progress_now = 0
        self.__progress_max = 0
        self.__can_pause = False
        self.__can_play = False
        self.__can_seek = False
        self.__can_next = False
        self.__can_prev = False
        self.__can_tracklist = False
        
        log.debug("init done")

    def start(self):
        
        PlayerAdapter.start(self)
        
        try:
            bus = dbus.SessionBus()
            proxy = bus.get_object("org.mpris.%s" % self.__name, "/Player")
            self.__mp_p = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")
            proxy = bus.get_object("org.mpris.%s" % self.__name, "/TrackList")
            self.__mp_t = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")
        except DBusException, e:
            log.error("dbus error: %s" % e)
            return

        try:
            self.__dbus_signal_handler = (
                self.__mp_p.connect_to_signal("TrackChange",
                                              self.__notify_track),
                self.__mp_p.connect_to_signal("StatusChange",
                                              self.__notify_status),
                self.__mp_p.connect_to_signal("CapsChange",
                                              self.__notify_caps),
                self.__mp_t.connect_to_signal("TrackListChange",
                                              self.__notify_tracklist_change),
            )
        except DBusException, e:
            log.error("dbus error: %s" % e)

        try:
            self.__mp_p.GetStatus(reply_handler=self.__notify_status,
                                  error_handler=self.__dbus_error)
            
            self.__mp_p.GetMetadata(reply_handler=self.__notify_track,
                                    error_handler=self.__dbus_error)
    
            self.__mp_p.GetCaps(reply_handler=self.__notify_caps,
                                error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def stop(self):
        
        PlayerAdapter.stop(self)
        
        for handler in self.__dbus_signal_handler:
            handler.remove()
            
        self.__dbus_signal_handler = ()
        
        self.__mp_p = None
        self.__mp_t = None
        
    def poll(self):
        
        log.debug("poll")
        
        self.__poll_volume()
        self.__poll_progress()
        
    def ctrl_toggle_playing(self):
        
        try:
            self.__mp_p.Pause(reply_handler=self.__dbus_ignore,
                              error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_toggle_repeat(self):
        
        try:
            self.__mp_t.SetLoop(not self.__repeat,
                                reply_handler=self.__dbus_ignore,
                                error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_toggle_shuffle(self):
        
        try:
            self.__mp_t.SetRandom(not self.__shuffle,
                                  reply_handler=self.__dbus_ignore,
                                  error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def ctrl_next(self):
        
        if not self.__can_next:
            log.debug("go to next item is currently not possible")
            return
        
        try:
            self.__mp_p.Next(reply_handler=self.__dbus_ignore,
                            error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_previous(self):

        if not self.__can_prev:
            log.debug("go to previous is currently not possible")
            return
        
        try:
            self.__mp_p.Prev(reply_handler=self.__dbus_ignore,
                            error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def ctrl_volume(self, direction):
        
        if direction == 0:
            volume = 0
        else:
            volume = self.__volume + 5 * direction
            volume = min(volume, 100)
            volume = max(volume, 0)
            
        try:
            self.__mp_p.VolumeSet(volume, reply_handler=self.__dbus_ignore,
                                  error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
        gobject.idle_add(self.__poll_volume)
        
    def ctrl_seek(self, direction):
        
        if not self.__can_seek:
            log.debug("seeking is currently not possible")
            return
        
        self.__progress_now += 5000 * direction
        self.__progress_now = min(self.__progress_now, self.__progress_max)
        self.__progress_now = max(self.__progress_now, 0)
        
        log.debug("new progress: %d" % self.__progress_now)
        
        try:
            self.__mp_p.PositionSet(self.__progress_now,
                                    reply_handler=self.__dbus_ignore,
                                    error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
        gobject.idle_add(self.__poll_progress)

    def action_files(self, action_id, files, uris):
        
        if action_id == IA_APPEND.id or action_id == IA_APPEND_PLAY.id:
            
            try:
                self.__mp_t.AddTrack(uris[0], action_id == IA_APPEND_PLAY.id)
                for uri in uris[1:]:
                    self.__mp_t.AddTrack(uri, False)
            except DBusException, e:
                log.warning("dbus error: %s" % e)
                return
        
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)

    def action_playlist_item(self, action_id, positions, ids):
        
        if action_id == IA_REMOVE.id:
            
            positions.sort()
            positions.reverse()
            try:
                for pos in positions:
                    self.__mp_t.DelTrack(pos)
            except DBusException, e:
                log.warning("dbus error: %s" % e)
                return
        
        elif action_id == IA_JUMP.id:
            
            self.__jump_to(positions[0])
        
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    def request_playlist(self, client):
        
        if not self.__can_tracklist:
            self.reply_playlist_request(client, [], [])
            return
        
        tracks = self.__get_tracklist()

        ids = []
        names = []
        for track in tracks:
            id, info = self.__track2info(track)
            artist = info.get(INFO_ARTIST) or "??"
            title = info.get(INFO_TITLE) or "??"
            name = "%s - %s" % (artist, title)
            ids.append(id)
            names.append(name)
        
        self.reply_playlist_request(client, ids, names,
                                    item_actions=PLAYLIST_ACTIONS)

    def _poll_status(self):
        """Poll player status information.
        
        Some MPRIS players do not notify about all status changes, so that
        status must be polled. Subclasses may call this method for that purpose.
        
        """
        try:
            self.__mp_p.GetStatus(reply_handler=self.__notify_status,
                                  error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def __poll_volume(self):
        
        try:
            self.__mp_p.VolumeGet(reply_handler=self.__notify_volume,
                                  error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def __poll_progress(self):
        
        try:
            self.__mp_p.PositionGet(reply_handler=self.__notify_progress,
                                    error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            
    def __notify_track(self, track):
        
        log.debug("track: %s" % str(track))
    
        id, info = self.__track2info(track)
        
        self.__progress_max = track.get("mtime", 0) # for update_progress()
    
        img = track.get("arturl")
        if not img or not img.startswith("file:"):
            img = self.find_image(id)
    
        self.update_item(id, info, img)
        
        try:
            self.__mp_t.GetCurrentTrack(reply_handler=self.__notify_position,
                                        error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            
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
        try:
            self.__mp_t.GetCurrentTrack(reply_handler=self.__notify_position,
                                        error_handler=self.__dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def __notify_position(self, position):
        
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
        self.__can_tracklist = caps & CAN_HAS_TRACKLIST != 0
    
    def __get_tracklist(self):
        """Get a list of track dicts of all tracks in the tracklist."""
        
        try:
            len = self.__mp_t.GetLength()
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            len = 0
        
        if len == 0:
            return []
        
        tracks = []
        for i in range(0, len):
            try:
                tracks.append(self.__mp_t.GetMetadata(i))
            except DBusException, e:
                log.warning("dbus error: %s" % e)
                return []
            
        return tracks

    def __track2info(self, track):
        """Convert an MPRIS meta data dict to a Remuco info dict."""
        
        id = track.get("location", "there must be a location")
        
        info = {}
        info[INFO_ARTIST] = track.get("artist", "")
        info[INFO_ALBUM] = track.get("album", "")
        info[INFO_TITLE] = track.get("title", "")
        info[INFO_GENRE] = track.get("genre", "")
        info[INFO_YEAR] = track.get("year", "")
        info[INFO_LENGTH] = track.get("time", 0)
        info[INFO_RATING] = track.get("rating", 0)
        
        return (id, info)
    
    def __jump_to(self, position):
        """Jump to a position in the tracklist.
        
        MPRIS has no such method and this a workaround. Unfortunately if
        behaves not as expected on dynamic playlists.
        
        """
        tracks = self.__get_tracklist()
        
        if position >= len(tracks):
            return
        
        uris = []
        for track in tracks[position:]:
            uris.append(track.get("location", "there must be a location"))
        
        positions = range(position, len(tracks))
        
        self.action_playlist_item(IA_REMOVE.id, positions, uris)
        
        self.action_files(IA_APPEND_PLAY.id, [], uris)
    
    def __dbus_error(self, error):
        """ DBus error handler."""
        
        if self.__mp_p is None:
            return # do not log errors when not stopped already
        
        log.warning("DBus error: %s" % error)
        
    def __dbus_ignore(self):
        """ DBus reply handler for methods without reply."""
        
        pass
        
    
