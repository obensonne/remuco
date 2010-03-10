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

"""Manage life cycle of stand-alone (not plugin based) player adapters."""

import signal

import gobject

from remuco import log

try:
    import dbus
    from dbus.exceptions import DBusException
    from dbus.mainloop.glib import DBusGMainLoop
except ImportError:
    log.warning("dbus not available - dbus using player adapters will crash")

# =============================================================================
# global items and signal handling
# =============================================================================

_ml = None

def _sighandler(signum, frame):
    
    log.info("received signal %i" % signum)
    if _ml is not None:
        _ml.quit()

# =============================================================================
# start stop functions
# =============================================================================

def _start_pa(pa):
    """Start the given player adapter with error handling."""
    
    log.info("start player adapter")
    try:
        pa.start()
    except StandardError, e:
        log.error("failed to start player adapter (%s)" % e)
        return False
    except Exception, e:
        log.exception("** BUG ** %s", e)
        return False
    else:
        log.info("player adapter started")
        return True

def _stop_pa(pa):
    """Stop the given player adapter with error handling."""
    
    log.info("stop player adapter")
    try:
        pa.stop()
    except Exception, e:
        log.exception("** BUG ** %s", e)
    else:
        log.info("player adapter stopped")

# =============================================================================
# Polling Observer
# =============================================================================

class _PollingObserver():
    """Polling based observer for a player's run state.
    
    A polling observer uses a custom function to periodically check if a media
    player is running and automatically starts and stops the player adapter
    accordingly.
    
    """
    def __init__(self, pa, poll_fn):
        """Create a new polling observer.
        
        @param pa:
            the PlayerAdapter to automatically start and stop
        @param poll_fn:
            the function to call periodically to check if the player is running
            
        """
        self.__pa = pa
        self.__poll_fn = poll_fn
        self.__sid = gobject.timeout_add(5123, self.__poll, False)
        
        gobject.idle_add(self.__poll, True)
        
    def __poll(self, first):
        
        running = self.__poll_fn()
        if running and self.__pa.stopped:
            _start_pa(self.__pa)
        elif not running and not self.__pa.stopped:
            _stop_pa(self.__pa)
        # else: nothing to do
        
        return first and False or True
        
    def stop(self):
        
        gobject.source_remove(self.__sid)

# =============================================================================
# DBus Observer
# =============================================================================

class _DBusObserver():
    """DBus based observer for a player's run state.
    
    A DBus observer uses DBus name owner change notifications to
    automatically start and stop a player adapter if the corresponding
    media player starts or stops.
    
    """
    def __init__(self, pa, dbus_name):
        """Create a new DBus observer.
        
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
                                              self.__on_owner_change,
                                              arg0=self.__dbus_name),
            )
            self.__dbus.NameHasOwner(self.__dbus_name,
                                     reply_handler=self.__reply_has_owner,
                                     error_handler=self.__dbus_error)
        except DBusException, e:
            log.error("failed to talk with dbus daemon (%s)" % e)
            return
        
    def __on_owner_change(self, name, old, new):
        
        log.debug("dbus name owner changed: '%s' -> '%s'" % (old, new))
        
        _stop_pa(self.__pa)
        if new:
            _start_pa(self.__pa)
    
    def __reply_has_owner(self, has_owner):
        
        log.debug("dbus name has owner: %s" % has_owner)
        
        if has_owner:
            _start_pa(self.__pa)
    
    def __dbus_error(self, error):
        
        log.warning("dbus error: %s" % error)
        
    def stop(self):
        
        for handler in self.__handlers:
            handler.remove()
        self.__handlers = ()
        self.__dbus = None

# =============================================================================
# Manager
# =============================================================================

class Manager(object):
    """Life cycle manager for a stand-alone player adapter.
    
    A manager cares about calling a PlayerAdapter's start and stop methods.
    Additionally, because Remuco needs a GLib main loop to run, it sets up and
    manages such a loop.
    
    It is intended for player adapters running stand-alone, outside the players
    they adapt. A manager is not needed for player adapters realized as a
    plugin for a media player. In that case the player's plugin interface
    should care about the life cycle of a player adapter (see the Rhythmbox
    player adapter as an example).
    
    """
    def __init__(self, pa, dbus_name=None, poll_fn=None):
        """Create a new manager.
        
        @param pa:
            the PlayerAdapter to manage
        @keyword dbus_name:
            if the player adapter uses DBus to communicate with its player set
            this to the player's well known bus name (see run() for more
            information)
        @keyword poll_fn:
            if DBus is not used, this function may be set for periodic checks
            if the player is running, used to automatically start and stop the
            player adapter
        
        When neither `dbus_name` nor `poll_fn` is given, the adapter is started
        immediately, assuming the player is running and the adapter is ready to
        work.
        
        """
        self.__pa = pa
        self.__pa.manager = self
        self.__stopped = False
        self.__observer = None
        
        global _ml
        if _ml is None:
            _ml = gobject.MainLoop()
            signal.signal(signal.SIGINT, _sighandler)
            signal.signal(signal.SIGTERM, _sighandler)
        self.__ml = _ml

        if dbus_name:
            log.info("start dbus observer")
            self.__observer = _DBusObserver(pa, dbus_name)
        elif poll_fn:
            log.info("start polling observer")
            self.__observer = _PollingObserver(pa, poll_fn)
        else:
            # nothing to do
            pass
        
    def run(self):
        """Activate the manager.
        
        This method starts the player adapter, runs a main loop (GLib) and
        blocks until SIGINT or SIGTERM arrives or until stop() gets called. If
        this happens the player adapter gets stopped and this method returns.
        
        If `player_dbus_name` or `poll_fn` has been passed to __init__(), then
        the player adapter does not get started until the player is running
        (according to checks based on the DBus name or poll function). Also the
        adapter gets stopped automatically if the player is not running
        anymore. However, the manager keeps running, i.e. the player adapter
        may get started and stopped multiple times while this method is
        running.
        
        """
        if self.__observer is None: # start pa directly
            ready = _start_pa(self.__pa)
        else: # observer will start pa
            ready = True
            
        if ready and not self.__stopped: # not stopped since creation 
            log.info("start main loop")
            try:
                self.__ml.run()
            except Exception, e:
                log.exception("** BUG ** %s", e)
            else:
                log.info("main loop stopped")
            
        if self.__observer: # stop observer
            self.__observer.stop()
            log.info("observer stopped")
        
        # stop pa
        _stop_pa(self.__pa)
        
    def stop(self):
        """Shut down the manager.
        
        Stops the manager's main loop and player adapter. As a result a
        previous call to run() will return now. This should be used by player
        adapters when there is a crucial error and restarting the adapter won't
        fix this.
        
        """
        log.info("manager stopped internally")
        self.__stopped = True
        self.__ml.quit()

class NoManager(object):
    """Dummy manager which can be stopped - does nothing.
    
    Initially this manager is assigned to every PlayerAdapter. That way it is
    always safe to call PlayerAdapter.manager.stop() even if an adapter has not
    yet or not at all a real Manager.
    
    """
    def stop(self):
        """Stop me, I do nothing."""
