#!/usr/bin/python

from xml.dom import minidom
import remuco
import gobject
import sys
import traceback
import signal
from dcopext import DCOPClient, DCOPApp
import fileinput
import re

###############################################################################

priv_global = None

###############################################################################

class PlayerProxyPriv:
    """This class is just a box for our private data."""
    
    def __init__(self):
        
        self.shutdown_in_progress = False
        
        # init some fields
        self.ml = None
        self.server = None
        self.playlist_pids = []
        self.playlist_xml_items = []
        
        self.repeat = False    # see gcb_tick_repeat_shuffle()
        self.shuffle = False   # see gcb_tick_repeat_shuffle()

        # init amarok dcop connection
        self.client = DCOPClient()
        self.client.attach()
        self.amarok = DCOPApp('amarok', self.client)
        
        # regex to detect Singal Of Interest
        # see http://amarok.kde.org/wiki/Script-Writing_HowTo
        self.rx_SOI = re.compile("^(engineStateChange: .+)|(trackChange)|(volumeChange: [0-9]+)|(playlistChange: .+)")
        
        # regex to detect pids of streams
        self.rx_stream_pid = re.compile("^[a-zA-Z]+://.+")
        
###############################################################################
#
# Private/Misc Functions
#
###############################################################################

def __connected(priv):
    """Check if there is a working connection to Amarok."""
    try:
        ok, vol = priv.amarok.player.getVolume()
    except:
        ok = False
    if not ok:
        remuco.log_noise("not connected") 
    return ok

def __updatePlaylist(priv):
    """Update the internal playlist.
    
    Request amarok to store the current playlist in an external file. Then we
    read this file to create a list of PIDs (global var 'playlist_pids') and a
    list of xml items (global var 'playlist_xml_items'). 
    """
    priv.playlist_pids = []
    
    ok, playlist_file = priv.amarok.playlist.saveCurrentPlaylist()
    
    remuco.log_noise("Current playlist in : %s" % playlist_file)
    document = minidom.parse(playlist_file)
    priv.playlist_xml_items = document.getElementsByTagName("item")
    
    remuco.log_debug("append pids")

    for item in priv.playlist_xml_items:
        remuco.log_noise("type: %s" % type(item.attributes["uniqueid"].value))
        pid = item.attributes["uniqueid"].value
        if not pid or len(pid) == 0: # this seems to be a stream
            pid = item.attributes["url"].value
        priv.playlist_pids.append(pid)
        remuco.log_debug("append pid %s" % pid)

def __get_plob_from_url(priv, url):
    """Get plob meta data via an URL.
    
    Function to get meta data about a plob which has no 'uniqueid' and which
    is therefore not stored in the database. This is valid for e.g. radio
    streams. In this case we just have an URL and try to get the meta data
    via the amarok dcop functions which give info about the current song or
    via the current playlist.
    """
    
    def tag_val(tag):
        try:
            val = item.getElementsByTagName(tag)[0].lastChild.nodeValue
            return val.encode("utf8")
        except AttributeError:
            return ""
        except IndexError:
            return ""
    
    # if 'url' is the current active song, stream .. then its easy:
    
    if url == priv.amarok.player.encodedURL()[1]:

        plob = {remuco.PLOB_META_ARTIST : priv.amarok.player.artist()[1], \
                remuco.PLOB_META_TITLE : priv.amarok.player.title()[1], \
                remuco.PLOB_META_RATING : priv.amarok.player.rating()[1], \
                remuco.PLOB_META_GENRE : priv.amarok.player.genre()[1], \
                remuco.PLOB_META_YEAR : priv.amarok.player.year()[1], \
                remuco.PLOB_META_COMMENT : priv.amarok.player.comment()[1], \
                remuco.PLOB_META_ALBUM : priv.amarok.player.album()[1], \
                remuco.PLOB_META_BITRATE : priv.amarok.player.bitrate()[1], \
                remuco.PLOB_META_TRACK : priv.amarok.player.track()[1], \
                remuco.PLOB_META_LENGTH : priv.amarok.player.trackTotalTime()[1], \
                }
    
        __add_img_to_plob(priv, plob)
        
        return plob
    
    # hm, check if the url is in the current playlist, then its still easy:
    
    for item in priv.playlist_xml_items:
        
        if item.attributes["url"].value == url:
            
            plob = {remuco.PLOB_META_ARTIST : tag_val("Artist"), \
                    remuco.PLOB_META_TITLE : tag_val("Title"), \
                    remuco.PLOB_META_RATING : tag_val("Rating"), \
                    remuco.PLOB_META_GENRE : tag_val("Genre"), \
                    remuco.PLOB_META_YEAR : tag_val("Year"), \
                    remuco.PLOB_META_COMMENT : tag_val("Comment"), \
                    remuco.PLOB_META_ALBUM : tag_val("Album"), \
                    remuco.PLOB_META_BITRATE : tag_val("Bitrate"), \
                    remuco.PLOB_META_TRACK : tag_val("Track"), \
                    remuco.PLOB_META_LENGTH : tag_val("Length") \
                    }
    
            __add_img_to_plob(priv, plob)
            
            return plob
    
    # damn, no way to get meta data for the url:
    
    remuco.log_warn("cannot get info about the plob with url %s" % url)
    
    return { PLOB_META_TITLE : "unknown" }

