package com.example.miker_000.breadcrumms;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Service that creates Google API connection and sends location to LocationReceiver
 *C
 */
public class GetPeriodicLocation extends Service implements
        GoogleApiClient.ConnectionCallbacks{
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private PendingIntent locationIntent;
    private LocationSettingsRequest.Builder locationSettingsRequest;


    public GetPeriodicLocation() {

    }

    //Connect to Google API and create location request
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Derp", "onBind() called");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(5000)
                .setInterval(5000);
        //have pendingIntent send broadcast that will be picked up
        //  by LocationReceiver
        Intent tempIntent = new Intent(this, LocationReceiver.class);
        //TODO: Change Flag to Cancel Current?
        locationIntent = PendingIntent.getBroadcast(getApplicationContext(),
                LocationReceiver.LOCATION_UPDATE_CODE, tempIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        googleApiClient.connect();
        //Local Binder stuff to start/stop service via public methods???
        return new Binder();
    }

    //Disconnect from Google API and remove location updates
    @Override
    public boolean onUnbind(Intent intent) {
        boolean superUnbind = super.onUnbind(intent);

        removeMyLocationUpdates();
        if(googleApiClient != null){
            googleApiClient.disconnect();
        }
        return superUnbind;

    }


    //Overriden Methods for Google API client

    //add location update
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        addMyLocationUpdates();
    }

    //remove the location update
    @Override
    public void onConnectionSuspended(int i) {

        removeMyLocationUpdates();
    }


    //Adds my location update request to LocationServices
    private void addMyLocationUpdates(){
        //Check if we have the settings for making request,
        //TODO: Add dialog to request for settings later
        PendingResult<LocationSettingsResult> result =
            LocationServices.SettingsApi.checkLocationSettings(
                googleApiClient,
                locationSettingsRequest.build()
            );

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch(status.getStatusCode()){
                    //All location settings were satisfied
                    case LocationSettingsStatusCodes.SUCCESS:
                        try{
                            //Do security exception thing here
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    googleApiClient,
                                    locationRequest,
                                    locationIntent
                            );
                            Log.d("Derp", "Location request added");
                        }
                        catch (SecurityException e){
                            //pass silently
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d("Derp", "Need to ask for permission");
                        //TODO: Bring up dialog
                        //status.startResolutionForResult(); ACTIVITY STUFF
                        break;
                }
            }
        });



    }

    //Removes the location updates requested by this service
    private void removeMyLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient,
                locationIntent
        );
        Log.d("Derp", "Location Request Removed");
    }
}
