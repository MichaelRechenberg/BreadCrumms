package com.example.miker_000.breadcrumms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MapLocationActivity extends AppCompatActivity
    implements OnMapReadyCallback {


    private GoogleMap theMap;
    private TileOverlay heatMapOverlay;
    private boolean isHeatMapOn;

    private SQLiteDatabase db;
    private LocationDatabaseDbHelper dbHelper;

    private SharedPreferences sharedPreferences;




    @SuppressWarnings("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location);

        //set up shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //init DB variable
        dbHelper = new LocationDatabaseDbHelper(getApplicationContext());
        db = dbHelper.getReadableDatabase();

        //set up the tool bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.theToolbar);
        toolbar.setTitleTextColor(Color.parseColor("#ffffff"));
        toolbar.setTitle("Heat Map");
        setSupportActionBar(toolbar);

        //set up toggle switch
        Switch heatMapSwitch = (Switch) findViewById(R.id.heatMapSwitch);
        heatMapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Turn on heatmap overlay
                if (isChecked) {
                    isHeatMapOn = true;
                    makeHeatMap();
                } else {
                    // The toggle is disabled
                    isHeatMapOn = false;
                    if(heatMapOverlay!=null){
                        heatMapOverlay.remove();
                    }

                }
            }
        });

        //Set up the Google Map Object
        FragmentManager fragmentManager= getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment)
                fragmentManager.findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        isHeatMapOn = false;
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
                Intent intent = new Intent()
                        .setClass(getApplicationContext(), HeatmapSettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


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


    /**
     * Make the heat map using the settings set by the user
     */
    private void makeHeatMap(){
        LatLngBounds bounds = theMap.getProjection().getVisibleRegion().latLngBounds;
        //Todo: Import settings from sharedPreferences
        String limit = null;
        Date latestDate = null;
        Date earliestDate = null;
        String order = LocationDatabaseContract.LocationEntry.COLUMN_NAME_TIME_CREATED;

        //Using the time interval set in the settings, generate latestDate and earliestDate
        String interval = sharedPreferences.getString(
                "heatmap_timeInterval",
                getString(R.string.heatmapActivitySettings_interval_allDays)
        );

        //Set latestDate to the appropriate time in the past
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        //This month
        if(interval.equals(getString(R.string.heatmapActivitySettings_interval_thisMonth))){
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            latestDate = cal.getTime();
        }
        //This Week
        else if (interval.equals(getString(R.string.heatmapActivitySettings_interval_thisWeek))){
            cal.set(Calendar.DAY_OF_WEEK, cal.getActualMinimum(Calendar.DAY_OF_WEEK));
            latestDate = cal.getTime();
        }
        //Today
        else if(interval.equals(getString(R.string.heatmapActivitySettings_interval_today))){
            latestDate = cal.getTime();
        }
        //If all the above if statements fail, that means that the user selected all days
        //  so latestDate and earliestDate should remain null



        String query = LocationDatabaseDbHelper.gatherLocationsQueryString(bounds, latestDate, earliestDate,
                limit, order);

        new DumpAllEntries().execute(db, query);

    }

    private class DumpAllEntries extends AsyncTask<Object, Integer, Cursor> {
        @Override
        protected Cursor doInBackground(Object... objects) {
            SQLiteDatabase db = (SQLiteDatabase) objects[0];
            String query = (String) objects[1];
            Cursor result = db.rawQuery(query, null);
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

                //Add all points retuned to ArrayList of LatLng points to then pass to Heat Map
                ArrayList<LatLng> pts = new ArrayList<LatLng>(row_count);
                for(result.moveToFirst(); !result.isAfterLast(); result.moveToNext()){
                    LatLng tempPt = new LatLng(result.getDouble(latitudeIndex), result.getDouble(longitudeIndex));
                    pts.add(tempPt);
                }

                HeatmapTileProvider heatmapProvider = new HeatmapTileProvider.Builder()
                        .data(pts)
                        .build();
                heatMapOverlay = theMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));

            }

            //release the cursor
            result.close();

        }
    }
}
