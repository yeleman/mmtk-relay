package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class SIMCardChanged extends BroadcastReceiver {

    private boolean initial_sim_state_received = false;
//    private boolean initial_network_state_received = false;

    private boolean isInitialized() {
        return initial_sim_state_received;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(Constants.TAG, "SIM card changed");
        if (isInitialized()) {
            Utils.updateConnexionsStatus(context);
        } else {
            initial_sim_state_received = true;
        }
    }
}