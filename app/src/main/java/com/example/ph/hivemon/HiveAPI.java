package com.example.ph.hivemon;

import org.json.JSONException;
import org.json.JSONObject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

public class HiveAPI {
    private static final String UTF_8 = "UTF-8";
    private static final String GET_RIGS = "getRigs";
    private static final String GET_WALLETS = "getWallets";
    private static final String GET_OC = "getOC";
    private static final String GET_CURRENT_STATS = "getCurrentStats";
    private static final String MULTI_ROCKET = "multiRocket";

    private String SecretKey;
    private String PublicKey;
    private static String HTTPS_URL = "https://api.hiveos.farm/worker/eypiay.php";

    static class Response{
        int responseCode = -1;
        JSONObject responseData = null;
    }

    private class MultiRocketParams{
        String rigIds;         // coma separated string with rig ids "1,2,3,4"
        String miner = null;   // Miner to set. Leave it null if you do not want to change. "claymore", "claymore-z", "ewbf", ...
        String miner2 = null;  // Second miner to set. Leave it null if you do not want to change. "0" - if you want to unset it.
        String idWal = null;   // ID of wallet. Leave it null if you do not want to change.
        String idOc = null;    // ID of OC profile. Leave it null if you do not want to change.
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    public HiveAPI(String SecretKey, String PublicKey){
        this.SecretKey = SecretKey;
        this.PublicKey = PublicKey;
    }

    public JSONObject getRigs() throws Exception {
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("method", GET_RIGS);
        Response result = sendPOST(HTTPS_URL, httpParams);
        if(result.responseCode == 200) {
            return ((JSONObject) result.responseData.get("result"));
        }else{
            return null;
        }
    }

    public JSONObject getWallets() throws Exception {
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("method", GET_WALLETS);
        Response result = sendPOST(HTTPS_URL, httpParams);
        if(result.responseCode == 200) {
            return ((JSONObject) result.responseData.get("result"));
        }else{
            return null;
        }
    }

    public JSONObject getOC() throws Exception {
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("method", GET_OC);
        Response result = sendPOST(HTTPS_URL, httpParams);
        if(result.responseCode == 200) {
            return ((JSONObject) result.responseData.get("result"));
        }else{
            return null;
        }
    }

    public JSONObject getCurrentStats() throws Exception {
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("method", GET_CURRENT_STATS);
        Response result = sendPOST(HTTPS_URL, httpParams);
        if(result.responseCode == 200) {
            return ((JSONObject) result.responseData.get("result"));
        }else{
            return null;
        }
    }

    public JSONObject multiRocket (MultiRocketParams params) throws Exception {
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("method", MULTI_ROCKET);
        if(params.rigIds != null) httpParams.put("rig_ids_str", params.rigIds);
        if(params.miner != null) httpParams.put("miner", params.miner);
        if(params.miner2 != null) httpParams.put("miner2", params.miner2);
        if(params.idWal != null) httpParams.put("id_wal", params.idWal);
        if(params.idOc != null) httpParams.put("id_oc", params.idOc);
        Response result = sendPOST(HTTPS_URL, httpParams);
        if(result.responseCode == 200) {
            return ((JSONObject) result.responseData.get("result"));
        }else{
            return null;
        }
    }

    private Response sendPOST(String url, Map<String, String> params) {
        params.put("public_key", this.PublicKey);
        String urlParameters = buildQueryString(params, UTF_8);
        Response response = new Response();
        StringBuffer buf = new StringBuffer();

        URL obj = null;
        try {
            obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            // add request header
            con.setRequestProperty("HMAC", encodeHMAC(this.SecretKey, urlParameters));
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);

            // Send post request
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            // Get result
            response.responseCode = con.getResponseCode();
            InputStream is;
            if(response.responseCode == 200) {
                is = con.getInputStream();
            }else{
                is = con.getErrorStream();
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                buf.append(inputLine);
            }
            in.close();
            if(response.responseCode == 200) {
                response.responseData = new JSONObject(buf.toString());
            }else{
                try{
                    response.responseData = new JSONObject(buf.toString());
                    log("ERROR: HTTP " + response.responseCode + ": \n" + response.responseData.toString(4));
                }catch (JSONException e) {
                    log("ERROR: HTTP " + response.responseCode + ": " + buf.toString());
                }
            }
        } catch (MalformedURLException e) {
            log("ERROR: Invalid URL: " + url);
            e.printStackTrace();
        } catch (ProtocolException e) {
            log("ERROR: Invalid request method");
            e.printStackTrace();
        } catch (IOException e) {
            log("ERROR: input/output operation");
            e.printStackTrace();
        } catch (JSONException e) {
            log("ERROR: Invalid json response: " + buf.toString());
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            log("ERROR: Can't find HmacSHA256 algorithm");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            log("ERROR: Invalid secret key");
            e.printStackTrace();
        }
        return response;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    static String encodeHMAC(String key, String data) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes(UTF_8));
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString().toLowerCase();

    }

    static String buildQueryString(Map<String, String> parameters, String encoding) {
        return parameters.entrySet().stream()
                .map(entry -> encodeParameter(entry.getKey(), entry.getValue(), encoding))
                .reduce((param1, param2) -> param1 + "&" + param2)
                .orElse("");
    }

    static String encodeParameter(String key, String value, String encoding) {
        return urlEncode(key, encoding) + "=" + urlEncode(value, encoding);
    }

    static String urlEncode(String value, String encoding) {
        try {
            return URLEncoder.encode(value, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot url encode " + value, e);
        }
    }

}

