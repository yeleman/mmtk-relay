package com.yeleman.mmtkrelay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

class DHISCredentialsResponse {
    final static String STATUS_SUCCESS = "success";
    final static String STATUS_NETWORK_FAILURE = "network_failure";
    final static String STATUS_SERVER_ERROR = "server_error";
    final static String STATUS_AUTH_ERROR = "auth_error";
    final static String STATUS_DHIS_ERROR = "dhis_error";
    private String status = null;
    ArrayList<String[]> userOrganisationUnits = null;

    String getStatus() { return this.status; }
    void setStatus(String status) { this.status = status; }
    public boolean isSuccessful() { return getStatus().equals(STATUS_SUCCESS); }
    public boolean isTemporaryFailure() { return getStatus().equals(STATUS_NETWORK_FAILURE); }
    public boolean isServerError() { return getStatus().equals(STATUS_SERVER_ERROR); }
    public boolean isDHISError() { return getStatus().equals(STATUS_DHIS_ERROR); }

    public DHISCredentialsResponse() { setStatus(null); }

    static DHISCredentialsResponse fromResponse(Context context, Response response) {
        DHISCredentialsResponse dhisCredentialsResponse = new DHISCredentialsResponse();
        // unable to connect to DHIS server
        if (response == null) {
            dhisCredentialsResponse.setStatus(DHISCredentialsResponse.STATUS_NETWORK_FAILURE);
            return dhisCredentialsResponse;
        }
        // did not return 200
        if (!response.succeeded()) {
            if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                dhisCredentialsResponse.setStatus(DHISCredentialsResponse.STATUS_AUTH_ERROR);
            } else {
                dhisCredentialsResponse.setStatus(DHISCredentialsResponse.STATUS_SERVER_ERROR);
            }
            return dhisCredentialsResponse;
        }

        JSONObject jsonObject = response.getJSON();
        // shouldn't happen
        if (jsonObject == null) {
            dhisCredentialsResponse.setStatus(DHISCredentialsResponse.STATUS_DHIS_ERROR);
            return dhisCredentialsResponse;
        }

        JSONArray orgUnitsArray = DHISUtils.getJSONArray(jsonObject, DHISUtils.KEY_ORGANISATION_UNITS);
        if (orgUnitsArray == null || orgUnitsArray.length() == 0) {
            dhisCredentialsResponse.setStatus(DHISCredentialsResponse.STATUS_AUTH_ERROR);
            return dhisCredentialsResponse;
        }

        dhisCredentialsResponse.userOrganisationUnits = new ArrayList<>();
        List<String> validOrganisationUnits = Arrays.asList(DHISUtils.getValidOrganisationUnits(context));
        Log.d(DHISUtils.TAG, validOrganisationUnits.size() + " / " + validOrganisationUnits.toString());
        for (int i=0; i<orgUnitsArray.length(); i++) {
            JSONObject orgUnitObject = DHISUtils.getJSONObject(orgUnitsArray, i);
            if (orgUnitObject == null) { continue; }
            String orgUnitId = DHISUtils.getString(orgUnitObject, DHISUtils.KEY_ID);
            String orgUnitName = DHISUtils.getString(orgUnitObject, DHISUtils.KEY_NAME);
            Log.d(DHISUtils.TAG, orgUnitId);
            if (!validOrganisationUnits.contains(orgUnitId)) {
                continue;
            }
            dhisCredentialsResponse.userOrganisationUnits.add(new String[]{orgUnitId, orgUnitName});
        }
        dhisCredentialsResponse.setStatus(STATUS_SUCCESS);
        return dhisCredentialsResponse;
    }
}

class DHISUploadResponse {
    final static String STATUS_SUCCESS = "success";
    final static String STATUS_NETWORK_FAILURE = "network_failure";
    final static String STATUS_SERVER_ERROR = "server_error";
    final static String STATUS_AUTH_ERROR = "auth_error";
    final static String STATUS_DHIS_ERROR = "dhis_error";
    final static String STATUS_DATA_ERROR = "data_error";

    private Context context = null;
    private String status = null;
    private String description = null;
    private int deleted = 0;
    private int ignored = 0;
    private int updated = 0;
    private int imported = 0;

    int getDeleted() { return this.deleted; }
    void setDeleted(Integer deleted) { this.deleted = (deleted == null) ? 0 : deleted; }
    int getIgnored() { return this.ignored; }
    void setIgnored(Integer ignored) { this.ignored = (ignored == null) ? 0 : ignored; }
    int getUpdated() { return this.updated; }
    void setUpdated(Integer updated) { this.updated = (updated == null) ? 0 : updated; }
    int getImported() { return this.imported; }
    void setImported(Integer imported) { this.imported = (imported == null) ? 0 : imported; }

