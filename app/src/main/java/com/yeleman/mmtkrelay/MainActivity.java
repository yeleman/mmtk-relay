package com.yeleman.mmtkrelay;

import android.*;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Hashtable;


public class MainActivity extends AppCompatActivity {

    public Session session;
    private static final String TAG = "LOG-MMTK-MainActivity";
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

    // wake lock
    PowerManager.WakeLock wakeLock;

    private Toolbar toolbar;
    // main header
    private TextView tvDhisServer;
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

    private final BroadcastReceiver NetworkStateChanged = new BroadcastReceiver() {
        private boolean initial_network_state_received = false;
        private boolean isInitialized() {
            return initial_network_state_received;
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(Constants.TAG, "Network State changed");
            if (isInitialized()) {
                Utils.updateConnexionsStatus(context);
            } else {
                initial_network_state_received = true;
            }
        }
    };

    private final BroadcastReceiver UITamperedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(Constants.TAG, "UITamperedReceiver onReceived");
            Boolean refreshMsisdn = intent.getBooleanExtra("refreshMsisdn", false);
            Boolean refreshUser = intent.getBooleanExtra("refreshUser", false);
            Boolean refreshBalance = intent.getBooleanExtra("refreshBalance", false);
            Boolean refreshDashboard = intent.getBooleanExtra("refreshDashboard", false);
            Boolean refreshFailedItems = intent.getBooleanExtra("refreshFailedItems", false);
            Boolean refreshServerStatus = intent.getBooleanExtra("refreshServerStatus", false);
            Boolean refreshNetworkStatus = intent.getBooleanExtra("refreshNetworkStatus", false);
            Boolean refreshDhis = intent.getBooleanExtra("refreshDhis", false);
            if (refreshMsisdn || refreshUser || refreshNetworkStatus || refreshServerStatus) {
                //Log.w(Constants.TAG, "brui refreshing session ");
                session.reloadPreferences();
            }
            //Log.w(Constants.TAG, "br ui tamp: " + session.getServerConnected());
            redrawUI(refreshMsisdn, refreshUser, refreshBalance, refreshDashboard, refreshFailedItems,
                     refreshNetworkStatus, refreshServerStatus, refreshDhis);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new Session(this);

        // prevent device from going to sleep
        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK_TAG);
        wakeLock.setReferenceCounted(false);
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // build UI with all elements (pages hidden)
        setupUI();

        // register receivers
        setupReceivers();

        // check for network requirements
        Utils.updateConnexionsStatus(this);

        // display dashboard
        switchToDashboard();

        // TODO: DHIS
		DHISUtils.setup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // display permission warning
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "PackageManager.PERMISSION_GRANTED: " + PackageManager.PERMISSION_GRANTED);
            Log.d(TAG, "SEND_SMS: " + ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS));
            Log.d(TAG, "RECEIVE_SMS: " + ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS));
            Log.d(TAG, "READ_PHONE_STATE: " + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE));
            Log.d(TAG, "PROCESS_OUTGOING_CALLS: " + ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS));
            Log.d(TAG, "WRITE_EXTERNAL_STORAGE: " + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));


            Utils.showdisplayPermissionErrorPopup(this,
                    getString(R.string.permission_required),
                    getString(R.string.enable_permissions_in_settings_text), true);
        }
    }

    protected void setupUI() {
        setContentView(R.layout.activity_dashboard);

        if (Constants.HAS_DHIS) {
            // hide regular widgets
            LinearLayout headerLayout = (LinearLayout) findViewById(R.id.ll_header);
            LinearLayout serverLineLayout = (LinearLayout) findViewById(R.id.ll_serverLine);
            headerLayout.setVisibility(View.GONE);
            serverLineLayout.setVisibility(View.GONE);

            // display DHIS replacement ones
            LinearLayout dhisHeaderLayout = (LinearLayout) findViewById(R.id.ll_dhisHeader);
            LinearLayout dhisServerLineLayout = (LinearLayout) findViewById(R.id.ll_dhisServerLine);
            dhisHeaderLayout.setVisibility(View.VISIBLE);
            dhisServerLineLayout.setVisibility(View.VISIBLE);
        }

        tvDhisServer = (TextView) findViewById(R.id.tvDhisServer);
        tvDhisServer.setText(DHISUtils.getAbsoluteDHISUrl(this, ""));

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
                            Boolean refreshNetworkStatus, Boolean refreshServerStatus,
                            Boolean refreshDHIS) {
//        Log.e(Constants.TAG, "redrawUI refreshMsisdn:"+refreshMsisdn+ " refreshUser:"+refreshUser+
//                " refreshBalance:"+refreshBalance+" refreshDashboard:"+refreshDashboard+" refreshFailedItems:"+refreshFailedItems+
//                " refreshNetworkStatus:"+refreshNetworkStatus+" refreshServerStatus:"+refreshServerStatus);

        if (refreshNetworkStatus) {
            tvNetworkStatus.setTextColor(Constants.getConnectionColor(session.getOrangeConnected()));
        }

        if (refreshServerStatus) {
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
                tvBalance.setText(getString(R.string.not_orange_money));
                tvBalanceUpdatedOn.setText(Constants.BLANK);
            } else {
                tvBalance.setText(session.getUser().getFormattedBalance());
                tvBalanceUpdatedOn.setText(session.getUser().getFormattedUpdatedOn());
            }
        }

        if (refreshDHIS) {
            Log.e(Constants.TAG, "refresh DHIS" + DHISUtils.getAbsoluteDHISUrl(this, ""));
            tvDhisServer.setText(DHISUtils.getAbsoluteDHISUrl(this, ""));
        }

        if (refreshDashboard) {
            Log.e(Constants.TAG, "refresh Dashboard");
            adapter.reset();
        }

        if (refreshFailedItems) {
            // display failed items
        }
    }

    private void setupReceivers() {

        // Local receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
                UITamperedReceiver, new IntentFilter(Constants.UI_TAMPERED_FILTER));

        // Connectivity Action
        registerReceiver(NetworkStateChanged, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    private void tearDownReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(UITamperedReceiver);
        unregisterReceiver(NetworkStateChanged);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (this.CURRENT_PAGE.equals(MainActivity.ABOUT)) {
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
                Utils.updateConnexionsStatus(this);
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
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // release all receivers
        tearDownReceivers();

        super.onDestroy();
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
        layout.setVisibility(RelativeLayout.GONE);
    }
    protected void showPage(String page) {
        Log.d(Constants.TAG, "showPage: " + page);
        int page_id = pages.get(page);
        RelativeLayout layout = (RelativeLayout) findViewById(page_id);
        layout.setVisibility(RelativeLayout.VISIBLE);
        CURRENT_PAGE = page;
    }
    protected void changeCurrentPage(String page) {
        if (!page.equals(DASHBOARD)) { hidePage(DASHBOARD); }
        if (!page.equals(FAILED_ITEMS)) { hidePage(FAILED_ITEMS); }
        if (!page.equals(ABOUT)) { hidePage(ABOUT); }
        this.showPage(page);
    }
}
