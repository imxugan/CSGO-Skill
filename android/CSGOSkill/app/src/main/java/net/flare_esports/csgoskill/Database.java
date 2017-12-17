/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

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

import static net.flare_esports.csgoskill.Constants.*;

class Database extends SQLiteOpenHelper{

    private static final int VERSION = 1;
    private static final String NAME = "csgoskill.db";

    private static final String USERTABLE = "Accounts";
    private static final String DATATABLE = "Data";

    private static final String USERNAME = "username";
    private static final String STEAMID = "steamID";
    private static final String EMAIL = "email";
    private static final String STATUS = "status";
    private static final String AVATAR = "avatar";
    private static final String VANITY_URL = "url";
    private static final String SECRET = "secret";
    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String VERS = "version";

    private String new_version = "";
    private Throwable last_error = null;

    private Context context;

    public Database(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, NAME, factory, VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE " + USERTABLE + " (" +
            USERNAME + " TEXT," +           // Custom username, updated at startup
            STEAMID + " TEXT UNIQUE," +     // Steam ID
            SECRET + " TEXT PRIMARY KEY," + // Secret
            EMAIL + " TEXT UNIQUE," +       // Email, updated at startup
            STATUS + " TEXT," +             // Account standing info
            AVATAR + " TEXT," +             // Link to full avatar
            VANITY_URL + " TEXT," +         // Link to Steam profile
            DATA + " TEXT)"                 // Stats, pulled from the server (includes main tasks)
        );

        sqLiteDatabase.execSQL("CREATE TABLE " + DATATABLE + " (" +
            TYPE + " TEXT UNIQUE," +        // Name/ Type of data stored in the row
            VERS + " TEXT," +               // Version of data
            DATA + " TEXT)"                 // The data
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        // Currently there are no plans to change the structure of the DB, so nothing is here

    }

    /**
     * Asks the server for the most current version number. When outdated, it will set
     * a variable called 'newVersion' to the response before returning.
     *
     * @return 1 if up-to-date, 0 if updates available, -1 if error
     */
    public int checkVersion() {
        try {
            JSONObject request = new JSONObject()
                    .put("url", "http://api.csgo-skill.com/version");
            JSONObject response = new HTTPJsonTask().execute(request).get(5, TimeUnit.SECONDS);
            new_version = response.getString("message");
            int first = new_version.indexOf('.');
            int last = new_version.lastIndexOf('.');
            response.put("major", Integer.valueOf(new_version.substring(1, first)));
            response.put("minor", Integer.valueOf(new_version.substring(first + 1, last)));
            response.put("point", Integer.valueOf(new_version.substring(last + 1)));
            if (C_MAJOR < response.getInt("major")) {
                return 0;
            } else if (C_MAJOR == response.getInt("major")) {
                if (C_MINOR < response.getInt("minor")) {
                    return 0;
                } else if (C_MINOR == response.getInt("minor") && C_POINT < response.getInt("point")) {
                    return 0;
                }
            }
            return 1;
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return -1;
    }

    /**
     * Returns the most recently received version number set by checkVersion()
     *
     * @return The most recently received version number, or null.
     */
    public String getNewVersion() {
        return new_version;
    }

    /**
     * Returns the current app version.
     *
     * @return The current app version, ex: "v1.0.0"
     */
    public String getVersion() {
        return "v" + C_MAJOR + "." + C_MINOR;
    }

    /**
     * Returns the int constant of the last (silently) thrown error.
     *
     * @return Last (silently) thrown error constant
     */
    public Throwable lastError() {
        return last_error;
    }

    public boolean insertUser(JSONObject data) {
        try {
            /*
            USERNAME + " TEXT," +           // Custom username, updated at startup
            STEAMID + " TEXT UNIQUE," +     // Steam ID
            SECRET + " TEXT PRIMARY KEY," + // Secret
            EMAIL + " TEXT UNIQUE," +       // Email, updated at startup
            STATUS + " TEXT," +             // Account standing info
            AVATAR + " TEXT," +             // Link to full avatar
            VANITY_URL + " TEXT," +         // Link to Steam profile
            DATA + " TEXT)"                 // Stats, pulled from the server (includes main tasks)
             */
            SQLiteDatabase sql = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(USERNAME, data.getString("username"));
            values.put(STEAMID, data.getString("steamid"));
            values.put(EMAIL, data.getString("email"));
            values.put(SECRET, data.getString("secret"));
            if (sql.insertOrThrow(USERTABLE, null, values) != -1){
                sql.close();
                return true;
            }
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return false;
    }

    /**
     * Attempts to download the most recent stats for a given user.
     * Also downloads the persona and email. Should only be called every 30 minutes,
     * any more frequently and changes are unlikely to be seen.
     * @param steamId The Steam ID of the user to update.
     * @return true if successful or up-to-date, false otherwise.
     */
    public boolean updateUser(String steamId, String secret) {
        try {
            // Grab email
            JSONObject request = new JSONObject()
                    .put("url", "http://api.csgo-skill.com/profile?id=" + steamId)
                    .put("post", new JSONObject()
                            .put("secret", secret)
                    );
            JSONObject response = new HTTPJsonTask().execute(request).get(5, TimeUnit.SECONDS);
            if (response.has("message")) {
                // oh no
                throw new Throwable(response.getString("message"));
            }
            ContentValues values = new ContentValues();
            values.put(EMAIL, response.getString("email"));
            values.put(STATUS, response.getString("status"));
            values.put(AVATAR, response.getString("avatar"));
            values.put(VANITY_URL, response.getString("url"));
            values.put(USERNAME, response.getString("persona"));
            request = new JSONObject()
                    .put("url", "http://api.csgo-skill.com/stats?steamid=" + steamId);
            response = new HTTPJsonTask().execute(request).get(5, TimeUnit.SECONDS);
            if (response.has("message")) {
                // oh no
                SQLiteDatabase sql = getWritableDatabase();
                if (sql.update(USERTABLE, values, STEAMID+"=?", new String[]{steamId}) > 0) {
                    sql.close();
                    throw new Throwable("Failed to update player stats. " + response.getString("message"));
                } else {
                    sql.close();
                    throw new Throwable("Failed to update player information. " + response.getString("message"));
                }
            }
            values.put(DATA, response.toString());
            SQLiteDatabase sql = getWritableDatabase();
            if (sql.update(USERTABLE, values, STEAMID+"=?", new String[]{steamId}) > 0) {
                sql.close();
                return true;
            } else {
                sql.close();
                throw new Throwable("Failed to update player information.");
            }
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return false;
    }

    /**
     * Gets a JSONObject representing the given user's info, according to the device.
     * DOES NOT request info from server.
     *
     * @param steamId The Steam ID of the user
     * @return        JSONObject of user information, or null if error occurred.
     */
    public JSONObject getUserInfo(String steamId) {
        try {
            // Grab stuff
            SQLiteDatabase sql = getWritableDatabase();
            Cursor c = sql.rawQuery("SELECT " +
                    USERNAME + ", " +
                    EMAIL + ", " +
                    STATUS + ", " +
                    AVATAR + "," +
                    VANITY_URL + " FROM " + USERTABLE +
                    " WHERE " + STEAMID + " = \"" + steamId + "\"", null);
            if (c.getCount() <= 0) {
                c.close();
                sql.close();
                throw new Throwable("No user found with Steam ID " + steamId);
            } else {
                c.moveToFirst();
                JSONObject result = new JSONObject()
                        .put(USERNAME, c.getString(c.getColumnIndex(USERNAME)))
                        .put(EMAIL, c.getString(c.getColumnIndex(EMAIL)))
                        .put(STATUS, new JSONObject(c.getString(c.getColumnIndex(STATUS))))
                        .put(AVATAR, c.getString(c.getColumnIndex(AVATAR)))
                        .put(VANITY_URL, c.getString(c.getColumnIndex(VANITY_URL)));
                c.close();
                sql.close();
                return result;
            }
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return null;
    }

    /**
     * Gets a JSONObject representing the given user's global tracked stats, according to the device.
     * DOES NOT request info from server.
     *
     * @param steamId The Steam ID of the user
     * @return        JSONObject of the user's global stats, or null if error occurred.
     * @see #getUserStatsHistory(String steamId)
     */
    public JSONObject getUserStats(String steamId) {
        try {
            return grabData(steamId).getJSONObject("global");
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return null;
    }

    /**
     * Gets a JSONObject representing the given user's stat history, according to the device.
     * DOES NOT request info from server.
     *
     * @param steamId The Steam ID of the user
     * @return        JSONObject of the user's stat history, or null if error occurred.
     */
    public JSONObject getUserStatsHistory(String steamId) {
        try {
            JSONObject result = grabData(steamId);
            return new JSONObject()
                    .put("daily", result.getJSONArray("daily"))
                    .put("monthly", result.getJSONArray("monthly"));
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return null;
    }

    /**
     * Gets a JSONObject of the most recently saved stats of user (the Steam API response), according to the device.
     * DOES NOT request info from server.
     *
     * @param steamId The Steam ID of the user
     * @return        JSONObject of the user's all-time stats, or null if error occurred.
     */
    public JSONObject getUserAllTime(String steamId) {
        try {
            return grabData(steamId).getJSONObject("current");
        } catch (Throwable e) {
            last_error = e;
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return null;
    }

    private JSONObject grabData(String steamId) throws Throwable {
        SQLiteDatabase sql = getWritableDatabase();
        Cursor c = sql.rawQuery("SELECT " +
                DATA + " FROM " + USERTABLE +
                " WHERE " + STEAMID + " = \"" + steamId + "\"", null);
        if (c.getCount() <= 0) {
            c.close();
            sql.close();
            throw new Throwable("No user found with Steam ID " + steamId);
        } else {
            c.moveToFirst();
            JSONObject result = new JSONObject(c.getString(c.getColumnIndex(DATA)));
            c.close();
            sql.close();
            return result;
        }
    }

    /**
     * Takes a JSONObject containing the URL and POST information, and returns the response as JSON.
     */

    private class HTTPJsonTask extends AsyncTask<JSONObject, Integer, JSONObject> {
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
                try {
                    return new JSONObject(builder.toString());
                } catch (JSONException e) {
                    return new JSONObject().put("message", builder.toString());
                }
            } catch (Throwable e) {
                try {
                    return new JSONObject().put("message", e.getMessage());
                } catch (Throwable e2) {
                    return null;
                }
            }
        }
    }
}