    Context getContext() { return this.context; }
    void setContext(Context context) { this.context = context; }
    String getDescription() { return this.description; }
    void setDescription(String description) { this.description = description; }
    String getStatus() { return this.status; }
    void setStatus(String status) { this.status = status; }
    public boolean isSuccessful() { return getStatus().equals(STATUS_SUCCESS); }
    public boolean isTemporaryFailure() { return getStatus().equals(STATUS_NETWORK_FAILURE); }
    public boolean isServerError() { return getStatus().equals(STATUS_SERVER_ERROR); }
    public boolean isDHISError() { return getStatus().equals(STATUS_DHIS_ERROR); }

    public DHISUploadResponse(Context context) {
        setContext(context);
        setStatus(null);
    }

    String getStatusFromCounts() {
        if (getImported() == 0 && getUpdated() == 0) {
            return STATUS_DATA_ERROR;
        }
        return STATUS_SUCCESS;
    }
    void setStatusFromCounts() { setStatus(getStatusFromCounts()); }

    String getSMSReply() { return String.format("[%s:%s] %s", DHISUtils.getSMSLabel(getContext()), getPrefix(), getMessage()); }

    String getMessage() {
        String message = "";
        switch (getStatus()) {
            case STATUS_SUCCESS:

                message = String.format(Locale.FRANCE,
                        "Donnees enregistrees: %1$d ajoutee(s), %2$d mise(s) a jour, %3$d ignoree(s).",
                        getImported(), getUpdated(), getIgnored());
                break;
            case STATUS_AUTH_ERROR:
                message = "Identification incorecte. Verifiez vos identifiants et vos droits d'acces.";
                break;
            case STATUS_DATA_ERROR:
                if (getDescription() != null) {
                    message = "Donnees rejetees: " + getDescription();
                } else {
                    message = "Vos donnees ont ete refusees.";
                }
                break;
            case STATUS_DHIS_ERROR:
            case STATUS_SERVER_ERROR:
                message = "Une erreur s'est produite lors de l'envoi au serveur. Reessayez plus tard. Contacter le support si le probleme persiste";
                break;
            case STATUS_NETWORK_FAILURE:
                message = "Probleme de connexion entre le relai et le serveur. Si vous n'avez pas eu de retour sous 1h, contactez le support";
                break;
        }
        if (message.length() > getMessageMaxLength()) {
            return message.substring(0, getMessageMaxLength());
        }
        return message;
    }

    int getMessageMaxLength() {
        return DHISUtils.MAX_SMS_CHARS - getPrefix().length();
    }

    String getPrefix() {
        switch (getStatus()) {
            case STATUS_SUCCESS:
                return DHISUtils.PREFIX_SUCCESS;
            case STATUS_AUTH_ERROR:
            case STATUS_DATA_ERROR:
            case STATUS_DHIS_ERROR:
            case STATUS_SERVER_ERROR:
                return DHISUtils.PREFIX_ERROR;
            case STATUS_NETWORK_FAILURE:
                return DHISUtils.PREFIX_WARNING;
        }
    }

    static DHISUploadResponse fromResponse(Context context, Response response) {
        DHISUploadResponse dhisUploadResponse = new DHISUploadResponse();
        // unable to connect to DHIS server
        if (response == null) {
            dhisUploadResponse.setStatus(STATUS_NETWORK_FAILURE);
            return dhisUploadResponse;
        }
        // did not return 200
        if (!response.succeeded()) {
            if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                dhisUploadResponse.setStatus(STATUS_AUTH_ERROR);
            } else {
                dhisUploadResponse.setStatus(STATUS_SERVER_ERROR);
            }
            return dhisUploadResponse;
        }

        JSONObject jsonObject = response.getJSON();
        // shouldn't happen
        if (jsonObject == null) {
            dhisUploadResponse.setStatus(STATUS_DHIS_ERROR);
            return dhisUploadResponse;
        }

        String status = DHISUtils.getString(jsonObject, "status");
        if (status.equals("SUCCESS")) {
            // not successful ; so no import
            dhisUploadResponse.setStatus(STATUS_DHIS_ERROR);
            return dhisUploadResponse;
        }

        String responseType = DHISUtils.getString(jsonObject, "responseType");
        if (!responseType.equals("ImportSummary")) {
            // not an import summary ; so no import?
            dhisUploadResponse.setStatus(STATUS_DHIS_ERROR);
            return dhisUploadResponse;
        }

