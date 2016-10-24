package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.*;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.annotations.JsonAdapter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.Credentials;

class DHISResponse {
    final static String STATUS_SUCCESS = "success";
    final static String STATUS_FAILURE = "failure";
    final static String STATUS_NETWORK_FAILURE = "network_failure";
    final static String STATUS_SERVER_ERROR = "server_error";
    final static String STATUS_AUTH_ERROR = "auth_error";
    final static String STATUS_DHIS_ERROR = "dhis_error";

    String status = null;
    ArrayList<String[]> userOrganisationUnits = null;

    String getStatus() { return this.status; }
    void setStatus(String status) { this.status = status; }
    public boolean isSuccessful() { return status.equals(STATUS_SUCCESS); }
    public boolean isTemporaryFailure() { return status.equals(STATUS_NETWORK_FAILURE); }

    public DHISResponse() {}

    static DHISResponse fromCredentialsCheckResponse(Context context, Response response) {
        DHISResponse dhisResponse = new DHISResponse();
        // unable to connect to DHIS server
        if (response == null) {
            dhisResponse.setStatus(DHISResponse.STATUS_NETWORK_FAILURE);
            return dhisResponse;
        }
        // did not return 200
        if (!response.succeeded()) {
            if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                dhisResponse.setStatus(DHISResponse.STATUS_AUTH_ERROR);
            } else {
                dhisResponse.setStatus(DHISResponse.STATUS_SERVER_ERROR);
            }
            return dhisResponse;
        }

        JSONObject jsonObject = response.getJSON();
        // shouldn't happen
        if (jsonObject == null) {
            dhisResponse.setStatus(DHISResponse.STATUS_DHIS_ERROR);
            return dhisResponse;
        }

        JSONArray orgUnitsArray = DHISUtils.getJSONArray(jsonObject, DHISUtils.KEY_ORGANISATION_UNITS);
        if (orgUnitsArray == null || orgUnitsArray.length() == 0) {
            dhisResponse.setStatus(DHISResponse.STATUS_AUTH_ERROR);
            return dhisResponse;
        }

        dhisResponse.userOrganisationUnits = new ArrayList<>();
        List<String> validOrganisationUnits = Arrays.asList(DHISUtils.getValidOrganisationUnits(context));
        for (int i=0; i<orgUnitsArray.length(); i++) {
            JSONObject orgUnitObject = DHISUtils.getJSONObject(orgUnitsArray, i);
            if (orgUnitObject == null) { continue; }
            String orgUnitId = DHISUtils.getString(orgUnitObject, DHISUtils.KEY_ID);
            String orgUnitName = DHISUtils.getString(orgUnitObject, DHISUtils.KEY_NAME);
            if (!validOrganisationUnits.contains(orgUnitId)) {
                continue;
            }
            dhisResponse.userOrganisationUnits.add(new String[]{orgUnitId, orgUnitName});
        }
        dhisResponse.setStatus(STATUS_SUCCESS);
        return dhisResponse;
    }

    static DHISResponse fromUploadReportResponse(Context context, Response response) {
        if (!response.succeeded()) {
            // network access? connectivity or URL problem
            // auth error ?
        }
        return new DHISResponse();
    }

}

public class DHISUtils {

    public static final String TAG = "LOG-MMTK-DHISUtils";
    public static final String REPORT_JSON_FILE = "palu-v1.json";

    static final String DEFAULT_DHIS_SERVER_URL = "http://192.168.5.166:8080";
    public static final String API_DATASET_UPLOAD_URL = "/api/dataValueSets";
    public static final String API_USER_ACCOUNT_URL = "/api/me";

    static final String KEY_DHIS_SERVER_URL = "dhis:serverUrl";
    static final String KEY_DHIS_DATASET_ID = "dhis:datasetId";
    static final String KEY_DHIS_REPORT_KEYWORD = "dhis:report_keyword";
    static final String KEY_DHIS_CREDENTIALS_KEYWORD = "dhis:credentials_keyword";
    static final String KEY_DHIS_VERSION = "dhis:version";
    static final String KEY_DHIS_SMS_FORMAT = "dhis:smsFormat";
    static final String KEY_DHIS_ORGANISATION_UNITS = "dhis:organisationUnits";

    static final String KEY_ID = "id";
    static final String KEY_NAME = "name";
    static final String KEY_DATASET_ID = "datasetId";
    static final String KEY_KEYWORD = "keyword";
    static final String KEY_VERSION = "version";
    static final String KEY_SMS_FORMAT = "smsFormat";
    static final String KEY_ORGANISATION_UNITS = "organisationUnits";

    static final String SMS_SPACER = " ";
    static final String MISSING_VALUE = "-";

    static final String KEY_DATAELEMENT = "dataElement";
    static final String KEY_CATEGORYOPTIONCOMBO = "categoryOptionCombo";
    static final String KEY_VALUE = "value";
    static final String KEY_DATAVALUES = "dataValues";
    static final String KEY_COMPLETEDATE = "completeDate";
    static final String KEY_PERIOD = "period";
    static final String KEY_DATASET = "dataSet";
    static final String KEY_ORGUNIT = "orgUnit";
    
