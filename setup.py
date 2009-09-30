"""Setup script to install Remuco base and player adapters.

This script simply installs everything: the Python module 'remuco', all
player adapters (either as script or as data files) and the client binaries.

Installation my be configured by environment variables - search for 'os.getenv'
within this script.

"""
from distutils.core import setup
import os
import os.path

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

# --- Okular ------------------------------------------------------------------

pa_files["okular"] = [
    ["remuco-okular"]
]

# --- Rhythmbox ---------------------------------------------------------------

# set prefix may not be valid for Rhythmbox -> use a Rhythmbox specific prefix:
PREFIX_RHYTHMBOX = os.getenv("PREFIX_RHYTHMBOX", "/usr/")

pa_files["rhythmbox"] = [
    [],
    ("%slib/rhythmbox/plugins/remuco" % PREFIX_RHYTHMBOX,
     ["remuco.rb-plugin", "remythm.py"])
]

# --- Totem -------------------------------------------------------------------

# set prefix may not be valid for Totem -> use a Totem specific prefix:
PREFIX_TOTEM = os.getenv("PREFIX_TOTEM", "/usr/")

pa_files["totem"] = [
    [],
    ("%slib/totem/plugins/remuco" % PREFIX_TOTEM,
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

client_from_path = None

if client:
    if os.path.exists("client/dist/remuco.jar"):
        client_from_path = "client/dist"
    elif os.path.exists("client/app/remuco.jar"):
        client_from_path = "client/app"
    elif os.path.exists("client/remuco.jar"):
        client_from_path = "client"
    else:
        pass
        #raise StandardError("client needs to be built first, run: "
        #                    "ant -f client/build.xml dist")

if client_from_path is not None:
    data_files.append((CLIENT_DEST, ["%s/remuco.jar" % client_from_path,
                                     "%s/remuco.jad" % client_from_path]))

# =============================================================================
# setup
# =============================================================================



setup(name='remuco',
      version='0.9.2',
      description='Remuco is a remote control system for media players.',
      author='Oben Sonne',
      author_email='obensonne@googlemail.com',
      url='http://remuco.sourcefourge.net',
      license='GPLv3',
      packages=['remuco'],
      package_dir={'remuco': 'base/module/remuco'},
      scripts=scripts,
      data_files=data_files,
)