def __add_img_to_plob(priv, plob):
    
    if plob[remuco.PLOB_META_ALBUM] == priv.amarok.player.album()[1] and \
       plob[remuco.PLOB_META_ARTIST] == priv.amarok.player.artist()[1]:
        
        remuco.log_debug("get image of cap");
        ok, img = priv.amarok.player.coverImage()
        if ok and not img.endswith("nocover.png"):
            plob[remuco.PLOB_META_ART] = img
    
#    else: # this does not work because the path we get here is relative
#    
#        remuco.log_debug("get image of any plob");
#
#        query = "SELECT " + \
#            "path FROM images WHERE artist = \"%s\" AND album = \"%s\"" \
#            % (plob[remuco.PLOB_META_ARTIST], plob[remuco.PLOB_META_ALBUM])
#    
#        result = __query(priv, query)
#        
#        if not result or len(result) == 0: return
#        
#        names = ( "front", "cover", "album", "folder" )
#        types = ( "png", "jpg" )
#        
#        img = result[0] # fallback
#        
#        for type in types:
#            for name in names:
#                for file in result:
#                    remuco.log_debug("check file %s" % file)
#                    if file.lower().endswith("%s.%s" % (name, type)):
#                        remuco.log_debug("use %s" % file)
#                        img = file
#    
#        remuco.log_debug("hallo")
#        plob[remuco.PLOB_META_IMG] = img

def __fix_query(priv, table, column, left, right, default):
    """Wrapper for a query for a single value.
    
    Does a query in 'table' for the value in 'column' where the column 'left'
    has the value 'right'. If the query returns no ore more than one result,
    'default' will be returnd, other wise the single returned result value.
    """
    result = __query(priv, "SELECT %s FROM %s WHERE %s = \"%s\"" % \
                     (column, table, left, right))
    
    if (not result) or (len(result) != 1):
        return default
    else:
        return result[0]

def __query(priv, query):
    
    remuco.log_debug("do query \"%s\"" % query)

    ok, result = priv.amarok.collection.query(str(query))
    
    if not ok:
        remuco.log_error("collection query failed")
        return None
    
    remuco.log_debug("query done: %s" % result)
    
    return result
        
def __convert_pid_to_url(priv, pid):
    """Get the url for a PID.
    
    'pid' is what Amarok stores as uniqueid.
    """
    return __fix_query(priv, "uniqueid", "url", "uniqueid", pid, None)

def acb_signal(src, cond, priv):
    """Amarok signal callback function."""
    
    remuco.log_debug("signal from amarok")
    
    try:
        input = src.readline()
        
        if len(input) == 0:
            remuco.log_debug("EOF on stdin")
            if not priv.shutdown_in_progress:
                priv.shutdown_in_progress = True
                remuco.down(priv.server)
            return False
        
        remuco.log_debug("amarok says: %s" % input)
        
        if priv.rx_SOI.match(input) != None:
            remuco.log_debug("notify server")
            remuco.notify(priv.server)
            
    except:
        remuco.log_error("exception: %s" % traceback.format_exc())

    remuco.log_debug("acb_signal done")
    return True

def gcb_tick_repeat_shuffle(priv):
    """Timer callback function to periodically check the repeat and shuffle
    state.
       
    These values get requested via a timer function because there is no signal
    for them.
    """
    
    if not __connected(priv):
        return True

    ###### repeat ######
    
    repeat = priv.amarok.player.repeatPlaylistStatus()[1]
    repeat |= priv.amarok.player.repeatTrackStatus()[1]

    if repeat != priv.repeat:
        priv.repeat = repeat
        remuco.log_debug("repeat mode changed")
        remuco.notify(priv.server)
    
    # Amarok has also some kind of album repeat, but there is no dcop function
    # to check if this is enabled
    
    ###### shuffle ######

    shuffle = priv.amarok.player.randomModeStatus()[1]
    
    if shuffle != priv.shuffle:
        priv.shuffle = shuffle
        remuco.log_debug("shuffle mode changed")
        remuco.notify(priv.server)

    return True

