# -*- coding: UTF-8 -*-

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

import remuco
from remuco import data
from remuco import serial

class _SerialzableClass(remuco.serial.Serializable):
    
    def __init__(self):

        self.b = None
        self.y = None
        self.n = None
        self.i = None
        self.l = None
        
        self.s1 = None
        self.s2 = None
        
        self.ba = None
        self.ya = None
        self.na = None
        self.ia = None
        self.la = None
        
        self.sa1 = None
        self.sa2 = None
        
    def __eq__(self, o):
        
        if self.b != o.b:
            print("b differs")
            return False
        if self.y != o.y:
            print("y differs")
            return False
        if self.n != o.n:
            print("n differs")
            return False
        if self.i != o.i:
            print("i differs")
            return False
        if self.l != o.l:
            print("l differs")
            return False
        
        if self.s1 != o.s1:
            print("s1 differs")
            return False
        if self.s2 != o.s2:
            print("s2 differs")
            return False
        
        if self.ba != o.ba:
            print("ba differs")
            return False
        if self.ya != o.ya:
            print("ya differs")
            return False
        if self.na != o.na:
            print("na differs")
            return False
        if self.ia != o.ia:
            print("ia differs")
            return False
        if self.la != o.la:
            print("la differs")
            return False
        
        if self.sa1 != o.sa1:
            print("sa1 differs")
            return False
        if self.sa2 != o.sa2:
            print("sa2 differs")
            return False
        
        return True
    
    def init(self):
        
        self.b = True
        self.y = 55
        self.n = 4 << 10
        self.i = 4 << 20
        self.l = 3 << 40
        
        self.s1 = "dfödas"
        self.s2 = ""
        
        self.ba = [ False, True, False ]
        self.ya1 = "bytes as string"
        self.ya2 = [ 1, 127, -128 ]
        self.na = [ 2 << 10 ]
        self.ia1 = [ 1 << 20, 2 << 20 ]
        self.ia2 = None
        self.la = [ 5 << 40, 6 << 50 ]
        
        self.sa1 = [ "1", "2éü+", None , "" ]
        self.sa2 = []
        
    def get_fmt(self):
        return (serial.TYPE_B, serial.TYPE_Y, serial.TYPE_N, serial.TYPE_I, serial.TYPE_L,
                serial.TYPE_S, serial.TYPE_S,
                serial.TYPE_AB, serial.TYPE_AY, serial.TYPE_AY, serial.TYPE_AN, serial.TYPE_AI, serial.TYPE_AI, serial.TYPE_AL,
                serial.TYPE_AS, serial.TYPE_AS)
        
    def get_data(self):
        return (self.b, self.y, self.n, self.i, self.l,
                self.s1, self.s2,
                self.ba, self.ya1, self.ya2, self.na, self.ia1, self.ia2, self.la,
                self.sa1, self.sa2)

    def set_data(self, data):
        self.b, self.y, self.n, self.i, self.l, \
            self.s1, self.s2, \
            self.ba, self.ya1, self.ya2, self.na, self.ia1, self.ia2, self.la, \
            self.sa1, self.sa2 = data

class SerializationTest(unittest.TestCase):
    
    def __serialize_and_dump(self, ser):
        
        print("--> serializable:\n%s" % ser)
        
        bindata = serial.pack(ser)
        
        print("--> binary data:")
        out = ""
        counter = 0
        for byte in bindata:
            counter += 1
            out = "%s %02X" % (out, ord(byte))
            if counter % 32 == 0:
                out = "%s\n" % out
        print(out)
        
        return bindata
    
    def test_serialize_playerinfo(self):
        
        #print("")
        
        ia = remuco.ItemAction("ia_l", multiple=True)
        pi = data.PlayerInfo("dings", 123, 4, [ia], ["sm1", "sm2"])
        
        #self.__serialize(pi)
        serial.pack(pi)
        
    def test_serialize_itemlist(self):
        
        #print("")
        
        path = [ "path", "to" ]
        nested = [ "n1", "n2" ]
        ids = ["id1", "id2", "id3" ]
        names = [ "na1", "na2", "na3" ]
        
        ia1 = remuco.ItemAction("ia1_l", multiple=True)
        ia2 = remuco.ItemAction("ia2_l", multiple=False)
        ias = [ ia1, ia2 ]
        
        la1 = remuco.ListAction("la1_l")
        las = [ la1 ]
        
        il = data.ItemList(path, nested, ids, names, 0, 1, 2, ias, las)
        
        #self.__serialize(il)
        serial.pack(il)
        
        # ---------------------------------------------------------------------
        
        path = [ ]
        nested = [ ]
        ids = None
        names = [ "na1", "na2", "na3" ]
        
        ia1 = remuco.ItemAction("ia1_l", multiple=True)
        ia2 = remuco.ItemAction("ia2_l", multiple=False)
        ias = [ ia1, ia2 ]
        
        #la1 = remuco.ListAction("la1_l")
        #las = [ la1 ]
        las = None
        
        il = data.ItemList(path, nested, ids, names, 0, 1, 2, ias, las)
        
        #self.__serialize(il)
        serial.pack(il)
        
    def test_serialize_deserialize(self):
        
        sc1 = _SerialzableClass()
        sc1.init()
        
        #bindata = self.__serialize(sc1)
        bindata = serial.pack(sc1)
        
        self.assertFalse(bindata is None)
        
        sc2 = _SerialzableClass()
        
        serial.unpack(sc2, bindata)
        
        self.assertFalse(sc2.sa1 is None)
        self.assertTrue(len(sc2.sa1) > 2)
        self.assertEquals(sc2.sa1[2], "") # None becomes empty string
        sc2.sa1[2] = None
        
        self.assertEquals(sc2.ia2, []) # None becomes empty list
        sc2.ia2 = None
        
        self.assertEquals(sc1, sc2)
        
        sc3 = serial.unpack(_SerialzableClass, "%strash" % bindata)
        self.assertTrue(sc3 is None)
        
        sc3 = serial.unpack(_SerialzableClass, "df")
        self.assertTrue(sc3 is None)

        sc3 = serial.unpack(_SerialzableClass(), "dfäsadfasd")
        self.assertTrue(sc3 is None)

        sc3 = serial.unpack(_SerialzableClass, "")
        self.assertTrue(sc3 is None)
        
        sc3 = serial.unpack(_SerialzableClass(), None)
        self.assertTrue(sc3 is None)
        
if __name__ == '__main__':
    
    unittest.main()
