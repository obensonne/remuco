# -*- coding: utf-8 -*-

__author__ = "Christian Bünnig <mondai@users.sourceforge.net>"
__version__ = "0.4.0"
__copyright__ = "Copyright (c) 2007 Christian Bünnig"
__license__ = "GPL2"

import pydcop, pcop
#import logging

try:
    app = pydcop.anyAppCalled("amarok")
except:
    print("ERROR: Could not connect with dcop !")
    print("       Maybe dcopserver is not running. In this case you can solve")
    print("       this problem by starting Amarok.")


#if not app:
#    logging.info("amarok is down")
#else:
#    logging.info("connected to amarok")

