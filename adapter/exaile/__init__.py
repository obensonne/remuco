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

"""Exaile adapter for Remuco, implemented as an Exaile plugin."""

from __future__ import with_statement

import os.path
import re

import gobject

import xl.event
import xl.settings

try:
    from xl.cover import NoCoverFoundException # exaile 3.0
except ImportError:
    class NoCoverFoundException(Exception): pass # exaile 3.1

try:
    from xl import covers # exaile 3.2
except ImportError:
    pass # exaile 3.1

import remuco
from remuco import log

# =============================================================================
# constants
# =============================================================================

PLAYLISTS_SMART = "Smart playlists"
PLAYLISTS_CUSTOM = "Custom playlists"
PLAYLISTS_OPEN = "Open playlists"

SEARCH_MASK = ("Artist", "Album", "Title", "Genre")

# =============================================================================
# actions
# =============================================================================

IA_JUMP = remuco.ItemAction("Jump to")
IA_REMOVE = remuco.ItemAction("Remove", multiple=True)
IA_ENQUEUE = remuco.ItemAction("Enqueue", multiple=True)
IA_APPEND = remuco.ItemAction("Append", multiple=True)
IA_REPLACE = remuco.ItemAction("Reset playlist", multiple=True)
IA_NEW_PLAYLIST = remuco.ItemAction("New playlist", multiple=True)

LA_OPEN = remuco.ListAction("Open")
LA_ACTIVATE = remuco.ListAction("Activate")
LA_CLOSE = remuco.ListAction("Close")

PLAYLIST_ACTIONS = (IA_JUMP, IA_REMOVE, IA_ENQUEUE)
QUEUE_ACTIONS = (IA_JUMP, IA_REMOVE)
MLIB_ITEM_ACTIONS = (IA_JUMP, IA_ENQUEUE, IA_APPEND, IA_REPLACE, IA_REMOVE,
                     IA_NEW_PLAYLIST)
MLIB_LIST_ACTIONS = (LA_OPEN,)
MLIB_LIST_OPEN_ACTIONS = (LA_ACTIVATE, LA_CLOSE)
SEARCH_ACTIONS = (IA_ENQUEUE, IA_APPEND, IA_REPLACE, IA_NEW_PLAYLIST)

# =============================================================================
# player adapter
# =============================================================================