###############################################################################
#
# Callback Functions for Remuco Server
#
###############################################################################

def rcb_synchronize(priv, ps):
    
    remuco.log_debug("rcb_synchronize called")

    if not __connected(priv):
        ps.pbs = remuco.PS_PBS_OFF
        ps.cap_pid = None
        ps.playlist = []
        return
    
    ###### playback state ######
    
    ok, state = priv.amarok.player.status()
    if state == 0:
        ps.pbs = remuco.PS_PBS_STOP
    elif state == 1:
        ps.pbs = remuco.PS_PBS_PAUSE
    elif state == 2:
        ps.pbs = remuco.PS_PBS_PLAY
    else:
        remuco.log_warn("unknown pbs: %i" % state)
        ps.pbs = remuco.PS_PBS_STOP

    ###### volume ######
    
    ok, ps.volume = priv.amarok.player.getVolume()
    
    ###### flags ######

    ps.flags = 0
    
    if priv.repeat:
        ps.flags |= remuco.PS_FLAG_REPEAT
    
    if priv.shuffle:
        ps.flags = remuco.PS_FLAG_SHUFFLE

    ###### playlist position and current plob pid ######
    
    __updatePlaylist(priv)
    
    ps.playlist = priv.playlist_pids
    
    ok, ps.cap_pos = priv.amarok.playlist.getActiveIndex()
    if ps.cap_pos == -1:
        ps.cap_pos == 0
    else:
        ps.cap_pid = priv.playlist_pids[ps.cap_pos]
        
    remuco.log_debug("rcb_synchronize done")
    
    return

def rcb_get_plob(priv, pid):
    
    remuco.log_debug("rcb_get_plob(%s) called" % pid)
    
    if priv.rx_stream_pid.match(pid) != None:
        # is a stream, won't find info about it in the databse, so:
        remuco.log_debug("stream url")
        return __get_plob_from_url(priv, pid)
    
    url = __fix_query(priv, "uniqueid", "url", "uniqueid", pid, None)
    
    if not url:
        remuco.log_warn("could not get url for pid")
        return { remuco.PLOB_META_TITLE : "unknown" }
    
    query = "SELECT " + \
        "album, artist, bitrate, comment, genre, length, title, track, year" + \
        " FROM tags WHERE url = \"%s\"" % url
    
    result = __query(priv, query)
    
    if (not result or len(result) != 9):
        remuco.log_error("query '%s' has bad result: '%s'" % (query, result))
        return None
    else:
        remuco.log_debug("plob query successfull")
        
    plob = {remuco.PLOB_META_ALBUM : result[0], 
            remuco.PLOB_META_ARTIST : result[1], \
            remuco.PLOB_META_BITRATE : result[2], \
            remuco.PLOB_META_COMMENT : result[3], \
            remuco.PLOB_META_GENRE : result[4], \
            remuco.PLOB_META_LENGTH : result[5], \
            remuco.PLOB_META_TITLE : result[6], \
            remuco.PLOB_META_TRACK : result[7], \
            remuco.PLOB_META_YEAR : result[8] \
            }
    
    plob[remuco.PLOB_META_ALBUM] = \
        __fix_query(priv, "album", "name", "id", plob[remuco.PLOB_META_ALBUM], "")
    
    plob[remuco.PLOB_META_ARTIST] = \
        __fix_query(priv, "artist", "name", "id", plob[remuco.PLOB_META_ARTIST], "")

    plob[remuco.PLOB_META_RATING] = \
        __fix_query(priv, "statistics", "rating", "url", url, "0")

    plob[remuco.PLOB_META_GENRE] = \
        __fix_query(priv, "genre", "name", "id", plob[remuco.PLOB_META_GENRE], "")
    
    plob[remuco.PLOB_META_YEAR] = \
        __fix_query(priv, "year", "name", "id", plob[remuco.PLOB_META_YEAR], "")
    
    __add_img_to_plob(priv, plob)
    
    remuco.log_debug("added image")
    
    remuco.log_debug("return plob %s" % url)

    return plob

