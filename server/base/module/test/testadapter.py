'''
Created on Mar 10, 2009

@author: mondai
'''
import unittest

import gobject

import sys

import remuco.log
from remuco import PlayerAdapter


class AdapterTest(unittest.TestCase):

    def setUp(self):
        
        self.__ml = gobject.MainLoop()
        
        ia = remuco.ItemAction("ia_l", "ia_h", multiple=True)

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