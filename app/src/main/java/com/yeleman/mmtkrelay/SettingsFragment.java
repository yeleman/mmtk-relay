package com.yeleman.mmtkrelay;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // display important values
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        EditTextPreference server_url_pref = (EditTextPreference) findPreference(Constants.SETTINGS_SERVER_URL);
        server_url_pref.setSummary(sharedPref.getString(Constants.SETTINGS_SERVER_URL, null));

        EditTextPreference server_id_pref = (EditTextPreference) findPreference(Constants.SETTINGS_SERVER_ID);
        server_id_pref.setSummary(sharedPref.getString(Constants.SETTINGS_SERVER_ID, null));
    }
}
