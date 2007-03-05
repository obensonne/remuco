# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.4.0"
__copyright__ = "Copyright (c) 2007 Christian Bünnig"
__license__ = "GPL2"

import pydcop, pcop

# amarok dcop-connection (global reference)
app = None

# the current playlist as a list of xml elements (global reference)
songs = []

# iterator for the songs above (global reference)
songs_iter = None

