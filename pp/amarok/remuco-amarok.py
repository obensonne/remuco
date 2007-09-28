#!/usr/bin/python

import sys
import logging
from xml.dom import minidom
import pydcop, pcop
import os
import signal
import commands
import thread
import time
import re

try:
    import remuco
except:
    print "The Python binding for the Remuco server library seems to be " \
        "missing or broken. See the following error output for details:\n %s" \
        % str(sys.exc_info()[1])
    sys.exit()

FEATURES = 0 \
    | remuco.FEATURE_PLAYLIST \
    | remuco.FEATURE_PLAYLIST_JUMP \
    | remuco.FEATURE_PLAYLIST_MODE_REPEAT_ALBUM \
    | remuco.FEATURE_PLAYLIST_MODE_REPEAT_ONE_PLOB \
    | remuco.FEATURE_PLAYLIST_MODE_REPEAT_PLAYLIST \
    | remuco.FEATURE_PLAYLIST_MODE_SHUFFLE \
    | remuco.FEATURE_PLOB_EDIT \
    | remuco.FEATURE_RATE \
    | remuco.FEATURE_LIBRARY

AMAROK_DIR = str ("%s/.kde/share/apps/amarok" % os.getenv("HOME"))
PL_STATIC_FILE = str("%s/playlistbrowser_save.xml" % AMAROK_DIR)
PL_STATIC_ELEM = "playlist"
PL_STATIC_ATTR = "title"
PL_STREAM_FILE = str("%s/streambrowser_save.xml" % AMAROK_DIR)
PL_STREAM_ELEM = "stream"
PL_STREAM_ATTR = "name"
PL_SMART_FILE = str("%s/smartplaylistbrowser_save.xml" % AMAROK_DIR)
PL_SMART_ELEM = "smartplaylist"
PL_SMART_ATTR = "name"
PL_DYNAMIC_FILE = str("%s/dynamicbrowser_save.xml" % AMAROK_DIR)
PL_DYNAMIC_ELEM = "dynamic"
PL_DYNAMIC_ATTR = "name"

PL_FILES = (PL_STATIC_FILE, PL_STREAM_FILE, PL_SMART_FILE, PL_DYNAMIC_FILE)
PL_ELEMS = (PL_STATIC_ELEM, PL_STREAM_ELEM, PL_SMART_ELEM, PL_DYNAMIC_ELEM)
PL_ATTRS = (PL_STATIC_ATTR, PL_STREAM_ATTR, PL_SMART_ATTR, PL_DYNAMIC_ATTR)

LIB_PLAYLISTS_RE = re.compile("playlist.*")

