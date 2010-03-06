# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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

import os.path

import gobject

from remuco.adapter import PlayerAdapter, ItemAction
from remuco.defs import *
from remuco import log

try:
    import dbus
    from dbus.exceptions import DBusException
except ImportError:
    log.warning("dbus not available - MPRIS based player adapters will crash")
    
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
PLAYLIST_ACTIONS = [IA_REMOVE]

# =============================================================================
# player adapter
# =============================================================================

class MPRISAdapter(PlayerAdapter):
    
    def __init__(self, name, display_name=None, poll=2.5, mime_types=None,
                 rating=False, extra_file_actions=None,
                 extra_playlist_actions=None):
        
        display_name = display_name or name
            
        if rating:
            max_rating = 5
        else:
            max_rating = 0
        
        all_file_actions = FILE_ACTIONS + tuple(extra_file_actions or ())
            
        PlayerAdapter.__init__(self, display_name,
                               max_rating=max_rating,
                               playback_known=True,
                               volume_known=True,
                               repeat_known=True,
                               shuffle_known=True,
                               progress_known=True,
                               file_actions=all_file_actions,
                               mime_types=mime_types)
        
        self.__playlist_actions = PLAYLIST_ACTIONS
        if self.config.getx("playlist-jump-enabled", "0", int):
            self.__playlist_actions.append(IA_JUMP)
        if extra_playlist_actions:
            self.__playlist_actions.extend(extra_playlist_actions)
         
        self.__name = name
        
        self.__dbus_signal_handler = ()
        self._mp_p = None
        self._mp_t = None
        
        self._repeat = False
        self._shuffle = False
        self._playing = PLAYBACK_STOP
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
            self._mp_p = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")
            proxy = bus.get_object("org.mpris.%s" % self.__name, "/TrackList")
            self._mp_t = dbus.Interface(proxy, "org.freedesktop.MediaPlayer")
        except DBusException, e:
            raise StandardError("dbus error: %s" % e)

        try:
            self.__dbus_signal_handler = (
                self._mp_p.connect_to_signal("TrackChange",
                                             self._notify_track),
                self._mp_p.connect_to_signal("StatusChange",
                                             self._notify_status),
                self._mp_p.connect_to_signal("CapsChange",
                                             self._notify_caps),
                self._mp_t.connect_to_signal("TrackListChange",
                                             self._notify_tracklist_change),
            )
        except DBusException, e:
            raise StandardError("dbus error: %s" % e)

        try:
            self._mp_p.GetStatus(reply_handler=self._notify_status,
                                 error_handler=self._dbus_error)
            
            self._mp_p.GetMetadata(reply_handler=self._notify_track,
                                   error_handler=self._dbus_error)
    
            self._mp_p.GetCaps(reply_handler=self._notify_caps,
                               error_handler=self._dbus_error)
        except DBusException, e:
            # this is not necessarily a fatal error
            log.warning("dbus error: %s" % e)
        
    def stop(self):
        
        PlayerAdapter.stop(self)
        
        for handler in self.__dbus_signal_handler:
            handler.remove()
            
        self.__dbus_signal_handler = ()
        
        self._mp_p = None
        self._mp_t = None
        
    def poll(self):
        
        self._poll_volume()
        self._poll_progress()
        
    # =========================================================================
    # control interface 
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        
        try:
            if self._playing == PLAYBACK_STOP:
                self._mp_p.Play(reply_handler=self._dbus_ignore,
                                error_handler=self._dbus_error)
            else:
                self._mp_p.Pause(reply_handler=self._dbus_ignore,
                                 error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_toggle_repeat(self):
        
        try:
            self._mp_t.SetLoop(not self._repeat,
                               reply_handler=self._dbus_ignore,
                               error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_toggle_shuffle(self):
        
        try:
            self._mp_t.SetRandom(not self._shuffle,
                                 reply_handler=self._dbus_ignore,
                                 error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def ctrl_next(self):
        
        if not self.__can_next:
            log.debug("go to next item is currently not possible")
            return
        
        try:
            self._mp_p.Next(reply_handler=self._dbus_ignore,
                            error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_previous(self):

        if not self.__can_prev:
            log.debug("go to previous is currently not possible")
            return
        
        try:
            self._mp_p.Prev(reply_handler=self._dbus_ignore,
                            error_handler=self._dbus_error)
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
            self._mp_p.VolumeSet(volume, reply_handler=self._dbus_ignore,
                                 error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
        gobject.idle_add(self._poll_volume)
        
    def ctrl_seek(self, direction):
        
        if not self.__can_seek:
            log.debug("seeking is currently not possible")
            return
        
        self.__progress_now += 5 * direction
        self.__progress_now = min(self.__progress_now, self.__progress_max)
        self.__progress_now = max(self.__progress_now, 0)
        
        log.debug("new progress: %d" % self.__progress_now)
        
        try:
            self._mp_p.PositionSet(self.__progress_now * 1000,
                                   reply_handler=self._dbus_ignore,
                                   error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
        gobject.idle_add(self._poll_progress)

    # =========================================================================
    # actions interface
    # =========================================================================
    
    def action_files(self, action_id, files, uris):
        
        if action_id == IA_APPEND.id or action_id == IA_APPEND_PLAY.id:
            
            try:
                self._mp_t.AddTrack(uris[0], action_id == IA_APPEND_PLAY.id)
                for uri in uris[1:]:
                    self._mp_t.AddTrack(uri, False)
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
                    self._mp_t.DelTrack(pos)
            except DBusException, e:
                log.warning("dbus error: %s" % e)
                return
        
        elif action_id == IA_JUMP.id:
            
            self.__jump_to(positions[0])
        
        else:
            log.error("** BUG ** unexpected action: %d" % action_id)
    
    # =========================================================================
    # request interface 
    # =========================================================================
    
    def request_playlist(self, reply):
        
        if not self.__can_tracklist:
            reply.send()
            return
        
        # TODO: very slow for SongBird, should be async
        tracks = self.__get_tracklist()

        for track in tracks:
            id, info = self.__track2info(track)
            artist = info.get(INFO_ARTIST, "???")
            title = info.get(INFO_TITLE, "???")
            name = "%s - %s" % (artist, title)
            reply.ids.append(id)
            reply.names.append(name)
        
        reply.item_actions = self.__playlist_actions
        
        reply.send()

    # =========================================================================
    # internal methods (may be overridden by subclasses to fix MPRIS issues) 
    # =========================================================================
    
    def _poll_status(self):
        """Poll player status information.
        
        Some MPRIS players do not notify about all status changes, so that
        status must be polled. Subclasses may call this method for that purpose.
        
        """
        try:
            self._mp_p.GetStatus(reply_handler=self._notify_status,
                                 error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def _poll_volume(self):
        
        try:
            self._mp_p.VolumeGet(reply_handler=self._notify_volume,
                                 error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def _poll_progress(self):
        
        try:
            self._mp_p.PositionGet(reply_handler=self._notify_progress,
                                   error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            
    def _notify_track(self, track):
        
        log.debug("track: %s" % str(track))
    
        id, info = self.__track2info(track)
        
        self.__progress_max = info.get(INFO_LENGTH, 0) # for update_progress()
    
        img = track.get("arturl")
        if not img or not img.startswith("file:"):
            img = self.find_image(id)
    
        self.update_item(id, info, img)
        
        try:
            self._mp_t.GetCurrentTrack(reply_handler=self._notify_position,
                                       error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            
    def _notify_status(self, status):
        
        log.debug("status: %s " % str(status))
        
        if status[0] == STATUS_STOPPED:
            self._playing = PLAYBACK_STOP
        elif status[0] == STATUS_PAUSED:
            self._playing = PLAYBACK_PAUSE
        elif status[0] == STATUS_PLAYING:
            self._playing = PLAYBACK_PLAY
        else:
            log.warning("unknown play state (%s), assume playing)" % status[0])
            self._playing = PLAYBACK_PLAY
            
        self.update_playback(self._playing)
        
        self._shuffle = status[1] # remember for toggle_shuffle()
        self.update_shuffle(self._shuffle)
        
        self._repeat = status[2] or status[3] # for toggle_repeat()
        self.update_repeat(self._repeat)
        
    def _notify_tracklist_change(self, new_len):
        
        log.debug("tracklist change")
        try:
            self._mp_t.GetCurrentTrack(reply_handler=self._notify_position,
                                       error_handler=self._dbus_error)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
        
    def _notify_position(self, position):
        
        log.debug("tracklist pos: %d" % position)
        
        self.update_position(position)
    
    def _notify_volume(self, volume):
        
        self.__volume = volume # remember for ctrl_volume()
        self.update_volume(volume)
        
    def _notify_progress(self, progress):
        
        self.__progress_now = progress // 1000 # remember for ctrl_seek()
        self.update_progress(self.__progress_now, self.__progress_max)
    
    def _notify_caps(self, caps):
        
        self.__can_play = caps & CAN_PLAY != 0
        self.__can_pause = caps & CAN_PAUSE != 0
        self.__can_next = caps & CAN_GO_NEXT != 0
        self.__can_prev = caps & CAN_GO_PREV != 0
        self.__can_seek = caps & CAN_SEEK != 0
        self.__can_tracklist = caps & CAN_HAS_TRACKLIST != 0
    
    # =========================================================================
    # internal methods (private) 
    # =========================================================================
    
    def __get_tracklist(self):
        """Get a list of track dicts of all tracks in the tracklist."""
        
        try:
            length = self._mp_t.GetLength()
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            length = 0
        
        if length == 0:
            return []
        
        tracks = []
        for i in range(0, length):
            try:
                tracks.append(self._mp_t.GetMetadata(i))
            except DBusException, e:
                log.warning("dbus error: %s" % e)
                return []
            
        return tracks

    def __track2info(self, track):
        """Convert an MPRIS meta data dict to a Remuco info dict."""
        
        id = track.get("location", "None")
        
        info = {}
        title_alt = os.path.basename(id)
        title_alt = os.path.splitext(title_alt)[0]
        info[INFO_TITLE] = track.get("title", title_alt)
        info[INFO_ARTIST] = track.get("artist", "")
        info[INFO_ALBUM] = track.get("album", "")
        info[INFO_GENRE] = track.get("genre", "")
        info[INFO_YEAR] = track.get("year", "")
        info[INFO_LENGTH] = track.get("time", track.get("mtime", 0) // 1000)
        info[INFO_RATING] = track.get("rating", 0)
        
        return (id, info)
    
    def __jump_to(self, position):
        """Jump to a position in the tracklist.
        
        MPRIS has no such method, this is a workaround. Unfortunately it
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
    
    # =========================================================================
    # dbus reply handler (may be reused by subclasses) 
    # =========================================================================
    
    def _dbus_error(self, error):
        """ DBus error handler."""
        
        if self._mp_p is None:
            return # do not log errors when not stopped already
        
        log.warning("DBus error: %s" % error)
        
    def _dbus_ignore(self):
        """ DBus reply handler for methods without reply."""
        
        pass
        
    
