package com.example.miker_000.breadcrumms;

import android.app.PendingIntent;
import android.location.Location;
import android.media.UnsupportedSchemeException;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MapLocationActivity extends AppCompatActivity
    implements OnMapReadyCallback {

    private GoogleMap theMap;
    //indicates if the map is loaded yet
    private boolean isLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location);

        FragmentManager fragmentManager= getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment)
                fragmentManager.findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {


        isLoaded = true;
        theMap = googleMap;
        theMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);


        Location userLocation = (Location) getIntent().getExtras().get("userLocation");
        //Todo: have the Lat/Lng default to user's location
        double latitude = 48.452000;
        double longitude = -38.414656;

        if(userLocation != null){
            latitude = userLocation.getLatitude();
            longitude = userLocation.getLongitude();
        }

        LatLng startLocation = new LatLng(latitude, longitude);
        //The 15 refers to zoom level [1-20]
        //See: https://developers.google.com/maps/documentation/android-api/views#zoom
        theMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15));

    }

    public void setBounds(View view){
        LatLngBounds bounds = theMap.getProjection().getVisibleRegion().latLngBounds;
        Log.d("Tmp", bounds.toString());
    }
}
