package com.yeleman.mmtkrelay;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

public class OutgoingSMSService extends IntentService {

    private static final String ACTION_SEND_OUTGOING_SMS = "com.yeleman.mmtkrelay.action.SEND_OUTGOING_SMS";

    private static final String EXTRA_IDENTITY = "com.yeleman.mmtkrelay.extra.IDENTITY";
    private static final String EXTRA_TEXT = "com.yeleman.mmtkrelay.extra.TEXT";

    private Context context;
    private Session session;

    public OutgoingSMSService() {
        super("OutgoingSMSService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        session = new Session(context);
    }

    public static void startSingleSMS(Context context, String identity, String text) {
        Log.d(Constants.TAG, "startSingleSMS");
        Intent intent = new Intent(context, OutgoingSMSService.class);
        intent.setAction(ACTION_SEND_OUTGOING_SMS);
        intent.putExtra(EXTRA_IDENTITY, identity);
        intent.putExtra(EXTRA_TEXT, text);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEND_OUTGOING_SMS.equals(action)) {
                final String identity = intent.getStringExtra(EXTRA_IDENTITY);
                final String text = intent.getStringExtra(EXTRA_TEXT);
                handleOutgoingSMS(identity, text, null);
            }
        }
    }

    private void handleOutgoingSMS(String identity, String text, String internalID) {
        Log.d(Constants.TAG, "handleOutgoingSMS");
        if (internalID == null) {
            Long opId = Operation.storeOutgoingSMSText(session.getMsisdn(), new Date(), identity, text);
            internalID = String.valueOf(opId);
        }

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> bodyParts = sms.divideMessage(text);
        Uri uri = Uri.withAppendedPath(Constants.OUTGOING_URI, internalID);
        SmsManager smgr = SmsManager.getDefault();

        Log.d(Constants.TAG, uri.toString());

        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveryIntents;

        deliveryIntents = new ArrayList<>();

        int numParts = bodyParts.size();

        for (int i = 0; i < numParts; i++)
        {
            Intent statusIntent = new Intent(Constants.MESSAGE_STATUS_INTENT, uri);
            statusIntent.putExtra(Constants.STATUS_EXTRA_INDEX, i);
            statusIntent.putExtra(Constants.STATUS_EXTRA_NUM_PARTS, numParts);
            sentIntents.add(PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_ONE_SHOT));

            Intent deliveryIntent = new Intent(Constants.MESSAGE_DELIVERY_INTENT, uri);
            deliveryIntent.putExtra(Constants.STATUS_EXTRA_INDEX, i);
            deliveryIntent.putExtra(Constants.STATUS_EXTRA_NUM_PARTS, numParts);
            deliveryIntent.putExtra(Constants.STATUS_EXTRA_SERVER_ID, internalID);
            deliveryIntents.add(PendingIntent.getBroadcast(context, 0, deliveryIntent, PendingIntent.FLAG_ONE_SHOT));
        }

        smgr.sendMultipartTextMessage(identity, null, bodyParts, sentIntents, deliveryIntents);
    }
}
