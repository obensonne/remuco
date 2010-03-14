# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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

import glob
import hashlib
import os.path
import re
import urllib
import urlparse

from remuco import log
from remuco.remos import user_home

_RE_IND = r'(?:front|album|cover|folder|art)' # words indicating art files
_RE_EXT = r'\.(?:png|jpeg|jpg|gif)' # art file extensions
_RE_FILE = (r'^%s%s$' % (_RE_IND,_RE_EXT), # typical name (e.g. front.jpg)
           r'^.*%s.*%s$' % (_RE_IND,_RE_EXT), # typical name with noise
           r'^.*%s$' % _RE_EXT) # any image file
_RE_FILE = [re.compile(rx, re.IGNORECASE) for rx in _RE_FILE]

# =============================================================================
# various methods to find local cover art / media images
# =============================================================================

_TN_DIR = os.path.join(user_home, ".thumbnails")

def _try_thumbnail(resource):
    """Try to find a thumbnail for a resource (path or URI)."""
    
    if not os.path.isdir(_TN_DIR):
        return None
    
    # we need a file://... URI
    elems = urlparse.urlparse(resource)
    if elems[0] and elems[0] != "file": # not local
        return None
    if not elems[0]: # resource is a path
        elems = list(elems) # make elems assignable
        elems[0] = "file"
        if isinstance(resource, unicode):
            resource = resource.encode("utf-8")
        elems[2] = urllib.pathname2url(resource)
        resource = urlparse.urlunparse(elems)

    hex = hashlib.md5(resource).hexdigest()
    for subdir in ("large", "normal"):
        file = os.path.join(_TN_DIR, subdir, "%s.png" % hex)
        if os.path.isfile(file):
            return file
    
    return None

def _try_folder(resource):
    """Try to find an image in the resource's folder."""
    
    # we need a local path
    elems = urlparse.urlparse(resource)
    if elems[0] and elems[0] != "file": # resource is not local
        return None
    rpath = elems[0] and urllib.url2pathname(elems[2]) or elems[2]
    rpath = os.path.dirname(rpath)
    
    log.debug("looking for art image in %s" % rpath)

    files = glob.glob(os.path.join(rpath, "*"))
    files = [os.path.basename(f) for f in files if os.path.isfile(f)]
    
    for rx in _RE_FILE:
        for file in files:
            if rx.match(file):
                return os.path.join(rpath, file)
            
    return None

# =============================================================================

def get_art(resource, prefer_thumbnail=False):
    
    if resource is None:
        return None
    
    fname = None
    methods = (_try_thumbnail, _try_folder)
    for meth in methods:
        fname = meth(resource)
        if fname:
            break
    
    return fname
