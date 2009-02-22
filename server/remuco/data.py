import tempfile
import Image

import log
import serial
import remuco
import command

class PlayerState(serial.Serializable):
    
    def __init__(self):
        
        self.__playback = remuco.PLAYBACK_STOP
        self.__volume = 0
        self.__position = 0
        self.__repeat = False
        self.__shuffle = False
        self.__queue = False
        
    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, PlayerState): return False
        
        try:
            if other.__playback != self.__playback: return False
            if other.__volume != self.__volume: return False
            if other.__position != self.__position: return False
            if other.__repeat != self.__repeat: return False
            if other.__queue != self.__queue: return False
        except AttributeError:
            return False
        
        return True
        
        
    def get_fmt(self):
        return (serial.TYPE_I, serial.TYPE_I, serial.TYPE_B,
                serial.TYPE_B, serial.TYPE_I, serial.TYPE_B)
        
    def get_data(self):
        return (self.__playback, self.__volume, self.__repeat,
                self.__shuffle, self.__position, self.__queue)
        
    def set_data(self, data):
        self.__playback, self.__volume, self.__repeat, \
            self.__shuffle, self.__position, self.__queue = data
            
    def get_playback(self):
        return self.__playback
        
    def get_volume(self):
        return self.__volume
    
    def get_position(self):
        return self.__position
    
    def get_repeat(self):
        return self.__repeat
        
    def get_shuffle(self):
        return self.__shuffle
        
    def get_queue(self):
        return self.__queue
    
    def set_playback(self, playback):
        self.__playback = playback
        
    def set_volume(self, volume):
        self.__volume = volume
    
    def set_position(self, position):
        self.__position = position
    
    def set_repeat(self, repeat):
        self.__repeat = repeat
        
    def set_shuffle(self, shuffle):
        self.__shuffle = shuffle
        
    def set_queue(self, queue):
        self.__queue = queue
    
class Library(serial.Serializable):
    
    PATH_QUEUE = ["__QUEUE__"];
    PATH_PLAYLIST = ["__PLAYLIST__"];
    MAX_LEN = 100
    
    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, Library): return False
        
        try:
            if other.__path != self.__path: return False
            if other.__nested != self.__nested: return False
            if other.__plob_ids != self.__plob_ids: return False
            if other.__plob_names != self.__plob_names: return False
        except AttributeError:
            return False
        
        return True

    def __init__(self, path, nested, plob_ids, plob_names):
        
        self.__path = path
        self.__nested = nested
        self.__plob_ids = plob_ids
        self.__plob_names = plob_names
        
    def get_fmt(self):
        return (serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS, serial.TYPE_AS)
        
    def get_data(self):
        ml = Library.MAX_LEN
        return (self.__path, self.__nested[:ml], self.__plob_ids[:ml],
                self.__plob_names[:ml])
        
    def set_data(self, data):
        self.__path, self.__nested, self.__plob_ids, self.__plob_names = data

class Plob(serial.Serializable):
    
    def __init__(self):
        
        self.__id = None
        self.__info = None
        self.__img_data = None
        
    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, Plob): return False
        
        try:
            if other.__id != self.__id: return False
            #if other.__img != self.__img: return False
            if other.__info != self.__info: return False
        except AttributeError:
            return False
        
        return True

    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_AS, serial.TYPE_AY)
        
    def get_data(self):
        return (self.__id, self.__info, self.__img_data)
    
    def get_id(self):
        return self.__id
        
    def set_data(self, data):
        self.__id, self.__info, self.__img_data = data
    
    def set_id(self, id):
        self.__id = id
    
    def set_img(self, img):
        
        if img is None:
            self.__img_data = None
        else:
            self.__img_data = self.__scale_image(img)
            
    def set_info(self, info_dict):
        
        if info_dict is None:
            self.__info = None 
        else:
            self.__info = []
            for key in info_dict.keys():
                self.__info.append(key)
                self.__info.append(str(info_dict.get(key)))
                
    def __scale_image(self, img_in):
    
        size = 300,300

        try:
            if isinstance(img_in, Image.Image):
                img_obj = img_in
            else: # img_in is a file name
                img_obj = Image.open(img_in)
            img_obj.thumbnail(size)
            outfile = tempfile.TemporaryFile()
            img_obj.save(outfile, "JPEG")
            outfile.seek(0)
            img_out = outfile.read()
            outfile.close()
        except IOError, e:
            log.warning("failed to thumbnail %s (%s)" % (infile, e))
            img_out = None
            
        return img_out
        
class Control(serial.Serializable):

    def __init__(self):
        
        self.__cmd = command.CMD_IGNORE
        self.__param_i = 0
        self.__param_s = ''
        
    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, Control): return False
        
        try:
            if other.__cmd != self.__cmd: return False
            if other.__param_i != self.__param_i: return False
            if other.__param_s != self.__param_s: return False
        except AttributeError:
            return False
        
        return True

    def get_fmt(self):
        return (serial.TYPE_I, serial.TYPE_I, serial.TYPE_S)
        
    def get_data(self):
        return (self.__cmd, self.__param_i, self.__param_s)
        
    def set_data(self, data):
        self.__cmd, self.__param_i, self.__param_s = data

    def get_cmd(self):
        return self.__cmd
    
    def get_param_i(self):
        return self.__param_i
    
    def get_param_s(self):
        return self.__param_s
        
class SerialString(serial.Serializable):
    
    def __init__(self):
        
        self.__s = ''
        
    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, SerialString): return False
        
        try:
            if other.__s != self.__s: return False
        except AttributeError:
            return False
        
        return True

    def get_fmt(self):
        return (serial.TYPE_S,)
        
    def get_data(self):
        return (self.__s,)
        
    def set_data(self, data):
        self.__s, = data

    def set(self, s):
        self.__s = s
        
    def get(self):
        return self.__s
    
class PlayerInfo(serial.Serializable):
    
    FEATURE_PLAYLIST = 1 << 0
    FEATURE_QUEUE = 1 << 1
    FEATURE_LIBRARY = 1 << 2
    FEATURE_TAGS = 1 << 3
    FEATURE_PLOBINFO = 1 << 4
    FEATURE_JUMP_PLAYLIST = 1 << 5
    FEATURE_JUMP_QUEUE = 1 << 6
    FEATURE_LOAD_PLAYLIST = 1 << 7
    FEATURE_SHUTDOWN_HOST = 1 << 8    
    
    def __init__(self, name, flags, rating_max):
        
        self.__name = name
        self.__flags = flags
        self.__rating_max = rating_max

    def __eq__(self, other):
        
        if other is None: return False
        
        if not isinstance(other, PlayerInfo): return False
        
        try:
            if other.__name != self.__name: return False
            if other.__flags != self.__flags: return False
            if other.__rating_max != self.__rating_max: return False
        except AttributeError:
            return False
        
        return True

    def get_fmt(self):
        return (serial.TYPE_S, serial.TYPE_I, serial.TYPE_I)
        
    def get_data(self):
        return (self.__name, self.__flags, self.__rating_max)
        
    def set_data(self, data):
        self.__name, self.__flags, self.__rating_max = data

    def get_name(self):
        return self.__name
    