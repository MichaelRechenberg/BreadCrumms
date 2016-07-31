package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

/**
 * Seek Bar Dialog Preference for use in settings
 */
public class SeekBarDialogPreference extends DialogPreference {

    private SeekBar seekBar;
    //Integer representing percent of opacity 0-100%
    private int opacity;
    //Default value of the SeekBar
    private final int DEFAULT_VALUE = 70;

    public SeekBarDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
        setDialogLayoutResource(R.layout.seekbar_dialog_preference);
    }

    //Get handle on the SeekBar of the Dialog
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setProgress(getPersistedInt(DEFAULT_VALUE));
    }

    //Store result if user selects OK
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult){
            opacity = seekBar.getProgress();
            persistInt(opacity);
        }
    }


    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if(restorePersistedValue){
            opacity = getPersistedInt(DEFAULT_VALUE);
        }
        else{
            opacity = (Integer) defaultValue;
            persistInt(opacity);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }
}
