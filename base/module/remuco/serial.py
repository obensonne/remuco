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

import inspect
import struct
import array

from remuco import log

TYPE_Y = 1
TYPE_I = 2
TYPE_B = 3
TYPE_S = 4
TYPE_AY = 5
TYPE_AI = 6
TYPE_AS = 7
TYPE_L = 8
TYPE_N = 9
TYPE_AN = 10
TYPE_AB = 11
TYPE_AL = 12

class Bin:
    
    NET_ENCODING = "UTF-8" # codec for data exchanged with clients
    NET_ENCODING_ALT = ("UTF-8", "UTF8", "utf-8", "utf8") # synonyms
    HOST_ENCODING = NET_ENCODING # will be updated with value from config file
    
    def __init__(self, buff=None):
        
        self.__data = buff or array.array('c')
        self.__off = 0
        
    def get_buff(self):
        if isinstance(self.__data, basestring):
            return self.__data
        elif isinstance(self.__data, array.array):
            return self.__data.tostring()
        else:
            log.error("** BUG ** unexpected buffer type")
        
    def read_boolean(self):
        
        b = self.read_byte()
        if b == 0:
            return False
        else:
            return True

    def read_byte(self):
        
        y = struct.unpack_from('b', self.__data, offset=self.__off)[0]
        self.__off += 1
        return y
        
    def read_short(self):
        
        n = struct.unpack_from('!h', self.__data, offset=self.__off)[0]
        self.__off += 2
        return n
    
    def read_int(self):
        
        i = struct.unpack_from('!i', self.__data, offset=self.__off)[0]
        self.__off += 4
        return i
    
    def read_long(self):
        
        l = struct.unpack_from('!q', self.__data, offset=self.__off)[0]
        self.__off += 8
        return l
    
    def read_string(self):
        """ Read a string.
        
        The read raw string will be converted from Bin.NET_ENCODING to
        Bin.HOST_ENCODING.
        """
        
        s = self.__read_string()
        
        if Bin.HOST_ENCODING not in Bin.NET_ENCODING_ALT:
            try:
                s = unicode(s, Bin.NET_ENCODING).encode(Bin.HOST_ENCODING)
            except UnicodeDecodeError, e:
                log.warning("could not decode '%s' with codec %s (%s)" %
                            (s, Bin.NET_ENCODING, e))
            except UnicodeEncodeError, e:
                log.warning("could not encode '%s' with codec %s (%s)" %
                            (s, Bin.HOST_ENCODING, e))
                
        return s

    def read_type(self, expected):
        
        type = self.read_byte()
         
        if type != expected:
            log.warning("bin data malformed (expected type %d, have %d)" %
                        (expected, type))
            return False
        else:
            return True
        
    def read_array_boolean(self):
        
        return self.__read_array(self.read_boolean)

    def read_array_byte(self):
        
        return self.__read_array(self.read_byte)

    def read_array_short(self):
        
        return self.__read_array(self.read_short)
    
    def read_array_int(self):
        
        return self.__read_array(self.read_int)
    
    def read_array_long(self):
        
        return self.__read_array(self.read_long)
    
    def read_array_string(self):
        
        return self.__read_array(self.read_string)
            
    def __read_string(self):
        """ Read a string as it is, i.e. without any codec conversion. """
        
        l = self.read_short()
        s = struct.unpack_from('%ds' % l, self.__data, offset=self.__off)[0]
        self.__off += l
        return s
        
    def __read_array(self, fn_read_element):
        
        num = self.read_int()
        
        a = []
        
        for i in range(num):
            
            a.append(fn_read_element())
            
        return a
    
    def get_unused_data(self):
        
        return len(self.__data) - self.__off
        
    def write_type(self, type):
        
        self.write_byte(type)
    
    def write_boolean(self, b):
        
        if b:
            self.write_byte(1)
        else:
            self.write_byte(0)
        
    def write_byte(self, y):
        
        if y is None: y = 0
        self.__data.extend(' ' * 1)
        struct.pack_into('b', self.__data, self.__off, y)
        self.__off += 1

    def write_short(self, n):
        
        if n is None: n = 0
        self.__data.extend(' ' * 2)
        struct.pack_into('!h', self.__data, self.__off, n)
        self.__off += 2

    def write_int(self, i):
        
        if i is None: i = 0
        self.__data.extend(' ' * 4)
        struct.pack_into('!i', self.__data, self.__off, i)
        self.__off += 4

    def write_long(self, l):
        
        if l is None: l = 0
        self.__data.extend(' ' * 8)
        struct.pack_into('!q', self.__data, self.__off, l)
        self.__off += 8

    def write_string(self, s):
        """ Write a string. 
        
        If the string is a unicode string, it will be encoded as a normal string
        in Bin.NET_ENCODING. If it already is a normal string it will be
        converted from Bin.HOST_ENCODING to Bin.NET_ENCODING.
        
        """
        if s is None:
            self.__write_string(s)
            return
        
        if isinstance(s, unicode):
            
            try:
                s = s.encode(Bin.NET_ENCODING)
            except UnicodeEncodeError, e:
                log.warning("could not encode '%s' with codec %s (%s)" %
                            (s, Bin.NET_ENCODING, e))
                s = str(s)
        
        elif Bin.HOST_ENCODING not in Bin.NET_ENCODING_ALT:
            log.debug("convert '%s' from %s to %s" %
                      (s, Bin.HOST_ENCODING, Bin.NET_ENCODING))
            try:
                s = unicode(s, Bin.HOST_ENCODING).encode(Bin.NET_ENCODING)
            except UnicodeDecodeError, e:
                log.warning("could not decode '%s' with codec %s (%s)" %
                            (s, Bin.HOST_ENCODING, e))
            except UnicodeEncodeError, e:
                log.warning("could not encode '%s' with codec %s (%s)" %
                            (s, Bin.NET_ENCODING, e))
            
        self.__write_string(s)

    def write_array_boolean(self, ba):
        
        self.__write_array(ba, self.write_boolean)

    def write_array_byte(self, ba):
        
        if isinstance(ba, str): # byte sequences often come as strings
            self.__write_string(ba, len_as_int=True)
        else:
            self.__write_array(ba, self.write_byte)

    def write_array_short(self, na):
        
        self.__write_array(na, self.write_short)

    def write_array_int(self, ia):
        
        self.__write_array(ia, self.write_int)

    def write_array_long(self, ia):
        
        self.__write_array(ia, self.write_long)

    def write_array_string(self, sa):
        
        self.__write_array(sa, self.write_string)

    def __write_string(self, s, len_as_int=False):
        """ Write a string. 
        
        The string is written as is, i.e. there is no codec conversion.
        """
        
        if s is None:
            s = ""
            
        if not isinstance(s, basestring):
            s = str(s)
        
        l = len(s)
        
        if len_as_int:
            self.write_int(l)
        else:
            self.write_short(l)
        
        self.__data.extend(' ' * l)
        struct.pack_into('%ds' % l, self.__data, self.__off, s)
        self.__off += l
        
    def __write_array(self, a, fn_element_write):
        
        if a is None:
            l = 0
        else:
            l = len(a)
        
        self.write_int(l)
        
        for i in range(l):
            
            fn_element_write(a[i])

