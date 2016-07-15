package com.example.miker_000.breadcrumms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

public class StoreLocation extends Service {

    public static final String LOCATION_UPDATE = "com.example.miker_000.breadcrumms.LOCATION_UPDATE";
    public static final int LOCATION_UPDATE_CODE = 1337;
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(LocationResult.hasResult(intent)){
                LocationResult result = LocationResult.extractResult(intent);
                Location loc = result.getLastLocation();
                if(loc!=null){
                    Log.d("Derp", "Lat: " + loc.getLatitude());
                    Log.d("Derp", "Long: " + loc.getLongitude());
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
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        IntentFilter filter = new IntentFilter(StoreLocation.LOCATION_UPDATE);
        registerReceiver(locationReceiver, filter);
        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Derp", "onUnbind() called");
        unregisterReceiver(locationReceiver);
        return super.onUnbind(intent);
    }


}
