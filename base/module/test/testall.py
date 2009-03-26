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

import unittest

import remuco.log
#remuco.log.set_level(remuco.log.DEBUG)
#remuco.log.set_level(remuco.log.INFO)
remuco.log.set_level(remuco.log.WARNING)

from testserial import SerializationTest
from testnet import ServerTest
from testfiles import FilesTest
from testadapter import AdapterTest

if __name__ == "__main__":
    
    unittest.main()