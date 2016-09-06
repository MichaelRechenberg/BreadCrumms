package com.example.miker_000.breadcrumms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by miker_000 on 8/1/2016.
 * DialogFragment for user to select Lat/Lng
 */
public class SetLatLngDialogFragment extends DialogFragment {

    public interface LatLngDialogListener {
        void moveCameraFromDialog(double lat, double lng);
    }

    LatLngDialogListener latLngDialogListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        latLngDialogListener = (LatLngDialogListener) context;
    }




    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.set_latlng_dialog, null);
        EditText latText = (EditText) view.findViewById(R.id.latitude);
        EditText lngText = (EditText) view.findViewById(R.id.longitude);

        latText.setText(String.valueOf(getArguments().getDouble("lat")));
        lngText.setText(String.valueOf(getArguments().getDouble("lng")));


        //Init custom layout and callback methods
        builder.setView(view)
                .setTitle(getString(R.string.setLatLngDialogFragmentMessage))
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        double lat;
                        double lng;

                        Dialog dialog = (Dialog) dialogInterface;

                        EditText latText = (EditText) dialog.findViewById(R.id.latitude);
                        EditText lngText = (EditText) dialog.findViewById(R.id.longitude);

                        try{
                            lat = Double.parseDouble(latText.getText().toString());
                            lng = Double.parseDouble(lngText.getText().toString());
                        }
                        //The number could not be parsed, set lat/lng to values outside
                        //  the acceptable range so they are rejected by MapLocationActivity
                        catch(NumberFormatException e){
                            lat = -1337;
                            lng = -1337;
                        }


                        latLngDialogListener.moveCameraFromDialog(lat, lng);
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), null);
        return builder.create();
    }


}
