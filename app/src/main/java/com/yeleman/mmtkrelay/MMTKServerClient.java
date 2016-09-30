package com.yeleman.mmtkrelay;

import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

class MMTKServerClient {

    private MainActivity activity;
    private static AsyncHttpClient client = new AsyncHttpClient();

    MMTKServerClient(MainActivity activity) {
        this.activity = activity;
    }


    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public void postJSON(String url, JSONObject jsonParams, AsyncHttpResponseHandler responseHandler) {
        StringEntity jsonString = null;
        try { jsonString = new StringEntity(jsonParams.toString()); } catch (UnsupportedEncodingException ex) {}
        client.post(activity, getAbsoluteUrl(url), jsonString, "application/json", responseHandler);
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return this.activity.session.getServerUrl() + relativeUrl;
    }

    void checkConnexion() {
        Log.d(Constants.TAG, "checkConnexion");
        Log.d(Constants.TAG, getAbsoluteUrl("/status"));
        activity.updateServerConnexionStatus(null);
        get("/status", null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d(Constants.TAG, "checkConnexion onSuccess");
                activity.updateServerConnexionStatus(true);
                try {
                    JSONObject json = new JSONObject(TextUtils.fromBytes(response));
                    activity.updateSharedPreferences(Constants.SETTINGS_SERVER_ID, json.getString("serverID"));
                } catch (JSONException ex) { }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d(Constants.TAG, "checkConnexion onFailure");
                activity.updateServerConnexionStatus(false);
            }
        });
    }

    void updateFCMToken() {
        if (activity.session.getFCMToken() == null) {
            activity.setupFCMRegistration();
            return;
        }

        Log.d(Constants.TAG, "updateFCMToken");
        activity.session.setFCMTokenTransmitted(null);
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("token", activity.session.getFCMToken());
            jsonParams.put("MSISDN", activity.session.getMsisdn());
        } catch (JSONException ex) {}
        postJSON("/token", jsonParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d(Constants.TAG, "updateFCMToken onSuccess");
                activity.session.setFCMTokenTransmitted(true);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d(Constants.TAG, "updateFCMToken onFailure");
                activity.session.setFCMTokenTransmitted(false);
            }
        });
    }

}