class PlayerProxy:
    """Kind of utitlity class for communication with Amarok."""
       
    def __init__(self):
        self.server = None
        self.app = None
        self.playlist_pids = []
        self.playlist_xml_items = []
        self.old_pid = None
        self.interrupted = False

    def updateAmarokConnection(self):
        """Get the dcop connection to Amarok.
        
        Checks the current connection to Amarok and reconnects if needed.
        """
        
        if self.interrupted: return False
        
        #logging.debug("check amarok connection")
        if not self.app:
            logging.debug("no connection, try to connect..")
            self.app = pydcop.anyAppCalled("amarok")
        if not self.app:
            logging.debug("could not connect, amarok seems to be down")
            return False
        try:
            logging.debug("amarok connection is there, test it...")
            # next: a test if the connection to amarok works
            self.app.player.getVolume()
            logging.debug("connection works")
            return True
        except:
            logging.warning("error in amarok connection." + \
                            "discard connection and try to reconnect ..")
            self.app = None
            self.app = pydcop.anyAppCalled("amarok")
            if not self.app:
                logging.info("could not connect, amarok seems to be down")
                return False
            logging.info("reconnected to amarok")
            return True

    def updatePlaylist(self):
        """Update the internal playlist.
        
        Request amarok to store the current playlist in an external file. Then we
        read this file to create a list of PIDs (global var 'playlist_pids') and a
        list of xml items (global var 'playlist_xml_items'). 
        """
        self.playlist_pids = []
        
        playlist_file = self.app.playlist.saveCurrentPlaylist()
        
        logging.debug("Current playlist in : %s" % playlist_file)
        document = minidom.parse(playlist_file)
        self.playlist_xml_items = document.getElementsByTagName("item")
        
        for item in self.playlist_xml_items:
            pid = item.attributes["uniqueid"].value
            if not pid or len(pid) == 0: # this seems to be a stream
                pid = item.attributes["url"].value
            self.playlist_pids.append(pid)
            logging.debug("append pid %s", pid)
            
    def fixquery(self, table, column, left, right, default):
        """Wrapper for a query for a single value.
        
        Does a query in 'table' for the value in 'column' where the column 'left'
        has the value 'right'. If the query returns no ore more than one result,
        'default' will be returnd, other wise the single returned result value.
        """
        result = self.query("SELECT %s FROM %s WHERE %s = \"%s\"" % \
                         (column, table, left, right))
        
        if (len(result) != 1):
            return default
        else:
            return result[0]
    
    def query(self, query):
        
        logging.debug("do query \"%s\"" % query)
    
        # Because a pydcop function used by '__app.collection.query(query)' only
        # works with latin1 strings (using python-dcop 3.5.5), an error may occur
        # if 'query' contains characters not compatible with latin1. In this case
        # we must use the tool 'dcop' because this accepts queries with all utf8
        # characters.
        # Note: This works on a system with default charset UTF8. I do not know
        # how the dcop-tool-solution works on other systems.
        try:
            query = query.decode("utf8").encode("latin1")
            result = self.app.collection.query(query)
        except: #UnicodeDecodeError or UnicodeEncodeError:
            output = commands.getoutput("dcop amarok collection query \"%s\"" % \
                                        query.replace('"', '\\"'))
            result = output.split("\n")
        
        return result
            
    def convertPidToUrl(self, pid):
        """Get the url for a PID.
        
        'pid' is what Amarok stores as uniqueid.
        """
        return self.fixquery("uniqueid", "url", "uniqueid", pid, None)
        
    def getPlobFromUrl(self, url):
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
        
        if url == self.app.player.encodedURL():
    
            plob = {remuco.PLOB_META_ARTIST : self.app.player.artist(), \
                    remuco.PLOB_META_TITLE : self.app.player.title(), \
                    remuco.PLOB_META_RATING : str(self.app.player.rating()), \
                    remuco.PLOB_META_GENRE : self.app.player.genre(), \
                    remuco.PLOB_META_YEAR : self.app.player.year(), \
                    remuco.PLOB_META_COMMENT : self.app.player.comment(), \
                    remuco.PLOB_META_ALBUM : self.app.player.album(), \
                    remuco.PLOB_META_BITRATE : self.app.player.bitrate(), \
                    remuco.PLOB_META_TRACK : self.app.player.track(), \
                    remuco.PLOB_META_LENGTH : str(self.app.player.trackTotalTime()), \
                    }
        
            self.addImgToPlob(plob)
            
            return plob
        
        # hm, check if the url is in the current playlist, then its still easy:
        
        for item in self.playlist_xml_items:
            
            if item.attributes["url"].value == url:
                
                plob = {remuco.PLOB_META_ARTIST : tag_val("Artist"), \
                        remuco.PLOB_META_TITLE : tag_val("Title"), \
                        remuco.PLOB_META_RATING : tag_val("Rating") + "/10", \
                        remuco.PLOB_META_GENRE : tag_val("Genre"), \
                        remuco.PLOB_META_YEAR : tag_val("Year"), \
                        remuco.PLOB_META_COMMENT : tag_val("Comment"), \
                        remuco.PLOB_META_ALBUM : tag_val("Album"), \
                        remuco.PLOB_META_BITRATE : tag_val("Bitrate"), \
                        remuco.PLOB_META_TRACK : tag_val("Track"), \
                        remuco.PLOB_META_LENGTH : tag_val("Length") \
                        }
        
                self.addImgToPlob(plob)
                
                return plob
        
        # damn, no way to get meta data for the url:
        
        logging.warning("cannot get info about the plob with url " + url)
        
        return {PLOB_META_TITLE : "unknown" }

    def addImgToPlob(self, plob):
        
        if plob[remuco.PLOB_META_ALBUM] == self.app.player.album() and \
           plob[remuco.PLOB_META_ARTIST] == self.app.player.artist():
            
            img = self.app.player.coverImage()
            if not img.endswith("nocover.png"):
                plob[remuco.PLOB_META_IMG] = img
        
        else:
        
            query = "SELECT " + \
                "path FROM images WHERE artist = \"%s\" AND album = \"%s\"" \
                % (plob[remuco.PLOB_META_ARTIST], plob[remuco.PLOB_META_ALBUM])
        
            result = self.query(query)
            
            if not result or len(result) == 0: return
            
            names = ( "front", "cover", "album", "folder" )
            types = ( ".png", ".jpg" )
            
            img = result[0] # fallback
            
            for type in types:
                for name in names:
                    for file in result:
                        #print "check file " + file
                        if file.lower().endswith(name + type):
                            #print "use " + file
                            img = file
        
            plob[remuco.PLOB_META_IMG] = img
        
