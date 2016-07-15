package com.example.miker_000.breadcrumms;


import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.IntentSender;
import android.content.ServiceConnection;

import android.location.Location;

import android.os.IBinder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks{
    private Location mLastLocation = null;
    //indicates if we are currently making location updates
    private boolean active = false;
    private TextView latitudeData;
    private TextView longitudeData;
    public static final int REQUEST_FOR_LOCATION = 1;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private PendingIntent locationIntent;
    private LocationSettingsRequest.Builder locationSettingsRequest;

    //indicates if we are bound to the service StoreLocation
    boolean isBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d("Derp", "Connected To Service");
            //messengerForService = new Messenger(service);
            isBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("Derp", "Disconnected From Service :/");
            isBound = false;
        }
    };



    //Application Lifetime Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitudeData = (TextView)findViewById(R.id.locLatData);
        longitudeData = (TextView) findViewById(R.id.locLngData);

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
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);
        googleApiClient.connect();

        updateUI();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //Unbind from the service if the app is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeMyLocationUpdates();
        Log.d("Derp", "onDestroy() called");
    }
    

    //Add the location request to FusedLocationAPI
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
                            //Actually add our request for Location Update
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    googleApiClient,
                                    locationRequest,
                                    locationIntent
                            );
                            //Bind to service StoreLocation, which will update
                            //  the SQLLite DB everytime we get a location update
                            Intent tmp = new Intent()
                                    .setClass(MainActivity.this, StoreLocation.class);
                            bindService(tmp, mConnection, Context.BIND_AUTO_CREATE);
                            Log.d("Derp", "Location request added");

                        }
                        catch (SecurityException e){
                            //pass silently
                        }

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d("Derp", "Need to ask for permission");
                        //TODO: Bring up dialog
                        try{
                            status.startResolutionForResult(MainActivity.this, REQUEST_FOR_LOCATION);
                        }
                        catch (IntentSender.SendIntentException err){
                            //pass silently
                        }
                        break;
                }
            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_FOR_LOCATION){
            switch(resultCode){
                case RESULT_OK:
                    Log.d("Derp", "ASDFASFA");
                    addMyLocationUpdates();
                    break;
                case RESULT_CANCELED:
                    //Notify user that location is necessary
                    Toast.makeText(
                            getApplicationContext(),
                            "Location must be enabled for BreadCrumms to work",
                            Toast.LENGTH_LONG).show();
                    ToggleButton button = (ToggleButton)
                            findViewById(R.id.myButton);
                    button.setChecked(false);
                    TextView locText = (TextView)
                            findViewById(R.id.message);
                    locText.setText("NOT Looking for Location");
                    active = false;


                    break;
                default:
                    break;
            }
        }
    }

    //Removes the location updates requested by this service
    //Also unbinds from service
    private void removeMyLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient,
                locationIntent
        );
        unbindService(mConnection);
        Log.d("Derp", "Location Request Removed");
    }

    private void updateUI(){
        if(mLastLocation!=null){
            latitudeData.setText(String.valueOf(mLastLocation.getLatitude()));
            longitudeData.setText(String.valueOf(mLastLocation.getLongitude()));
        }
        else{
            latitudeData.setText("Could not Find Latitude");
            longitudeData.setText("Could not Find Longititude");
        }
    }


    public void toggleFindLocation(View view){
        ToggleButton button = (ToggleButton) findViewById(R.id.myButton);
        TextView msg = (TextView) findViewById(R.id.message);

        if(active){
            active = false;
            msg.setText("NOT Looking for Location!");
            removeMyLocationUpdates();

        }
        else{
            active = true;
            msg.setText("Looking for Location");
            addMyLocationUpdates();

        }
    }

}