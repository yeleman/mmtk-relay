package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

class Session {

    private MMTKBaseActivity activity;

    // from prefs
    private String server_url = null;
    private boolean server_polling = false;
    private int server_polling_interval = 600; // 10mn
    private String orange_pin = null;
    private boolean balance_polling = false;
    private int balance_polling_interval = 600; // 10mn
    private String fcm_token = null;
    private Boolean fcm_token_transmitted = false;

    // OMAPI
    private OMUser user = new OMUser();
    private String msisdn = null;
    private String server_id = null;

    // states
    private Boolean orange_connected = null;
    private Boolean server_connected = null;


    Session(MMTKBaseActivity activity) {
        Log.d(Constants.TAG, "init session");
        this.activity = activity;
        this.loadFromPreferences(this.activity);
    }

    public void reloadPreferences() { this.loadFromPreferences(this.activity); }

    public void loadFromPreferences(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        setServerUrl(sharedPref.getString("server_url", null));
        setServerPolling(sharedPref.getBoolean("server_polling", false));
        setServerPollingInterval(sharedPref.getString("server_polling_interval", "600"));

        setOrangePin(sharedPref.getString("orange_pin", null));
        setBalancePolling(sharedPref.getBoolean("balance_polling", false));
        setBalancePollingInterval(sharedPref.getString("balance_polling_interval", "600"));

        setFCMToken(sharedPref.getString("fcm_token", null));

        Log.d(Constants.TAG, String.format("settings loaded: %d", getBalancePollingInterval()));
    }

    String getMsisdn() { return msisdn; }
    void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    String getServerId() { return server_id; }
    void setServerId(String server_id) { this.server_id = server_id; }

    String getFCMToken() { return fcm_token; }
    void setFCMToken(String fcm_token) { this.fcm_token = fcm_token; }

    Boolean getFCMTokenTransmitted() { return fcm_token_transmitted; }
    void setFCMTokenTransmitted(Boolean fcm_token_transmitted) { this.fcm_token_transmitted = fcm_token_transmitted; }

    Boolean getOrangeConnected() { return orange_connected; }
    void setOrangeConnected(Boolean orange_connected) { this.orange_connected = orange_connected; }

    Boolean getServerConnected() { return server_connected; }
    void setServerConnected(Boolean server_connected) { this.server_connected = server_connected; }

    OMUser getUser() { return user; }
    void setUser(OMUser user) { this.user = user; }

    String getServerUrl() { return server_url; }
    void setServerUrl(String server_url) { this.server_url = server_url; }

    boolean getServerPolling() { return server_polling; }
    void setServerPolling(boolean server_polling) { this.server_polling = server_polling; }

    int getServerPollingInterval() { return server_polling_interval; }
    void setServerPollingInterval(String server_polling_interval) { this.server_polling_interval = Integer.parseInt(server_polling_interval); }
    void setServerPollingInterval(int server_polling_interval) { this.server_polling_interval = server_polling_interval; }

    String getOragePin() { return orange_pin; }
    void setOrangePin(String orange_pin) { this.orange_pin = orange_pin; }

    boolean getBalancePolling() { return balance_polling; }
    void setBalancePolling(boolean balance_polling) { this.balance_polling = balance_polling; }

    int getBalancePollingInterval() { return balance_polling_interval; }
    void setBalancePollingInterval(String balance_polling_interval) { this.balance_polling_interval = Integer.parseInt(balance_polling_interval); }
    void setBalancePollingInterval(int balance_polling_interval) { this.balance_polling_interval = balance_polling_interval; }

    String getFormattedMsisdn() {
        String formatted = getMsisdn().replaceFirst("223", "");
        return String.format("%s %s %s %s",
                             formatted.subSequence(0, 2), formatted.subSequence(2, 4),
                             formatted.subSequence(4, 6), formatted.subSequence(6, 8));
    }
}
