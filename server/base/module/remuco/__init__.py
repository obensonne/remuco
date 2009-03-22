# -*- coding: UTF-8 -*-
"""Remuco player adapter module.

The module 'remuco' provides classes and constants for Remuco player adapters.

Class PlayerAdapter:
    Base class for player adapters.

Class Manager:
    Helper class for managing the life cycle of a player adapter.

Constants:
    The constants starting with 'INFO' are keys to be used for the dictionary
    describing an item (a playable object: song, video, slide, picture, ...).
    
    The constants starting with 'PLAYBACK' are the values used by Remuco to
    describe a playback state.

Logging:
    It is recommended to use the remuco logging system within player adapters.
    To do so, import the module 'remuco.log' and use the functions
    
    * remuco.log.debug(),
    * remuco.log.info(),
    * remuco.log.warning() and
    * remuco.log.error().
    
    Then all messages of the player adapter will be written into the same file
    as used internally by the remuco module - that makes debugging a lot easier.
    
    Internally Remuco uses the module 'logging' for all its logging messages.
    Messages go into a player specific log file (usually
    ~/.cache/remuco/PLAYER/log). The log level is defined in a player specific
    configuration file (usually ~/.config/remuco/PLAYER/conf).

"""

#==============================================================================
# imports
#==============================================================================

from remuco.adapter import PlayerAdapter, ItemAction, ListAction
from remuco.mpris import MPRISAdapter
from remuco.config import Config
from remuco.defs import *
from remuco.manager import Manager

#==============================================================================
# exports
#==============================================================================

API_VERSION = 1

__all__ = ("PlayerAdapter", "MPRISAdapter",
           "ItemAction", "ListAction", "Manager", "Config",
           
           "INFO_ALBUM", "INFO_ARTIST", "INFO_GENRE", "INFO_LENGTH",
           "INFO_RATING", "INFO_TAGS", "INFO_TITLE", "INFO_YEAR",
           
           "PLAYBACK_PAUSE", "PLAYBACK_PLAY", "PLAYBACK_STOP",
           
           "API_VERSION" 
           )

__version__ = "0.8.0"
