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
import android.util.Log;

import org.json.JSONObject;

import static net.flare_esports.csgoskill.Constants.*;
import static net.flare_esports.csgoskill.InternetHelper.*;

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
            String response = HTTPRequest("http://api.csgo-skill.com/version");
            if (response.isEmpty()) {
                throw new Throwable("Empty response");
            }
            new_version = response;
            int first = new_version.indexOf('.');
            int last = new_version.lastIndexOf('.');
            JSONObject version = new JSONObject()
                    .put("major", Integer.valueOf(new_version.substring(1, first)))
                    .put("minor", Integer.valueOf(new_version.substring(first + 1, last)))
                    .put("point", Integer.valueOf(new_version.substring(last + 1)));
            if (C_MAJOR < version.getInt("major")) {
                return 0;
            } else if (C_MAJOR == version.getInt("major")) {
                if (C_MINOR < version.getInt("minor")) {
                    return 0;
                } else if (C_MINOR == version.getInt("minor") && C_POINT < version.getInt("point")) {
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
    public String getNewVersion() { return new_version; }

    /**
     * Returns the current app version.
     *
     * @return The current app version, ex: "v1.0.0"
     */
    public String getVersion() { return "v" + C_MAJOR + "." + C_MINOR; }

    /**
     * Returns the int constant of the last (silently) thrown error.
     *
     * @return Last (silently) thrown error constant
     */
    public Throwable lastError() { return last_error; }

    /**
     * Inserts the given user (Username, Steam ID, Email, Secret), updateUser() should be called immediately after.
     * @param data Contains the 'username', 'steamid', 'email', and 'secret' information
     * @return     True if successful, false otherwise.
     */
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
     * @return        True if successful or up-to-date, false otherwise.
     */
    public boolean updateUser(String steamId, String secret) {
        try {
            // Grab email
            JSONObject request = new JSONObject()
                    .put("url", "http://api.csgo-skill.com/profile?id=" + steamId)
                    .put("post", new JSONObject()
                            .put("secret", secret)
                    );
            JSONObject response = HTTPJsonRequest(request);

            if (response.length() < 1 || response.has("message"))
                throw new Throwable((response.length() < 1) ? "Empty response" : response.getString("message"));

            ContentValues values = new ContentValues();
            values.put(EMAIL, response.getString("email"));
            values.put(STATUS, response.getString("status"));
            values.put(AVATAR, response.getString("avatar"));
            values.put(VANITY_URL, response.getString("url"));
            values.put(USERNAME, response.getString("persona"));
            request = new JSONObject()
                    .put("url", "http://api.csgo-skill.com/stats?steamid=" + steamId);
            response = HTTPJsonRequest(request);
            if (response.length() < 1 || response.has("message")) {
                // oh no
                SQLiteDatabase sql = getWritableDatabase();
                if (sql.update(USERTABLE, values, STEAMID+"=?", new String[]{steamId}) > 0) {
                    sql.close();
                    throw new Throwable("Failed to update player stats. " + ((response.length() < 1) ? "Empty response" : response.getString("message")));
                } else {
                    sql.close();
                    throw new Throwable("Failed to update player information. " + ((response.length() < 1) ? "Empty response" : response.getString("message")));
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
                        .put("username", c.getString(c.getColumnIndex(USERNAME)))
                        .put("steamid", steamId)
                        .put("email", c.getString(c.getColumnIndex(EMAIL)))
                        .put("status", new JSONObject(c.getString(c.getColumnIndex(STATUS))))
                        .put("avatar", c.getString(c.getColumnIndex(AVATAR)))
                        .put("url", c.getString(c.getColumnIndex(VANITY_URL)));
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
     * Uses getUserInfo() to return a list of all users on the device.
     *
     * @return JSONObject[] of all users, using getUserInfo()
     */
    public JSONObject[] getUsers() {
        SQLiteDatabase sql = getWritableDatabase();
        Cursor c = sql.rawQuery("SELECT " + STEAMID + " FROM " + USERTABLE + " WHERE 1", null);
        if (c.getCount() <= 0) {
            c.close();
            sql.close();
            return new JSONObject[0];
        } else {
            JSONObject[] users = new JSONObject[c.getCount()];
            int index = 0;
            int column = c.getColumnIndex(STEAMID);
            while (c.moveToNext()) {
                users[index] = getUserInfo(c.getString(column));
                index++;
            }
            c.close();
            sql.close();
            return users;
        }
    }

}
