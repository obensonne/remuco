#!/usr/bin/python

import os
import sys
import dircache

import gobject
import xdg.BaseDirectory
import dbus
import dbus.exceptions

import remuco
from remuco import log

DBUS_NAME = "org.bansheeproject.Banshee"
DBUS_PATH_ENGINE = "/org/bansheeproject/Banshee/PlayerEngine"
DBUS_IFACE_ENGINE = "org.bansheeproject.Banshee.PlayerEngine"
DBUS_PATH_CONTROLLER = "/org/bansheeproject/Banshee/PlaybackController"
DBUS_IFACE_CONTROLLER = "org.bansheeproject.Banshee.PlaybackController"

class BansheeAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        remuco.PlayerAdapter.__init__(self, "Banshee")
        
        self.__dbus_signal_handler = ()
        self.__sid_poll = 0
    
        self.__repeat = False
        self.__shuffle = False
    
        log.debug("init done")
        
    def start(self):
        
        remuco.PlayerAdapter.start(self)
        
        bus = dbus.SessionBus()

        proxy = bus.get_object(DBUS_NAME, DBUS_PATH_ENGINE)
        self.__bse = dbus.Interface (proxy, DBUS_IFACE_ENGINE)

        proxy = bus.get_object(DBUS_NAME, DBUS_PATH_CONTROLLER)
        self.__bsc = dbus.Interface(proxy, DBUS_IFACE_CONTROLLER)

        # set up callbacks

        self.__dbus_signal_handler = (
            self.__bse.connect_to_signal("EventChanged",
                                               self.__notify_event),
            self.__bse.connect_to_signal("StateChanged",
                                               self.__notify_playback),
        )
        
        self.__sid_poll = gobject.timeout_add(2500, self.__poll)

        # initial state query

        try:
            self.__bse.GetCurrentTrack(reply_handler=self.__notify_track,
                                             error_handler=self.__dbus_error)
            self.__bse.GetCurrentState(reply_handler=self.__notify_playback,
                                             error_handler=self.__dbus_error)
            self.__bse.GetVolume(reply_handler=self.__notify_volume,
                                       error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)

        log.debug("start done")
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)
        
        for handler in self.__dbus_signal_handler:
            handler.remove()
            
        self.__dbus_signal_handler = ()
        
        if self.__sid_poll > 0:
            gobject.source_remove(self.__sid_poll)
            self.__sid_poll = 0
        
        self.__bsc = None
        self.__bse = None

    def ctrl_toggle_playing(self):
        
        try:
            self.__bse.TogglePlaying(reply_handler=self.__dbus_ignore,
                                     error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)

    def ctrl_next(self):
        
        try:
            self.__bsc.Next(False,
                            reply_handler=self.__dbus_ignore,
                            error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def ctrl_previous(self):
        
        try:
            self.__bsc.Previous(False,
                                reply_handler=self.__dbus_ignore,
                                error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
    
    
    def ctrl_volume(self, volume):
        
        try:
            self.__bse.SetVolume(dbus.UInt16(volume),
                                 reply_handler=self.__dbus_ignore,
                                 error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
            
        self.__poll()
        

    def ctrl_toggle_repeat(self):
        
        try:
            self.__bsc.SetRepeatMode(int(not self.__repeat),
                                     reply_handler=self.__dbus_ignore,
                                     error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
            
        self.__poll()
            
    def ctrl_toggle_shuffle(self):

        try:
            self.__bsc.SetShuffleMode(int(not self.__shuffle),
                                      reply_handler=self.__dbus_ignore,
                                      error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
            
        self.__poll()

    def __notify_event(self, event, message, buff_percent):
        
        try:
            
            if event == "startofstream" or event == "trackinfoupdated":
                self.__bse.GetCurrentTrack(reply_handler=self.__notify_track,
                                           error_handler=self.__dbus_error)
            elif event == "volume":
                self.__bse.GetVolume(reply_handler=self.__notify_volume,
                                     error_handler=self.__dbus_error)
            else:
                log.debug("event: %s (%s)" %(event, message))
                
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
    
    def __notify_playback(self, state):
        
        log.debug("state: %s" % state)

        if state == "playing":
            playback = remuco.PLAYBACK_PLAY
        elif state == "idle":
            playback = remuco.PLAYBACK_STOP
            self.update_plob(None, None, None)
        else:
            playback = remuco.PLAYBACK_PAUSE
            
        self.update_playback(playback)
        
    def __notify_volume(self, volume):
        
        self.update_volume(volume)
        
    def __notify_track(self, track):
        
        id = track.get("URI")
        
        info = {}
        info[remuco.INFO_TITLE] = track.get("name")
        info[remuco.INFO_ARTIST] = track.get("artist")
        info[remuco.INFO_ALBUM] = track.get("album")
        info[remuco.INFO_LENGTH] = str(track.get("length", 0))
        info[remuco.INFO_RATING] = str(track.get("rating", 0))

        img = None
        art_id = track.get("artwork-id")
        if art_id is not None and len(art_id) > 0:
            file = "%s/album-art/%s.jpg" % (xdg.BaseDirectory.xdg_cache_home,
                                            art_id)
            if os.access(file, os.F_OK):
                img = file
            
        log.debug("track: %s" % info)

        self.update_plob(id, info, img)
        
    def __notify_repeat(self, repeat):
        
        self.__repeat = repeat > 0
        self.update_repeat(self.__repeat)
    
    def __notify_shuffle(self, shuffle):
        
        self.__shuffle = shuffle > 0
        self.update_shuffle(self.__shuffle)

    def __dbus_error(self, error):
        """ DBus error handler. """
        
        log.warning("dbus error: %s" % error)
        
    def __dbus_ignore(self):
        """ DBus reply handler for methods without reply. """
        
        pass
        
    def __poll(self):
        """ GObject timeout callback for repeat and shuffle mode sync. """
        
        try:
            self.__bsc.GetRepeatMode(reply_handler=self.__notify_repeat,
                                     error_handler=self.__dbus_error)
            self.__bsc.GetShuffleMode(reply_handler=self.__notify_shuffle,
                                      error_handler=self.__dbus_error)
        except dbus.exceptions.DBusException, e:
            log.warning("dbus error: %s" % e)
        
        return True
        
# =============================================================================
# main
# =============================================================================

if __name__ == '__main__':

    ba = BansheeAdapter()
    dm = remuco.DBusManager(ba, DBUS_NAME)
    sm = remuco.ScriptManager(dm)
    
    sm.run()
