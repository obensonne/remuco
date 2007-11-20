from distutils.core import setup, Extension
import commands
import os

DEBUG = os.getenv("DEBUG")

if DEBUG == "yes":
    macros = [ ('LOGLEVEL', 'LL_DEBUG') ]
    macros_off = [ 'NDEBUG' ]
else:
    macros = [ ('LOGLEVEL', 'LL_INFO') ]
    macros_off = []

inc_dirs = commands.getoutput("pkg-config glib-2.0 remuco --cflags").replace('-I','').split()
libs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-l").replace('-l','').split()
lib_dirs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-L").replace('-L','').split()
src_files = ['module.c', 'functions.c', 'constants.c', 'types/ppdesc.c', 'types/ppcb.c', 'types/pstatus.c']


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
