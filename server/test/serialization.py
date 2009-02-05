# -*- coding: UTF-8 -*-
import unittest
import logging

import remuco
import data
import serial

class TestSerialization(unittest.TestCase):
    
    def test_player_state(self):
        
        
        p1 = data.PlayerState()
        p1.set_playback(remuco.PLAYBACK_PLAY)
        p1.set_volume(34)
        p1.set_position(2345)
        p1.set_queue(False)
        p1.set_repeat(True)
        p1.set_shuffle(False)
        
        ba = serial.pack(p1)
        
        self.failIf(not ba)
        
        p2 = data.PlayerState()
        
        serial.unpack(p2, ba)
        
        self.assertEqual(p1, p2)

    def test_song(self):

        
        info_1 = { "eins" : "I", "zwei" : "II", "drei" : "III" }
        info_2 = { "öns" : "1", "zwö" : "2", "drä" : "3", "nix" : "" }
        
        s1 = data.Plob()
        s1.set_id("id1")
        s1.set_info(info_1)
        s1.set_img("/panama/Photos/2008/10.Misc/img_0839.jpg")
        
        ba1 = serial.pack(s1)
        self.failIf(ba1 is None)

        s1c = data.Plob()
        ok = serial.unpack(s1c, ba1)
        self.failIf(not ok)

        self.assertEqual(s1, s1c)

        s2 = data.Plob()
        s2.set_id("id2")
        s2.set_info(info_2)
        s2.set_img(None)
        
        ba2 = serial.pack(s2)
        self.failIf(ba2 is None)
        
        s2c = data.Plob()
        ok = serial.unpack(s2c, ba2)
        self.failIf(not ok)
        
        self.assertEqual(s2, s2c)
        
#    def __init__(self):
#        pass

if __name__ == '__main__':
    
    #logging.basicConfig(level=logging.DEBUG)
    
    #t = TestSerialization()
    #t.test_song()
    
    unittest.main()
        