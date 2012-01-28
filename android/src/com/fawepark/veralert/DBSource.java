package com.fawepark.veralert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBSource {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private Context context;
    private String[] allColumns = {DBHelper.COLUMN_MESSAGE, DBHelper.COLUMN_ALERTTYPE, DBHelper.COLUMN_TIMESTAMP};

    public DBSource(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }
    
    public void open(boolean ForEdit) throws SQLException {
        if (ForEdit) {
            database = dbHelper.getWritableDatabase();
        } else {
            database = dbHelper.getReadableDatabase();
        }
    }

    public void close() {
        dbHelper.close();
    }

    public void Add(DBTuple t) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COLUMN_MESSAGE, t.Message);
        v.put(DBHelper.COLUMN_ALERTTYPE, t.AlertType);
        v.put(DBHelper.COLUMN_TIMESTAMP, t.TimeStamp);
        database.insert(DBHelper.TABLE_NAME, null, v);
    }

    public List<DBTuple> getAllData() {
        List<DBTuple> Results = new ArrayList<DBTuple>();
        Cursor cur = database.query(DBHelper.TABLE_NAME, allColumns, null, null, null, null, null);
        AddCursorData(Results, cur);
        return(Results);
    }

    private void AddCursorData(List<DBTuple> Results, Cursor cur) {
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            DBTuple tuple = cursorToTuple(cur);
            Results.add(tuple);
            cur.moveToNext();
        }
        // Make sure to close the cursor
        cur.close();
        LimitData(Results);
    }

    private void LimitData(List<DBTuple> Results) {
        int Max = Preferences.getMaxRetention(context);
        if (Max == 0) return;
        
        // Sort A copy of the data by Message
        List<DBTuple> TmpCpy = new ArrayList<DBTuple>(Results);
        Collections.sort(TmpCpy, new Comparator<DBTuple>() {
            @Override
            public int compare(DBTuple t1, DBTuple t2) {
                int c = t1.Message.compareTo(t2.Message);
                if (c == 0) {
                    if (t1.TimeStamp == t2.TimeStamp) return 0;
                    if (t1.TimeStamp > t2.TimeStamp) { 
                        return -1;
                    } else {
                        return 1;
                    }
                }
                return c;
            }
        });
        // Remove if we see more than Max of any message type
        String LastMsg = "xyzzy";
        int Cnt = 1;
        for (int i = 0; i < TmpCpy.size(); i++) {
            DBTuple t = TmpCpy.get(i);
            if (LastMsg.equals(t.Message)) {
                Cnt++;
                if (Cnt > Max) {
                    Remove(t);
                    Results.remove(t);
                }
            } else {
                Cnt = 1;
                LastMsg = t.Message;
            }
        }
    }

    private DBTuple cursorToTuple(Cursor cur) {
        DBTuple Result = new DBTuple();
        Result.Message = cur.getString(0);
        Result.AlertType = cur.getInt(1);
        Result.TimeStamp = cur.getLong(2);
        return(Result);
    }
    
    public void Remove(DBTuple t) {
        database.delete(DBHelper.TABLE_NAME,
                        DBHelper.COLUMN_MESSAGE + "=? and " + DBHelper.COLUMN_TIMESTAMP + "=?",
                        new String[] {t.Message, Long.toString(t.TimeStamp)});
    }
    
    public void GetNewData(List<DBTuple> values) {
        long max = 0;
        for (int i = 0; i < values.size(); i++) {
            DBTuple t = values.get(i);
            if (t.TimeStamp > max) max = t.TimeStamp;
        }
        Cursor cur = database.query(
                                    DBHelper.TABLE_NAME, allColumns, 
                                    DBHelper.COLUMN_TIMESTAMP + ">?", 
                                    new String[] {Long.toString(max)}, 
                                    null, null, null);
        AddCursorData(values, cur);     
    }
}
