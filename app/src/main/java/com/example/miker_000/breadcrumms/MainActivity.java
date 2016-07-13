package com.example.miker_000.breadcrumms;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;
    //indicates if we are currently making location updates
    private boolean active = false;
    private TextView latitudeData;
    private TextView longitudeData;
    private final int REQUEST_FOR_LOCATION = 1;
    private Messenger messengerForService = null;
    //Inicates if this Activity is currently bound to GetPeriodicLocation Service
    private boolean isBound = false;

    //Key used to index Messenger in GetPeriodicLocation service
    private final String messengerKey = "com.mrechenberg.MainActivity";


    //Application Lifetime Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitudeData = (TextView)findViewById(R.id.locLatData);
        longitudeData = (TextView) findViewById(R.id.locLngData);

        updateUI();
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(isBound){
            stopLocationMonitoring();
        }
    }


    /**
     * Defines Handler for Messenger
     * TODO: Make this in a separate Java Class file
     */
    private class LocationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("IT WORKED", "NANANANNANAN BATMAN");
            Toast.makeText(getApplicationContext(), "Activity received message", Toast.LENGTH_SHORT).show();
        }
    };




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

    /*
     * Retrieves the last location of the user and checks for permission
     * TODO: Have some boolean value to check rather than check permission every time
     *  Initalize the Service and Messenger for IPC
     */
    private void beginLocationMonitoring(){
        messengerForService = new Messenger(new LocationHandler());
        Intent derp = new Intent()
                .setClass(this, GetPeriodicLocation.class)
                .putExtra("messenger", messengerForService);
        Log.d("Derp", String.valueOf(bindService(derp, mConnection,
                Context.BIND_AUTO_CREATE)));
    }

    //Helper method that unbinds from service
    private void stopLocationMonitoring(){
        unbindService(mConnection);
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