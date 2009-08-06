# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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

import gobject

import remuco
from remuco import log

import xl.event
import xl.settings

IA_JUMP = remuco.ItemAction("Jump to")
IA_REMOVE = remuco.ItemAction("Remove", multiple=True)
IA_ENQUEUE = remuco.ItemAction("Enqueue", multiple=True)
LA_ACTIVATE = remuco.ListAction("Activate")

PLAYLIST_ACTIONS = (IA_JUMP, IA_REMOVE, IA_ENQUEUE)
QUEUE_ACTIONS = (IA_JUMP, IA_REMOVE)
MLIB_ITEM_ACTIONS = (IA_ENQUEUE, )
MLIB_LIST_ACTIONS = (LA_ACTIVATE, )

PLAYLISTS_SMART = "Smart playlists"
PLAYLISTS_CUSTOM = "Custom playlists"

class ExaileAdapter(remuco.PlayerAdapter):
    
    def __init__(self, exaile):
        
        remuco.PlayerAdapter.__init__(self, "Exaile",
                                      max_rating=5,
                                      playback_known=True,
                                      volume_known=True,
                                      repeat_known=True,
                                      shuffle_known=True,
                                      progress_known=True)
        
        self.__ex = exaile
        
    def start(self, event, exaile, ignore):
        
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
        
        log.debug("here we go")
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        xl.event.remove_callback(self.__notify_track_change)
        xl.event.remove_callback(self.__notify_playback_change)

        log.debug("bye, turning off the light")
        
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
        q = self.__ex.queue
        print("type: %s, dir: %s" % (type(q), dir(q)))
    
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
        pos = min(pos, track.get_duration())
        pos = max(pos, 0)
        
        self.__ex.player.seek(pos)
        
        gobject.idle_add(self.__update_progress)
    
    def ctrl_rate(self, rating):
        
        track = self.__ex.player.current
        if not track:
            return
        
        track.set_rating(rating)
    
    def ctrl_tag(self, id, tags):
        """Attach some tags to an item.
        
        @param id:
            ID of the item to attach the tags to
        @param tags:
            a list of tags
        
        @note: Tags does not mean ID3 tags or similar. It means the general
            idea of tags (e.g. like used at last.fm). 

        @note: Override if it is possible and makes sense.
               
        """
    
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
            
            reply.nested = (PLAYLISTS_SMART, PLAYLISTS_CUSTOM)
            reply.send()
        
        elif path[0] == PLAYLISTS_SMART:
            
            if len(path) == 1:
                reply.nested = self.__ex.smart_playlists.list_playlists()
                reply.list_actions = MLIB_LIST_ACTIONS
            else:
                pl = self.__ex.smart_playlists.get_playlist(path[1])
                tracks = pl.get_playlist(collection=self.__ex.collection)
                reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
                reply.item_actions = MLIB_ITEM_ACTIONS
             
        elif path[0] == PLAYLISTS_CUSTOM:
        
            if len(path) == 1:
                reply.nested = self.__ex.playlists.list_playlists()
                #reply.list_actions = MLIB_LIST_ACTIONS
            else:
                pl = self.__ex.playlists.get_playlist(path[1])
                tracks = pl.get_ordered_tracks()
                reply.ids, reply.names = self.__tracklist_to_itemlist(tracks)
                reply.item_actions = MLIB_ITEM_ACTIONS
                
        else:
            log.error("** BUG ** unexpected mlib path")
        
        reply.send()

    # =========================================================================
    # action interface
    # =========================================================================
    
    def action_playlist_item(self, action_id, positions, ids):
        
        if action_id == IA_JUMP.id:
            # TODO: recheck this once Exaile 3 final is released
            track = self.__ex.collection.get_track_by_loc(ids[0])
            self.__ex.queue.next(track=track)
            self.__ex.queue.current_playlist.set_current_pos(positions[0])
        elif action_id == IA_REMOVE.id:
            positions.reverse()
            for pos in positions:
                log.debug("remove item %d" % pos)
                self.__ex.queue.current_playlist.remove_tracks(pos, pos)
        elif action_id == IA_ENQUEUE.id:
            track_list = self.__ex.collection.get_tracks_by_locs(ids)
            self.__ex.queue.add_tracks(track_list)
        else:
            log.error("** BUG ** unexpected playlist item action")

    def action_queue_item(self, action_id, positions, ids):
        
        if action_id == IA_JUMP.id:
            # TODO: recheck this once Exaile 3 final is released
            track = self.__ex.collection.get_track_by_loc(ids[0])
            self.__ex.queue.next(track=track)
            self.__ex.queue.remove_tracks(positions[0], positions[0])
            #self.__ex.queue.set_current_pos(positions[0])
        elif action_id == IA_REMOVE.id:
            positions.reverse()
            for pos in positions:
                log.debug("remove item %d" % pos)
                self.__ex.queue.remove_tracks(pos, pos)
        else:
            log.error("** BUG ** unexpected queue item action")
            
    def action_mlib_item(self, action_id, path, positions, ids):
        
        if action_id == IA_ENQUEUE.id:
            track_list = self.__ex.collection.get_tracks_by_locs(ids)
            self.__ex.queue.add_tracks(track_list)
        else:
            log.error("** BUG ** unexpected mlib item action")

    def action_mlib_list(self, action_id, path):

        if action_id == LA_ACTIVATE.id:
            
            pl = None
            if path[0] == PLAYLISTS_SMART:
                pl = self.__ex.smart_playlists.get_playlist(path[1])
            elif path[0] == PLAYLISTS_CUSTOM:
                pl = self.__ex.playlists.get_playlist(path[1])
            else:
                log.error("** BUG ** unexpected mlib path")
            
            if pl:
                # this does not refelct in GUI, as a result,
                # queue.current_playlist is a smart playlist (but we need
                # an instance of it, a normal playlist)
                self.__ex.queue.set_current_playlist(pl)
        else:
            log.error("** BUG ** unexpected mlib list action")
    # =========================================================================
    # internal methods
    # =========================================================================
    
    def __notify_track_change(self, type, object, data):
        """Callback on track change."""
        
        log.debug("track change: %s" % data)
        self.__update_track(data)
        self.__update_progress()
        self.__update_position()
    
    def __notify_playback_change(self, type, object, data):
        """Callback on playback change."""
        
        self.__update_playback()
        
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
            val = track.get_tag(key)
            if val:
                try:
                    val = val[0]
                except IndexError:
                    pass
            return val
        
        if track is None:
            id = None
            info = None
            img = None
        else:
            id = track.get_loc()
            info = {}
            info[remuco.INFO_ARTIST] = get_tag("artist")
            info[remuco.INFO_ALBUM] = get_tag("album")
            info[remuco.INFO_TITLE] = get_tag("title")
            info[remuco.INFO_YEAR] = get_tag("date")
            info[remuco.INFO_GENRE] = get_tag("genre")
            info[remuco.INFO_BITRATE] = track.get_bitrate().replace('k','')
            info[remuco.INFO_RATING] = track.get_rating()
            info[remuco.INFO_LENGTH] = track.get_duration()
            img = track.get_tag("arturl")
            if not img:
                img = self.find_image(id)
            log.debug("img: %s" % img)
            
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
            len = track.get_duration()
            pos = self.__ex.player.get_time()
        
        self.update_progress(pos, len)
        
    def __update_position(self):
        
        pl = self.__ex.queue.current_playlist
        
        self.update_position(pl.get_current_pos())
        
    def __tracklist_to_itemlist(self, track_list):
        
        ids = []
        names = []
        
        for track in track_list:
            
            ids.append(track.get_loc())
            artist = track.get_tag("artist")
            if artist:
                artist = artist[0]
            title = track.get_tag("title")
            if title:
                title = title[0]
            name = "%s - %s" % (artist or "???", title or "???") 
            names.append(name)
        
        return ids, names
        
# =============================================================================
# Exaile plugin interface
# =============================================================================
    
ea = None

def enable(exaile):
    global ea
    ea = ExaileAdapter(exaile)
    if exaile.loading:
        xl.event.add_callback(ea.start, "exaile_loaded")
    else:
        ea.start("exaile_loaded", exaile, None)

def disable(exaile):
    global ea
    if ea:
        ea.stop()
        ea = None

def teardown(exaile):
    # teardown and disable are the same here
    disable(exaile)