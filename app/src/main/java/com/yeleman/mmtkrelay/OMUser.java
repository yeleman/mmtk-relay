package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;

class OMUser {

    public int balance;
    public boolean barredAsReceiver;
    public boolean barredAsSender;
    public String firstName;
    public String lastName;
    public String frBalance;
    public String message;
    public int status;
    public boolean suspended;
    public Date updatedOn;

    public static OMUser fromPreferences(Context context) {
        OMUser user = new OMUser();
        user.loadFromPreferences(context);
        return user;
    }

    public void loadFromPreferences(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        balance = sharedPref.getInt("user_balance", 0);
        barredAsReceiver = sharedPref.getBoolean("user_barredAsReceiver", false);
        barredAsSender = sharedPref.getBoolean("user_barredAsSender", false);
        firstName = sharedPref.getString("user_firstName", null);
        lastName = sharedPref.getString("user_lastName", null);
        frBalance = sharedPref.getString("user_frBalance", null);
        message = sharedPref.getString("user_message", null);
        status = sharedPref.getInt("user_status", 0);
        suspended = sharedPref.getBoolean("user_suspended", false);
        updatedOn = new Date(sharedPref.getInt("user_updatedOn", (int) new Date().getTime()));
    }

    void saveToPreferences(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putInt("user_balance", balance);
        prefEditor.putBoolean("user_barredAsReceiver", barredAsReceiver);
        prefEditor.putBoolean("user_barredAsReceiver", barredAsReceiver);
        prefEditor.putString("user_firstName", firstName);
        prefEditor.putString("user_lastName", lastName);
        prefEditor.putString("user_message", message);
        prefEditor.putInt("user_status", status);
        prefEditor.putBoolean("user_suspended", suspended);
        prefEditor.putInt("user_updatedOn", (int) updatedOn.getTime());
        prefEditor.apply();
    }

    public void setBalance(String balance) {
        this.balance = Integer.parseInt(balance);
    }

    public int getBalance() { return this.balance; };

    public String getFormattedBalance() {
        return TextUtils.moneyFormat(this.getBalance(), true);
    }

    public String getFirstName() {
        return TextUtils.toTitleCase(this.firstName);
    }

    public String getLastName() {
        return this.lastName.toUpperCase();
    }

    public String getFullName() {
        return String.format("%s %s", this.getFirstName(), this.getLastName());
    }

    public String getFormattedUpdatedOn() {
        return TextUtils.shortDateFormat(this.getUpdatedOn());
    }
    public Date getUpdatedOn() {
        return this.updatedOn;
    }
    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public boolean isSuspended() { return this.suspended; }
    public boolean isValid() { return this.status == 200; }

    public String toString() {
        if (this.status != 200) {
            if (this.status == 40201) {
                return "Non enregistré sur OrangeMoney (40201)";
            }
            return "Erreur OM.";
        }
        return String.format("%s: %s", this.getFullName(), this.getFormattedBalance());
    }


}
