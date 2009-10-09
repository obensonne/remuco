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

"""Remuco client theme builder.

This tool reads in an SVG and exports and checks Remuco theme elements. Theme
elements are areas in the SVG occupied by the objects whose ID starts with
'rte.' (see layer 'Mask'). Before building a theme make sure the SVG's mask
layer is hidden.

Usage:
   theme-tool --all
   theme-tool [--dpi=DPI] --svg=SVG_FILE --name=NAME

Examples:
   $ ./theme-tool --all # all themes listed in themes.conf
   $ ./theme-tool --svg=Purple.svg --name=Purple
   $ ./theme-tool --dpi=135 --svg=Purple.svg --name=Purple-XXL
   
Default DPI is 90. Bigger/smaller DPI values produce bigger/smaller images.
"""

from ConfigParser import SafeConfigParser
import Image
import os
import os.path
import shutil
import subprocess
import sys

ELEMENTS = (
    # bottom bar elements
    "rte.buttonbar.left", "rte.buttonbar.spacer", "rte.buttonbar.right",
    "rte.button.fullscreen", "rte.button.next", "rte.button.prev",
    "rte.button.rate", "rte.button.tags",
     
    # top bar elements
    "rte.state.left", "rte.state.spacer", "rte.state.right",
    "rte.button.playback.pause", "rte.button.playback.play",
    "rte.button.playback.stop",
    "rte.button.repeat.off", "rte.button.repeat.on",
    "rte.button.shuffle.off", "rte.button.shuffle.on",
    "rte.slider.volume.left", "rte.slider.volume.off", "rte.slider.volume.on",
    "rte.slider.volume.right",
    
    # colors
    "rte.color.bg", "rte.color.text.album", "rte.color.text.artist",
    "rte.color.text.other", "rte.color.text.title",
    
    # misc icons
    "rte.icon.rating.off", "rte.icon.rating.on"
)

SAME_WIDTH = (
    ( "rte.icon.rating.off", "rte.icon.rating.on" ),
    ( "rte.button.playback.pause", "rte.button.playback.play",
      "rte.button.playback.stop" ),
    ( "rte.button.repeat.off", "rte.button.repeat.on" ),
    ( "rte.button.shuffle.off", "rte.button.shuffle.on" ),
    ( "rte.slider.volume.off", "rte.slider.volume.on" )
)

SAME_HEIGHT = (
    ( "rte.buttonbar.left", "rte.buttonbar.spacer", "rte.buttonbar.right",
      "rte.button.fullscreen", "rte.button.next", "rte.button.prev",
      "rte.button.rate", "rte.button.tags" ),
    ( "rte.state.left", "rte.state.spacer", "rte.state.right",
      "rte.button.playback.pause", "rte.button.playback.play",
      "rte.button.playback.stop",
      "rte.button.repeat.off", "rte.button.repeat.on",
      "rte.button.shuffle.off", "rte.button.shuffle.on",
      "rte.slider.volume.left", "rte.slider.volume.off", "rte.slider.volume.on",
      "rte.slider.volume.right" ),
    ( "rte.icon.rating.off", "rte.icon.rating.on" )
)

ONE_PIXEL = (
    "rte.color.bg", "rte.color.text.album", "rte.color.text.artist",
    "rte.color.text.other", "rte.color.text.title"
)

WIDTH_SPANNING_ELEMENTS = (
    "rte.state.left",
    "rte.button.playback.pause", "rte.state.spacer",
    "rte.button.repeat.off", "rte.state.spacer",
    "rte.button.shuffle.off", "rte.state.spacer",
    "rte.slider.volume.left", 10, 10, "rte.slider.volume.right",
    "rte.state.right"
)

HEIGHT_SPANNING_ELEMENTS = (
    "rte.state.left", 60, "rte.icon.rating.off", "rte.buttonbar.left"
)

DPI_DEFAULT=90

# -----------------------------------------------------------------------------

def exit_err(msg):
    """Print out error and exit."""
    print(msg)
    print("FAILED")
    sys.exit(1)

def get_size(dir, file_name):
    """Get size of the image file 'file_name' located in 'dir'."""

    file = os.path.join(dir, "%s.png" % file_name)
    try:
        return Image.open(file).size
    except IOError, e:
        exit_err("--| could not open '%s' (%s)" % (file, e))

def list_elements(svg):
    """Inspect 'svg' for alvailable theme elements."""

    p = subprocess.Popen(["inkscape", "--query-all", svg],
                         stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                         universal_newlines=True)
    r = p.wait()
    
    out, err = p.communicate()
    
    if (r != os.EX_OK):
        exit_err("--| failed to list theme elements in '%s'\n%s" % (svg, err))
    
    elems = []
    lines = out.split('\n')
    for line in lines:
        if line.startswith("rte."):
            elem = line.split(',')[0]
            elems.append(elem)
    elems.sort()
    return tuple(elems)
    
