import os
import os.path
import re
import mimetypes
import sys

from xdg.BaseDirectory import xdg_config_home as xdg_config

from remuco import log


class _AllMimeTypes(list):
    
    def __init__(self):
        
        list.__init__(self)
    
    def __contains__(self, elem):
        return True

class _DirMap(dict):
    """Maps aliases and mime types to directories. """
    
    __DEFAULT = {
        "XDG_DESKTOP_DIR": "$HOME/Desktop",
        "XDG_DOWNLOAD_DIR": "$HOME/Download",
        "XDG_TEMPLATES_DIR": "$HOME/Templates",
        "XDG_PUBLICSHARE_DIR": "$HOME/Public",
        "XDG_DOCUMENTS_DIR": "$HOME/Documents",
        "XDG_MUSIC_DIR": "$HOME/Music",
        "XDG_PICTURES_DIR": "$HOME/Photos",
        "XDG_VIDEOS_DIR": "$HOME/Videos" }
    
    __MIME_TO_ALIAS_MAP = {
        "audio": "XDG_MUSIC_DIR",
        "video": "XDG_VIDEOS_DIR",
        "image": "XDG_PICTURES_DIR" }

    def __init__(self):
        
        dict.__init__(self)

        self.update(_DirMap.__DEFAULT)
        
        udc = self.__load_xdg_user_dirs_config()
        
        self.update(udc)
        
        for mime_type in _DirMap.__MIME_TO_ALIAS_MAP:
            alias = _DirMap.__MIME_TO_ALIAS_MAP[mime_type]
            self[mime_type] = self[alias]
            
    def __load_xdg_user_dirs_config(self):
    
        filename = os.path.join(xdg_config, "user-dirs.dirs")
    
        if not os.path.isfile(filename):
            return {}
        
        try:
            udc_file = open(filename, "r")
            udc_content = udc_file.read()
        except IOError, e:
            log.warning("failed to load user dirs config (%s)" % e)
            return {}
    
        pattern = re.compile("\\n(XDG_[_A-Z]+)=\"([^\\n]+)\"")
        tuples = re.findall(pattern, udc_content)
        
        config = {}
        for kv in tuples: config[kv[0]] = kv[1]
        
        return config
    
class FileSystemLibrary(object):
    
    def __init__(self, root_dirs, mime_types, show_extensions=False,
                 show_hidden=False, use_user_dirs=True):
        
        self.__mime_types = mime_types or _AllMimeTypes()
        self.__show_extensions = show_extensions
        self.__show_hidden = show_hidden
        
        if not sys.getfilesystemencoding() in ("UTF8", "UTF-8", "UTF_8"):
            log.warning("file system encoding is not UTF-8, this may cause " + 
                        "problems with file browser features")
        
        root_dirs = root_dirs or []
        
        if use_user_dirs:
            root_dirs += self.__get_mime_dirs(mime_types)
            
        root_dirs = self.__trim_dirs(root_dirs)
        
        if not root_dirs:
            root_dirs = (os.getenv("HOME", os.path.sep), )
        
        # map root dirs to names
        self.__roots = {}
        for dir in root_dirs:
            name = os.path.basename(dir)
            if name == dir: # == "/"
                name = "Root"
            else:
                name.capitalize()
            counter = 2
            name_x = name
            while name_x in self.__roots:
                name_x = "%s (%d)" % (name, counter)
                counter += 1
            self.__roots[name_x] = dir
            
        log.info("file browser root dirs: %s " % self.__roots)

        if not mimetypes.inited:
            mimetypes.init()
            
    def __get_mime_dirs(self, mime_types):
        """Get dirs which probably contain files with the given mime types."""
        
        if not mime_types:
            return []
        
        dm = _DirMap()
        
        dirs = []
        for type in mime_types:
            if type in dm:
                dirs.append(dm[type])
            type = type.split("/")[0] # use main mime type
            if type in dm:
                dirs.append(dm[type])
        
        return dirs
    
    def __trim_dirs(self, dirs):
        """Trim a directory list.
        
        Expands variables and '~' and removes duplicate, relative, non
        existent and optionally hidden directories.
        
        @return: a trimmed directory list
        """
        
        trimmed = []
        
        for dir in dirs:
            dir = os.path.expandvars(dir)
            dir = os.path.expanduser(dir)
            if not self.__show_hidden and dir.startswith("."):
                continue
            if not os.path.isabs(dir):
                log.warning("path %s not absolute, ignore" % dir)
                continue
            if not os.path.isdir(dir):
                log.warning("path %s not a directory, ignore" % dir)
                continue
            if dir not in trimmed:
                trimmed.append(dir)
                
        return trimmed
    
    def get_level(self, path):
        
        def is_hidden(name):
            return name.startswith(".") or name.endswith("~")
        
        def mimetype_is_supported(name):
            type = mimetypes.guess_type(name)[0] or ""
            type_main = type.split("/")[0]
            return type_main in self.__mime_types or type in self.__mime_types
        
        nested = []
        ids = []
        names = []

        if not path:
            nested = list(self.__roots.keys()) # Py3K
            nested.sort()
            return (nested, ids, names)
        
        label = path[0] # root dir label
        dir = self.__roots[label] # root dir
        path = path[1:] # path elements relative to root dir
        for elem in path:
            dir = os.path.join(dir, elem)
            
        try:
            x, dirs, files = os.walk(dir).next()
        except StopIteration:
            return (nested, ids, names)
              
        for entry in dirs:
            
            entry_abs = os.path.join(dir, entry)
            
            if not self.__show_hidden and is_hidden(entry):
                log.debug("ignore %s (hidden)" % entry_abs)
                continue
            
            if not os.access(entry_abs, os.X_OK | os.R_OK):
                log.debug("ignore %s (no access)" % entry_abs)
                continue
            
            nested.append(entry)
            
        for entry in files:
            
            entry_abs = os.path.join(dir, entry)
            
            if not self.__show_hidden and is_hidden(entry):
                log.debug("ignore %s (hidden)" % entry_abs)
                continue

            if not os.access(entry_abs, os.R_OK):
                log.debug("ignore %s (no access)" % entry_abs)
                continue
            
            if not os.path.isfile(entry_abs):
                log.debug("ignore %s (no regular file)" % entry_abs)
                continue
            
            if not mimetype_is_supported(entry):
                log.debug("ignore %s (wrong mime type)" % entry_abs)
                continue
                
            ids.append(entry_abs)
            if not self.__show_extensions:
                entry = os.path.splitext(entry)[0]
            names.append(entry)
                        
        return (nested, ids, names)
    
  
    
