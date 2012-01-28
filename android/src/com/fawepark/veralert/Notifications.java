package com.fawepark.veralert;

import java.util.List;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

public class Notifications extends ListActivity {
    public static final String TAG = "Veralert";
    private static final String Preferences = "Settings";
    private static final String DeleteNotifications = "Delete";
    private View EditOptions;
    private DBViewAdapter dbAdapter; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications);

        SharedPreferences sp = getDefault(this);
        if (! sp.contains("ID") )
        {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
            finish();
        }
        CancelNotification();

        // Use the SimpleCursorAdapter to show the
        // elements in a ListView
        dbAdapter = DBViewAdapter.GetAdapter(this);
        dbAdapter.SetEdit(false);
        setListAdapter(dbAdapter);
        EditOptions = findViewById(R.id.deleteoptions);
        registerForContextMenu(getListView());
    }
    
    
    @Override    
    public void onResume() 
    {
        super.onResume();
        CancelNotification();
        dbAdapter.Refresh();
        dbAdapter.notifyDataSetChanged();
    }   
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      menu.add(0, 1, 0, Preferences);
      menu.add(0, 1, 0, DeleteNotifications);
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String i = item.toString();
        Log.v(TAG,"In menu select "+ i);
        if (i.equals(Preferences)) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }
        if (i.equals(DeleteNotifications)) {
            dbAdapter.SetEdit(true);
            dbAdapter.notifyDataSetChanged();
            EditOptions.setVisibility(View.VISIBLE);
        }
        return true;
    }
    
    public void DeleteTuples(View view) {
        dbAdapter.DeleteSelected();
        CancelDeleteTuples(view);
    }
    
    public void CancelDeleteTuples(View view) {    
        dbAdapter.SetEdit(false);
        EditOptions.setVisibility(View.GONE);
        List<DBTuple> vals = dbAdapter.GetValues();
        for (int i = 0; i < vals.size(); i++) {
            DBTuple t = vals.get(i);
            t.Selected = false;
        }
        DeSelectAll(view);
    }
    
    public void SelectAll(View view) {
        List<DBTuple> vals = dbAdapter.GetValues();
        for (int i = 0; i < vals.size(); i++) {
            DBTuple t = vals.get(i);
            t.Selected = true;
        }
        dbAdapter.notifyDataSetChanged();
    }
    
    public void DeSelectAll(View view) {
        List<DBTuple> vals = dbAdapter.GetValues();
        for (int i = 0; i < vals.size(); i++) {
            DBTuple t = vals.get(i);
            t.Selected = false;
        }
        dbAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v == (View) getListView()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            DBTuple t = dbAdapter.Get(info.position);
            String ts = t.TimeString();
            menu.setHeaderTitle(t.Message + "  " + ts);
            menu.add(Menu.NONE, 100, 0, "Order by Message");
            menu.add(Menu.NONE, 101, 1, "Order by Date");
            if (dbAdapter.EditState) {
                menu.add(Menu.NONE, 102, 2, "Select all:" + t.Message);
                menu.add(Menu.NONE, 103, 3, "Select all before: " + ts);
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean Result = false;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        DBTuple t = dbAdapter.Get(info.position);
        List<DBTuple> vals = dbAdapter.GetValues();
        switch (item.getItemId()) {
          case 102:
            for (int i = 0; i < vals.size(); i++) {
                DBTuple t2 = vals.get(i);
                if (t.Message.equals(t2.Message)) {
                    t2.Selected = true;
                }
            }
            Result = true;
            break;
          case 103:
            for (int i = 0; i < vals.size(); i++) {
                DBTuple t2 = vals.get(i);
                if (t2.TimeStamp < t.TimeStamp) {
                    t2.Selected = true;
                }
            }
            Result = true;
            break;
          case 100:
            dbAdapter.SetSortOrder(DBViewAdapter.BY_MESSAGE);
            Result = true;
            break;
          case 101:
            dbAdapter.SetSortOrder(DBViewAdapter.BY_DATE);
            Result = true;
            break;
        }
        if (Result) {
            dbAdapter.notifyDataSetChanged();
        }
        return Result;
    }

    private void CancelNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        // Cancel the persistent notification.
        // We user a string ID because it's a unique number
        // This is the ID the notification was created with in the C2DMReceiver Class
        mNotificationManager.cancel(R.string.alert_server);
    }


    protected static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

}