##############################################################################
#
# Private functions
#
##############################################################################


def __catch_sig(signum, frame):
    
    global __pp
    
    if __pp.interrupted: return
    
    __pp.interrupted = True
    

##############################################################################
#
# Player proxy interface
#
##############################################################################

def pp_get_ps(pp):
    
    logging.debug("enter pp_get_ps()")

    if not pp.updateAmarokConnection():
        return (remuco.PS_STATE_OFF, 0, 0, 0, 0)
    
    # playback state
    
    state = pp.app.player.status()
    if state == 0:
        state = remuco.PS_STATE_STOP
    elif state == 1:
        state = remuco.PS_STATE_PAUSE
    elif state == 2:
        state = remuco.PS_STATE_PLAY
    else:
        state = remuco.PS_STATE_PROBLEM

    # volume
    
    volume = pp.app.player.getVolume()
    
    # repeat mode
    
    if pp.app.player.repeatPlaylistStatus():
        repeat = remuco.PS_REPEAT_MODE_PL
    elif pp.app.player.repeatTrackStatus():
        repeat = remuco.PS_REPEAT_MODE_PLOB
    else:
        repeat = remuco.PS_REPEAT_MODE_NONE
    
    # Amarok has also some kind of album repeat, but there is no dcop function
    # to check if this is enabled
    
    # shuffle mode
    
    if pp.app.player.randomModeStatus():
        shuffle = remuco.PS_SHUFFLE_MODE_ON
    else:
        shuffle = remuco.PS_SHUFFLE_MODE_OFF
    
    # playlist (we need this now to get the pid of the current song)
    
    pp.updatePlaylist()

    # playlist position and current plob pid
    
    pos = pp.app.playlist.getActiveIndex()
    if pos == -1:
        if state != remuco.PS_STATE_PAUSE and state != remuco.PS_STATE_PLAY:
            pp.old_pid = None
    else:
        pp.old_pid = pp.playlist_pids[pos]

    return ( state, volume, 0, repeat, shuffle)
            
                
def pp_get_current_plob_pid(pp):

    logging.debug("enter pp_get_current_plob_pid()")

    if not pp.updateAmarokConnection():
        return None

    return (pp.old_pid)

def pp_get_plob(pp, pid):
    
    logging.debug("enter pp_get_plob(%s)" % pid)

    if not pp.updateAmarokConnection():
        return { remuco.PLOB_META_TITLE : "Amarok is down" }
    
    if pid.startswith("http://") or pid.startswith("mms://"):
        # is a stream, won't find info about it in the databse, so:
        return pp.getPlobFromUrl(pid)
    
    url = pp.fixquery("uniqueid", "url", "uniqueid", pid, None)
    
    if (not url):
        return { remuco.PLOB_META_TITLE : "unknown" }
    
    query = "SELECT " + \
        "album, artist, bitrate, comment, genre, length, title, track, year" + \
        " FROM tags WHERE url = \"%s\"" % url
    
    result = pp.query(query)
    
    if (not result or len(result) != 9):
        logging.error("query '%s' has bad result: '%s'" % (query, str(result)))
        return None
    
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
        pp.fixquery("album", "name", "id", plob[remuco.PLOB_META_ALBUM], "")
    
    plob[remuco.PLOB_META_ARTIST] = \
        pp.fixquery("artist", "name", "id", plob[remuco.PLOB_META_ARTIST], "")

    plob[remuco.PLOB_META_RATING] = \
        pp.fixquery("statistics", "rating", "url", url, "0")

    plob[remuco.PLOB_META_GENRE] = \
        pp.fixquery("genre", "name", "id", plob[remuco.PLOB_META_GENRE], "")
    
    plob[remuco.PLOB_META_YEAR] = \
        pp.fixquery("year", "name", "id", plob[remuco.PLOB_META_YEAR], "")
    
    pp.addImgToPlob(plob)
    
    logging.debug("return plob %s" % url)

    return plob


