#!/usr/bin/python

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

from __future__ import with_statement

import ConfigParser
import optparse
import os
import os.path
import pydoc
import re
import shutil
import subprocess
import sys

"""Tool to assist in creating Remuco releases."""

# -----------------------------------------------------------------------------
# configuration
# -----------------------------------------------------------------------------

cp = ConfigParser.SafeConfigParser()
cp.read("release.conf")

sys.path.insert(0, cp.get("paths", "remuco"))

import remuco

# -----------------------------------------------------------------------------
# options
# -----------------------------------------------------------------------------

op = optparse.OptionParser("usage: %prog -v VERSION [options] [actions]")

og = optparse.OptionGroup(op, "Actions")

op.add_option_group(og)
og.add_option("-p", "--prepare", action="store_true", dest="prepare",
              default=False, help="prepare a release")
og.add_option("-t", "--tag", action="store_true", dest="tag",
              default=False, help="tag a release")
og.add_option("-a", "--tarball", action="store_true", dest="tarball",
              default=False, help="build release tarballs")

op.add_option("-r", "--release", dest="version",
              help="release version")
op.add_option("-d", "--dry-run", action="store_false", dest="commit",
              default=True, help="do not commit anything")

options, args = op.parse_args()

# -----------------------------------------------------------------------------
# utility functions
# -----------------------------------------------------------------------------

class CommandError(StandardError):
    pass

def command(cmd):
    if not isinstance(cmd, list):
        cmd = cmd.split()
    sp = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    out, err = sp.communicate()
    if err:
        raise CommandError(err)
    return out

def grep(lines, rex, invert=False):
    """Grep some lines for the given expression."""
    
    if not isinstance(lines, list):
        lines = lines.split("\n")
    if invert:
        return [e for e in lines if not re.match(rex, e)]
    else:
        return [e for e in lines if re.match(rex, e)]

# -----------------------------------------------------------------------------
# API doc generation
# -----------------------------------------------------------------------------

def refresh_api_doc():
    """Update and sanitize the API doc."""

    pydoc.writedoc(remuco)
    
    patt_module_link = r'href="[^"]+\.html'
    repl_module_link = 'href="api.html'
    patt_file_link = r'<a href="[^"]+">index</a><br><a href="[^"]+">[^<]+</a>'
    repl_file_link = ''
    
    with open("remuco.html", 'r') as api:
        content = api.read()
        
    os.remove("remuco.html")
        
    content = re.sub(patt_module_link, repl_module_link, content)
    content = re.sub(patt_file_link, repl_file_link, content)
    
    with open(cp.get("paths", "api"), 'w') as api:
        api.write(content)
        
# -----------------------------------------------------------------------------
# THANKS file
# -----------------------------------------------------------------------------

def refresh_thanks():
    """Update the THANKS file."""
    
    with open(cp.get("paths", "thanks"), 'r') as thanks:
        preamble, names = thanks.read().split("\n\n")
        
    names = names.split("\n")
    names = set(names)
    names.remove('')

    names_hg = command("hg log --template={author},").split(',')
    names_hg = set(names_hg)
    names_hg.remove('')
    
    names = names.union(names_hg)
    names_ex = zip(*cp.items("authormap"))[0]
    names = [name for name in names if name not in names_ex]
    names.sort()
    
    with open(cp.get("paths", "thanks"), 'w') as thanks:
        thanks.write(preamble)
        thanks.write("\n\n")
        for name in names:
            thanks.write("%s\n" % name)

# -----------------------------------------------------------------------------
# Tarballs
# -----------------------------------------------------------------------------
   
def tarball():
    """Build release tarballs."""
    print("TODO")

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
   
def main():
    
    if not options.version:
        raise op.error("need a version")
    
    if grep(command("hg st"), "(^\?)|(^$)", invert=True):
        print("-> uncommitted changes, abort")
        sys.exit(1)
    
    if options.prepare:
        print("-> prepare release")
        refresh_api_doc()
        refresh_thanks()
        if options.commit:
            command(["hg", "ci", "-m",
                     "Final changes for release %s" %options.version])
    if options.tag:
        print("-> tag release")
        if options.commit:
            command("hg tag %s" % options.version)

    if options.tarball:
        print("-> build tarballs")
        tarball()

    print("-> done")
    
if __name__ == '__main__':
    main()