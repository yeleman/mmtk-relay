package com.yeleman.mmtkrelay;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

class Requests {

    static final int DEFAULT_TIMEOUT = 6000;

    static Response getResponse(String url, String authorization, int timeout) {
        HttpURLConnection c = null;
        Response response = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            if (authorization != null) {
                c.addRequestProperty("Authorization", authorization);
            }
            c.connect();

            response = new Response(c);
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
        return response;
    }
    static Response getResponse(String url) {
        return getResponse(url, null, DEFAULT_TIMEOUT);
    }

    static Response getResponse(String url, String authorization) {
        return getResponse(url, authorization, DEFAULT_TIMEOUT);
    }

    static Response postJSON(String url, JSONObject params) { return postJSON(url, params, DEFAULT_TIMEOUT); }

    static Response postJSON(String url, JSONObject params, int timeout) {
        HttpURLConnection c = null;
        Response response = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
//            c.connect();
            OutputStream os = c.getOutputStream();
            os.write(params.toString().getBytes("UTF-8"));
            os.flush();

            response = new Response(c);

        } catch (Exception ex) { // MalformedURLException|IOException
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
        return response;
    }
}


class Response {
    private HttpURLConnection connection;
    private String body = null;

    Response(HttpURLConnection connection) {
        this.connection = connection;
        body = readBody();

    }

    HttpURLConnection getConnection() { return connection; }

    Integer getResponseCode() {
        try {
            return connection.getResponseCode();
        } catch (IOException ex) {
            Log.e(Constants.TAG, ex.toString());
            return null;
        }
    }

    boolean succeeded() { return getResponseCode() != null && (getResponseCode() == 200 || getResponseCode() == 201); }

    String getBody() { return body; }

    String readBody() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException ex) {
            Log.e(Constants.TAG, ex.toString());
            return null;
        }
    }

    JSONObject getJSON() {
        try {
            return new JSONObject(getBody());
        } catch (JSONException|NullPointerException ex) {
            Log.e(Constants.TAG, ex.toString());
            return null;
        }
    }

    String getHeaderField(String name) { return connection.getHeaderField(name); }

    long getHeaderFieldDate(String name, long Default) {
        return connection.getHeaderFieldDate(name, Default);
    }
    Date getHeaderFieldDate(String name, Date Default) {
        return new Date(connection.getHeaderFieldDate(name, Default.getTime()));
    }
    Date getHeaderFieldDate(String name) {
        return getHeaderFieldDate(name, new Date());
    }

    Map<String, List<String>> getHeaderFields() { return connection.getHeaderFields(); }

    List<String> getHeaderKeys() {
        List<String> keys = new ArrayList<>();
        Map<String, List<String>> map = getHeaderFields();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }
}