def pp_get_ploblist(pp, plid):

    logging.debug("enter pp_get_ploblist(%s)" % plid)

    if not pp.updateAmarokConnection():
        return []
    
    if (plid == remuco.PLOBLIST_PLID_PLAYLIST):
        
        return pp.playlist_pids
    
    elif (plid == remuco.PLOBLIST_PLID_QUEUE):
        
        return []
    
    else:
    
        logging.warning("not yet implemented")
        return []
    
    return
    
    
def pp_get_library(pp):
    
    logging.debug("enter pp_get_library")
    
    library_pids = []
    library_names = []
    library_flags = []
    
    for file, elem, attr in zip(PL_FILES, PL_ELEMS, PL_ATTRS):
    
        document = minidom.parse(file)
        
        xml_items = document.getElementsByTagName(elem)
        
        for item in xml_items:
            name = item.attributes[attr].value
            if not name:
                continue
            logging.debug("found %s", name)
            if library_pids.__contains__(name):
                logging.warning("playlist name %s occurs twice, this may result in strange behaviour when loading a playlist with a client" % name)
            else:
                library_pids.append(name);
                library_names.append(name);
                library_flags.append(0);
    
    print "return %s" % str(library_pids)
    print "return %s" % str(library_names)
    print "return %s" % str(library_flags)
    return (library_pids, library_names, library_flags)
    
def pp_ctrl(pp, cmd, param):
    
    logging.debug("enter pp_ctrl(%i, %i)" % (cmd, param))
    
    if not pp.updateAmarokConnection():
        return
    
    try:
        if cmd == remuco.SCTRL_CMD_STOP:
            pp.app.player.stop()
        elif cmd == remuco.SCTRL_CMD_PLAYPAUSE:
            pp.app.player.playPause()
        elif cmd == remuco.SCTRL_CMD_NEXT:
            pp.app.player.next()
        elif cmd == remuco.SCTRL_CMD_PREV:
            pp.app.player.prev()
        elif cmd == remuco.SCTRL_CMD_VOLUME:
            pp.app.player.setVolume(param)
        elif cmd == remuco.SCTRL_CMD_RESTART:
            pp.app.player.stop()
            pp.app.playlist.playByIndex(0)
            pp.app.player.playPause()
        elif cmd == remuco.SCTRL_CMD_JUMP:
            pp.app.playlist.playByIndex(param - 1)
        elif cmd == remuco.SCTRL_CMD_RATE:
            pp.app.player.setRating(param)
        elif cmd == remuco.SCTRL_CMD_SEEK:
            pp.app.player.seekRelative(param)
        else:
            logging.warning("command %d not supported", cmd)
    except:
        logging.error(str(sys.exc_info()))
        logging.warning("sctrl %d with param %d caused an error", cmd, param)
    else:
        logging.debug("sctrl processed")
    
