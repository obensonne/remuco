'''
Created on Mar 10, 2009

@author: mondai
'''
import unittest

import gobject

from remuco.data import PlayerInfo
from remuco.net import WifiServer, BluetoothServer


class ServerTest(unittest.TestCase):

    def setUp(self):
        
        self.__ml = gobject.MainLoop()
        self.__pi = PlayerInfo("xxx", 0, 0, None)

    def test_wifi(self):
        
        s = WifiServer([], self.__pi, None, 0)
        
        gobject.timeout_add(2000, self.__stop, s)
        
        self.__ml.run()

    def test_bluetooth(self):
        
        s = BluetoothServer([], self.__pi, None, 0)
        
        gobject.timeout_add(2000, self.__stop, s)
        
        self.__ml.run()

    def __stop(self, s):
        
        s.down()
        self.__ml.quit()
        
if __name__ == "__main__":
    
    unittest.main()