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

from remuco.files import FileSystemLibrary


class FilesTest(unittest.TestCase):


    def setUp(self):
        pass


    def tearDown(self):
        pass


    def __test_path(self, fs, path, depth, limit):

        nested, ids, names = fs.get_level(path)
        
        #print("%spath   : %s" % (depth, path))
        #if nested:
        #    print("%snested : %s" % (depth, nested))
        #if ids:
        #    print("%sids    : %s" % (depth, ids))
        #if names:
        #    print("%snames  : %s" % (depth, names))
        
        if len(depth) == limit * 2:
            return
        
        if path is None:
            path = []
            
        for sub in nested:
            self.__test_path(fs, path + [sub], depth + "  ", limit)
        
    def test_files(self):
        
        #print("")
        
        fs = FileSystemLibrary(None, ["audio","video"], False, False, True)
        
        self.__test_path(fs, None, "", limit=0)
        self.__test_path(fs, [], "", limit=2)
        
        fs = FileSystemLibrary([ "/home", "/nonexistent" ],
            ["audio/mp3", "video/mp4", "application/x-iso9660-image"],
            True, False, True)
        
        self.__test_path(fs, [], "", limit=3)
        
if __name__ == "__main__":
    
    unittest.main()