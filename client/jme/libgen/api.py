# =============================================================================
#
#    Remuco - A remote control system for media players.
#    Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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
"""
Script to generate Java source files based on an API signature file.

Usage:
   python api.py SIG_FILE DEST_DIR

Note: The only purpose of these source files is to build JARs which can be used
      within a class path while compiling other Java sources. They are by no
      means usable on their own!
"""
import os
import os.path
import sys
from xml.dom import minidom as xml

# Some super classes have no default constructor. These super classes are
# listed here, together with a dummy parameter list to pass to super(). 
SUPER_CONSTRUCTOR_PARAMS = {
    "javax.microedition.lcdui.Item" : "null",
    "javax.microedition.lcdui.game.Layer" : "0,0",
    "java.lang.ref.Reference" : "null"
}

# Some constant fields need to be reassigned for a successful compilation.
CONST_REPLACE = {
    "java.lang.Float" : {
        "MIN_VALUE" : "0",
        "MAX_VALUE" : "0",
        "NaN" : "0",
        "NEGATIVE_INFINITY" : "0",
        "POSITIVE_INFINITY" : "0" },
    "java.lang.Double" : {
        "NaN" : "0",
        "NEGATIVE_INFINITY" : "0",
        "POSITIVE_INFINITY" : "0" },
    "java.lang.Long" : {
        "MIN_VALUE" : 0,
        "MAX_VALUE" : 0 }
}

NUM_TYPES = ("byte", "short", "int", "long", "float", "double", "char")

def get_field_string(node_field, fqcn):
    
    modifier = node_field.getAttribute("modifiers")
    name = node_field.getAttribute("name")
    type = node_field.getAttribute("type")
    const = node_field.getAttribute("constant-value")
    
    if type == "java.lang.String" and const:
        const = '"%s"' % const
        
    if const and fqcn in CONST_REPLACE and name in CONST_REPLACE[fqcn]:
        const = CONST_REPLACE[fqcn][name] 
    
    if "final" in modifier and not const:
        if type in NUM_TYPES:
            const = "0"
        elif type == "boolean":
            const = "false"
        else:
            const = "null"
            
    if const:
        const = "= %s" % const
    
    field = "%s %s %s %s;" % (modifier, type, name, const)
    
    return field
    

def get_throws_string(nl_exceptions):
    
    throws = ""
    if nl_exceptions:
        throws = "throws " 
        for node in nl_exceptions:
            throws += "%s, " % node.getAttribute("name")
        throws = throws[:-2]
    return throws
        
def get_parameters_string(nl_parameters):
    
    parameters = ""
    if nl_parameters:
        parameters = "" 
        i = 0
        for node in nl_parameters:
            parameters += "%s arg%d, " % (node.getAttribute("type"), i)
            i += 1
        parameters = parameters[:-2]
    return parameters

def get_constructor_string(xc, name, super_class):
    
    modifier = xc.getAttribute("modifiers")
    throws = get_throws_string(xc.getElementsByTagName("exception"))
    parameters = get_parameters_string(xc.getElementsByTagName("parameter"))
    
    if super_class in SUPER_CONSTRUCTOR_PARAMS:
        body = "super(%s);" % SUPER_CONSTRUCTOR_PARAMS[super_class]
    else:
        body = ""
    
    constructor = "%s %s (%s) %s { %s };" % (modifier, name, parameters,
                                             throws, body)
    
    return constructor

def get_method_string(xc, is_interface):
    
    name = xc.getAttribute("name")
    modifier = xc.getAttribute("modifiers")
    throws = get_throws_string(xc.getElementsByTagName("exception"))
    parameters = get_parameters_string(xc.getElementsByTagName("parameter"))
    ret = xc.getAttribute("return")
    
    if "abstract" in modifier or is_interface:
        body = ""
    else:
        body = "{ return "
        if ret == "void":
            pass
        elif ret in NUM_TYPES:
            body += "0"
        elif ret == "boolean":
            body += "false"
        else:
            body += "null"
        body += "; }"
    
    method = "%s %s %s (%s) %s %s;" % (modifier, ret, name, parameters, throws, body)
    
    return method

def get_interfaces(xc):
    
    interfaces = []
    
    nl_implements = xc.getElementsByTagName("implements")
    if nl_implements:
        nl_interface = nl_implements[0].getElementsByTagName("interface")
        if nl_interface:
            for n_interface in nl_interface:
                interfaces.append(n_interface.getAttribute("name"))

    return interfaces

def get_class_identifiers(xc, dir):
    
    name_full = xc.getAttribute("name")
    splitted = name_full.split(".")
    name_short = splitted[-1]
    package = splitted[0]
    path = "%s/%s" % (dir, splitted[0])
    for package_level in splitted[1:-1]:
        package += ".%s" % package_level
        path += "/%s" % package_level
        
    file_name = "%s/%s.java" % (path, name_short)
    
    return name_full, name_short, package, path, file_name

def generate_class(xc, is_interface, dir):
    
    c_fqcn, c_name, c_package, c_path, c_file = get_class_identifiers(xc, dir)
    
    c_modifiers = xc.getAttribute("modifiers")
    c_super = xc.getAttribute("extends")
    if c_super:
        c_extends = "extends %s" % c_super
    else:
        c_extends = ""
        
    interfaces = get_interfaces(xc)
    if interfaces:
        if is_interface:
            c_implements = "extends "
        else:
            c_implements = "implements "
        for interface in interfaces:
            c_implements += "%s, " % interface
        c_implements = c_implements[:-2]
    else:
        c_implements = ""
        
    content = "package %s;\n" % c_package
    if is_interface:
        c_type = "interface"
    else:
        c_type = "class"
    content += "%s %s %s %s %s {\n" % (c_modifiers, c_type, c_name, c_extends,
                                       c_implements)
    
    nl = xc.getElementsByTagName("field")
    for node in nl:
        content += get_field_string(node, c_fqcn) + "\n"
    nl = xc.getElementsByTagName("constructor")
    for node in nl:
        super_class = xc.getAttribute("extends")
        content += get_constructor_string(node, c_name, c_super) + "\n"
    nl = xc.getElementsByTagName("method")
    for node in nl:
        content += get_method_string(node, is_interface) + "\n"
    
    content += "}\n"
    
    #print content
    
    print("generate %s" % c_file)
    
    if not os.path.exists(c_path):
        os.makedirs(c_path)
        
    the_file = open(c_file, "w")
    the_file.write(content)
    the_file.close()

def generate(xs, dir):
    
    classes = xs.getElementsByTagName("class")
    interfaces = xs.getElementsByTagName("interface")
    
    for node in xs.childNodes:
        if node.nodeName == "class":
            generate_class(node, False, dir)
        elif node.nodeName == "interface":
            generate_class(node, True, dir)
        else:
            pass
            #print("unknown node: %s (%s)" % (node.nodeName, node.nodeValue))

if __name__ == '__main__':
    
    dom = xml.parse(sys.argv[1])
    
    generate(dom.getElementsByTagName("signature")[0], sys.argv[2])
    
    