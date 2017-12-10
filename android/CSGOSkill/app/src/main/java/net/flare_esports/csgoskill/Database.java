/*
 * Created by the Dev Team from Flare E-Sports on 12/5/17 4:13 PM
 * Copyright (c) 2017. All rights reserved.
 *
 * Last modified 12/5/17 3:28 PM
 */

package net.flare_esports.csgoskill;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

public class Database extends SQLiteOpenHelper{

    private static final int VERSION = 1;
    private static final int MAJOR = 1;
    private static final int MINOR = 0;
    private static final String NAME = "csgoskill.db";

    private static final String USERTABLE = "Accounts";
    private static final String DATATABLE = "Data";

    private static final String USERNAME = "username";
    private static final String STEAMID = "steamID";
    private static final String EMAIL = "email";
    private static final String SECRET = "secret";
    private static final String DATA = "data";
    private static final String TASKS = "tasks";
    private static final String TYPE = "type";
    private static final String VERS = "version";

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
            DATA + " TEXT," +               // Stats, pulled from the server (includes main tasks)
            TASKS + " TEXT)"                // Recently selected (Daily) Tasks, to prevent doubling
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
     * @return 1 if up-to-date, 0 if guide updates available, -1 if outdated.
     */
    public int checkVersion() {
        // TODO
        return -1;
    }

    /**
     * Returns the most recently received version number set by checkVersion()
     *
     * @return The most recently received version number, or null.
     */
    public String getNewVersion() {
        // TODO
        return null;
    }

    /**
     * Returns the current app version, as last set by the most recent guide update.
     *
     * @return The current app version, ex: "v1.0.0"
     */
    public String getVersion() {
        // TODO
        return null;
    }

    /**
     * Returns the int constant of the last (silently) thrown error.
     *
     * @return Last (silently) thrown error constant
     */
    public int lastError() {
        // TODO
        return -1;
    }

    /**
     * Attempts to download the most recent stats for a given user.
     * Also downloads the persona and email. Should only be called every 30 minutes,
     * any more frequently and changes are unlikely to be seen.
     * @param steamId The Steam ID of the user to update.
     * @return true if successful or up-to-date, false otherwise.
     */
    public boolean updateUser(String steamId) {
        // TODO
        return false;
    }

    /**
     * Gets a JSONObject representing the given user's persona and email, according to the device.
     * DOES NOT request info from server.
     *
     * @param steamId The Steam ID of the user
     * @return        JSONObject of user information, or null if error occurred.
     */
    public JSONObject getUserInfo(String steamId) {
        // TODO
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
        // TODO
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
        // TODO
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
        // TODO
        return null;
    }

}
