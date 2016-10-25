package com.yeleman.mmtkrelay;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IncomingTextProcessor extends IntentService {

    private static final String BALANCE_REGEX = "^Le solde de votre compte est de ([0-9]+\\.[0-9]+) FCFA.$";
    private static final String INCOMING_TRANSFER_REGEX = "^Vous avez recu ([0-9]+\\.[0-9]+) FCFA du ([0-9]+). Votre nouveau solde est de:([0-9]+\\.[0-9]+) FCFA. ID: ([A-Z\\s\\.0-9]+)$";
    private static final String OUTGOING_TRANSFER_REGEX = "^Votre transfert de ([0-9]+\\.[0-9]+) FCFA vers le ([0-9]+) a reussi. Frais: ([0-9]+\\.[0-9]+) FCFA. Nouveau solde:([0-9]+\\.[0-9]+) FCFA. ID:([A-Z\\s\\.0-9]+)$";
    private static final String FAILED_TRANSACTION_REGEX = "^La transaction a echoue car (.+)$";
    private static final String DEPOSIT_REGEX = "^Votre recharge par Orange Money de ([0-9]+\\.[0-9]+) FCFA a reussi. Votre nouveau solde est de: ([0-9]+\\.[0-9]+) FCFA. ID : ([A-Z\\s\\.0-9]+)$";
    private static final String PAYMENT_REGEX = "^Retrait de ([0-9]+\\.[0-9]+) FCFA effectue par le ([0-9]+). Frais:([0-9]+\\.[0-9]+) FCFA. Votre nouveau solde est de ([0-9]+\\.[0-9]+) FCFA. ID:([A-Z\\s\\.0-9]+)$";

    private static final String ACTION_PROCESS_SMS = "com.yeleman.mmtkrelay.action.PROCESS_SMS";
    private static final String ACTION_PROCESS_CALL = "com.yeleman.mmtkrelay.action.PROCESS_CALL";

    private static final String EXTRA_FROM = "com.yeleman.mmtkrelay.extra.FROM";
    private static final String EXTRA_TIMESTAMP = "com.yeleman.mmtkrelay.extra.TIMESTAMP";
    private static final String EXTRA_TEXT = "com.yeleman.mmtkrelay.extra.TEXT";

    private Context context;
    private Session session;

    public IncomingTextProcessor() {
        super("IncomingTextProcessor");
    }

    public static void startProcessing(Context context, String from, long timestamp, String text) {
        Intent intent = new Intent(context, IncomingTextProcessor.class);
        intent.setAction(ACTION_PROCESS_SMS);
        intent.putExtra(EXTRA_FROM, from);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp);
        intent.putExtra(EXTRA_TEXT, text);
        context.startService(intent);
    }

    public static void startProcessingCall(Context context, String from, Long timestamp) {
        Intent intent = new Intent(context, IncomingTextProcessor.class);
        intent.setAction(ACTION_PROCESS_CALL);
        intent.putExtra(EXTRA_FROM, from);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        session = new Session(context);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        session.reloadPreferences();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_SMS.equals(action)) {
                final String from = intent.getStringExtra(EXTRA_FROM);
                final long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);
                final String text = intent.getStringExtra(EXTRA_TEXT);
                handleIncomingSMS(from, timestamp, text);
            } else if (ACTION_PROCESS_CALL.equals(action)) {
                final String from = intent.getStringExtra(EXTRA_FROM);
                final long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);
                handleIncomingCall(from, timestamp);
            }
            Utils.triggerUIRefresh(context, "refreshDashboard");
        }
    }

    private void handleIncomingSMS(String from, Long timestamp, String text) {
        Log.e(Constants.TAG, "handleIncomingSMS from "+ from + " on " + timestamp + ": " + text);

        // check sender. If sender filter is set and no match, exit.
        from = cleanMsisdn(from);
        if (!session.getSMSForwarding() && !from.equals(session.getOrangeSender())) {
            Log.e(Constants.TAG, "SMS forwarding disabled and not from expected sender. exiting.");
            return;
        }

        // parse date
        Date received_on = fromTimestamp(timestamp);

        // TODO: move DHIS out
        if (DHISUtils.handleIncomingText(context, from, text)) {
            Log.d(Constants.TAG, "DHIS will handle");
            return;
        } else {
            Log.d(Constants.TAG, "DHIS will NOT handle");
        }

        // parse text and prepare data
        if (!from.equals(session.getOrangeSender())) {
            // text from stranger to forward without parsing
            Long oid = Operation.storeSMSText(session.getMsisdn(), received_on, from, text);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());
            return;
        }

        // parse for orange money transactions

        // balance
        Pattern pattern;
        Matcher matcher;

        // balance update
        pattern = Pattern.compile(BALANCE_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            Float balance = Float.parseFloat(matcher.group(1));
            Long oid = Operation.storeBalance(session.getMsisdn(), received_on, balance);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());
            return;
        }

        // incoming transfer succeeded
        pattern = Pattern.compile(INCOMING_TRANSFER_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            Float amount = Float.parseFloat(matcher.group(1));
            String msisdn = matcher.group(2);
            Float balance = Float.parseFloat(matcher.group(3));
            String transaction_id = matcher.group(4);
            Long oid = Operation.storeIncomingTransfer(session.getMsisdn(), received_on, msisdn, amount, transaction_id);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());

            Long oid2 = Operation.storeBalance(session.getMsisdn(), received_on, balance);
            Log.d(Constants.TAG, "Recorded operation#" + oid2.toString());
            return;
        }

        // outgoing transfer succeeded
        pattern = Pattern.compile(OUTGOING_TRANSFER_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            Float amount = Float.parseFloat(matcher.group(1));
            String msisdn = matcher.group(2);
            Float fees = Float.parseFloat(matcher.group(3));
            Float balance = Float.parseFloat(matcher.group(4));
            String transaction_id = matcher.group(5);
            Long oid = Operation.storeOutgoingTransfer(session.getMsisdn(), received_on, msisdn, amount, fees, transaction_id);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());

            Long oid2 = Operation.storeBalance(session.getMsisdn(), received_on, balance);
            Log.d(Constants.TAG, "Recorded operation#" + oid2.toString());
            return;
        }

        // deposit onto account
        pattern = Pattern.compile(DEPOSIT_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            Float amount = Float.parseFloat(matcher.group(1));
            Float balance = Float.parseFloat(matcher.group(2));
            String transaction_id = matcher.group(3);
            Long oid = Operation.storeDeposit(session.getMsisdn(), received_on, amount, transaction_id);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());

            Long oid2 = Operation.storeBalance(session.getMsisdn(), received_on, balance);
            Log.d(Constants.TAG, "Recorded operation#" + oid2.toString());
            return;
        }

        // last transaction failed: should already be recorded as such
        pattern = Pattern.compile(FAILED_TRANSACTION_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            String reason = matcher.group(1);
            Operation operation = Operation.getLatestTransaction();
//            if (operation.getAction().equals(Operation.OUTGOING_TRANSFER) && operation.getStatus().equals(Operation.PENDING)) {
//
//            }
            Log.e(Constants.TAG, operation.toString());
            // Long oid = Operation.storeFailedTransaction(session.getMsisdn(), received_on, reason);
            // Log.d(Constants.TAG, "Recorded operation#" + oid.toString());
            return;
        }

        // invoice payment
        pattern = Pattern.compile(PAYMENT_REGEX);
        matcher = pattern.matcher(text);
        if (matcher.matches()) {
            Float amount = Float.parseFloat(matcher.group(1));
            String msisdn = matcher.group(2);
            Float fees = Float.parseFloat(matcher.group(3));
            Float balance = Float.parseFloat(matcher.group(4));
            String transaction_id = matcher.group(5);

            Long oid = Operation.storePayment(session.getMsisdn(), received_on, msisdn, amount, fees, transaction_id);
            Log.d(Constants.TAG, "Recorded operation#" + oid.toString());

            Long oid2 = Operation.storeBalance(session.getMsisdn(), received_on, balance);
            Log.d(Constants.TAG, "Recorded operation#" + oid2.toString());
            return;
        }

        // no matching pattern. must be a text message from OrangeMoney
        Long oid = Operation.storeSMSText(session.getMsisdn(), received_on, from, text);
        Log.d(Constants.TAG, "Recorded operation#" + oid.toString());
    }

    private void handleIncomingCall(String from, Long timestamp) {
        Log.e(Constants.TAG, "handleIncomingCall from "+ from + " on " + timestamp);

        // check sender. If sender filter is set and no match, exit.
        from = cleanMsisdn(from);
        if (!session.getCallForwarding()) {
            Log.e(Constants.TAG, "Call forwarding disabled. exiting.");
            return;
        }

        Date received_on = fromTimestamp(timestamp);

        Long oid = Operation.storeCall(session.getMsisdn(), received_on, from);
        Log.d(Constants.TAG, "Recorded operation#" + oid.toString());

    }

    public static Date fromTimestamp(Long timestamp) {
        if (timestamp == 0L) {
            return new Date();
        } else {
            return new Date(timestamp);
        }
    }

    public static String cleanMsisdn(String from) {
        return from.replace("+", "");
    }

}
