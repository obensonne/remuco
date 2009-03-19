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
    
    handler_default = logging.StreamHandler()
    handler_default.setFormatter(FMT)
    handler = handler_default
    
    logga = logging.getLogger("remuco")
    logga.addHandler(handler)

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
    
    _config.logga.removeHandler(_config.handler)
    _config.handler = new_handler or _config.handler_default
    _config.logga.addHandler(new_handler)
    
def set_level(level):
    """ Set log level (one of log.DEBUG, log.INFO, log.WARNING, log.ERROR)."""
    
    _config.logga.setLevel(level)
    
    if _config.handler is not None:
        _config.handler.setLevel(level) 

        

    