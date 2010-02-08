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

import unittest

import gobject

from remuco.data import PlayerInfo
from remuco.net import WifiServer, BluetoothServer
from remuco.config import Config


class ServerTest(unittest.TestCase):

    def setUp(self):
        
        self.__ml = gobject.MainLoop()
        self.__pi = PlayerInfo("xxx", 0, 0, None, ["1", "2"])
        self.__config = Config("unittest")

    def test_wifi(self):
        
        s = WifiServer([], self.__pi, None, self.__config)
        
        gobject.timeout_add(2000, self.__stop, s)
        
        self.__ml.run()

    def test_bluetooth(self):
        
        s = BluetoothServer([], self.__pi, None, self.__config)
        
        gobject.timeout_add(2000, self.__stop, s)
        
        self.__ml.run()

    def __stop(self, s):
        
        s.down()
        self.__ml.quit()
        
if __name__ == "__main__":
    
    unittest.main()