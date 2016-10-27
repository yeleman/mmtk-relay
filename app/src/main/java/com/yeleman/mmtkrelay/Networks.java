package com.yeleman.mmtkrelay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

class Networks {

    static final String WIFI = "WiFi";
    static final String MOBILE = "mobile";
    static final String NONE = "none";

    public static boolean isMobileActive(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "mobile_data", 1) == 1;
    }

    static String getMainConnexionType(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return Networks.WIFI;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return Networks.MOBILE;
            }
        } else {
            return Networks.NONE;
        }
        return Networks.NONE;
    }

    static boolean isConnected(final Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnectivityManager.getActiveNetworkInfo() != null
                && mConnectivityManager.getActiveNetworkInfo().isAvailable()
                && mConnectivityManager.getActiveNetworkInfo().isConnected()) {
            Log.d(Constants.TAG, "connected");
            return true;
        } else {
            Log.d(Constants.TAG, "not connected");
            return false;
        }
    }

    static String getSIMOperator(final Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Log.d(Constants.TAG, telephonyManager.getSimCountryIso());
        Log.d(Constants.TAG, telephonyManager.getNetworkOperatorName());
        Log.d(Constants.TAG, telephonyManager.getSimOperator());
        return telephonyManager.getSimOperator();
    }

    static boolean isOnOrange(final Context context) {
        return Networks.getSIMOperator(context).equals(context.getResources().getString(R.string.orange_HNI));
    }

    static boolean isOnMobile(final Context context) {
        return Networks.getMainConnexionType(context).equals(Networks.MOBILE);
    }

    static boolean isOnOrangeMobile(final Context context) {
        return (Networks.isOnOrange(context) && Networks.isOnMobile(context));
    }

    static boolean isConnectedToOrangeMobile(final Context context) {
        return (Networks.isOnOrangeMobile(context) && Networks.isConnected(context));
    }
}
