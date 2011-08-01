import cgi
import datetime
import logging

import os

from google.appengine.ext.webapp import template
from google.appengine.api import memcache as memcache
from google.appengine.api import datastore_errors

from google.appengine.ext import db
from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.api import images
from google.appengine.api import mail
from google.appengine.ext import deferred

from google.appengine.api import xmpp

from google.appengine.api import datastore_errors

from xml.etree import ElementTree as ET

import bbc2dm
import constants

class Player(db.Model):
    """The main user class"""
    #: the primary search key for the player
    id_a = db.StringProperty()
       
    #: the currently registered cloud id for c2d messaging (if applicable)
    google_cloud_id = db.StringProperty()
   
    email_id = db.EmailProperty()
   
    google_acct = db.UserProperty()
   
    #: created datetime
    created = db.DateTimeProperty(auto_now_add = True)
    #: the last time the player was "seen"
    last_seen = db.DateTimeProperty(auto_now = True)
    
    def pprint(self):
        """Pretty print function to display all attributes"""
        repr_x = self.__repr__()
        print repr_x

        for a in self.__dict__:
            print "%s : %s " % ( a, self.__dict__[a] )
            
    def __repr__(self):
        idx="unsaved"
        if self.is_saved():
            idx = self.key().id()
        return "Player <%s> (%s) { %s } " % (idx,self.id_a, self.google_acct )

    @classmethod
    def createFromId(cls,id_a,id_b):
        """Creates a player given the id variables"""
        p = cls()
        p.id_a = id_a
        p.put()
        
        return p

    @classmethod
    def createFromEmail(cls,email):
        p = cls()
        p.email_id = email
        p.put()
        
        return p


def getOrCreatePlayer(id_a,id_b=""):
    if "@" not in id_a:
        p = Player.gql("where id_a = :1",id_a).get()
        if p == None:
            p = Player.createFromId(id_a,id_b)
        return p
    else:
        p = Player.gql("where email_id = :1",id_a).get()
        if p == None:
            p = Player.createFromEmail(id_a)
        return p

        


class Message(db.Model):
    user =  db.ReferenceProperty()
    message_text = db.StringProperty()
    message_rcvd = db.DateTimeProperty(auto_now_add = True)
    send_status = db.StringProperty(multiline=True)
    priority = db.IntegerProperty()

    def send(self):
        if self.user.google_cloud_id:
            result = bbc2dm.sendToPlayer(self.user.google_cloud_id,{"type":"alert","msg":self.message_text,"priority":self.priority})
            self.send_status = "%s / %s" % ( str(result.status_code) , str(result.content) )
            self.put()
        else:
            self.send_status = str(send_message_via_gtalk(self.user.email_id, self.message_text))
            self.put()
        
    def __repr__(self):
        idx="unsaved"
        if self.is_saved():
            idx = self.key().id()
        return "Message <%s> to:%s [ Status:%s ] { %s } " % (idx,self.user, self.send_status, self.message_text  )

        
    
        
class RegisterC2DMPage(webapp.RequestHandler):
    def get(self):
        id_a = self.request.get("id_a")
        google_cloud_id = self.request.get("google_cloud_id")        
        p = getOrCreatePlayer(id_a)
        p.google_cloud_id = google_cloud_id
        p.put()
        
        print "registered!"
        #
        return # don't bother returning anything - its ignored

class UnregisterC2DMPage(webapp.RequestHandler):
    def get(self):
        id_a = self.request.get("id_a")
        google_cloud_id = None
        p = getOrCreatePlayer(id_a)
        p.google_cloud_id = google_cloud_id
        p.put()
    
        print "unregistered!"
        
        return # don't bother returning anything - its ignored

class IndexPage(webapp.RequestHandler):
    def get(self):
        f = "templates/index.html"
        path = os.path.join(os.path.dirname(__file__), f) 
        self.response.out.write(template.render(path,{}))        

class SendMessagePage(webapp.RequestHandler):
    def get(self):
        msg = self.request.get("msg")        
        if "recipient" in self.request.arguments():
            recipient = self.request.get("recipient")
            p = getOrCreatePlayer(recipient)
            m  = Message()
            m.user = p
            m.message_text = msg
            
            try:
                m.put()
            except Exception,e:
                logging.error(e)
                
            logging.info("Sending %s" % m )
            
            try:
                m.send()
            except Exception,e:
                logging.error(e)

            logging.info("Sent : %s" % m)
            print "msg sent"
        else:
            logging.info("No recipient / debug message: %s" % msg)
        return # don't bother returning anything - its ignored
    
def send_message_via_gtalk(user_address, msg):    
    logging.debug("Attempting to send Player: %s msg %s " % (user_address,msg))
    """Sends message via best way"""
    status_code = "not sent"
    chat_message_sent = False
    try:
        if xmpp.get_presence(user_address, constants.gtalk_jid):
            status_code = xmpp.send_message(user_address, msg, constants.gtalk_jid)
            if status_code == xmpp.NO_ERROR:
                chat_message_sent = True
                logging.info("Message %s sent ok to %s" % (msg, user_address))            
        else:
            logging.info("Couldn't send msg to %s due to not present. (Resending invite) " % (user_address))
            send_invite(user_address)
        if not chat_message_sent:
            logging.info("Couldn't send msg to %s due to error code %s " % (user_address, status_code))
        
    except Exception, e:
        logging.error("Grrr : %s for %s %s" % (e,user_address,msg))

    return status_code


def send_invite(user_address):
    logging.info("Trying to invite %s" % (user_address))
    xmpp.send_invite(user_address, constants.gtalk_jid)
    logging.info("Invite sent..")



class ListMessagePage(webapp.RequestHandler):
    def get(self):
        recipient = self.request.get("recipient")
        p = getOrCreatePlayer(recipient)
        msgs = p.message_set.order("-message_rcvd")
        f = "templates/message.html"
        path = os.path.join(os.path.dirname(__file__), f) 
        self.response.out.write(template.render(path,{"messages":msgs}))        
        


application = webapp.WSGIApplication([
    ('/stat',SendMessagePage), 
    ('/list',ListMessagePage), 
    ('/player/c2dm/register',RegisterC2DMPage), 
    ('/player/c2dm/unregister',UnregisterC2DMPage), 
    ('/',IndexPage), 

    
], debug=True)

def main():
    logging.getLogger().setLevel(logging.DEBUG)
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
    