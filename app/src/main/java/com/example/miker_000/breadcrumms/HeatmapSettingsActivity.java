package com.example.miker_000.breadcrumms;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.prefs.PreferenceChangeListener;

/**
 * Activity for changing heatmap settings
 */
public class HeatmapSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new HeatmapActivityFragment())
                .commit();
    }

    public static class HeatmapActivityFragment extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.heatmap_activity_settings);


            //disable the set custom range preference if customTimeInterval option is not selected
            String timeIntervalValue = (String)
                    ((ListPreference) findPreference("heatmap_timeInterval")).getEntry();
            if(!timeIntervalValue.equals(getString(R.string.heatmapActivitySettings_interval_custom))){
                Preference setCustomRangePreference = findPreference("heatmap_customTimeIntervalPreference");
                setCustomRangePreference.setEnabled(false);
            }




            //set up preference change listener to update List Preferences
            prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    Preference preference = findPreference(key);
                    if(key.equals("heatmap_timeInterval")){
                        ListPreference timeInterval  = (ListPreference) preference;
                        String summary = (String) timeInterval.getEntry();
                        Preference setCustomRangePreference = findPreference("heatmap_customTimeIntervalPreference");
                        if(summary.equals(getString(R.string.heatmapActivitySettings_interval_custom))){
                            setCustomRangePreference.setEnabled(true);
                        }
                        else{
                            //disable set custom range
                            setCustomRangePreference.setEnabled(false);
                        }
                    }
                }
            };
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        }
    }
}
