package com.fawepark.veralert;


import java.util.UUID;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity {
    private static final String TAG = "Veralert";
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        
        SharedPreferences sp = getDefault(this);
        if (! sp.contains("ID") )
        {
        	scrambleIDS();
    		      	
        }        
        Toast t = Toast.makeText(this, "ID used is : " + sp.getString("ID", ""), Toast.LENGTH_SHORT);
		t.show();  
		
		
		PendingIntent app = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		Log.i(TAG,"Attempting c2dm registration of "+ app);
		Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
		registrationIntent.putExtra("app", app); // boilerplate
		registrationIntent.putExtra("sender", this.getString(R.string.c2dm_name));
		startService(registrationIntent);
		Log.i(TAG,"c2dm registration intent started");		
		Toast t2 = Toast.makeText(this, "registered", Toast.LENGTH_SHORT);
		t2.show();
				
		TextView tv = new TextView(this);
		tv.setTextAppearance(this, android.R.style.TextAppearance_Large);
	       tv.setText("ALERT ID:\n " + sp.getString("ID", ""));
	       setContentView(tv);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
  
  	  menu.add(0, 1, 0, "Preferences");
	return true;
  	        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {	
    Log.v(TAG,"In menu select "+ item.toString());
    Intent settingsActivity = new Intent(getBaseContext(),Preferences.class);
    startActivity(settingsActivity);
    
    grr();
    
	return true;
    }
    
    public void grr()
    { 
    	// Get the app's shared preferences
        SharedPreferences app_preferences = 
        	PreferenceManager.getDefaultSharedPreferences(this);
      Log.i(TAG,app_preferences.getString("ringtonePref1", "<null>"));
    }
    
    @Override    
    public void onResume() 
    {
    	super.onResume();
    }	
 
    protected void scrambleIDS()
    {
    	UUID idOne = UUID.randomUUID();
		Log.i(TAG,"Created UUID of " + idOne);    		
		Long l1 = idOne.getLeastSignificantBits();
		if ( l1 < 0) { l1 = l1 + Long.MAX_VALUE; }
		String token1 = Long.toString(l1, 36);	
		Editor ed = getEditor(this);
		ed.putString("ID", token1);
		ed.commit();		
    }
    
    
    public static String getIDStringA(Context ctx) {
		SharedPreferences sp = getDefault(ctx);
		return sp.getString("ID", null);
	}

    protected static SharedPreferences getDefault(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx);
	}
    
    protected static Editor getEditor(Context ctx) {
		return getDefault(ctx).edit();
	}

    
}