#def rcb_get_library(priv):
#    remuco.log_debug("rcb_get_library called")
#    return (['pl1', 'pl2'], ['dub', 'jazz'], [0 , 0])

#def rcb_get_ploblist(priv, plid):
#    remuco.log_debug("rcb_get_ploblist(%s) called" % plid)
#    return ['333', '4444']

def rcb_notify(priv, event):
    
    remuco.log_debug("rcb_notify called ")

    if event == remuco.SERVER_EVENT_DOWN:
        remuco.log_debug("EVENT: server is down now")        
        priv.ml.quit()
    elif even == remuco.SERVER_EVENT_ERROR:
        remuco.log_debug("EVENT: server crashed")        
        remuco.down(priv.server)
    else:
        print "ERROR: unknown event"
        sys.exit()
    return

#def rcb_play_ploblist(priv, plid):
#    remuco.log_debug("rcb_play_ploblist called")
#    return

#def rcb_search(priv, plob):
#    remuco.log_debug("rcb_search called")
#    return

def rcb_simple_control(priv, cmd, param):
    
    remuco.log_debug("rcb_simple_control(%i, %i) called" % (cmd, param))
    
    if not __connected(priv): return

    try:
        if cmd == remuco.SCTRL_CMD_STOP:
            priv.amarok.player.stop()
        elif cmd == remuco.SCTRL_CMD_PLAYPAUSE:
            priv.amarok.player.playPause()
        elif cmd == remuco.SCTRL_CMD_NEXT:
            priv.amarok.player.next()
        elif cmd == remuco.SCTRL_CMD_PREV:
            priv.amarok.player.prev()
        elif cmd == remuco.SCTRL_CMD_VOLUME:
            priv.amarok.player.setVolume(param)
        elif cmd == remuco.SCTRL_CMD_RESTART:
            priv.amarok.player.stop()
            priv.amarok.playlist.playByIndex(0)
            priv.amarok.player.playPause()
        elif cmd == remuco.SCTRL_CMD_JUMP:
            priv.amarok.playlist.playByIndex(param - 1)
        elif cmd == remuco.SCTRL_CMD_RATE:
            priv.amarok.player.setRating(param)
        elif cmd == remuco.SCTRL_CMD_SEEK:
            priv.amarok.player.seekRelative(param)
        else:
            logging.warning("command %d not supported", cmd)
    except:
        remuco.log_error(str(sys.exc_info()))
    else:
        remuco.log_debug("sctrl processed")

def rcb_update_plob(priv, plob):
    remuco.log_debug("rcb_update_plob called")

def rcb_update_ploblist(priv, plid, pids):
    remuco.log_debug("rcb_update_ploblist called")
    return

def sighandler(signum, frame):
    remuco.log_debug("got signal %i" % signum)
    if not priv_global.shutdown_in_progress:
        priv_global.shutdown_in_progress = True
        remuco.down(priv_global.server)

def main():

    global priv_global
    
    priv = PlayerProxyPriv()
    
    priv_global = priv
    
    ###### misc initializations ######

    signal.signal(signal.SIGINT, sighandler)
    signal.signal(signal.SIGTERM, sighandler)
    
    priv.ml = gobject.MainLoop()
    
    ###### remuco related ######
    
    callbacks = remuco.PPCallbacks()
    callbacks.snychronize = rcb_synchronize
    callbacks.get_plob = rcb_get_plob
    callbacks.notify = rcb_notify
    callbacks.simple_control = rcb_simple_control
    
    descriptor = remuco.PPDescriptor()
    descriptor.player_name = "test"
    descriptor.max_rating_value = 10
    descriptor.supports_playlist = True
    descriptor.supports_playlist_jump = True
    descriptor.supports_seek = True
    
    try:
        priv.server = remuco.up(descriptor, callbacks, priv)
    except:
        remuco.log_error("error starting server: %s" % str(sys.exc_info()))
        sys.exit()
    
    remuco.notify(priv.server)

    ###### amarok related ######
    
    # catch signals form amarok
    gobject.io_add_watch(sys.stdin, gobject.IO_IN, acb_signal, priv)
    
    # periodically check for change in repeat/shuffle mode
    gobject.timeout_add(2000, gcb_tick_repeat_shuffle, priv, priority = gobject.PRIORITY_LOW)
    
    ###### here we go ######

    remuco.log_debug("run mainloop")
    
    priv.ml.run()
    
    remuco.log_debug("back from mainloop .. bye")

    
if __name__ == "__main__":
    
    main()
    