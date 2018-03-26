/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

import org.json.JSONObject

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.*

class Database(
        private var context: Context,
        factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(context, NAME, factory, VERSION) {

    companion object {
        private const val VERSION = 1
        private const val NAME = "csgoskill.db"
        private const val USERTABLE = "Accounts"
        private const val DATATABLE = "Data"

        private const val USERNAME = "username"
        private const val STEAMID = "steamID"
        private const val EMAIL = "email"
        private const val STATUS = "status"
        private const val AVATAR = "avatar"
        private const val VANITY_URL = "url"
        private const val SECRET = "secret"
        private const val DATA = "data"
        private const val TYPE = "type"
        private const val VERS = "version"
    }

    private var newVersion = ""
    private var lastError: Throwable? = null

    val version: String
        get() = "v$C_MAJOR.$C_MINOR"

    /**
     * Uses getUserInfo() to return a list of all users on the device.
     *
     * @return JSONObject[] of all users, using getUserInfo()
     */
    val users: Array<JSONObject>
        get() {
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT $STEAMID FROM $USERTABLE WHERE 1", null)
            if (c.count <= 0) {
                c.close()
                sql.close()
                return emptyArray()
            } else {
                val users = Array(c.count){JSONObject()}
                var index = 0
                val column = c.getColumnIndex(STEAMID)
                while (c.moveToNext()) {
                    users[index] = getUserInfo(c.getString(column))
                    index++
                }
                c.close()
                sql.close()
                return users
            }
        }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE " + USERTABLE + " (" +
                USERNAME + " TEXT," +           // Custom username, updated at startup
                STEAMID + " TEXT UNIQUE," +     // Steam ID
                SECRET + " TEXT PRIMARY KEY," + // Secret
                EMAIL + " TEXT UNIQUE," +       // Email, updated at startup
                STATUS + " TEXT," +             // Account standing info
                AVATAR + " TEXT," +             // Link to full avatar
                VANITY_URL + " TEXT," +         // Link to Steam profile
                DATA + " TEXT)"                 // Stats, pulled from the server (includes main tasks)
        )

        sqLiteDatabase.execSQL("CREATE TABLE " + DATATABLE + " (" +
                TYPE + " TEXT UNIQUE," +        // Name/ Type of data stored in the row
                VERS + " TEXT," +               // Version of data
                DATA + " TEXT)"                 // The data
        )

    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        // Currently there are no plans to change the structure of the DB, so nothing is here

    }

    /**
     * <p>Asks the server for the most current version number. When outdated,
     * it will set a variable called 'newVersion' to the response before
     * returning.</p>
     *
     * @return 1 if up-to-date, 0 if updates available, -1 if error
     */
    fun checkVersion(): Int {
        try {
            if (!isOnline()) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            val response = HTTPRequest("http://api.csgo-skill.com/version")
            if (response.isEmpty()) {
                throw Throwable("No response")
            } else if (response == "Unable to resolve host \"api.csgo-skill.com\": No address associated with hostname") {
                throw Throwable("No connection")
            }
            newVersion = response
            val first = newVersion.indexOf('.')
            val last = newVersion.lastIndexOf('.')
            val version = JSONObject()
                    .put("major", Integer.valueOf(newVersion.substring(1, first)))
                    .put("minor", Integer.valueOf(newVersion.substring(first + 1, last)))
                    .put("point", Integer.valueOf(newVersion.substring(last + 1)))
            if (C_MAJOR < version.getInt("major")) {
                return 0
            } else if (C_MAJOR == version.getInt("major")) {
                if (C_MINOR < version.getInt("minor")) {
                    return 0
                } else if (C_MINOR == version.getInt("minor")
                        && C_POINT < version.getInt("point")) {
                    return 0
                }
            }
            return 1
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) {
                if (e.message == "No connection")
                    Log.e("Database.checkVersion", "No connection")
                else
                    Log.e("Database.checkVersion", e)
            }
        }

        return -1
    }

    /**
     * Returns the int constant of the last (silently) thrown error.
     *
     * @return Last (silently) thrown error constant
     */
    fun lastError(): Throwable? {
        return lastError
    }

    /**
     * Polls the database to see if a given Steam ID is found.
     *
     * @param steamId The Steam ID of the account to search
     * @return True if found, false otherwise
     */
    fun hasUser(steamId: String): Boolean {
        val l = users.size
        var i = 0
        try {
            while (i < l) {
                if (steamId == users[i].getString("steamid")) return true
                i++
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.hasUser", e)
        }
        return false
    }

    /**
     * <p>Inserts the given user (Username, Steam ID, Email, Secret),
     * updateUser() should be called immediately after.</p>
     *
     * @param data Contains the 'username', 'steamid', 'email', and 'secret' information
     * @return True if successful, false otherwise.
     */
    fun insertUser(data: JSONObject): Boolean {
        try {
            val sql = writableDatabase
            val values = ContentValues()
            values.put(USERNAME, data.getString("username"))
            values.put(STEAMID, data.getString("steamid"))
            values.put(EMAIL, data.getString("email"))
            values.put(SECRET, data.getString("secret"))
            if (sql.insertOrThrow(USERTABLE, null, values) != -1L) {
                sql.close()
                return true
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.insertUser", e)
        }

        return false
    }

    /**
     * <p>Attempts to download the most recent stats for a given user. Also
     * downloads the persona and email. Should only be called every 30 minutes,
     * any more frequently and changes are unlikely to be seen.</p>
     *
     * This function will run assuming our servers know the account. If the
     * server doesn't, it would provide a response that would ultimately throw
     * an error when trying to access "persona". This is desired behavior.
     *
     * @param steamId The Steam ID of the user to update.
     * @return True if successful or up-to-date, false otherwise.
     */
    fun updateUser(steamId: String, secret: String): Boolean {
        try {
            if (!isOnline()) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/profile?id=" + steamId)
                    .put("post", JSONObject()
                            .put("secret", secret)
                    )
            request = HTTPJsonRequest(request)

            if (request.length() < 1 || request.has("message"))
                throw Throwable("Failed to update player.\n" +
                                if (request.length() < 1) "Empty response"
                                else request.getString("message"))
            // This call is safe, it allows us to call updateUser even if the
            // account isn't on the device.
            if (!hasUser(steamId)) insertUser(request.put("secret", secret))

            val values = ContentValues()
            values.put(EMAIL, request.getString("email"))
            values.put(STATUS, request.getString("status"))
            values.put(AVATAR, request.getString("avatar"))
            values.put(VANITY_URL, request.getString("url"))
            values.put(USERNAME, request.getString("persona"))
            request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/stats?steamid=" + steamId)
            request = HTTPJsonRequest(request)
            if (request.length() < 1 || request.has("message")) {
                // Response is NOT good!
                val sql = writableDatabase
                if (sql.update(USERTABLE, values, STEAMID + "=?", arrayOf(steamId)) > 0) {
                    sql.close()
                    throw Throwable("Failed to update player stats.\n" +
                            if (request.length() < 1) "Empty response"
                            else request.getString("message"))
                } else {
                    sql.close()
                    throw Throwable("Failed to update player information.\n" +
                            if (request.length() < 1) "Empty response"
                            else request.getString("message"))
                }
            }
            values.put(DATA, request.toString())
            val sql = writableDatabase
            if (sql.update(USERTABLE, values, STEAMID + "=?", arrayOf(steamId)) > 0) {
                sql.close()
                return true
            } else {
                sql.close()
                throw Throwable("Failed to update player information.")
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.updateUser", e)
        }

        return false
    }

    /**
     * <p>Gets a JSONObject representing the given user's info, according to
     * the device. DOES NOT request info from server.</p>
     *
     * @param steamId The Steam ID of the user
     * @return JSONObject of user information, or null if error occurred.
     */
    fun getUserInfo(steamId: String): JSONObject {
        try {
            // Grab stuff
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT " +
                    USERNAME + ", " +
                    SECRET + ", " +
                    EMAIL + ", " +
                    STATUS + ", " +
                    AVATAR + "," +
                    VANITY_URL + " FROM " + USERTABLE +
                    " WHERE " + STEAMID + " = \"" + steamId + "\"", null)
            if (c.count <= 0) {
                c.close()
                sql.close()
                throw Throwable("No user found with Steam ID " + steamId)
            } else {
                c.moveToFirst()
                val result = JSONObject()
                        .put("username", c.getString(c.getColumnIndex(USERNAME)))
                        .put("secret", c.getString(c.getColumnIndex(SECRET)))
                        .put("steamid", steamId)
                        .put("email", c.getString(c.getColumnIndex(EMAIL)))
                        .put("status", JSONObject(c.getString(c.getColumnIndex(STATUS))))
                        .put("avatar", c.getString(c.getColumnIndex(AVATAR)))
                        .put("url", c.getString(c.getColumnIndex(VANITY_URL)))
                c.close()
                sql.close()
                return result
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.getUserInfo", e)
        }

        return JSONObject()
    }

    /**
     * <p>Gets a JSONObject representing the given user's global tracked stats,
     * according to the device. DOES NOT request info from server.</p>
     *
     * @param steamId The Steam ID of the user
     * @return JSONObject of the user's global stats, or null if error occurred.
     * @see #getUserStatsHistory(String)
     */
    fun getUserStats(steamId: String): JSONObject? {
        try {
            return grabData(steamId).getJSONObject("global")
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.getUserStats", e)
        }

        return null
    }

    /**
     * <p>Gets a JSONObject representing the given user's stat history,
     * according to the device. DOES NOT request info from server.</p>
     *
     * @param steamId The Steam ID of the user
     * @return JSONObject of the user's stat history, or null if error occurred.
     */
    fun getUserStatsHistory(steamId: String): JSONObject? {
        try {
            val result = grabData(steamId)
            return JSONObject()
                    .put("daily", result.getJSONArray("daily"))
                    .put("monthly", result.getJSONArray("monthly"))
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.getUserStatsHistory", e)
        }

        return null
    }

    /**
     * <p>Gets a JSONObject of the most recently saved stats of user (the Steam
     * API response), according to the device. DOES NOT request info from
     * server.</p>
     *
     * @param steamId The Steam ID of the user
     * @return JSONObject of the user's all-time stats, or null if error occurred.
     */
    fun getUserAllTime(steamId: String): JSONObject? {
        try {
            return grabData(steamId).getJSONObject("current")
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.getUserAllTime", e)
        }

        return null
    }

    @Throws(Throwable::class)
    private fun grabData(steamId: String): JSONObject {
        val sql = writableDatabase
        val c = sql.rawQuery("SELECT " +
                DATA + " FROM " + USERTABLE +
                " WHERE " + STEAMID + " = \"" + steamId + "\"", null)
        if (c.count <= 0) {
            c.close()
            sql.close()
            throw Throwable("No user found with Steam ID " + steamId)
        } else {
            c.moveToFirst()
            val result = JSONObject(c.getString(c.getColumnIndex(DATA)))
            c.close()
            sql.close()
            return result
        }
    }

}
