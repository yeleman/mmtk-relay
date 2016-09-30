package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.yeleman.mmtkrelay.Constants;


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

        Context context = getApplicationContext();
        Session session = new Session(context);
        session.setFCMToken(refreshedToken);
        session.setFCMTokenTransmitted(false);
        session.saveToPreferences();

        ServerAPIService.startFCMTokenUpdate(context);
    }
}