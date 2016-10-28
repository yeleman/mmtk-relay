package com.yeleman.mmtkrelay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SettingsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Log.d(Constants.TAG, "onSharedPreferenceChanged: " + key);
        if (key.equals(Constants.SETTINGS_SERVER_URL)) {
            // send a broadcast
            Intent intent = new Intent(Constants.SETTINGS_CHANGED_FILTER);
            intent.putExtra("key", key);
            sendBroadcast(intent);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
        if (key.equals(DHISUtils.KEY_DHIS_SERVER_URL)) {
            Utils.triggerUIRefresh(this, "refreshDhis");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

}