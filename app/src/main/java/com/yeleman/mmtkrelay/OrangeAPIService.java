package com.yeleman.mmtkrelay;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import org.json.JSONException;
import java.util.Date;

public class OrangeAPIService extends IntentService {
    private static final String BASE_URL = "http://omapp.orangemali.com/OMMali";

    private static final String ACTION_MSISDN = "com.yeleman.mmtkrelay.action.MSISDN";
    private static final String ACTION_USER = "com.yeleman.mmtkrelay.action.USER";
    private static final String ACTION_CHECK_NETWORK = "com.yeleman.mmtkrelay.action.CHECK_NETWORK";
    private Context context;
    private Session session;

    public OrangeAPIService() {
        super("OrangeAPIService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        session = new Session(context);
    }

    public static void startMsisdn(Context context) {
        Intent intent = new Intent(context, OrangeAPIService.class);
        intent.setAction(ACTION_MSISDN);
        context.startService(intent);
    }

    public static void startUser(Context context) {
        Intent intent = new Intent(context, OrangeAPIService.class);
        intent.setAction(ACTION_USER);
        context.startService(intent);
    }

    public static void startConnexionCheck(Context context) {
        Intent intent = new Intent(context, OrangeAPIService.class);
        intent.setAction(ACTION_CHECK_NETWORK);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_MSISDN.equals(action)) {
                handleMsisdn();
            } else if (ACTION_USER.equals(action)) {
                handleUser();
            } else if (ACTION_CHECK_NETWORK.equals(action)) {
                handleConnexionCheck();
            }
        }
    }

    static String getAbsoluteUrl(String relativeUrl) { return BASE_URL + relativeUrl; }

    void updateConnexionStatus(Boolean connected) {
        session.setOrangeConnected(connected);
        session.saveToPreferences();
        Utils.triggerUIRefresh(this, "refreshNetworkStatus");
    }

    private void handleConnexionCheck() {
        updateConnexionStatus(null);
        if (!Networks.isConnectedToOrangeMobile(this)) {
            updateConnexionStatus(false);
        } else {
            // connected to Orange network
            updateConnexionStatus(true);

            // update session
            OrangeAPIService.startMsisdn(this);
            OrangeAPIService.startUser(this);
        }
    }

    void handleUser() {
        Log.e(Constants.TAG, "handleUser");
        Response response = Requests.getResponse(getAbsoluteUrl("/tbw/init/api/user/status/country/ml"));
        Log.e(Constants.TAG, response.getBody());
        OMUser user = new Gson().fromJson(response.getBody(), OMUser.class);
        Log.e(Constants.TAG, "user.isValid():"+user.isValid());
        Log.w(Constants.TAG, response.getHeaderField("Date"));
        Log.w(Constants.TAG, response.getHeaderFieldDate("Date").toString());
        Date updated_on = response.getHeaderFieldDate("Date");
        user.setUpdatedOn(updated_on);

        session.setUser(user);
        session.getUser().saveToPreferences(context);
        Utils.triggerUIRefresh(context, "refreshUser", "refreshBalance");
    }

    private void handleMsisdn() {
        Log.e(Constants.TAG, "handleMsisdn");
        Response response = Requests.getResponse(getAbsoluteUrl("/version.php"));
        Log.e(Constants.TAG, response.getBody());
        String msisdn = null;
        try { msisdn = response.getJSON().getString("client"); } catch (JSONException|NullPointerException ex) { Log.e(Constants.TAG, ex.toString()); }
        Log.e(Constants.TAG, "---------" + msisdn);
        session.setMsisdn(msisdn);
        session.saveToPreferences();
        Utils.triggerUIRefresh(context, "refreshMsisdn");

        // ensure our token is up-to-date and transmitted
        FirebaseInstanceId.getInstance().getToken();
    }
}
