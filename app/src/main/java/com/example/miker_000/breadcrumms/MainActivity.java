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
    private boolean active = false;
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
        //Reset the DB.  The arguments 1,2 do nothing but satisfy func signature
        //dbHelper.onUpgrade(db, 1, 2);

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
            unbindService(mConnection);
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
                            //Bind to service StoreLocation, which will update
                            //  the SQLLite DB everytime we get a location update
                            Intent tmp = new Intent()
                                    .setClass(MainActivity.this, StoreLocation.class);
                            bindService(tmp, mConnection, Context.BIND_AUTO_CREATE);
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
            unbindService(mConnection);
            logEntries();

        }
        else{
            active = true;
            msg.setText(R.string.looking_for_location);
            addMyLocationUpdates();
            registerReceiver(locationReceiver, new IntentFilter(StoreLocation.LOCATION_UPDATE));

        }
    }

    //Helper function to test SQLlite functionality
    //Uses AsyncTask to not block UI thread
    private void logEntries(){

        new DumpAllEntries().execute(db);
    }

    private class DumpAllEntries extends AsyncTask<Object, Integer, Cursor>{
        @Override
        protected Cursor doInBackground(Object... objects) {
            SQLiteDatabase db = (SQLiteDatabase) objects[0];

            String table = LocationDatabaseContract.LocationEntry.TABLE_NAME;
            String[] columns = {
                    LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED,
                    LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE,
                    LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE
            };
            //get the most recent logs
            String orderBy = LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED + " DESC";
            String limit = "";


            //Get the 10 most recent locations and log them
            //This is really just for getting used to SQLlite, not used in final app
            Cursor result = db.query(table, columns, null, null, null, null, orderBy, limit);
            return result;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            int row_count = result.getCount();
            if(row_count > 0){
                //convert from UTC to local timezone
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                int latitudeIndex = result.getColumnIndex(LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE);
                int longitudeIndex = result.getColumnIndex(LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE);
                int time_createdIndex = result.getColumnIndex(LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED);
                Log.d("SQL", String.valueOf(row_count) + " rows were returned");
                for(result.moveToFirst(); !result.isAfterLast(); result.moveToNext()){
                    String dateString = result.getString(time_createdIndex);
                    try{
                        dateString = df.parse(dateString).toString();
                    }
                    catch (ParseException e){
                        dateString = "ERROR IN PARSING DATE";
                    }


                    Log.d("SQL", "Time Created: " + dateString);
                    Log.d("SQL", "Latitude: " + result.getDouble(latitudeIndex));
                    Log.d("SQL", "Longitude: " + result.getDouble(longitudeIndex));
                }
            }

            //release the cursor
            result.close();



            double southwest_lat =
                    Double.longBitsToDouble(
                            sharedPreferences.getLong(MapLocationActivity.SW_BOUND_LAT, 0)
                    );
            double southwest_lng =
                    Double.longBitsToDouble(
                            sharedPreferences.getLong(MapLocationActivity.SW_BOUND_LNG, 0)
                    );
            double northwest_lat =
                    Double.longBitsToDouble(
                            sharedPreferences.getLong(MapLocationActivity.NE_BOUND_LAT, 0)
                    );
            double northwest_lng =
                    Double.longBitsToDouble(
                            sharedPreferences.getLong(MapLocationActivity.NE_BOUND_LNG, 40)
                    );

            Log.d("Tmp", String.valueOf(southwest_lat));
            Log.d("Tmp", String.valueOf(southwest_lng));
            Log.d("Tmp", String.valueOf(northwest_lat));
            Log.d("Tmp", String.valueOf(northwest_lng));




        }
    }

    public void startMapLocationActivity(View view){

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, MapLocationActivity.class);
        intent.putExtra("userLocation", mLastLocation);
        startActivity(intent);
    }

}