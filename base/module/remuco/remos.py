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

"""Module to abstract platform specific code."""

from __future__ import with_statement

import os
from os.path import join as opj
import re
import sys

from remuco import log

# =============================================================================
# platform detection
# =============================================================================

_linux = sys.platform == "linux2"
_windows = sys.platform.startswith("win")
_mac = sys.platform == "darwin"

# =============================================================================
# helpers
# =============================================================================

def _real_path(p):
    """Expand a path to a variable-free absolute path."""
    
    return os.path.abspath(os.path.expanduser(os.path.expandvars(p)))

# =============================================================================
# locations
# =============================================================================

if _linux:
    
    import xdg.BaseDirectory

    user_home = os.getenv("HOME")
    user_config_dir = xdg.BaseDirectory.xdg_config_home
    user_cache_dir = xdg.BaseDirectory.xdg_cache_home

    _dir_map = {}
    try:
        with open(opj(user_config_dir, "user-dirs.dirs")) as fp:
            _udc = fp.read()
    except IOError, e:
        log.warning("failed to load user dirs config (%s)" % e)
        music_dirs = ["~/Music"]
        video_dirs = ["~/Videos"]
    else:
        m = re.search(r'XDG_MUSIC_DIR="([^"]+)', _udc)
        music_dirs = [m and m.groups()[0] or "~/Music"]
        m = re.search(r'XDG_VIDEOS_DIR="([^"]+)', _udc)
        video_dirs = [m and m.groups()[0] or "~/Videos"]
    
elif _windows:
    
    from win32com.shell import shell, shellcon

    user_home = shell.SHGetFolderPath(0, shellcon.CSIDL_PERSONAL, 0, 0)

    user_config_dir = shell.SHGetFolderPath(0, shellcon.CSIDL_APPDATA,0, 0)
    user_cache_dir = shell.SHGetFolderPath(0, shellcon.CSIDL_LOCAL_APPDATA, 0, 0)
    
    music_dirs = [shell.SHGetFolderPath(0, shellcon.CSIDL_MYMUSIC, 0, 0),
                  shell.SHGetFolderPath(0, shellcon.CSIDL_COMMON_MUSIC, 0, 0)]
    video_dirs = [shell.SHGetFolderPath(0, shellcon.CSIDL_MYVIDEO, 0, 0),
                  shell.SHGetFolderPath(0, shellcon.CSIDL_COMMON_VIDEO, 0, 0)]
    
elif _mac:
    raise NotImplementedError
else:
    assert False
    
# sanitize locations:
music_dirs = [_real_path(p) for p in music_dirs] # real path
music_dirs = [p for p in music_dirs if os.path.exists(p)] # remove non-existent
music_dirs = music_dirs or [user_home] # fall back to home if list is empty
video_dirs = [_real_path(p) for p in video_dirs] # real path
video_dirs = [p for p in video_dirs if os.path.exists(p)] # remove non-existent
video_dirs = video_dirs or [user_home] # fall back to home if list is empty