class Serializable(object):

    def get_fmt(self):
        
        raise NotImplementedError
        
    def get_data(self):

        raise NotImplementedError
        
    def set_data(self, data):

        raise NotImplementedError
    
def pack(serializable):

    fmt = serializable.get_fmt()
    
    data = serializable.get_data()
    
    if len(fmt) != len(data):
        log.error("** BUG ** format string and data differ in length")
        return None
        
    #log.debug("data to pack: %s" % str(data))

    bin = Bin()
    
    try:

        for i in range(0,len(fmt)):
            
            type = fmt[i]
            
            bin.write_byte(type)
            
            if type == TYPE_Y:
                
                bin.write_byte(data[i])
                
            elif type == TYPE_B:
                
                bin.write_boolean(data[i])
        
            elif type == TYPE_N:
                
                bin.write_short(data[i])
                
            elif type == TYPE_I:
                
                bin.write_int(data[i])
                
            elif type == TYPE_L:
                
                bin.write_long(data[i])
                
            elif type == TYPE_S:
                
                bin.write_string(data[i])
                
            elif type == TYPE_AB:
                
                bin.write_array_boolean(data[i])
                
            elif type == TYPE_AY:
                
                bin.write_array_byte(data[i])
                
            elif type == TYPE_AN:
                
                bin.write_array_short(data[i])

            elif type == TYPE_AI:
                
                bin.write_array_int(data[i])

            elif type == TYPE_AL:
                
                bin.write_array_long(data[i])

            elif type == TYPE_AS:
                
                bin.write_array_string(data[i])

            else:
                log.error("** BUG ** unknown type (%d) in format string" % type)
                return None
        
    except struct.error, e:
        
        log.exception("** BUG ** %s" % e)
        
        return None
    
    return bin.get_buff()

def unpack(serializable, bytes):
    """ Deserialize a Serializable.
    
    @param serializable:
        the Serializable to apply the binary data to (may be a class, in which
        case a new instance of this class is created)
    @param bytes:
        binary data (serialized Serializable)
    
    @return: 'serializable' itself if it is an instance of Serializable, a new
        instance of 'serializable' if it is a class or None if an error
        occurred
    """
    
    if inspect.isclass(serializable):
        serializable = serializable()
    
    fmt = serializable.get_fmt()
    
    if fmt and not bytes:
        log.warning("there is no data to unpack")
        return None
    
    data = []
    
    bin = Bin(buff=bytes)
    
    try:

        for type in fmt:
            
            if not bin.read_type(type):
                return None
            
            if type == TYPE_Y:
                
                data.append(bin.read_byte())
                
            elif type == TYPE_B:
                
                data.append(bin.read_boolean())
        
            elif type == TYPE_N:
                
                data.append(bin.read_short())
                
            elif type == TYPE_I:
                
                data.append(bin.read_int())
                
            elif type == TYPE_L:
                
                data.append(bin.read_long())
                
            elif type == TYPE_S:
                
                data.append(bin.read_string())
                
            elif type == TYPE_AB:
                
                data.append(bin.read_array_boolean())
                
            elif type == TYPE_AY:
                
                data.append(bin.read_array_byte())
                
            elif type == TYPE_AN:
                
                data.append(bin.read_array_short())

            elif type == TYPE_AI:
                
                data.append(bin.read_array_int())

            elif type == TYPE_AL:
                
                data.append(bin.read_array_long())

            elif type == TYPE_AS:
                
                data.append(bin.read_array_string())

            else:
                
                log.warning("bin data malformed (unknown data type: %d)" % type)
                return None
        
    except struct.error, e:
        
        log.warning("bin data malformed (%s)" % e)
        
        return None
    
    unused = bin.get_unused_data()
    if unused:
        log.warning("there are %d unused bytes" % unused)
        return None
    
    serializable.set_data(data)
    
    #log.debug("unpacked data  : %s" % str(data))

    return serializable


