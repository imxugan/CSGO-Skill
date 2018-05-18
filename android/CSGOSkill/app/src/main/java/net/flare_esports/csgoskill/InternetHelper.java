/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

// Intentionally left as Java

package net.flare_esports.csgoskill;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.flare_esports.csgoskill.Constants.DEV_MODE;
import static net.flare_esports.csgoskill.Constants.NO_RESPONSE;
import static net.flare_esports.csgoskill.Constants.REQUEST_FAIL;
import static net.flare_esports.csgoskill.Constants.REQUEST_TIMEOUT;

/**
 * Handles all internet interactions in a simplified way, very useful when posting data and
 * expecting a JSON response.
 */
class InternetHelper {

    /**
     * Pings Google server for a response, assuming no internet connection if there is no response
     *
     * @return {@code true} if connection is available, {@code false} otherwise
     */
    public static boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (Throwable e) {
            if (DEV_MODE) Log.e("InternetHelper.isOnline", e);
        }
        return false;
    }

    /**
     * Makes an internet Bitmap request on the current thread, useful for calling inside an {@link AsyncTask}
     *
     * @param url image url
     * @return {@link Bitmap} image, may be {@code null}
     */
    @Nullable
    public static Bitmap HardBitmapRequest(String url) {
        return new BitmapTask().doInBackground(url);
    }

    /**
     * Makes an internet Bitmap request using {@link AsyncTask}, will wait 3 seconds before giving up
     *
     * @param url image url
     * @return {@link Bitmap} image, may be {@code null}
     */
    @Nullable
    public static Bitmap BitmapRequest(String url) {
        try {
            return new BitmapTask().execute(url).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Simply return null for failed requests, we can try again later
            if (DEV_MODE) Log.e("InternetHelper.BitmapRequest", e);
            return null;
        }
    }

    /**
     * Makes a plain request using {@link AsyncTask}, only returning the response body
     *
     * @param url request location
     * @return response body, may be empty, {@link Constants#REQUEST_TIMEOUT}, or {@link Constants#REQUEST_FAIL}
     */
    @NotNull
    public static String HTTPRequest(String url) {
        try {
            JSONObject object = new HTTPJsonTask().execute(new JSONObject().put("url", url)).get(5, TimeUnit.SECONDS);
            if (object == null) return "";
            else if (object.has("reason")) return object.optString("reason", REQUEST_FAIL);
            else return object.toString();
        } catch (JSONException e) {
            if (DEV_MODE) {
                Log.e("InternetHelper.HTTPRequest", "provided url \"" + url + "\" could not be put() into JSONObject");
                Log.e("InternetHelper.HTTPRequest", e);
            }
            return REQUEST_FAIL;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // In general, the request basically just fails, but we'll say it timed out since we could retry later
            return REQUEST_TIMEOUT;
        }
    }

    /**
     * Makes an internet JSON request on the current thread, useful for calling inside an {@link AsyncTask}
     *
     * @param request <p>{@link JSONObject} containing two keys, {@code url} which is a string,
     *                 and {@code post} which is another JSONObject with keys and values to post.</p>
     * @return <p>{@link JSONObject} containing two keys, {@code success} which is a boolean if
     *         the request gave a valid JSON response, and {@code response} which contains either
     *         an error code if {@code success} was false, or the JSON response of the request.</p>
     */
    @NotNull
    public static JSONObject HardHTTPJsonRequest(JSONObject request) {
        return new HTTPJsonTask().doInBackground(request);
    }

    /**
     * Makes an internet JSON request using {@link AsyncTask}
     *
     * @param request <p>{@link JSONObject} containing two keys, {@code url} which is a string,
     *                 and {@code post} which is another JSONObject with keys and values to post.</p>
     * @return <p>{@link JSONObject} containing two keys, {@code success} which is a boolean if
     *         the request gave a valid JSON response, and {@code response} which contains either
     *         an error code if {@code success} was false, or the JSON response of the request.</p>
     */
    @NotNull
    public static JSONObject HTTPJsonRequest(JSONObject request) {
        try {
            return new HTTPJsonTask().execute(request).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            try {
                return new JSONObject().put("success", false).put("reason", REQUEST_TIMEOUT);
            } catch (Throwable e2) {
                return new JSONObject(); // Spectacular fail
            }
        }
    }

    /**
     * Takes a single url and returns a Bitmap, may be null
     */
    private static class BitmapTask extends AsyncTask<String, Integer, Bitmap> {
        @Override
        @Nullable
        protected Bitmap doInBackground(String... params) {
            try {
                return BitmapFactory.decodeStream(new java.net.URL(params[0]).openStream());
            } catch (IOException e) {
                if (DEV_MODE) Log.e("InternetHelper.BitmapTask", e);
                return null;
            }
        }
    }

    /**
     * Takes a single JSONObject containing the URL and POST information, and returns the response as JSON
     */
    private static class HTTPJsonTask extends AsyncTask<JSONObject, Integer, JSONObject> {
        @Override
        @NotNull
        protected JSONObject doInBackground(JSONObject... params) {
            try {
                BufferedReader stream;
                StringBuilder builder;
                JSONObject jsonObject = params[0];
                URL url = new URL(jsonObject.getString("url"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (jsonObject.has("post")) {
                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    builder = new StringBuilder();
                    JSONArray keys = jsonObject.getJSONObject("post").names();
                    int index = 0;
                    while (index < keys.length()) {
                        builder.append(keys.getString(index))
                                .append("=")
                                .append(URLEncoder.encode(jsonObject.getJSONObject("post").getString(keys.getString(index)), "UTF-8"))
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
                    if (jsonObject.has("post")) {
                        Log.d("InternetHelper.HTTPJsonTask", "\nRequested: " + jsonObject.getString("url") +
                                "\nPosted: \n" + jsonObject.getJSONObject("post").toString(4) +
                                "\nReceived: \n" + builder.toString());
                    } else {
                        Log.d("InternetHelper.HTTPJsonTask", "\nRequested: " + jsonObject.getString("url") +
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
                        return new JSONObject().put("success", false).put("reason", NO_RESPONSE);
                }
            } catch (Throwable e) {
                if (DEV_MODE) Log.e("InternetHelper.HTTPJsonTask.catch1", e);
                try {
                    return new JSONObject().put("success", false).put("reason", e.getMessage());
                } catch (Throwable e2) {
                    if (DEV_MODE) Log.e("InternetHelper.HTTPJsonTask.catch2", e2);
                    return new JSONObject(); // Spectacular fail
                }
            }
        }
    }

}
