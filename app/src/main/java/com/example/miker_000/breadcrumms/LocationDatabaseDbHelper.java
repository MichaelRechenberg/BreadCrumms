package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.UnsupportedSchemeException;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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


    /**
     * Convenience method that returns query string used to query database for all Lat/Lng
     *  Points within the bounds of the Heat Map
     *
     *  @param bounds The bounds of where to select points from
     *  @param latestDate The latest Date from which you want to begin searching.
     *                    Set this to null if you wish to not bound the date from below
     *  @param earliestDate The earliest Date from which you want to begin searching.
     *                    Set this to null if you want to set this to the current date
     *  @param limit String denoting LIMIT clause (do not include the literal "LIMIT")
     *               Set this to null to have no limit on the number or results returned
     *  @param orderBy String denoting ORDER BY clause (do not include the literal "ORDER BY")
     *               Setting this null leads to default behavior of SQLLiteDatabase.query()
     */
    public static String gatherLocationsQueryString(LatLngBounds bounds, Date latestDate, Date earliestDate, String limit, String orderBy){


        //Invalid arguments
        if((latestDate != null && earliestDate!=null && latestDate.after(earliestDate)) || bounds==null){
            return null;
        }

        LatLng southwest = bounds.southwest;
        LatLng northeast = bounds.northeast;


        String[] projection = {
                LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE,
                LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE,
                LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED
        };

        //restrict points returned to those within bounds
        String where= "((" + LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE + " BETWEEN " +
                southwest.latitude + " AND " + northeast.latitude + ")" +
                " AND (" + LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE +
                " BETWEEN " + southwest.longitude + " AND " + northeast.longitude+ "))";






        where += " AND ";
        //Add date restraints
        if(latestDate != null || earliestDate != null){

            //Both dates are specified
            if(latestDate != null && earliestDate!=null){

            }
            //Only the earliest date is specified
            else if (latestDate == null && earliestDate!=null){

            }
            //Only the latestDate is specified
            else{

            }
        }
        //No dates are specified, gather all any points before current time
        else{
            Calendar calendar = Calendar.getInstance();
            Date currentDate = calendar.getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            String currentDateAsString = df.format(currentDate);
            where += "DATETIME(" + LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED +
                    ") < " + "DATETIME('" + currentDateAsString + "')";
        }


        String query =  SQLiteQueryBuilder.buildQueryString(false, LocationDatabaseContract.LocationEntry.TABLE_NAME,
                projection, where, null, null, orderBy, limit);

        Log.d("Tmp", query);
        return query;


    }
}
