import md5
import os
import os.path
import urllib
import urlparse

import log

TN_DIR = "%s/.thumbnails" % os.getenv("HOME")
TN_SUBDIRS = ("large", "normal")

ART_FILE_NAMES = ("folder", "front", "album", "cover", "art")
ART_FILE_TYPES = ("png", "jpeg", "jpg")

def __get_art_in_folder(resource):
    """Try to find art images in the given resource' folder.
    
    @param resource: a local path or an URL (string)
    
    @return: path to an image file or None if resource is not local or if there
             is no matching image file in the resource' folder
    """
    
    elems = urlparse.urlparse(resource)
    
    if elems[0] is not None and elems[0] != "file":
        return None
    
    path = urllib.url2pathname(elems[2])
    path = os.path.dirname(path)
    
    log.debug("looking for an image in %s" % path)
    
    for name in ART_FILE_NAMES:
        for type in ART_FILE_TYPES:
            for ext in (type, type.upper()):
                file = os.path.join(path, "%s.%s" % (name, ext))
                if os.path.isfile(file):
                    return file
                file = os.path.join(path, "%s.%s" % (name.capitalize(), ext))
                if os.path.isfile(file):
                    return file
            
    return None

def __resource_to_file_uri(resource):
    """Convert a resource to a file URI (file://...).
    
    @param resource: a local path or an URL (string)
    
    @return: the resource as a file URI string or None if resource is not local
    """

    elems = urlparse.urlparse(resource)
    
    if elems[0] == "file": # location already is a file URI
        return resource
    
    if elems[0] is None: # location is a path
    
        elems = list(elems) # make elems assignable
        elems[0] = "file"
        elems[2] = urllib.pathname2url(resource)
        
        return urlparse.urlunparse(elems)
        
    # location is neither a file URI nor a path
    
    return None

def __get_art_from_thumbnails(resource):
    """Try to find a thumbnail for the given resource.
    
    @param resource: a local path or an URL (string)
    
    @return: path to a thumbnail file or None if resource is not local or
             if there is no thumbnail for that resource
    """
    
    if not os.path.isdir(TN_DIR):
        return None
    
    uri = __resource_to_file_uri(resource)
    
    if uri is None:
        log.debug("%s is not local, cannot get thumbnail" % resource)
        return None
    
    hex = md5.new(uri).hexdigest()
    
    for subdir in TN_SUBDIRS:
        file = os.path.join(TN_DIR, subdir, "%s.png" % hex)
        if os.path.isfile(file):
            return file
    
    log.debug("no thumbnail for %s" % resource)
    
    return None

def get_art(resource, prefer_thumbnail=False):
    
    if resource is None:
        return None
    
    if prefer_thumbnail:
        file = __get_art_from_thumbnails(resource)
        if file is not None:
            return file
        
    file = __get_art_in_folder(resource)
    if file is not None:
        return file
    
    if not prefer_thumbnail:
        file = __get_art_from_thumbnails(resource)
        if file is not None:
            return file
    
    return None