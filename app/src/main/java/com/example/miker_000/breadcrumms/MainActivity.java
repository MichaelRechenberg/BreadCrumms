package com.example.miker_000.breadcrumms;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;

import android.location.Location;

import android.os.IBinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {
    private Location mLastLocation = null;
    //indicates if we are currently making location updates
    private boolean active = false;
    private TextView latitudeData;
    private TextView longitudeData;
    private final int REQUEST_FOR_LOCATION = 1;
    //Inicates if this Activity is currently bound to GetPeriodicLocation Service
    private boolean isBound = false;


    //Application Lifetime Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitudeData = (TextView)findViewById(R.id.locLatData);
        longitudeData = (TextView) findViewById(R.id.locLngData);

        updateUI();
    }


    //Unbind from the service if the app is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            stopLocationMonitoring();
        }
        Log.d("Derp", "onDestroy() called");
    }

    //Used to connect to GetPeriodicLocation service
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d("Derp", "Connected To Service");
            isBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("Derp", "Disconnected From Service :/");
            isBound = false;
        }
    };


    /*
     * Begins service GetPeriodicLocation to retrieve location updates
     */
    private void beginLocationMonitoring(){
        Intent derp = new Intent()
                .setClass(this, GetPeriodicLocation.class);
        bindService(derp, mConnection, Context.BIND_AUTO_CREATE);
    }

    //Helper method that unbinds from service
    private void stopLocationMonitoring(){
        unbindService(mConnection);
        Log.d("Derp", "unBound From service");
        isBound = false;
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
            msg.setText("NOT Looking for Location!");
            stopLocationMonitoring();
            active = false;
        }
        else{
            msg.setText("Looking for Location");
            beginLocationMonitoring();
            active = true;
        }
    }

}