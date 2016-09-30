package com.yeleman.mmtkrelay;


import android.graphics.Color;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {

    public static final String TAG = "LOG-MMTK";
    public static final DateFormat OAPI_HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z", Locale.ENGLISH);
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM à kk'h'mm", Locale.FRANCE);
    public static final int CONNECTED_COLOR = Color.GREEN;
    public static final int NOT_CONNECTED_COLOR = Color.RED;
    public static final int PENDING_COLOR = Color.DKGRAY;
    public static final String BLANK = "-";

    public static final String SETTINGS_CHANGED_FILTER = "com.yeleman.mmtkrelay.SETTINGS_CHANGED";
    public static final String FCM_TOKEN_RECEIVED_FILTER = "com.yeleman.mmtkrelay.FCM_TOKEN_RECEIVED";

    // sensitive settings keys
    public static final String SETTINGS_SERVER_URL = "server_url";
    public static final String SETTINGS_FCM_TOKEN = "fcm_token";
    public static final String SETTINGS_SERVER_ID = "server_id";
    public static final String SETTINGS_BALANCE_POLLING_INTERVAL = "balance_polling_interval";
    public static final String SETTINGS_SERVER_POLLING_INTERVAL = "server_polling_interval";

    public static int getConnectionColor(Boolean connected) {
        if (connected == null) {
            return Constants.PENDING_COLOR;
        }
        return connected ? Constants.CONNECTED_COLOR : Constants.NOT_CONNECTED_COLOR;
    }

}
