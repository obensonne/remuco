# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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

import signal
import gobject

_paref = None
_cmdlist = None

def setup(adapter):
    global _paref, _cmdlist

    _paref = adapter

    #TODO fetch this automatically
    _cmdlist = [adapter.ctrl_toggle_playing,
         adapter.ctrl_toggle_fullscreen]

    signal.signal(signal.SIGHUP, handler)

def handler(signum, frame):
    """Ugly handler to call PlayerAdapter's functions and test
    functionality. """

    if _paref is not None:
        count = 0

        print 'Which function should I call?'
        for f in _cmdlist:
            #this is the ugliest thing i've ever written
            print '[', count, ']', f.im_func.__str__().split()[1]

            count = count + 1

        b = int(raw_input('Choice: '))
        if b >= 0 and b < _cmdlist.__len__():
            #TODO ask for parameters
            gobject.idle_add(_cmdlist[b])
        else:
            print 'Invalid function'

