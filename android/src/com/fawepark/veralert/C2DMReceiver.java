package com.fawepark.veralert;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;
import com.google.android.c2dm.Config;

/**
 * Broadcast receiver that handles Android Cloud to Data Messaging (AC2DM) messages, initiated
 */
public class C2DMReceiver extends C2DMBaseReceiver {
    static final String TAG = "Veralert C2DM";
    private DBSource dbSource = null;
    
    public C2DMReceiver() {
        super(Config.C2DM_SENDER);
        Log.i(TAG,"A c2dm recv has been constructed..");
    }
    
    @Override
    public void onDestroy() {
        if (dbSource != null) {
            dbSource.close();
        }
        super.onDestroy();
    }

    static public void FireAndForgetUrl(String url_string, Map<String,String> dict) {
                
        HttpURLConnection httpConnection = null;
        
        try {
            
            String post_string = url_string+"?";
            
            // + thing_to_send;
            boolean first = true;
            for (String k  : dict.keySet()) {
                if (!first) {
                    post_string = post_string +"&";
                } else {
                    first = false;
                }
                post_string = post_string + k +"=" + URLEncoder.encode(dict.get(k));
            }
            
            //"&val=" +URLEncoder.encode(scanned_content, "UTF-8") ;
            
            Log.d(TAG,post_string);
            
            URL post_url = new URL(post_string);
            
            httpConnection = (HttpURLConnection) post_url.openConnection();
            
            int response_code = httpConnection.getResponseCode();
                        
            if (response_code == HttpURLConnection.HTTP_OK) {
                { Log.v(TAG, "Logging scan " + post_string + " succeeded."); } 
            } else {
                { Log.v(TAG, "Logging scan " + post_string + " failed. Response: " + response_code); }
            }
            
        } catch (MalformedURLException ex) {                    
            { Log.e(TAG, "FireAndForget " + ex.toString()); }
        } catch (IOException ex) {
            { Log.e(TAG, "FireAndForget " + ex.toString()); }
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }
    
    @Override
    public void onError(Context context, String errorId) {
        Toast.makeText(context, "Messaging registration error: " + errorId,
                       Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onRegistrered(Context context, String registrationId) throws IOException {
        Log.i(TAG,"GOT A REGISTRATION FROM THE CLOUD : " + registrationId);
        Map<String,String> dict = new HashMap<String,String>();
        dict.put("id_a", Preferences.getIDString(this));
        dict.put("google_cloud_id",registrationId);
        FireAndForgetUrl(context.getString(R.string.alert_server) + "player/c2dm/register", dict);
    }

    void AlertIndicator(Context context, String msg){
        Log.d(TAG,"message text is " + msg);
         
        final String match_string = "AlertTone";
        String human_msg = msg;
        String atn = "1";
        try {
            if ( msg.indexOf(match_string) > -1) {
                int i = msg.indexOf(match_string);
                atn = msg.substring(i+match_string.length(), i+match_string.length()+1);
                human_msg = msg.replace(match_string + atn, "");
            }
        }
        catch (StringIndexOutOfBoundsException sioobe) {
            Log.e(TAG,sioobe.toString());
            // Keep calm and carry on...
        }

        // Get the static global NotificationManager object.
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
                
        int icon = R.drawable.icon;
        CharSequence tickerText = human_msg;
        long when = System.currentTimeMillis();
        Log.i(TAG,"TimeStamp : "+ when);
         
        Intent notificationIntent = new Intent(this, Notifications.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification n = new Notification(icon, tickerText, when);

        String rt = Preferences.getAlertTone(this, atn);
        if ( rt != "" ) {
            n.sound = Uri.parse(rt);
        }
        n.defaults |= Notification.DEFAULT_VIBRATE;

        n.ledARGB = 0xffff0000;
        n.ledOnMS = 300;
        n.ledOffMS = 300;
        n.flags |= Notification.FLAG_SHOW_LIGHTS;
         
        n.setLatestEventInfo(context, "Vera Alert", human_msg, contentIntent);
         
        // We user a string ID because it's a unique number
        // We also use it to cancel the notification in the notification class
        mNotificationManager.notify(R.string.alert_server, n);

        DBTuple t = new DBTuple();
        t.Message = human_msg;
        t.AlertType = atn.charAt(0) - '0';
        t.TimeStamp = when;
        if (dbSource == null) {
            dbSource = new DBSource(this);
            dbSource.open(true);
        }
        dbSource.Add(t);
    }
   
    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG,"GOT A MESSAGE FROM THE CLOUD");
        String type = intent.getExtras().getString("type");
        String msg = intent.getExtras().getString("msg");
        
        Log.d(TAG,"message type is " + type);
        
        if (type.equals( "alert")) {
            AlertIndicator(context, msg );
            return;
        }   
        
        String accountName = intent.getExtras().getString(Config.C2DM_ACCOUNT_EXTRA);
        String message = intent.getExtras().getString(Config.C2DM_MESSAGE_EXTRA);
        if (Config.C2DM_MESSAGE_SYNC.equals(message)) {
            if (accountName != null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Messaging request received for account " + accountName);
                }
            }
        }
    }

    /**
     * Register or unregister based on phone sync settings.
     * Called on each performSync by the SyncAdapter.
     */
    public static void refreshAppC2DMRegistrationState(Context context) {
        Log.i(TAG,"GOT A REFRESH");
        // Determine if there are any auto-syncable accounts. If there are, make sure we are
        // registered with the C2DM servers. If not, unregister the application.
        boolean autoSyncDesired = false;
        if (ContentResolver.getMasterSyncAutomatically()) {
            //           AccountManager am = AccountManager.get(context);
            //            Account[] accounts = am.getAccountsByType(SyncAdapter.GOOGLE_ACCOUNT_TYPE);
            //           for (Account account : accounts) {
            //               if (ContentResolver.getIsSyncable(account, JumpNoteContract.AUTHORITY) > 0 &&
            //                      ContentResolver.getSyncAutomatically(account, JumpNoteContract.AUTHORITY)) {
            //                  autoSyncDesired = true;
            //                 break;
            //            }
            //     }
        }

        boolean autoSyncEnabled = !C2DMessaging.getRegistrationId(context).equals("");

        if (autoSyncEnabled != autoSyncDesired) {
            Log.i(TAG, "System-wide desirability for JumpNote auto sync has changed; " +
                  (autoSyncDesired ? "registering" : "unregistering") +
                  " application with C2DM servers.");
            
            if (autoSyncDesired == true) {
                C2DMessaging.register(context, Config.C2DM_SENDER);
            } else {
                C2DMessaging.unregister(context);
            }
        }
    }
}
