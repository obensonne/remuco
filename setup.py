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

# --- XMMS2 -------------------------------------------------------------------

# set prefix may not be valid for XMMS2 -> use an XMMS2 specific prefix:
PREFIX_XMMS2 = os.getenv("PREFIX_XMMS2", "/usr/")

pa_files["xmms2"] = [
    [],
    ("%sshare/xmms2/scripts/startup.d" % PREFIX_XMMS2, ["remuco-xmms2"])
]

# =============================================================================
# select player adapters to build/install (all by default)
# =============================================================================

pa_selection = os.getenv("REMUCO_ADAPTERS")

if pa_selection is None:
    # build/install all adapters
    player_adapters = pa_files.keys()
elif pa_selection == "":
    # build/install no adapters
    player_adapters = []
else:
    # build/install selected adapters
    player_adapters = pa_selection.split(',')
    
# =============================================================================
# generate script and date file list (add prefix to pa_files)
# =============================================================================

scripts = []
data_files = []

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
# client binaries
# =============================================================================

CLIENT_DEST = os.getenv("REMUCO_CLIENT_DEST", "share/remuco/client")

# do not install client if certain environment variables are set (used by
# Makefile to suppress client installation)
install_client = (os.getenv("REMUCO_ADAPTERS") is None and
                  os.getenv("REMUCO_NO_CLIENT") is None)  

if install_client and os.path.exists("client/app"):
    
    for client in ("", "non-optimized"):
        data_files.append(("%s/%s" % (CLIENT_DEST, client),
                           ["client/app/%s/remuco.jar" % client,
                            "client/app/%s/remuco.jad" % client]))
        

# =============================================================================
# setup
# =============================================================================

setup(name='remuco',
      version='0.8.1',
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

