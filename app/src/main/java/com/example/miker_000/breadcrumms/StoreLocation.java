package com.example.miker_000.breadcrumms;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class StoreLocation extends Service {

    public static final String LOCATION_UPDATE = "com.example.miker_000.breadcrumms.LOCATION_UPDATE";
    public static final int LOCATION_UPDATE_CODE = 1337;
    private static final int LOCATION_TRACKING_ONGOING_ID = 1;

    private SQLiteDatabase db = null;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(LocationResult.hasResult(intent)){
                LocationResult result = LocationResult.extractResult(intent);
                Location loc = result.getLastLocation();
                if(loc!=null){
                    Log.d("Derp", "Lat: " + loc.getLatitude());
                    Log.d("Derp", "Long: " + loc.getLongitude());

                    //Insert into SQLite DB
                    Log.d("SQL", "Beginning insertion");
                    ContentValues values = new ContentValues();
                    values.put(
                            LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE,
                            loc.getLatitude());
                    values.put(
                            LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE,
                            loc.getLongitude()
                    );
                    //Todo: Async task
                    long row_id = db.insert(LocationDatabaseContract.LocationEntry.TABLE_NAME,
                            null,
                            values);
                    Log.d("SQL", "Insertion Successful of row " + row_id);

                }

            }
        }
    };

    public StoreLocation() {
    }

    //TODO: Create and release resources on onBind, onUnbind,
    //  and when user swipes off of recent apps (onTaskRemove())
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Derp", "onBind() called");
        //throw new UnsupportedOperationException("Not yet implemented");
        IntentFilter filter = new IntentFilter(StoreLocation.LOCATION_UPDATE);
        registerReceiver(locationReceiver, filter);
        LocationDatabaseDbHelper dbHelper = new LocationDatabaseDbHelper(getApplicationContext());
        //TODO: Place getWritableDatabase in AsyncTask to not block UI thread
        db = dbHelper.getWritableDatabase();

        //Start the service in the foreground and add notification
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("BreadCrumms")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentText("Location Tracking Is On")
                .build();

        startForeground(LOCATION_TRACKING_ONGOING_ID, notification);

        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Derp", "onUnbind() called");
        unregisterReceiver(locationReceiver);
        db = null;
        stopForeground(true);
        return super.onUnbind(intent);
    }


}