class ExaileAdapter(remuco.PlayerAdapter):
    
    def __init__(self, exaile):
        
        remuco.PlayerAdapter.__init__(self, "Exaile",
                                      max_rating=5,
                                      playback_known=True,
                                      volume_known=True,
                                      repeat_known=True,
                                      shuffle_known=True,
                                      progress_known=True,
                                      search_mask=SEARCH_MASK)
        
        self.__ex = exaile
        
    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        for event in ("playback_track_start", "playback_track_end"):
            xl.event.add_callback(self.__notify_track_change, event)
            
        for event in ("playback_player_end", "playback_player_start",
                      "playback_toggle_pause"):
            xl.event.add_callback(self.__notify_playback_change, event)
        
        self.__update_track(self.__ex.player.current)
        self.__update_position()
        self.__update_playback()
        # other updates via poll()
        
        log.debug("remuco exaile adapter started")
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        xl.event.remove_callback(self.__notify_track_change)
        xl.event.remove_callback(self.__notify_playback_change)

        log.debug("remuco exaile adapter stopped")
        
    def poll(self):
        
        self.__update_repeat_and_shuffle()
        self.__update_volume()
        self.__update_progress()
        
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        
        if self.__ex.player.is_playing() or self.__ex.player.is_paused():
            self.__ex.player.toggle_pause()
        else:
            self.__ex.queue.play()
            
        # when playing after stopped, the 'playback_player_start' is missed
        gobject.idle_add(self.__update_playback)
            
    def ctrl_toggle_repeat(self):
        
        repeat = not self.__ex.queue.current_playlist.is_repeat()
        self.__ex.queue.current_playlist.set_repeat(repeat)
        gobject.idle_add(self.__update_repeat_and_shuffle)
    
    def ctrl_toggle_shuffle(self):
        
        shuffle = not self.__ex.queue.current_playlist.is_random()
        self.__ex.queue.current_playlist.set_random(shuffle)
        gobject.idle_add(self.__update_repeat_and_shuffle)
    
    def ctrl_next(self):
        """Play the next item. 
        
        @note: Override if it is possible and makes sense.
        
        """
        self.__ex.queue.next()
    
    def ctrl_previous(self):
        """Play the previous item. 
        
        @note: Override if it is possible and makes sense.
        
        """
        self.__ex.queue.prev()
    
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
        if not self.__ex.player.is_playing():
            return
        
        track = self.__ex.player.current
        if not track:
            return
        
        pos = self.__ex.player.get_time()
        pos = pos + direction * 5
        if self.__ex.get_version() < "0.3.1":
            pos = min(pos, track.get_duration())
        else:
            pos = min(pos, int(track.get_tag_raw("__length")))
        pos = max(pos, 0)
        
        self.__ex.player.seek(pos)
        
        gobject.idle_add(self.__update_progress)
    
    def ctrl_rate(self, rating):
        
        track = self.__ex.player.current
        if not track:
            return
        
        track.set_rating(rating)
    
    def ctrl_volume(self, direction):
        
        if direction == 0:
            self.__ex.player.set_volume(0)
        else:
            volume = self.__ex.player.get_volume() + direction * 5
            volume = min(volume, 100)
            volume = max(volume, 0)
            self.__ex.player.set_volume(volume)
            
        gobject.idle_add(self.__update_volume)
        
    # =========================================================================
    # request interface
    # =========================================================================
    
    def request_playlist(self, reply):
        
        tracks = self.__ex.queue.current_playlist.get_ordered_tracks()
        
        reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
        
        reply.item_actions = PLAYLIST_ACTIONS
        
        reply.send()

    def request_queue(self, reply):
        
        tracks = self.__ex.queue.get_tracks()

        reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
    
        reply.item_actions = QUEUE_ACTIONS
        
        reply.send()
        
    def request_mlib(self, reply, path):
        
        if not path:
            
            reply.nested = (PLAYLISTS_OPEN, PLAYLISTS_SMART, PLAYLISTS_CUSTOM)
            reply.send()
        
        elif path[0] == PLAYLISTS_SMART:
            
            if len(path) == 1:
                reply.nested = self.__ex.smart_playlists.list_playlists()
                reply.list_actions = MLIB_LIST_ACTIONS
            else:
                pl = self.__ex.smart_playlists.get_playlist(path[1])
                reply.ids, reply.names = ["XXX"], ["This is a dynamic playlist!"] 
             
        elif path[0] == PLAYLISTS_CUSTOM:
        
            if len(path) == 1:
                reply.nested = self.__ex.playlists.list_playlists()
                reply.list_actions = MLIB_LIST_ACTIONS
            else:
                pl = self.__ex.playlists.get_playlist(path[1])
                tracks = pl.get_ordered_tracks()
                reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
                
        elif path[0] == PLAYLISTS_OPEN:
                
            
            if len(path) == 1:
                plo_list, pln_list = self.__get_open_playlists()
                reply.nested = pln_list
                reply.list_actions = MLIB_LIST_OPEN_ACTIONS
            else:
                pl, i = self.__get_open_playlist(path)
                tracks = pl.get_ordered_tracks()
                reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
                reply.item_actions = MLIB_ITEM_ACTIONS
                
        else:
            log.error("** BUG ** unexpected mlib path")
        
        reply.send()

    def request_search(self, reply, query):
        
        if self.__ex.get_version() < "0.3.1":
            tracks = None
            for key, val in zip(SEARCH_MASK, query):
                val = val.strip()
                if val:
                    expr = "%s=%s" % (key, val)
                    tracks = self.__ex.collection.search(expr, tracks=tracks)
                    
            if tracks is None: # empty query, return _all_ tracks
                tracks = self.__ex.collection.search("", tracks=tracks)
        else:
            tml = []
            for key, val in zip(SEARCH_MASK, query):
                val = val.strip()
                if val:
                    sexpr = "%s=%s" %  (key.lower(), val)
                    tml.append(xl.trax.TracksMatcher(sexpr, case_sensitive=False))
            tracks = xl.trax.search_tracks(self.__ex.collection, tml)

        reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
        reply.item_actions = SEARCH_ACTIONS
        reply.send()
    
    # =========================================================================
    # action interface
    # =========================================================================
    
    def action_playlist_item(self, action_id, positions, ids):
        
        if self.__handle_generic_item_action(action_id, ids):
            pass # we are done
        elif action_id == IA_JUMP.id:
            track = self.__ex.collection.get_track_by_loc(ids[0])
            self.__ex.queue.next(track=track)
            self.__ex.queue.current_playlist.set_current_pos(positions[0])
        elif action_id == IA_REMOVE.id:
            self.__remove_tracks_from_playlist(positions)
        else:
            log.error("** BUG ** unexpected playlist item action")

    def action_queue_item(self, action_id, positions, ids):
        
        if action_id == IA_JUMP.id:
            track = self.__ex.collection.get_track_by_loc(ids[0])
            self.__ex.queue.next(track=track)
            self.__remove_tracks_from_playlist(positions, pl=self.__ex.queue)
        elif action_id == IA_REMOVE.id:
            self.__remove_tracks_from_playlist(positions, pl=self.__ex.queue)
        else:
            log.error("** BUG ** unexpected queue item action")
            
    def action_mlib_item(self, action_id, path, positions, ids):
        
        if self.__handle_generic_item_action(action_id, ids):
            pass # we are done
        elif action_id == IA_JUMP.id:
            self.action_mlib_list(LA_ACTIVATE.id, path)
            track = self.__ex.collection.get_track_by_loc(ids[0])
            self.__ex.queue.next(track=track)
            self.__ex.queue.current_playlist.set_current_pos(positions[0])
        elif action_id == IA_REMOVE.id:
            pl, i = self.__get_open_playlist(path)
            self.__remove_tracks_from_playlist(positions, pl=pl)
        else:
            log.error("** BUG ** unexpected mlib item action")

    def action_mlib_list(self, action_id, path):

        if action_id == LA_ACTIVATE.id:
            
            if path[0] == PLAYLISTS_OPEN:
                pl, i = self.__get_open_playlist(path)
                self.__ex.gui.main.playlist_notebook.set_current_page(i)
            else:
                log.error("** BUG ** unexpected mlib path %s" % path)
                
        elif action_id == LA_OPEN.id:
            
            if path[0] == PLAYLISTS_SMART:
                pl = self.__ex.smart_playlists.get_playlist(path[1])
                pl = pl.get_playlist(self.__ex.collection)
                self.__ex.gui.main.add_playlist(pl)
            elif path[0] == PLAYLISTS_CUSTOM:
                pl = self.__ex.playlists.get_playlist(path[1])
                self.__ex.gui.main.add_playlist(pl)
            else:
                log.error("** BUG ** unexpected mlib path %s" % path)
        
        elif action_id == LA_CLOSE.id:
            
            pl, i = self.__get_open_playlist(path)
            nb = self.__ex.gui.main.playlist_notebook
            nb.remove_page(i)
        
        else:
            log.error("** BUG ** unexpected mlib list action")
            
    def action_search_item(self, action_id, positions, ids):
        
        if self.__handle_generic_item_action(action_id, ids):
            pass # we are done
        else:
            log.error("** BUG ** unexpected search item action")
            
    # =========================================================================
    # callbacks
    # =========================================================================
    
    def __notify_track_change(self, type, object, data):
        """Callback on track change."""
        
        self.__update_track(data)
        self.__update_progress()
        self.__update_position()
    
    def __notify_playback_change(self, type, object, data):
        """Callback on playback change."""
        
        self.__update_playback()
        
    # =========================================================================
    # helper methods
    # =========================================================================

    def __update_playback(self):
        """Update playback state."""
        
        if self.__ex.player.is_playing():
            playback = remuco.PLAYBACK_PLAY
        elif self.__ex.player.is_paused():
            playback = remuco.PLAYBACK_PAUSE
        else:
            playback = remuco.PLAYBACK_STOP
            
        self.update_playback(playback)
        
    def __update_repeat_and_shuffle(self):
        """Update repeat and shuffle state."""
        
        repeat = self.__ex.queue.current_playlist.is_repeat()
        self.update_repeat(repeat)

        shuffle = self.__ex.queue.current_playlist.is_random()
        self.update_shuffle(shuffle)
    
    def __update_track(self, track):
        """Update meta information of current track."""
        
        def get_tag(key):
            if self.__ex.get_version() < "0.3.1":
                val = track.get_tag(key)
            else: # Exaile >= 0.3.1
                val = track.get_tag_raw(key)
            return val and (isinstance(val, list) and val[0] or val) or None
        
        id = None
        info = None
        img = None
        
        if track:
            id = track.get_loc_for_io()
            info = {}
            info[remuco.INFO_ARTIST] = get_tag("artist")
            info[remuco.INFO_ALBUM] = get_tag("album")
            info[remuco.INFO_TITLE] = get_tag("title")
            info[remuco.INFO_YEAR] = get_tag("date")
            info[remuco.INFO_GENRE] = get_tag("genre")
            info[remuco.INFO_RATING] = track.get_rating()
            if self.__ex.get_version() < "0.3.1":
                info[remuco.INFO_BITRATE] = track.get_bitrate().replace('k','')
                info[remuco.INFO_LENGTH] = track.get_duration()
                try:
                    img = self.__ex.covers.get_cover(track)
                except NoCoverFoundException: # exaile 0.3.0 only
                    pass
            else: # Exaile >= 0.3.1
                info[remuco.INFO_BITRATE] = get_tag("__bitrate") // 1000
                info[remuco.INFO_LENGTH] = int(get_tag("__length"))
                if self.__ex.get_version() < "0.3.2":
                    idata = self.__ex.covers.get_cover(track, set_only=True)
                else:
                    idata = covers.MANAGER.get_cover(track, set_only=True)
                if idata:
                    img = os.path.join(self.config.cache, "exaile.cover")
                    with open(img, "w") as fp:
                        fp.write(idata)
            if not img and track.local_file_name(): # loc_for_io may be != UTF-8
                img = self.find_image(track.local_file_name())
            
        self.update_item(id, info, img)
        
    def __update_volume(self):
        """Update volume."""
        
        self.update_volume(self.__ex.player.get_volume())
        
    def __update_progress(self):
        """Update play progress of current track."""
        
        track = self.__ex.player.current
        
        if not track:
            len = 0
            pos = 0
        else:
            if self.__ex.get_version() < "0.3.1":
                len = track.get_duration()
            else: # Exaile >= 0.3.1
                len = int(track.get_tag_raw("__length"))
            pos = self.__ex.player.get_time()
        
        self.update_progress(pos, len)
        
    def __update_position(self):
        """Update current playlist position."""
        
        pl = self.__ex.queue.current_playlist
        
        self.update_position(pl.get_current_pos())
        
    def __handle_generic_item_action(self, action_id, ids):
        """Process generic item actions.
        
        Actions: IA_ENQUEUE, IA_APPEND, IA_REPLACE, IA_NEW_PLAYLIST
        
        Generic item actions are processed independent of the list containing
        the items (playlist, queue, mlib or search result).
        
        @return: True if action has been handled, False other wise 
        """
        
        handled = True
        
        tracks = self.__ex.collection.get_tracks_by_locs(ids)
        tracks = [t for t in tracks if t is not None]
        
        if action_id == IA_ENQUEUE.id:
            self.__ex.queue.add_tracks(tracks)
        elif action_id == IA_APPEND.id:
            self.__ex.queue.current_playlist.add_tracks(tracks)
        elif action_id == IA_REPLACE.id:
            self.__ex.queue.current_playlist.set_tracks(tracks)
        elif action_id == IA_NEW_PLAYLIST.id:
            self.__ex.gui.main.add_playlist()
            self.__ex.queue.current_playlist.add_tracks(tracks)
        else:
            handled = False
        
        return handled
    
    def __tracklist_to_itemlist(self, tracks):
        """Convert a list if track objects to a Remuco item list."""
        
        ids = []
        names = []
        
        for track in tracks:
            # first, check if track is a SearchResultTrack (since Exaile 0.3.1)
            track = hasattr(track, "track") and track.track or track
            ids.append(track.get_loc_for_io())
            if self.__ex.get_version() < "0.3.1":
                artist = track.get_tag("artist")
                title = track.get_tag("title")
            else:
                title = track.get_tag_raw("title")
                artist = track.get_tag_raw("artist")
            artist = artist and artist[0] or None
            title = title and title[0] or None
            name = "%s - %s" % (artist or "???", title or "???") 
            names.append(name)
        
        return ids, names
    
    def __get_open_playlists(self):
        """Get open playlists.
        
        Returns 2 lists, one with the playlist objects and one with the
        playlist names (formatted appropriately to handle duplicates).
        
        """
        nb = self.__ex.gui.main.playlist_notebook
        plo_list = []
        pln_list = []
        for i in range(nb.get_n_pages()):
            plo = nb.get_nth_page(i).playlist
            plo_list.append(plo)
            if plo == self.__ex.queue.current_playlist:
                num = "(%d)" % (i+1)
            else:
                num = "[%d]" % (i+1)
            pln_list.append("%s %s" % (num, plo.get_name()))
        return plo_list, pln_list
    
    def __get_open_playlist(self, path):
        """Get a playlist object and tab number of an open playlist."""
        
        plo_list, pln_list = self.__get_open_playlists()
        i = int(re.match("[\(\[](\d+)[\)\]]", path[1]).group(1)) - 1 
        try:
            return plo_list[i], i
        except IndexError:
            return plo_list[0], 0
    
    def __remove_tracks_from_playlist(self, positions, pl=None):
        """Remove tracks from a playlist given their positions. """
        
        if pl is None:
            pl = self.__ex.queue.current_playlist
            
        revpos = positions[:]
        revpos.reverse()
        
        for pos in revpos:
            pl.remove_tracks(pos, pos)
            
# =============================================================================
# plugin interface
# =============================================================================
    
epa = None

def enable(exaile):
    if exaile.loading:
        xl.event.add_callback(_enable, "exaile_loaded")
    else:
        _enable("exaile_loaded", exaile, None)

def _enable(event, exaile, nothing):
    global epa
    epa = ExaileAdapter(exaile)
    epa.start()

def disable(exaile):
    global epa
    if epa:
        epa.stop()
        epa = None

def teardown(exaile):
    # teardown and disable are the same here
    disable(exaile)