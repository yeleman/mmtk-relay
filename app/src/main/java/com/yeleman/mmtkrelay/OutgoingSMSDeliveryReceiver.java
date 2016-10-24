package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class OutgoingSMSDeliveryReceiver extends BroadcastReceiver {
    public OutgoingSMSDeliveryReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        Operation operation = Operation.getFromUri(uri);
        operation.markDelivered();

        Log.d(Constants.TAG, uri.toString() + " delivered");
        Toast.makeText(context, uri.toString() + " delivered", Toast.LENGTH_SHORT).show();

        Utils.triggerUIRefresh(context, "refreshDashboard");
    }
}
