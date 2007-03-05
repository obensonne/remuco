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
from rem import *
import pydcop, pcop
import amapp

def rem_pp_init():
    logging.basicConfig(level=logging.DEBUG, \
                        format='%(asctime)s [ %(levelname)s ] %(message)s')
    logging.debug("amarok connection: %r", amapp.app)
    logging.debug("initialized")
    return 0

def rem_pp_process_cmd(code, param):
    logging.debug("process command %d with param %d", code, param)
    ret = rem_pp_check_amarok_connection()
    if ret <= 0:
        return ret
    # ok, from here amarok is up and connection works
    try:
        if code == REM_PC_CMD_STOP:
            amapp.app.player.stop()
        elif code == REM_PC_CMD_PLAY_PAUSE:
            amapp.app.player.playPause()
        elif code == REM_PC_CMD_NEXT:
            amapp.app.player.next()
        elif code == REM_PC_CMD_PREV:
            amapp.app.player.prev()
        elif code == REM_PC_CMD_VOLUME:
            amapp.app.player.setVolume(param)
        elif code == REM_PC_CMD_RESTART:
            amapp.app.player.stop()
            self.app.playlist.playByIndex(0)
            amapp.app.player.playPause()
        elif code == REM_PC_CMD_JUMP:
            amapp.app.playlist.playByIndex(param)
        elif code == REM_PC_CMD_RATE:
            amapp.app.player.setRating(param)
        else:
            logging.debug("command %d not supported", code)
    except:
        logging.warning("cmd %d with param %d caused an error", code, param)
    else:
        logging.debug("cmd processed")
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
    ps = [ REM_PS_STATE_OFF, 50, 0, 1, 0, [ ] ]
    
    ret = rem_pp_check_amarok_connection()
    if ret <= 0:
        return ps;

    state = amapp.app.player.status()
    if state == 0:
        state = REM_PS_STATE_STOP
    elif state == 1:
        state = REM_PS_STATE_PAUSE
    elif state == 2:
        state = REM_PS_STATE_PLAY
    else:
        state = REM_PS_STATE_PROBLEM

    volume = amapp.app.player.getVolume()
    #if volume < 0: volume = 0
    #if volume > 100: volume = 100
    
    #x = amapp.app.player.getRating()
    #rating = "%d/10" % x
    
    if amapp.app.player.repeatTrackStatus():
        repeat = 1
    else:
        repeat = 0
    if amapp.app.player.randomModeStatus():
        shuffle = 1
    else:
        shuffle = 0
    
    ps = [ state, volume, repeat, shuffle, 1, [ "1", "A1", "645" ] ]
    
    return ps

def rem_pp_get_song(sid):
    """
    This function shall return the meta data information of the song with id
    'sid' as a dictionary. The dictionary may contain only strings.
    """
    
    """ Example song to return: """
    
    song = {REM_TAG_NAME_ARTIST : "Sade", \
            REM_TAG_NAME_TITLE : "Smooth Operator", \
            REM_TAG_NAME_RATING : "4/5", \
            REM_TAG_NAME_UID : sid }    
    return song
    
def rem_pp_check_amarok_connection():
    logging.debug("check amarok connection")
    if not amapp.app:
        logging.debug("no connection, try to connect..")
        amapp.app = pydcop.anyAppCalled("amarok")
    if not amapp.app:
        logging.debug("could not connect, amarok seems to be down")
        return 0
    try:
        logging.debug("amarok connection is there, test it...")
        amapp.app.player.getVolume()
        logging.debug("connection works")
        return 1
    except:
        logging.warning("error in amarok connection." + \
                        "discard connection and try to reconnect ..")
        amapp.app = None
        amapp.app = pydcop.anyAppCalled("amarok")
        if not amapp.app:
            logging.debug("could not connect, amarok seems to be down")
            return 0
        logging.debug("reconnected")
        return 1
