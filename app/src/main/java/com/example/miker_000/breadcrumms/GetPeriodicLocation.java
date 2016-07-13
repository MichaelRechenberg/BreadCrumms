package com.example.miker_000.breadcrumms;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class GetPeriodicLocation extends Service {
    private Messenger messenger = new Messenger(new LocationHandler());

    public GetPeriodicLocation() {

    }

    /**
     * Defines Handler for Messenger
     * TODO: Make this in a separate Java Class file
     */
    private class LocationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //Log.d("IT WORKED", "NANANANNANAN BATMAN");
            Toast.makeText(getApplicationContext(), "From Service", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Derp", "onBind() called");
        Messenger temp = intent.getParcelableExtra("messenger");
        messenger =  new Messenger (temp.getBinder());
        //This is just here for testing
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                Message msg = Message.obtain(null, 0, 0, 0);
                try{
                    messenger.send(msg);
                }
                catch (RemoteException e){
                    e.printStackTrace();
                    Log.e("Derp", "Fucked Up");
                }
            }
        }, 2000);

        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        messenger = null;
        return super.onUnbind(intent);
    }

}