def pp_update_plob(pp, pid, meta):

    def idref_update(field, id_table, value):
        """Update an ID-field from table 'tags'.
        
        Some fields in table 'tags' are IDs which refer to a row in another table
        where the actual value is stored in column 'name'. This function
        updates such fields.
        """
        id = pp.fixquery(id_table, "id", "name", value, None)
        if not id:
            logging.info("%s '%s' not yet present -> insert into table %s" % \
                          (field, value, id_table))
            pp.query("INSERT INTO %s (name) VALUES('%s')" % (id_table, value))
            id = pp.fixquery(id_table, "id", "name", value, None)
        pp.query("UPDATE tags SET %s=%s WHERE url = '%s'" % (field, id, url))
    
    logging.debug("enter pp_update_plob(%s, meta)" % pid)

    if not pp.updateAmarokConnection():
        return

    meta_old = pp_get_plob(pp, pid)
    if not meta_old:
        logging.warning("no info about plob '%s' in db -> cannot update" % pid)
        return

    url = pp.convertPidToUrl(pid)

    if not url:
        logging.warning("could not find plob '%s' in db" % pid)
        return
        
    logging.debug("changing meta information of '%s'" % url)

    for mtn in meta.keys():
        
        if meta[mtn] == meta_old[mtn]: continue
        
        logging.debug("changing meta information '%s' to '%s'" % (mtn, meta[mtn]))
        
        if (mtn == remuco.PLOB_META_TITLE):
            pp.query("UPDATE tags SET title='%s' WHERE url = '%s'" % \
                        (meta[mtn], url))
        elif (mtn == remuco.PLOB_META_ARTIST):
            idref_update("artist", "artist", meta[mtn])
        elif (mtn == remuco.PLOB_META_ALBUM):
            idref_update("album", "album", meta[mtn])
        elif (mtn == remuco.PLOB_META_YEAR):
            idref_update("year", "year", meta[mtn])
        elif (mtn == remuco.PLOB_META_GENRE):
            idref_update("genre", "genre", meta[mtn])
        elif (mtn == remuco.PLOB_META_RATING):
            rate = pp.fixquery("statistics", "rating", "url", url, None)
            if not rate:
                logging.warning("no statistics stored yet for %s -> cannot " + \
                                "set rating, please rate manually in Amarok" % url)
                continue
            pp.query("UPDATE statistics SET rating=%s WHERE url = '%s'" % \
                        (meta[mtn], url))
#        if (mtn == remuco.PLOB_META_COMMENT):
#            pp.query("UPDATE tags SET comment='%s' WHERE url = '%s'" % \
#                        (meta[mtn], url))
#        does not work :/
        else:
            logging.warning("changing tag %s not supported" % mtn)

    return
    
def pp_update_ploblist(pp, plid, list):

    logging.debug("enter pp_update_ploblist(%s, list)" % plid)
    
    logging.warning("FUTURE FEATURE (not yet implemented on client side)")
    
def pp_play_ploblist(pp, plid):
    
    logging.debug("enter pp_play_ploblist(%s)" % plid)
    
    if not pp.updateAmarokConnection():
        return

    try:
        name = "".split(".", 2)[1]
    except:
        loggin.warning("invalid PLID: %" % plid)
        return
    
    pp.app.playlistbrowser.loadPlaylist(name)
    
def pp_search(pp, meta):
    
    logging.debug("enter pp_search(meta)")
    
    logging.warning("FUTURE FEATURE (not yet implemented on client side)")
    
def pp_error(pp, err):

    logging.debug("enter pp_search(meta)")
    
    logging.error(err)
    
    pp.interrupted = True
    
    signal.alarm(1)

##############################################################################

def main():
    logging.basicConfig(level=logging.INFO, \
               format='[ %(levelname)5s ] amarok-py-pp             : %(message)s')
    #           format='%(asctime)s [ %(levelname)s ] amarok-pp : %(message)s')

    # check corect system state
    
    ret = os.system("dcopserver --serverid > /dev/null 2>&1")
    if ret != 0:
        logging.error("dcopserver is not running! Try to start Amarok first..")
        exit()
    
    ret = os.system("dcop --help > /dev/null 2>&1")
    if ret != 0:
        logging.error("could not run dcop ! this tool is needed!")
        exit()

    global __pp
    
    __pp = PlayerProxy()
    
#    try:
    __pp.server = remuco.start(__pp, FEATURES, "Aamrok", 0, 10, 0, __name__)
    logging.info("server started")
#    except:
#        print(str(sys.exc_traceback))
#        logging.error("could not start server");
#        exit()
    
    signal.signal(signal.SIGINT, __catch_sig)
    signal.signal(signal.SIGTERM, __catch_sig)
    
    logging.info("here we go")
    
    while not __pp.interrupted:
        signal.pause()

    logging.info("shutdown")
    
    remuco.stop(__pp.server)
    
    logging.info("ok, down")

##############################################################################

if __name__ == "__main__":
    main()
