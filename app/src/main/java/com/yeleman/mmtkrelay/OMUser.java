package com.yeleman.mmtkrelay;

import java.util.Date;

/**
 * Created by reg on 9/21/16.
 */

public class OMUser {

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