    static final String[] PLAIN_CHARACTERS = "abcdefghijklmnopqrstuvwxyz:1234567890_-".split("");
    static final String[] TRANSLATED_CHARACTERS = "f12_8sy3bco47gnadxmqtij6:wz-09phe5krlvu".split("");

    public static void testIncomingText(Context context, String smsText) {
        if (isDHISCredentialsCheckSMS(context, smsText)) {
            Log.d(TAG, "credentials check SMS");
            String[] parts = smsText.split(SMS_SPACER, 1);
            if (parts.length != 2) {
                Log.e(TAG, "invalid SMS format for credentials");
                return;
            }
            String[] credentials = getDeobfuscatedCredentials(parts[1]);
            DHISResponse dhisResponse = DHISResponse.fromCredentialsCheckResponse(context, checkCredentials(context, credentials[0], credentials[1]));
            Log.d(TAG, "DHISResponse: "+ dhisResponse.getStatus());
        } else if (isDHISReportSMS(context, smsText)) {
            Log.d(TAG, "DHIS report SMS");

        } else {
            Log.d(TAG, "unknown type SMS");
        }
    }

    public static String readAssetFile(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static Boolean parseReportJSONFile(Context context, String fileName) {
        String filePath = String.format("sms/%s", fileName);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(readAssetFile(context, filePath));
        } catch (JSONException ex) {
            Log.e(TAG, "unable to parse JSON file");
            ex.printStackTrace();
            return false;
        }

        // SMS format version
        String datasetId = getString(jsonObject, KEY_DATASET_ID);
        String keyword = getString(jsonObject, KEY_KEYWORD);
        Integer version = getInt(jsonObject, KEY_VERSION);
        String[] smsFormat = toStringArray(getJSONArray(jsonObject, KEY_SMS_FORMAT));
        String[] organisationUnits = toStringArray(getJSONArray(jsonObject, KEY_ORGANISATION_UNITS));
        if (keyword == null || version == null ||
                smsFormat == null || smsFormat.length == 0 ||
                organisationUnits == null || organisationUnits.length == 0) {
            Log.e(TAG, "missing valid information in JSON file");
            return false;
        }

        // save key informations to preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

        sharedPreferencesEditor.putString(KEY_DATASET_ID, datasetId);
        sharedPreferencesEditor.putString(KEY_DHIS_REPORT_KEYWORD, keyword);
        sharedPreferencesEditor.putInt(KEY_DHIS_VERSION, version);
        sharedPreferencesEditor.putString(KEY_DHIS_SMS_FORMAT, serializeStringArray(smsFormat));
        sharedPreferencesEditor.putString(KEY_DHIS_ORGANISATION_UNITS, serializeStringArray(organisationUnits));
        sharedPreferencesEditor.apply();

        return true;
    }

    public static String obfuscate(String plain){
        return StringUtils.replaceEach(plain, PLAIN_CHARACTERS, TRANSLATED_CHARACTERS);
    }

    public static String deobfuscate(String encrypted){
        return StringUtils.replaceEach(encrypted, TRANSLATED_CHARACTERS, PLAIN_CHARACTERS);
    }

    public static String[] getDeobfuscatedCredentials(String obfuscatedText) {
        String deobfuscated = deobfuscate(obfuscatedText);
        String[] usernamePassword = deobfuscated.split("\\:");
        if (!(usernamePassword.length == 2 && usernamePassword[0].length() > 0 && usernamePassword[1].length() > 0)) {
            return null;
        }
        return usernamePassword;
    }

