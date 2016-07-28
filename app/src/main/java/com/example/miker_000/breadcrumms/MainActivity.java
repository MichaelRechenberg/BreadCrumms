package com.example.miker_000.breadcrumms;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;

import android.os.AsyncTask;
import android.os.IBinder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks{

    //Handle on shared preferences
    SharedPreferences sharedPreferences;


    private Location mLastLocation = null;
    //indicates if we are currently making location updates
    private boolean active;
    private TextView latitudeData;
    private TextView longitudeData;
    public static final int REQUEST_FOR_LOCATION = 1;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private PendingIntent locationIntent;
    private LocationSettingsRequest.Builder locationSettingsRequest;
    private final long updateDelay = 1000*60;
    //Broadcast Receiver to update Lat/Lng on Activity everytime we get a new location update
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Derp", "Updating UI");
            if(LocationResult.hasResult(intent)){
                LocationResult result = LocationResult.extractResult(intent);
                Location loc = result.getLastLocation();
                updateUI(loc);

            }
        }
    };

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

    //SQLlite database variables
    private LocationDatabaseDbHelper dbHelper;
    private SQLiteDatabase db;



    //Application Lifetime Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState!=null){
            //Update the UI to indicate whether or not the location service is currently active
            active = savedInstanceState.getBoolean("active");

            if(active){
                TextView msg = (TextView) findViewById(R.id.message);
                msg.setText(R.string.not_looking_for_location);
                ToggleButton button = (ToggleButton) findViewById(R.id.myButton);
                button.setChecked(true);
            }
        }


        //Get handle on shared preferences
        sharedPreferences = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );

        //Init UI handles
        latitudeData = (TextView)findViewById(R.id.locLatData);
        longitudeData = (TextView) findViewById(R.id.locLngData);

        Toolbar toolbar = (Toolbar) findViewById(R.id.theToolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        toolbar.setTitle("BreadCrumms");
        setSupportActionBar(toolbar);

        //Setup for location updates
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(updateDelay)
                .setInterval(updateDelay);
        //have pendingIntent send broadcast that will be picked up
        //  by any registered BroadcastReceivers in the current package
        //  that have the action StoreLocation.LOCATION_UPDATE set
        Intent tempIntent = new Intent()
                .setPackage(getApplicationContext().getPackageName())
                .setAction(StoreLocation.LOCATION_UPDATE);
        //TODO: Change Flag to Cancel Current?
        locationIntent = PendingIntent.getBroadcast(getApplicationContext(),
                StoreLocation.LOCATION_UPDATE_CODE, tempIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);
        googleApiClient.connect();

        //initialize SQLlite variables
        dbHelper = new LocationDatabaseDbHelper(getApplicationContext());
        db = dbHelper.getReadableDatabase();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Tmp", "onResume() called");
        registerReceiver(locationReceiver, new IntentFilter(StoreLocation.LOCATION_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("active", active);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try{
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }
        catch(SecurityException e){
            //pass silently
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //Unbind from the service if the app is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeMyLocationUpdates();
        if(active){
            Intent tmp = new Intent()
                    .setClass(MainActivity.this, StoreLocation.class);
            stopService(tmp);
        }

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
                            Intent tmp = new Intent()
                                    .setClass(MainActivity.this, StoreLocation.class);
                            startService(tmp);
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Each location update will occur every " + updateDelay/1000 + " seconds",
                                    Toast.LENGTH_LONG
                            ).show();

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
                    locText.setText(R.string.not_looking_for_location);
                    active = false;


                    break;
                default:
                    break;
            }
        }
    }

    //Removes the location updates requested by this service
    private void removeMyLocationUpdates(){
        PendingResult<Status> removeLocationStatus = LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient,
                locationIntent
        );
        removeLocationStatus.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if(status.isSuccess()){
                    Log.d("Derp", "Successfully removed location request");
                }
                else{
                    Log.d("Derp", "COULD NOT REMOVE LOCATION REQUEST");
                }
            }
        });

        Log.d("Derp", "Location Request Removed");
    }

    private void updateUI(Location newLoc){
        mLastLocation = newLoc;
        if(mLastLocation!=null){
            latitudeData.setText(String.valueOf(mLastLocation.getLatitude()));
            longitudeData.setText(String.valueOf(mLastLocation.getLongitude()));
        }
        else{
            latitudeData.setText(R.string.no_latititude);
            longitudeData.setText(R.string.no_longitude);
        }
    }


    public void toggleFindLocation(View view){
        TextView msg = (TextView) findViewById(R.id.message);

        if(active){
            active = false;
            msg.setText(R.string.not_looking_for_location);
            removeMyLocationUpdates();
            Intent tmp = new Intent()
                    .setClass(MainActivity.this, StoreLocation.class);
            stopService(tmp);

        }
        else{
            active = true;
            msg.setText(R.string.looking_for_location);
            addMyLocationUpdates();
            registerReceiver(locationReceiver, new IntentFilter(StoreLocation.LOCATION_UPDATE));

        }
    }



    public void startMapLocationActivity(View view){

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MapLocationActivity.class);
        intent.putExtra("userLocation", mLastLocation);
        startActivity(intent);
    }

}