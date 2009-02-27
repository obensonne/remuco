import sys
import logging

DEBUG = logging.DEBUG
INFO = logging.INFO
WARNING = logging.WARNING
ERROR = logging.ERROR

#===============================================================================
# set up default logger
#===============================================================================

class config:

    logga = logging.getLogger("remuco")
    
    handler = None
    
    FORMATTER = logging.Formatter("%(asctime)s [%(levelname)7s] [%(filename)11s %(lineno)4d] %(message)s")
    
debug = config.logga.debug
info = config.logga.info
warning = config.logga.warning
error = config.logga.error

#===============================================================================
# configuration functions
#===============================================================================

def reset_functions():
    """ Reset logging to use default log functions.
    
    Default log functions use module 'logging'. If a logging file has been
    set with set_file(), the log functions log to this file, otherwise they log
    to standard out.
    """
    
    global debug, info, warning, error
    
    debug = config.logga.debug
    info = config.logga.info
    warning = config.logga.warning
    error = config.logga.error


def set_file(file):
 
    if config.handler is not None:
        config.logga.removeHandler(config.handler)
    
    if file is None:
        new_handler = logging.StreamHandler()
    else:
        try:
            new_handler = logging.FileHandler(file, 'w')
        except IOError, e:
            print("failed to set up log handler (%s)" % e)
            return
    
    new_handler.setFormatter(config.FORMATTER)
    config.handler = new_handler
    config.logga.addHandler(new_handler)

def set_level(level):
    """ Set log level.
    
    @param level: a level as used in module logging
    @see: module logging
    """
    
    config.logga.setLevel(level)
    
    if config.handler is not None:
        config.handler.setLevel(level) 

def set_functions(fn_debug, fn_info, fn_warning, fn_error):
    """ Set log functions.
    
    @param fn_debug: function to use for logging debug messages
    @param fn_info: function to use for logging info messages
    @param fn_warning: function to use for logging warning messages
    @param fn_error: function to use for logging error messages
    
    @note: All functions must take exactly one argument (a string to log).
    """
    
    global debug, info, warning, error
    
    debug = fn_debug
    info = fn_info
    warning = fn_warning
    error = fn_error
        
#===============================================================================
# function which does nothing (suppress logging)
#===============================================================================

def dev_null(msg):
    pass




    