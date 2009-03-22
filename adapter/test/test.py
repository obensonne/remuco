'''
Test player adapter.
'''

import remuco
from remuco import log

ITEMS = {
         "ID1": { remuco.INFO_TITLE: "Song No. 1"},
         "ID2": { remuco.INFO_TITLE: "Song No. 2"},
         "ID3": { remuco.INFO_TITLE: "Song No. 3"},
         "ID4": { remuco.INFO_TITLE: "Song No. 4"},
         "ID5": { remuco.INFO_TITLE: "Song No. 5"},
         "ID6": { remuco.INFO_TITLE: "Song No. 6"},
         "ID7": { remuco.INFO_TITLE: "Song No. 7"},
         }

FA1 = remuco.ItemAction("A1")
FA2 = remuco.ItemAction("A2", multiple=True)


class Player(object):
    
    def __init__(self):
        
        self.playback = remuco.PLAYBACK_STOP
        self.progress = 0

class TestAdapter(remuco.PlayerAdapter):
    
    def __init__(self):
        
        super(TestAdapter, self).__init__("Test", file_actions=[FA1, FA2])
        self.__progress = 0
        
    def poll(self):
        
        log.debug("poll")
        
        self.update_playback(remuco.PLAYBACK_PLAY)
        self.__progress += 3
        #self.update_progress(self.__progress)
    
if __name__ == '__main__':
    
    ta = TestAdapter()
    
    mg = remuco.Manager(ta)
    
    mg.run()