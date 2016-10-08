package com.yeleman.mmtkrelay;

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
    public static final String BALANCE_REQUEST = "balance_request";
    @Ignore
    public static final String BALANCE_RECEIVED = "balance_received";
    @Ignore
    public static final String BALANCE_RECEIVED_SMS = "balance_received_sms";
    @Ignore
    public static final String TEXT_RECEIVED = "text_received";
    @Ignore
    public static final String CALL_RECEIVED = "call_received";
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
        LABELS.put(CALL_RECEIVED, "dépot");
        LABELS.put(BALANCE_REQUEST, "demande solde");
        LABELS.put(BALANCE_RECEIVED, "solde");
        LABELS.put(BALANCE_RECEIVED_SMS, "solde (SMS)");
        LABELS.put(TEXT_RECEIVED, "SMS");
        LABELS.put(CALL_RECEIVED, "appel");
    }

    @Ignore
    public static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(new String[] {
            INCOMING_TRANSFER, OUTGOING_TRANSFER, BALANCE_REQUEST,
            BALANCE_RECEIVED, BALANCE_RECEIVED_SMS,
            TEXT_RECEIVED, CALL_RECEIVED, DEPOSIT, PAYMENT }));

    @Ignore
    public static final Set<String> TRANSACTIONS = new HashSet<>(Arrays.asList(new String[] {
            INCOMING_TRANSFER, OUTGOING_TRANSFER, DEPOSIT, PAYMENT }));

    @Ignore
    public static final Set<String> BALANCES = new HashSet<>(Arrays.asList(new String[] {
            BALANCE_RECEIVED, BALANCE_RECEIVED_SMS }));

    @Ignore
    public static final String SUCCESS = "success";
    @Ignore
    public static final String FAILURE = "failure";
    @Ignore
    public static final String PENDING = "pending";

    private String action;
    private Date created_on;
    private Boolean handled = false;
    private Date handled_on;
    private String status;

    private String msisdn;
    private String own_msisdn; // to support

    // for transaction
    private Float amount;
    private Float fees; // mostly for
    private String transaction_id;

    // text message content
    private String text;


    public Operation(){
    }

    public Operation(String own_msisdn, String action) {
        this.own_msisdn = own_msisdn;
        this.action = action;
        this.created_on = new Date();
    }

    String getLabel() {
        if (!LABELS.containsKey(getAction())) {
            return "?";
        }
        return LABELS.get(getAction());
    }
    public String getAction() { return action; }
    public Date getCreatedOn() { return created_on; }
    public String getFormattedCreatedOn() { return TextUtils.shortDateFormat(this.getCreatedOn()); }
    public Boolean getHandled() { return handled; }
    public Date getHandledOn() { return handled_on; }
    public String getStatus() { return status; }
    public Boolean getBooleanStatus() {
        if (getStatus() == null || getStatus().equals(PENDING)) {
            return null;
        }
        return getStatus().equals(SUCCESS);
    }
    public String getMsisdn() { return msisdn; }
    public String getFormattedMsisdn() { return TextUtils.msisdnFormat(getMsisdn()); }
    public String getOwnMsisdn() { return own_msisdn; }
    public Float getAmount() { return amount; }
    public Float getFees() { return fees; }
    public String getFormattedAmount() { return TextUtils.moneyFormat((float) getAmount(), true); }
    public String getFormattedFees() { return TextUtils.moneyFormat((float) getFees(), true); }
    public String getFormattedAmountAndFees() {
        if (getFees() == null) {
            return getFormattedAmount();
        }
        return String.format("%s (%s)", getFormattedAmount(), getFormattedFees());
    }
    public String getTransationId() { return transaction_id; }
    public String getStrippedTransactionId() { return TextUtils.transactionFormat(getTransationId()); }
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
    public Boolean isTransaction() { return TRANSACTIONS.contains(getAction()); }
    public Boolean isBalance() { return BALANCES.contains(getAction()); }

    public String toString() {
        return String.format(Locale.ENGLISH, "Operation<%d>/%s/%s", getId(), getLabel(), getFormattedCreatedOn());
    }

    static Long storeSMSText(String own_msisdn, Date created_on, String msisdn, String text) {
        Operation operation = new Operation(own_msisdn, TEXT_RECEIVED);
        operation.created_on = created_on;
        operation.msisdn = msisdn;
        operation.text = text;
        return operation.save();
    }

    static Long storeCall(String own_msisdn, Date created_on, String msisdn) {
        Operation operation = new Operation(own_msisdn, CALL_RECEIVED);
        operation.created_on = created_on;
        operation.msisdn = msisdn;
        return operation.save();
    }

    static Long storeBalance(String own_msisdn, Date created_on, Float balance) {
        Operation operation = new Operation(own_msisdn, BALANCE_RECEIVED);
        operation.created_on = created_on;
        operation.amount = balance;
        return operation.save();
    }

    static Long storeIncomingTransfer(String own_msisdn, Date created_on, String msisdn, Float amount, String transaction_id) {
        Operation operation = new Operation(own_msisdn, INCOMING_TRANSFER);
        operation.created_on = created_on;
        operation.msisdn = msisdn;
        operation.amount = amount;
        operation.transaction_id = transaction_id;
        return operation.save();
    }

    static Long storeOutgoingTransfer(String own_msisdn, Date created_on, String msisdn, Float amount, Float fees, String transaction_id) {
        Operation operation = new Operation(own_msisdn, OUTGOING_TRANSFER);
        operation.created_on = created_on;
        operation.msisdn = msisdn;
        operation.amount = amount;
        operation.fees = fees;
        operation.transaction_id = transaction_id;
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
        operation.amount = amount;
        operation.transaction_id = transaction_id;
        return operation.save();
    }

    static Long storePayment(String own_msisdn, Date created_on, String msisdn, Float amount, Float fees, String transaction_id) {
        Operation operation = new Operation(own_msisdn, PAYMENT);
        operation.created_on = created_on;
        operation.msisdn = msisdn;
        operation.amount = amount;
        operation.amount = fees;
        operation.transaction_id = transaction_id;
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
        return Select.from(Operation.class).where("action in ?", (String[]) TRANSACTIONS.toArray()).orderBy("created_on Desc").limit(Constants.DASHBOARD_ITEMS_LIMIT).first();
    }

    public HashMap<String, Object> toHash() {
        String action = getAction();

        HashMap<String, Object> hm = new HashMap<>();
        hm.put("action", action);
        hm.put("created_on", getFormattedCreatedOn());
        hm.put("own_msisdn", getOwnMsisdn());

        if (isBalance()) {
            hm.put("amount", getAmount());
        } else if (isTransaction()) {
            hm.put("transaction_id", getTransationId());
            hm.put("msisdn", getMsisdn());
            hm.put("amount", getAmount());
            hm.put("fees", getFees());
        } else if (action.equals(TEXT_RECEIVED)) {
            hm.put("text", getText());
        }

        return hm;
    }
}


