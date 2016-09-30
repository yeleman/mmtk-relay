package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Hashtable;


public class MainActivity extends MMTKBaseActivity {

    private MMTKServerClient client;
    private static final String DASHBOARD = "dashboard";
    private static final String FAILED_ITEMS = "failed_items";
    private static final String ABOUT = "about";
    private String CURRENT_PAGE;
    private static final Hashtable<String, Integer> pages;
    static
    {
        pages = new Hashtable<>();
        pages.put(DASHBOARD, R.id.content_dashboard);
        pages.put(FAILED_ITEMS, R.id.content_failed_items);
        pages.put(ABOUT, R.id.content_about);
    }

    private Toolbar toolbar;
    // main header
    private TextView balance_textview;
    private TextView balance_updated_on_textview;
    private TextView msisdn_textview;
    private TextView first_name_textview;
    private TextView last_name_textview;
    // servers
    private TextView omapi_status_textview;
    private TextView server_status_textview;

    private BroadcastReceiver FCMTokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String token = intent.getStringExtra("token");
            Log.d(Constants.TAG, "FCMTokenReceiver: " + token);

            // save token to shared Prefs
            updateSharedPreferences(Constants.SETTINGS_FCM_TOKEN, token);

            // update session
            session.setFCMToken(token);
            session.setFCMTokenTransmitted(false);

            // transmit token to server
            client.updateFCMToken();
        }
    };

    private BroadcastReceiver SettingsUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "SettingsUpdatedReceiver onReceived");
            String key = intent.getStringExtra("key");
            if (key != null && key.equals(Constants.SETTINGS_SERVER_URL)) {
                session.reloadPreferences();
                updateConnexionsStatus();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prevent device from going to sleep
        Utils.AcquireWakeLock(this);

        // prepare server connection
        client = new MMTKServerClient(this);

        // build UI with all elements (pages hidden)
        setupUI();

        // register receivers
        setupReceivers();

        // check for network requirements
        updateConnexionsStatus();

        // display dashboard
        displayDashboard();
    }

    protected void setupUI() {
        setContentView(R.layout.activity_dashboard);

        // load references to moving elements
        balance_textview = (TextView) findViewById(R.id.balance_textview);
        balance_updated_on_textview = (TextView) findViewById(R.id.balance_updated_on_textview);
        msisdn_textview = (TextView) findViewById(R.id.msisdn_textview);
        first_name_textview = (TextView) findViewById(R.id.first_name_textview);
        last_name_textview = (TextView) findViewById(R.id.last_name_textview);
        omapi_status_textview = (TextView) findViewById(R.id.omapi_status_textview);
        server_status_textview = (TextView) findViewById(R.id.server_status_textview);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void updateSharedPreferences(String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(key, value);
        prefEditor.apply();
    }

    public void setupFCMRegistration() { FirebaseInstanceId.getInstance().getToken(); }

    private void setupReceivers() {

        // Local receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
                FCMTokenReceiver, new IntentFilter(Constants.FCM_TOKEN_RECEIVED_FILTER));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                SettingsUpdatedReceiver, new IntentFilter(Constants.SETTINGS_CHANGED_FILTER));

        // SIMOrConnectivityChangedReceiver
        SIMOrConnectivityChangedReceiver conn_receiver = new SIMOrConnectivityChangedReceiver(this);
        IntentFilter filter_sim = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        IntentFilter filter_conn = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(conn_receiver, filter_sim);
        registerReceiver(conn_receiver, filter_conn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public void onBackPressed() {
        if (this.CURRENT_PAGE == this.ABOUT) {
            this.displayDashboard();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            case R.id.failed_items:
                this.displayFailedItems();
                return true;
            case R.id.check_connexions:
                return true;
            case R.id.about:
                this.displayAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(Constants.TAG, "onDestroy");

        // release lock on CPU
        Utils.ReleaseWakeLock(this);

        // remove all receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(FCMTokenReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(SettingsUpdatedReceiver);

        super.onDestroy();
    }
    public void quit() {
        this.finish();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    // pages switchers
    protected void displayDashboard(View view) { displayDashboard(); }
    protected void displayDashboard() { changeCurrentPage(DASHBOARD); }
    protected void displayAbout() { changeCurrentPage(ABOUT); }
    protected void displayFailedItems(View view) { displayFailedItems(); }
    protected void displayFailedItems() { changeCurrentPage(FAILED_ITEMS); }
    protected void hidePage(String page) {
        Log.d(Constants.TAG, "hidePage: " + page);
        int page_id = pages.get(page);
        RelativeLayout layout = (RelativeLayout) findViewById(page_id);
        layout.setVisibility(layout.GONE);
    }
    protected void showPage(String page) {
        Log.d(Constants.TAG, "showPage: " + page);
        int page_id = pages.get(page);
        RelativeLayout layout = (RelativeLayout) findViewById(page_id);
        layout.setVisibility(layout.VISIBLE);
        CURRENT_PAGE = page;
    }
    protected void changeCurrentPage(String page) {
        if (page != DASHBOARD) { hidePage(DASHBOARD); }
        if (page != FAILED_ITEMS) { hidePage(FAILED_ITEMS); }
        if (page != ABOUT) { hidePage(ABOUT); }
        this.showPage(page);
    }

    public void updateConnexionsStatus() {
        Log.d(Constants.TAG, "updateWithOMAPI");

        // check for internet and networks requirements
        updateOrangeConnexionStatus(null);
        if (!Networks.isConnectedToOrangeMobile(this)) {
            updateOrangeConnexionStatus(false);
        } else {
            // connected to Orange network
            updateOrangeConnexionStatus(true);

            // refresh token if necessary
            setupFCMRegistration();

            // update session
            OrangeMaliAPIClient.fetchUser(this);
            OrangeMaliAPIClient.fetchMsisdn(this);

            // checking server connexion now
            client.checkConnexion();
        }
    }

    public void updateOrangeConnexionStatus(Boolean connected) {
        session.setOrangeConnected(connected);
        omapi_status_textview.setTextColor(Constants.getConnectionColor(connected));
    }

    public void updateServerConnexionStatus(Boolean connected) {
        session.setServerConnected(connected);
        server_status_textview.setTextColor(Constants.getConnectionColor(connected));
        if (connected != null && connected) {
            // connected to server ; ensure we sent our token
            if (!session.getFCMTokenTransmitted()) {
                client.updateFCMToken();
            }
        }
    }

    public void updateUIWithUser(OMUser user) {
        session.setUser(user);
        if (user == null) {
            balance_textview.setText("!OMoney");
            first_name_textview.setText(Constants.BLANK);
            last_name_textview.setText(Constants.BLANK);
            balance_updated_on_textview.setText(Constants.BLANK);
        } else {
            Log.d(Constants.TAG, "updateUIWithUser: " + user.toString());
            balance_textview.setText(user.getFormattedBalance());
            first_name_textview.setText(user.getFirstName());
            last_name_textview.setText(user.getLastName());
            balance_updated_on_textview.setText(user.getFormattedUpdatedOn());
        }
    }

    public void updateUIWithMsisdn(String msisdn) {
        Log.d(Constants.TAG, "Phone number: "+ msisdn);
        session.setMsisdn(msisdn);
        msisdn_textview.setText(session.getFormattedMsisdn());
    }
}
