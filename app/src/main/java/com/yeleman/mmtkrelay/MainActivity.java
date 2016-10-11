package com.yeleman.mmtkrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;


public class MainActivity extends AppCompatActivity {

    public Session session;
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
    private TextView tvBalance;
    private TextView tvBalanceUpdatedOn;
    private TextView tvMsisdn;
    private TextView tvFirstName;
    private TextView tvLastName;
    // servers
    private TextView tvNetworkStatus;
    private TextView tvServerStatus;

    // dashboard
    private ArrayList<Operation> operationsArrayList;
    OperationAdapter adapter;
    ListView lvOperations;

    private SIMOrConnectivityChangedReceiver connectity_receiver;

    private BroadcastReceiver FCMMessageReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "FCMMessageReceived onReceived");
            String from = intent.getStringExtra("from");
            if (!from.equals(session.getServerId())) {
                Log.e(Constants.TAG, "Received message from different SERVER_ID: --" + from + "-- instead of: --" + session.getServerId()+ "--");
            }
            JSONObject jsdata = null;
            try {
                jsdata = new JSONObject(intent.getStringExtra("data"));
            } catch (JSONException ex) {}
            if (jsdata != null) {
                Log.d(Constants.TAG, "Main activity received payload: " + jsdata.toString());
            }
        }
    };

    private BroadcastReceiver UITamperedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "UITamperedReceiver onReceived");
            Boolean refreshMsisdn = intent.getBooleanExtra("refreshMsisdn", false);
            Boolean refreshUser = intent.getBooleanExtra("refreshUser", false);
            Boolean refreshBalance = intent.getBooleanExtra("refreshBalance", false);
            Boolean refreshDashboard = intent.getBooleanExtra("refreshDashboard", false);
            Boolean refreshFailedItems = intent.getBooleanExtra("refreshFailedItems", false);
            Boolean refreshServerStatus = intent.getBooleanExtra("refreshServerStatus", false);
            Boolean refreshNetworkStatus = intent.getBooleanExtra("refreshNetworkStatus", false);
            if (refreshMsisdn || refreshUser || refreshNetworkStatus || refreshServerStatus) {
                Log.w(Constants.TAG, "brui refreshing session ");
                session.reloadPreferences();
            }
            Log.w(Constants.TAG, "br ui tamp: " + session.getServerConnected());
            redrawUI(refreshMsisdn, refreshUser, refreshBalance, refreshDashboard, refreshFailedItems,
                     refreshNetworkStatus, refreshServerStatus);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new Session(this);

        // prevent device from going to sleep
        Utils.AcquireWakeLock(this);

        // build UI with all elements (pages hidden)
        setupUI();

        // register receivers
        setupReceivers();

        // check for network requirements
        updateConnexionsStatus();

        // display dashboard
        switchToDashboard();
    }

    protected void setupUI() {
        setContentView(R.layout.activity_dashboard);

        // load references to moving elements
        tvBalance = (TextView) findViewById(R.id.tvBalance);
        tvBalanceUpdatedOn = (TextView) findViewById(R.id.tvBalanceUpdatedOn);
        tvMsisdn = (TextView) findViewById(R.id.tvMsisdn);
        tvFirstName = (TextView) findViewById(R.id.tvFirstName);
        tvLastName = (TextView) findViewById(R.id.tvLastName);
        tvNetworkStatus = (TextView) findViewById(R.id.tvNetworkStatus);
        tvServerStatus = (TextView) findViewById(R.id.tvServerStatus);

        // Dashboard
        operationsArrayList = (ArrayList<Operation>) Operation.getLatests();
        adapter = new OperationAdapter(this, operationsArrayList);
        lvOperations = (ListView) findViewById(R.id.lvOperations);
        lvOperations.setAdapter(adapter);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    protected void redrawUI(Boolean refreshMsisdn, Boolean refreshUser, Boolean refreshBalance,
                            Boolean refreshDashboard, Boolean refreshFailedItems,
                            Boolean refreshNetworkStatus, Boolean refreshServerStatus) {
        Log.e(Constants.TAG, "redrawUI refreshMsisdn:"+refreshMsisdn+ " refreshUser:"+refreshUser+
                " refreshBalance:"+refreshBalance+" refreshDashboard:"+refreshDashboard+" refreshFailedItems:"+refreshFailedItems+
                " refreshNetworkStatus:"+refreshNetworkStatus+" refreshServerStatus:"+refreshServerStatus);

        if (refreshNetworkStatus) {
            tvNetworkStatus.setTextColor(Constants.getConnectionColor(session.getOrangeConnected()));
        }

        if (refreshServerStatus) {
            Log.w(Constants.TAG, "tv update: " + session.getServerConnected());
            tvServerStatus.setTextColor(Constants.getConnectionColor(session.getServerConnected()));
            if (session.getServerConnected() != null && session.getServerConnected()) {
                // connected to server ; ensure we sent our token
                if (!session.getFCMTokenTransmitted()) {
                    ServerAPIService.startFCMTokenUpdate(this);
                }
            }
        }

        if (refreshMsisdn) {
            Log.d(Constants.TAG, "MSISDN: "+ session.getMsisdn() + " --- " + session.getFormattedMsisdn());
            tvMsisdn.setText(session.getFormattedMsisdn());
        }

        if (refreshUser) {
            if (!session.getUser().isValid()) {
                tvFirstName.setText(Constants.BLANK);
                tvLastName.setText(Constants.BLANK);
            } else {
                tvFirstName.setText(session.getUser().getFirstName());
                tvLastName.setText(session.getUser().getLastName());
            }
        }

        if (refreshBalance) {
            if (!session.getUser().isValid()) {
                tvBalance.setText("!OMoney");
                tvBalanceUpdatedOn.setText(Constants.BLANK);
            } else {
                tvBalance.setText(session.getUser().getFormattedBalance());
                tvBalanceUpdatedOn.setText(session.getUser().getFormattedUpdatedOn());
            }
        }

        if (refreshDashboard) {
            Log.e(Constants.TAG, "refresh Dashboard");
            adapter.reset();
        }

        if (refreshFailedItems) {

        }
    }

    public void setupFCMRegistration() { FirebaseInstanceId.getInstance().getToken(); }

    private void setupReceivers() {

        // Local receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
                FCMMessageReceived, new IntentFilter(Constants.FCM_MESSAGE_FILTER));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                UITamperedReceiver, new IntentFilter(Constants.UI_TAMPERED_FILTER));

        // SIMOrConnectivityChangedReceiver
        connectity_receiver = new SIMOrConnectivityChangedReceiver(this);
        IntentFilter filter_sim = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        IntentFilter filter_conn = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(connectity_receiver, filter_sim);
        registerReceiver(connectity_receiver, filter_conn);
    }

    private void tearDownReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(FCMMessageReceived);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(UITamperedReceiver);
        unregisterReceiver(connectity_receiver);
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
            this.switchToDashboard();
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
            case R.id.export_database:
                this.exportDatabase();
                return true;
            case R.id.check_connexions:
                return true;
            case R.id.about:
                this.switchToAbout();
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

        // release all receivers
        tearDownReceivers();

        super.onDestroy();
    }
    public void quit() {
        this.finish();
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void exportDatabase() {
        String message;
        if (Utils.exportDatabase(this)) {
            message = getString(R.string.export_database_successful);
        } else {
            message = getString(R.string.export_database_failed);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    // pages switchers
    public void displayDashboard(View view) { switchToDashboard(); }
    protected void switchToDashboard() { changeCurrentPage(DASHBOARD); }
    protected void switchToAbout() { changeCurrentPage(ABOUT); }
    public void displayFailedItems(View view) { switchToFailedItems(); }
    protected void switchToFailedItems() { changeCurrentPage(FAILED_ITEMS); }
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
            OrangeAPIService.startMsisdn(this);
            OrangeAPIService.startUser(this);

            // checking server connexion now
            ServerAPIService.startConnexionCheck(this);
        }
    }

    public void updateOrangeConnexionStatus(Boolean connected) {
        session.setOrangeConnected(connected);
        tvNetworkStatus.setTextColor(Constants.getConnectionColor(connected));
    }
}
