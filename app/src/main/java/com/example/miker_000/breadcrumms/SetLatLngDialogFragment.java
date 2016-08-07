package com.example.miker_000.breadcrumms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
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

        //Init custom layout and callback methods
        builder.setView(inflater.inflate(R.layout.set_latlng_dialog, null))
                .setTitle(getString(R.string.setLatLngDialogFragmentMessage))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        double lat;
                        double lng;

                        Dialog dialog = (Dialog) dialogInterface;

                        EditText latText = (EditText) dialog.findViewById(R.id.latitude);
                        EditText lngText = (EditText) dialog.findViewById(R.id.longitude);

                        lat = Double.parseDouble(latText.getText().toString());
                        lng = Double.parseDouble(lngText.getText().toString());

                        latLngDialogListener.moveCameraFromDialog(lat, lng);
                    }
                })
                .setNegativeButton("Cancel", null);
        return builder.create();
    }
}
