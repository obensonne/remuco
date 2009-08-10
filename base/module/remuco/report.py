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
import os.path
import urllib

import dbus
from dbus.exceptions import DBusException

from remuco.config import DEVICE_FILE
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
__FIELDS = ("version", "conn", "name")

def log_device(device):
    """Log a client device."""
    
    new_device = ""
    for key in __FIELDS:
        new_device += "%s:%s," % (key, device.get(key,"unknown"))
    new_device = new_device.strip(",")
    
    device_list = __load_device_list(flat=True)
    
    if not new_device in device_list:
        __user_notification("New Remuco Client",
                            "Please run the tool <b>remuco-report</b> !")
        device_list.append(new_device)
        lines = [__DEVICE_FILE_COMMENT] + device_list
        try:
            fp = open(DEVICE_FILE, "w")
            for line in lines:
                fp.write("%s\n" % line)
            fp.close()
        except IOError, e:
            log.warning("failed to write to %s (%s)" % (DEVICE_FILE, e))
            return

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

def __load_device_list(flat=True):
    """Load all known devices from the device file.
    
    @keyword flat: if True, then devices are flat strings, if False, devices
        are dictionaries with the fields listed in __FIELDS.
        
    @return: the list of devices
    
    """
    lines = []
    if os.path.exists(DEVICE_FILE):
        try:
            fp = open(DEVICE_FILE, "r")
            lines = fp.readlines()
            fp.close()
        except IOError, e:
            log.warning("failed to open %s (%s)" % (DEVICE_FILE, e))
    
    device_list_flat = []
    for line in lines:
        line = line.replace("\n", "")
        line = line.strip(" ")
        if line.startswith("#") or len(line) == 0:
            continue
        device_list_flat.append(line)
    
    if flat:
        return device_list_flat
    
    device_list = []
    for device_flat in device_list_flat:
        device = {}
        try:
            fields = device_flat.split(",", len(__FIELDS) - 1)
            for field in fields:
                #print field
                key, value = field.split(":", 1)
                device[key] = value
            device_list.append(device)
        except ValueError:
            log.warning("bug or bad line in device file: %s" % device_flat)

    return device_list
    
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
    data = response.read()
    #print(str(data))
    conn.close()
    
    return response.status, response.reason

def __send_devices():
    """Send all seen devices.
    
    @return: True if sending was successful, False if something failed
    """
    
    device_list = __load_device_list(flat=False)

    ok = True

    for device in device_list:
        # add a simple watchword marking this report as a real one
        device["watchword"] = "sun_is_shining"
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
    ok = __send_devices()
    sys.exit(int(not ok))

