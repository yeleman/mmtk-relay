package com.yeleman.mmtkrelay;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {

    public static final String TAG = "LOG-MMTK";
    public static final String WAKELOCK_TAG = "MMTK-LOCK";
    public static final DateFormat OAPI_HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z", Locale.ENGLISH);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM à kk'h'mm", Locale.FRANCE);
    public static final DateFormat FILE_DATE_FORMAT = new SimpleDateFormat("ddMMyyyykkmm", Locale.ENGLISH);
    public static final int CONNECTED_COLOR = Color.GREEN;
    public static final int NOT_CONNECTED_COLOR = Color.RED;
    public static final int PENDING_COLOR = Color.DKGRAY;
    public static final String BLANK = "-";

    public static final String SETTINGS_CHANGED_FILTER = "com.yeleman.mmtkrelay.SETTINGS_CHANGED";
    public static final String FCM_TOKEN_RECEIVED_FILTER = "com.yeleman.mmtkrelay.FCM_TOKEN_RECEIVED";
    public static final String FCM_MESSAGE_FILTER = "com.yeleman.mmtkrelay.FCM_MESSAGE";
    public static final String UI_TAMPERED_FILTER = "com.yeleman.mmtkrelay.UI_TAMPERED";

    // sensitive settings keys
    public static final String SETTINGS_SERVER_URL = "server_url";
    public static final String SETTINGS_FCM_TOKEN = "fcm_token";
    public static final String SETTINGS_SERVER_ID = "server_id";
    public static final String SETTINGS_BALANCE_POLLING_INTERVAL = "balance_polling_interval";
    public static final String SETTINGS_SERVER_POLLING_INTERVAL = "server_polling_interval";

    public static final String DASHBOARD_ITEMS_LIMIT = "50";
    public static final int DASHBOARD_TEXT_PREVIEW_LIMIT = 20;

    public static final Uri CONTENT_URI = Uri.parse("content://com.yeleman.mmtkrelay");
    public static final Uri INCOMING_URI = Uri.withAppendedPath(CONTENT_URI, Operation.INCOMING_TEXT);
    public static final Uri OUTGOING_URI = Uri.withAppendedPath(CONTENT_URI, Operation.OUTGOING_TEXT);

    // OUTGOING SMS
    public static final String MESSAGE_STATUS_INTENT = "com.yeleman.mmtkrelay.MESSAGE_STATUS";
    public static final String MESSAGE_DELIVERY_INTENT = "com.yeleman.mmtkrelay.MESSAGE_DELIVERY";

    public static final String STATUS_EXTRA_INDEX = "status";
    public static final String STATUS_EXTRA_NUM_PARTS = "num_parts";
    public static final String STATUS_EXTRA_SERVER_ID = "internal_id";

    public static String getDatabaseName(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("DATABASE");
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }

    public static int getConnectionColor(Boolean connected) {
        if (connected == null) {
            return Constants.PENDING_COLOR;
        }
        return connected ? Constants.CONNECTED_COLOR : Constants.NOT_CONNECTED_COLOR;
    }

    public static int getStatusColor(Boolean success) {
        if (success == null) {
            return Constants.PENDING_COLOR;
        }
        return success ? Constants.CONNECTED_COLOR : Constants.NOT_CONNECTED_COLOR;
    }

}
