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
op.add_option("-s", "--test-run", action="store_true", dest="test",
              default=False,
              help="ignore uncommitted changes and do not commit anything")

options, args = op.parse_args()

# -----------------------------------------------------------------------------
# utility functions
# -----------------------------------------------------------------------------

class CommandError(StandardError):
    pass

def command(cmd, rout=False):
    """Execute a command.
    
    @param cmd:
        the command to execute, either as a string which will be split into
        arguments on ' ', or already a list of arguments
    @keyword rout:
        see below
    @return:
        command output if keyword 'rout' is set True, empty string otherwise
    @raise CommandError:
        if command returns with error 
    
    """
    if not isinstance(cmd, list):
        cmd = cmd.split()
    print("  CMD: %s" % ' '.join(cmd))
    if rout:
        sp = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    else:
        sp = subprocess.Popen(cmd)
    out, err = sp.communicate()
    if sp.returncode != os.EX_OK:
        raise CommandError("Command failed, return code: %d" % sp.returncode)
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

    names_hg = command("hg log --template={author},", rout=True).split(',')
    names_hg = set(names_hg)
    names_hg.remove('')
    
    names = names.union(names_hg)
    names = [name for name in names if name not in cp.options("authormap")]
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
    
    build_dir = "release.build"
    dist_dir = "release.dist"
    pkg_src = "remuco-source-%s" % options.version
    pkg_all = "remuco-%s" % options.version
    pkg_src_dir = "%s/%s" % (build_dir, pkg_src)
    pkg_all_dir = "%s/%s" % (build_dir, pkg_all)
    pkg_src_tb = "%s/%s.tar.gz" % (dist_dir, pkg_src)
    pkg_all_tb = "%s/%s.tar.gz" % (dist_dir, pkg_all)
    
    for dir in (build_dir, dist_dir):
        if os.path.exists(dir):
            shutil.rmtree(dir)
        os.mkdir(dir)
    
    excludes = ""
    for exc in zip(*cp.items("tarball-exclude"))[1]:
        excludes += "--exclude %s " % exc 
    
    
    command("hg archive --rev %s --type tgz %s %s" %
            (options.version, excludes, pkg_src_tb))
    command("tar zxf %s -C %s" % (pkg_src_tb, build_dir))
    
    command("%s/%s/client/setup.sh" % (build_dir, pkg_src))
    command("ant -f %s/client/build.xml dist" % pkg_src_dir)
    
    shutil.move("%s/client/app" % pkg_src_dir, build_dir)
    shutil.rmtree(pkg_src_dir)
    command("tar zxf %s -C %s" % (pkg_src_tb, build_dir))
    shutil.move("%s/app" % build_dir, "%s/client" % pkg_src_dir )
    shutil.move(pkg_src_dir, pkg_all_dir)
    command("tar zcf %s -C %s %s" % (pkg_all_tb, build_dir, pkg_all))
    
    #shutil.rmtree(build_dir)

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
   
def main():
    
    if not options.version:
        raise op.error("need a version")
    
    if not options.test and grep(command("hg st", rout=True),
                                 "(^\?)|(^$)", invert=True):
        print("-> uncommitted changes, abort")
        sys.exit(1)
    
    if options.prepare:
        print("-> prepare release")
        refresh_api_doc()
        refresh_thanks()
        if not options.test:
            command(["hg", "ci", "-m",
                     "Final changes for release %s" %options.version])
    if options.tag:
        print("-> tag release")
        if not options.test:
            command("hg tag %s" % options.version)

    if options.tarball:
        print("-> build tarballs")
        tarball()

    print("-> done")
    
if __name__ == '__main__':
    main()