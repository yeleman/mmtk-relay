package com.yeleman.mmtkrelay;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Table;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Operation extends SugarRecord {

    @Ignore
    public static final String INCOMING_TRANSFER = "incoming_transfer";
    @Ignore
    public static final String OUTGOING_TRANSFER = "outgoing_transfer";
    @Ignore
    public static final String BALANCE = "balance";
    @Ignore
    public static final String INCOMING_TEXT = "incoming_text";
    @Ignore
    public static final String OUTGOING_TEXT = "outgoing_text";
    @Ignore
    public static final String INCOMING_CALL = "incoming_call";
    @Ignore
    public static final String DEPOSIT = "deposit";
    @Ignore
    public static final String PAYMENT = "payment";

    @Ignore
    public static final HashMap<String, String> LABELS;
    static {
        LABELS = new HashMap<>();
        LABELS.put(INCOMING_TRANSFER, "reçu");
        LABELS.put(OUTGOING_TRANSFER, "envoyé");
        LABELS.put(DEPOSIT, "dépot");
        LABELS.put(BALANCE, "solde");
        LABELS.put(INCOMING_TEXT, "SMS reçu");
        LABELS.put(OUTGOING_TEXT, "SMS envoyé");
        LABELS.put(INCOMING_CALL, "appel");
    }

    @Ignore
    public static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(new String[] {
            INCOMING_TRANSFER, OUTGOING_TRANSFER, BALANCE,
            INCOMING_TEXT, INCOMING_CALL, OUTGOING_TEXT, DEPOSIT, PAYMENT }));

    @Ignore
    public static final Set<String> TRANSACTIONS = new HashSet<>(Arrays.asList(new String[] {
            INCOMING_TRANSFER, OUTGOING_TRANSFER, DEPOSIT, PAYMENT }));

    // successfulness statuses
    @Ignore
    public static final String SUCCESS = "success";
    @Ignore
    public static final String FAILURE = "failure";
    @Ignore
    public static final String PENDING = "pending";
    public static final Set<String> STATUSES = new HashSet<>(Arrays.asList(new String[] { SUCCESS, FAILURE, PENDING }));

    // handling statuses
    @Ignore
    public static final String HANDLED = "handled";
    @Ignore
    public static final String IN_PROGRESS = "in_progress";
    @Ignore
    public static final String NOT_HANDLED = "not_handled";
    @Ignore
    public static final Set<String> HANDLING_STATUSES = new HashSet<>(Arrays.asList(new String[] { HANDLED, IN_PROGRESS, NOT_HANDLED }));

    // origins (creator)
    @Ignore
    public static final String SMS = "sms";
    @Ignore
    public static final String CALL = "call";
    @Ignore
    public static final String POLLING = "polling";
    @Ignore
    public static final String SERVER = "server";
    @Ignore
    public static final Set<String> ORIGINS = new HashSet<>(Arrays.asList(new String[] { SMS, CALL, POLLING, SERVER }));

    private String action;
    private Date created_on;

    // whether we are finished treating this operation
    private String handling;
    private Date handling_on;

    // whether this operation was inherently successful
    private String status;
    private Date status_on;
    private String reason;

    // rellay's own MSISDN to support SIM swaping
    private String own_msisdn;
    // other party MSISDN
    private String msisdn;

    // transaction data
    private Float amount;
    private Float fees; // mostly for
    private String transaction_id;

    // text message content
    private String text;
    private Boolean delivered = false;
    private Date delivered_on;


    public Operation(){
    }

    public Operation(@NonNull String own_msisdn, @NonNull String action) {
        if (!ACTIONS.contains(action)) {
            throw new UnsupportedOperationException("Unknown operation action " + action);
        }
        this.own_msisdn = own_msisdn;
        this.action = action;
        this.created_on = new Date();
        this.status = PENDING;
        this.handling = NOT_HANDLED;
    }

    static Operation getBy(Long id) {
        String[] params = new String[1];
        params[0] = String.valueOf(id);
        return Select.from(Operation.class).where("ID = ?", params).limit("1").first();
    }

    String getLabel() {
        if (!LABELS.containsKey(getAction())) {
            return "?";
        }
        if (getAction().equals(OUTGOING_TEXT) && getDelivered()) {
            return LABELS.get(getAction()) + "✓";
        }
        return LABELS.get(getAction());
    }

    public String getAction() { return action; }
    public Date getCreatedOn() { return created_on; }
    public String getFormattedCreatedOn() { return TextUtils.shortDateFormat(this.getCreatedOn()); }

    boolean isHandled() { return (getHandling() != null && getHandling().equals(HANDLED)); }
    void setHandling(String handling) { this.handling = handling; }
    void setHandlingOn(Date handlingOn) { this.handling_on = handlingOn; }
    public String getHandling() { return handling; }
    void markHandled() { markHandled(new Date());}
    void markHandled(Date handlingOn) {
        setHandling(HANDLED);
        setHandlingOn(handlingOn);
        save(); }
    public Date getHandlingOn() { return handling_on; }

    void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
    public Boolean isSuccessful() { return (getStatus()!= null && getStatus().equals(SUCCESS)); }
    void markSuccessful() { setStatus(SUCCESS); save(); }
    void markPending() { setStatus(PENDING); save(); }
    void markFailed() { setStatus(FAILURE); save(); }
    void markFailed(String reason) { setStatus(FAILURE); setReason(reason); save(); }
    Date getStatusOn() { return status_on; }
    void setStatusOn(Date statusOn) { this.status_on = statusOn; }

    void setDeliveredOn(Date deliveredOn) { this.delivered_on = deliveredOn; }
    Date getDeliveredOn() { return  delivered_on; }
    void setDelivered(Boolean delivered) { this.delivered = delivered; }
    public Boolean getDelivered() { return delivered; }
    void markDelivered() { markDelivered(new Date()); };
    void markDelivered(Date deliveredOn) {
        setDelivered(true);
        setDeliveredOn(deliveredOn);
        save();
    }

    void setMsisdn(String msisdn) { this.msisdn = msisdn; }
    public String getMsisdn() { return msisdn; }
    public String getFormattedMsisdn() { return TextUtils.msisdnFormat(getMsisdn()); }
    public String getOwnMsisdn() { return own_msisdn; }
    void setAmount(Float amount) { this.amount = amount; }
    public Float getAmount() { return amount; }
    void setFees(Float fees) { this.fees = fees; }
    public Float getFees() { return fees; }
    public String getFormattedAmount() { return TextUtils.moneyFormat(getAmount(), true); }
    public String getFormattedFees() { return TextUtils.moneyFormat(getFees(), true); }
    public String getFormattedAmountAndFees() {
        if (getFees() == null) {
            return getFormattedAmount();
        }
        return String.format("%s (%s)", getFormattedAmount(), getFormattedFees());
    }
    void setTransactionId(String transactionId) { this.transaction_id = transactionId; }
    public String getTransationId() { return transaction_id; }
    public String getStrippedTransactionId() { return TextUtils.transactionFormat(getTransationId()); }

    void setText(String text) { this.text = text; }
    public String getText() { return text; }
    public String getStrippedText() {
        if (getText() == null || getText().length() == 0) {
            return "";
        }
        if (getText().length() < Constants.DASHBOARD_TEXT_PREVIEW_LIMIT) {
            return getText();
        }
        return getText().substring(0, Constants.DASHBOARD_TEXT_PREVIEW_LIMIT) + "…";
    }
    void setReason(String reason) { this.reason = reason; }


    public Boolean isTransaction() { return TRANSACTIONS.contains(getAction()); }
    public Boolean isBalance() { return getAction().equals(BALANCE); }

    public String toString() {
        return String.format(Locale.ENGLISH, "Operation<%d>/%s/%s", getId(), getLabel(), getFormattedCreatedOn());
    }

    static Long storeSMSText(String own_msisdn, Date created_on, String msisdn, String text) {
        Operation operation = new Operation(own_msisdn, INCOMING_TEXT);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setText(text);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeOutgoingSMSText(String own_msisdn, Date created_on, String msisdn, String text) {
        Operation operation = new Operation(own_msisdn, OUTGOING_TEXT);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setText(text);
        operation.setStatus(PENDING);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeCall(String own_msisdn, Date created_on, String msisdn) {
        Operation operation = new Operation(own_msisdn, INCOMING_CALL);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeBalanceRequest(String own_msisdn, Date created_on) {
        Operation operation = new Operation(own_msisdn, BALANCE);
        operation.created_on = created_on;
        operation.setStatus(PENDING);
        operation.setStatusOn(created_on);
        operation.save();
    }

    static Long storeBalance(String own_msisdn, Date created_on, Float balance) {
        Operation operation = new Operation(own_msisdn, BALANCE);
        operation.created_on = created_on;
        operation.setAmount(balance);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeIncomingTransfer(String own_msisdn, Date created_on, String msisdn, Float amount, String transaction_id) {
        Operation operation = new Operation(own_msisdn, INCOMING_TRANSFER);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setAmount(amount);
        operation.setTransactionId(transaction_id);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeOutgoingTransfer(String own_msisdn, Date created_on, String msisdn, Float amount, Float fees, String transaction_id) {
        Operation operation = new Operation(own_msisdn, OUTGOING_TRANSFER);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setAmount(amount);
        operation.setFees(fees);
        operation.setTransactionId(transaction_id);
        operation.setStatus(PENDING);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storeFailedTransaction(String own_msisdn, Date created_on, String text) {
        Operation operation = new Operation(own_msisdn, "failed_transaction");
        operation.created_on = created_on;
        operation.text = text;
        return operation.save();
    }

    static Long storeDeposit(String own_msisdn, Date created_on, Float amount, String transaction_id) {
        Operation operation = new Operation(own_msisdn, DEPOSIT);
        operation.created_on = created_on;
        operation.setAmount(amount);
        operation.setTransactionId(transaction_id);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static Long storePayment(String own_msisdn, Date created_on, String msisdn, Float amount, Float fees, String transaction_id) {
        Operation operation = new Operation(own_msisdn, PAYMENT);
        operation.created_on = created_on;
        operation.setMsisdn(msisdn);
        operation.setAmount(amount);
        operation.setFees(fees);
        operation.setTransactionId(transaction_id);
        operation.setStatus(SUCCESS);
        operation.setStatusOn(created_on);
        return operation.save();
    }

    static List<Operation> getLatests()
    {
        return Select.from(Operation.class).orderBy("CREATEDON Desc").limit(Constants.DASHBOARD_ITEMS_LIMIT).list();
    }

    static List<Operation> getNewestSince(Long since)
    {
        String[] params = new String[1];
        params[0] = String.valueOf(since);
        return Select.from(Operation.class).where("ID > ?", params).orderBy("CREATEDON Desc").list();
    }

    static Operation getLatestTransaction()
    {
        return Select.from(Operation.class).where("action in ?", (String[]) TRANSACTIONS.toArray()).orderBy("CREATEDON Desc").limit(Constants.DASHBOARD_ITEMS_LIMIT).first();
    }

    static Operation getFromUri(Uri uri) {
        List<String> segments = uri.getPathSegments();
        String[] params = new String[2];
        params[0] = segments.get(0);
        params[1] = segments.get(1);
        Log.e(Constants.TAG, params.toString());
        return Select.from(Operation.class).where("action = ? AND id = ?", params).orderBy("CREATEDON Desc").limit("1").first();
    }

    public HashMap<String, Object> toHash() {
        String action = getAction();

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("action", action);
        hm.put("created_on", getFormattedCreatedOn());
        hm.put("own_msisdn", getOwnMsisdn());

        if (action.equals(BALANCE)) {
            hm.put("amount", getAmount());
        } else if (isTransaction()) {
            hm.put("transaction_id", getTransationId());
            hm.put("msisdn", getMsisdn());
            hm.put("amount", getAmount());
            hm.put("fees", getFees());
        } else if (action.equals(INCOMING_TEXT)) {
            hm.put("text", getText());
        }

        return hm;
    }
}


