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

linux = sys.platform.startswith("linux")
windows = sys.platform.startswith("win")
mac = sys.platform == "darwin"

# =============================================================================
# helpers
# =============================================================================

def _real_path(p):
    """Expand a path to a variable-free absolute path."""
    
    return os.path.abspath(os.path.expanduser(os.path.expandvars(p)))

# =============================================================================
# locations
# =============================================================================

# media_dirs:
#    Maps some mimetypes to a list of locations which typically contain files
#    of a specific mimetype. For those mime types not mapped, `user_home` may
#    be used as a fallback.

if linux:
    
    import xdg.BaseDirectory

    user_home = os.getenv("HOME")
    user_config_dir = xdg.BaseDirectory.xdg_config_home
    user_cache_dir = xdg.BaseDirectory.xdg_cache_home

    media_dirs = {}
    try:
        with open(opj(user_config_dir, "user-dirs.dirs")) as fp:
            _udc = fp.read()
    except IOError, e:
        log.warning("failed to load user dirs config (%s)" % e)
        media_dirs["audio"] = ["~/Music"]
        media_dirs["video"] = ["~/Videos"]
    else:
        m = re.search(r'XDG_MUSIC_DIR="([^"]+)', _udc)
        media_dirs["audio"] = [m and m.groups()[0] or "~/Music"]
        m = re.search(r'XDG_VIDEOS_DIR="([^"]+)', _udc)
        media_dirs["video"] = [m and m.groups()[0] or "~/Video"]
    
elif windows:
    
    from win32com.shell import shell, shellcon

    user_home = shell.SHGetFolderPath(0, shellcon.CSIDL_PERSONAL, 0, 0)

    user_config_dir = shell.SHGetFolderPath(0, shellcon.CSIDL_APPDATA,0, 0)
    user_cache_dir = shell.SHGetFolderPath(0, shellcon.CSIDL_LOCAL_APPDATA, 0, 0)
    
    media_dirs = {}
    media_dirs["audio"] = [shell.SHGetFolderPath(0, shellcon.CSIDL_MYMUSIC, 0, 0),
                          shell.SHGetFolderPath(0, shellcon.CSIDL_COMMON_MUSIC, 0, 0)]
    media_dirs["video"] = [shell.SHGetFolderPath(0, shellcon.CSIDL_MYVIDEO, 0, 0),
                          shell.SHGetFolderPath(0, shellcon.CSIDL_COMMON_VIDEO, 0, 0)]
    
elif mac:
    raise NotImplementedError
else:
    assert False

# sanitize locations:
for mime_type, dirs in list(media_dirs.items()): # list prevents iter/edit conflicts
    media_dirs[mime_type] = [_real_path(p) for p in dirs]
    media_dirs[mime_type] = [p for p in dirs if os.path.exists(p)]

# =============================================================================
# user notifications
# =============================================================================

if linux:

    import dbus
    from dbus.exceptions import DBusException
    
    def notify(title, text):
        """Notify the user that a new device has been loggend."""
    
        try:
            bus = dbus.SessionBus()
        except DBusException, e:
            log.error("no dbus session bus (%s)" % e)
            return
        
        try:
            proxy = bus.get_object("org.freedesktop.Notifications",
                                   "/org/freedesktop/Notifications")
            notid = dbus.Interface(proxy, "org.freedesktop.Notifications")
        except DBusException, e:
            log.error("failed to connect to notification daemon (%s)" % e)
            return
    
        try:
            caps = notid.GetCapabilities()
        except DBusException, e:
            return
        
        if not caps or "body-markup" not in caps:
            text = text.replace("<b>", "")
            text = text.replace("</b>", "")
            
        try:
            notid.Notify("Remuco", 0, "phone", title, text, [], {}, 15)
        except DBusException, e:
            log.warning("user notification failed (%s)" % e)
            return
        
else:
    def notify(title, text):
        log.info("%s: %s" % (title, text))
        # TODO: implementations for mac and win