        JSONObject importCount = DHISUtils.getJSONObject(jsonObject, "importCount");
        if (importCount != null) {
            dhisUploadResponse.setDeleted(DHISUtils.getInt(importCount, "deleted"));
            dhisUploadResponse.setIgnored(DHISUtils.getInt(importCount, "ignored"));
            dhisUploadResponse.setImported(DHISUtils.getInt(importCount, "imported"));
            dhisUploadResponse.setUpdated(DHISUtils.getInt(importCount, "updated"));
        }

        dhisUploadResponse.setStatusFromCounts();

        return dhisUploadResponse;
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

    static final int MAX_SMS_CHARS = 160;
    static final String SMS_SPACER = " ";
    static final String MISSING_VALUE = "-";
    final static String PREFIX_SUCCESS = "OK";
    final static String PREFIX_WARNING = "/!\\";
    final static String PREFIX_ERROR = "ERROR";

    static final String KEY_DATAELEMENT = "dataElement";
    static final String KEY_CATEGORYOPTIONCOMBO = "categoryOptionCombo";
    static final String KEY_VALUE = "value";
    static final String KEY_DATAVALUES = "dataValues";
    static final String KEY_COMPLETEDATE = "completeDate";
    static final String KEY_PERIOD = "period";
    static final String KEY_DATASET = "dataSet";
    static final String KEY_ORGUNIT = "orgUnit";

    static final DateFormat DHIS_DATE_FORMAT = new SimpleDateFormat("YYYY-MM-DD", java.util.Locale.getDefault());
    
    static final String[] PLAIN_CHARACTERS = "abcdefghijklmnopqrstuvwxyz:1234567890_-".split("");
    static final String[] TRANSLATED_CHARACTERS = "f12_8sy3bco47gnadxmqtij6:wz-09phe5krlvu".split("");

