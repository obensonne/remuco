# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.4.0"
__copyright__ = "Copyright (c) 2007 Christian Bünnig"
__license__ = "GPL2"

__doc__ = """
Remuco python player proxy for Amarok.

Based on the Amarok player adapter for Remuco 0.3 from Iván Campaña
<ivan.campana@gmail.com>
"""

import logging
from rem import *
from xml.dom import minidom
import pydcop, pcop
#import amapp
import os
import pputil

def rem_pp_init():
    logging.basicConfig(level=logging.INFO, \
                format='%(asctime)s [ %(levelname)s ] amarok-pp : %(message)s')
    
    ret = os.system("dcopserver --serverid > /dev/null 2>&1")
    if ret != 0:
        logging.error("dcopserver is not running! Try to start Amarok first..")
        return -1
    rem_pp_check_amarok_connection()
    if not pputil.app:
        logging.info("amarok is down.")
    else:
        logging.info("connected to amarok")
    
    logging.debug("amarok connection: %r", pputil.app)
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
            pputil.app.player.stop()
        elif code == REM_PC_CMD_PLAY_PAUSE:
            pputil.app.player.playPause()
        elif code == REM_PC_CMD_NEXT:
            pputil.app.player.next()
        elif code == REM_PC_CMD_PREV:
            pputil.app.player.prev()
        elif code == REM_PC_CMD_VOLUME:
            pputil.app.player.setVolume(param)
        elif code == REM_PC_CMD_RESTART:
            pputil.app.player.stop()
            pputil.app.playlist.playByIndex(0)
            pputil.app.player.playPause()
        elif code == REM_PC_CMD_JUMP:
            pputil.app.playlist.playByIndex(param)
        elif code == REM_PC_CMD_RATE:
            pputil.app.player.setRating(param)
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
    
    ret = rem_pp_check_amarok_connection()
    if ret <= 0:
        return [ REM_PS_STATE_OFF, 50, 0, 0, REM_PS_PL_POS_NONE, [ ] ]

    state = pputil.app.player.status()
    if state == 0:
        state = REM_PS_STATE_STOP
    elif state == 1:
        state = REM_PS_STATE_PAUSE
    elif state == 2:
        state = REM_PS_STATE_PLAY
    else:
        state = REM_PS_STATE_PROBLEM

    volume = pputil.app.player.getVolume()
    
    if pputil.app.player.repeatPlaylistStatus():
        repeat = 1
    else:
        repeat = 0
    if pputil.app.player.randomModeStatus():
        shuffle = 1
    else:
        shuffle = 0
    
    pos = pputil.app.playlist.getActiveIndex()    
    if pos == -1: pos = REM_PS_PL_POS_NONE
    
    sidlist = rem_pp_get_sidlist()
    
    ps = [ state, volume, repeat, shuffle, pos, sidlist ]
    
    return ps

def rem_pp_get_song(sid):
    
    def tag_val(tag):
        try:
            val = song.getElementsByTagName(tag)[0].lastChild.nodeValue
            return str(val.encode("utf8"))
        except AttributeError:
            return ""
        except IndexError:
            return ""
    
    #logging.debug("songs: %s", pputil.songs)
    try:
        song = pputil.songs_iter.next()
    except StopIteration:
        logging.error("playlist iteration error (no more songs)")
        return None
    
    song = {REM_TAG_NAME_ARTIST : tag_val("Artist"), \
            REM_TAG_NAME_TITLE : tag_val("Title"), \
            REM_TAG_NAME_RATING : tag_val("Rating") + "/10", \
            REM_TAG_NAME_GENRE : tag_val("Genre"), \
            REM_TAG_NAME_YEAR : tag_val("Year"), \
            REM_TAG_NAME_COMMENT : tag_val("Comment"), \
            REM_TAG_NAME_ALBUM : tag_val("Album"), \
            REM_TAG_NAME_BITRATE : tag_val("Bitrate"), \
            REM_TAG_NAME_TRACK : tag_val("Track"), \
            REM_TAG_NAME_LENGTH : tag_val("Length"), \
            REM_TAG_NAME_UID : sid }
    
    return song
    
def rem_pp_check_amarok_connection():
    logging.debug("check amarok connection")
    if not pputil.app:
        logging.debug("no connection, try to connect..")
        pputil.app = pydcop.anyAppCalled("amarok")
    if not pputil.app:
        logging.debug("could not connect, amarok seems to be down")
        return 0
    try:
        logging.debug("amarok connection is there, test it...")
        # next: a test if the connection to amarok works
        pputil.app.player.getVolume()
        logging.debug("connection works")
        return 1
    except:
        logging.warning("error in amarok connection." + \
                        "discard connection and try to reconnect ..")
        pputil.app = None
        pputil.app = pydcop.anyAppCalled("amarok")
        if not pputil.app:
            logging.info("could not connect, amarok seems to be down")
            return 0
        logging.info("reconnected to amarok")
        return 1

def rem_pp_get_sidlist():
    
    sidlist = []
    
    playlist_file = pputil.app.playlist.saveCurrentPlaylist()
    
    logging.debug("Current playlist in : %s" % playlist_file)
    document = minidom.parse(playlist_file)
    pputil.songs = document.getElementsByTagName("item")
    
    for song in pputil.songs:
        sidlist.append(song.attributes["url"].value)
        #logging.debug("append sid %s", song.attributes["url"].value)
    
    # Init the songs iterator for later use in rem_pp_get_song(sid)
    pputil.songs_iter = pputil.songs.__iter__()
    
    return sidlist
