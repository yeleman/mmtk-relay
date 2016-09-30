package com.yeleman.mmtkrelay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.widget.Toast;

public class OutgoingSMSStatusReceiver extends BroadcastReceiver {
    public OutgoingSMSStatusReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();

        // int numParts = intent.getIntExtra(Constants.STATUS_EXTRA_NUM_PARTS, 0);

        // we don't handle individual parts and mark status for first one only
        int index = intent.getIntExtra(Constants.STATUS_EXTRA_INDEX, 0);
        if (index != 0) {
            return;
        }

        // OutgoingMessage sms = app.outbox.getMessage(uri);
        Operation operation = Operation.getFromUri(uri);
        if (operation == null) {
            return;
        }

        int resultCode = getResultCode();
        String message;
        if (resultCode == Activity.RESULT_OK)
        {
            // update operation for success
            message = uri.toString() + " sent";
            operation.markSuccessful();
        }
        else
        {
            message = uri.toString() + " " + getErrorMessage(resultCode);
            operation.markFailed(String.valueOf(resultCode) + ": "+getErrorMessage(resultCode));
        }
        operation.markHandled();
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        Utils.triggerUIRefresh(context, "refreshDashboard");
    }

    public String getErrorMessage(int resultCode)
    {
        switch (resultCode) {
            case Activity.RESULT_OK:
                return "sent OK";
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "generic failure";
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return "radio off";
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return "no service";
            case SmsManager.RESULT_ERROR_NULL_PDU:
                return "null PDU";
            default:
                return "unknown error";
        }

    }
}