def export_elements(svg, dpi, dir):
    """Export all theme elements from 'svg' with DPI 'dpi' into 'dir'."""

    for elem in ELEMENTS:
        elem_file = os.path.join(dir, "%s.png" % elem)
        print("    %s" % elem)
        if elem in ONE_PIXEL:
            dpi = str(DPI_DEFAULT)
        p = subprocess.Popen(["inkscape", "-i", elem, "-f", svg, "-d", dpi,
                              "-e", elem_file],
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                             universal_newlines=True)
        r = p.wait()
        
        out, err = p.communicate()
        
        if (r != os.EX_OK):
            exit_err("--| failed to export theme element '%s' from '%s'\n%s" %
                     (elem, svg, err))
        

def generate_java_enum():
    """For easy copy and paste when theme elements change (see Theme.java)."""
    
    out = ""
    i = 0
    for elem in ELEMENTS:
        elem = elem.replace(".", "_")
        elem = elem.upper()
        if elem.startswith("RTE_COLOR"):
            elem = elem.replace("RTE_COLOR", "RTC")
        out += "%s = %d, " % (elem, i)
        i += 1
        
    print out

def build_one(svg, dpi, name):
    """Build a theme from 'svg' with DPI 'dpi' and target name 'name'."""

    dpi = str(dpi)

    print("--> building  theme %s (%s with DPI=%s)" % (name, svg, dpi))

    if not os.path.isfile(svg):
        exit_err("--| no such file: '%s'" % svg)
        
    dir = "build/%s" % name
    
    if os.path.exists(dir):
        print("--> remove build dir '%s'" % dir)
        try:
            shutil.rmtree(dir)
        except OSError, e:
            exit_err("--| failed to remove '%s' (%s)" % (dir, e))
        
    print("--> create build dir '%s'" % dir)
    try:
        os.mkdir(dir)
    except OSError, e:
        exit_err("--| failed to create '%s' (%s)" % (dir, e))
        
    print("--> inspect svg")

    elems = list_elements(svg)

    print("--> check list of theme elements")
    
    ok = True
    
    for need in ELEMENTS:
        if not need in elems:
            print("    missing    : %s" % need)
            ok = False
    for have in elems:
        if not have in ELEMENTS:
            print("    unexpected : %s" % have)
            ok = False

    if not ok:
        exit_err("--| list of expected and actual theme elements differ")

    print("--> export theme elements")

    export_elements(svg, dpi, dir)

    print("--> validate size of theme elements")

    for tuple in SAME_WIDTH:
        first = get_size(dir, tuple[0])[0]
        for elem in tuple[1:]:
            current = get_size(dir, elem)[0]
            if current != first:
                print("    %s and %s must have the same width, but %d != %d" %
                      (tuple[0], elem, first, current))
                ok = False

    for tuple in SAME_HEIGHT:
        first = get_size(dir, tuple[0])[1]
        for elem in tuple[1:]:
            current = get_size(dir, elem)[1]
            if current != first:
                print("    %s and %s must have the same height, but %d != %d" %
                      (tuple[0], elem, first, current))
                ok = False

    for elem in ONE_PIXEL:
        size = get_size(dir, elem)
        if size != (1, 1):
            print("    %s is not a one pixel element (size: %dx%d)" %
                  (elem, size[0], size[1]))
            ok = False
            

    if not ok:
        exit_err("--| validation failed")

    minw = 0
    for elem in WIDTH_SPANNING_ELEMENTS:
        if isinstance(elem, int):
            minw += elem
        else:
            size = get_size(dir, elem)
            minw += size[0]
            
    minh = 0
    for elem in HEIGHT_SPANNING_ELEMENTS:
        if isinstance(elem, int):
            minh += elem
        else:
            size = get_size(dir, elem)
            minh += size[1]
            
    print("--> recommended minimum screen size: %dx%d" % (minw, minh))
    
    print("--> finished, good job :)")

def build_all():
    """Build all default themes."""
    
    cp = SafeConfigParser()
    cp.read("themes.conf")
    
    for theme in cp.sections():
        build_one(cp.get(theme, "svg"), cp.get(theme, "dpi"), theme)
    
def main():

    svg = None
    name = None
    dpi=DPI_DEFAULT
    
    for arg in sys.argv[1:]:
        if arg.startswith("--dpi="):
            dpi=arg.split("=")[1]
        elif arg.startswith("--svg="):
            svg = arg.split("=")[1]
        elif arg.startswith("--name="):
            name = arg.split("=")[1]
        elif arg ==  "--all":
            svg = "all"
        elif arg == "--java":
            print_elements_in_java_style()
            sys.exit(os.EX_OK)
        else:
            print(__doc__)
            print("Unknown option: %s" % arg)
            sys.exit(os.EX_USAGE)
    
    if svg is None:
        print(__doc__)
        print("Need a theme file!")
        sys.exit(os.EX_USAGE)
        
    if name is None and svg != "all":
        print(__doc__)
        print("Need a theme name!")
        sys.exit(os.EX_USAGE)
    
    if not os.path.exists("build"):
        print("--> create build dir")
        try:
            os.mkdir("build")
        except OSError, e:
            exit_err("--| failed to create build dir (%s)" % e)

    if svg == "all":
        build_all()
    else:
        build_one(svg, dpi, name)

# -----------------------------------------------------------------------------

if __name__ == "__main__":

    main()