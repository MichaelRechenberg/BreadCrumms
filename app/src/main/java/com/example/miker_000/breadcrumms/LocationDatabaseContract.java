package com.example.miker_000.breadcrumms;

import android.provider.BaseColumns;

/**
 * Created by miker_000 on 7/15/2016.
 * Contract helper class for LocationDatabase
 */
public final class LocationDatabaseContract {
    public LocationDatabaseContract(){}

    public static abstract class LocationEntry implements BaseColumns{
        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_NAME_LATITUDE = "lat";
        public static final String COLUMN_NAME_LONGITUDE = "lng";
        public static final String COLUMN_NAME_TIME_CREATED = "time_created";
    }
}
