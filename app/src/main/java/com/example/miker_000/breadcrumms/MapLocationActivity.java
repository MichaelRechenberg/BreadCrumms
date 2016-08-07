package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Displays a GoogleMap Fragment that will show a heat map of all the points stored
 *  within the user's desired range of dates (All Time, Past Month, Past Week, Today)
 *
 *  User can store a snapshot of the Google Map
 *  User can manually set location via a Dialog
 */
public class MapLocationActivity extends AppCompatActivity
    implements OnMapReadyCallback, SetLatLngDialogFragment.LatLngDialogListener{


    private GoogleMap theMap;
    private TileOverlay heatMapOverlay;
    private boolean isHeatMapOn;

    private SQLiteDatabase db;
    private LocationDatabaseDbHelper dbHelper;

    private SharedPreferences sharedPreferences;
    private Switch heatmapSwitch;




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
        heatmapSwitch = (Switch) findViewById(R.id.heatMapSwitch);
        heatmapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

    //Check for Internet connectivity for Google Maps
    //If not connected to the internet, make a Toast notification
    @Override
    protected void onStart() {
        super.onStart();


        //Taken from Android Documentation
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if(!isConnected){
            Toast.makeText(
                    getApplicationContext(),
                    R.string.heatmap_noWifi_toastMessage,
                    Toast.LENGTH_LONG).show();
        }

        if(theMap!=null){
            changeMapType();
        }
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
                return true;
            case R.id.heatmap_snapshot:
                //Save the picture to external storage

                //External storage is not available
                if(!isExternalStorageWritable()){
                    return false;
                }


                final File path = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    getString(R.string.heatmap_snapshotsDirName)
                );


                theMap.snapshot(
                        new GoogleMap.SnapshotReadyCallback() {
                            @Override
                            public void onSnapshotReady(Bitmap bitmap) {
                                try{
                                    path.mkdirs();
                                    int photoCount = sharedPreferences.getInt("snapshotCounter", 0);
                                    File snapshot = new File(path, "map_snapshot00"+photoCount+".jpeg");

                                    FileOutputStream outputStream = new FileOutputStream(snapshot);
                                    bitmap.compress(
                                            Bitmap.CompressFormat.JPEG,
                                            100,
                                            outputStream
                                    );

                                    //Have MediaScanner scan file so the user can immediately access it
                                    MediaScannerConnection.scanFile(
                                            getApplicationContext(),
                                            new String[] { snapshot.getAbsolutePath() },
                                            null,
                                            null
                                    );

                                    Toast.makeText(
                                            getApplication(),
                                            "Saved!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    photoCount++;
                                    editor.putInt("snapshotCounter", photoCount);
                                    editor.commit();


                                    try{
                                        outputStream.flush();
                                        outputStream.close();
                                    }
                                    catch(IOException e){
                                        e.printStackTrace();
                                    }

                                }
                                catch(FileNotFoundException e){
                                    Log.e("MapLocation", "Could not find snapshot image file");
                                }
                            }
                        }
                );

                return true;

            case R.id.heatmap_setLocation:
                //open Dialog for user to set Lat/Lng manually
                SetLatLngDialogFragment dialog = new SetLatLngDialogFragment();
                dialog.show(getSupportFragmentManager(), "SetLatLngDialogFragment");
            default:
                return super.onOptionsItemSelected(item);
        }

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {


        theMap = googleMap;


        changeMapType();


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


    //Move the map the the desired lat/lng, not changing zoom level
    //This is called when the user hits OK on the SetLatLngDialogFragment
    public void moveCameraFromDialog(double lat, double lng){
        //Inavlid coordinates, let user know there's a mistake
        if(lat < -90 || lat > 90 || lng < -180 || lng >180){
            Toast.makeText(
                    getApplicationContext(),
                    "Invalid Coordinates, Please Try Again",
                    Toast.LENGTH_SHORT
            ).show();
        }
        else{
            theMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        }
    }


    /**
     * Make the heat map using the settings set by the user
     */
    private void makeHeatMap(){
        Toast.makeText(
                getApplicationContext(),
                getString(R.string.heatmapBeingGeneratedToastText),
                Toast.LENGTH_LONG
        ).show();
        LatLngBounds bounds = theMap.getProjection().getVisibleRegion().latLngBounds;
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

        //disable the switch until the heatmap is generated
        heatmapSwitch.setEnabled(false);
        new GatherPointsAndGenerateHeatmap().execute(db, query);

    }

    /**
     * Async Task that querys the database for the points within the bounds of the Google Map
     *  and then takes those points and generates a HeatmapTileProvider.
     *
     * The heatmapTileOverlay is updated within postExecute
     *
     * The first argument is the SQLiteDatabase to query
     * The second argument is the query you wish to execute
     */
    private class GatherPointsAndGenerateHeatmap extends AsyncTask<Object, Integer, HeatmapTileProvider> {
        @Override
        protected HeatmapTileProvider doInBackground(Object... objects) {
            SQLiteDatabase db = (SQLiteDatabase) objects[0];
            String query = (String) objects[1];
            Cursor result = db.rawQuery(query, null);
            int row_count = result.getCount();
            HeatmapTileProvider heatmapProvider = null;
            if(row_count > 0) {
                int latitudeIndex = result.getColumnIndex(LocationDatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE);
                int longitudeIndex = result.getColumnIndex(LocationDatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE);
                //Add all points retuned to ArrayList of LatLng points to then pass to Heat Map
                ArrayList<LatLng> pts = new ArrayList<LatLng>(row_count);
                for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
                    LatLng tempPt = new LatLng(result.getDouble(latitudeIndex), result.getDouble(longitudeIndex));
                    pts.add(tempPt);
                }
                //get opacity setting from SharedPreferences
                final double opacity = ((double) sharedPreferences.getInt("heatmap_opacity", 70)) / 100;

                heatmapProvider = new HeatmapTileProvider.Builder()
                        .data(pts)
                        .opacity(opacity)
                        .build();
            }
            //release the cursor
            result.close();

            return heatmapProvider;
        }

        @Override
        protected void onPostExecute(HeatmapTileProvider heatmapProvider) {
            if(heatmapProvider!=null){
                heatMapOverlay = theMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
            }
            //re-enable the switch so user can toggle it again
            heatmapSwitch.setEnabled(true);
        }
    }


    //Helper method provided in Android Documenation for
    //  saving a file to external storage
    private static boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }
        return false;
    }

    //Helper method that sets the map type based on user's settings
    private void changeMapType(){

        String mapType = sharedPreferences.getString(
                "heatmap_mapType",
                getString(R.string.heatmapActivitySettings_mapType_MAP_TYPE_SATELLITE)
        );
        if(mapType.equals(getString(R.string.heatmapActivitySettings_mapType_MAP_TYPE_NORMAL))){
            theMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        else if (mapType.equals(getString(R.string.heatmapActivitySettings_mapType_MAP_TYPE_TERRAIN))){
            theMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        else{
            theMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }
}
