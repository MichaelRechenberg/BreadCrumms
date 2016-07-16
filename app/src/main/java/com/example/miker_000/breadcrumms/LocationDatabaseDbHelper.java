package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.UnsupportedSchemeException;

/**
 * Created by miker_000 on 7/15/2016.
 */
public class LocationDatabaseDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "LocationDatabase.db";

    private static final String SQL_CREATE_TABLE =
        "CREATE TABLE " + LocationDatabaseContract.LocationEntry.TABLE_NAME +
        "(" + LocationDatabaseContract.LocationEntry._ID +
        " INTEGER PRIMARY KEY, " + LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE +
        " REAL NOT NULL, " + LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE +
        " REAL NOT NULL, " + LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED +
        " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" + ")";

    private static final String SQL_DROP_TABLE =
        "DROP TABLE IF EXISTS " + LocationDatabaseContract.LocationEntry.TABLE_NAME;


    public LocationDatabaseDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }
}
