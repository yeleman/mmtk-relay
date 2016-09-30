package com.yeleman.mmtkrelay;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Requests {

    public static final int DEFAULT_TIMEOUT = 600;

    public static String getRequest(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Log.e(Constants.TAG, ex.toString());
        } catch (IOException ex) {
            Log.e(Constants.TAG, ex.toString());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Log.e(Constants.TAG, ex.toString());
                }
            }
        }
        return null;
    }

    public static String getJSON(String url) {
        return Requests.getJSON(url, Requests.DEFAULT_TIMEOUT);
    }

    public static String getJSON(String url, int timeout) {
        return Requests.getRequest(url, timeout);
    }

    public static Response getServerResponse(String url) {
        return new Gson().fromJson(Requests.getJSON(url), Response.class);
    }

    public static OMUser getOMUser(String url) {
        // warn on java.socket timeout
        OMUser user = new Gson().fromJson(Requests.getJSON(url), OMUser.class);
        Log.d(Constants.TAG, user.toString());
        return user;
    }
}
