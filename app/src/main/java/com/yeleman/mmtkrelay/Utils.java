package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Iterator;
import java.util.Set;


public class Utils {

    private static final String WAKELOCK_TAG = "MMTK-LOCK";

    private static PowerManager.WakeLock getWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Utils.WAKELOCK_TAG);
    }
    public static void AcquireWakeLock(Context context) {
        PowerManager.WakeLock wakeLock = Utils.getWakeLock(context);
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }
    public static void ReleaseWakeLock(Context context) {
        PowerManager.WakeLock wakeLock = Utils.getWakeLock(context);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public static void dumpIntent(Intent i){

        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Log.e(Constants.TAG, "Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.e(Constants.TAG, "[" + key + "=" + bundle.get(key)+"]");
            }
            Log.e(Constants.TAG, "Dumping Intent end");
        }
    }

    public static void triggerUIRefresh(Context context, String... refreshKeys) {
        Log.e(Constants.TAG, "triggerUIRefresh");
        Intent intent = new Intent(Constants.UI_TAMPERED_FILTER);
        for (String refreshKey : refreshKeys) {
            intent.putExtra(refreshKey, true);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void updateSharedPreferences(Context context, String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(key, value);
        prefEditor.apply();
    }
}
