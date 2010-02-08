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

"""Utility functions to read/write simple str<->str dicts from/to files."""

import os.path

from remuco import log

def dict_to_string(dic, keys=None):
    """Flatten a dictionary.
    
    @param dic: The dictionary to flatten.
    @keyword only: A list of keys to include in the flattened string. This list
        also gives the order in which items are flattened. If only is None,
        all items are flattened in an arbitrary order.
        
    @return: the flattened dictionary as a string
    """
    
    flat = ""
    keys = keys or dic.keys()
    for key in keys:
        value = dic.get(key, "")
        value = value.replace(",", "_")
        flat += "%s:%s," % (key, value)
    flat = flat.strip(",")
    return flat

def string_to_dict(s, keys=None):
    """Create a dictionary from a flattened string representation.
    
    @param s: The string to build dictionary from.
    @keyword keys: A list of keys to include in dictionary. If keys is None,
        all items are flattened in an arbitrary order.
        
    @return: the dictionary
    
    """
    dic = {}
    items = s.split(",")
    for item in items:
        try:
            key, value = item.split(":", 1)
        except ValueError:
            key, value = item, ""
        if keys is None or key in keys:
            dic[key] = value
    return dic

def read_dicts_from_file(filename, flat=False, keys=None):
    """Read a list of dictionaries from a file.
    
    @param filename: Name of the file to read.
    @keyword flat: If True, the dictionaries are returned flattened, i.e. as
        strings.
    @keyword keys: See string_to_dict(). Only used if flat is False.
    
    @return: the list of dictionaries
    
    """
    if not os.path.exists(filename):
        return []
    
    lines = []
    try:
        fp = open(filename, "r")
        lines = fp.readlines()
        fp.close()
    except IOError, e:
        log.warning("failed to open %s (%s)" % (filename, e))
    
    dicts_flat = []
    for line in lines:
        line = line.replace("\n", "")
        line = line.strip(" ")
        if line.startswith("#") or len(line) == 0:
            continue
        dicts_flat.append(line)
        
    if flat:
        return dicts_flat
    
    dicts = []
    
    for dic_flat in dicts_flat:
        dicts.append(string_to_dict(dic_flat, keys=keys)) 

    return dicts

def write_dicts_to_file(filename, dicts, keys=None, comment=None):
    """Write a list of dictionaries into a file.
    
    @param filename: Name of the file to write into.
    @param dicts: Either a list of dictionaries or a list of strings, i.e.
        already flattened dictionaries.
    @keyword keys: See dict_to_string(). Only used if dictionaries are not yet
        flattened.
    @keyword comment: A comment text to put at the beginning of the file.
    
    """
    lines = []
    
    if comment:
        lines.append("%s\n" % comment)
        
    for dic in dicts:
        if not isinstance(dic, basestring):
            dic = dict_to_string(dic, keys=keys)
        lines.append("%s\n" % dic)
        
    try:
        fp = open(filename, "w")
        fp.writelines(lines)
        fp.close()
    except IOError, e:
        log.warning("failed to write to %s (%s)" % (filename, e))
    
