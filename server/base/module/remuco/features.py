# --- 'is known' features ---

FT_KNOWN_VOLUME = 1 << 0
FT_KNOWN_REPEAT = 1 << 1
FT_KNOWN_SHUFFLE = 1 << 2
FT_KNOWN_PLAYBACK = 1 << 3
FT_KNOWN_PROGRESS = 1 << 4

# --- control features ---

FT_CTRL_PLAYBACK = 1 << 9
FT_CTRL_VOLUME = 1 << 10
FT_CTRL_SEEK = 1 << 11
FT_CTRL_TAG = 1 << 12
FT_CTRL_CLEAR_PL = 1 << 13
FT_CTRL_CLEAR_QU = 1 << 14
FT_CTRL_RATE = 1 << 15
FT_CTRL_REPEAT = 1 << 16
FT_CTRL_SHUFFLE = 1 << 17
FT_CTRL_NEXT = 1 << 18
FT_CTRL_PREV = 1 << 19
FT_CTRL_FULLSCREEN = 1 << 20
        
# --- request features ---

FT_REQ_ITEM = 1 << 25
FT_REQ_PL = 1 << 26
FT_REQ_QU = 1 << 27
FT_REQ_MLIB = 1 << 28

# --- misc features

FT_SHUTDOWN = 1 << 30

