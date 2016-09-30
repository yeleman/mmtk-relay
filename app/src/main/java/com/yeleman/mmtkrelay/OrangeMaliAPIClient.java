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
import java.util.Date;

import cz.msebera.android.httpclient.Header;


class OrangeMaliAPIClient {

    private static final String BASE_URL = "http://omapp.orangemali.com/OMMali";
    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(OrangeMaliAPIClient.getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(OrangeMaliAPIClient.getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    static void fetchMsisdn(final MainActivity activity) {
        OrangeMaliAPIClient.get("/version.php", null, new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    activity.updateUIWithMsisdn(response.getString("client"));
                } catch (JSONException ex) {
                    Log.d(Constants.TAG, "Unable to parse JSON");
                }
            }
        });
    }

    static void fetchUser(final MainActivity activity) {
        OrangeMaliAPIClient.get("/tbw/init/api/user/status/country/ml", null, new AsyncHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Date updated_on = new Date();
                for (Header header: headers) {
                    if (header.getName().equals("Date")) {
                        try {
                            updated_on =  Constants.OAPI_HEADER_DATE_FORMAT.parse(header.getValue());
                        } catch (java.text.ParseException ex) {}
                    }
//                    Log.d(Constants.TAG, header.toString());
                }
                OMUser user = new Gson().fromJson(TextUtils.fromBytes(response), OMUser.class);
                user.setUpdatedOn(updated_on);
                activity.updateUIWithUser(user.isValid() ? user : null);
            }
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                if (errorResponse != null) {
                    String errorMessage = new String(errorResponse, StandardCharsets.UTF_8);
                    Log.d(Constants.TAG, "onFailure: " + errorMessage);
                }
            }
        });
    }

}
