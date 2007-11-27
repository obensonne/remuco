from distutils.core import setup, Extension
import commands
import os

DEBUG = os.getenv("DEBUG")
DO_LOG_NOISE = os.getenv("DO_LOG_NOISE")

macros = []
macros_off = []

if DEBUG == "yes":
    macros += [ ('DEBUG', '') ]
    macros_off += [ 'NDEBUG' ]

if DO_LOG_NOISE == "yes":
    macros += [ ('DO_LOG_NOISE', '') ]

inc_dirs = commands.getoutput("pkg-config glib-2.0 remuco --cflags").replace('-I','').split()
libs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-l").replace('-l','').split()
lib_dirs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-L").replace('-L','').split()
src_files = ['src/module.c', 'src/functions.c', 'src/constants.c',
             'src/types/ppdesc.c', 'src/types/ppcb.c', 'src/types/pstatus.c']


#print "libs: " + str(libs)

module1 = Extension('remuco',
                    define_macros = macros,
                    undef_macros = macros_off,
                    include_dirs = inc_dirs,
                    libraries = libs,
                    library_dirs = lib_dirs,
                    sources = src_files)


setup (name = 'Remuco',
       version = '0.6.0',
       author = 'Christian Buennig',
       author_email = 'mondai@users.sourceforge.net',
       url = 'http://remuco.sourceforge.net',
       license = 'GPLv3',
       description = 'Python interface to the Remuco server library',
       long_description = '''
   Python interface to the Remuco server library for Remuco player proxies
   written in Python.
   ''',
       ext_modules = [module1])
