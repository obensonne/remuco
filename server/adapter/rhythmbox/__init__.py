import rb, rhythmdb

from remythm.rba import RhythmboxAdapter

# =============================================================================
# plugin
# =============================================================================

class RemucoPlugin(rb.Plugin):
    
    def __init__(self):
        
        rb.Plugin.__init__(self)
        
        self.__rba = None
        
    def activate(self, shell):
        
        if self.__rba is not None:
            return
        
        print("create RhythmboxAdapter")
        self.__rba = RhythmboxAdapter()
        print("RhythmboxAdapter created")

        print("start RhythmboxAdapter")
        self.__rba.start(shell)
        print("RhythmboxAdapter started")
        
    def deactivate(self, shell):
    
        if self.__rba is None:
            return
        
        print("stop RhythmboxAdapter")
        self.__rba.stop()
        print("RhythmboxAdapter stopped")
        
        self.__rba = None
        
