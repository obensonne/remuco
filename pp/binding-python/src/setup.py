from distutils.core import setup, Extension
import commands

debug = True

if debug:
    macros = [ ('DEBUG',''), ('LOGLEVEL', 'LL_DEBUG') ]
    macros_off = [ 'NDEBUG' ]
else:
    macros = [ ('G_DISABLE_ASSERT',''), ('LOGLEVEL', 'LL_INFO') ]
    macros_off = []

# use this when to compile against an installed Remuco server library
inc_dirs = commands.getoutput("pkg-config glib-2.0 remuco --cflags").replace('-I','').split()
#inc_dirs.append("../../lib/src")
libs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-l").replace('-l','').split()
lib_dirs = commands.getoutput("pkg-config glib-2.0 remuco --libs-only-L").replace('-L','').split()
#lib_dirs.append("../../lib/src/remuco") # to build within source tree withoud installed server library

# use this when to compile against the server lib within the source tree
#inc_dirs = commands.getoutput("pkg-config glib-2.0 --cflags").replace('-I','').split()
#inc_dirs.append("../../lib/src")
#libs = commands.getoutput("pkg-config glib-2.0 --libs-only-l").replace('-l','').split()
#libs.append("remuco")
#lib_dirs = commands.getoutput("pkg-config glib-2.0 --libs-only-L").replace('-L','').split()
#lib_dirs.append("../../lib/src/remuco")

#print "libs: " + str(libs)

module1 = Extension('remuco',
                    define_macros = macros,
                    undef_macros = macros_off,
                    include_dirs = inc_dirs,
                    libraries = libs,
                    library_dirs = lib_dirs,
                    sources = ['binding.c'])


setup (name = 'Remuco',
       version = '0.6.0',
       author = 'Christian Buennig',
       author_email = 'mondai@users.sourceforge.net',
       url = 'http://remuco.sourceforge.net',
       license = 'GPLv3',
       description = 'Python interface to the Remuco server library',
       long_description = '''
   Python interface to the Remuco server library for Remuco player proxies
   writtten in Python.
   ''',
       ext_modules = [module1])