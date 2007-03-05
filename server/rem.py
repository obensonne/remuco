# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.4.0"
__copyright__ = "Copyright (c) 2007 Christian Bünnig"
__license__ = "GPL2"

__doc__ = """
Some Remuco specific constants. See also rem.h.
"""

""" Player state """

REM_PS_STATE_STOP = 0
REM_PS_STATE_PLAY = 1
REM_PS_STATE_PAUSE = 2
REM_PS_STATE_PROBLEM = 3
REM_PS_STATE_OFF = 4
REM_PS_STATE_ERROR = 10
REM_PS_STATE_SRVOFF = 20

REM_PS_PL_POS_NONE = 0

""" Music meta data (Tags) """

REM_TAG_NAME_UID = "uid"        # the song id
REM_TAG_NAME_TITLE = "title"
REM_TAG_NAME_ARTIST = "artist"
REM_TAG_NAME_ALBUM = "album"
REM_TAG_NAME_GENRE = "genre"
REM_TAG_NAME_YEAR = "year"
REM_TAG_NAME_RATING = "rating"   # format: 'n/m'
                                 # n: rating (-1: unrated)
                                 # m: max possible rating
REM_TAG_NAME_COMMENT = "comment"
REM_TAG_NAME_BITRATE = "bitrate"
REM_TAG_NAME_LENGTH = "length"   # in s
REM_TAG_NAME_TRACK = "track"

REM_TAG_VAL_RATING_UNRATED = "-1"

REM_MAX_TAGS = 20

""" Player control commands """

REM_PC_CMD_VOLUME = 1
REM_PC_CMD_PLAY_PAUSE = 3
REM_PC_CMD_NEXT = 4
REM_PC_CMD_PREV = 5
REM_PC_CMD_STOP = 6
REM_PC_CMD_RESTART = 7
REM_PC_CMD_JUMP = 8
REM_PC_CMD_RATE = 9
REM_PC_CMD_LOGOFF = 10
REM_PC_CMD_NOOP = 99


