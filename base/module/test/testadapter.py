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

import unittest

import gobject

import sys

import remuco.log
from remuco import PlayerAdapter


class AdapterTest(unittest.TestCase):

    def setUp(self):
        
        self.__ml = gobject.MainLoop()
        
        ia = remuco.ItemAction("ia_l", multiple=True)

        logarg = "--remuco-log-stdout"
        if not logarg in sys.argv:
            sys.argv.append(logarg)

        self.__pa = PlayerAdapter("unittest", playback_known=False,
            volume_known=True, repeat_known=True, shuffle_known=False,
            progress_known=True, max_rating=3, mime_types=["audio"], poll=1,
            file_actions=[ia])
        
        #self.__pa.config.log_level = remuco.log.DEBUG
        #self.__pa.config.log_level = remuco.log.INFO
        self.__pa.config.log_level = remuco.log.WARNING

    def test_adapter(self):

        self.__pa.start()
        
        gobject.timeout_add(4000, self.__stop)
        
        self.__ml.run()

    def __stop(self):
        
        self.__pa.stop()
        self.__ml.quit()
        

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.test_adapter']
    unittest.main()