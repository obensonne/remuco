# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
#
#    This file is part of Remuco.
#
#    Remuco is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Remuco is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
#
# =============================================================================

"""Data containers to send to and receive from clients."""

import tempfile
import Image
import urlparse
import urllib

from remuco import log
from remuco import serial

# TODO: remove help fields

# =============================================================================
# outgoing data (to clients)
# =============================================================================

class PlayerInfo(serial.Serializable):
    """ Parameter of the player info message sent to clients."""
    
    def __init__(self, name, flags, max_rating, file_item_actions, search_mask):
        
        self.name = name
        self.flags = flags
        self.max_rating = max_rating

        self.fia_ids = []
        self.fia_labels = []
        self.fia_multiples = []
        for action in file_item_actions or []:
            self.fia_ids.append(action.id);
            self.fia_labels.append(action.label);
            self.fia_multiples.append(action.multiple);
            
        self.search_mask = search_mask or []
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_I, serial.TYPE_Y,
                serial.TYPE_AI, serial.TYPE_AS, serial.TYPE_AB,
                serial.TYPE_AS)
        
    def get_data(self):
        return (self.name, self.flags, self.max_rating,
                self.fia_ids, self.fia_labels, self.fia_multiples,
                self.search_mask)

class PlayerState(serial.Serializable):
    """ Parameter of the state sync message sent to clients."""
    
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
    """ Parameter of the progress sync message sent to clients."""
    
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
    """ Parameter of the item sync message sent to clients."""
    
    def __init__(self, id, info, img, img_size, img_type):
        
        self.__id = id
        self.__info = self.__flatten_info(info)
        self.__img = self.__thumbnail_img(img, img_size, img_type)
        
    def __str__(self):
        
        return "(%s, %s, %s)" % (self.id, self.__info, self.__img)

    # === serial interface ===
    
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS, serial.TYPE_AY)
        
    def get_data(self):
        return (self.__id, self.__info, self.__img)
    
    # === misc ===

    def __flatten_info(self, info_dict):
        
        info_list = []
        
        if not info_dict:
            return info_list
        
        for key in info_dict.keys():
            val = info_dict.get(key)
            if val is not None:
                info_list.append(key)
                if not isinstance(val, basestring):
                    val = str(val)
                info_list.append(val)
                
        return info_list

    def __thumbnail_img(self, img, img_size, img_type):
    
        if img_size == 0:
            return []
    
        if isinstance(img, basestring) and img.startswith("file://"):
            img = urlparse.urlparse(img)[2]
            img = urllib.url2pathname(img)
            
        if not img:
            return []
    
        try:
            if not isinstance(img, Image.Image):
                img = Image.open(img)
            img.thumbnail((img_size, img_size))
            file_tmp = tempfile.TemporaryFile()
            img.save(file_tmp, img_type)
            file_tmp.seek(0)
            thumb = file_tmp.read()
            file_tmp.close()
            return thumb
        except IOError, e:
            log.warning("failed to thumbnail %s (%s)" % (img, e))
            return []

class ItemList(serial.Serializable):
    """ Parameter of a request reply message sent to clients."""
    
    def __init__(self, path, nested, item_ids, item_names, item_offset,
                 page, page_max, item_actions, list_actions):
        
        self.path = path or []
        self.nested = nested or []
        self.item_ids = item_ids or []
        self.item_names = item_names or []
        self.item_offset = item_offset
        self.page = page or 0
        self.page_max = page_max or 0

        self.ia_ids = []
        self.ia_labels = []
        self.ia_multiples = []
        for action in item_actions or []:
            self.ia_ids.append(action.id);
            self.ia_labels.append(action.label);
            self.ia_multiples.append(action.multiple);
        
        self.la_ids = []
        self.la_labels = []
        for action in list_actions or []:
            self.la_ids.append(action.id);
            self.la_labels.append(action.label);
            
    def __str__(self):
        
        return "(%s, %s, %s, %s, %d, %d, %d, %s, %s, %s, %s, %s, %s, %s)" % (
                self.path, self.nested, self.item_ids, self.item_names,
                self.item_offset, self.page, self.page_max,
                self.ia_ids, self.ia_labels, self.ia_multiples,
                self.la_ids, self.la_labels)
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS,
                serial.TYPE_I, serial.TYPE_I, serial.TYPE_I,
                serial.TYPE_AI, serial.TYPE_AS, serial.TYPE_AB,
                serial.TYPE_AI, serial.TYPE_AS)
        
    def get_data(self):
        return (self.path, self.nested, self.item_ids, self.item_names,
                self.item_offset, self.page, self.page_max,
                self.ia_ids, self.ia_labels, self.ia_multiples,
                self.la_ids, self.la_labels)


# =============================================================================
# incoming data (from clients)
# =============================================================================

class ClientInfo(serial.Serializable):
    """ Parameter of a client info messages from a client."""

    def __init__(self):
        
        self.img_size = 0
        self.img_type = None
        self.page_size = 0

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_I, serial.TYPE_S, serial.TYPE_I)
        
    def set_data(self, data):
        self.img_size, self.img_type, self.page_size = data

class Control(serial.Serializable):
    """ Parameter of control messages from clients with integer arguments."""
    
    def __init__(self):
        
        self.param = 0

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_I,)
        
    def set_data(self, data):
        self.param, = data

class Action(serial.Serializable):
    """ Parameter of an action message from a client."""
    
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
    """ Parameter of a tagging message from a client."""
    
    def __init__(self):
        
        self.id = None
        self.tags = None

    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS)
        
    def set_data(self, data):
        self.id, self.tags = data

class Request(serial.Serializable):
    """ Parameter of a request message from a client."""

    def __init__(self):
        
        self.id = None
        self.path = None
        self.page = 0
        
    # === serial interface ===
        
    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS, serial.TYPE_I)
        
    def set_data(self, data):
        self.id, self.path, self.page = data

