package com.yeleman.mmtkrelay;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerAPIService extends IntentService {
    private Context context;
    private Session session;

    private static final String ACTION_CONNEXION_CHECK = "com.yeleman.mmtkrelay.action.CONNEXION_CHECK";
    private static final String ACTION_FCM_TOKEN_UPDATE = "com.yeleman.mmtkrelay.action.FCM_TOKEN_UPDATE";

    public ServerAPIService() {
        super("ServerAPIService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        session = new Session(context);
    }

    public static void startConnexionCheck(Context context) {
        Intent intent = new Intent(context, ServerAPIService.class);
        intent.setAction(ACTION_CONNEXION_CHECK);
        context.startService(intent);
    }

    public static void startFCMTokenUpdate(Context context) {
        Intent intent = new Intent(context, ServerAPIService.class);
        intent.setAction(ACTION_FCM_TOKEN_UPDATE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CONNEXION_CHECK.equals(action)) {
                handleConnexionCheck();
            } else if (ACTION_FCM_TOKEN_UPDATE.equals(action)) {
                handleFCMTokenUpdate();
            }
        }
    }

    String getAbsoluteUrl(String relativeUrl) { return (session.getServerUrl() == null) ? null : session.getServerUrl() + relativeUrl; }

    private void updateServerStatus(Boolean connected) {
        Log.w(Constants.TAG, "updateServerStatus: " + connected);
        session.setServerConnected(connected);
        session.saveToPreferences();
        Utils.triggerUIRefresh(context, "refreshServerStatus");
    }

    private void handleConnexionCheck() {
        Log.d(Constants.TAG, "handleConnexionCheck");
        updateServerStatus(null);
        Response response = Requests.getResponse(getAbsoluteUrl("/status"));
        Boolean connected;
        if (response != null && response.succeeded()) {
            String serverID = null;
            try {
                serverID = response.getJSON().getString("serverID");
            } catch (JSONException ex) { Log.e(Constants.TAG, ex.toString()); }
            session.setServerId(serverID);
            connected = true;
            // ensure our token is up-to-date and transmitted
            FirebaseInstanceId.getInstance().getToken();
        } else {
            connected = false;
        }
        updateServerStatus(connected);
    }

    private void handleFCMTokenUpdate() {
        Log.d(Constants.TAG, "handleFCMTokenUpdate");
        if (session.getFCMToken() == null) {
            // request FCM Registration and exit
            return;
        }

        Log.d(Constants.TAG, "updateFCMToken");
        session.setFCMTokenTransmitted(null);
        session.saveToPreferences();
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("token", session.getFCMToken());
            jsonParams.put("MSISDN", session.getMsisdn());
        } catch (JSONException ex) {}
        Response response = Requests.postJSON(getAbsoluteUrl("/token"), jsonParams);
        Boolean succeeded = (response != null && response.succeeded());
        session.setFCMTokenTransmitted(succeeded);
        session.saveToPreferences();
    }


}
