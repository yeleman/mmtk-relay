package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

class Session {

    private final Context context;

    // from prefs
    private String server_url = null;
    private boolean server_polling = false;
    private int server_polling_interval = 600; // 10mn
    private String orange_pin = null;
    private boolean balance_polling = false;
    private int balance_polling_interval = 600; // 10mn
    private String fcm_token = null;
    private Boolean fcm_token_transmitted = false;
    private Boolean sms_forwarding = false;
    private Boolean call_forwarding = false;

    // OMAPI
    private final String orange_sender = "22373120896"; //"OrangeMoney";
    private OMUser user = null;
    private String msisdn = null;
    private String server_id = null;

    // states
    private Boolean orange_connected = null;
    private Boolean server_connected = null;


    Session(Context context) {
        this.context = context;
        this.loadFromPreferences(this.context, true);
    }

    public void reloadPreferences(boolean includeUser) { this.loadFromPreferences(this.context, includeUser); }
    public void reloadPreferences() { this.loadFromPreferences(this.context, false); }

    public void loadFromPreferences(Context context) {
        loadFromPreferences(context, false);
    }
    public void loadFromPreferences(Context context, boolean includeUser) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        setServerUrl(sharedPref.getString("server_url", null));
        setServerPolling(sharedPref.getBoolean("server_polling", false));
        setServerPollingInterval(sharedPref.getString("server_polling_interval", "600"));

        setMsisdn(sharedPref.getString("msisdn", null));
        setServerId(sharedPref.getString("server_id", null));

        setSMSForwarding(sharedPref.getBoolean("sms_forwarding", false));
        setCallForwarding(sharedPref.getBoolean("call_forwarding", false));

        setOrangePin(sharedPref.getString("orange_pin", null));
        setBalancePolling(sharedPref.getBoolean("balance_polling", false));
        setBalancePollingInterval(sharedPref.getString("balance_polling_interval", "600"));

        setFCMToken(sharedPref.getString("fcm_token", null));
        setFCMTokenTransmitted(sharedPref.getBoolean("fcm_token_transmitted", false));

        setServerConnected(sharedPref.getBoolean("server_connected", false));
        setOrangeConnected(sharedPref.getBoolean("orange_connected", false));

        if (includeUser) {
            setUser(OMUser.fromPreferences(context));
        }
    }

    void saveToPreferences() { saveToPreferences(false); }
    void saveToPreferences(boolean includeUser) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("server_url", getServerUrl());

        prefEditor.putBoolean("server_polling", getServerPolling());
        prefEditor.putString("server_polling_interval", String.valueOf(getServerPollingInterval()));

        prefEditor.putString("msisdn", getMsisdn());
        prefEditor.putString("server_id", getServerId());

        prefEditor.putBoolean("sms_forwarding", getSMSForwarding());
        prefEditor.putBoolean("call_forwarding", getCallForwarding());

        prefEditor.putString("orange_pin", getOragePin());

        prefEditor.putBoolean("balance_polling", getServerPolling());
        prefEditor.putString("balance_polling_interval", String.valueOf(getServerPollingInterval()));

        prefEditor.putString("fcm_token", getFCMToken());

        if (getFCMTokenTransmitted() == null) {
            prefEditor.putBoolean("fcm_token_transmitted", false);
        } else {
            prefEditor.putBoolean("fcm_token_transmitted", getFCMTokenTransmitted());
        }

        if (getServerConnected() == null) {
            prefEditor.putBoolean("server_connected", false);
        } else {
            prefEditor.putBoolean("server_connected", getServerConnected());
        }
        if (getOrangeConnected() == null) {
            prefEditor.putBoolean("orange_connected", false);
        } else {
            prefEditor.putBoolean("orange_connected", getOrangeConnected());
        }

        prefEditor.apply();

        if (includeUser) {
            getUser().saveToPreferences(context);
        }
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

    String getOrangeSender() { return orange_sender; }

    OMUser getUser() { return user; }
    void setUser(OMUser user) { this.user = user; }

    String getServerUrl() { return server_url; }
    void setServerUrl(String server_url) { this.server_url = server_url; }

    Boolean getServerPolling() { return server_polling; }
    void setServerPolling(boolean server_polling) { this.server_polling = server_polling; }

    int getServerPollingInterval() { return server_polling_interval; }
    void setServerPollingInterval(String server_polling_interval) { this.server_polling_interval = Integer.parseInt(server_polling_interval); }
    void setServerPollingInterval(int server_polling_interval) { this.server_polling_interval = server_polling_interval; }

    Boolean getSMSForwarding() { return sms_forwarding; }
    void setSMSForwarding(Boolean sms_forwarding) { this.sms_forwarding = sms_forwarding; }

    Boolean getCallForwarding() { return call_forwarding; }
    void setCallForwarding(Boolean call_forwarding) { this.call_forwarding = call_forwarding; }

    String getOragePin() { return orange_pin; }
    void setOrangePin(String orange_pin) { this.orange_pin = orange_pin; }

    boolean getBalancePolling() { return balance_polling; }
    void setBalancePolling(boolean balance_polling) { this.balance_polling = balance_polling; }

    int getBalancePollingInterval() { return balance_polling_interval; }
    void setBalancePollingInterval(String balance_polling_interval) { this.balance_polling_interval = Integer.parseInt(balance_polling_interval); }
    void setBalancePollingInterval(int balance_polling_interval) { this.balance_polling_interval = balance_polling_interval; }

    String getFormattedMsisdn() { return TextUtils.msisdnFormat(getMsisdn()); }
}
