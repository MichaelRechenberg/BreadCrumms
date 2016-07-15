package com.example.miker_000.breadcrumms;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class StoreLocation extends Service {
    public StoreLocation() {
    }

    //TODO: Create and release resources on onBind, onUnbind,
    //  and when user swipes off of recent apps (onTaskRemove())
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Derp", "onBind() called");
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Derp", "onUnbind() called");
        return super.onUnbind(intent);
    }


}
