package com.yeleman.mmtkrelay;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.ArrayMap;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class IncomingCommandProcessor extends IntentService {
    public static final String ACTION_PROCESS_COMMAND = "com.yeleman.mmtkrelay.action.PROCESS_COMMAND";
    public static final String ACTION_PROCESS_MESSAGE = "com.yeleman.mmtkrelay.action.PROCESS_MESSAGE";

    private static final String EXTRA_TIMESTAMP = "com.yeleman.mmtkrelay.extra.TIMESTAMP";
    private static final String EXTRA_DATA = "com.yeleman.mmtkrelay.extra.DATA";
    private static final String EXTRA_MESSAGE = "com.yeleman.mmtkrelay.extra.MESSAGE";


    private Context context;
    private Session session;

    public IncomingCommandProcessor() {
        super("IncomingCommandProcessor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        session = new Session(context);
    }

    public static void startWithFCM(Context context, RemoteMessage remoteMessage, Long timestamp) {
        Log.d(Constants.TAG, "startWithFCM");
        Intent intent = new Intent(context, IncomingCommandProcessor.class);
        intent.setAction(ACTION_PROCESS_MESSAGE);
        intent.putExtra(EXTRA_MESSAGE, remoteMessage);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp);
        Log.d(Constants.TAG, "ready to start Service");
        context.startService(intent);
    }

    public static void startProcessing(Context context, JSONObject data, Long timestamp) {
        Intent intent = new Intent(context, IncomingCommandProcessor.class);
        intent.setAction(ACTION_PROCESS_COMMAND);
        intent.putExtra(EXTRA_DATA, data.toString());
        intent.putExtra(EXTRA_TIMESTAMP, timestamp);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(Constants.TAG, "onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_MESSAGE.equals(action)) {
                Log.d(Constants.TAG, ACTION_PROCESS_MESSAGE);
                RemoteMessage remoteMessage = intent.getParcelableExtra(EXTRA_MESSAGE);
                Log.d(Constants.TAG, remoteMessage.toString());
                final Long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);
                handleMessage(remoteMessage, timestamp);
            } else if (ACTION_PROCESS_COMMAND.equals(action)) {
                final Long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);
                final String jsData = intent.getStringExtra(EXTRA_DATA);
                handleCommand(jsData, timestamp);
            }
        }
    }

    private void handleMessage(RemoteMessage remoteMessage, Long timestamp) {
        Log.d(Constants.TAG, "handleMessage");
        if (!remoteMessage.getFrom().equals(session.getServerId())) {
            Log.e(Constants.TAG, "Ignoring incoming FCM as from incorrect sender ("+ remoteMessage.getFrom() +")");
            return;
        }
        Long sentOn = remoteMessage.getSentTime();
        Map<String, String> data = remoteMessage.getData();

        // retrieve data from message
        String fcm_action = data.get("action");
        String fcm_identity = data.get("identity");
        String fcm_identities = data.get("identities");
        String fcm_text = data.get("text");
        String fcm_amount = data.get("amount");

        // build JSONObject with proper corresponding format
        JSONObject jsData = new JSONObject();
        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        try { action.put("action", fcm_action); } catch (JSONException ex) {}

        if (fcm_identity != null) {
            try { action.put("identity", fcm_identity); } catch (JSONException ex) {}
        }
        if (fcm_identities != null) {
            try { action.put("identities", fcm_identities); } catch (JSONException ex) {}
        }
        if (fcm_text != null) {
            try { action.put("text", fcm_text); } catch (JSONException ex) {}
        }
        if (fcm_amount != null) {
            try { action.put("amount", fcm_amount); } catch (JSONException ex) {}
        }
        actions.put(action);
        try { jsData.put("actions", actions); } catch (JSONException ex) {}
        try { jsData.put("created_on", sentOn); } catch (JSONException ex) {}

        handleCommand(jsData.toString(), timestamp);
    }

    private void handleCommand(String jsData, Long timestamp) {
        Log.d(Constants.TAG, "handleCommand");
        JSONObject data = null;
        try {
            data = new JSONObject(jsData);
        } catch (JSONException ex) {
            Log.e(Constants.TAG, "Unable to parse JSON data. exiting. " + ex.toString());
            return;
        }

        // reception date is passed separately
        Date receivedOn;
        receivedOn = (timestamp != null && timestamp > 0) ? new Date(timestamp) : new Date();

        // we might have a createdOn (on the server) date
        Long created_on = null;
        Date createdOn;
        try { created_on = data.getLong("created_on"); } catch (JSONException ex) {}
        createdOn = (created_on != null && created_on > 0) ? new Date(created_on) : null;

        // multiple actions can be sent at once
        JSONArray actions = new JSONArray();
        try { actions = data.getJSONArray("actions"); } catch (JSONException ex) {}

        for (int i = 0; i < actions.length(); i++) {
            JSONObject actionObj;
            try {
                actionObj = actions.getJSONObject(i);
            } catch (JSONException ex) {
                break;
            }

            // action respects the following format
            // {"action": "outgoing_transfer_multi", "identities": ["22373120896", "22376333005"], "amount": 1000}
            // {"action": "outgoing_text", "identity": "22373120896", "text": "bonjour"}
            JSONArray identitiesObj = null;
            ArrayList<String> identities = new ArrayList<>();
            String action = null;
            String text = null;
            String identity = null;
            Double amount = null;
            try { action = actionObj.getString("action"); } catch (JSONException ex) { break; }
            try { text = actionObj.getString("text"); } catch (JSONException ex) {}
            try { identity = actionObj.getString("identity"); } catch (JSONException ex) {}
            try { amount = actionObj.getDouble("amount"); } catch (JSONException ex) {}
            try { identitiesObj = actionObj.getJSONArray("identities"); } catch (JSONException ex) {}
            if (identitiesObj != null) {
                for (int j = 0; j < identitiesObj.length(); j++) {
                    String ident = null;
                    try { ident = identitiesObj.getString(j); } catch (JSONException ex) {}
                    if (ident != null) {
                        identities.add(ident);
                    }
                }
            }
            // handle action
            switch(action) {
                case "outgoing_text":
                    Log.d(Constants.TAG, "outgoing_sms case");
                    handleOutgoingText(identity, text);
                    break;
                case "outgoing_text_multi":
                    handleOutgoingText(identities, text);
                    break;
            }

        }

    }
    void handleOutgoingText(String identity, String text) {
        Log.d(Constants.TAG, "handleOutgoingText");
        OutgoingSMSService.startSingleSMS(context, identity, text);
        Utils.triggerUIRefresh(context, "refreshDashboard");
    }
    void handleOutgoingText(ArrayList<String> identities, String text) {
        // OutgoingSMSService.startSingleSMS();
    }
}
