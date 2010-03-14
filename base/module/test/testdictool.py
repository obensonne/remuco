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

from remuco import dictool

class DicToolTest(unittest.TestCase):


    def setUp(self):
        pass


    def tearDown(self):
        pass

    def test_it(self):
        
        keys = ("a", "c", "b")
        dic1 = { "a": "1:d", "b": "2", "d": "4", "c": "3" }
        dic3 = { "a": "1", "b": "2,0", "d": "4", "c": "3" }
        
        flat = dictool.dict_to_string(dic1)
        dic2 = dictool.string_to_dict(flat)
        assert dic1 == dic2
        
        flat = dictool.dict_to_string(dic1, keys=keys)
        assert flat == "a:1:d,c:3,b:2"
        dic2 = dictool.string_to_dict(flat)
        assert dic1 != dic2
        
        flat = dictool.dict_to_string(dic3, keys=keys)
        assert flat == "a:1,c:3,b:2_0"
        
        l1 = [dic1,dic2]
        dictool.write_dicts_to_file("/var/tmp/dictest", l1, comment="# hallo")
        l2 = dictool.read_dicts_from_file("/var/tmp/dictest")
        assert l1 == l2
        
        dictool.write_dicts_to_file("/var/tmp/dictest", [dic1,dic2,dic3,flat],
                                    comment="# hallo", keys=["a"])
        l = dictool.read_dicts_from_file("/var/tmp/dictest")
        
        l = dictool.read_dicts_from_file("/var/tmp/non_existent")
        
if __name__ == "__main__":
    
    unittest.main()