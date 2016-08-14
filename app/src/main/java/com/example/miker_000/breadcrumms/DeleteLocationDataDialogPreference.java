package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Dialog preference allowing user to delete all location data
 */
public class DeleteLocationDataDialogPreference extends DialogPreference {
    public DeleteLocationDataDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
    }




    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult){
            SQLiteDatabase db = new LocationDatabaseDbHelper(getContext().getApplicationContext()).getWritableDatabase();
            db.delete(LocationDatabaseContract.LocationEntry.TABLE_NAME,
                    null,
                    null);
        }

    }
}
