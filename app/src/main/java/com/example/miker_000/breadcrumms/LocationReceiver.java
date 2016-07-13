package com.example.miker_000.breadcrumms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

//Todo: Make Observer so Activity can be notified of update
public class LocationReceiver extends BroadcastReceiver {
    static public final int LOCATION_UPDATE_CODE = 1337;

    public LocationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if(LocationResult.hasResult(intent)){
            LocationResult result = LocationResult.extractResult(intent);
            Location loc = result.getLastLocation();
            if(loc!=null){
                Log.d("Derp", "Lat: " + loc.getLatitude());
                Log.d("Derp", "Long: " + loc.getLongitude());
            }

        }



//        Location newLoc = null;
//        Log.d("Results", "Location: " + newLoc.toString());
    }
}
