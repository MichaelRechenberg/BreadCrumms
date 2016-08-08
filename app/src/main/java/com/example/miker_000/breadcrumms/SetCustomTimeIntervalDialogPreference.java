package com.example.miker_000.breadcrumms;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.DialogPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

/**
 * Created by miker_000 on 8/7/2016.
 */
public class SetCustomTimeIntervalDialogPreference extends DialogPreference {

    public static String datePattern = "yyyy.MM.dd";
    public static String DEFAULT_LATEST_DATE = "2000.01.01";
    public static String DEFAULT_EARLIEST_DATE = "2020.01.01";
    public static String LATEST_DATE_STRING_KEY = "heatmap_latestDateString";
    public static String EARLIEST_DATE_STRING_KEY = "heatmap_earliestDateString";
    private DatePicker latestDatePicker;
    private DatePicker earliestDatePicker;

    SharedPreferences sharedPref;

    public SetCustomTimeIntervalDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
        setDialogLayoutResource(R.layout.setcustomtimeinterval_dialog_preference);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        SimpleDateFormat df = new SimpleDateFormat(datePattern);
        String latestDateString = sharedPref.getString(LATEST_DATE_STRING_KEY, DEFAULT_LATEST_DATE);
        String earliestDateString = sharedPref.getString(EARLIEST_DATE_STRING_KEY, DEFAULT_EARLIEST_DATE);
        Log.d("Tmp", "latest Stored as " + latestDateString);
        Log.d("Tmp", "earliest stored as " + earliestDateString);

        Date latestDate = null;
        Date earliestDate = null;
        try {
            latestDate = df.parse(latestDateString);
            earliestDate = df.parse(earliestDateString);
        }
        catch(ParseException e){
            e.printStackTrace();
        }

        latestDatePicker = (DatePicker) view.findViewById(R.id.latestDatePicker);
        earliestDatePicker = (DatePicker) view.findViewById(R.id.earliestDatePicker);

        //set pickers and set up listeners
        Calendar cal = new GregorianCalendar();
        cal.setTime(latestDate);
        int latestDateYear = cal.get(Calendar.YEAR);
        int latestDateMonth = cal.get(Calendar.MONTH);
        int latestDateDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        cal.add(Calendar.DATE, 1);
        Date minDateForEarliestDatePicker = cal.getTime();
        Log.d("Tmp", "Latest Date set to: " + cal.getTime().toString());

        //Every time the date of the latestDatePicker is changed, ensure that the earliestDatePicker is
        //  at least one day in the future of the new date of latestDatePicker
        latestDatePicker.init(latestDateYear, latestDateMonth, latestDateDayOfMonth, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Log.d("Tmp", "onDateChanged() called");
                SimpleDateFormat df = new SimpleDateFormat(datePattern);
                Date minDate = null;
                try{
                    minDate = df.parse(generateDateString(year, month, dayOfMonth));
                }
                catch (ParseException e){
                    e.printStackTrace();
                }

                //Set the minimum allowable date for earliestDatePicker to be one day after
                //  the current value of latestDatePicker
                //Also, if the earliestDatePicker is currently at a date value less than
                //  this new minimum allowable date, set earliestDatePicker to this
                //  new minimum allowable date
                Calendar cal = Calendar.getInstance();
                cal.setTime(minDate);
                cal.add(Calendar.DATE, 1);
                minDate = cal.getTime();

                int minYear = cal.get(Calendar.YEAR);
                int minMonth = cal.get(Calendar.MONTH);
                int minDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

                //set year to previous to force setMinDate to update
                //required because of check within setMinDate() http://stackoverflow.com/a/19722636
                cal.set(minYear -1, minMonth, minDayOfMonth);
                earliestDatePicker.setMinDate(cal.getTime().getTime());
                //set min date to actual new minimum date
                cal.set(minYear, minMonth, minDayOfMonth);
                earliestDatePicker.setMinDate(minDate.getTime());


                //Check to see if we have to force earliestDatePicker to new min date
                int earliestDatePickerCurrYear = earliestDatePicker.getYear();
                int earliestDatePickerCurrMonth = earliestDatePicker.getMonth();
                int earliestDatePickerCurrDayOfMonth = earliestDatePicker.getDayOfMonth();
                Date earliestDatePickerCurrentDate = null;
                try{
                    earliestDatePickerCurrentDate =  df.parse(
                            generateDateString(
                                    earliestDatePickerCurrYear,
                                    earliestDatePickerCurrMonth,
                                    earliestDatePickerCurrDayOfMonth
                            )
                    );
                }
                catch(ParseException e){
                    e.printStackTrace();
                }
                Log.d("Tmp", "The minDate is: " + minDate.toString());
                boolean needToChange = earliestDatePickerCurrentDate.before(minDate);
                Log.d("Tmp", "Need to update earliestDatePicker? " + String.valueOf(needToChange));
                if(needToChange){
                    earliestDatePicker.updateDate(minYear, minMonth, minDayOfMonth);
                }

                Log.d("Tmp", "The new Set min Date is: " + new Date(earliestDatePicker.getMinDate()).toString());


            }
        });

        cal.setTime(earliestDate);
        int earliestDateYear = cal.get(Calendar.YEAR);
        int earliestDateMonth = cal.get(Calendar.MONTH);
        int earliestDateDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        Log.d("Tmp", "Earliest Date set to: " + cal.getTime().toString());
        earliestDatePicker.setMinDate(minDateForEarliestDatePicker.getTime());
        earliestDatePicker.updateDate(earliestDateYear, earliestDateMonth, earliestDateDayOfMonth);


    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult){

            int earliestDateYear = earliestDatePicker.getYear();
            int earliestDateMonth = earliestDatePicker.getMonth();
            int earliestDateDayOfMonth = earliestDatePicker.getDayOfMonth();

            int latestDateYear = latestDatePicker.getYear();
            int latestDateMonth = latestDatePicker.getMonth();
            int latestDateDayOfMonth = latestDatePicker.getDayOfMonth();

            String earliestDateString = generateDateString(earliestDateYear, earliestDateMonth, earliestDateDayOfMonth);
            String latestDateString = generateDateString(latestDateYear, latestDateMonth, latestDateDayOfMonth);

            Log.d("Tmp", latestDateString);
            Log.d("Tmp", earliestDateString);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(LATEST_DATE_STRING_KEY, latestDateString);
            editor.putString(EARLIEST_DATE_STRING_KEY, earliestDateString);
            editor.commit();
        }
    }

    //but what if year and month are not in format???
    private static String generateDateString(int year, int month, int dayOfMonth){
        //Add one to month b/c Android is zero-based while SimpleDateFormatter is not
        month++;
        String yearString = String.format(Locale.getDefault(), "%04d", year);
        String monthString = String.format(Locale.getDefault(), "%02d", month);
        String dayOfMonthString = String.format(Locale.getDefault(), "%02d", dayOfMonth);

        String result =  yearString + "." + monthString + "." + dayOfMonthString;
        return result;
    }
}
