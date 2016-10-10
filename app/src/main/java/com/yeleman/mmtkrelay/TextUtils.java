package com.yeleman.mmtkrelay;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class TextUtils {

    public static String toTitleCase(String string) {
        String[] words = string.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
            }
        }
        return sb.toString();
    }

    public static String moneyFormat(float number, boolean addCurrency) {
        String formatted = String.format("%,.0f", number);
        if (addCurrency) {
            return String.format("%sF", formatted);
        }
        return formatted;
    }

    public static String msisdnFormat(String msisdn) {
        try {
            if (msisdn.matches("^[a-zA-Z]+$")) {
                return msisdn;
            }
            String formatted = msisdn.replaceFirst("223", "");
            return String.format("%s %s %s %s",
                    formatted.subSequence(0, 2), formatted.subSequence(2, 4),
                    formatted.subSequence(4, 6), formatted.subSequence(6, 8));
        } catch (Exception ex) {}
        return msisdn;
    }

    public static String transactionFormat(String transactionId) {
        try {
            String[] parts = transactionId.split("\\.", 4);
            return parts[2];
        } catch (Exception ex) {}
        return "";
    }

    public static String shortDateFormat(Date date) { return Constants.DATE_FORMAT.format(date); }

    public static String fileDateFormat(Date date) { return Constants.FILE_DATE_FORMAT.format(date); }

    public static String fromBytes(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8); }
}
