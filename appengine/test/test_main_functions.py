'''
Created on 17 Jul 2010

@author: richard
'''
import unittest

from main import register, unregister, user


class Test(unittest.TestCase):
    
    def setUp(self):
        pass        
    
    def testRegister(self):
        reg_id = register(client_id, cloud_id)
        assert(reg_id)
        
    def testUnregister(self):
        unregistered = unregister(client_id)
        assert(unregistered)
        
    def testSendAlert(self, client_id):
        cloud_id = user.get_via_client_id(client_id)
        assert(sendToPlayer(cloud_id, {"ALERT":"This is an alert"}))
        

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()