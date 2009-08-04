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

import remuco
from remuco import log

class ExaileAdapter(remuco.PlayerAdapter):
    
    def __init__(self, exaile):
        
        remuco.PlayerAdapter.__init__(self, "Exaile",
                                      playback_known=True,
                                      volume_known=True)
        
        self.__ex = exaile
        
    def start(self):
        
        remuco.PlayerAdapter.start(self)

        log.debug("here we go")
        
    def stop(self):
        
        remuco.PlayerAdapter.stop(self)

        log.debug("bye, turning off the light")
        
    def poll(self):
        
        import random
        
        volume = random.randint(0,100)
        self.update_volume(volume)
        
        playing = random.randint(0,1)
        if playing:
            self.update_playback(remuco.PLAYBACK_PLAY)
        else:
            self.update_playback(remuco.PLAYBACK_PAUSE)
        
    # =========================================================================
    # control interface
    # =========================================================================
    
    def ctrl_toggle_playing(self):
        """Toggle play and pause. 
        
        @note: Override if it is possible and makes sense.
        
        """
        if self.__ex.player.is_playing() or self.__ex.player.is_paused():
            self.__ex.player.toggle_pause()
        else:
            self.__ex.queue.play()
            
    def ctrl_toggle_repeat(self):
        """Toggle repeat mode. 
        
        @note: Override if it is possible and makes sense.
        
        @see: update_repeat()
               
        """
        print(str(dir(self.__ex.player)))
    
    def ctrl_toggle_shuffle(self):
        """Toggle shuffle mode. 
        
        @note: Override if it is possible and makes sense.
        
        @see: update_shuffle()
               
        """
        print(str(dir(self.__ex)))
    
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
    
    def ctrl_rate(self, rating):
        """Rate the currently played item. 
        
        @param rating:
            rating value (int)
        
        @note: Override if it is possible and makes sense.
        
        """
    
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
        """Adjust volume. 
        
        @param volume:
            * -1: decrease by some percent (5 is a good value)
            *  0: mute volume
            * +1: increase by some percent (5 is a good value)
        
        @note: Override if it is possible and makes sense.
               
        """
        
    # =========================================================================
    # request interface
    # =========================================================================
    
# =============================================================================
# Exaile plugin interface
# =============================================================================
    
ea = None

def enable(exaile):
    print "Hello, world!"
    global ea
    ea = ExaileAdapter(exaile)
    ea.start()


def disable(exaile):
    print "Goodbye. :("
    global ea
    ea.stop()
    ea = None

