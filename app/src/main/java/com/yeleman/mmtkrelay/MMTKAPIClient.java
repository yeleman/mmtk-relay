package com.yeleman.mmtkrelay;

import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;


class MMTKAPIClient {
    private MMTKBaseActivity activity;
    private static AsyncHttpClient client = new AsyncHttpClient();

    MMTKAPIClient(MMTKBaseActivity activity) {
        this.activity = activity;
    }

    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return activity.session.getServerUrl() + relativeUrl;
    }

    void fetchMsisdn() {
        get("/version.php", null, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    activity.session.setMsisdn(response.getString("client"));
                    Log.d(Constants.TAG, "Phone number: "+ activity.session.getMsisdn());
                } catch (JSONException ex) {
                    Log.d(Constants.TAG, "Unable to parse JSON");
                }
            }
        });
    }

    void fetchUser() {
        get("/tbw/init/api/user/status/country/ml", null, new AsyncHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d(Constants.TAG, "onSuccess");
                activity.session.setUser(new Gson().fromJson(new String(response, StandardCharsets.UTF_8), OMUser.class));
                Log.d(Constants.TAG, "success async user: " + activity.session.getUser().toString());
            }
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                String errorMessage = new String(errorResponse, StandardCharsets.UTF_8);
                Log.d(Constants.TAG, "onFailure: " + errorMessage);
            }
        });
    }

}
