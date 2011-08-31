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

import logging

DEBUG = logging.DEBUG
INFO = logging.INFO
WARNING = logging.WARNING
ERROR = logging.ERROR

#===============================================================================
# set up default logger
#===============================================================================

class _config:
    """Configuration container"""
    
    FMT = logging.Formatter("%(asctime)s [%(levelname)7s] [%(filename)11s " +
                            "%(lineno)4d] %(message)s")
    FMTX = logging.Formatter("%(levelname)s: %(message)s (check the log for "
                             "details)")
    
    handler_stdout = logging.StreamHandler()
    handler_stdout.setFormatter(FMT)
    handler = handler_stdout
    
    logga = logging.getLogger("remuco")
    logga.addHandler(handler)
    logga.setLevel(INFO)

#===============================================================================
# log functions
#===============================================================================

debug = _config.logga.debug
info = _config.logga.info
warning = _config.logga.warning
error = _config.logga.error
exception = _config.logga.exception

#===============================================================================
# configuration functions
#===============================================================================

def set_file(file):
    """Set log file (pass None to log to stdout)."""
 
    new_handler = None
    if file is not None:
        try:
            new_handler = logging.FileHandler(file, 'w')
        except IOError, e:
            print("failed to set up log handler (%s)" % e)
            return
        new_handler.setFormatter(_config.FMT)
        print("Log output will be stored in %s" % file)
        print("Contribute to Remuco: Please run 'remuco-report' once a client "
              "has connected, thanks!")
    
    if _config.handler != _config.handler_stdout:
        _config.logga.removeHandler(_config.handler)
    if new_handler:
        _config.handler_stdout.setLevel(ERROR)
        _config.handler_stdout.setFormatter(_config.FMTX)
        _config.logga.addHandler(new_handler)
        _config.handler = new_handler
    else:
        _config.handler_stdout.setLevel(_config.logga.level)
        _config.handler_stdout.setFormatter(_config.FMT)
        _config.handler = _config.handler_stdout
    
def set_level(level):
    """ Set log level (one of log.DEBUG, log.INFO, log.WARNING, log.ERROR)."""
    
    _config.logga.setLevel(level)
    
    if _config.handler is not None:
        _config.handler.setLevel(level) 

        

    