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

import httplib
import os
import os.path
import urllib

import dbus
from dbus.exceptions import DBusException

from remuco.config import DEVICE_FILE
from remuco import dictool
from remuco import log

__HOST = "remuco.sourceforge.net"
__LOC = "/cgi-bin/report"

__DEVICE_FILE_COMMENT = """# Seen Remuco client devices.
#
# The information in this file is sent to remuco.sourceforge.net if you run
# the tool 'remuco-report'. It is used to set up a list of Remuco compatible
# mobile devices.
#
"""

# Fields of a client device info to log.
__FIELDS = ("name", "version", "conn", "utf8", "touch")

def log_device(device):
    """Log a client device."""
    
    device = dictool.dict_to_string(device, keys=__FIELDS)
    
    seen_devices = dictool.read_dicts_from_file(DEVICE_FILE, flat=True,
                                                keys=__FIELDS)
    
    if not device in seen_devices:
        __user_notification("New Remuco Client",
                            "Please run the tool <b>remuco-report</b> !")
        seen_devices.append(device)
        dictool.write_dicts_to_file(DEVICE_FILE, seen_devices,
                                    comment=__DEVICE_FILE_COMMENT)

def __user_notification(summary, text):
    """Notify the user that a new device has been loggend."""

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
    
    if not "body-markup" in caps:
        text = text.replace("<b>", "")
        text = text.replace("</b>", "")
        
    try:
        notid.Notify("Remuco", 0, "phone", summary, text, [], {}, 15)
    except DBusException, e:
        log.warning("user notification failed (%s)" % e)
        return

def __send_device(device):
    """Send a single device."""
        
    print("sending %s" % device)
    
    params = urllib.urlencode(device)
    #print(params)
    headers = {"Content-type": "application/x-www-form-urlencoded",
               "Accept": "text/plain"}
    try:
        conn = httplib.HTTPConnection(__HOST)
        conn.request("POST", __LOC, params, headers)
        response = conn.getresponse()
    except IOError, e:
        return -1, str(e) 
    response.read() # needed ?
    conn.close()
    
    return response.status, response.reason

def __send_devices():
    """Send all seen devices.
    
    @return: True if sending was successful, False if something failed
    """
    
    device_list = dictool.read_dicts_from_file(DEVICE_FILE, flat=False,
                                               keys=__FIELDS)
    ok = True

    for device in device_list:
        # add a simple watchword marking this report as a real one
        device["ww"] = "sun_is_shining"
        status, reason = __send_device(device)
        if status != httplib.OK:
            print("-> failed (%s - %s)" % (status, reason))
            if status == httplib.NOT_FOUND:
                print("   the submission link I'm using may be outdated")
            ok = False
        else:
            print("-> ok")

    return ok

if __name__ == '__main__':
    
    import sys
    if len(sys.argv) == 2:
        if sys.argv[1] == "send":
            ok = __send_devices()
            if ok:
                sys.exit(os.EX_OK)
            else:
                sys.exit(os.EX_TEMPFAIL)
        elif sys.argv[1] == "dump":
            devices = dictool.read_dicts_from_file(DEVICE_FILE, flat=True)
            for dev in devices:
                print(dev)
            sys.exit(os.EX_OK)
    
    sys.exit(os.EX_USAGE)

