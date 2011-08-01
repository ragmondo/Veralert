'''
Created on 14 Jun 2010

@author: richard
'''

import os
import cgi

from google.appengine.ext.webapp import template

from google.appengine.ext import db
from google.appengine.api import mail
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

from google.appengine.api import memcache

import constants
import logging
import urllib

from google.appengine.api import urlfetch

    
#@PydevCodeAnalysisIgnore

def get_auth_key():
    auth = None
    mk = __name__+".authkey"
    logging.debug("Looking for memcached auth key under %s " % mk )
    try:
        auth = memcache.get(mk)
    except:
        pass
    if auth:
        logging.debug("Found auth key %s... " % auth[:20] )
        return auth
    else:        
        form_fields = {
            "accountType": "HOSTED",
            "Email": constants.c2dm_email,
            "Passwd": constants.c2dm_pwd,
            "service":constants.c2dm_service,
            "source": constants.c2dm_source,
        }
        form_data = urllib.urlencode(form_fields)
        result = urlfetch.fetch(url=constants.client_auth_url,
                                payload=form_data,
                                method=urlfetch.POST,
                                headers={'Content-Type': 'application/x-www-form-urlencoded'})
        
        d = dict()
        fields=result.content.split("\n")
        for r in fields:
            if "=" in r:
                (k,v)=r.split("=")
                d[k]=v
                logging.debug(result)           
                
        logging.debug("Not in memcache :-(" )
        auth = d["Auth"]
        logging.debug("Now got key %s " % auth)
        memcache.set(mk,auth)
        return auth
                
    
def sendToPlayer(gci,msg_dict):
    if not gci:
        logging.debug("No google cloud id - returning immediately")
        return False
        
    auth = get_auth_key()
    
                
    form_fields = {
        "registration_id":gci,
        "collapse_key":"x",
        }

    for k in msg_dict:
        form_fields["data."+k] = msg_dict[k]

    form_data = urllib.urlencode(form_fields)
    
    headers={"Authorization":"GoogleLogin auth="+auth}
    #'Content-Type': 'application/x-www-form-urlencoded'}

    logging.debug("Sending message via cloud...")
    
    result = urlfetch.fetch(url=constants.c2dm_url,
                        payload=form_data,
                        method=urlfetch.POST,
                        headers=headers)

    logging.debug("Message sending details : status_code: %s content: %s" % (result.status_code, result.content))
    if result.status_code == 200:
        lines=result.content.split()
        ret_dict = dict()
        for l in lines:
            if "=" in l:
                (k,v) = l.split("=")
                ret_dict[k]=v
                
            if ret_dict.has_key("Error"):
                logging.fatal("c2dm failure %s / %s" % ( result.status_code , result.content))
                me = "richard@barcodebeasties.com"
                subject = "Urgent: C2DM Failure"
                body = """
                Error : %s while trying %s 
                """ % ( result.content, form_data)
                
                mail.send_mail(me,me, subject, body)                
                
        return result
    
    logging.fatal("c2dm failure %s " % ( result.status_code , result.content))
    me = "richard@barcodebeasties.com"
    subject = "Urgent: C2DM Failure"
    body = """
    Error : %s while trying %s 
    """ % ( result.content, form_data)    
    mail.send_mail(me,me, subject, body)                
                  
    
    return False
    
if __name__ == '__main__':
    main()
  
  
  
      
