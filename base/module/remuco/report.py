# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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

"""Remuco report handler."""

import ConfigParser
import os.path
import random

import dbus
from dbus.exceptions import DBusException
from dbus.mainloop.glib import DBusGMainLoop

from remuco import config
from remuco import log

REPORT_FILE = config.get_report_file()

# =============================================================================
# log and notify new clients
# =============================================================================

def notify_client_connected(device, conn):
    """Save a client's device info report."""
    
    log.debug("client device: %s" % device)
    
    device = device.copy()
    
    try:
        sec = "%s.%s" % (device.pop("name"), conn)
    except KeyError:
        log.warning("client device name unknown, no report")
        return
    
    cp = ConfigParser.SafeConfigParser()
    
    try:
        cp.read(REPORT_FILE)
    except ConfigParser.Error, e:
        log.warning("failed to read %s (%s) -> reset report" % (REPORT_FILE, e))
    
    if not cp.has_section(sec):
        _user_notification()
        cp.add_section(sec)
    
    for key, value in device.items():
        cp.set(sec, key, value)
    
    try:
        fp = open(REPORT_FILE, 'w')
        fp.write(REPORT_COMMENT)
        cp.write(fp)
        fp.close()
    except IOError, e:
        log.warning("failed to save report to %s (%s)" % (REPORT_FILE, e))

#class Notifier(object):
#    
#    def __init__(self):
#
#        DBusGMainLoop(set_as_default=True)
#        
#        try:
#            bus = dbus.SessionBus()
#        except DBusException, e:
#            log.error("no dbus session bus (%s)" % e)
#            return
#        
#        try:
#            proxy = bus.get_object("org.freedesktop.Notifications",
#                                   "/org/freedesktop/Notifications")
#            notid = dbus.Interface(proxy, "org.freedesktop.Notifications")
#        except DBusException, e:
#            log.error("failed to connect to notification daemon (%s)" % e)
#            return
#    
#        try:
#            caps = notid.GetCapabilities()
#        except DBusException, e:
#            return
#        
#        if True:
#        #if "actions" in caps:
#            text = "Please send device information to the Remuco developers."
#            actions = ["mail", "Ok, please prepare a report mail"]
#            timeout = 20
#        elif "body-markup" in caps:
#            text = "Please run the tool <b>remuco-report</b> !"
#            actions = []
#            timeout = 15
#        else:
#            text = "Please run the tool remuco-report !"
#            actions = []
#            timeout = 15
#            
#        self.__dh_action = None
#        self.__dh_closed = None
#        
#        try:
#            caps = notid.GetCapabilities()
#            self.__id = notid.Notify("Remuco", 0, "phone", "New Remuco Client",
#                                     text, actions, {}, timeout)
#            log.debug("nid: %d" % self.__id)
#        except DBusException, e:
#            log.warning("user notification failed (%s)" % e)
#            return
#        
#        if actions:
#            try:
#                self.__dh_closed = notid.connect_to_signal("NotificationClosed",
#                                                           self.__closed)
#                self.__dh_action = notid.connect_to_signal("ActionInvoked",
#                                                           self.__action)
#            except DBusException, e:
#                log.warning("unable to connect to signals (%s)" % e)
#                return
#            
#    def __action(self, id, key):
#        log.debug("action - id: %d, key: %s" % (id, key))
#        if id == self.__id:
#            
#            self.__dh_action.remove()
#            self.__dh_action = None
#    
#    def __closed(self, id, reason):
#        # action: 3, "mail"
#        # cancel: 2
#        # ok: 3, "default"
#        # close: 2
#        log.debug("closed - id: %d, reason: %d" % (id, reason))
#        if id == self.__id:
#            self.__dh_closed.remove()
#            self.__dh_closed = None
#            if reason != 3:
#                self.__dh_action.remove()
#                self.__dh_action = None
            
def _user_notification():

    try:
        bus = dbus.SessionBus()
    except DBusException, e:
        log.error("no dbus session bus (%s)" % e)
        return
    
    try:
        proxy = bus.get_object("org.freedesktop.Notifications",
                               "/org/freedesktop/Notifications")
        notid = dbus.Interface(proxy, "org.freedesktop.Notifications")
    except DBusException, e:
        log.error("failed to connect to notification daemon (%s)" % e)
        return

    try:
        caps = notid.GetCapabilities()
    except DBusException, e:
        return
    
    if "body-markup" in caps:
        text = "Please run the tool <b>remuco-report</b> !"
    else:
        text = "Please run the tool remuco-report !"
        
    try:
        notid.Notify("Remuco", 0, "phone", "New Remuco Client", text, [], {},
                     15)
    except DBusException, e:
        log.warning("user notification failed (%s)" % e)
        return
        
# =============================================================================
# report mailing
# =============================================================================

# simple encoding, just to not show the address in plain text
REPORT_MAIL = "ZXJjYmVnQGVyemhwYi5iZXQ=\n".decode("base64").decode("rot13")

REPORT_COMMENT = """# Remuco client report.

# This report contains information about the mobile devices Remuco
# has been used with. Please send it to %s whenever
# new Remuco client devices have connected to this computer. This
# helps the developers of Remuco to better understand on which devices
# Remuco works and is used.

# If a device name below is set to "unknown", please fill in the real
# device name (e.g. Motorola-KRZR-K1).

# Thank you for your contribution !

""" % REPORT_MAIL

_MSG_NO_REPORT = (
"Report file '%s' does not exist yet.\n\n"
"Please try again later after using Remuco a while." % REPORT_FILE)

_MSG_READ_ERROR = "Failed to read the report file %s." % REPORT_FILE

_MSG_MAIL_FAILED = (
"Could not open your mail application to send a report mail to the Remuco "
"developers.\n\n"
"Please mail the report file %s manually to %s.\n\n"
"Thank you for your contribution!" %
(REPORT_FILE, REPORT_MAIL))

_MSG_MAIL_OK = (
"It looks like opening your mail application to send a report mail to the "
"Remuco developers was successful.\n\n"
"If not, please mail the report file %s manually to %s.\n\n"
"Thank you for your contribution!" %
(REPORT_FILE, REPORT_MAIL))

def mail():
    
    import subprocess
    
    if not os.path.exists(REPORT_FILE):
        return _MSG_NO_REPORT
    
    try:
        fo = open(REPORT_FILE, 'r')
        report = fo.read()
        fo.close()
    except IOError, e:
        return "%s %s" % (_MSG_READ_ERROR, e)
    
    try:
        subprocess.Popen(["xdg-email", "--utf8", "--subject", "remuco report",
                          "--body", "%s" % report, REPORT_MAIL])
    except OSError, e:
        return _MSG_MAIL_FAILED
    else:
        return _MSG_MAIL_OK

if __name__ == '__main__':
    
    msg = mail()
    print(msg)