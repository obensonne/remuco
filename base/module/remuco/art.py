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

import hashlib
import os
import os.path
import urllib
import urlparse

from remuco import log

TN_DIR = os.path.join(os.getenv("HOME"), ".thumbnails")
TN_SUBDIRS = ("large", "normal")

ART_FILE_NAMES = ("folder", "front", "album", "cover", "art")
ART_FILE_TYPES = ("png", "jpeg", "jpg")

def __resource_to_file_uri(resource):
    """Convert a resource to a file URI (file://...).
    
    @param resource: a local path or an URI (string)
    
    @return: the resource as a file URI string or None if resource is not local
    """

    elems = urlparse.urlparse(resource)
    
    if elems[0] == "file": # location already is a file URI
        return resource
    
    if not elems[0]: # location is a path
    
        elems = list(elems) # make elems assignable
        elems[0] = "file"
        elems[2] = urllib.pathname2url(resource)
        
        return urlparse.urlunparse(elems)
        
    # location is neither a file URI nor a path
    
    return None

def __get_art_in_folder(uri):
    """Try to find art images in the given URI's folder.
    
    @param uri: a file URI ('file://...')
    
    @return: path to an image file or None if there is no matching image file
             in the URI's folder
    """
    
    elems = urlparse.urlparse(uri)
    
    path = urllib.url2pathname(elems[2])
    path = os.path.dirname(path)
    
    log.debug("looking for art image in %s" % path)
    
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

def __get_art_from_thumbnails(uri):
    """Try to find a thumbnail for the given resource.
    
    @param uri: a file URI ('file://...')
    
    @return: path to a thumbnail file or None if URI is not local or if there
             is no thumbnail for that URI
    """
    
    if not os.path.isdir(TN_DIR):
        return None
    
    log.debug("looking for art image in %s" % TN_DIR)

    hex = hashlib.md5(uri).hexdigest()
    
    for subdir in TN_SUBDIRS:
        file = os.path.join(TN_DIR, subdir, "%s.png" % hex)
        if os.path.isfile(file):
            return file
    
    return None

def get_art(resource, prefer_thumbnail=False):
    
    if resource is None:
        return None
    
    uri = __resource_to_file_uri(resource)
    if uri is None:
        log.debug("resource '%s' is not local, ignore" % resource)
        return None
    
    if prefer_thumbnail:
        file = __get_art_from_thumbnails(uri)
        if file is not None:
            return file
        
    file = __get_art_in_folder(uri)
    if file is not None:
        return file
    
    if not prefer_thumbnail:
        file = __get_art_from_thumbnails(uri)
        if file is not None:
            return file
    
    return None