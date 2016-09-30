package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class SIMOrConnectivityChangedReceiver extends BroadcastReceiver {

    private MainActivity activity;
    private boolean initial_sim_state_received = false;
    private boolean initial_network_state_received = false;

    public SIMOrConnectivityChangedReceiver(MainActivity activity) {
        this.activity = activity;
    }

    private boolean isInitialized() {
        return (initial_sim_state_received && initial_network_state_received);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(Constants.TAG, "SIM card or connectivity changed");
        if (isInitialized()) {
            activity.updateConnexionsStatus();
        } else {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                if (!initial_sim_state_received) {
                    String value = (String) bundle.get("phoneName");
                    if (value != null) {
                        initial_sim_state_received = true;
                    }
                }
                if (!initial_network_state_received) {
                    NetworkInfo value = (NetworkInfo) bundle.get("networkInfo");
                    if (value != null) {
                        initial_network_state_received = true;
                    }
                }
            }
        }
    }
}