package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class MMTKFirebaseInstanceIDService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(Constants.TAG, "Refreshed token: " + refreshedToken);

        // broadcast new token locally
        Intent intent = new Intent(Constants.FCM_TOKEN_RECEIVED_FILTER);
        intent.putExtra("token", refreshedToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}