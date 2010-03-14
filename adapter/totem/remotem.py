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

"""Totem player adapter for Remuco, implemented as a Totem plugin."""

import mimetypes
import os
import os.path
import subprocess

import gobject
import totem

import remuco
from remuco import log

# =============================================================================
# totem plugin interface
# =============================================================================

class RemucoPlugin(totem.Plugin):

    def __init__(self):
        
        totem.Plugin.__init__(self)
            
        self.__ta = None
        
    def activate(self, totem):
        
        if self.__ta is not None:
            return
        
        print("create TotemAdapter")
        self.__ta = TotemAdapter()
        print("TotemAdapter created")

        print("start TotemboxAdapter")
        self.__ta.start(totem)
        print("TotemAdapter started")
    
    def deactivate(self, totem):

        if self.__ta is None:
            return
        
        print("stop TotemboxAdapter")
        self.__ta.stop()
        print("TotemAdapter stopped")
        
        self.__ta = None
    

# =============================================================================
# supported file actions
# =============================================================================

FA_SETPL = remuco.ItemAction("Set as playlist", multiple=True)
FA_ENQUEUE = remuco.ItemAction("Enqueue", multiple=True)

FILE_ACTIONS=(FA_ENQUEUE, FA_SETPL)

# =============================================================================
# totem player adapter
# =============================================================================

class TotemAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        remuco.PlayerAdapter.__init__(self, "Totem",
                                      mime_types=("audio", "video"),
                                      volume_known=True,
                                      playback_known=True,
                                      progress_known=True,
                                      file_actions=FILE_ACTIONS)
        
        self.__to = None
        
        self.__signal_ids = ()
        
        self.__update_item = False
        self.__md_album = None
        self.__md_artist = None
        self.__md_title = None
        self.__last_mrl = None
        
        self.__seek_step_initial = 5000
        self.__seek_step = self.__seek_step_initial
        self.__css_sid = 0
        
        if not mimetypes.inited:
            mimetypes.init()
                             
    # -------------------------------------------------------------------------
    # player adapter interface
    # -------------------------------------------------------------------------

    def start(self, totem):
        
        remuco.PlayerAdapter.start(self)
        
        self.__to = totem
        self.__vw = totem.get_video_widget()
        
        self.__signal_ids = (
            self.__to.connect("file-opened", self.__notify_file_opened),
            self.__to.connect("file-closed", self.__notify_file_closed),
            self.__to.connect("metadata-updated", self.__notify_metadata_updated)
        )
        self.__css_sid = 0
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        for sid in self.__signal_ids:
            self.__to.disconnect(sid)
        self.__signal_ids = ()
        
        self.__to = None
        self.__vw = None

    def poll(self):
        
        self.__poll_item()
        self.__poll_state()
        self.__poll_progress()
        
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        
        self.__to.action_play_pause()
        
        gobject.idle_add(self.__poll_state)
    
    def ctrl_next(self):
        
        self.__to.action_next()
    
    def ctrl_previous(self):
        
        self.__to.action_previous()
    
    def ctrl_seek(self, direction):
        
        if not self.__to.is_seekable() or self.__seek_step == 0:
            return
        
        progress = self.__to.get_current_time()
        
        self.__to.action_seek_relative(self.__seek_step * direction)
        
        gobject.idle_add(self.__poll_progress)

        if direction < 0 and progress < self.__seek_step:
            return # no seek check
        
        if self.__css_sid == 0:
            # in 1.5 seconds, at least 3 x initial seek step should be elapsed:
            self.__css_sid = gobject.timeout_add(1500, self.__check_seek_step,
                progress, self.__seek_step_initial * 3, self.__seek_step + 5000)
        
    def ctrl_volume(self, direction):
        
        # FIXME: action_volume_relative() in 2.24 says it needs an int but it
        #        behaves as if it gets a float. Internally volume is set via
        #        the video widget, so we do it the same way here:
        
        if direction == 0:
            volume = 0
        else:
            volume = self.__get_volume() + (direction * 5)
            volume = min(volume, 100)
            volume = max(volume, 0)
        
        self.__vw.set_property("volume", volume / 100.0)
        
        gobject.idle_add(self.__poll_state)
        
    def ctrl_toggle_fullscreen(self):
        
        self.__to.action_fullscreen_toggle()

    # =========================================================================
    # actions interface
    # =========================================================================
    
    def action_files(self, action_id, files, uris):
        
        if action_id == FA_ENQUEUE.id:
            subprocess.Popen(["totem", "--enqueue"] + uris)
        elif action_id == FA_SETPL.id:
            subprocess.Popen(["totem", "--replace"] + uris)
        else:
            log.error("** BUG ** unexpected action ID")

    # =========================================================================
    # internal methods
    # =========================================================================

    def __get_title_from_window(self):

        # FIXME: In C plugins there is a function totem_get_short_title(). I
        #        could not find something similar in the Python bindings that
        #        works for all types of media played in Totem.
        #        Here we grab the window title as a work around.
        
        title = self.__to.get_main_window().get_title()
        
        type, enc = mimetypes.guess_type(title)
        if type: # looks like a file name
            title = os.path.splitext(title)[0]
            
        return title
    
    def __check_seek_step(self, progress_before, exp_min_diff, new_step):
        """Check if a seek had some effect and adjust seek step if not.
        
        @param progress_before:
            playback progress before seeking
        @param new_step:
            new seek step to set if progress did not change significantly
            
        """
        progress_now = self.__to.get_current_time()
        
        log.debug("seek diff: %d" % abs(progress_now - progress_before))
        
        if abs(progress_now - progress_before) < exp_min_diff:
            log.debug("adjust seek step to %d" % new_step)
            self.__seek_step = new_step
            
        self.__css_sid = 0
        
    def __poll_item(self):
        
        try:
            mrl = self.__to.get_current_mrl()
        except AttributeError: # totem < 2.24
            mrl = self.__to.get_main_window().get_title() # <- fake mrl
        
        if not self.__update_item and mrl == self.__last_mrl:
            return
        
        # reset seek step
        
        len = self.__to.get_property("stream-length")
        
        if len < 10000:
            self.__seek_step_initial = 0
        else:
            self.__seek_step_initial = max(5000, len // 200)
            
        self.__seek_step = self.__seek_step_initial
            
        log.debug("reset seek step to %d" % self.__seek_step)
        
        # update meta information
        
        log.debug("update item")

        self.__update_item = False
        self.__last_mrl = mrl
        
        info = {}
        
        if ((self.__md_artist, self.__md_title, self.__md_album) ==
            (None, None, None)): 
            info[remuco.INFO_TITLE] = self.__get_title_from_window()
        else:
            info[remuco.INFO_ARTIST] = self.__md_artist
            info[remuco.INFO_TITLE] = self.__md_title
            info[remuco.INFO_ALBUM] = self.__md_album
        
        info[remuco.INFO_LENGTH] = int(len / 1000)
        
        img = self.find_image(mrl)
        
        self.update_item(mrl, info, img)
        
    def __poll_state(self):
        
        if self.__to.is_playing():
            playback = remuco.PLAYBACK_PLAY
        else:
            playback = remuco.PLAYBACK_PAUSE
        self.update_playback(playback)
        
        self.update_volume(self.__get_volume())
        
    def __poll_progress(self):
        
        progress = self.__to.get_current_time() / 1000
        length = self.__to.get_property("stream-length") / 1000
        
        self.update_progress(progress, length)
        
    def __get_volume(self):
        
        return int(self.__vw.get_property("volume") * 100)
        
    def __notify_metadata_updated(self, totem, artist, title, album, track=0):
        
        # 'track' has been added in Totem 2.26
        
        log.debug("metadata updated: %s, %s, %s" % (artist, title, album))

        # in Totem < 2.26 meta data is always None
        
        self.__md_artist = artist
        self.__md_title = title
        self.__md_album = album
        
        self.__update_item = True

    def __notify_file_opened(self, totem, file):
        
        # XXX: does not get called for podcasts from BBC plugin
        
        log.debug("file opened: %s" % file)

        self.__update_item = True
        
    def __notify_file_closed(self, totem):
        
        log.debug("file closed")
        
        self.__update_item = True
        
