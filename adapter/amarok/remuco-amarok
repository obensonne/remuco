#!/usr/bin/python

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

"""Remuco player adapter for Amarok, implemented as an executable script."""

import dbus
from dbus.exceptions import DBusException
import gobject

import remuco
from remuco import log

# =============================================================================
# player adapter
# =============================================================================

class AmarokAdapter(remuco.MPRISAdapter):
    
    def __init__(self):
        
        remuco.MPRISAdapter.__init__(self, "amarok", "Amarok",
                                     mime_types=("audio",), rating=True)
        
        self.__am = None
        
    def start(self):
        
        remuco.MPRISAdapter.start(self)
        
        try:
            bus = dbus.SessionBus()
            proxy = bus.get_object("org.kde.amarok", "/amarok/MainWindow")
            self.__am = dbus.Interface(proxy, "org.kde.KMainWindow")
        except DBusException, e:
            raise StandardError("dbus error: %s" % e)

    def stop(self):
        
        remuco.MPRISAdapter.stop(self)
        
        self.__am = None
        
    def poll(self):

        remuco.MPRISAdapter.poll(self)
        
        # amarok does not signal change in shuffle state
        self._poll_status()
        
    # =========================================================================
    # control interface 
    # =========================================================================
    
    def ctrl_rate(self, rating):
        
        rating = min(rating, 5)
        rating = max(rating, 1)
        action = "rate%s" % rating
        
        try:
            self.__am.activateAction(action)
        except DBusException, e:
            log.warning("dbus error: %s" % e)
            
    def ctrl_toggle_shuffle(self):
        
        remuco.MPRISAdapter.ctrl_toggle_shuffle(self)
        
        # amarok does not signal change in shuffle state
        gobject.idle_add(self._poll_status)
            
# =============================================================================
# main
# =============================================================================

if __name__ == '__main__':

    pa = AmarokAdapter()
    mg = remuco.Manager(pa, player_dbus_name="org.mpris.amarok")
    
    mg.run()

