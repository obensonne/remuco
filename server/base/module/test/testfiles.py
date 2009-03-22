'''
Created on Mar 10, 2009

@author: mondai
'''
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