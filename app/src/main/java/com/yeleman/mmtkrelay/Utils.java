package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import static android.os.Environment.getExternalStorageDirectory;


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

    public static Boolean copyFile(File from, File to) {
        try {
            FileChannel src = new FileInputStream(from).getChannel();
            FileChannel dst = new FileOutputStream(to).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
        } catch (Exception ex) {
            Log.e(Constants.TAG, ex.toString());
            return false;
        }
        return true;
    }

    public static Boolean exportDatabase(Context context) {
        File localDBFile = context.getDatabasePath(Constants.DATABASE_NAME);
        File sdCard = getExternalStorageDirectory();

        File destination = new File(sdCard.getAbsolutePath() + File.pathSeparator + context.getPackageName());
        if (!sdCard.canWrite()) {
            Log.e(Constants.TAG, "Unable to write to SD card");
            return false;
        }

        if (!destination.exists()) {
            if (destination.mkdir()) {
            }
        }

        Date now = new Date();
        String fileName = String.format("backup-%s.db", TextUtils.fileDateFormat(now));
        File backupFile = new File(destination, fileName);

        return copyFile(localDBFile, backupFile);
    }
}
