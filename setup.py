"""Setup script to install Remuco base and player adapters.

This script simply installs everything: the Python module 'remuco', all
player adapters (either as script or as data files) and the client binaries.

Installation my be configured by environment variables - search for 'os.getenv'
within this script.

"""
from distutils.core import setup
import os
import os.path
import platform

# used for plugin based adapters which install to /usr/lib...
if platform.architecture()[0] == "32bit":
    LIB_DIR = "lib"
else:
    LIB_DIR = "lib64"

# =============================================================================
# specify script and data files for each player adapter (relative file names)
# =============================================================================

pa_files = {} # keys are adapter names, values are lists with:
               # first element: script file list
               # following elements: date file tuples

# --- Amarok ------------------------------------------------------------------

pa_files["amarok"] = [
    ["remuco-amarok"]
]

# --- Amarok 1.4 --------------------------------------------------------------

pa_files["amarok14"] = [
    ["remuco-amarok14"]
]

# --- Audacious ---------------------------------------------------------------

pa_files["audacious"] = [
    ["remuco-audacious"]
]

# --- Banshee -----------------------------------------------------------------

pa_files["banshee"] = [
    ["remuco-banshee"]
]

# --- Rhythmbox ---------------------------------------------------------------

# set prefix may not be valid for Exaile -> use an Exaile specific prefix:
PREFIX_EXAILE = os.getenv("PREFIX_EXAILE", "/usr/")

pa_files["exaile"] = [
    [],
    ("%sshare/exaile/plugins/remex" % PREFIX_EXAILE,
     ["PLUGININFO", "__init__.py"])
]

# --- FooPlay -----------------------------------------------------------------

pa_files["fooplay"] = [
    ["remuco-fooplay"]
]

# --- MPD ---------------------------------------------------------------------

pa_files["mpd"] = [
    ["remuco-mpd"]
]

# --- MPLAYER -------------------------------------------------------------------

pa_files["mplayer"] = [
    ["remuco-mplayer"]
]

# --- Okular ------------------------------------------------------------------

pa_files["okular"] = [
    ["remuco-okular"]
]

# --- QuodLibet ------------------------------------------------------------------

pa_files["quodlibet"] = [
    ["remuco-quodlibet"]
]

# --- Rhythmbox ---------------------------------------------------------------

# set prefix may not be valid for Rhythmbox -> use a Rhythmbox specific prefix:
PREFIX_RHYTHMBOX = os.getenv("PREFIX_RHYTHMBOX", "/usr/")

pa_files["rhythmbox"] = [
    [],
    ("%s%s/rhythmbox/plugins/remuco" % (PREFIX_RHYTHMBOX, LIB_DIR),
     ["remuco.rb-plugin", "remythm.py"])
]

# --- Songbird ------------------------------------------------------------------

pa_files["songbird"] = [
    ["remuco-songbird"]
]

# --- Totem -------------------------------------------------------------------

# set prefix may not be valid for Totem -> use a Totem specific prefix:
PREFIX_TOTEM = os.getenv("PREFIX_TOTEM", "/usr/")

pa_files["totem"] = [
    [],
    ("%s%s/totem/plugins/remuco" % (PREFIX_TOTEM, LIB_DIR),
     ["remuco.totem-plugin", "remotem.py"])
]

# --- TVtime ------------------------------------------------------------------

pa_files["tvtime"] = [
    ["remuco-tvtime"]
]

# --- VLC ---------------------------------------------------------------------

pa_files["vlc"] = [
    ["remuco-vlc"]
]

# --- XMMS2 -------------------------------------------------------------------

pa_files["xmms2"] = [
    ["remuco-xmms2"]
]

# --- Winamp -------------------------------------------------------------------

# development on this adapter seems to have been stopped
#pa_files["winamp"] = [
#    ["remuco-winamp", "winamp.py"]
#]

# =============================================================================
# select player adapters to build/install (all by default)
# =============================================================================

components = os.getenv("REMUCO_COMPONENTS")

if components is None:
    # build/install everything
    player_adapters = pa_files.keys()
    client = True
elif components == "adapters":
    # build/install all player adapters
    player_adapters = pa_files.keys()
    client = False
elif components == "":
    # build/install no adapters, no client, only the base module
    player_adapters = []
    client = False
else:
    # build/install according to list selection
    player_adapters = components.split(',')
    try:
        player_adapters.remove("client")
        client = True
    except ValueError:
        client = False
    
# =============================================================================
# compile general script and data file list
# =============================================================================

scripts = []

try:
    x, dirs, files = os.walk("base/scripts").next()
except StopIteration:
    pass

for script in files:
    scripts.append("base/scripts/%s" % script)

data_files = [("share/man/man1", ["doc/remuco-report.1"])]

# =============================================================================
# compile player adapter related script and data file list
# =============================================================================

for pa in player_adapters:
    
    pa_scripts = pa_files[pa][0]
    pa_data_files = pa_files[pa][1:]
    
    for script in pa_scripts:
        scripts.append("adapter/%s/%s" % (pa, script))
    
    for tup in pa_data_files:
        group = []
        for data in tup[1]:
            group.append("adapter/%s/%s" % (pa, data))
        data_files.append((tup[0], group))

# =============================================================================
# compile client related data file list
# =============================================================================

CLIENT_DEST = os.getenv("REMUCO_CLIENT_DEST", "share/remuco/client")

for variant in ("", "no-bluetooth", "motorola-fix"):
    sdir = "client/midp/app/%s" % variant
    ddir = "%s/%s" % (CLIENT_DEST, variant)
    if client and os.path.exists("%s/remuco.jar" % sdir):
        data_files.append((ddir, ["%s/remuco.jar" % sdir,
                                  "%s/remuco.jad" % sdir]))

# =============================================================================
# setup
# =============================================================================

setup(name='remuco',
      version='0.9.2',
      description='Remuco is a remote control system for media players.',
      author='Remuco team',
      url='http://remuco.googlecode.com',
      license='GPLv3',
      packages=['remuco'],
      package_dir={'remuco': 'base/module/remuco'},
      scripts=scripts,
      data_files=data_files,
)