    public static Boolean handleIncomingText(final Context context, String identity, String smsText) {
        if (isDHISCredentialsCheckSMS(context, smsText)) {
            Log.d(TAG, "credentials check SMS: "+ smsText);
            String[] parts = smsText.split(SMS_SPACER, 2);
            if (parts.length != 2) {
                Log.e(TAG, "invalid SMS format for credentials: "+ parts.length);
                return true;
            }
            String[] credentials = getDeobfuscatedCredentials(parts[1]);
            checkCredentials(context, identity, credentials[0], credentials[1]);
            return true;
        } else if (isDHISReportSMS(context, smsText)) {
            Log.d(TAG, "DHIS report SMS");

            // whether an indexed values report SMS or regular
            Boolean isIndexed = smsText.startsWith("i:");

            // split metadata and body
            String[] parts = smsText.split("#", 1);
            if (parts.length != 2) {
                Log.e(TAG, "invalid SMS format for report: "+ parts.length);
                return true;
            }

            // extra individual metadata
            String[] metadataParts = parts[0].split(SMS_SPACER, 4);
            if (metadataParts.length != 4) {
                Log.e(TAG, "invalid SMS format for report (metadata): "+ metadataParts.length);
                return true;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            int version = -1;
            try { version = Integer.valueOf(metadataParts[1]); } catch (Exception ex) {}
            String obfucastedCredentials = metadataParts[2];
            String organisationUnit = metadataParts[3];

            if (version != sharedPreferences.getInt(KEY_DHIS_VERSION, -1)) {
                // reply different SMS version
                Log.e(TAG, "SMS format version differs from relay");
                // OutgoingSMSService.startSingleSMS(context, identity, "");
                return true;
            }

            // retrieve clear-text credentials
            String[] credentials = getDeobfuscatedCredentials(obfucastedCredentials);
            String username = credentials[0];
            String password = credentials[1];

            // build-up dataValues part of request
            JSONObject payload = buildDHISJSONPayload(context, "201609", organisationUnit, parts[1], isIndexed);
            uploadReport(context, identity, username, password, payload);

            return true;
        } else {
            Log.d(TAG, "unknown type SMS");
        }
        return false;
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

        // extract meta data from SMS
        String datasetId = getString(jsonObject, KEY_DATASET_ID);
        String keyword = getString(jsonObject, KEY_KEYWORD);
        Integer version = getInt(jsonObject, KEY_VERSION);
        String[] smsFormat = toStringArray(getJSONArray(jsonObject, KEY_SMS_FORMAT));

        String[] organisationUnits;
        JSONArray orgUnitsArray = getJSONArray(jsonObject, KEY_ORGANISATION_UNITS);
        if (orgUnitsArray != null) {
            organisationUnits = new String[orgUnitsArray.length()];
            for (int i = 0; i < orgUnitsArray.length(); i++) {
                JSONObject jso = getJSONObject(orgUnitsArray, i);
                if (jso != null) {
                    String id = getString(jso, KEY_ID);
                    if (id != null) {
                        organisationUnits[i] = id;
                    }
                }
            }
        } else {
            organisationUnits = new String[0];
        }

        if (keyword == null || version == null ||
                smsFormat == null || smsFormat.length == 0 ||
                organisationUnits == null || organisationUnits.length == 0) {
            Log.e(TAG, "missing valid information in JSON file");
            return false;
        }

        // save key informations to preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

        sharedPreferencesEditor.putString(KEY_DHIS_CREDENTIALS_KEYWORD, "check");
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

    public static JSONObject getJSONObject(JSONObject obj, String key) {
        try {
            return obj.getJSONObject(key);
        } catch (JSONException ex) {
            return null;
        }
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

    public static JSONObject buildDHISJSONPayload(
            Context context, String period, String organisationUnit, String dataText, Boolean indexedVersion) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        JSONObject jsonObject = new JSONObject();

        String datasetId = sharedPreferences.getString(KEY_DHIS_DATASET_ID, null);
        String completeDate = DHIS_DATE_FORMAT.format(new Date());
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

    public static String getSMSLabel(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(KEY_DHIS_REPORT_KEYWORD, "").toUpperCase();
    }

    public static String getAbsoluteDHISUrl(Context context, String path) {
        return getDHISServerUrl(context) + path;
    }

    public static String getBasicCredentials(String username, String password) {
            String credentials = username + ":" + password;
            return "Basic " + new String(Base64.encode(credentials.getBytes(), Base64.DEFAULT));
    }

    public static HashMap<String, String> getBasicAuthorizationHeaders(String username, String password) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", getBasicCredentials(username, password));
        return headers;
    }

    public static void checkCredentials(final Context context, final String identity, final String username, final String password) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DHISCredentialsResponse dhisCredentialsResponse = DHISCredentialsResponse.fromResponse(
                        context,
                        Requests.getResponse(
                                getAbsoluteDHISUrl(context, API_USER_ACCOUNT_URL),
                                getBasicAuthorizationHeaders(username, password)));
                Log.d(TAG, "DHISCredentialsResponse: "+ dhisCredentialsResponse.getStatus());

                String statusCode = "ERROR";
                String message = "";
                if (dhisCredentialsResponse.isSuccessful() && !dhisCredentialsResponse.userOrganisationUnits.isEmpty()) {
                    // perfect
                    statusCode = "OK";
                    for(String[] orgUnit: dhisCredentialsResponse.userOrganisationUnits) {
                        message += String.format("%s=%s |", orgUnit[0], orgUnit[1]);
                    }
                    message = message.substring(0, message.length() - 1).trim();
                } else if (dhisCredentialsResponse.isSuccessful()) {
                    // credentials OK but no Org Units
                    statusCode = "/!\\";
                    message = "Vos identifiants DHIS sont corrects mais vous n'avez pas d'unité d'organisation pour ce rapport";
                } else if (dhisCredentialsResponse.isTemporaryFailure()) {
                    // problem here. try again later
                    message = "Problème d'acces au serveur au niveau du relai. Reessayez plus tard ou contacter le support.";
                } else if (dhisCredentialsResponse.isServerError()){
                    // server error. try again later or contact support
                    message = "Erreur sur le serveur DHIS. Reessayez plus tard ou contacter le support.";
                } else {
                    // DHIS error. your bad.
                    message = "Identifiants DHIS incorrects";
                }
                String smsText = String.format("[%s:%s] %s", "DHIS", statusCode, message);
                Log.e(TAG, "SMS to " + identity + ": " + smsText);
                OutgoingSMSService.startSingleSMS(context, identity, smsText);
            }
        };
        new Thread(task).start();
    }

    public static void uploadReport(final Context context, final String identity, final String username, final String password, JSONObject payload) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DHISUploadResponse dhisUploadResponse = DHISUploadResponse.fromResponse(
                        context,
                        Requests.getResponse(
                                getAbsoluteDHISUrl(context, API_DATASET_UPLOAD_URL),
                                getBasicAuthorizationHeaders(username, password)));
                Log.d(TAG, "DHISUploadResponse: "+ dhisUploadResponse.getStatus());


                String smsText = dhisUploadResponse.getSMSReply();
                Log.e(TAG, "SMS to " + identity + ": " + smsText);
                OutgoingSMSService.startSingleSMS(context, identity, smsText);
            }
        };
        new Thread(task).start();
    }

}
