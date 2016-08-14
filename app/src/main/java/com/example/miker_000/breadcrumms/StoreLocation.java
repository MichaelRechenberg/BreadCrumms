package com.example.miker_000.breadcrumms;

import android.app.Notification;
import android.app.PendingIntent;
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

import com.google.android.gms.location.LocationResult;

/**
 * Service to store the location of the user in local database
 */
public class StoreLocation extends Service {

    public static final String LOCATION_UPDATE = "com.example.miker_000.breadcrumms.LOCATION_UPDATE";
    public static final int LOCATION_UPDATE_CODE = 1337;
    private static final int LOCATION_TRACKING_ONGOING_ID = 1;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(LocationResult.hasResult(intent)){
                LocationResult result = LocationResult.extractResult(intent);
                Location loc = result.getLastLocation();
                if(loc!=null){
                    //Insert into SQLite DB
                    ContentValues values = new ContentValues();
                    values.put(
                            LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE,
                            loc.getLatitude());
                    values.put(
                            LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE,
                            loc.getLongitude()
                    );
                    LocationDatabaseDbHelper dbHelper = new LocationDatabaseDbHelper(getApplicationContext());
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    long row_id = -1;
                    try {
                        //only write to the DB if not blocked by the current thread (in MapLocationActivity)
                        if(!db.isDbLockedByCurrentThread()){
                            row_id = db.insert(LocationDatabaseContract.LocationEntry.TABLE_NAME,
                                    null,
                                    values);
                        }

                    } finally{
                        db.close();
                    }


                }

            }
        }
    };

    public StoreLocation() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //throw new UnsupportedOperationException("Not yet implemented");
        IntentFilter filter = new IntentFilter(StoreLocation.LOCATION_UPDATE);
        registerReceiver(locationReceiver, filter);


        Intent tmpIntent = new Intent()
                .setClass(getApplicationContext(), MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent returnToMainActivityPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                MainActivity.REQUEST_CODE_FROM_NOTIFICATION,
                tmpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        //Start the service in the foreground and add notification
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.bc_notification_icon)
                .setContentText(getString(R.string.trackingNotificationMessage))
                .setContentIntent(returnToMainActivityPendingIntent)
                .build();

        startForeground(LOCATION_TRACKING_ONGOING_ID, notification);

        return 1;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(locationReceiver);
        stopForeground(true);
    }
}
