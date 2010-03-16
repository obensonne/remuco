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

from __future__ import with_statement

import ConfigParser
from datetime import datetime
from glob import glob
import os
from os.path import join, isdir, exists, pathsep, basename
import re
import shutil
import sys
import textwrap

from remuco import log
from remuco import defs
from remuco.remos import user_config_dir
from remuco.remos import user_cache_dir

# =============================================================================
# Ordered dictionary, makes CP write sorted config files
# =============================================================================

class _odict(dict):
    
    def keys(self):
        kl = list(super(_odict, self).keys())
        kl.sort()
        return kl
    
    def items(self):
        il = list(super(_odict, self).items())
        il.sort(cmp=lambda a,b: cmp(a[0], b[0]))
        return il

# =============================================================================
# Constants and definitions
# =============================================================================

DEVICE_FILE = join(user_cache_dir, "remuco", "devices")

_DOC_HEADER = """# Player Adapter Configuration
# ============================
#
# Options defined in section DEFAULT affect *all* player adapters. Individual
# option values can be defined in each player's section.
#
# Options starting with `x-` are player specific options, i.e. they don't
# appear in section DEFAULT because they only make sense for specific players.  
#
# Options
# =======
#"""

# should be updated on major changes in config backend (simple removal or
# additions of options do not require a version update)
_CONFIG_VERSION = "3"

# standard options with default values, converter functions and documentation
_OPTIONS = {
    "config-version": ("0", None,
        "Used internally, don't edit."),
    "bluetooth-enabled": ("1", int,
        "Enable or disable Bluetooth."),
    "bluetooth-channel": ("0", int,
        "Bluetooth channel to use. 0 mean the next free channel."),
    "wifi-enabled": ("1", int,
        "Enable or disable WiFi (Inet)."),
    "wifi-port": ("34271", int,
        "WiFi port to use. Should be changed if Remuco is used for multiple "
        "players simultaneously to prevent port conflicts among adapters."),
    "player-encoding": ("UTF8", None,
        "Encoding of text coming from the player (i.e. artist, title, ...)."),
    "log-level": ("INFO", lambda v: getattr(log, v),
        "Log verbosity. Possible values: ERROR, WARNING, INFO, DEBUG."),
    "fb-show-extensions": ("0", int,
        "If to show file name extensions in a client's file browser."),
    "fb-root-dirs": ("auto", lambda v: v.split(pathsep),
        "List of directories (separated by `%s`) to show in a client's file "
        "browser. `auto` expands to all directories which typically contain "
        "files of the mime types a player supports (e.g. `~/Music` for audio "
        "players)." % pathsep),
    "master-volume-enabled": ("0", int,
        "Enable or disable master volume. By default a player's volume level "
        "is controlled by and displayed on clients. By setting this to `1` "
        "the system's master volume is used instead - in that case the "
        "following options *may* need to get adusted."),
    "master-volume-get-cmd": (r'amixer get Master | grep -E "\[[0-9]+%\]" | '
        'sed -re "s/^.*\[([0-9]+)%\].*$/\\1/"', None,
        "Command to get the master volume level in percent."),
    "master-volume-up-cmd": ("amixer set Master 5%+", None,
        "Command to increase the master volume."),
    "master-volume-down-cmd": ("amixer set Master 5%-", None,
        "Command to decrease the master volume."),
    "master-volume-mute-cmd": ("amixer set Master 0%", None,
        "Command to mute the master volume."),
    "system-shutdown-enabled": ("0", int,
        "Enable or disable system shutdown by clients. If enabled, the "
        "following option *may* need to get adjusted."),
    "system-shutdown-cmd": ("dbus-send --session --type=method_call "
        "--dest=org.freedesktop.PowerManagement "
        "/org/freedesktop/PowerManagement "
        "org.freedesktop.PowerManagement.Shutdown", None,
        "Command to shut down the system."),
}

# defaults-only version of _OPTIONS to pass to config parser
_DEFAULTS = _odict()
for k, v in _OPTIONS.items():
    _DEFAULTS[k] = v[0]

# timestamp (used for backups of old config data)
_TS = datetime.now().strftime("%Y%m%d-%H%M%S")

# =============================================================================
# Config class
# =============================================================================

