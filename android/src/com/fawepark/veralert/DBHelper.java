package com.fawepark.veralert;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper  extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "VeraAlerts.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "Notifications";
    public static final String COLUMN_MESSAGE = "Message";
    public static final String COLUMN_ALERTTYPE = "AlertType";
    public static final String COLUMN_TIMESTAMP = "TimeStamp";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        final String TABLE_CREATE = 
                "create table " + TABLE_NAME + " (" +
                COLUMN_MESSAGE + " TEXT not null," +
                COLUMN_ALERTTYPE + " INTEGER," +
                COLUMN_TIMESTAMP + " INTEGER" +
                ");";
        database.execSQL(TABLE_CREATE);
    }
        
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