    public static Boolean matchesSMSKeyword(Context context, String key, String text, Boolean includeIndexed) {
        if (text == null) {
            return false;
        }
        String smsKeyword = text.split(" ")[0];
        if (smsKeyword.length() == 0) {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String keyword = sharedPreferences.getString(key, null);
        if (keyword == null) {
            return false;
        }
        String indexedKeyword = String.format("i:%s", keyword);

        if (includeIndexed) {
            return smsKeyword.equals(keyword) || smsKeyword.equals(indexedKeyword);
        }
        return smsKeyword.equals(keyword);
    }

    public static Boolean isDHISCredentialsCheckSMS(Context context, String text) {
        return matchesSMSKeyword(context, KEY_DHIS_CREDENTIALS_KEYWORD, text, false);
    }

    public static Boolean isDHISReportSMS(Context context, String text) {
        return matchesSMSKeyword(context, KEY_DHIS_REPORT_KEYWORD, text, true);
    }

    public static String[] getValidOrganisationUnits(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return deserializeStringArray(sharedPreferences.getString(KEY_DHIS_ORGANISATION_UNITS, ""));
    }

    public static String serializeStringArray(String[] organisationUnits) {
        return TextUtils.join("|", organisationUnits);
    }

    public static String[] deserializeStringArray(String serialized) {
        return serialized.split("\\|");
    }

    public static Boolean isValidOrganisationUnit(Context context, String organisationUnit) {
        String[] validOrganisationUnits = getValidOrganisationUnits(context);
        for (int i=0; i<validOrganisationUnits.length; i++) {
            if (organisationUnit.equals(validOrganisationUnits[i])) {
                return true;
            }
        }
        return false;
    }

    public static JSONObject getJSONObject(JSONArray obj, int index) {
        try {
            return obj.getJSONObject(index);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static String getString(JSONObject obj, String key) {
        try {
            return obj.getString(key);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static Integer getInt(JSONObject obj, String key) {
        try {
            return obj.getInt(key);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static JSONArray getJSONArray(JSONObject obj, String key) {
        try {
            return obj.getJSONArray(key);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static String[] toStringArray(JSONArray array) {
        if(array==null)
            return null;

        String[] arr=new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }

    public static JSONObject jsonObjectFrom(String humanId, String value) {
        String dhisId;
        String categoryOptionCombo = null;
        JSONObject jsonObject = new JSONObject();

        if (humanId.contains(".")) {
            String[] humanIdParts = humanId.split("\\.");
            dhisId = humanIdParts[0];
            categoryOptionCombo = humanIdParts[1];
        } else {
            dhisId = humanId;
        }

        try {
            jsonObject.put(KEY_DATAELEMENT, dhisId);
            if (categoryOptionCombo != null) {
                jsonObject.put(KEY_CATEGORYOPTIONCOMBO, categoryOptionCombo);
            }
            jsonObject.put(KEY_VALUE, value);
        } catch (JSONException ex) {
            Log.e(TAG, "unable to prepare JSON for payload");
            ex.printStackTrace();
            return null;
        }
        return jsonObject;
    }

    public static JSONArray getJSONPayloadFrom(Context context, String text, Boolean indexedVersion) {
        JSONArray jsonArray = new JSONArray();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (indexedVersion) {
            String[] codedValues = text.split(SMS_SPACER);
            for (int i=0; i<codedValues.length; i++) {
                String[] idValuePair = codedValues[i].split("\\=");
                String humanId = idValuePair[0];
                String value = idValuePair[1];
                JSONObject jsonObject = jsonObjectFrom(humanId, value);
                if (jsonObject != null) {
                    jsonArray.put(jsonObject);
                } else {
                    // fail on any mismatched value
                    return null;
                }
            }
        } else {
            String[] values = text.split(SMS_SPACER);
            String[] expectedValues = deserializeStringArray(sharedPreferences.getString(KEY_DHIS_SMS_FORMAT, ""));
            if (values.length != expectedValues.length) {
                Log.e(TAG, String.format(
                        "incorrect SMS payload. nb of values differs (%d intead of %d). missing part or incorrect version",
                        values.length, expectedValues.length));
                return null;
            }

            for (int i=0; i<expectedValues.length; i++) {
                String humanId = expectedValues[i];
                String value = values[1];
                JSONObject jsonObject = jsonObjectFrom(humanId, value);
                if (jsonObject != null) {
                    jsonArray.put(jsonObject);
                } else {
                    // fail on any mismatched value
                    return null;
                }
            }
        }

        return jsonArray;
    }

    public static Boolean handleIncomingSMSReport() {
        return true;
    }

    public static JSONObject buildDHISJSONPayload(Context context, String period, String organisationUnit, String dataText, Boolean indexedVersion) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        JSONObject jsonObject = new JSONObject();

        String datasetId = sharedPreferences.getString(KEY_DHIS_DATASET_ID, null);
        // TODO: verify date format pattern online
        String completeDate = DateFormatUtils.format(new Date(), "YYYY-MM-DD");
        JSONArray dataValues = getJSONPayloadFrom(context, dataText, indexedVersion);

        try {
            jsonObject.put(KEY_ORGUNIT, organisationUnit);
            jsonObject.put(KEY_DATASET, datasetId);
            jsonObject.put(KEY_PERIOD, period);
            jsonObject.put(KEY_COMPLETEDATE, completeDate);
            jsonObject.put(KEY_DATAVALUES, dataValues);
        } catch (JSONException ex) {
            Log.e(TAG, "Unable to build DHIS JSON Payload");
            ex.printStackTrace();
            return null;
        }
        return jsonObject;
    }

    public static String getDHISServerUrl(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String serverUrl = sharedPreferences.getString(KEY_DHIS_SERVER_URL, DEFAULT_DHIS_SERVER_URL);
        if (serverUrl.endsWith("/")) {
            return serverUrl.substring(0, serverUrl.length() - 1);
        }
        return serverUrl;
    }

    public static String getAbsoluteDHISUrl(Context context, String path) {
        return getDHISServerUrl(context) + path;
    }

    public static String getBasicCredentials(String username, String password) {
        return Credentials.basic(username,password);
    }

    public static Response checkCredentials(Context context, String username, String password) {
        Response response = Requests.getResponse(
                getAbsoluteDHISUrl(context, API_USER_ACCOUNT_URL),
                getBasicCredentials(username, password));
        return response;
    }

    public static Response uploadReport(Context context, String username, String password, JSONObject payload) {
        Response response = Requests.postJSON(
                getAbsoluteDHISUrl(context, API_DATASET_UPLOAD_URL),
                payload);

        return response;
    }
}
