#!/usr/bin/python
"""Remuco player adapter for Amarok, implemented as an executable script.

__author__ = "Oben Sonne <obensonne@googlemail.com>"
__copyright__ = "Copyright 2009, Oben Sonne"
__license__ = "GPL"
__version__ = "0.8.0"

"""
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

