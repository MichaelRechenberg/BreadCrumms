package com.example.miker_000.breadcrumms;


import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;

import android.os.AsyncTask;
import android.os.IBinder;

import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Map;
import java.util.TimeZone;

/**
 * Launcher activity that provides way to start/stop location tracking
 *  and launch MapLocationActivity to view heatmap
 */
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
    private long updateDelay;

    private boolean hasAcceptedEULA;
    //Broadcast Receiver to update Lat/Lng on Activity everytime we get a new location update
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(LocationResult.hasResult(intent)){
                LocationResult result = LocationResult.extractResult(intent);
                Location loc = result.getLastLocation();
                updateUI(loc);

            }
        }
    };





    //Application Lifetime Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Get handle on shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        //Show EULA the first time the user installs app and on version updates
        //Code adapted from http://www.donnfelker.com/android-a-simple-eula-for-your-android-apps/
        int versionNumber=1;
        try{
            versionNumber = getPackageManager()
                    .getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES)
                    .versionCode;
        }
        catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }

        final String EULA_KEY = "EULA_" + versionNumber;
        hasAcceptedEULA = sharedPreferences.getBoolean(EULA_KEY, false);
        if(!hasAcceptedEULA){
            final Activity thisActivity = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("EULA Notice")
                    .setMessage(Html.fromHtml(getString(R.string.EULA_text)))
                    .setCancelable(false)
                    .setPositiveButton(R.string.EULA_PositiveButton, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(EULA_KEY, true);
                            editor.commit();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the activity as they have declined the EULA
                            thisActivity.finish();
                        }

                    });
            builder.create().show();
        }


        active = sharedPreferences.getBoolean("active", false);
        //The location Service is active, so have the button be "On"
        if(active){
            ToggleButton button = (ToggleButton) findViewById(R.id.myButton);
            button.setChecked(true);
        }


        //Init UI handles
        latitudeData = (TextView)findViewById(R.id.locLatData);
        longitudeData = (TextView) findViewById(R.id.locLngData);


        Toolbar toolbar = (Toolbar) findViewById(R.id.theToolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        toolbar.setTitle("Bread Crumms");
        setSupportActionBar(toolbar);

        //Setup for location updates
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        //initialize the appropriate member variables for the LocationRequest with the
        //  values set in the settings
        //look at documentation for initLocationInformation() for more information
        initLocationInformation();


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(locationReceiver, new IntentFilter(StoreLocation.LOCATION_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("active", active);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.main_activity_settings:
                Intent intent = new Intent()
                        .setClass(getApplicationContext(), MainActivitySettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try{
            //get the user's current location only after they accepted the EULA
            if(hasAcceptedEULA){
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                updateUI(mLastLocation);
            }
        }
        catch(SecurityException e){
            //pass silently
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    //Add the location request to FusedLocationAPI
    private void addMyLocationUpdates(){
        //Build the Location Request based off the user's setting for updateDelay,
        // then check if we have the settings for making request,
        initLocationInformation();
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
                                    Toast.LENGTH_SHORT
                            ).show();

                            Log.d("Derp", "Location request added");

                        }
                        catch (SecurityException e){
                            //pass silently
                        }

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
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
                    addMyLocationUpdates();
                    break;
                case RESULT_CANCELED:
                    //Notify user that location is necessary
                    Toast.makeText(
                            getApplicationContext(),
                            "Location must be enabled for Bread Crumms to work",
                            Toast.LENGTH_LONG).show();
                    ToggleButton button = (ToggleButton)
                            findViewById(R.id.myButton);
                    button.setChecked(false);
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

    /**
     * Triggered whenever the user clicks the button turning starting/stopping StoreLocation service
     * @param view
     */
    public void toggleFindLocation(View view){

        if(active){
            active = false;
            removeMyLocationUpdates();
            Intent tmp = new Intent()
                    .setClass(MainActivity.this, StoreLocation.class);
            stopService(tmp);

        }
        else{
            active = true;
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

    /**
     * Helper function that sets the value of member variables of MainActivity
     *  used for generating the LocationRequest to the user's settings
     *      <ul>
     *          <li>
     *              LocationRequest locationRequest
     *           </li>
     *           <li>
     *               PendingIntent locationIntent
     *           </li>
     *           <li>
     *               LocationSettingsRequest.Builder locationSettingsRequest
     *           </li>
     *           <li>
     *               long updateDelay
     *           </li>
     *      </ul>
     */
    private void initLocationInformation(){
        //set how many milliseconds to wait until the next location update
        updateDelay = Long.valueOf(sharedPreferences.getString("updateDelay", "60000"));

        locationRequest = new LocationRequest()
                .setFastestInterval(updateDelay)
                .setInterval(updateDelay);

        //set priority of locationRequest based on user settings

        String locationRequestSharedPreferenceKey = sharedPreferences.getString(
                "locationPrecision",
                getString(R.string.mainActivitySettings_locationPrecision_HIGH_ACCURACY)
        );

        if(locationRequestSharedPreferenceKey.equals(getString(R.string.mainActivitySettings_locationPrecision_BALANCED_POWER_ACCURACY))){
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
        //if all the above fail, set the priority to HIGH_ACCURACY
        else {
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        //have pendingIntent send broadcast that will be picked up
        //  by any registered BroadcastReceivers in the current package
        //  that have the action StoreLocation.LOCATION_UPDATE set
        Intent tempIntent = new Intent()
                .setPackage(getApplicationContext().getPackageName())
                .setAction(StoreLocation.LOCATION_UPDATE);
        locationIntent = PendingIntent.getBroadcast(getApplicationContext(),
                StoreLocation.LOCATION_UPDATE_CODE, tempIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);
    }

}