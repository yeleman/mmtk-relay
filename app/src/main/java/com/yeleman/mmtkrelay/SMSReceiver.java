package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
    public SMSReceiver() {
        Log.d(Constants.TAG, "Created SMSReceiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(Constants.TAG, "onReceive SMS");
        Bundle bundle = intent.getExtras();
        SmsMessage[] parts = null;
        if (bundle != null) {
            // Retrieve the SMS Messages received
            parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            // For every SMS message received
            String body = "";
            String from = null;
            Long timestamp = 0L;
            for (SmsMessage part: parts) {
                // message timestamp from first part
                if (timestamp == 0L) {
                    timestamp = part.getTimestampMillis();
                }
                // message sender from first part. fail on different sender
                if (from == null) {
                    from = part.getOriginatingAddress();
                } else if (!from.equals(part.getOriginatingAddress())) {
                    Log.e(Constants.TAG, "Received multipart SMS with multiple senders alltogether");
                }
                // message body is contaneation of all parts
                body += part.getMessageBody();
            }
            IncomingTextProcessor.startProcessing(context, from, timestamp, body);
            // request UI reload?
        }
    }
}
