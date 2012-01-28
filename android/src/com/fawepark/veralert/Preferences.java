package com.fawepark.veralert;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.UUID;
 
public class Preferences extends PreferenceActivity {
    private static final String notifications = "Notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        // Get the custom preference

        String ID =  getIDString(this);
        if (ID.equals("")) {
            ID = scrambleIDS();
            Toast t = Toast.makeText(this, "ID used is : " + ID, Toast.LENGTH_SHORT);
            t.show();  
        }
        
        EditTextPreference RegID = (EditTextPreference) this.findPreference("RegistrationID");
        RegID.setTitle(ID);
        PendingIntent app = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
        Log.i(Notifications.TAG,"Attempting c2dm registration of "+ app);
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.putExtra("app", app); // boilerplate
        registrationIntent.putExtra("sender", this.getString(R.string.c2dm_name));
        startService(registrationIntent);
        Log.i(Notifications.TAG,"c2dm registration intent started");             
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      menu.add(0, 1, 0, notifications);
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String i = item.toString();
        Log.v(Notifications.TAG,"In menu select "+ i);
        if (i.equals(notifications)) {
            Intent intent = new Intent(this, Notifications.class);
            startActivity(intent);
        }
        return true;
    }    

    private String scrambleIDS() {
        UUID idOne = UUID.randomUUID();
        Log.i(Notifications.TAG,"Created UUID of " + idOne);                  
        Long l1 = idOne.getLeastSignificantBits();
        if ( l1 < 0) { l1 = l1 + Long.MAX_VALUE; }
        String token1 = Long.toString(l1, 36);  
        Editor ed = getEditor(this);
        ed.putString("ID", token1);
        ed.commit();
        return(token1);
    }

    private static Editor getEditor(Context ctx) {
        return getDefault(ctx).edit();
    }

    public static String getIDString(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        return sp.getString("ID", "");
    }
    
    public static String getAlertTone(Context ctx, String val) {
        String pref_string = "ringtonePref" + val;
        SharedPreferences sp = getDefault(ctx);
        String res_string = sp.getString(pref_string, "");
        Log.i(Notifications.TAG,"using ringtone: " + res_string + " for AlertTone" + val); 
        return(res_string);
    }

    public static int getMaxRetention(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        String tmp = sp.getString("MaxRetention", "0");
        return Integer.parseInt(tmp);
    }


    private static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
