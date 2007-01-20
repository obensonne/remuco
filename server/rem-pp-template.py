# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.4.0"
__copyright__ = "Copyright (c) 2007 Christian Bünnig"
__license__ = "GPL2"

__doc__ = """
Template module to use to implement a Remuco player proxy with Python.

The module's functions get called by the Remuco server. They are expected to
return immediately (or at least with low latency).
In principle no threading is necessary when implementing a player proxy.
If threading is needed it should get initialized in the init function.

More documentation about these functions can be found in rem-pp.h and
rem-pp-template.c.
"""

import logging
import rem

def rem_pp_init():
    logging.basicConfig(level=logging.DEBUG, \
                        format='%(asctime)s [ %(levelname)s ] %(message)s')
    logging.debug("initialized")
    return 0

def rem_pp_process_cmd(code, param):
    logging.debug("process command %d with param %d", code, param)
    return 0

def rem_pp_dispose():
    logging.debug("disposing")

def rem_pp_get_ps():
    """
    This function shall return the current music player state as a list.
    The list must be formatted as follows:
        [ state (int), volume (int), pl_repeat (int), pl_shuffle (int),
          pl_pos (int), song-ids (a nested list of strings) ]
    """
    
    """ Example player state to return: """
    ps = [ rem.REM_PS_STATE_OFF, 50, 0, 1, 1, [ "1", "A1", "645" ] ]
    
    return ps

def rem_pp_get_song(sid):
    """
    This function shall return the meta data information of the song with id
    'sid' as a dictionary. The dictionary may contain only strings.
    """
    
    """ Example song to return: """
    
    song = {rem.REM_TAG_NAME_ARTIST : "Sade", \
            rem.REM_TAG_NAME_TITLE : "Smooth Operator", \
            rem.REM_TAG_NAME_RATING : "4/5", \
            rem.REM_TAG_NAME_UID : sid }    
    return song

    
    

