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
import inspect

_paref = None
_cmdlist = None

def setup(adapter):
    global _paref, _cmdlist

    _paref = adapter

    _cmdlist = [getattr(adapter, f) for f in dir(adapter)
                if f.startswith("ctrl_")]

    signal.signal(signal.SIGHUP, handler)

def handler(signum, frame):
    """Ugly handler to call PlayerAdapter's functions and test
    functionality. """

    signal.signal(signal.SIGHUP, signal.SIG_IGN) #ignore further SIGHUPs

    if _paref is not None:

        print('Which function should I call?')
        for count, f in enumerate(_cmdlist):
            parms, _, _, _ = inspect.getargspec(f)
            showparms = ''
            if parms.__len__() > 1:
                showparms = parms[1:] #ignore 'self'

            print('[%d] %s (%s)' % (count, f.__name__, showparms))

        try:
            command = raw_input('Choice: ').split(' ')
            idx = int(command[0])
            args = command[1:]

            #cast what seems to be integer
            for i,arg in enumerate(args):
                possible_number = arg

                if arg.startswith('-'): #negative? isdigit() will allow too
                    possible_number = arg[1:]

                if possible_number.isdigit(): args[i] = int(args[i])

            if idx >= 0 and idx < _cmdlist.__len__():
                gobject.idle_add(_cmdlist[idx], *args)
            else:
                print('Invalid function')
        except ValueError:
            pass

    signal.signal(signal.SIGHUP, handler) #be ready for the next calls