class Config(object):
    """Class for getting and setting player adapter specific configurations.
    
    An instance of Config mirrors the configuration of a specific player
    adapter (usually ~/.config/remuco/PLAYER/conf).
    
    Player adapters are not supposed to create instances of Config. Instead
    use the 'config' attribute of a PlayerAdapter instance to access the
    currently used Config instance.
    
    """
    def __init__(self, player_name):
        """Create a new instance for the given player (adapter)."""

        super(Config, self).__init__()
        
        # convert descriptive name to a plain canonical one
        self.player = re.sub(r'[^\w-]', '', player_name).lower()
        
        # paths
        self.dir = join(user_config_dir, "remuco")
        self.cache = join(user_cache_dir, "remuco")
        self.file = join(self.dir, "remuco.cfg")

        # remove old stuff
        self.__cleanup()

        # create directories
        for dname in (self.dir, self.cache):
            try:
                if not isdir(dname):
                    os.makedirs(dname)
            except OSError, e:
                log.error("failed to make dir: %s", e)
        if not "REMUCO_LOG_STDOUT" in os.environ and isdir(self.cache):
            log.set_file(join(self.cache, "%s.log" % self.player))

        # load
        cp = ConfigParser.RawConfigParser(_DEFAULTS, _odict)
        if not cp.has_section(self.player):
            cp.add_section(self.player)
        if exists(self.file):
            try:
                cp.read(self.file)
            except ConfigParser.Error, e:
                log.warning("failed to read config %s (%s)" % (self.file, e))

        # reset on version change
        if cp.get(ConfigParser.DEFAULTSECT, "config-version") != _CONFIG_VERSION:
            sections = cp.sections() # keep already existing player sections
            cp = ConfigParser.RawConfigParser(_DEFAULTS, _odict)
            for sec in sections:
                cp.add_section(sec)
            if exists(self.file):
                bak = "%s.%s.backup" % (self.file, _TS)
                log.info("reset config (major changes, backup: %s)" % bak)
                shutil.copy(self.file, bak)
            
        # remove unknown options in all sections
        for sec in cp.sections() + [ConfigParser.DEFAULTSECT]:
            for key, value in cp.items(sec):
                if key not in _DEFAULTS and not key.startswith("x-"):
                    cp.remove_option(sec, key)
                    
        # add not yet existing options to default section
        for key, value in _DEFAULTS.items():
            if not cp.has_option(ConfigParser.DEFAULTSECT, key):
                cp.set(ConfigParser.DEFAULTSECT, key, value)
        
        # update version
        cp.set(ConfigParser.DEFAULTSECT, "config-version", _CONFIG_VERSION)

        self.__cp = cp
        
        # save to always have a clean file
        self.__save()

        log.set_level(self.log_level)
        
        log.info("remuco version: %s" % defs.REMUCO_VERSION)
        
    def __getattribute__(self, attr):
        """Attribute-style access to standard options."""
        
        try:
            return super(Config, self).__getattribute__(attr)
        except AttributeError, e:
            _attr = attr.replace("_", "-")
            if _attr in _OPTIONS:
                attr = _attr
            elif attr not in _OPTIONS:
                raise e
            value = self.__cp.get(self.player, attr)
            converter = _OPTIONS[attr][1] or (lambda v: v)
            try:
                return converter(value)
            except Exception, e:
                log.error("malformed option '%s: %s' (%s)" % (attr, e))
                return converter(_DEFAULTS[attr])
    
    def getx(self, key, default, converter=None, save=True):
        """Get the value of a non-standard, player specific option.
        
        @param key:
            config option name
        @param default:
            default value (as string!)
        @keyword converter:
            value converter function, e.g. `int`
        @keyword save:
            save default value in config file if not yet set
        @return:
            option value, optionally converted
        
        """
        key = "x-%s" % key
        if not self.__cp.has_option(self.player, key) and save:
            self.__cp.set(self.player, key, default)
            self.__save()
        try:
            value = self.__cp.get(self.player, key)
        except ConfigParser.NoOptionError:
            value = default
        converter = converter or (lambda v: v)
        try:
            return converter(value)
        except Exception, e:
            log.error("malformed option '%s: %s' (%s)" % (key, value, e))
            return converter(default) # if this fails then, it's a bug

    def __save(self):
        """Save config to it's file."""
        
        doc = [_DOC_HEADER]
        for key in _DEFAULTS.keys():
            idoc = "# %s:" % key 
            idoc = [idoc] + textwrap.wrap(_OPTIONS[key][2], 73)
            idoc = "\n#     ".join(idoc)
            doc.append(idoc)
        doc = "\n".join(doc)
        
        try:
            with open(self.file, 'w') as fp:
                fp.write(doc)
                fp.write("\n\n")
                self.__cp.write(fp)
        except IOError, e:
            log.warning("failed to save config to %s (%s)" % (self.file, e))

    def __cleanup(self):
        """Trash obsolete config and cache data from older versions."""
        
        def obsolete(fn):
            """Check if a config or cache item may be trashed."""
            obs  = isdir(fn)
            obs |= basename(fn) in ("shutdown-system", "volume")
            obs &= not basename(fn).startswith("old-")
            return obs
        
        for dname, dtype in ((self.dir, "config"), (self.cache, "cache")):
            trash = join(dname, "old-%s.backup" % _TS)
            fnames = [f for f in glob(join(dname, "*")) if obsolete(f)]
            if fnames:
                log.info("moving old %s data to %s" % (dtype, trash))
                if not exists(trash):
                    os.makedirs(trash)
                for fn in fnames:
                    shutil.move(fn, trash)
