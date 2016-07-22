package com.example.miker_000.breadcrumms;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.media.UnsupportedSchemeException;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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

    //String constants referring to the keys of the Latitude and Longititude
    //  of the two points used in LatLngBound (The Southwest point and the
    //  Northeast point)
    public static final String SW_BOUND_LAT = "LatLngBound_SW_Lat";
    public static final String SW_BOUND_LNG = "LatLngBound_SW_Lng";
    public static final String NE_BOUND_LAT = "LatLngBound_NE_Lat";
    public static final String NE_BOUND_LNG = "LatLngBound_NE_Lng";



    private GoogleMap theMap;
    //indicates if the map is loaded yet
    private boolean isLoaded = false;

    private SharedPreferences sharedPreferences;


    @SuppressWarnings("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location);

        sharedPreferences = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );
        //set up the tool bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.theToolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));

        toolbar.setTitle("Heat Map");
        setSupportActionBar(toolbar);
        FragmentManager fragmentManager= getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment)
                fragmentManager.findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.heatmap_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.heatmap_settings:

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


        isLoaded = true;
        theMap = googleMap;
        theMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);


        Location userLocation = (Location) getIntent().getExtras().get("userLocation");


        //defaults to google headquarters
        double latitude = 37.420841;
        double longitude = -127.084063;

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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(SW_BOUND_LAT, Double.doubleToLongBits(bounds.southwest.latitude));
        editor.putLong(SW_BOUND_LNG,  Double.doubleToLongBits(bounds.southwest.longitude));
        editor.putLong(NE_BOUND_LAT, Double.doubleToLongBits(bounds.northeast.latitude));
        editor.putLong(NE_BOUND_LNG, Double.doubleToLongBits(bounds.northeast.longitude));
        editor.commit();
        Toast.makeText(getApplicationContext(), "Bounds Successfully Set!", Toast.LENGTH_SHORT).show();
    }
}
