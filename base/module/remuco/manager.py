import os
import signal
import subprocess

import dbus
from dbus.exceptions import DBusException
from dbus.mainloop.glib import DBusGMainLoop
import gobject

from remuco import log

_ml = None

def _sighandler(signum, frame):
    """Used by Manager. """
    
    log.info("received signal %i" % signum)
    global _ml
    if _ml is not None:
        _ml.quit()

def _init_main_loop():
    """Used by Manager. """
    
    global _ml
    
    if _ml is None:
        _ml = gobject.MainLoop()
        signal.signal(signal.SIGINT, _sighandler)
        signal.signal(signal.SIGTERM, _sighandler)
    
    return _ml

def _start_pa(pa):
    
    log.info("start player adapter")
    try:
        pa.start()
    except StandardError, e:
        log.error("failed to start player adapter (%s)" % e)
    except:
        log.exception("** BUG **")
    else:
        log.info("player adapter started")

def _stop_pa(pa):
    
    log.info("stop player adapter")
    try:
        pa.stop()
    except:
        log.exception("** BUG **")
    else:
        log.info("player adapter stopped")


class _DBusObserver():
    """Helper class used by Manager.
    
    A DBus observer automatically starts and stops a player adapter if the
    corresponding media player starts or stops.
    """
    
    def __init__(self, pa, dbus_name):
        """ Create a new DBusManager.
        
        @param pa:
            the PlayerAdapter to automatically start and stop
        @param dbus_name:
            the bus name used by the adapter's media player
        """

        DBusGMainLoop(set_as_default=True)

        self.__pa = pa
        self.__dbus_name = dbus_name
        
        try:
            bus = dbus.SessionBus()
        except DBusException, e:
            log.error("no dbus session bus (%s)" % e)
            return
        
        try:
            proxy = bus.get_object(dbus.BUS_DAEMON_NAME, dbus.BUS_DAEMON_PATH)
            self.__dbus = dbus.Interface(proxy, dbus.BUS_DAEMON_IFACE)
        except DBusException, e:
            log.error("failed to connect to dbus daemon (%s)" % e)
            return

        try:
            self.__handlers = (
                self.__dbus.connect_to_signal("NameOwnerChanged",
                                              self.__notify_owner_change,
                                              arg0=self.__dbus_name)
                ,
            )
            self.__dbus.NameHasOwner(self.__dbus_name,
                                     reply_handler=self.__set_has_owner,
                                     error_handler=self.__dbus_error)
        except DBusException, e:
            log.error("failed to talk with dbus daemon (%s)" % e)
            return
        
    def __notify_owner_change(self, name, old, new):
        
        log.debug("dbus name owner changed: '%s' -> '%s'" % (old, new))
        
        _stop_pa(self.__pa)
        
        if not new:
            return
        
        _start_pa(self.__pa)
    
    def __set_has_owner(self, has_owner):
        
        log.debug("dbus name has owner: %s" % has_owner)

        if not has_owner:
            return
        
        _start_pa(self.__pa)
    
    def __dbus_error(self, error):
        log.warning("dbus error: %s" % error)
        
    def disconnect(self):
        
        for handler in self.__handlers:
            handler.remove()
            
        self.__handlers = ()
        
        self.__dbus = None

class Manager(object):
    """ Manages life cycle of a player adapter.
    
    A Manager cares about calling a PlayerAdapter's start and stop methods.
    Additionally, because Remuco needs a GLib main loop to run, it sets up and
    manages such a loop.
    
    It is intended for player adapters running stand-alone, outside the players
    they adapt. A Manager is not needed for player adapters realized as a
    plugin for a media player. In that case the player's plugin interface
    should care about the life cycle of a player adapter (see the Rhythmbox
    player adapter as an example).
    
    To activate a manager call run().
    
    """
    
    def __init__(self, pa, player_dbus_name=None):
        """Create a new Manager.
        
        @param pa:
            the PlayerAdapter to manage
        @keyword player_dbus_name:
            if the player adapter uses DBus to communicate with its player set
            this to the player's well known bus name (see run() for more
            information)
        """

        self.__pa = pa
        
        self.__stopped = False
        
        self.__ml = _init_main_loop()

        if player_dbus_name is None:
            self.__dbus_observer = None
        else:
            log.info("start dbus observer")
            self.__dbus_observer = _DBusObserver(pa, player_dbus_name)
            log.info("dbus observer started")
        
    def run(self):
        """Activate the manager.
        
        This method starts the player adapter, runs a main loop (GLib) and
        blocks until SIGINT or SIGTERM arrives or until stop() gets called. If
        this happens the player adapter gets stopped and this method returns.
        
        @note: If the keyword 'player_dbus_name' has been set in __init__(),
            then the player adapter does not get started until an application
            owns the bus name given by 'player_dbus_name'. It automatically
            gets started whenever the DBus name has an owner (which means the
            adapter's player is running) and it gets stopped when it has no
            owner. Obvisously here the player adapter may get started and
            stopped repeatedly while this method is running.
        
        """
        if self.__dbus_observer is None: # start pa directly
            _start_pa(self.__pa)
        # else: dbus observer will start pa
            
        if not self.__stopped: # not stopped since creation 
            
            log.info("start main loop")
            try:
                self.__ml.run()
            except:
                log.exception("** BUG **")
            log.info("main loop stopped")
            
        if self.__dbus_observer is not None: # disconnect dbus observer
            log.info("stop dbus observer")
            self.__dbus_observer.disconnect()
            log.info("dbus observer stopped")
        
        # stop pa
        _stop_pa(self.__pa)
        
    def stop(self):
        """Manually shut down the manager.
        
        Stops the manager's main loop and player adapter. As a result a
        previous call to run() will return now.
        """
        
        log.info("stop manager manually")
        self.__stopped = True
        self.__ml.quit()
    
