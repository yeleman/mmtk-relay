package com.yeleman.mmtkrelay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import static android.os.Environment.getExternalStorageDirectory;

class ExternalStorage {
    private boolean available = false;
    private boolean writable = false;
    ExternalStorage() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            available = writable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            available = true;
            writable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            available = writable = false;
        }
    }
    public boolean isAvailable() { return available; }
    public boolean isWritable() { return writable; }
    public File getDirectory() { return getExternalStorageDirectory(); }
}

class Utils {

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

    static void triggerUIRefresh(Context context, String... refreshKeys) {
        Intent intent = new Intent(Constants.UI_TAMPERED_FILTER);
        for (String refreshKey : refreshKeys) {
            intent.putExtra(refreshKey, true);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static void updateSharedPreferences(Context context, String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(key, value);
        prefEditor.apply();
    }

    static ExternalStorage getExternalStorage() { return new ExternalStorage(); }

    static Boolean copyFile(File from, File to) {
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

    static Boolean exportDatabase(Context context) {
        File localDBFile = context.getDatabasePath(Constants.getDatabaseName(context));
        Log.i(Constants.TAG, "DB exists: "+ localDBFile.exists());

        ExternalStorage sd = getExternalStorage();
        if (!sd.isWritable()) {
            Log.e(Constants.TAG, "Unable to write to SD card");
            return false;
        }

        File destination = new File(sd.getDirectory(), context.getPackageName());
        if (!destination.exists()) {
            Log.d(Constants.TAG, "destination folder does not exist.");
            if (destination.mkdirs()) {
                Log.d(Constants.TAG, "destination folder created.");
            } else {
                Log.e(Constants.TAG, "Unable to create export folder.");
                return false;
            }
        } else { Log.d(Constants.TAG, "destination folder exist."); }

        Date now = new Date();
        String fileName = String.format("backup-%s.db", TextUtils.fileDateFormat(now));
        File backupFile = new File(destination, fileName);
        try {
            if (!backupFile.createNewFile()) {
                Log.e(Constants.TAG, "Unable to write file at "+backupFile.getAbsolutePath());
                return false;
            }
            if (!copyFile(localDBFile, backupFile)) {
                Log.e(Constants.TAG, "Unable to copy file from "+ localDBFile.getAbsolutePath() +" to "+backupFile.getAbsolutePath());
                backupFile.delete();
                return false;
            }
            Log.i(Constants.TAG, "exported on " + backupFile.getAbsolutePath());
            return true;
        } catch(IOException ex) {
            backupFile.delete();
            Log.e(Constants.TAG, ex.toString());
            return false;
        }
    }

    public static void updateConnexionsStatus(Context context) {
        Log.d(Constants.TAG, "updateConnexionsStatus");

        // check for internet and networks requirements
        OrangeAPIService.startConnexionCheck(context);

        // checking server connexion now
        ServerAPIService.startConnexionCheck(context);
    }

    public static void showdisplayPermissionErrorPopup(final Context context, String title, String message, Boolean goToSettings) {
        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(message);
        helpBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        if (goToSettings) {
            helpBuilder.setPositiveButton(context.getString(R.string.open_settings_button),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                            intent.setData(uri);
                            context.startActivity(intent);
                        }
                    });
        } else {
            helpBuilder.setPositiveButton(context.getString(R.string.standard_dialog_ok),
                    new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {}});
        }

        // Remember, create doesn't show the dialog
        final AlertDialog dialog = helpBuilder.create();
        dialog.show();
        int color = Color.RED;
        // title
        int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
        TextView tv = (TextView) dialog.findViewById(textViewId);
        if (tv != null) {
            tv.setTextColor(color);
        }
        // divider
        int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = dialog.findViewById(dividerId);
        if (divider != null) {
            divider.setBackgroundColor(color);
        }
    }
}
