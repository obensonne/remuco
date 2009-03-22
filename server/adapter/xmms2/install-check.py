import sys

try:
    import xmmsclient
except ImportError:
    print("--> ERROR: Missing required Python module 'xmmsclient'!")
    sys.exit(1)