"""Data containers to send to and receive from clients."""
import tempfile
import Image
import urlparse
import urllib

from remuco import command
from remuco import log
from remuco import serial

# =============================================================================
# outgoing data (to clients)
# =============================================================================

class PlayerInfo(serial.Serializable):
    
    def __init__(self, name, flags, max_rating, file_item_actions):
        
        self.name = name
        self.flags = flags
        self.max_rating = max_rating
        
        self.fia_ids = []
        self.fia_labels = []
        self.fia_multiples = []
        self.fia_helps = []
        for action in file_item_actions or []:
            self.fia_ids.append(action.id);
            self.fia_labels.append(action.label);
            self.fia_multiples.append(action.multiple);
            self.fia_helps.append(action.help);
            
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_I, serial.TYPE_Y,
                serial.TYPE_AI, serial.TYPE_AS, serial.TYPE_AB,
                serial.TYPE_AS)
        
    def get_data(self):
        return (self.name, self.flags, self.max_rating,
                self.fia_ids, self.fia_labels, self.fia_multiples,
                self.fia_helps)

class PlayerState(serial.Serializable):
    
    def __init__(self):
        
        self.playback = 0
        self.volume = 0
        self.position = 0
        self.repeat = False
        self.shuffle = False
        self.queue = False
        
    def __str__(self):
        
        return "(%d, %d, %d, %s, %s, %s)" % (
                self.playback, self.volume, self.position,
                self.repeat, self.shuffle, self.queue)
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_Y, serial.TYPE_Y, serial.TYPE_I,
                serial.TYPE_B, serial.TYPE_B, serial.TYPE_B)
        
    def get_data(self):
        return (self.playback, self.volume, self.position,
                self.repeat, self.shuffle, self.queue)

class Progress(serial.Serializable):
    
    def __init__(self):
        
        self.progress = 0
        self.length = 0
        
    def __str__(self):
        return "(%d/%d)" % (self.progress, self.length)
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_I, serial.TYPE_I)
        
    def get_data(self):
        return (self.progress, self.length)

class Item(serial.Serializable):
    
    def __init__(self):
        
        self.id = None
        self.__info_orig = None
        self.__info_list = None
        self.__img_orig = None
        self.__img_bin = None
        
    def __str__(self):
        
        return "(%s, %s, %s)" % (self.id, self.__info_orig, self.__img_orig)

    # === property: img ===
    
    def __pget_img(self):
        return self.__img_orig
    
    def __pset_img(self, img):
        self.__img_orig = img
        self.__img_bin = self.__thumbnail(img)
    
    img = property(__pget_img, __pset_img, None, None)
        
    # === property: info ===
    
    def __pget_info(self):
        return self.__info_orig
    
    def __pset_info(self, info_dict):
        
        self.__info_orig = info_dict
        self.__info_list = []
        
        if not info_dict:
            return
        
        for key in info_dict.keys():
            val = info_dict.get(key)
            if val is not None:
                self.__info_list.append(key)
                if not isinstance(val, basestring):
                    val = str(val)
                self.__info_list.append(val)
    
    info = property(__pget_info, __pset_info, None, None)

    # === serial interface ===
    
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS, serial.TYPE_AY)
        
    def get_data(self):
        return (self.id, self.__info_list, self.__img_bin)
    
    # === misc ===

    def __thumbnail(self, img):
    
        if isinstance(img, basestring) and img.startswith("file://"):
            img = urlparse.urlparse(img)[2]
            img = urllib.url2pathname(img)
            
        if not img:
            return []
    
        size = 300,300
        
        try:
            if not isinstance(img, Image.Image):
                img = Image.open(img)
            img.thumbnail(size)
            file_tmp = tempfile.TemporaryFile()
            img.save(file_tmp, "JPEG")
            file_tmp.seek(0)
            thumb = file_tmp.read()
            file_tmp.close()
            return thumb
        except IOError, e:
            log.warning("failed to thumbnail %s (%s)" % (img, e))
            return []

class ItemList(serial.Serializable):
    
    MAX_LEN = 100
    
    def __init__(self, path, nested, plob_ids, plob_names, item_actions,
                 list_actions):
        
        self.path = path or []
        self.nested = nested or []
        self.items = plob_ids or []
        self.names = plob_names or []

        self.ia_ids = []
        self.ia_labels = []
        self.ia_multiples = []
        self.ia_helps = []
        for action in item_actions or []:
            self.ia_ids.append(action.id);
            self.ia_labels.append(action.label);
            self.ia_multiples.append(action.multiple);
            self.ia_helps.append(action.help);
        
        self.la_ids = []
        self.la_labels = []
        self.la_helps = []
        for action in list_actions or []:
            self.la_ids.append(action.id);
            self.la_labels.append(action.label);
            self.la_helps.append(action.help);
        
    def __str__(self):
        
        return "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)" % (
                self.path, self.nested, self.items, self.names,
                self.ia_ids, self.ia_labels, self.ia_multiples, self.ia_helps,
                self.la_ids, self.la_labels, self.la_helps)
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS,
                serial.TYPE_AI, serial.TYPE_AS, serial.TYPE_AB, serial.TYPE_AS,
                serial.TYPE_AI, serial.TYPE_AS, serial.TYPE_AS)
        
    def get_data(self):
        ml = ItemList.MAX_LEN
        return (self.path, self.nested[:ml], self.items[:ml], self.names[:ml],
                self.ia_ids, self.ia_labels, self.ia_multiples, self.ia_helps,
                self.la_ids, self.la_labels, self.la_helps)


# =============================================================================
# incoming data (from clients)
# =============================================================================


class Control(serial.Serializable):
    
    def __init__(self):
        
        self.param = 0

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_I,)
        
    def set_data(self, data):
        self.param, = data

class Action(serial.Serializable):
    
    def __init__(self):
        
        self.id = 0
        self.path = None
        self.positions = None
        self.items = None # item ids or file names

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_I, serial.TYPE_AS, serial.TYPE_AI, serial.TYPE_AS)
        
    def set_data(self, data):
        self.id, self.path, self.positions, self.items = data

class Tagging(serial.Serializable):
    
    def __init__(self):
        
        self.id = None
        self.tags = None

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS)
        
    def set_data(self, data):
        self.id, self.tags = data

class Request(serial.Serializable):

    def __init__(self):
        
        self.id = None
        self.path = None
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS)
        
    def set_data(self, data):
        self.id, self.path = data

