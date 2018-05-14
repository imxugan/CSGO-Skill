/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

// Intentionally left as Java

package net.flare_esports.csgoskill;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.flare_esports.csgoskill.Constants.DEV_MODE;

class InternetHelper {

    public static boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (Throwable e) {
            if (DEV_MODE) Log.e("InternetHelper.isOnline()", e);
        }
        return false;
    }

    public static Bitmap HardBitmapRequest(String url) {
        return new BitmapTask().doInBackground(url);
    }

    public static Bitmap BitmapRequest(String url) throws Throwable {
        try {
            return new BitmapTask().execute(url).get(3, TimeUnit.SECONDS);
        } catch (Throwable e) {
            if (e instanceof TimeoutException) {
                // Don't worry about failed image requests
                return null;
            } else {
                throw e;
            }
        }
    }

    public static String HTTPRequest(String url) throws Throwable {
        try {
            JSONObject object = new HTTPJsonTask().execute(new JSONObject().put("url", url)).get(5, TimeUnit.SECONDS);
            if (object == null) return "";
            else if (object.has("reason")) return object.getString("reason");
            else return object.toString();
        } catch (Throwable e) {
            if (e instanceof TimeoutException) {
                return "Request timed out";
            } else {
                throw e;
            }
        }
    }

    public static JSONObject HardHTTPJsonRequest(JSONObject request) {
        return new HTTPJsonTask().doInBackground(request);
    }

    public static JSONObject HTTPJsonRequest(JSONObject request) throws Throwable {
        try {
            return new HTTPJsonTask().execute(request).get(5, TimeUnit.SECONDS);
        } catch (Throwable e) {
            if (e instanceof TimeoutException) {
                return new JSONObject().put("success", false).put("reason", "timed-out");
            } else {
                throw e;
            }
        }
    }

    /**
     * Takes a URL and returns the Bitmap image.
     */
    private static class BitmapTask extends AsyncTask<String, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                return BitmapFactory.decodeStream(new java.net.URL(strings[0]).openStream());
            } catch (Throwable e) {
                if (DEV_MODE) Log.e("InternetHelper.BitmapTask", e);
                return null;
            }
        }
    }

    /**
     * Takes a JSONObject containing the URL and POST information, and returns the response as JSON.
     */

    private static class HTTPJsonTask extends AsyncTask<JSONObject, Integer, JSONObject> {
        @Override
        protected JSONObject doInBackground(JSONObject... jsonObjects) {
            try {
                BufferedReader stream;
                StringBuilder builder;
                URL url = new URL(jsonObjects[0].getString("url"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (jsonObjects[0].has("post")) {
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    builder = new StringBuilder();
                    JSONArray keys = jsonObjects[0].getJSONObject("post").names();
                    int index = 0;
                    while (index < keys.length()) {
                        builder.append(keys.getString(index))
                                .append("=")
                                .append(URLEncoder.encode(jsonObjects[0].getJSONObject("post").getString(keys.getString(index)), "UTF-8"))
                                .append("&");
                        index++;
                    }
                    wr.write(builder.substring(0, builder.length() - 1));
                    wr.flush();
                }
                stream = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                builder = new StringBuilder();
                String line;
                while ((line = stream.readLine()) != null) {
                    builder.append(line);
                }
                stream.close();
                if (DEV_MODE) {
                    if (jsonObjects[0].has("post")) {
                        Log.d("InternetHelper.HTTPJsonTask", "\nRequested: " + jsonObjects[0].getString("url") +
                                "\nPosted: \n" + jsonObjects[0].getJSONObject("post").toString(4) +
                                "\nRecieved: \n" + builder.toString());
                    } else {
                        Log.d("InternetHelper.HTTPJsonTask", "\nRequested: " + jsonObjects[0].getString("url") +
                                "\nReceived: " + builder.toString());
                    }
                }
                try {
                    return new JSONObject(builder.toString());
                } catch (JSONException e) {
                    // No need to report the error, already logged output above
                    if (builder.toString().length() > 0)
                        return new JSONObject().put("success", false).put("reason", builder.toString());
                    else
                        return new JSONObject().put("success", false).put("reason", "no-response");
                }
            } catch (Throwable e) {
                if (DEV_MODE) Log.e("InternetHelper.HTTPJsonTask.catch1", e);
                try {
                    return new JSONObject().put("success", false).put("reason", e.getMessage());
                } catch (Throwable e2) {
                    if (DEV_MODE) Log.e("InternetHelper.HTTPJsonTask.catch2", e2);
                    return null; // Return null, easier to know if the request failed
                }
            }
        }
    }

}
