

from bbc2dm import get_auth_key


import unittest


class Test(unittest.TestCase):
    def test_get_auth_key(self):
        a = get_auth_key()
        assert (len(a) > 0 )
        
        
        
        