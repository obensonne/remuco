import ConfigParser
import types
import os.path
import xdg.BaseDirectory

from remuco import log

class Config:
    
    SECTION_GENERAL = "general"
    OPTION_BLUETOOTH = "bluetooth"
    OPTION_WIFI = "wifi"
    OPTION_ENDCODING = "encoding"
    OPTION_LOGLEVEL = "loglevel"
    OPTION_PING = "ping"
    SECTION_CUSTOM = "custom"
    
    def __init__(self, player_name):
        
        self.__name = player_name
        
        self.__init_dirs()
        
        self.__cp = ConfigParser.SafeConfigParser()

        self.__init_values()
        
        self.__load()
        
    def __init_dirs(self):
        """Create config and cache dirs for given player."""
        
        self.__dir_config = "%s/remuco/%s" % \
                    (xdg.BaseDirectory.xdg_config_home, self.__name.lower())
        self.__dir_cache = "%s/remuco/%s" % \
                    (xdg.BaseDirectory.xdg_cache_home, self.__name.lower())
        self.__file_config = "%s/conf" % self.__dir_config
        self.__file_log = "%s/log" % self.__dir_cache
    
        try:
            if not os.path.isdir(self.__dir_config):
                os.makedirs(self.__dir_config)
            if not os.path.isdir(self.__dir_cache):
                os.makedirs(self.__dir_cache)
        except OSError, e:
            log.error("failed to create config and cache dir (%s)", e)

    def __init_values(self):
        """Initialize all configuration options to default values."""
    
        self.__cp.add_section(Config.SECTION_GENERAL)
        self.__cp.add_section(Config.SECTION_CUSTOM)
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_BLUETOOTH, "1")
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_WIFI, "1")
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_ENDCODING, "UTF-8")
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_LOGLEVEL, "INFO")
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_PING, "15")
        
    def __load(self):

        log.debug("try to load config from %s" % self.__file_config)
        
        try:
            self.__cp.read(self.__file_config)
        except ConfigParser.Error, e:
            log.warning("failed to read config from %s (%s) -> %s" %
                            (self.__file_config, e, "using defaults"))
    

    def __save(self):
        
        try:
            self.__cp.write(open(self.__file_config, 'w'))
        except IOError, e:
            log.warning("failed to save config to %s (%s)" %
                        (self.__file_config, e))

    def get_player_name(self):
        return self.__name

    def get_bluetooth(self):
        
        try:
            return self.__cp.getboolean(Config.SECTION_GENERAL,
                                        Config.OPTION_BLUETOOTH)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" %
                        (Config.OPTION_BLUETOOTH, e))
            return True
    
    def get_wifi(self):
        
        try:
            return self.__cp.getboolean(Config.SECTION_GENERAL,
                                        Config.OPTION_WIFI)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" %
                        (Config.OPTION_WIFI, e))
            return True
    
    def get_encoding(self):
        
        try:
            return self.__cp.get(Config.SECTION_GENERAL, Config.OPTION_ENDCODING)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" %
                        (Config.OPTION_ENDCODING, e))
            return "UTF-8"
    
    def get_log_level(self):
        
        try:
            s = self.__cp.get(Config.SECTION_GENERAL, Config.OPTION_LOGLEVEL)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" %
                        (Config.OPTION_LOGLEVEL, e))
            return log.INFO
        
        if s == "DEBUG": return log.DEBUG
        if s == "INFO": return log.INFO
        if s == "WARNING": return log.WARNING
        if s == "ERROR": return log.ERROR
        
        log.warning("config option %s has invalid value" %
                    Config.OPTION_LOGLEVEL)
        return log.INFO
    
    def get_ping(self):
        
        try:
            return self.__cp.getint(Config.SECTION_GENERAL, Config.OPTION_PING)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" %
                        (Config.OPTION_PING, e))
            return 15
        
    
    def get_custom(self, option):
        
        try:
            return self.__cp.get(Config.SECTION_CUSTOM, option)
        except ValueError, e:
            log.warning("config option %s malformed (%s)" % (option, e))
            return None

    def set_bluetooth(self, enabled):
        
        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_BLUETOOTH,
                      str(enabled))
        self.__save()
        
    def set_wifi(self, enabled):

        self.__cp.set(Config.SECTION_GENERAL, Config.OPTION_WIFI,
                      str(enabled))
        self.__save()
        
    def set_custom(self, option, value):
        
        self.__cp.set(Config.SECTION_CUSTOM, option, str(value))
        self.__save()
        

    def get_config_dir(self):
        return self.__dir_config
    
    def get_cache_dir(self):
        return self.__dir_cache
    
    def get_log_file(self):
        return self.__file_log
    
    def get_shutdown_system_command(self):
        
        file = "%s/../%s" % (self.__dir_config, "shutdown-system")
        
        if not os.path.isfile(file):
            log.info("system shutdown command (%s) does not exist" % file)
            return None
        else:
            return file
        
