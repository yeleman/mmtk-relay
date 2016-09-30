package com.yeleman.mmtkrelay;

import java.nio.charset.StandardCharsets;

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

    public static String fromBytes(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
