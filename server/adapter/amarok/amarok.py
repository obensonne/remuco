#!/usr/bin/python
"""Remuco player adapter for Amarok, implemented as an executable script.

__author__ = "Oben Sonne <obensonne@googlemail.com>"
__copyright__ = "Copyright 2009, Oben Sonne"
__license__ = "GPL"
__version__ = "0.8.0"

"""
import gobject

import remuco
from remuco import log

# =============================================================================
# Amarok constants
# =============================================================================


# =============================================================================
# player adapter
# =============================================================================

class AmarokAdapter(remuco.MPRISAdapter):
    
    def __init__(self):
        
        remuco.MPRISAdapter.__init__(self, "amarok", "Amarok",
                                     mime_types=("audio",), rating=True)
        
    def poll(self):

        remuco.MPRISAdapter.poll(self)
        
        # amarok does not signal change in shuffle state
        self._poll_status()
        
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

