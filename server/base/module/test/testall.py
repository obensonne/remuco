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